package com.dotcms.business.bytebuddy;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.github.rjeschke.txtmark.Run;
import net.bytebuddy.asm.Advice;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.lang.reflect.Method;
import java.sql.Connection;

public class WrapInTransactionAdvice {

    private static enum TransactionErrorEnum {
        NOTHING,
        LOG,
        THROW;
    }
    private final static String WARN_MESSAGE = "Transaction broken - Connection that started the transaction is not the same as the one who is commiting";

    @Advice.OnMethodEnter(inline = false)
    public static TransactionInfo enter(@Advice.Origin("#m") String methodName) throws DotDataException {

        TransactionInfo info = null;
        boolean isLocalTransaction = false;
        boolean isNewConnection = false;
        try {
            isNewConnection = !DbConnectionFactory.connectionExists();
            isLocalTransaction = HibernateUtil.startLocalTransactionIfNeeded();
            final Connection conn = DbConnectionFactory.getConnection();
            info = new TransactionInfo(isNewConnection,isLocalTransaction,conn);
        } catch (Throwable e) {
            if (isLocalTransaction) {
                HibernateUtil.rollbackTransaction();
            }
            if (isNewConnection) {
                HibernateUtil.closeSessionSilently();
            }
            throwException(e);
        }
        return info;
    }

    @Advice.OnMethodExit(inline = false, onThrowable = Throwable.class )
    public static void exit(@Advice.Enter TransactionInfo info, @Advice.Thrown Throwable t) throws Throwable {
        if (info!=null)
        {
            try {
            if (info.isLocalTransaction)
            {
                if (t!=null)
                {
                    HibernateUtil.rollbackTransaction();
                    throw t;
                }

                try {
                    handleTransactionInteruption(info.connection);
                    HibernateUtil.commitTransaction();
                } catch (Throwable e) {
                    HibernateUtil.rollbackTransaction();
                    throwException(e);
                }
            }
            } finally {
                if (info.isNewConnection) {
                    HibernateUtil.closeSessionSilently();
                }
            }
        }

        if (t!=null)
            throw t;


    }

    private static class TransactionInfo {
        private final boolean isNewConnection;
        private final boolean isLocalTransaction;
        private final Connection connection;

        public TransactionInfo(boolean isNewConnection, final boolean isLocalTransaction, Connection connection) {
            this.isNewConnection = isNewConnection;
            this.isLocalTransaction = isLocalTransaction;
            this.connection = connection;
        }
    }


    private static void throwException ( final Throwable  e) throws DotDataException {

        if (e instanceof RuntimeException) {
            throw (RuntimeException)e;
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

    private static void handleTransactionInteruption(final Connection conn) throws DotDataException {
        if (DbConnectionFactory.getConnection() != conn) {
            final String action = Config.getStringProperty("LOCAL_TRANSACTION_INTERUPTED_ACTION", TransactionErrorEnum.LOG.name());
            if (TransactionErrorEnum.LOG.name().equalsIgnoreCase(action)) {

                Logger.warn(WrapInTransactionAdvice.class, WARN_MESSAGE );
                Logger.warn(WrapInTransactionAdvice.class, ExceptionUtils.getStackTrace(new Throwable()));

            } else if (TransactionErrorEnum.THROW.name().equalsIgnoreCase(action)) {
                throw new DotDataException(WARN_MESSAGE);
            }
        }
    }

}
