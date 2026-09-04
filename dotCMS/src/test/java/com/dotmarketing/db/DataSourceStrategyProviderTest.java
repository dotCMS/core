package com.dotmarketing.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dotmarketing.util.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the default datasource strategy resolution.
 *
 * <p>The default must be {@link SystemEnvDataSourceStrategy} so that {@code DB_*}
 * environment variables work out of the box without operators having to set
 * {@code DATASOURCE_PROVIDER_STRATEGY_CLASS} (see issue #34067), while an
 * explicit configuration (as used by the integration test harness) still takes
 * precedence.
 */
class DataSourceStrategyProviderTest {

    private static final String STRATEGY_PROPERTY = "DATASOURCE_PROVIDER_STRATEGY_CLASS";

    @AfterEach
    void resetProperty() {
        Config.setProperty(STRATEGY_PROPERTY, null);
    }

    /**
     * Method to test: {@link DataSourceStrategyProvider#getCustomDataSourceProvider()}
     * Test case: {@code DATASOURCE_PROVIDER_STRATEGY_CLASS} is not configured
     * Expected result: defaults to {@link SystemEnvDataSourceStrategy}, so DB connection
     * properties are read from the {@code DB_*} environment variables by default
     */
    @Test
    void defaultsToSystemEnvDataSourceStrategy() {
        Config.setProperty(STRATEGY_PROPERTY, null);

        assertEquals(SystemEnvDataSourceStrategy.class.getName(),
                DataSourceStrategyProvider.getInstance().getCustomDataSourceProvider());
    }

    /**
     * Method to test: {@link DataSourceStrategyProvider#getCustomDataSourceProvider()}
     * Test case: {@code DATASOURCE_PROVIDER_STRATEGY_CLASS} is explicitly configured
     * (as the integration test harness does)
     * Expected result: the configured strategy class wins over the default
     */
    @Test
    void explicitConfigurationOverridesTheDefault() {
        Config.setProperty(STRATEGY_PROPERTY, "com.dotmarketing.db.TomcatDataSourceStrategy");

        assertEquals("com.dotmarketing.db.TomcatDataSourceStrategy",
                DataSourceStrategyProvider.getInstance().getCustomDataSourceProvider());
    }

    /**
     * Method to test: {@link DataSourceStrategyProvider#getCustomDataSourceProvider()}
     * Test case: {@code DATASOURCE_PROVIDER_STRATEGY_CLASS} is explicitly set to an
     * empty value (the documented escape hatch for deployments that rely on the
     * legacy resolution order: db.properties, then env vars, then Docker Secrets,
     * then context.xml)
     * Expected result: the empty value is passed through, and since it is treated as
     * "not set" by {@code UtilMethods.isSet}, the ordered fallback chain engages
     */
    @Test
    void explicitEmptyValueEngagesTheOrderedFallbackChain() {
        Config.setProperty(STRATEGY_PROPERTY, "");

        assertEquals("",
                DataSourceStrategyProvider.getInstance().getCustomDataSourceProvider());
    }
}