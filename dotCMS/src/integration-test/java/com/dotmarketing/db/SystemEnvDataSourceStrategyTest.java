package com.dotmarketing.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.TestInitialContext;
import com.dotmarketing.util.Constants;
import com.liferay.util.SystemEnvironmentProperties;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.naming.NamingException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for {@link SystemEnvDataSourceStrategy}
 * @author nollymar
 */
public class SystemEnvDataSourceStrategyTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link SystemEnvDataSourceStrategy#apply()}
     * Test case: Happy path to get a DataSource using system environment variables
     * Expected result: A valid HikariDataSource should be returned
     * @throws SQLException
     * @throws NamingException
     */
    @Test
    public void testApply() throws SQLException, NamingException {
        final Map<String, String> properties = new HashMap<>();
        final HikariDataSource testDatasource = (HikariDataSource) TestInitialContext.getInstance()
                .getDataSource();
        properties.put("connection_db_driver", testDatasource.getDriverClassName());
        properties.put("connection_db_base_url", testDatasource.getJdbcUrl());
        properties.put("connection_db_username", testDatasource.getUsername());
        properties.put("connection_db_password", testDatasource.getPassword());
        properties.put("connection_db_max_total", "60");
        properties.put("connection_db_max_idle", "10");
        properties.put("connection_db_max_wait", "60000");
        properties.put("connection_db_validation_query", "SELECT 1");
        properties.put("connection_db_leak_detection_threshold", "60000");

        final SystemEnvironmentProperties systemEnvironmentProperties = Mockito.mock(SystemEnvironmentProperties.class);

        properties.forEach((k,v) -> Mockito.when(systemEnvironmentProperties.getVariable(k)).thenReturn(v));

        SystemEnvDataSourceStrategy strategy = new SystemEnvDataSourceStrategy(systemEnvironmentProperties);

        final HikariDataSource dataSource = (HikariDataSource)
                strategy.apply();

        assertNotNull(dataSource);
        assertNotNull(dataSource.getConnection());
        validateConfiguration(dataSource, properties);
    }

    /**
     * Verifies all values in the dataSource object are set correctly
     * @param dataSource {@link HikariDataSource} to be validated
     * @param properties {@link Map} contains the expected values to be compared
     */
    private void validateConfiguration(final HikariDataSource dataSource, Map<String, String> properties) {
        assertEquals(Constants.DATABASE_DEFAULT_DATASOURCE, dataSource.getPoolName());
        assertEquals(properties.get("connection_db_driver"), dataSource.getDriverClassName());
        assertEquals(properties.get("connection_db_base_url"), dataSource.getJdbcUrl());
        assertEquals(properties.get("connection_db_username"), dataSource.getUsername());
        assertEquals(properties.get("connection_db_password"), dataSource.getPassword());
        assertEquals(Integer.parseInt(properties.get("connection_db_max_total")),
                dataSource.getMaximumPoolSize());
        assertEquals(Integer.parseInt(properties.get("connection_db_max_idle")) * 1000,
                dataSource.getIdleTimeout());
        assertEquals(Integer.parseInt(properties.get("connection_db_max_wait")),
                dataSource.getMaxLifetime());
        assertEquals(properties.get("connection_db_validation_query"),
                dataSource.getConnectionTestQuery());
        assertEquals(
                Integer.parseInt(properties.get("connection_db_leak_detection_threshold")),
                dataSource.getLeakDetectionThreshold());
        assertEquals(properties.get("connection_db_default_transaction_isolation"),
                dataSource.getTransactionIsolation());
    }

}
