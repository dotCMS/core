package com.dotmarketing.db;

import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import javax.sql.DataSource;

/**
 * Singleton class that provides a datasource using a <b>db.properties</b> file configuration
 * @author nollymar
 */
public class DBPropertiesDataSourceStrategy implements DotDataSourceStrategy {

    private static final String DB_PROPERTIES_FILE_NAME = "db.properties";

    private static File propertiesFile;

    @VisibleForTesting
    DBPropertiesDataSourceStrategy(final File file){
        propertiesFile = file;
    }

    @VisibleForTesting
    DBPropertiesDataSourceStrategy(){
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final URL resourceURL = loader.getResource(DB_PROPERTIES_FILE_NAME);
        if (resourceURL!=null){
            propertiesFile = new File(resourceURL.getPath());
        }

    }

    private static class SingletonHelper{
        private static final DBPropertiesDataSourceStrategy INSTANCE = new DBPropertiesDataSourceStrategy();
    }

    public static DBPropertiesDataSourceStrategy getInstance(){
        return SingletonHelper.INSTANCE;
    }

    /**
     *
     * @return True if a <b>db.properties</b> file exists in WEB-INF/classes directory
     */
    public boolean existsDBPropertiesFile() {
        return propertiesFile!=null && propertiesFile.exists();
    }

    @Override
    public DataSource apply() {
        try {

            if (!(existsDBPropertiesFile())){
                throw new FileNotFoundException("DB properties file not found");
            }
            return new HikariDataSource(getHikariConfig());
        } catch (IOException e) {
            Logger.error(DBPropertiesDataSourceStrategy.class,
                    "---------- Error getting dbconnection " + Constants.DATABASE_DEFAULT_DATASOURCE
                            + " from db.properties file",
                    e);

            throw new DotRuntimeException(e.toString(), e);
        }
    }

    @VisibleForTesting
    File getPropertiesFile() {
        return propertiesFile;
    }

    @VisibleForTesting
    HikariConfig getHikariConfig() {
        final HikariConfig config = new HikariConfig(propertiesFile.getPath());
        config.setPoolName(Constants.DATABASE_DEFAULT_DATASOURCE);
        config.setRegisterMbeans(com.dotmarketing.util.Config.getBooleanProperty("hikari.register.mbeans", true));
        return config;
    }
}
