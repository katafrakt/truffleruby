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
