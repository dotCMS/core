package com.dotmarketing.db;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.SystemEnvironmentProperties;
import javax.sql.DataSource;

/**
 * Class used to obtain a valid DataSource strategy provider
 * @author nollymar
 */
public class DataSourceStrategyProvider {

    static final String CONNECTION_DB_DRIVER = "connection_db_driver";
    static final String CONNECTION_DB_BASE_URL = "connection_db_base_url";
    static final String CONNECTION_DB_USERNAME = "connection_db_username";
    static final String CONNECTION_DB_PASSWORD = "connection_db_password";
    static final String CONNECTION_DB_MAX_WAIT = "connection_db_max_wait";
    static final String CONNECTION_DB_MAX_TOTAL = "connection_db_max_total";
    static final String CONNECTION_DB_MAX_IDLE = "connection_db_max_idle";
    static final String CONNECTION_DB_VALIDATION_QUERY = "connection_db_validation_query";
    static final String CONNECTION_DB_LEAK_DETECTION_THRESHOLD = "connection_db_leak_detection_threshold";
    static final String CONNECTION_DB_DEFAULT_TRANSACTION_ISOLATION = "connection_db_default_transaction_isolation";
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
     * Method that loads a datasource from a custom implementation if <b>DATASOURCE_PROVIDER_STRATEGY_CLASS</b>
     * property is defined. Otherwise, the datasource is initialized using any of these implementations (respecting order):<br>
     * 1. A db.properties file in WEB-INF/classes implemented by {@link DBPropertiesDataSourceStrategy}<br>
     * 2. Configuration is taken from environment variables implemented by {@link SystemEnvDataSourceStrategy}<br>
     * 3. Getting Docker Secrets if set. Implementation: {@link DockerSecretDataSourceStrategy}<br>
     * 4. A context.xml file in META-INF. Implementation: {@link TomcatDataSourceStrategy}
     *
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
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
                } else if (systemEnvironmentProperties.getVariable("connection_db_base_url")
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
                        .forName(providerClassName)).newInstance();
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
