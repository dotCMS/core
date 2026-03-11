package com.dotcms.business.interceptor;

import java.sql.Connection;

/**
 * Shared handler for {@code @WrapInTransaction} logic. Used by both the ByteBuddy advice
 * and the CDI interceptor to keep the implementation DRY.
 */
public final class WrapInTransactionHandler {

    private WrapInTransactionHandler() { }

    /**
     * State captured on method entry; passed to {@link #onSuccess} or {@link #onError} and
     * finally to {@link #onFinally}.
     */
    public static final class TransactionState {
        public final boolean isNewConnection;
        public final boolean isLocalTransaction;
        public final Connection connection;

        public TransactionState(boolean isNewConnection, boolean isLocalTransaction,
                                Connection connection) {
            this.isNewConnection = isNewConnection;
            this.isLocalTransaction = isLocalTransaction;
            this.connection = connection;
        }
    }

    /**
     * Called before the annotated method. Opens a connection and starts a local transaction
     * if needed.
     *
     * @return state to be passed to exit methods, or {@code null} on failure (after re-throw)
     */
    public static TransactionState onEnter() throws Exception {
        final DatabaseConnectionOps dbOps = InterceptorServiceProvider.getDatabaseOps();
        final TransactionOps txOps = InterceptorServiceProvider.getTransactionOps();

        boolean isNewConnection = false;
        boolean isLocalTransaction = false;
        try {
            isNewConnection = !dbOps.connectionExists();
            isLocalTransaction = txOps.startLocalTransactionIfNeeded();
            final Connection conn = dbOps.getConnection();
            return new TransactionState(isNewConnection, isLocalTransaction, conn);
        } catch (Throwable e) {
            if (isLocalTransaction) {
                txOps.rollbackTransaction();
            }
            if (isNewConnection) {
                txOps.closeSessionSilently();
            }
            txOps.throwException(e);
            return null; // unreachable
        }
    }

    /**
     * Called when the annotated method completes successfully.
     */
    public static void onSuccess(final TransactionState state) throws Exception {
        if (state != null && state.isLocalTransaction) {
            final TransactionOps txOps = InterceptorServiceProvider.getTransactionOps();
            try {
                txOps.handleTransactionInterruption(state.connection, null);
                txOps.commitTransaction();
            } catch (Throwable e) {
                txOps.rollbackTransaction();
                txOps.throwException(e);
            }
        }
    }

    /**
     * Called when the annotated method throws.
     */
    public static void onError(final TransactionState state, final Throwable thrown) throws Exception {
        if (state != null && state.isLocalTransaction) {
            InterceptorServiceProvider.getTransactionOps().rollbackTransaction();
        }
        InterceptorServiceProvider.getTransactionOps().throwException(thrown);
    }

    /**
     * Called in the finally block to clean up the connection if it was new.
     */
    public static void onFinally(final TransactionState state) {
        if (state != null && state.isNewConnection) {
            InterceptorServiceProvider.getTransactionOps().closeSessionSilently();
        }
    }
}
