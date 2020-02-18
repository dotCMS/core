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

            config.setPoolName(Constants.DATABASE_DEFAULT_DATASOURCE);

            config.setDriverClassName(System.getenv("connection_db_driver") != null ? System
                    .getenv("connection_db_driver") : "org.postgresql.Driver");

            config.setJdbcUrl(System.getenv("connection_db_base_url") != null ? System
                    .getenv("connection_db_base_url") : "jdbc:postgresql://localhost/dotcms");

            config.setUsername(System.getenv("connection_db_username"));

            config.setPassword(System.getenv("connection_db_password"));

            config.setMaximumPoolSize(Integer.parseInt(
                    System.getenv("connection_db_max_total") != null ? System
                            .getenv("connection_db_max_total") : "60"));

            config.setIdleTimeout(
                    Integer.parseInt(System.getenv("connection_db_max_idle") != null ? System
                            .getenv("connection_db_max_idle") : "10")
                            * 1000);

            config.setMaxLifetime(Integer.parseInt(System.getenv("connection_db_max_wait") != null ? System
                    .getenv("connection_db_max_wait") : "60000"));

            config.setConnectionTestQuery(System.getenv("connection_db_validation_query") != null ? System
                    .getenv("connection_db_validation_query") : "SELECT 1");

            // This property controls the amount of time that a connection can be out of the pool before a message
            // is logged indicating a possible connection leak. A value of 0 means leak detection is disabled.
            // Lowest acceptable value for enabling leak detection is 2000 (2 seconds). Default: 0
            config.setLeakDetectionThreshold(Integer.parseInt(System.getenv("connection_db_leak_detection_threshold") != null ? System
                    .getenv("connection_db_leak_detection_threshold"): "60000"));

            config.setTransactionIsolation(
                    System.getenv("connection_db_default_transaction_isolation"));

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
