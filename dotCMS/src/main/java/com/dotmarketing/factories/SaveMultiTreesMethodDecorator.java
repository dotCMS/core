package com.dotmarketing.factories;

import com.dotmarketing.beans.MultiTree;

import java.util.List;

public class SaveMultiTreesMethodDecorator extends SaveMultiTreeMethodDecorator {

    @Override
    public Object[] decorate(final Object[] arguments) {

        final Object [] newArguments = new Object[arguments.length];

        for (int i=0; i < arguments.length; ++i) {

            if (arguments[i] instanceof List) {

                final List<MultiTree> multiTrees   = (List<MultiTree>)arguments[i];
                for (final MultiTree multiTree : multiTrees) {

                    this.getMultiTreeArgument(multiTree);
                }

                newArguments[i] = multiTrees;
            } else {
                newArguments[i] = arguments[i];
            }
        }

        return newArguments;
    }
}
