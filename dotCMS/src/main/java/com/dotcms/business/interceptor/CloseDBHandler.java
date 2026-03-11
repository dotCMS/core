package com.dotcms.business.interceptor;

/**
 * Shared handler for {@code @CloseDB} logic. Used by both the ByteBuddy advice and
 * the CDI interceptor to keep the implementation DRY.
 */
public final class CloseDBHandler {

    private CloseDBHandler() { }

    /**
     * Called after the annotated method completes. Commits and closes the connection.
     */
    public static void onExit() {
        try {
            InterceptorServiceProvider.getDatabaseOps().closeAndCommit();
        } finally {
            InterceptorServiceProvider.getDatabaseOps().closeSilently();
        }
    }
}
