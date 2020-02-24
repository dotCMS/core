package com.dotmarketing.db;

import com.dotmarketing.util.Constants;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.SystemEnvironmentProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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

    private DockerSecretDataSourceStrategy(){
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

    @Override
    public DataSource apply() {

        Map<String, String> dockerSecretsMap;

        if (systemEnvironmentProperties.getVariable("DOCKER_SECRET_FILE_PATH") != null) {
            dockerSecretsMap = DockerSecretsUtil
                    .loadFromFile(systemEnvironmentProperties.getVariable("DOCKER_SECRET_FILE_PATH"));
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
        config.setDriverClassName(dockerSecretsMap.getOrDefault("connection_db_driver", "org.postgresql.Driver"));
        config.setJdbcUrl(dockerSecretsMap.getOrDefault("connection_db_base_url", "jdbc:postgresql://localhost/dotcms"));
        config.setUsername(dockerSecretsMap.get("connection_db_username"));
        config.setPassword(dockerSecretsMap.get("connection_db_password"));
        config.setMaximumPoolSize(Integer.parseInt(
                dockerSecretsMap.getOrDefault("connection_db_max_total", "60")));
        config.setIdleTimeout(
                Integer.parseInt(dockerSecretsMap.getOrDefault("connection_db_max_idle", "10"))
                        * 1000);
        config.setMaxLifetime(Integer.parseInt(
                dockerSecretsMap.getOrDefault("connection_db_max_wait", "60000")));
        config.setConnectionTestQuery(dockerSecretsMap.getOrDefault("connection_db_validation_query", "SELECT 1"));

        // This property controls the amount of time that a connection can be out of the pool before a message
        // is logged indicating a possible connection leak. A value of 0 means leak detection is disabled.
        // Lowest acceptable value for enabling leak detection is 2000 (2 seconds). Default: 0
        config.setLeakDetectionThreshold(Integer.parseInt(dockerSecretsMap
                .getOrDefault("connection_db_leak_detection_threshold", "60000")));

        config.setTransactionIsolation(
                    dockerSecretsMap.get("connection_db_default_transaction_isolation"));
        return config;
    }
}
