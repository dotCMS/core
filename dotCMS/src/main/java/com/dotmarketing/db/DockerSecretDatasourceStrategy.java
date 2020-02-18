package com.dotmarketing.db;

import com.dotcms.repackage.com.zaxxer.hikari.HikariConfig;
import com.dotcms.repackage.com.zaxxer.hikari.HikariDataSource;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Singleton class that provides a datasource using docker secret configuration
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
    public DataSource getDatasource() {

        Map<String, String> dockerSecretsMap;

        try {
            if (System.getenv("DOCKER_SECRET_FILE_PATH") != null) {
                dockerSecretsMap = DockerSecretsUtil
                        .loadFromFile(System.getenv("DOCKER_SECRET_FILE_PATH"));
            } else {
                dockerSecretsMap = DockerSecretsUtil.load();
            }

            final HikariConfig config = new HikariConfig();

            config.setPoolName(Constants.DATABASE_DEFAULT_DATASOURCE);
            config.setDriverClassName(dockerSecretsMap.getOrDefault("connection.db.driver", "org.postgresql.Driver"));
            config.setJdbcUrl(dockerSecretsMap.getOrDefault("connection.db.base.url", "jdbc:postgresql://localhost/dotcms"));
            config.setUsername(dockerSecretsMap.get("connection.db.username"));
            config.setPassword(dockerSecretsMap.get("connection.db.password"));
            config.setMaximumPoolSize(Integer.parseInt(
                    dockerSecretsMap.getOrDefault("connection.db.max.total", "60")));
            config.setIdleTimeout(
                    Integer.parseInt(dockerSecretsMap.getOrDefault("connection.db.max.idle", "10"))
                            * 1000);
            config.setMaxLifetime(Integer.parseInt(
                    dockerSecretsMap.getOrDefault("connection.db.max.wait", "60000")));
            config.setConnectionTestQuery(dockerSecretsMap.getOrDefault("connection.db.validation.query", "SELECT 1"));

            // This property controls the amount of time that a connection can be out of the pool before a message
            // is logged indicating a possible connection leak. A value of 0 means leak detection is disabled.
            // Lowest acceptable value for enabling leak detection is 2000 (2 seconds). Default: 0
            config.setLeakDetectionThreshold(Integer.parseInt(dockerSecretsMap
                    .getOrDefault("connection.db.leak.detection.threshold", "60000")));

            if (dockerSecretsMap.get("connection.db.default.transaction.isolation") != null) {
                config.setTransactionIsolation(
                        dockerSecretsMap.get("connection.db.default.transaction.isolation"));
            }

            dockerSecretsMap.clear();
            return new HikariDataSource(config);
        }catch (DotRuntimeException e){
            Logger.error(DockerSecretDatasourceStrategy.class,
                    "---------- Error getting dbconnection " + Constants.DATABASE_DEFAULT_DATASOURCE
                            + " from Docker Secret. Reason: " + e.getMessage(),
                    e);
            if(Config.getBooleanProperty("SYSTEM_EXIT_ON_STARTUP_FAILURE", true)){
                e.printStackTrace();
                System.exit(1);
            }

            throw e;
        }
    }
}
