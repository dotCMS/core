package com.dotcms.dotpubsub;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.Test;

/**
 * Unit tests for where the Postgres pub/sub listener gets its connection from (issue #36934).
 *
 * <p>The listener holds its connection open for the lifetime of the JVM, which is exactly what a
 * request pool must not give away: the slot never comes back and HikariCP reports the hold as an
 * apparent connection leak on every boot. These tests pin the selection — dedicated when the
 * datasource exposes JDBC coordinates, pooled only as a fallback — without needing a live pool.</p>
 *
 * @author Fabrizzio Araya
 */
public class JDBCPubSubListenerConnectionTest {

    /**
     * Given : a datasource that exposes no JDBC URL — a JNDI-provided or otherwise wrapped one.
     * When  : the listener asks for its connection.
     * Then  : it falls back to the pool, because a listener that works while logging a spurious
     *         warning beats no listener at all.
     */
    @Test
    public void datasourceWithoutJdbcCoordinates_fallsBackToThePool() throws SQLException {

        final DataSource pool = mock(DataSource.class);
        final Connection pooled = mock(Connection.class);
        when(pool.getConnection()).thenReturn(pooled);

        assertSame("Without JDBC coordinates the pool is the only option left",
                pooled, JDBCPubSubImpl.PGListener.listenerConnection(pool));
        verify(pool).getConnection();
    }

    /**
     * Given : a Hikari datasource that does expose a JDBC URL.
     * When  : the listener asks for its connection.
     * Then  : the pool is never asked for one — the connection is opened directly, so the pool
     *         keeps its full capacity and the leak detector has nothing to report.
     *
     * <p>The URL points at a closed port, so opening it fails; the assertion that matters is not
     * the failure but that it was attempted <em>instead of</em> borrowing from the pool.</p>
     */
    @Test
    public void hikariWithJdbcCoordinates_neverBorrowsFromThePool() throws SQLException {

        try (final HikariDataSource hikari = spy(new HikariDataSource())) {
            hikari.setJdbcUrl("jdbc:postgresql://127.0.0.1:1/dotcms-does-not-exist");
            hikari.setUsername("dotcmsdbuser");
            hikari.setPassword("unused");

            assertThrows("A closed port must surface as a SQLException from the direct connection",
                    SQLException.class,
                    () -> JDBCPubSubImpl.PGListener.listenerConnection(hikari));

            verify(hikari, never()).getConnection();
        }
    }
}
