package com.dotcms.rendering.velocity.events;

/**
 * Exception handler for velocity context
 * @param <T>
 */
@FunctionalInterface
public interface ExceptionHandler<T extends Throwable> {

    /**
     * Handles the exception
     * @param exception
     */
    void handle (T exception);

}
