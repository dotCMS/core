package com.dotmarketing.db;

import static com.dotmarketing.db.DataSourceStrategyProvider.CONNECTION_DB_BASE_URL;
import static com.dotmarketing.db.DataSourceStrategyProvider.CONNECTION_DB_DEFAULT_TRANSACTION_ISOLATION;
import static com.dotmarketing.db.DataSourceStrategyProvider.CONNECTION_DB_DRIVER;
import static com.dotmarketing.db.DataSourceStrategyProvider.CONNECTION_DB_LEAK_DETECTION_THRESHOLD;
import static com.dotmarketing.db.DataSourceStrategyProvider.CONNECTION_DB_MAX_IDLE;
import static com.dotmarketing.db.DataSourceStrategyProvider.CONNECTION_DB_MAX_TOTAL;
import static com.dotmarketing.db.DataSourceStrategyProvider.CONNECTION_DB_MAX_WAIT;
import static com.dotmarketing.db.DataSourceStrategyProvider.CONNECTION_DB_PASSWORD;
import static com.dotmarketing.db.DataSourceStrategyProvider.CONNECTION_DB_USERNAME;
import static com.dotmarketing.db.DataSourceStrategyProvider.CONNECTION_DB_VALIDATION_QUERY;

import com.dotmarketing.util.Constants;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.SystemEnvironmentProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;


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
                : "jdbc:postgresql://localhost/dotcms");

        config.setUsername(systemEnvironmentProperties.getVariable(CONNECTION_DB_USERNAME));

        config.setPassword(systemEnvironmentProperties.getVariable(CONNECTION_DB_PASSWORD));

        config.setMaximumPoolSize(Integer.parseInt(
                systemEnvironmentProperties.getVariable(CONNECTION_DB_MAX_TOTAL) != null
                        ? systemEnvironmentProperties.getVariable(CONNECTION_DB_MAX_TOTAL)
                        : "60"));

        config.setIdleTimeout(
                Integer.parseInt(
                        systemEnvironmentProperties.getVariable(CONNECTION_DB_MAX_IDLE) != null
                                ? systemEnvironmentProperties.getVariable(CONNECTION_DB_MAX_IDLE)
                                : "10")
                        * 1000);

        config.setMaxLifetime(Integer.parseInt(
                systemEnvironmentProperties.getVariable(CONNECTION_DB_MAX_WAIT) != null
                        ? systemEnvironmentProperties.getVariable(CONNECTION_DB_MAX_WAIT)
                        : "60000"));

        config.setConnectionTestQuery(
                systemEnvironmentProperties.getVariable(CONNECTION_DB_VALIDATION_QUERY) != null
                        ? systemEnvironmentProperties.getVariable(CONNECTION_DB_VALIDATION_QUERY)
                        : "SELECT 1");

        // This property controls the amount of time that a connection can be out of the pool before a message
        // is logged indicating a possible connection leak. A value of 0 means leak detection is disabled.
        // Lowest acceptable value for enabling leak detection is 2000 (2 seconds). Default: 0
        config.setLeakDetectionThreshold(Integer.parseInt(
                systemEnvironmentProperties.getVariable(CONNECTION_DB_LEAK_DETECTION_THRESHOLD)
                        != null ? systemEnvironmentProperties
                        .getVariable(CONNECTION_DB_LEAK_DETECTION_THRESHOLD) : "60000"));

        config.setTransactionIsolation(systemEnvironmentProperties
                .getVariable(CONNECTION_DB_DEFAULT_TRANSACTION_ISOLATION));

        return new HikariDataSource(config);
    }
}
