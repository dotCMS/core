package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.Test;
import org.mockito.MockedStatic;

/** A failed metadata lookup must not be treated as a completed outbox migration. */
public class Task261112CreateRequestCostOutboxTest {

    @Test
    public void test_metadataFailureStopsMigration() throws Exception {
        final Connection connection = mock(Connection.class);
        final SQLException failure = new SQLException("metadata unavailable");
        when(connection.getMetaData()).thenThrow(failure);
        try (MockedStatic<DbConnectionFactory> db = mockStatic(DbConnectionFactory.class)) {
            db.when(DbConnectionFactory::getConnection).thenReturn(connection);
            final DotRuntimeException actual = assertThrows(DotRuntimeException.class,
                    () -> new Task261112CreateRequestCostOutbox().forceRun());
            assertSame(failure, actual.getCause());
        }
    }
}
