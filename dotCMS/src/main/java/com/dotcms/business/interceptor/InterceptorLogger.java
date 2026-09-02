package com.dotcms.business.interceptor;

/**
 * Abstraction over logging. Decouples the interceptor/advice layer from the dotCMS
 * {@code Logger} so that the annotations and handlers can eventually live in a utility
 * module that uses standard logging.
 */
public interface InterceptorLogger {

    void info(Class<?> clazz, String message);

    void debug(Class<?> clazz, String message);

    void warn(Class<?> clazz, String message);

    void error(Class<?> clazz, String message, Throwable t);
}
