package com.dotcms.business.interceptor;

/**
 * Shared handler for {@code @CloseDBIfOpened} logic. Used by both the ByteBuddy advice and
 * the CDI interceptor to keep the implementation DRY.
 */
public final class CloseDBIfOpenedHandler {

    private CloseDBIfOpenedHandler() { }

    /**
     * Called before the annotated method executes.
     *
     * @return {@code true} if no connection existed (i.e. a new one will be created)
     */
    public static boolean onEnter() {
        return !InterceptorServiceProvider.getDatabaseOps().connectionExists();
    }

    /**
     * Called after the annotated method completes (normally or exceptionally).
     *
     * @param isNewConnection whether the connection was new (from {@link #onEnter()})
     * @param connectionParam the {@code connection} attribute from the annotation
     */
    public static void onExit(final boolean isNewConnection, final boolean connectionParam) {
        if (connectionParam && isNewConnection) {
            InterceptorServiceProvider.getDatabaseOps().closeSilently();
        }
    }
}
