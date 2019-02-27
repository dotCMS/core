package com.dotcms.business;

@FunctionalInterface
public interface ParameterDecorator {
    static ParameterDecorator DEFAULT = arg -> arg;

    /**
     * Decorates the arguments
     * @param arguments {@link Object} array
     * @return Object array
     */
    Object[] decorate (final Object[] arguments);
}
