package com.dotmarketing.db;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.SystemEnvironmentProperties;

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

    private static SystemEnvironmentProperties systemEnvironmentProperties;

    @VisibleForTesting
    DataSourceStrategyProvider(){
        systemEnvironmentProperties = new SystemEnvironmentProperties();
    }

    private static class SingletonHelper{
        private static final DataSourceStrategyProvider INSTANCE = new DataSourceStrategyProvider();
    }

    public static DataSourceStrategyProvider getInstance(){
        return SingletonHelper.INSTANCE;
    }

    /**
     * Method that loads a datasource from a custom implementation if <b>DATASOURCE_PROVIDER_STRATEGY_CLASS</b> property
     * is defined. Otherwise, the datasource is initialized using any of these implementations (respecting order):
     * <ol>
     *     <li>A {@code db.properties} file in {@code /WEB-INF/classes} implemented by
     *     {@link DBPropertiesDataSourceStrategy}</li>
     *     <li>Configuration is taken from environment variables implemented by {@link SystemEnvDataSourceStrategy}</li>
     *     <li>Getting Docker Secrets if set. Implementation: {@link DockerSecretDataSourceStrategy}</li>
     *     <li>A {@code context.xml} file in {@code /META-INF/}. Implementation: {@link TomcatDataSourceStrategy}</li>
     * </ol>
     * It's also worth noting that creating the {@link DataSource} instance requires the default {@link TimeZone} object
     * to be set to {@code UTC}. Otherwise, the default Time Zone will be used instead and several database operations
     * will not result as expected. Once the Data Source instance is created, the default Time Zone value will be
     * brought back.
     *
     * @return The {@link DataSource} singleton.
     *
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public DataSource get()
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {

        DataSource defaultDataSource = null;

        final SystemEnvironmentProperties systemEnvironmentProperties = getSystemEnvironmentProperties();

        final String providerClassName = getCustomDataSourceProvider();
        try {
            if (!UtilMethods.isSet(providerClassName)) {
                if (getDBPropertiesInstance()
                        .existsDBPropertiesFile()) {
                    defaultDataSource = getDBPropertiesInstance()
                            .apply();
                    Logger.info(DataSourceStrategyProvider.class,
                            "Datasource loaded from db.properties file");
                } else if (systemEnvironmentProperties.getVariable(CONNECTION_DB_BASE_URL)
                        != null) {
                    defaultDataSource = getSystemEnvDataSourceInstance()
                            .apply();
                    Logger.info(DataSourceStrategyProvider.class,
                            "Datasource loaded from system environment");
                } else if (getDockerSecretDataSourceInstance().dockerSecretPathExists()) {
                    defaultDataSource = getDockerSecretDataSourceInstance()
                            .apply();
                    Logger.info(DataSourceStrategyProvider.class,
                            "Datasource loaded from Docker Secret");
                }
            } else {
                DotDataSourceStrategy customStrategy = ((Class<DotDataSourceStrategy>) Class
                        .forName(providerClassName)).getDeclaredConstructor().newInstance();
                defaultDataSource = customStrategy.apply();

                Logger.info(DataSourceStrategyProvider.class,
                        "Datasource loaded using custom class " + providerClassName);
            }

        } catch(Exception e) {
            Logger.warnAndDebug(DataSourceStrategyProvider.class,
                    "Error initializing datasource. Reason: " + e.getMessage()
                            + "Trying to load datasource from context.xml ...", e);
        } finally {
            if (null == defaultDataSource) {
                defaultDataSource = getTomcatDataSourceInstance()
                        .apply();
                Logger.info(DataSourceStrategyProvider.class,
                        "Datasource loaded from context.xml");
            }
        }

        return defaultDataSource;
    }

    @VisibleForTesting
    String getCustomDataSourceProvider() {
        return Config
                .getStringProperty("DATASOURCE_PROVIDER_STRATEGY_CLASS", null);
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
