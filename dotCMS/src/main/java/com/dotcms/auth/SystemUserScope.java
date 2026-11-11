package com.dotcms.auth;

/**
 * Thread-bound, server-side scope during which identity resolution answers with the built-in system user
 * without consulting the request. Stub for the Red gate: no behavior yet.
 */
public final class SystemUserScope implements AutoCloseable {

    private SystemUserScope() {
    }

    public static SystemUserScope open() {
        return new SystemUserScope();
    }

    /**
     * Returns a task that, when run on any thread, runs inside a scope if one was active when this method
     * was called. Stub for the Red gate: returns the task unchanged.
     */
    public static Runnable wrap(final Runnable task) {
        return task;
    }

    /**
     * Callable variant of {@link #wrap(Runnable)}. Stub for the Red gate: returns the task unchanged.
     */
    public static <T> java.util.concurrent.Callable<T> wrap(final java.util.concurrent.Callable<T> task) {
        return task;
    }

    public static boolean isActive() {
        return false;
    }

    @Override
    public void close() {
    }
}
