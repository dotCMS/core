package com.dotmarketing.db;


import com.dotcms.repackage.net.sf.hibernate.Session;
import com.dotcms.util.ReturnableDelegate;
import com.dotcms.util.VoidDelegate;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.microsoft.sqlserver.jdbc.ISQLServerConnection;
import io.vavr.control.Try;

import java.sql.Connection;
import java.util.function.Function;

public class LocalTransaction {

    private enum TransactionErrorEnum {
        NOTHING,
        LOG,
        THROW
    }


    private final static String WARN_MESSAGE = "Transaction broken - Connection that started the transaction is not the same as the one who is commiting";

    /**
     * @param delegate {@link ReturnableDelegate}
     * @return T result of the {@link ReturnableDelegate}
     * @throws DotDataException This class can be used to wrap methods in a "externalized
     *                          transaction" pattern including the listeners (commit and rollback
     *                          listeners) this pattern will use a new connection (if the parent
     *                          caller is already in a transaction, that transaction will be
     *                          restored after this method gets done) If the SQL call fails, it will
     *                          rollback the work, close  the db connection and throw the error up
     *                          the stack.
     *                          <p>
     *                          Since it uses a new connection, that one will be closed at the end
     *                          of the transaction.
     *                          <p>
     *                          How to use:
     *                          <p>
     *                          return new LocalTransaction().externalizeTransaction(() ->{ return
     *                          myDBMethod(args); });
     */
    static public <T> T externalizeTransaction(final ReturnableDelegate<T> delegate)
            throws Exception {
        T result = null;
        // gets the current conn
        final Connection currentConnection = DbConnectionFactory.getConnection();
        final Session currentSession = HibernateUtil.getSession();
        // creates a new one
        final Connection newTransactionConnection = DbConnectionFactory.getDataSource()
                .getConnection();

        // overrides the current thread
        DbConnectionFactory.setConnection(newTransactionConnection);
        final Session newSession = HibernateUtil.createNewSession(newTransactionConnection);
        HibernateUtil.setSession(newSession);

        HibernateUtil.startTransaction();

        try {
            final StackTraceElement[] threadStack = Thread.currentThread().getStackTrace();
            result = delegate.execute();
            handleTransactionInteruption(newTransactionConnection, threadStack);
            HibernateUtil.commitTransaction();
        } catch (Throwable e) {

            HibernateUtil.rollbackTransaction();
            throwException(e);
        } finally {

            HibernateUtil.closeSessionSilently();
            // return the previous conn, if needed
            HibernateUtil.setSession(currentSession);
            DbConnectionFactory.setConnection(currentConnection);
        }
        return result;
    } // transaction.

    /**
     * @param delegate {@link ReturnableDelegate}
     * @return T result of the {@link ReturnableDelegate}
     * @throws DotDataException
     * @deprecated use wrapReturn All commits need to handle listeners or there will be unknown or
     * unintended side effects
     */
    @Deprecated
    public static <T> T wrapReturnWithListeners(final ReturnableDelegate<T> delegate)
            throws Exception {
        return wrapReturn(delegate);
    } // wrapReturn.


    /**
     * @param delegate {@link ReturnableDelegate}
     * @return T result of the {@link ReturnableDelegate}
     * @throws DotDataException This class can be used to wrap methods in a "local transaction"
     *                          pattern this pattern will check to see if the method is being called
     *                          in an existing transaction. if it is being called in a transaction,
     *                          it will do nothing.  If it is not being called in a transaction it
     *                          will checkout a db connection,start a transaction, do the work,
     *                          commit the transaction, return the result and  finally close the db
     *                          connection.  If the SQL call fails, it will rollback the work, close
     *                          the db connection and throw the error up the stack.
     *                          <p>
     *                          How to use:
     *                          <p>
     *                          return new LocalTransaction().wrapReturn(() ->{ return
     *                          myDBMethod(args); });
     */
    public static <T> T wrapReturn(final ReturnableDelegate<T> delegate) throws Exception {

        final boolean isNewConnection = !DbConnectionFactory.connectionExists();
        final boolean isLocalTransaction = HibernateUtil.startLocalTransactionIfNeeded();

        T result = null;

        try {
            final StackTraceElement[] threadStack = Thread.currentThread().getStackTrace();
            final Connection conn = DbConnectionFactory.getConnection();
            result = delegate.execute();
            if (isLocalTransaction) {
                handleTransactionInteruption(conn, threadStack);
                HibernateUtil.commitTransaction();
            }
        } catch (Throwable e) {
            handleException(isLocalTransaction, e);
        } finally {

            if (isLocalTransaction) {
                DbConnectionFactory.setAutoCommit(true);
                if (isNewConnection) {
                    DbConnectionFactory.closeConnection();
                }
            }
        }

        return result;
    } // wrapReturn.

    /**
     * @param delegate {@link VoidDelegate}
     * @throws DotDataException this will accept a method that does not need to return anything and
     *                          wrap it in a transaction if it is not in one already.  At the end of
     *                          the call, it will return the db connection to the connection pool
     *                          <p>
     *                          *  How to use:
     *                          <p>
     *                          new LocalTransaction().wrap(() ->{ myDBMethod(args); return null;
     *                          });
     */
    public static void wrap(final VoidDelegate delegate)
            throws DotDataException, DotSecurityException {

        final boolean isNewConnection = !DbConnectionFactory.connectionExists();
        final boolean isLocalTransaction = HibernateUtil.startLocalTransactionIfNeeded();

        try {

            final StackTraceElement[] threadStack = Thread.currentThread().getStackTrace();
            final Connection conn = DbConnectionFactory.getConnection();
            delegate.execute();

            if (isLocalTransaction) {
                handleTransactionInteruption(conn, threadStack);
                HibernateUtil.commitTransaction();
            }
        } catch (Exception e) {
            handleException(isLocalTransaction, e);
        } finally {

            if (isLocalTransaction) {

                DbConnectionFactory.setAutoCommit(true);
                if (isNewConnection) {
                    DbConnectionFactory.closeConnection();
                }
            }
        }
    } // wrap.

    @SuppressWarnings("unchecked")
    static <T extends Throwable, R> R sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    public interface ThrowingFunction<T, R> {

        @SuppressWarnings("java:S112")
        R apply(T t) throws Exception;
    }


    public static <T> Function<T, T> unchecked(ReturnableDelegate<T> f) {
        return t -> {
            try {
                return f.execute();
            } catch (Throwable ex) {
                return sneakyThrow(ex);
            }
        };
    }


    public static VoidDelegate unchecked(VoidDelegate t) {
        return () -> {
            try {
                t.execute();
            } catch (Exception ex) {
                sneakyThrow(ex);
            }
        };
    }


    private static <T extends Throwable> void handleException(final boolean isLocalTransaction,
            final Throwable t) throws T, DotDataException {
        if (isLocalTransaction) {
            HibernateUtil.rollbackTransaction();
        }
        throw (T) t;
    } // handleException.


    public static void handleTransactionInteruption(final Connection conn,
            final StackTraceElement[] threadStack) throws DotDataException {
        if (DbConnectionFactory.getConnection() != conn) {
            final String action = Config.getStringProperty("LOCAL_TRANSACTION_INTERUPTED_ACTION",
                    TransactionErrorEnum.LOG.name());
            if (TransactionErrorEnum.LOG.name().equalsIgnoreCase(action)) {

                Logger.warn(LocalTransaction.class, WARN_MESSAGE);
                for (StackTraceElement ste : threadStack) {
                    Logger.warn(LocalTransaction.class, "    " + ste.toString());
                }
            } else if (TransactionErrorEnum.THROW.name().equalsIgnoreCase(action)) {
                throw new DotDataException(WARN_MESSAGE);
            }
        }
    }


    public static void throwException(final Throwable e) throws Exception {

        if (e instanceof Exception) {

            throw (Exception) e;
        }

        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        if (t instanceof DotDataException) {
            throw (DotDataException) t;
        }
        throw new DotDataException(t.getMessage(), t);
    }


    private static final String LOCAL_TRANSACTION_NAME = LocalTransaction.class.getCanonicalName();

    static public boolean inLocalTransaction() {
        final StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        for (int i = 2; i < stes.length; i++) {
            final int stackNumber = i;
            String steName = Try.of(() -> stes[stackNumber].getClassName()).getOrNull();
            if (LOCAL_TRANSACTION_NAME.equals(steName)) {
                return true;
            }
        }
        return false;
    }

} // E:O:F:LocalTransaction.
