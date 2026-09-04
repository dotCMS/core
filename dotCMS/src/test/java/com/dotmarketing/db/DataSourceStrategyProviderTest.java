package com.dotmarketing.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.util.Config;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DataSourceStrategyProvider}.
 *
 * <p>Verifies the datasource resolution precedence:
 * <ol>
 *   <li>Explicit custom class (if configured and != SystemEnvDataSourceStrategy)</li>
 *   <li>{@code db.properties} file (if present)</li>
 *   <li>Docker Secrets (if present)</li>
 *   <li>{@link SystemEnvDataSourceStrategy} (the default)</li>
 *   <li>{@link TomcatDataSourceStrategy} (fallback on failure)</li>
 * </ol>
 */
class DataSourceStrategyProviderTest {

    private static final String STRATEGY_PROPERTY = "DATASOURCE_PROVIDER_STRATEGY_CLASS";

    private DBPropertiesDataSourceStrategy mockDbProperties;
    private DockerSecretDataSourceStrategy mockDockerSecret;
    private SystemEnvDataSourceStrategy mockSystemEnv;
    private TomcatDataSourceStrategy mockTomcat;

    private DataSource dsDbProps;
    private DataSource dsDockerSecret;
    private DataSource dsSystemEnv;
    private DataSource dsTomcat;

    @BeforeEach
    void setUp() {
        mockDbProperties = mock(DBPropertiesDataSourceStrategy.class);
        mockDockerSecret = mock(DockerSecretDataSourceStrategy.class);
        mockSystemEnv = mock(SystemEnvDataSourceStrategy.class);
        mockTomcat = mock(TomcatDataSourceStrategy.class);

        dsDbProps = mock(DataSource.class);
        dsDockerSecret = mock(DataSource.class);
        dsSystemEnv = mock(DataSource.class);
        dsTomcat = mock(DataSource.class);

        when(mockDbProperties.apply()).thenReturn(dsDbProps);
        when(mockDockerSecret.apply()).thenReturn(dsDockerSecret);
        when(mockSystemEnv.apply()).thenReturn(dsSystemEnv);
        when(mockTomcat.apply()).thenReturn(dsTomcat);
    }

    @AfterEach
    void resetProperty() {
        Config.setProperty(STRATEGY_PROPERTY, null);
    }

    private DataSourceStrategyProvider createTestProvider() {
        return new DataSourceStrategyProvider() {
            @Override
            DBPropertiesDataSourceStrategy getDBPropertiesInstance() {
                return mockDbProperties;
            }

            @Override
            DockerSecretDataSourceStrategy getDockerSecretDataSourceInstance() {
                return mockDockerSecret;
            }

            @Override
            SystemEnvDataSourceStrategy getSystemEnvDataSourceInstance() {
                return mockSystemEnv;
            }

            @Override
            TomcatDataSourceStrategy getTomcatDataSourceInstance() {
                return mockTomcat;
            }
        };
    }

    @Test
    void defaultsToSystemEnvDataSourceStrategy() {
        Config.setProperty(STRATEGY_PROPERTY, null);

        assertEquals(SystemEnvDataSourceStrategy.class.getName(),
                DataSourceStrategyProvider.getInstance().getCustomDataSourceProvider());
    }

    @Test
    void explicitConfigurationOverridesTheDefault() {
        Config.setProperty(STRATEGY_PROPERTY, "com.dotmarketing.db.TomcatDataSourceStrategy");

        assertEquals("com.dotmarketing.db.TomcatDataSourceStrategy",
                DataSourceStrategyProvider.getInstance().getCustomDataSourceProvider());
    }

    @Test
    void get_defaultsToSystemEnvWhenNoOtherConfigPresent() throws Exception {
        Config.setProperty(STRATEGY_PROPERTY, null);
        when(mockDbProperties.existsDBPropertiesFile()).thenReturn(false);
        when(mockDockerSecret.dockerSecretPathExists()).thenReturn(false);

        DataSourceStrategyProvider provider = createTestProvider();
        DataSource result = provider.get();

        assertSame(dsSystemEnv, result);
        verify(mockSystemEnv).apply();
        verify(mockDbProperties, never()).apply();
        verify(mockDockerSecret, never()).apply();
    }

    @Test
    void get_dbPropertiesTakesPrecedenceOverDefault() throws Exception {
        Config.setProperty(STRATEGY_PROPERTY, null);
        when(mockDbProperties.existsDBPropertiesFile()).thenReturn(true);

        DataSourceStrategyProvider provider = createTestProvider();
        DataSource result = provider.get();

        assertSame(dsDbProps, result);
        verify(mockDbProperties).apply();
        verify(mockSystemEnv, never()).apply();
        verify(mockDockerSecret, never()).apply();
    }

    @Test
    void get_dockerSecretTakesPrecedenceOverDefaultWhenNoDbProperties() throws Exception {
        Config.setProperty(STRATEGY_PROPERTY, null);
        when(mockDbProperties.existsDBPropertiesFile()).thenReturn(false);
        when(mockDockerSecret.dockerSecretPathExists()).thenReturn(true);

        DataSourceStrategyProvider provider = createTestProvider();
        DataSource result = provider.get();

        assertSame(dsDockerSecret, result);
        verify(mockDockerSecret).apply();
        verify(mockSystemEnv, never()).apply();
        verify(mockDbProperties, never()).apply();
    }

    @Test
    void get_fallbackToTomcatWhenStrategyThrowsException() throws Exception {
        Config.setProperty(STRATEGY_PROPERTY, null);
        when(mockDbProperties.existsDBPropertiesFile()).thenReturn(false);
        when(mockDockerSecret.dockerSecretPathExists()).thenReturn(false);
        when(mockSystemEnv.apply()).thenThrow(new RuntimeException("Connection failed"));

        DataSourceStrategyProvider provider = createTestProvider();
        DataSource result = provider.get();

        assertSame(dsTomcat, result);
        verify(mockTomcat).apply();
    }
}
