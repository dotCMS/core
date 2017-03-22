package com.dotmarketing.db;

import java.util.concurrent.Callable;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;

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

	static public <T> T wrapReturn(final Callable<T> callable) throws DotDataException{
		boolean localTransaction = DbConnectionFactory.startTransactionIfNeeded();
		try {
			T result= callable.call();
			return result;
		} catch (Exception e) {
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
	
	/**
	 * 
	 * this will accept a method with a return of void and wrap it in a transaction
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
	static public <T> T wrap(final Callable<T> callable) throws DotDataException{
		return wrapReturn(callable);
	}
}
