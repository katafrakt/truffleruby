package org.truffleruby.core.fiber;

import org.truffleruby.RubyContext;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.profiles.BranchProfile;

public interface FiberManager {

    interface FiberMessage {
    }

    String NAME_PREFIX = "Ruby Fiber";

    DynamicObject getRootFiber();

    DynamicObject getCurrentFiber();

    // If the currentFiber is read from another Ruby Thread,
    // there is no guarantee that fiber will remain the current one
    // as it could switch to another Fiber before the actual operation on the returned fiber.
    DynamicObject getCurrentFiberRacy();

    DynamicObject getRubyFiberFromCurrentJavaThread();

    DynamicObject createFiber(RubyContext context, DynamicObject thread, DynamicObjectFactory factory, String name);

    void initialize(DynamicObject fiber, DynamicObject block, Node currentNode);

    DynamicObject getReturnFiber(DynamicObject currentFiber, Node currentNode, BranchProfile errorProfile);

    Object[] transferControlTo(DynamicObject fromFiber, DynamicObject fiber, FiberOperation operation, Object[] args);

    void start(DynamicObject fiber, Thread javaThread);

    void cleanup(DynamicObject fiber, Thread javaThread);

    void killOtherFibers();

    void shutdown(Thread javaThread);

    String getFiberDebugInfo();

}