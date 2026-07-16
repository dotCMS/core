package com.dotcms.business.interceptor;

import com.dotmarketing.util.Logger;

/**
 * Core implementation of {@link InterceptorLogger} that delegates to the dotCMS
 * {@link Logger}. This class stays in the core module when the SPI interfaces and handlers
 * are extracted to a utility module.
 */
public final class CoreInterceptorLogger implements InterceptorLogger {

    public static final CoreInterceptorLogger INSTANCE = new CoreInterceptorLogger();

    private CoreInterceptorLogger() { }

    @Override
    public void info(final Class<?> clazz, final String message) {
        Logger.info(clazz, message);
    }

    @Override
    public void debug(final Class<?> clazz, final String message) {
        Logger.debug(clazz, message);
    }

    @Override
    public void warn(final Class<?> clazz, final String message) {
        Logger.warn(clazz, message);
    }

    @Override
    public void error(final Class<?> clazz, final String message, final Throwable t) {
        Logger.error(clazz, message, t);
    }
}
