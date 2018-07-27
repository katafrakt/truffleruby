package org.truffleruby.core.fiber;

import org.truffleruby.RubyContext;

import com.oracle.truffle.api.object.DynamicObject;

public class FiberManagerFactory {
    private static final boolean USE_CONTINUATIONS;

    static {
        boolean tmp;
        try {
            Class.forName("java.lang.Continuation");
            tmp = true;
        } catch (ClassNotFoundException e) {
            tmp = false;
        }
        USE_CONTINUATIONS = tmp;
    }

    public static FiberManager create(RubyContext context, DynamicObject rubyThread) {
        if (USE_CONTINUATIONS) {
            return new FiberManagerContinuationImpl(context, rubyThread);
        } else {
            return new FiberManagerThreadImpl(context, rubyThread);
        }
    }
}
