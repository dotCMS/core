package com.dotmarketing.db;

import com.dotmarketing.util.Constants;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.SystemEnvironmentProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

import static com.dotmarketing.db.DataSourceStrategyProvider.*;


/**
 * Singleton class that provides a DataSource reading properties from system environment
 * @author nollymar
 */
public class SystemEnvDataSourceStrategy implements DotDataSourceStrategy {

    private static SystemEnvironmentProperties systemEnvironmentProperties;

    @VisibleForTesting
    SystemEnvDataSourceStrategy(final SystemEnvironmentProperties systemEnvironmentProperties){
        this.systemEnvironmentProperties = systemEnvironmentProperties;
    }

    @VisibleForTesting
    SystemEnvDataSourceStrategy(){
        systemEnvironmentProperties = new SystemEnvironmentProperties();
    }

    private static class SingletonHelper{
        private static final SystemEnvDataSourceStrategy INSTANCE = new SystemEnvDataSourceStrategy();
    }

    public static SystemEnvDataSourceStrategy getInstance(){
        return SingletonHelper.INSTANCE;
    }

    @Override
    public DataSource apply() {

        final HikariConfig config = new HikariConfig();

        config.setPoolName(Constants.DATABASE_DEFAULT_DATASOURCE);

        config.setDriverClassName(
                systemEnvironmentProperties.getVariable(CONNECTION_DB_DRIVER) != null
                        ? systemEnvironmentProperties.getVariable(CONNECTION_DB_DRIVER)
                        : "org.postgresql.Driver");

        config.setJdbcUrl(systemEnvironmentProperties.getVariable(CONNECTION_DB_BASE_URL) != null
                ? systemEnvironmentProperties.getVariable(CONNECTION_DB_BASE_URL)
                : "jdbc:postgresql://db.dotcms.site/dotcms");

        config.setUsername(systemEnvironmentProperties.getVariable(CONNECTION_DB_USERNAME));

        config.setPassword(systemEnvironmentProperties.getVariable(CONNECTION_DB_PASSWORD));

        config.setMaximumPoolSize(Integer.parseInt(
                systemEnvironmentProperties.getVariable(CONNECTION_DB_MAX_TOTAL) != null
                        ? systemEnvironmentProperties.getVariable(CONNECTION_DB_MAX_TOTAL)
                        : "60"));
        
        config.setMinimumIdle(Integer.parseInt(
                        systemEnvironmentProperties.getVariable(CONNECTION_DB_MIN_IDLE) != null
                                ? systemEnvironmentProperties.getVariable(CONNECTION_DB_MIN_IDLE)
                                : "10"));
        

        config.setIdleTimeout(
                Long.parseLong(
                        systemEnvironmentProperties.getVariable(CONNECTION_DB_IDLE_TIMEOUT) != null
                                ? systemEnvironmentProperties.getVariable(CONNECTION_DB_IDLE_TIMEOUT)
                                : "600000"));

        config.setConnectionTimeout(
                Integer.parseInt(
                        systemEnvironmentProperties.getVariable(CONNECTION_DB_CONNECTION_TIMEOUT) != null
                                ? systemEnvironmentProperties.getVariable(CONNECTION_DB_CONNECTION_TIMEOUT)
                                : "5000"));

        config.setMaxLifetime(Integer.parseInt(
                systemEnvironmentProperties.getVariable(CONNECTION_DB_MAX_WAIT) != null
                        ? systemEnvironmentProperties.getVariable(CONNECTION_DB_MAX_WAIT)
                        : "60000"));

        config.setConnectionTestQuery(
                systemEnvironmentProperties.getVariable(CONNECTION_DB_VALIDATION_QUERY));

        // This property controls the amount of time that a connection can be out of the pool before a message
        // is logged indicating a possible connection leak. A value of 0 means leak detection is disabled.
        // Lowest acceptable value for enabling leak detection is 2000 (2 seconds). Default: 0
        config.setLeakDetectionThreshold(Integer.parseInt(
                systemEnvironmentProperties.getVariable(CONNECTION_DB_LEAK_DETECTION_THRESHOLD)
                        != null ? systemEnvironmentProperties
                        .getVariable(CONNECTION_DB_LEAK_DETECTION_THRESHOLD) : "300000"));

        config.setTransactionIsolation(systemEnvironmentProperties
                .getVariable(CONNECTION_DB_DEFAULT_TRANSACTION_ISOLATION));


        config.setValidationTimeout(Integer.parseInt(
                systemEnvironmentProperties.getVariable(CONNECTION_DB_VALIDATION_TIMEOUT)
                        != null ? systemEnvironmentProperties
                        .getVariable(CONNECTION_DB_VALIDATION_TIMEOUT) : "5000"));

        config.setRegisterMbeans(com.dotmarketing.util.Config.getBooleanProperty("hikari.register.mbeans", true));

        return new HikariDataSource(config);
    }
}
