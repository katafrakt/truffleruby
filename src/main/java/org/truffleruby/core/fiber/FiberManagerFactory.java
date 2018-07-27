package org.truffleruby.core.fiber;

import org.truffleruby.RubyContext;

import com.oracle.truffle.api.object.DynamicObject;

public class FiberManagerFactory {
    private static final boolean USE_CONTINUATIONS;

    static {
        USE_CONTINUATIONS = false;
    }

    public static FiberManager create(RubyContext context, DynamicObject rubyThread) {
        if (USE_CONTINUATIONS) {
            throw new Error("Continuations not done yet.");
        } else {
            return new FiberManagerThreadImpl(context, rubyThread);
        }
    }
}
