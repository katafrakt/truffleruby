/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.language.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.truffleruby.language.RubyNode;
import org.truffleruby.language.control.RaiseException;

public class MissingKeywordArgumentNode extends RubyNode {

    private final String name;

    public MissingKeywordArgumentNode(String name) {
        this.name = name;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new RaiseException(getContext(), coreExceptions().argumentErrorMissingKeyword(name, this));
    }

}
