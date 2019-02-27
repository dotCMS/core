package com.dotcms.business;

@FunctionalInterface
public interface ParameterDecorator {

    /**
     * Decorates the arguments
     * @param arguments {@link Object} array
     * @return Object array
     */
    Object[] decorate (final Object[] arguments);
}
