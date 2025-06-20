package com.dotmarketing.db;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.SystemEnvironmentProperties;
import com.dotcms.test.TestUtil;

import javax.sql.DataSource;
import java.util.TimeZone;

/**
 * Class used to obtain a valid DataSource strategy provider
 * @author nollymar
 */
public class DataSourceStrategyProvider {

    static final String CONNECTION_DB_DRIVER = "DB_DRIVER";
    static final String CONNECTION_DB_BASE_URL = "DB_BASE_URL";
    static final String CONNECTION_DB_USERNAME = "DB_USERNAME";
    static final String CONNECTION_DB_PASSWORD = "DB_PASSWORD";
    static final String CONNECTION_DB_MAX_WAIT = "DB_MAXWAIT";
    static final String CONNECTION_DB_MAX_TOTAL = "DB_MAX_TOTAL";
    static final String CONNECTION_DB_MIN_IDLE = "DB_MIN_IDLE";
    static final String CONNECTION_DB_CONNECTION_TIMEOUT = "DB_CONNECTION_TIMEOUT";
    static final String CONNECTION_DB_IDLE_TIMEOUT = "DB_IDLE_TIMEOUT";
    static final String CONNECTION_DB_VALIDATION_QUERY = "DB_VALIDATION_QUERY";
    static final String CONNECTION_DB_LEAK_DETECTION_THRESHOLD = "DB_LEAK_DETECTION_THRESHOLD";
    static final String CONNECTION_DB_DEFAULT_TRANSACTION_ISOLATION = "DB_DEFAULT_TRANSACTION_ISOLATION";

    static final String CONNECTION_DB_VALIDATION_TIMEOUT = "DB_DEFAULT_VALIDATION_TIMEOUT";
    static final String DATASOURCE_STRATEGY = "DATASOURCE_STRATEGY";

    // Strategy type constants
    static final String STRATEGY_PROPERTIES = "properties";
    static final String STRATEGY_ENVIRONMENT = "environment";
    static final String STRATEGY_DOCKER_SECRETS = "docker-secrets";
    static final String STRATEGY_TOMCAT = "tomcat";

    private final SystemEnvironmentProperties systemEnvironmentProperties;

    @VisibleForTesting
    DataSourceStrategyProvider(){
        this.systemEnvironmentProperties = new SystemEnvironmentProperties();
    }

    private static class SingletonHelper{
        private static final DataSourceStrategyProvider INSTANCE = new DataSourceStrategyProvider();
    }

    public static DataSourceStrategyProvider getInstance(){
        return SingletonHelper.INSTANCE;
    }

    /**
     * Method that loads a datasource using explicit strategy selection.
     * 
     * Strategy selection (in order of precedence):
     * <ol>
     *     <li>Custom implementation via <b>DATASOURCE_PROVIDER_STRATEGY_CLASS</b> property
     *         (environment variable: <b>DOT_DATASOURCE_PROVIDER_STRATEGY_CLASS</b>)</li>
     *     <li>Explicit strategy via <b>DATASOURCE_STRATEGY</b> property
     *         (environment variable: <b>DOT_DATASOURCE_STRATEGY</b>):
     *         <ul>
     *             <li>"properties" - Uses {@code db.properties} file ({@link DBPropertiesDataSourceStrategy})</li>
     *             <li>"environment" - Uses environment variables ({@link SystemEnvDataSourceStrategy})</li>
     *             <li>"docker-secrets" - Uses Docker secrets ({@link DockerSecretDataSourceStrategy})</li>
     *             <li>"tomcat" - Uses {@code context.xml} ({@link TomcatDataSourceStrategy})</li>
     *         </ul>
     *     </li>
     *     <li>Auto-detection (legacy behavior):
     *         <ul>
     *             <li>db.properties file if exists</li>
     *             <li>Environment variables if DB_BASE_URL (or DOT_DB_BASE_URL) is set</li>
     *             <li>Docker secrets if path exists</li>
     *             <li>Tomcat context.xml as last resort</li>
     *         </ul>
     *     </li>
     * </ol>
     * 
     * It's also worth noting that creating the {@link DataSource} instance requires the default {@link TimeZone} object
     * to be set to {@code UTC}. Otherwise, the default Time Zone will be used instead and several database operations
     * will not result as expected. Once the Data Source instance is created, the default Time Zone value will be
     * brought back.
     *
     * @return The {@link DataSource} singleton.
     *
     * @throws RuntimeException If no valid datasource strategy can be determined or applied
     */
    public DataSource get() {
        
        // Circuit breaker: Check if database configuration is available before attempting connections
        if (!isDatabaseConfigurationAvailable()) {
            throw new RuntimeException("Database configuration not available. " +
                "This is expected in unit test environments where no database is configured. " +
                "For production environments, ensure database connection properties are properly set.");
        }

        final String customProviderClassName = getCustomDataSourceProvider();
        if (UtilMethods.isSet(customProviderClassName)) {
            return loadCustomDataSource(customProviderClassName);
        }

        final String explicitStrategy = getExplicitStrategy();
        if (UtilMethods.isSet(explicitStrategy)) {
            return loadExplicitStrategy(explicitStrategy);
        }

        return loadAutoDetectedStrategy();
    }

    /**
     * Loads a custom datasource strategy from the specified class name.
     */
    private DataSource loadCustomDataSource(final String providerClassName) {
        try {
            Class<?> strategyClass = Class.forName(providerClassName);
            if (!DotDataSourceStrategy.class.isAssignableFrom(strategyClass)) {
                throw new RuntimeException("Custom datasource class '" + providerClassName + 
                    "' must implement DotDataSourceStrategy interface");
            }
            
            @SuppressWarnings("unchecked")
            Class<? extends DotDataSourceStrategy> typedStrategyClass = 
                (Class<? extends DotDataSourceStrategy>) strategyClass;
            
            DotDataSourceStrategy customStrategy = typedStrategyClass
                .getDeclaredConstructor().newInstance();
            DataSource dataSource = customStrategy.apply();

            Logger.info(DataSourceStrategyProvider.class,
                    "Datasource loaded using custom strategy class: " + providerClassName);
            return dataSource;
        } catch (Exception e) {
            Logger.error(DataSourceStrategyProvider.class,
                    "Failed to load custom datasource strategy: " + providerClassName, e);
            throw new RuntimeException("Failed to load custom datasource strategy: " + providerClassName, e);
        }
    }

    /**
     * Loads datasource using explicitly configured strategy.
     */
    private DataSource loadExplicitStrategy(final String strategy) {
        final String normalizedStrategy = strategy.toLowerCase().trim();
        
        switch (normalizedStrategy) {
            case STRATEGY_PROPERTIES:
                return loadPropertiesStrategy();
            case STRATEGY_ENVIRONMENT:
                return loadEnvironmentStrategy();
            case STRATEGY_DOCKER_SECRETS:
                return loadDockerSecretsStrategy();
            case STRATEGY_TOMCAT:
                return loadTomcatStrategy();
            default:
                throw new RuntimeException("Unknown datasource strategy: '" + strategy + 
                    "'. Valid values are: " + STRATEGY_PROPERTIES + ", " + STRATEGY_ENVIRONMENT + 
                    ", " + STRATEGY_DOCKER_SECRETS + ", " + STRATEGY_TOMCAT);
        }
    }

    /**
     * Auto-detects and loads datasource strategy using legacy behavior.
     */
    private DataSource loadAutoDetectedStrategy() {
        final SystemEnvironmentProperties systemEnvironmentProperties = getSystemEnvironmentProperties();

        // Try db.properties first
        if (getDBPropertiesInstance().existsDBPropertiesFile()) {
            return loadPropertiesStrategy();
        }
        
        // Try environment variables
        if (systemEnvironmentProperties.getVariable(CONNECTION_DB_BASE_URL) != null) {
            return loadEnvironmentStrategy();
        }
        
        // Try Docker secrets
        if (getDockerSecretDataSourceInstance().dockerSecretPathExists()) {
            return loadDockerSecretsStrategy();
        }
        
        // Fallback to Tomcat as last resort
        Logger.warn(DataSourceStrategyProvider.class,
                "No explicit datasource configuration found. Falling back to Tomcat context.xml. " +
                "Consider setting DATASOURCE_STRATEGY property for explicit configuration.");
        return loadTomcatStrategy();
    }

    private DataSource loadPropertiesStrategy() {
        try {
            DataSource dataSource = getDBPropertiesInstance().apply();
            Logger.info(DataSourceStrategyProvider.class, "Datasource loaded from db.properties file");
            return dataSource;
        } catch (Exception e) {
            Logger.error(DataSourceStrategyProvider.class, "Failed to load datasource from db.properties", e);
            throw new RuntimeException("Failed to load datasource from db.properties file", e);
        }
    }

    private DataSource loadEnvironmentStrategy() {
        try {
            DataSource dataSource = getSystemEnvDataSourceInstance().apply();
            Logger.info(DataSourceStrategyProvider.class, "Datasource loaded from system environment");
            return dataSource;
        } catch (Exception e) {
            Logger.error(DataSourceStrategyProvider.class, "Failed to load datasource from environment variables", e);
            throw new RuntimeException("Failed to load datasource from environment variables", e);
        }
    }

    private DataSource loadDockerSecretsStrategy() {
        try {
            DataSource dataSource = getDockerSecretDataSourceInstance().apply();
            Logger.info(DataSourceStrategyProvider.class, "Datasource loaded from Docker secrets");
            return dataSource;
        } catch (Exception e) {
            Logger.error(DataSourceStrategyProvider.class, "Failed to load datasource from Docker secrets", e);
            throw new RuntimeException("Failed to load datasource from Docker secrets", e);
        }
    }

    private DataSource loadTomcatStrategy() {
        try {
            DataSource dataSource = getTomcatDataSourceInstance().apply();
            Logger.info(DataSourceStrategyProvider.class, "Datasource loaded from context.xml");
            return dataSource;
        } catch (Exception e) {
            Logger.error(DataSourceStrategyProvider.class, "Failed to load datasource from context.xml", e);
            throw new RuntimeException("Failed to load datasource from context.xml", e);
        }
    }

    @VisibleForTesting
    String getCustomDataSourceProvider() {
        return Config
                .getStringProperty("DATASOURCE_PROVIDER_STRATEGY_CLASS", null);
    }

    @VisibleForTesting
    String getExplicitStrategy() {
        return Config.getStringProperty(DATASOURCE_STRATEGY, null);
    }

    /**
     * Circuit breaker to check if database configuration appears to be available.
     * Returns false in unit test environments where database is typically not configured.
     * This prevents unnecessary connection attempts and associated error logging.
     */
    private static boolean isDatabaseConfigurationAvailable() {
        // In unit tests, assume database is not available to prevent connection attempts
        if (TestUtil.isUnitTest()) {
            Logger.debug(DataSourceStrategyProvider.class, 
                "Unit test environment detected - skipping database configuration check");
            return false;
        }
        
        // Check if we have basic database configuration
        final SystemEnvironmentProperties env = getInstance().getSystemEnvironmentProperties();
        
        // For environment strategy, check if DB_BASE_URL is configured
        if (env.getVariable(CONNECTION_DB_BASE_URL) != null) {
            return true;
        }
        
        // For properties strategy, check if db.properties file exists
        if (DBPropertiesDataSourceStrategy.getInstance().existsDBPropertiesFile()) {
            return true;
        }
        
        // For Docker secrets, check if secrets path exists
        if (DockerSecretDataSourceStrategy.getInstance().dockerSecretPathExists()) {
            return true;
        }
        
        // For Tomcat strategy, we'll assume it might work (can't easily check JNDI availability)
        return true;
    }

    @VisibleForTesting
    SystemEnvironmentProperties getSystemEnvironmentProperties() {
        return systemEnvironmentProperties;
    }

    @VisibleForTesting
    TomcatDataSourceStrategy getTomcatDataSourceInstance() {
        return TomcatDataSourceStrategy.getInstance();
    }

    @VisibleForTesting
    DockerSecretDataSourceStrategy getDockerSecretDataSourceInstance() {
        return DockerSecretDataSourceStrategy.getInstance();
    }

    @VisibleForTesting
    SystemEnvDataSourceStrategy getSystemEnvDataSourceInstance() {
        return SystemEnvDataSourceStrategy.getInstance();
    }

    @VisibleForTesting
    DBPropertiesDataSourceStrategy getDBPropertiesInstance() {
        return DBPropertiesDataSourceStrategy.getInstance();
    }

}
