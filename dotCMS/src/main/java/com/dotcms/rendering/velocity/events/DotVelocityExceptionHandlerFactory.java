package com.dotcms.rendering.velocity.events;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Velocity exception handler
 * @author jsanca
 */
public class DotVelocityExceptionHandlerFactory {

    private final static Map<Class, ExceptionHandler> handlersMap = new ConcurrentHashMap<>();

    public static <T extends Throwable> void register (final Class<T> tClass, final ExceptionHandler<T> handler) {

        handlersMap.put(tClass, handler);
    }

    public static <T extends Throwable> Optional<ExceptionHandler<T>> get (final Throwable exception) {

        return get(exception.getClass());
    }

    public static <T extends Throwable> Optional<ExceptionHandler<T>> get (final Class exceptionClass) {

        final ExceptionHandler<T> handler = handlersMap.get(exceptionClass);

        return null == handler? Optional.empty(): Optional.of(handler);
    }

}
