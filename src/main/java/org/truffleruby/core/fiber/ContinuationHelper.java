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

public class ContinuationHelper {

    // private static final ContinuationScope rubyFiberScope = new ContinuationScope() { };

    protected static void safeRun(Object continuation) {
        // ((Continuation)continuation).run();
        throw new Error();
    }

    protected static void safeYield() {
        // Continuation.yield(rubyFiberScope);
        throw new Error();
    }

    public static Object newContinuation(Runnable runnable) {
        // return new Continuation(rubyFiberScope, runnable);
        throw new Error();
    }

}
