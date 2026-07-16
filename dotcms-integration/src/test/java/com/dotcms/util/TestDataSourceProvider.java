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
        config.setRegisterMbeans(com.dotmarketing.util.Config.getBooleanProperty("hikari.register.mbeans", true));

        return new HikariDataSource(config);
    }

    /**
     * Load properties file with DB connection details
     * @throws NamingException
     */
    private Properties getProperties()  {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final Properties properties = new Properties();

        final String dbType = System.getProperty("databaseType",
                (System.getenv("databaseType") != null ? System.getenv("databaseType")
                        : "postgres"));

        try (InputStream resourceStream = loader.getResourceAsStream(dbType + "-db-config.properties")) {
            properties.load(resourceStream);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Unable to get properties from file db-config.properties",  e);
            throw new DotRuntimeException(e);
        }

        return properties;
    }

}
