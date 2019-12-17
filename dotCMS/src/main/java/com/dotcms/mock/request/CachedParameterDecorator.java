package com.dotcms.mock.request;

/**
 * Just a wrapper of a Decorator to cache the value instead requesting everytime
 * @author jsanca
 */
public class CachedParameterDecorator implements ParameterDecorator {

    private final ParameterDecorator decorator;
    private volatile String value;

    public CachedParameterDecorator(final ParameterDecorator decorator) {
        this.decorator = decorator;
    }

    @Override
    public String key() {
        return this.decorator.key();
    }

    @Override
    public String decorate(final String parameterValue) {

        if (null == value) {

            value = decorator.decorate(parameterValue);
        }

        return value;
    }
}
