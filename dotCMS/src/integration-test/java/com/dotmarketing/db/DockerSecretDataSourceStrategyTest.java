package com.dotmarketing.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.TestInitialContext;
import com.dotmarketing.util.Constants;
import com.liferay.util.SystemEnvironmentProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for {@link DockerSecretDataSourceStrategy}
 * @author nollymar
 */
public class DockerSecretDataSourceStrategyTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testApply() throws SQLException, IOException, NamingException {
        final HikariDataSource testDatasource = (HikariDataSource) TestInitialContext.getInstance()
                .getDataSource();

        final File tempFile = createTempFile(
                "connection_db_driver=" + testDatasource.getDriverClassName() + "\n"
                        + "connection_db_base_url=" + testDatasource.getJdbcUrl() + "\n"
                        + "connection_db_username=" + testDatasource.getUsername() + "\n"
                        + "connection_db_password=" + testDatasource.getPassword() + "\n"
                        + "connection_db_validation_query=SELECT 1");

        final SystemEnvironmentProperties systemEnvironmentProperties = Mockito.mock(SystemEnvironmentProperties.class);

        Mockito.when(systemEnvironmentProperties.getVariable("DOCKER_SECRET_FILE_PATH")).thenReturn(tempFile.getPath());

        final DockerSecretDataSourceStrategy strategy = new DockerSecretDataSourceStrategy(systemEnvironmentProperties);

        final DataSource dataSource = strategy.apply();

        assertNotNull(dataSource);
        assertTrue(dataSource instanceof HikariDataSource);
        assertNotNull(dataSource.getConnection());
    }

    @Test
    public void testGetHikariConfig(){
        final Map<String, String> dockerSecretsMap = new HashMap<>();

        dockerSecretsMap.put("connection_db_driver", "org.postgresql.Driver");
        dockerSecretsMap.put("connection_db_base_url", "jdbc:postgresql://localhost/dotcms");
        dockerSecretsMap.put("connection_db_username", "username");
        dockerSecretsMap.put("connection_db_password", "password");
        dockerSecretsMap.put("connection_db_max_total", "60");
        dockerSecretsMap.put("connection_db_max_idle", "10");
        dockerSecretsMap.put("connection_db_max_wait", "60000");
        dockerSecretsMap.put("connection_db_validation_query", "SELECT 1");
        dockerSecretsMap.put("connection_db_leak_detection_threshold", "60000");

        final HikariConfig config = DockerSecretDataSourceStrategy.getInstance()
                .getHikariConfig(dockerSecretsMap);

        validateConfiguration(config, dockerSecretsMap);
    }

    /**
     * Verifies all values in the config object are set correctly
     * @param config {@link HikariConfig} to be validated
     * @param dockerSecretsMap {@link Map} contains the expected values to be compared
     */
    private void validateConfiguration(final HikariConfig config, Map<String, String> dockerSecretsMap) {
        assertNotNull(config);
        assertEquals(Constants.DATABASE_DEFAULT_DATASOURCE, config.getPoolName());
        assertEquals(dockerSecretsMap.get("connection_db_driver"), config.getDriverClassName());
        assertEquals(dockerSecretsMap.get("connection_db_base_url"), config.getJdbcUrl());
        assertEquals(dockerSecretsMap.get("connection_db_username"), config.getUsername());
        assertEquals(dockerSecretsMap.get("connection_db_password"), config.getPassword());
        assertEquals(Integer.parseInt(dockerSecretsMap.get("connection_db_max_total")),
                config.getMaximumPoolSize());
        assertEquals(Integer.parseInt(dockerSecretsMap.get("connection_db_max_idle")) * 1000,
                config.getIdleTimeout());
        assertEquals(Integer.parseInt(dockerSecretsMap.get("connection_db_max_wait")),
                config.getMaxLifetime());
        assertEquals(dockerSecretsMap.get("connection_db_validation_query"),
                config.getConnectionTestQuery());
        assertEquals(
                Integer.parseInt(dockerSecretsMap.get("connection_db_leak_detection_threshold")),
                config.getLeakDetectionThreshold());
        assertEquals(dockerSecretsMap.get("connection_db_default_transaction_isolation"),
                config.getTransactionIsolation());
    }

    /**
     * Creates a temporal file using a given content
     * @param content {@link String}
     * @return
     * @throws IOException
     */
    private File createTempFile(final String content) throws IOException {

        final File tempTestFile = File
                .createTempFile("TestDockerSecrets_" + new Date().getTime(), ".txt");
        FileUtils.writeStringToFile(tempTestFile, content);

        return tempTestFile;
    }

}
