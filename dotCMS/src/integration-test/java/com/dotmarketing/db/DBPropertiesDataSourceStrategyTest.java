package com.dotmarketing.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.TestInitialContext;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Constants;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Date;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for {@link DBPropertiesDataSourceStrategy} class
 *
 * @author nollymar
 */
public class DBPropertiesDataSourceStrategyTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExistsDBPropertiesFileShouldReturnFalse() {
        final DBPropertiesDataSourceStrategy strategy = new DBPropertiesDataSourceStrategy(
                new File("fake_file.properties"));
        assertFalse(strategy.existsDBPropertiesFile());
    }

    @Test
    public void testExistsDBPropertiesFileShouldReturnTrue() throws IOException {
        final Path path = Files.createTempFile("test", String.valueOf(System.currentTimeMillis()));
        final DBPropertiesDataSourceStrategy strategy = new DBPropertiesDataSourceStrategy(
                path.toFile());
        assertTrue(strategy.existsDBPropertiesFile());
    }

    @Test(expected = DotRuntimeException.class)
    public void testApplyWithANonExistingFileShouldFail() {
        new DBPropertiesDataSourceStrategy(new File("fake_file.properties")).apply();
    }

    @Test(expected = DotRuntimeException.class)
    public void testApplyWithANullFileShouldFail() {
        new DBPropertiesDataSourceStrategy(null).apply();
    }

    @Test
    public void testApplyWithValidFileShouldPass()
            throws IOException, SQLException, NamingException {
        final HikariDataSource testDatasource = (HikariDataSource) TestInitialContext.getInstance()
                .getDataSource();

        final File tempFile = createTempFile(
                "connection_db_driver=" + testDatasource.getDriverClassName() + "\n"
                        + "connection_db_base_url=" + testDatasource.getJdbcUrl() + "\n"
                        + "connection_db_username=" + testDatasource.getUsername() + "\n"
                        + "connection_db_password=" + testDatasource.getPassword() + "\n"
                        + "connection_db_validation_query=SELECT 1");

        final DataSource dataSource = new DBPropertiesDataSourceStrategy(tempFile).apply();

        assertNotNull(dataSource);
        assertTrue(dataSource instanceof HikariDataSource);
        assertNotNull(dataSource.getConnection());
    }

    @Test
    public void testGetHikariConfigWithValidFileShouldPass()
            throws IOException, ConfigurationException {
        final File tempFile = createTempFile("connection_db_driver=org.postgresql.Driver\n"
                + "connection_db_base_url=jdbc:postgresql://localhost/dotcms\n"
                + "connection_db_username=postgres\n"
                + "connection_db_password=postgres\n"
                + "connection_db_validation_query=SELECT 1");

        final PropertiesConfiguration properties = new PropertiesConfiguration();
        properties.load(tempFile);
        final HikariConfig config = new DBPropertiesDataSourceStrategy(tempFile)
                .getHikariConfig(properties);
        validateConfiguration(config, properties);
    }

    /**
     * Verifies all values in the config object are set correctly
     *
     * @param config {@link HikariConfig} to be validated
     * @param properties {@link PropertiesConfiguration} contains the expected values to be
     * compared
     */
    private void validateConfiguration(final HikariConfig config,
            final PropertiesConfiguration properties) {
        assertNotNull(config);
        assertEquals(Constants.DATABASE_DEFAULT_DATASOURCE, config.getPoolName());
        assertEquals(properties.getString("connection_db_driver"), config.getDriverClassName());
        assertEquals(properties.getString("connection_db_base_url"), config.getJdbcUrl());
        assertEquals(properties.getString("connection_db_username"), config.getUsername());
        assertEquals(properties.getString("connection_db_password"), config.getPassword());
        assertEquals(properties.getInt("connection_db_max_total", 60), config.getMaximumPoolSize());
        assertEquals(properties.getInt("connection_db_max_idle", 10) * 1000,
                config.getIdleTimeout());
        assertEquals(properties.getInt("connection_db_max_wait", 60000), config.getMaxLifetime());
        assertEquals(properties.getString("connection_db_validation_query"),
                config.getConnectionTestQuery());
        assertEquals(properties.getInt("connection_db_leak_detection_threshold", 60000),
                config.getLeakDetectionThreshold());
        assertEquals(properties.getString("connection_db_default_transaction_isolation"),
                config.getTransactionIsolation());
    }

    /**
     * Creates a temporal file using a given content
     *
     * @param content {@link String}
     */
    private File createTempFile(final String content) throws IOException {

        final File tempTestFile = File
                .createTempFile("TestDockerSecrets_" + new Date().getTime(), ".txt");
        FileUtils.writeStringToFile(tempTestFile, content);

        return tempTestFile;
    }


}
