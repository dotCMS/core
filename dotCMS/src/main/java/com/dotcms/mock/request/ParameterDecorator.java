package com.dotcms.mock.request;

/**
 * Decorates a request parameter
 * @author jsanca
 */
public interface ParameterDecorator {

    /**
     * Get the parameter key for what this decorator applies
     * @return String
     */
    String key();

    /**
     * Decorates the parameter value
     * @param parameterValue String the parameter value to decorate
     * @return String
     */
    String decorate(String parameterValue);
}
