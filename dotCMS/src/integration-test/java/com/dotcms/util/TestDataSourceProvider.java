package com.dotcms.util;

import com.dotmarketing.db.DotDataSourceStrategy;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.util.Properties;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class TestDataSourceProvider implements DotDataSourceStrategy {

    @Override
    public DataSource apply() {

        HikariConfig config = new HikariConfig(getProperties());

        config.setPoolName("jdbc/dotCMSPool");

        return new HikariDataSource(config);
    }

    /**
     * Load properties file with DB connection details
     * @throws NamingException
     */
    private Properties getProperties()  {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final Properties properties = new Properties();

        String dbType = "postgres";

        if (System.getProperty("databaseType")!=null){
            dbType = System.getProperty("databaseType");
        } else if(System.getenv("databaseType")!=null){
            dbType = System.getenv("databaseType");
        }

        try (InputStream resourceStream = loader.getResourceAsStream(dbType + "-db-config.properties")) {
            properties.load(resourceStream);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Unable to get properties from file db-config.properties",  e);
            throw new DotRuntimeException(e);
        }

        return properties;
    }

}
