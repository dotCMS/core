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
        public final StackTraceElement[] threadStack;

        public TransactionState(boolean isNewConnection, boolean isLocalTransaction,
                                Connection connection, StackTraceElement[] threadStack) {
            this.isNewConnection = isNewConnection;
            this.isLocalTransaction = isLocalTransaction;
            this.connection = connection;
            this.threadStack = threadStack;
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
            final StackTraceElement[] threadStack = Thread.currentThread().getStackTrace();
            final Connection conn = dbOps.getConnection();
            return new TransactionState(isNewConnection, isLocalTransaction, conn, threadStack);
        } catch (Throwable e) {
            if (isLocalTransaction) {
                txOps.rollbackTransaction();
                dbOps.setAutoCommit(true);
            }
            if (isNewConnection) {
                txOps.closeSessionSilently();
                dbOps.closeConnection();
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
                txOps.handleTransactionInterruption(state.connection, state.threadStack);
                txOps.commitTransaction();
            } catch (Throwable e) {
                txOps.rollbackTransaction();
                txOps.throwException(e);
            }
        }
    }

    /**
     * Called when the annotated method throws. Rolls back the transaction if it was locally
     * started. The caller is responsible for rethrowing the original exception.
     */
    public static void onError(final TransactionState state) {
        if (state != null && state.isLocalTransaction) {
            InterceptorServiceProvider.getTransactionOps().rollbackTransaction();
        }
    }

    /**
     * Called in the finally block to reset autocommit and close the connection if it was new.
     */
    public static void onFinally(final TransactionState state) {
        if (state != null && state.isLocalTransaction) {
            InterceptorServiceProvider.getDatabaseOps().setAutoCommit(true);
            if (state.isNewConnection) {
                InterceptorServiceProvider.getDatabaseOps().closeConnection();
            }
        }
    }

    // ---- Lambda convenience methods (used by LocalTransaction) ----

    /**
     * Functional interface for operations that return a value and may throw.
     * Equivalent to {@code ReturnableDelegate} but with no core dependency.
     */
    @FunctionalInterface
    public interface ThrowableSupplier<T> {
        T execute() throws Throwable;
    }

    /**
     * Functional interface for void operations that may throw.
     */
    @FunctionalInterface
    public interface ThrowableRunnable {
        void execute() throws Throwable;
    }

    /**
     * Wraps a lambda in a local transaction, matching the semantics of
     * {@code LocalTransaction.wrapReturn()}. If not already in a transaction, starts one,
     * executes the delegate, commits, and cleans up. If already in a transaction, just
     * executes the delegate.
     *
     * <p>Uses {@code setAutoCommit(true)} + {@code closeConnection()} for cleanup
     * (matching the original LocalTransaction behavior).</p>
     *
     * @param delegate the operation to execute
     * @return the result of the operation
     */
    public static <T> T wrapReturn(final ThrowableSupplier<T> delegate) throws Exception {
        final DatabaseConnectionOps dbOps = InterceptorServiceProvider.getDatabaseOps();
        final TransactionOps txOps = InterceptorServiceProvider.getTransactionOps();

        final boolean isNewConnection = !dbOps.connectionExists();
        final boolean isLocalTransaction = txOps.startLocalTransactionIfNeeded();

        T result = null;

        try {
            final StackTraceElement[] threadStack = Thread.currentThread().getStackTrace();
            final Connection conn = dbOps.getConnection();
            result = delegate.execute();
            if (isLocalTransaction) {
                txOps.handleTransactionInterruption(conn, threadStack);
                txOps.commitTransaction();
            }
        } catch (Throwable e) {
            if (isLocalTransaction) {
                txOps.rollbackTransaction();
            }
            // Direct rethrow — preserves original LocalTransaction.wrapReturn semantics
            if (e instanceof Exception) {
                throw (Exception) e;
            }
            throw new RuntimeException(e);
        } finally {
            if (isLocalTransaction) {
                dbOps.setAutoCommit(true);
                if (isNewConnection) {
                    dbOps.closeConnection();
                }
            }
        }

        return result;
    }

    /**
     * Void version of {@link #wrapReturn(ThrowableSupplier)}, matching the semantics of
     * {@code LocalTransaction.wrap()}.
     */
    public static void wrap(final ThrowableRunnable delegate) throws Exception {
        wrapReturn(() -> {
            delegate.execute();
            return null;
        });
    }
}
