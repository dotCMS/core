package com.dotmarketing.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Constants;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.FileInputStream;
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
        final HikariDataSource testDatasource = (HikariDataSource) DbConnectionFactory.getDataSource();

        final File tempFile = createTempFile(
                "driverClassName=" + testDatasource.getDriverClassName() + "\n"
                        + "jdbcUrl=" + testDatasource.getJdbcUrl() + "\n"
                        + "username=" + testDatasource.getUsername() + "\n"
                        + "password=" + testDatasource.getPassword() + "\n"
                        + "connectionTestQuery=" + testDatasource.getConnectionTestQuery());

        final DataSource dataSource = new DBPropertiesDataSourceStrategy(tempFile).apply();

        assertNotNull(dataSource);
        assertTrue(dataSource instanceof HikariDataSource);
        assertNotNull(dataSource.getConnection());
    }

    @Test
    public void testGetHikariConfigWithValidFileShouldPass()
            throws IOException, ConfigurationException {
        final File tempFile = createTempFile("driverClassName=org.postgresql.Driver\n"
                + "jdbcUrl=jdbc:postgresql://localhost/dotcms\n"
                + "username=postgres\n"
                + "password=postgres\n"
                + "connectionTestQuery=SELECT 1\n"
                + "maximumPoolSize=60\n"
                + "idleTimeout=10000\n"
                + "maxLifetime=60000\n"
                + "leakDetectionThreshold=60000");

        final PropertiesConfiguration properties = new PropertiesConfiguration();
        properties.load(new FileInputStream(tempFile));
        final HikariConfig config = new DBPropertiesDataSourceStrategy(tempFile)
                .getHikariConfig();
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
        assertEquals(properties.getString("driverClassName"), config.getDriverClassName());
        assertEquals(properties.getString("jdbcUrl"), config.getJdbcUrl());
        assertEquals(properties.getString("username"), config.getUsername());
        assertEquals(properties.getString("password"), config.getPassword());
        assertEquals(properties.getInt("maximumPoolSize"), config.getMaximumPoolSize());
        assertEquals(properties.getInt("idleTimeout"),config.getIdleTimeout());
        assertEquals(properties.getInt("maxLifetime"), config.getMaxLifetime());
        assertEquals(properties.getString("connectionTestQuery"), config.getConnectionTestQuery());
        assertEquals(properties.getInt("leakDetectionThreshold"),
                config.getLeakDetectionThreshold());
        assertEquals(properties.getString("transactionIsolation"),
                config.getTransactionIsolation());
    }

    /**
     * Creates a temporal file using a given content
     *
     * @param content {@link String}
     */
    private File createTempFile(final String content) throws IOException {

        final File tempTestFile = File
                .createTempFile("TestDBProperties_" + new Date().getTime(), ".txt");
        FileUtils.writeStringToFile(tempTestFile, content);

        return tempTestFile;
    }


}
