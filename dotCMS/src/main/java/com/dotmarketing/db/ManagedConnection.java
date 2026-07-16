package com.dotmarketing.db;

import com.dotmarketing.util.Logger;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * A {@link Connection} wrapper that is safe for use in try-with-resources blocks
 * with {@link DbConnectionFactory#getConnection()}.
 *
 * <p>The problem: {@code DbConnectionFactory.getConnection()} returns a ThreadLocal-managed
 * connection. If a connection already exists on the current thread, the same connection is
 * returned. Using it in try-with-resources causes {@code close()} to be called on exit,
 * which closes and removes the ThreadLocal connection — breaking any outer scope that
 * was still using it.</p>
 *
 * <p>This wrapper tracks whether the connection was <b>newly created</b> by the
 * {@code getConnection()} call or <b>borrowed</b> from an existing ThreadLocal entry.
 * On {@code close()}:</p>
 * <ul>
 *   <li>If the connection was newly created (this call opened it): close via
 *       {@code DbConnectionFactory.closeSilently()} to properly clean up the ThreadLocal.</li>
 *   <li>If the connection was borrowed (already existed): no-op. The owning scope
 *       (typically {@code @CloseDBIfOpened} or {@code @WrapInTransaction}) manages lifecycle.</li>
 * </ul>
 *
 * @see DbConnectionFactory#getConnection()
 */
public class ManagedConnection extends DotConnectionWrapper {

    private final boolean ownsConnection;
    private boolean closed;

    /**
     * Creates a managed connection wrapper.
     *
     * @param delegate       the real JDBC connection
     * @param ownsConnection true if this wrapper owns the connection lifecycle
     *                       (i.e., no connection existed on the thread before this call)
     */
    ManagedConnection(final Connection delegate, final boolean ownsConnection) {
        super(delegate);
        this.ownsConnection = ownsConnection;
        this.closed = false;
    }

    /**
     * Returns whether this wrapper owns the underlying connection.
     * When true, {@link #close()} will actually close and clean up the ThreadLocal.
     *
     * @return true if this wrapper created the connection
     */
    public boolean isOwner() {
        return ownsConnection;
    }

    /**
     * Closes the connection only if this wrapper owns it.
     *
     * <p>If the connection was borrowed from an existing ThreadLocal entry,
     * this method is a no-op — the connection remains open for the owning scope.</p>
     */
    @Override
    public void close() throws SQLException {
        if (closed) {
            return;
        }

        if (ownsConnection) {
            closed = true;
            Logger.debug(ManagedConnection.class,
                    () -> "ManagedConnection closing owned connection for thread "
                            + Thread.currentThread().getId());
            DbConnectionFactory.closeSilently();
        } else {
            Logger.debug(ManagedConnection.class,
                    () -> "ManagedConnection skipping close() — connection borrowed from outer scope on thread "
                            + Thread.currentThread().getId());
        }
    }

    /**
     * Returns whether this managed connection has been logically closed.
     * For borrowed connections, this always returns false (the underlying connection
     * is still open and managed by its owner).
     */
    @Override
    public boolean isClosed() throws SQLException {
        if (closed) {
            return true;
        }
        return internalConnection.isClosed();
    }

    @Override
    public String toString() {
        return "ManagedConnection{owns=" + ownsConnection
                + ", closed=" + closed
                + ", delegate=" + internalConnection + "}";
    }
}