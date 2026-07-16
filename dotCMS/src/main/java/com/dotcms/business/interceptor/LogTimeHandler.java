package com.dotcms.business.interceptor;

/**
 * Shared handler for {@code @LogTime} logic. Used by both the ByteBuddy advice and the
 * CDI interceptor to keep the implementation DRY.
 */
public final class LogTimeHandler {

    private LogTimeHandler() { }

    /**
     * Logs method execution time at the specified level.
     *
     * @param clazz        the declaring class of the method
     * @param methodName   the method name
     * @param durationMs   the execution duration in milliseconds
     * @param loggingLevel "INFO" or "DEBUG"
     */
    public static void logTime(final Class<?> clazz, final String methodName,
                               final long durationMs, final String loggingLevel) {
        final String message = "Call for class: " + clazz.getName() + "#"
                + methodName + ", duration:" + durationMs + " millis";
        final InterceptorLogger logger = InterceptorServiceProvider.getLogger();
        if ("INFO".equals(loggingLevel)) {
            logger.info(clazz, message);
        } else {
            logger.debug(clazz, message);
        }
    }
}
