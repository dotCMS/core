package com.dotcms.business.interceptor;

import com.dotcms.repackage.net.sf.hibernate.Session;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;

import java.sql.Connection;

/**
 * Core implementation of {@link TransactionOps} that delegates to {@link HibernateUtil},
 * {@link LocalTransaction}, and {@link Config}. This class stays in the core module when
 * the SPI interfaces and handlers are extracted to a utility module.
 */
public final class CoreTransactionOps implements TransactionOps {

    public static final CoreTransactionOps INSTANCE = new CoreTransactionOps();

    private CoreTransactionOps() { }

    @Override
    public boolean startLocalTransactionIfNeeded() throws Exception {
        return HibernateUtil.startLocalTransactionIfNeeded();
    }

    @Override
    public void commitTransaction() throws Exception {
        HibernateUtil.commitTransaction();
    }

    @Override
    public void rollbackTransaction() {
        try {
            HibernateUtil.rollbackTransaction();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void closeSessionSilently() {
        HibernateUtil.closeSessionSilently();
    }

    @Override
    public void startTransaction() throws Exception {
        HibernateUtil.startTransaction();
    }

    @Override
    public Object getSession() {
        return HibernateUtil.getSession();
    }

    @Override
    public void setSession(final Object session) {
        HibernateUtil.setSession((Session) session);
    }

    @Override
    public Object createNewSession(final Connection connection) throws Exception {
        return HibernateUtil.createNewSession(connection);
    }

    @Override
    public void handleTransactionInterruption(final Connection connection,
                                              final StackTraceElement[] threadStack) throws Exception {
        if (threadStack != null) {
            LocalTransaction.handleTransactionInteruption(connection, threadStack);
        } else {
            // When called from WrapInTransaction (no stack captured), check inline
            if (DbConnectionFactory.getConnection() != connection) {
                final String action = Config.getStringProperty(
                        "LOCAL_TRANSACTION_INTERUPTED_ACTION", "LOG");
                if ("LOG".equalsIgnoreCase(action)) {
                    com.dotmarketing.util.Logger.warn(CoreTransactionOps.class,
                            "Transaction broken - Connection that started the transaction is not the same as the one who is commiting");
                } else if ("THROW".equalsIgnoreCase(action)) {
                    throw new DotDataException(
                            "Transaction broken - Connection that started the transaction is not the same as the one who is commiting");
                }
            }
        }
    }

    @Override
    public void throwException(final Throwable t) throws Exception {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof DotDataException) {
            throw (DotDataException) cause;
        }
        throw new DotDataException(cause.getMessage(), cause);
    }

    @Override
    public String getConfigProperty(final String key, final String defaultValue) {
        return Config.getStringProperty(key, defaultValue);
    }
}
