package com.dotmarketing.util;

import com.dotmarketing.exception.DotRuntimeException;
import io.vavr.control.Try;

import java.util.Optional;

/**
 * This utility class exposes useful methods to interact with threads in dotCMS.
 *
 * @author root
 * @since Mar 22nd, 2012
 */
public final class ThreadUtils {

    private ThreadUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void sleep(final long sleepTime) {

        Try.run(() -> Thread.sleep(sleepTime)).onFailure(DotRuntimeException::new);

    }


    public static Thread getThread(final String name) {

        if (name == null) {
            return null;
        }


        Optional<Thread> thread = Thread.getAllStackTraces().keySet().parallelStream()
                        .filter(t -> t.getName().equalsIgnoreCase(name)).findFirst();


        return thread.isPresent() ? thread.get() : null;
    }

    /**
     * Checks the current stack trace and counts the number of times that a specific method in a
     * class is being called.
     *
     * @param className  The name of the class that counts the target method.
     * @param methodName The name of the target method whose call count we need to check.
     * @param maxCount   The maximum number of times the target method is allowed to be called.
     *
     * @return If the method is present in the call stack at least {@code maxCount} times, returns
     * {@code true}.
     */
    public static boolean isMethodCallCountEqualThan(final String className,
                                                     final String methodName, final int maxCount) {
        final StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        int counter = 0;
        for (final StackTraceElement trace : traces) {
            if (trace.getClassName().equals(className) && trace.getMethodName().equals(methodName)) {
                counter++;
                if (counter == maxCount) {
                    return true;
                }
            }
        }
        return false;
    }

}
