/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.fiber;

import org.truffleruby.RubyContext;
import org.truffleruby.language.RubyGuards;
import org.truffleruby.language.control.TerminationException;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.profiles.BranchProfile;

public interface FiberManager {

    interface FiberMessage {
    }

    class FiberResumeMessage implements FiberMessage {
    
        private final FiberOperation operation;
        private final DynamicObject sendingFiber;
        private final Object[] args;
    
        public FiberResumeMessage(FiberOperation operation, DynamicObject sendingFiber, Object[] args) {
            assert RubyGuards.isRubyFiber(sendingFiber);
            this.operation = operation;
            this.sendingFiber = sendingFiber;
            this.args = args;
        }
    
        public FiberOperation getOperation() {
            return operation;
        }
    
        public DynamicObject getSendingFiber() {
            return sendingFiber;
        }
    
        public Object[] getArgs() {
            return args;
        }
    
    }

    /**
     * Used to cleanup and terminate Fibers when the parent Thread dies.
     */
    class FiberShutdownException extends TerminationException {
        private static final long serialVersionUID = 1522270454305076317L;
    }

    class FiberShutdownMessage implements FiberMessage {
    }

    class FiberExceptionMessage implements FiberMessage {
    
        private final RuntimeException exception;
    
        public FiberExceptionMessage(RuntimeException exception) {
            this.exception = exception;
        }
    
        public RuntimeException getException() {
            return exception;
        }
    
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
