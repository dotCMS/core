package com.dotmarketing.db;

import com.dotcms.repackage.com.zaxxer.hikari.HikariConfig;
import com.dotcms.repackage.com.zaxxer.hikari.HikariDataSource;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import javax.sql.DataSource;


/**
 * Singleton class that provides a datasource reading properties from system environment
 * @author nollymar
 */
public class SystemEnvDatasourceStrategy implements DotDatasourceStrategy {

    private SystemEnvDatasourceStrategy(){}

    private static class SingletonHelper{
        private static final SystemEnvDatasourceStrategy INSTANCE = new SystemEnvDatasourceStrategy();
    }

    public static SystemEnvDatasourceStrategy getInstance(){
        return SingletonHelper.INSTANCE;
    }

    @Override
    public DataSource getDatasource() {
        try {

            final HikariConfig config = new HikariConfig();

            config.setPoolName(System.getenv("connection.db.name") != null ? System
                    .getenv("connection.db.name") : Constants.DATABASE_DEFAULT_DATASOURCE);

            config.setDriverClassName(System.getenv("connection.db.driver") != null ? System
                    .getenv("connection.db.driver") : "org.postgresql.Driver");

            config.setJdbcUrl(System.getenv("connection.db.base.url") != null ? System
                    .getenv("connection.db.base.url") : "jdbc:postgresql://localhost/dotcms");

            config.setUsername(System.getenv("connection.db.username"));

            config.setPassword(System.getenv("connection.db.password"));

            config.setMaximumPoolSize(Integer.parseInt(
                    System.getenv("connection.db.max.total") != null ? System
                            .getenv("connection.db.max.total") : "60"));

            config.setIdleTimeout(
                    Integer.parseInt(System.getenv("connection.db.max.idle") != null ? System
                            .getenv("connection.db.max.idle") : "10")
                            * 1000);

            config.setMaxLifetime(Integer.parseInt(System.getenv("connection.db.max.wait") != null ? System
                    .getenv("connection.db.max.wait") : "60000"));

            config.setConnectionTestQuery(System.getenv("connection.db.validation.query") != null ? System
                    .getenv("connection.db.validation.query") : "SELECT 1");

            // This property controls the amount of time that a connection can be out of the pool before a message
            // is logged indicating a possible connection leak. A value of 0 means leak detection is disabled.
            // Lowest acceptable value for enabling leak detection is 2000 (2 seconds). Default: 0
            config.setLeakDetectionThreshold(Integer.parseInt(System.getenv("connection.db.leak.detection.threshold") != null ? System
                    .getenv("connection.db.leak.detection.threshold"): "60000"));

            config.setTransactionIsolation(
                    System.getenv("connection.db.default.transaction.isolation"));

            return new HikariDataSource(config);
        }catch (DotRuntimeException e){
            Logger.error(SystemEnvDatasourceStrategy.class,
                    "---------- Error getting dbconnection " + Constants.DATABASE_DEFAULT_DATASOURCE
                            + " from System Environment. Reason: " + e.getMessage(),
                    e);
            if(Config.getBooleanProperty("SYSTEM_EXIT_ON_STARTUP_FAILURE", true)){
                e.printStackTrace();
                System.exit(1);
            }

            throw e;
        }
    }
}
