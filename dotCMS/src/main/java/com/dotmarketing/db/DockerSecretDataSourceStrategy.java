package com.dotmarketing.db;


import com.dotmarketing.util.Constants;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.SystemEnvironmentProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Singleton class that provides a datasource using docker secret configuration
 * If <b>DOCKER_SECRET_FILE_PATH</b> environment variable is set, secrets credential will be taken from this file.
 * Otherwise, they will be read from <b>/run/secrets/</b> path
 * @author nollymar
 */
public class DockerSecretDataSourceStrategy implements DotDataSourceStrategy {

    private static SystemEnvironmentProperties systemEnvironmentProperties;

    public static final String DOCKER_SECRET_FILE_PATH_PROPERTY = "DOCKER_SECRET_FILE_PATH";

    @VisibleForTesting
    DockerSecretDataSourceStrategy(){
        systemEnvironmentProperties = new SystemEnvironmentProperties();
    }

    @VisibleForTesting
    DockerSecretDataSourceStrategy(final SystemEnvironmentProperties systemEnvironmentProperties){
        this.systemEnvironmentProperties = systemEnvironmentProperties;
    }


    private static class SingletonHelper{
        private static final DockerSecretDataSourceStrategy INSTANCE = new DockerSecretDataSourceStrategy();
    }

    public static DockerSecretDataSourceStrategy getInstance(){
        return SingletonHelper.INSTANCE;
    }

    public boolean dockerSecretPathExists(){

        final File secretsDir = new File(DockerSecretsUtil.SECRETS_DIR);
        return systemEnvironmentProperties.getVariable(DOCKER_SECRET_FILE_PATH_PROPERTY) != null
                || (secretsDir.exists() && secretsDir.canRead());
    }

    @Override
    public DataSource apply() {

        Map<String, String> dockerSecretsMap;

        if (systemEnvironmentProperties.getVariable(DOCKER_SECRET_FILE_PATH_PROPERTY) != null) {
            dockerSecretsMap = DockerSecretsUtil
                    .loadFromFile(systemEnvironmentProperties.getVariable(DOCKER_SECRET_FILE_PATH_PROPERTY));
        } else {
            dockerSecretsMap = DockerSecretsUtil.load();
        }

        final HikariConfig config = getHikariConfig(dockerSecretsMap);

        dockerSecretsMap.clear();
        return new HikariDataSource(config);
    }

    @VisibleForTesting
    HikariConfig getHikariConfig(final Map<String, String> dockerSecretsMap) {
        final HikariConfig config = new HikariConfig();

        config.setPoolName(Constants.DATABASE_DEFAULT_DATASOURCE);
        config.setDriverClassName(dockerSecretsMap.getOrDefault(
                DataSourceStrategyProvider.CONNECTION_DB_DRIVER, "org.postgresql.Driver"));
        config.setJdbcUrl(dockerSecretsMap.getOrDefault(
                DataSourceStrategyProvider.CONNECTION_DB_BASE_URL, "jdbc:postgresql://localhost/dotcms"));
        config.setUsername(dockerSecretsMap.get(DataSourceStrategyProvider.CONNECTION_DB_USERNAME));
        config.setPassword(dockerSecretsMap.get(DataSourceStrategyProvider.CONNECTION_DB_PASSWORD));
        config.setMaximumPoolSize(Integer.parseInt(
                dockerSecretsMap.getOrDefault(DataSourceStrategyProvider.CONNECTION_DB_MAX_TOTAL, "60")));
        
        config.setMinimumIdle(Integer.parseInt(
                dockerSecretsMap.getOrDefault(DataSourceStrategyProvider.CONNECTION_DB_MIN_IDLE, "10")));
        
        // ConnectionTimeout: Time in milliseconds to wait for a connection from the pool
        // Default: 30 seconds (30000ms)
        config.setConnectionTimeout(Integer.parseInt(
                dockerSecretsMap.getOrDefault(DataSourceStrategyProvider.CONNECTION_DB_CONNECTION_TIMEOUT, "30000")));
        
        // IdleTimeout: Time in milliseconds that a connection can sit idle in the pool
        // Default: 10 minutes (600000ms)
        config.setIdleTimeout(Integer.parseInt(
                dockerSecretsMap.getOrDefault(DataSourceStrategyProvider.CONNECTION_DB_IDLE_TIMEOUT, "600000")));
        
        // MaxLifetime: Maximum lifetime of a connection in the pool in milliseconds
        // Default: 30 minutes (1800000ms) - should be less than database connection timeout
        config.setMaxLifetime(Integer.parseInt(
                dockerSecretsMap.getOrDefault(DataSourceStrategyProvider.CONNECTION_DB_MAX_WAIT, "1800000")));
        config.setConnectionTestQuery(dockerSecretsMap.get(
                DataSourceStrategyProvider.CONNECTION_DB_VALIDATION_QUERY));

        // LeakDetectionThreshold: Time in milliseconds that a connection can be out of the pool 
        // before a message is logged indicating a possible connection leak. 
        // A value of 0 means leak detection is disabled.
        // Lowest acceptable value for enabling leak detection is 2000 (2 seconds).
        // Default: 60 seconds (60000ms) - reasonable for detecting actual leaks
        config.setLeakDetectionThreshold(Integer.parseInt(dockerSecretsMap
                .getOrDefault(DataSourceStrategyProvider.CONNECTION_DB_LEAK_DETECTION_THRESHOLD, "60000")));

        config.setTransactionIsolation(
                    dockerSecretsMap.get(
                            DataSourceStrategyProvider.CONNECTION_DB_DEFAULT_TRANSACTION_ISOLATION));
        return config;
    }
}
