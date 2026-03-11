package com.dotcms.business.interceptor;

import java.sql.Connection;

/**
 * Abstraction over database connection lifecycle operations. Decouples the interceptor/advice
 * layer from the concrete {@code DbConnectionFactory} so that the annotations and handlers
 * can eventually live in a utility module with no core dependency.
 *
 * <p>The core module provides a real implementation that delegates to
 * {@code DbConnectionFactory}; a no-op implementation is used by default until the core
 * implementation is registered via {@link InterceptorServiceProvider}.</p>
 */
public interface DatabaseConnectionOps {

    /**
     * Returns {@code true} if a database connection already exists on the current thread.
     */
    boolean connectionExists();

    /**
     * Returns the current thread's database connection, creating one if necessary.
     */
    Connection getConnection();

    /**
     * Replaces the current thread's database connection with the given one.
     */
    void setConnection(Connection connection);

    /**
     * Creates a brand-new database connection from the data source, independent of the
     * current thread's connection.
     */
    Connection newConnection() throws Exception;

    /**
     * Commits and closes the current thread's connection, swallowing any exceptions.
     */
    void closeAndCommit();

    /**
     * Closes the current thread's connection silently, swallowing any exceptions.
     */
    void closeSilently();
}
