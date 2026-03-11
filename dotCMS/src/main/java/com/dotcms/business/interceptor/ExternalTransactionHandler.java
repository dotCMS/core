package com.dotcms.business.interceptor;

import java.sql.Connection;

/**
 * Shared handler for {@code @ExternalTransaction} logic. Used by both the ByteBuddy advice
 * and the CDI interceptor to keep the implementation DRY.
 */
public final class ExternalTransactionHandler {

    private ExternalTransactionHandler() { }

    /**
     * State captured on method entry; preserves the original connection/session so they
     * can be restored after the external transaction completes.
     */
    public static final class ExternalTransactionState {
        public final Connection newTransactionConnection;
        public final Connection currentConnection;
        public final Object currentSession;
        public final StackTraceElement[] threadStack;

        public ExternalTransactionState(Connection newTransactionConnection,
                                        Connection currentConnection,
                                        Object currentSession,
                                        StackTraceElement[] threadStack) {
            this.newTransactionConnection = newTransactionConnection;
            this.currentConnection = currentConnection;
            this.currentSession = currentSession;
            this.threadStack = threadStack;
        }
    }

    /**
     * Called before the annotated method. Saves the current connection/session, creates a
     * new connection, and starts a transaction on it.
     */
    public static ExternalTransactionState onEnter() throws Exception {
        final DatabaseConnectionOps dbOps = InterceptorServiceProvider.getDatabaseOps();
        final TransactionOps txOps = InterceptorServiceProvider.getTransactionOps();

        try {
            final Connection currentConnection = dbOps.getConnection();
            final Object currentSession = txOps.getSession();

            final Connection newConn = dbOps.newConnection();
            dbOps.setConnection(newConn);

            final Object newSession = txOps.createNewSession(newConn);
            txOps.setSession(newSession);
            txOps.startTransaction();

            final StackTraceElement[] threadStack = Thread.currentThread().getStackTrace();
            return new ExternalTransactionState(newConn, currentConnection,
                    currentSession, threadStack);
        } catch (Throwable e) {
            txOps.rollbackTransaction();
            txOps.closeSessionSilently();
            txOps.throwException(e);
            return null; // unreachable
        }
    }

    /**
     * Called when the annotated method completes successfully.
     */
    public static void onSuccess(final ExternalTransactionState state) throws Exception {
        if (state != null) {
            final TransactionOps txOps = InterceptorServiceProvider.getTransactionOps();
            try {
                txOps.handleTransactionInterruption(state.newTransactionConnection, state.threadStack);
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
    public static void onError(final ExternalTransactionState state, final Throwable thrown) throws Exception {
        InterceptorServiceProvider.getTransactionOps().rollbackTransaction();
        InterceptorServiceProvider.getTransactionOps().throwException(thrown);
    }

    /**
     * Called in the finally block to close the external session and restore the original
     * connection/session.
     */
    public static void onFinally(final ExternalTransactionState state) {
        if (state != null) {
            final TransactionOps txOps = InterceptorServiceProvider.getTransactionOps();
            final DatabaseConnectionOps dbOps = InterceptorServiceProvider.getDatabaseOps();
            txOps.closeSessionSilently();
            txOps.setSession(state.currentSession);
            dbOps.setConnection(state.currentConnection);
        }
    }
}
