package com.dotcms.business.interceptor;

import java.sql.Connection;

/**
 * Abstraction over Hibernate/JPA transaction and session lifecycle. Decouples the
 * interceptor/advice layer from {@code HibernateUtil} and {@code LocalTransaction} so
 * that the annotations and handlers can eventually live in a utility module.
 *
 * <p>The core module provides a real implementation; a no-op implementation is used by
 * default until the core implementation is registered via
 * {@link InterceptorServiceProvider}.</p>
 */
public interface TransactionOps {

    /**
     * Starts a local transaction if one is not already active.
     *
     * @return {@code true} if a <em>new</em> local transaction was started
     */
    boolean startLocalTransactionIfNeeded() throws Exception;

    /** Commits the current local transaction. */
    void commitTransaction() throws Exception;

    /** Rolls back the current local transaction. */
    void rollbackTransaction();

    /** Closes the current Hibernate session silently, swallowing exceptions. */
    void closeSessionSilently();

    /** Starts a new transaction on the current thread's connection. */
    void startTransaction() throws Exception;

    /**
     * Returns the current Hibernate session (typed as {@code Object} to avoid a compile-time
     * dependency on the Hibernate Session class).
     */
    Object getSession();

    /**
     * Sets the current thread's Hibernate session.
     *
     * @param session the session object (must be a Hibernate {@code Session} at runtime)
     */
    void setSession(Object session);

    /**
     * Creates a new Hibernate session bound to the given connection.
     *
     * @return the new session object
     */
    Object createNewSession(Connection connection) throws Exception;

    /**
     * Checks whether the connection that started the transaction is still the active one.
     * If a different connection is found, the implementation should log a warning or throw
     * based on the {@code LOCAL_TRANSACTION_INTERUPTED_ACTION} config property.
     *
     * @param connection  the connection that originally started the transaction
     * @param threadStack the stack trace captured at transaction start (for diagnostics)
     */
    void handleTransactionInterruption(Connection connection, StackTraceElement[] threadStack) throws Exception;

    /**
     * Re-throws the given throwable, unwrapping causes as needed. Exists to share the
     * exception-handling policy between advice and interceptor code.
     */
    void throwException(Throwable t) throws Exception;

    /**
     * Reads a string configuration property with a default.
     */
    String getConfigProperty(String key, String defaultValue);
}
