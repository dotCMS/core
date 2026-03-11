package com.dotcms.business.cdi;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.Serializable;
import java.sql.Connection;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * CDI interceptor for {@link WrapInTransaction}. Wraps method execution in a local transaction,
 * starting a new one if needed and committing/rolling back on completion.
 *
 * <p>This interceptor fires at the Weld proxy boundary for CDI-managed beans, complementing
 * the ByteBuddy advice that instruments non-CDI classes at load-time. The
 * {@link InterceptorGuard} prevents double-processing when both mechanisms are active.</p>
 */
@Interceptor
@WrapInTransaction
@Priority(Interceptor.Priority.APPLICATION + 1)
public class WrapInTransactionInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String WARN_MESSAGE =
            "Transaction broken - Connection that started the transaction is not the same as the one who is commiting";

    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Exception {
        if (!InterceptorGuard.acquire(WrapInTransaction.class)) {
            return context.proceed();
        }

        boolean isNewConnection = false;
        boolean isLocalTransaction = false;
        Connection conn = null;
        try {
            isNewConnection = !DbConnectionFactory.connectionExists();
            isLocalTransaction = HibernateUtil.startLocalTransactionIfNeeded();
            conn = DbConnectionFactory.getConnection();

            final Object result = context.proceed();

            if (isLocalTransaction) {
                handleTransactionInterruption(conn);
                HibernateUtil.commitTransaction();
            }
            return result;
        } catch (Throwable t) {
            if (isLocalTransaction) {
                HibernateUtil.rollbackTransaction();
            }
            throwException(t);
            return null; // unreachable, throwException always throws
        } finally {
            if (isNewConnection) {
                HibernateUtil.closeSessionSilently();
            }
            InterceptorGuard.release(WrapInTransaction.class);
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

    private static void handleTransactionInterruption(final Connection conn) throws DotDataException {
        if (DbConnectionFactory.getConnection() != conn) {
            final String action = Config.getStringProperty(
                    "LOCAL_TRANSACTION_INTERUPTED_ACTION", "LOG");
            if ("LOG".equalsIgnoreCase(action)) {
                Logger.warn(WrapInTransactionInterceptor.class, WARN_MESSAGE);
                Logger.warn(WrapInTransactionInterceptor.class,
                        ExceptionUtils.getStackTrace(new Throwable()));
            } else if ("THROW".equalsIgnoreCase(action)) {
                throw new DotDataException(WARN_MESSAGE);
            }
        }
    }
}
