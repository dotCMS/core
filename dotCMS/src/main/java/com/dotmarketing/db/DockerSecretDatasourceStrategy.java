package com.dotmarketing.db;

import com.dotmarketing.util.Constants;
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
public class DockerSecretDatasourceStrategy implements DotDatasourceStrategy {

    private DockerSecretDatasourceStrategy(){}

    private static class SingletonHelper{
        private static final DockerSecretDatasourceStrategy INSTANCE = new DockerSecretDatasourceStrategy();
    }

    public static DockerSecretDatasourceStrategy getInstance(){
        return SingletonHelper.INSTANCE;
    }

    @Override
    public DataSource apply() {

        Map<String, String> dockerSecretsMap;

        if (System.getenv("DOCKER_SECRET_FILE_PATH") != null) {
            dockerSecretsMap = DockerSecretsUtil
                    .loadFromFile(System.getenv("DOCKER_SECRET_FILE_PATH"));
        } else {
            dockerSecretsMap = DockerSecretsUtil.load();
        }

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

        if (dockerSecretsMap.get("connection_db_default_transaction_isolation") != null) {
            config.setTransactionIsolation(
                    dockerSecretsMap.get("connection_db_default_transaction_isolation"));
        }

        dockerSecretsMap.clear();
        return new HikariDataSource(config);
    }
}
