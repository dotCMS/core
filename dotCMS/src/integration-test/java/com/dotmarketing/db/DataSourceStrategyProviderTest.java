package com.dotmarketing.db;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.postgresql.jdbc.PgConnection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.TimeZone;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Provides test routines for verifying the expected behavior of the {@link DataSourceStrategyProvider} class.
 *
 * @author Jose Castro
 * @since Oct 18th, 2022
 */
public class DataSourceStrategyProviderTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link DataSourceStrategyProvider#get()}</li>
     *     <li><b>Given Scenario:</b> By default, all database connection objects must be created using the UTC Time
     *     Zone, regardless of the current default or server Time Zone.</li>
     *     <li><b>Expected Result:</b> If the default Time Zone is NOT in UTC, the {@link Connection} object generated
     *     by our {@link DataSourceStrategyProvider} must always reference the UTC Time Zone.</li>
     * </ul>
     */
    @Test
    public void testConnectionObjectTimeZone() {
        final TimeZone defaultTz = TimeZone.getDefault();
        if ("UTC".equalsIgnoreCase(defaultTz.getID())) {
            // If the default TimeZone is already set to UTC, this test can be ignored
            return;
        }
        try {
            final DataSource dataSource = DataSourceStrategyProvider.getInstance().get();
            final Connection connection = dataSource.getConnection();
            final PgConnection pgConn = connection.unwrap(PgConnection.class);
            final TimeZone connectionTz = pgConn.getQueryExecutor().getTimeZone();
            assertTrue("Connection object Time Zone must always be UTC", "UTC".equalsIgnoreCase(connectionTz.getID()));
            assertFalse("TimeZone value from the Connection object must always be UTC!",
                    connectionTz.getID().equals(defaultTz.getID()));
        } catch (final ClassNotFoundException | IllegalAccessException | InstantiationException | SQLException e) {
            throw new RuntimeException("An error occurred when checking the TimeZone value form the Connection " +
                                               "object: " + e.getMessage(), e);
        }
    }

}
