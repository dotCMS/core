package com.dotmarketing.db;

import com.dotcms.util.ReturnableDelegate;
import com.dotcms.util.VoidDelegate;
import com.dotmarketing.exception.DotDataException;

public class LocalTransaction {

    /**
     *
     * @param delegate {@link ReturnableDelegate}
     * @return T result of the {@link ReturnableDelegate}
     * @throws DotDataException
     *
     * This class can be used to wrap methods in a "local transaction" pattern including the listeners (commit and rollback listeners)
     * this pattern will check to see if the method is being called in an existing transaction.
     * if it is being called in a transaction, it will do nothing.  If it is not being called in a transaction
     * it will checkout a db connection,start a transaction, do the work, commit the transaction, return the result
     * and  finally close the db connection.  If the SQL call fails, it will rollback the work, close  the db connection
     * and throw the error up the stack.
     *
     * In addition the connection will be closed, only if the current transaction is opening it.
     *
     *  How to use:
     *
     *	return new LocalTransaction().wrapReturnWithListeners(() ->{
     *		return myDBMethod(args);
     *  });
     */
    static public <T> T wrapReturnWithListeners(final ReturnableDelegate<T> delegate) throws Exception {

        final boolean isNewConnection    = !DbConnectionFactory.connectionExists();
        final boolean isLocalTransaction = HibernateUtil.startLocalTransactionIfNeeded();

        T result = null;

        try {

            result= delegate.execute();
            if (isLocalTransaction) {
                HibernateUtil.commitTransaction();
            }
        } catch (Throwable e) {

            if (isLocalTransaction) {
                HibernateUtil.rollbackTransaction();
            }

            throwException(e);
        } finally {

            if (isLocalTransaction && isNewConnection) {
                HibernateUtil.closeSessionSilently();
            }
        }

        return result;
    } // wrapReturn.


    /**
     *
     * @param delegate {@link ReturnableDelegate}
     * @return T result of the {@link ReturnableDelegate}
     * @throws DotDataException
     *
     * This class can be used to wrap methods in a "local transaction" pattern
     * this pattern will check to see if the method is being called in an existing transaction.
     * if it is being called in a transaction, it will do nothing.  If it is not being called in a transaction
     * it will checkout a db connection,start a transaction, do the work, commit the transaction, return the result
     * and  finally close the db connection.  If the SQL call fails, it will rollback the work, close  the db connection
     * and throw the error up the stack.
     *
     *  How to use:
     *
     *	return new LocalTransaction().wrapReturn(() ->{
     *		return myDBMethod(args);
     *  });
     */
    static public <T> T wrapReturn(final ReturnableDelegate<T> delegate) throws Exception {

        final boolean isNewConnection    = !DbConnectionFactory.connectionExists();
        final boolean isLocalTransaction = DbConnectionFactory.startTransactionIfNeeded();

        T result = null;

        try {

            result= delegate.execute();
            if (isLocalTransaction) {
                DbConnectionFactory.commit();
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
     *
     * @param delegate {@link VoidDelegate}
     * @throws DotDataException
     *
     * this will accept a method that does not need to return anything and wrap it in a transaction
     * if it is not in one already.  At the end of the call, it will
     * return the db connection to the connection pool
     *
     * 	 *  How to use:
     *
     *	 new LocalTransaction().wrap(() ->{
     *		 myDBMethod(args);
     *      return null;
     *  });
     */
    static public void wrap(final VoidDelegate delegate) throws Exception {

        final boolean isNewConnection    = !DbConnectionFactory.connectionExists();
        final boolean isLocalTransaction = DbConnectionFactory.startTransactionIfNeeded();
        
        try {

            delegate.execute();

            if (isLocalTransaction) {
                DbConnectionFactory.commit();
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

    private static void handleException(final boolean isLocalTransaction,
                                        final Throwable  e) throws Exception {
        if(isLocalTransaction){
            DbConnectionFactory.rollbackTransaction();
        }

        throwException(e);
    } // handleException.

    private static void throwException ( final Throwable  e) throws Exception {

        if (e instanceof Exception) {

            throw Exception.class.cast(e);
        }

        Throwable t = e;
        while(t.getCause()!=null){
            t=t.getCause();
        }
        if(t instanceof DotDataException){
            throw (DotDataException) t;
        }
        throw new DotDataException(t.getMessage(),t);
    }

} // E:O:F:LocalTransaction.
