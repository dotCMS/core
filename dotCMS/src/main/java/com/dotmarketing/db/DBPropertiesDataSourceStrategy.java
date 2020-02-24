package com.dotmarketing.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.net.URL;
import javax.sql.DataSource;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Singleton class that provides a datasource using a <b>db.properties</b> file configuration
 * @author nollymar
 */
public class DBPropertiesDatasourceStrategy implements DotDatasourceStrategy {

    private static final String DB_PROPERTIES_FILE_NAME = "db.properties";

    private DBPropertiesDatasourceStrategy(){}

    private static class SingletonHelper{
        private static final DBPropertiesDatasourceStrategy INSTANCE = new DBPropertiesDatasourceStrategy();
    }

    public static DBPropertiesDatasourceStrategy getInstance(){
        return SingletonHelper.INSTANCE;
    }

    /**
     *
     * @return True if a <b>db.properties</b> file exists in WEB-INF/classes directory
     */
    public boolean existsDBPropertiesFile() {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final URL resourceURL = loader.getResource(DB_PROPERTIES_FILE_NAME);
        return resourceURL!=null && new File(resourceURL.getPath()).exists();
    }

    @Override
    public DataSource apply() {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        loader.getResourceAsStream(DB_PROPERTIES_FILE_NAME);

        final PropertiesConfiguration properties = new PropertiesConfiguration();
        try {
            properties.load(loader.getResourceAsStream(DB_PROPERTIES_FILE_NAME));

            final HikariConfig config = new HikariConfig();

            config.setPoolName(Constants.DATABASE_DEFAULT_DATASOURCE);
            config.setDriverClassName(properties.getString("connection_db_driver"));
            config.setJdbcUrl(properties.getString("connection_db_base_url"));
            config.setUsername(properties.getString("connection_db_username"));
            config.setPassword(properties.getString("connection_db_password"));
            config.setMaximumPoolSize(properties.getInt("connection_db_max_total", 60));
            config.setIdleTimeout(properties.getInt("connection_db_max_idle", 10) * 1000);
            config.setMaxLifetime(properties.getInt("connection_db_max_wait", 60000));
            config.setConnectionTestQuery(properties.getString("connection_db_validation_query"));

            // This property controls the amount of time that a connection can be out of the pool before a message
            // is logged indicating a possible connection leak. A value of 0 means leak detection is disabled.
            // Lowest acceptable value for enabling leak detection is 2000 (2 seconds). Default: 0
            config.setLeakDetectionThreshold(properties.getInt("connection_db_leak_detection_threshold", 60000));

            config.setTransactionIsolation(properties.getString("connection_db_default_transaction_isolation"));

            properties.clear();
            return new HikariDataSource(config);
        } catch (ConfigurationException e) {
            Logger.error(DBPropertiesDatasourceStrategy.class,
                    "---------- Error getting dbconnection " + Constants.DATABASE_DEFAULT_DATASOURCE
                            + " from db.properties file",
                    e);

            throw new DotRuntimeException(e.toString());
        }
    }
}
