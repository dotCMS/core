package com.dotmarketing.db;

import com.dotcms.util.ReturnableDelegate;
import com.dotcms.util.VoidDelegate;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;

public class LocalTransaction {



    /**
	 * 
	 * @param callable
	 * @return
	 * @throws Exception
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

	static public <T> T wrapReturn(final ReturnableDelegate<T> callable) throws DotDataException{
		final boolean localTransaction = DbConnectionFactory.startTransactionIfNeeded();
		T result = null;
		try {
			result= callable.execute();
		} catch (Throwable e) {
            handleException(localTransaction, e);
		} finally {
			if (localTransaction) {
				try {
					DbConnectionFactory.closeAndCommit();
				} catch (DotHibernateException e) {
					throw new DotDataException(e.getMessage(), e);
				}
			}
		}
		return result;
	}
	
	/**
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
	static public void wrap(final VoidDelegate callable) throws DotDataException {
        final boolean localTransaction = DbConnectionFactory.startTransactionIfNeeded();
        try {
            callable.execute();
        } catch (Exception e) {
            handleException(localTransaction, e);
        } finally {
            if (localTransaction) {
                try {
                    DbConnectionFactory.closeAndCommit();
                } catch (DotHibernateException e) {
                    throw new DotDataException(e.getMessage(), e);
                }
            }
        }
	}

    private static void handleException(boolean localTransaction, Throwable  e) throws DotDataException {
        if(localTransaction){
            DbConnectionFactory.rollbackTransaction();
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
}
