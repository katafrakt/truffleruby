package org.truffleruby.core.fiber;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.truffleruby.Layouts;
import org.truffleruby.RubyContext;
import org.truffleruby.core.array.ArrayHelpers;
import org.truffleruby.core.proc.ProcOperations;
import org.truffleruby.core.thread.ThreadManager;
import org.truffleruby.language.RubyGuards;
import org.truffleruby.language.control.RaiseException;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.profiles.BranchProfile;

public class FiberManagerContinuationImpl implements FiberManager {

    private final RubyContext context;
    private final DynamicObject rootFiber;
    private DynamicObject currentFiber;
    private DynamicObject scheduledFiber;

    public FiberManagerContinuationImpl(RubyContext context, DynamicObject rubyThread) {
        this.context = context;
        this.rootFiber = createRootFiber(context, rubyThread);
        this.currentFiber = rootFiber;
    }

    private DynamicObject createRootFiber(RubyContext context2, DynamicObject rubyThread) {
        return createFiber(context, rubyThread, context.getCoreLibrary().getFiberFactory(), "root Fiber for Thread");
    }

    @TruffleBoundary
    private static Set<DynamicObject> newFiberSet() {
        return Collections.newSetFromMap(new ConcurrentHashMap<DynamicObject, Boolean>());
    }

    public DynamicObject getRootFiber() {
        return rootFiber;
    }

    public DynamicObject getCurrentFiber() {
        assert Layouts.THREAD.getFiberManager(context.getThreadManager().getCurrentThread()) == this : "Trying to read the current Fiber of another Thread which is inherently racy";
        return currentFiber;
    }

    // If the currentFiber is read from another Ruby Thread,
    // there is no guarantee that fiber will remain the current one
    // as it could switch to another Fiber before the actual operation on the returned fiber.
    public DynamicObject getCurrentFiberRacy() {
        return currentFiber;
    }

    public DynamicObject getRubyFiberFromCurrentJavaThread() {
        return currentFiber;
    }

    private void setCurrentFiber(DynamicObject fiber) {
        assert RubyGuards.isRubyFiber(fiber);
        currentFiber = fiber;
    }

    public DynamicObject createFiber(RubyContext context, DynamicObject thread, DynamicObjectFactory factory, String name) {
        assert RubyGuards.isRubyThread(thread);
        CompilerAsserts.partialEvaluationConstant(context);
        final DynamicObject fiberLocals = Layouts.BASIC_OBJECT.createBasicObject(context.getCoreLibrary().getObjectFactory());
        final DynamicObject catchTags = ArrayHelpers.createArray(context, null, 0);

        return Layouts.FIBER.createFiber(
                factory,
                fiberLocals,
                catchTags,
                new CountDownLatch(0),
                new CountDownLatch(0),
                newMessageQueue(),
                thread,
                null,
                true,
                null,
                false);
    }

    @TruffleBoundary
    private static LinkedBlockingQueue<FiberManager.FiberMessage> newMessageQueue() {
        return new LinkedBlockingQueue<>();
    }

    private static final BranchProfile UNPROFILED = BranchProfile.create();

    public void initialize(DynamicObject fiber, DynamicObject block, Node currentNode) {
        assert fiber != rootFiber;
        Runnable runnable = () -> {
            ContinuationHelper.safeYield();
            final Object[] args = getArgs(fiber);
            final Object result;
            try {
                try {
                    currentFiber = fiber;
                    result = ProcOperations.rootCall(block, args);
                } finally {
                    Layouts.FIBER.setAlive(fiber, false);
                }
                resume(fiber, getReturnFiber(fiber, currentNode, UNPROFILED), FiberOperation.YIELD, new Object[]{ result });
            } finally {
                cleanup(fiber, Thread.currentThread());
            }
        };
        Object continuation = null;
        try {
            continuation = ContinuationHelper.newContinuation(runnable);
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
        }
        Layouts.FIBER.setThread(fiber, continuation);
    }

    private Object[] getArgs(DynamicObject fiber) {
        assert RubyGuards.isRubyFiber(fiber);

        final FiberManager.FiberMessage message = context.getThreadManager().runUntilResultKeepStatus(null,
                () -> Layouts.FIBER.getMessageQueue(fiber).take());

        setCurrentFiber(fiber);

        if (message instanceof FiberShutdownMessage) {
            throw new FiberShutdownException();
        } else if (message instanceof FiberExceptionMessage) {
            throw ((FiberExceptionMessage) message).getException();
        } else if (message instanceof FiberResumeMessage) {
            final FiberResumeMessage resumeMessage = (FiberResumeMessage) message;
            assert context.getThreadManager().getCurrentThread() == Layouts.FIBER.getRubyThread(resumeMessage.getSendingFiber());
            if (resumeMessage.getOperation() == FiberOperation.RESUME) {
                Layouts.FIBER.setLastResumedByFiber(fiber, resumeMessage.getSendingFiber());
            }
            return resumeMessage.getArgs();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Signal that a fiber should be the next one scheduled and should preform the indicated
     * operation with args.
     */
    private void scheduleFiber(DynamicObject fromFiber, DynamicObject fiber, FiberOperation operation, Object... args) {
        addToMessageQueue(fiber, new FiberManager.FiberResumeMessage(operation, fromFiber, args));
        scheduledFiber = fiber;
    }

    @TruffleBoundary
    private void addToMessageQueue(DynamicObject fiber, FiberManager.FiberMessage message) {
        Layouts.FIBER.getMessageQueue(fiber).add(message);
    }

    @Override
    public DynamicObject getReturnFiber(DynamicObject currentFiber, Node currentNode, BranchProfile errorProfile) {
        assert currentFiber == this.currentFiber;

        if (currentFiber == rootFiber) {
            errorProfile.enter();
            throw new RaiseException(context, context.getCoreExceptions().yieldFromRootFiberError(currentNode));
        }

        final DynamicObject parentFiber = Layouts.FIBER.getLastResumedByFiber(currentFiber);
        if (parentFiber != null) {
            Layouts.FIBER.setLastResumedByFiber(currentFiber, null);
            return parentFiber;
        } else {
            return rootFiber;
        }
    }

    public Object[] transferControlTo(DynamicObject fromFiber, DynamicObject fiber, FiberOperation operation, Object[] args) {
        resume(fromFiber, fiber, operation, args);
        return getArgs(fromFiber);
    }

    protected void resume(DynamicObject fromFiber, DynamicObject fiber, FiberOperation operation, Object[] args) {
        scheduleFiber(fromFiber, fiber, operation, args);
        if (fromFiber != rootFiber) {
            ContinuationHelper.safeYield();
        } else {
            while (scheduledFiber != rootFiber) {
                Object continuation = Layouts.FIBER.getThread(scheduledFiber);
                ContinuationHelper.safeRun(continuation);
            }
        }
    }

    public void start(DynamicObject fiber, Thread javaThread) {
        final ThreadManager threadManager = context.getThreadManager();
        final DynamicObject rubyThread = Layouts.FIBER.getRubyThread(fiber);
        threadManager.initializeValuesForJavaThread(rubyThread, javaThread);
    }

    public void cleanup(DynamicObject fiber, Thread javaThread) {
        if (fiber == rootFiber) {
            if (context.getThreadManager().isRubyManagedThread(javaThread)) {
                context.getSafepointManager().leaveThread();
            }

            context.getThreadManager().cleanupValuesForJavaThread(javaThread);
        }
        Layouts.FIBER.setAlive(fiber, false);

        Layouts.FIBER.setThread(fiber, null);
    }

    public void killOtherFibers() {
    }

    public void shutdown(Thread javaThread) {
        cleanup(rootFiber, javaThread);
    }

    public String getFiberDebugInfo() {
        return "We know nothing!";
    }

}
