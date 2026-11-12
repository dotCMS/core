package com.dotcms.auth;

import com.dotmarketing.business.UserAPI;
import com.liferay.portal.model.User;
import java.util.concurrent.Callable;

/**
 * Thread-bound, server-side scope during which identity resolution answers with the built-in system user
 * without consulting the request.
 * <p>
 * It is for server-side work only: nothing a request, a session or a template carries can open it. It is
 * tied to the thread that opened it; to run work on another thread use {@link #wrap(Runnable)} or
 * {@link #wrap(Callable)} at the point where the work is handed over.
 */
public final class SystemUserScope implements AutoCloseable {

    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private final boolean opened;

    private SystemUserScope(final boolean opened) {
        this.opened = opened;
    }

    /**
     * Opens a scope on the current thread. Scopes nest; the outermost one ends it.
     *
     * @return the scope, to be closed in a try-with-resources
     */
    public static SystemUserScope open() {
        DEPTH.set(DEPTH.get() + 1);
        return new SystemUserScope(true);
    }

    /**
     * Opens a scope only when the acting user is the built-in system user; for any other user the returned
     * object does nothing, so a call site can use one try-with-resources whoever the user is.
     *
     * @param actingUser the user server code is acting as, may be null
     * @return a scope to be closed in a try-with-resources
     */
    public static SystemUserScope openIfSystemUser(final User actingUser) {
        if (actingUser != null
                && UserAPI.SYSTEM_USER_ID.equalsIgnoreCase(actingUser.getUserId())) {
            return open();
        }
        return new SystemUserScope(false);
    }

    /**
     * @return true while a scope is open on the current thread
     */
    public static boolean isActive() {
        return DEPTH.get() > 0;
    }

    /**
     * Returns a task that runs inside a scope on whichever thread executes it, if a scope is active on the
     * thread that calls this method; otherwise the task is returned unchanged.
     *
     * @param task the work to hand to another thread
     * @return the task to hand over
     */
    public static Runnable wrap(final Runnable task) {
        if (!isActive()) {
            return task;
        }
        return () -> {
            try (SystemUserScope ignored = open()) {
                task.run();
            }
        };
    }

    /**
     * Callable variant of {@link #wrap(Runnable)}.
     *
     * @param task the work to hand to another thread
     * @param <T>  the result type
     * @return the task to hand over
     */
    public static <T> Callable<T> wrap(final Callable<T> task) {
        if (!isActive()) {
            return task;
        }
        return () -> {
            try (SystemUserScope ignored = open()) {
                return task.call();
            }
        };
    }

    @Override
    public void close() {
        if (!opened) {
            return;
        }
        final int depth = DEPTH.get() - 1;
        if (depth <= 0) {
            DEPTH.remove();
        } else {
            DEPTH.set(depth);
        }
    }
}
