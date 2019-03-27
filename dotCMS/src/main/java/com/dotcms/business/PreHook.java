package com.dotcms.business;

import io.vavr.Tuple2;

/**
 * Defines a prehook for the {@link MethodHook}
 * @author jsanca
 */
@FunctionalInterface
public interface PreHook {

    /**
     * Executes any pre hook over the method, it  can avoid the actual method  call by passing
     * first argument on false.
     * Also can decorate the argument by passing something different on the second tuple arguments, for instance you can do somethig like
     * Tuple.of (Boolean.FALSE, new Object []{})
     * If you want to avoid the actual method call.
     *
     * @param arguments {@link Object} array
     * @return Tuple2: boolean true if want to call the method, Object array decorated arguments
     */
    Tuple2<Boolean,Object[]> pre (final Object[] arguments);
}
