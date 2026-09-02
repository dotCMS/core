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

    // ---- Lambda convenience method (used by DbConnectionFactory.wrapConnection) ----

    /**
     * Wraps a lambda with connection cleanup. If no connection existed before the call,
     * closes it silently afterward. Preserves thread interrupt status during cleanup.
     *
     * @param delegate the operation to execute
     * @return the result of the operation
     */
    public static <T> T wrapConnection(
            final WrapInTransactionHandler.ThrowableSupplier<T> delegate) throws Exception {
        final DatabaseConnectionOps dbOps = InterceptorServiceProvider.getDatabaseOps();
        final boolean isNewConnection = !dbOps.connectionExists();

        try {
            return delegate.execute();
        } catch (Throwable e) {
            if (e instanceof Exception) {
                throw (Exception) e;
            }
            throw new RuntimeException(e);
        } finally {
            if (isNewConnection && dbOps.connectionExists()) {
                // Preserve interrupted status but ensure connection cleanup completes
                final boolean wasInterrupted = Thread.interrupted();
                try {
                    dbOps.closeSilently();
                } finally {
                    if (wasInterrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
}
