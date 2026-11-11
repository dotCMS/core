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

    public static boolean isActive() {
        return false;
    }

    @Override
    public void close() {
    }
}
