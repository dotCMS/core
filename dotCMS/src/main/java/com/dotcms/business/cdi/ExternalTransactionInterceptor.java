package com.dotcms.business.cdi;

import com.dotcms.business.ExternalTransaction;
import com.dotcms.repackage.net.sf.hibernate.Session;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import io.vavr.control.Try;

import java.io.Serializable;
import java.sql.Connection;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * CDI interceptor for {@link ExternalTransaction}. Creates a new, isolated transaction context
 * while preserving the current connection and session, restoring them after completion.
 *
 * <p>This interceptor fires at the Weld proxy boundary for CDI-managed beans, complementing
 * the ByteBuddy advice that instruments non-CDI classes at load-time. The
 * {@link InterceptorGuard} prevents double-processing when both mechanisms are active.</p>
 */
@Interceptor
@ExternalTransaction
@Priority(Interceptor.Priority.APPLICATION + 2)
public class ExternalTransactionInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Exception {
        if (!InterceptorGuard.acquire(ExternalTransaction.class)) {
            return context.proceed();
        }

        Connection currentConnection = null;
        Session currentSession = null;
        Connection newTransactionConnection = null;
        final StackTraceElement[] threadStack = Thread.currentThread().getStackTrace();

        try {
            // Save current connection and session
            currentConnection = DbConnectionFactory.getConnection();
            currentSession = HibernateUtil.getSession();

            // Create a new connection for the external transaction
            newTransactionConnection = DbConnectionFactory.getDataSource().getConnection();

            // Override the current thread's connection and session
            DbConnectionFactory.setConnection(newTransactionConnection);
            final Session newSession = HibernateUtil.createNewSession(newTransactionConnection);
            HibernateUtil.setSession(newSession);

            HibernateUtil.startTransaction();

            final Object result = context.proceed();

            // Commit if successful
            LocalTransaction.handleTransactionInteruption(newTransactionConnection, threadStack);
            HibernateUtil.commitTransaction();

            return result;
        } catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            throwException(t);
            return null; // unreachable
        } finally {
            HibernateUtil.closeSessionSilently();
            // Restore previous connection and session
            if (currentSession != null) {
                HibernateUtil.setSession(currentSession);
            }
            if (currentConnection != null) {
                DbConnectionFactory.setConnection(currentConnection);
            }
            InterceptorGuard.release(ExternalTransaction.class);
        }
    }

    private static void throwException(final Throwable e) throws Exception {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
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
}
