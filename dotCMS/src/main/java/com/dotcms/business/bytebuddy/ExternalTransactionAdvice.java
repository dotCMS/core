package com.dotcms.business.bytebuddy;

import com.dotcms.repackage.net.sf.hibernate.Session;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import io.vavr.control.Try;
import net.bytebuddy.asm.Advice;

import java.sql.Connection;

/**
 * This implements the ExternalTransaction annotation functionality.
 */
public class ExternalTransactionAdvice {

    @Advice.OnMethodEnter(inline = false)
    public static ExternalTransactionAdvice.TransactionInfo enter(@Advice.Origin("#m") String methodName) throws DotDataException {

        ExternalTransactionAdvice.TransactionInfo info = null;
        try {
            // gets the current connection
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
            final StackTraceElement[] threadStack = Thread.currentThread().getStackTrace();

            info = new ExternalTransactionAdvice.TransactionInfo(newTransactionConnection,
                    currentConnection, currentSession, threadStack);
        } catch (Throwable e) {

            HibernateUtil.rollbackTransaction();
            HibernateUtil.closeSessionSilently();
            Try.run(()->LocalTransaction.throwException(e)).getOrElseThrow(DotDataException::new);
        }

        return info;
    }

    @Advice.OnMethodExit(inline = false, onThrowable = Throwable.class )
    public static void exit(@Advice.Enter final ExternalTransactionAdvice.TransactionInfo info,
                            @Advice.Thrown final Throwable throwable) throws Throwable {

        if (info!=null) {

            try {
                if (throwable !=null)
                {
                    HibernateUtil.rollbackTransaction();
                    throw throwable;
                }

                try {
                    LocalTransaction.handleTransactionInteruption(info.newTransactionConnection, info.threadStack);
                    HibernateUtil.commitTransaction();
                } catch (Throwable e) {
                    HibernateUtil.rollbackTransaction();
                    LocalTransaction.throwException(e);
                }
            } finally {

                HibernateUtil.closeSessionSilently();
                // return the previous conn, if needed
                HibernateUtil.setSession(info.currentSession);
                DbConnectionFactory.setConnection(info.currentConnection);
            }
        }

        if (throwable !=null) {
            throw throwable;
        }
    }

    public static class TransactionInfo {
        private final Connection newTransactionConnection;
        private final Connection currentConnection;
        private final Session currentSession;
        private final StackTraceElement[] threadStack;

        public TransactionInfo(final Connection newTransactionConnection,
                               final Connection currentConnection, final Session currentSession,
                               final StackTraceElement[] threadStack) {

            this.newTransactionConnection = newTransactionConnection;
            this.currentConnection = currentConnection;
            this.currentSession = currentSession;
            this.threadStack = threadStack;
        }
    }
}
