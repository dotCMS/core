package com.dotcms.business;

/**
 * Defines a posthook for the {@link MethodHook}
 * @author jsanca
 */
@FunctionalInterface
public interface PostHook {

    /**
     * After the method invoke, this hook if present will be called, with the argument and optional result (if the method  is void null be null)
     * You can run any post condition or even decorate the result and return it again
     * @param arguments {@link Object} array
     * @param result  {@link Object} method result (if any)
     * @return Tuple2: boolean true if want to call the method, Object array decorated arguments
     */
    Object post(final Object[] arguments, final Object result);
}
