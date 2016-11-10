package com.dotmarketing.util;

import com.dotcms.repackage.org.apache.commons.dbcp.BasicDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Singleton that defines a context that provides a datasource for testing purpose
 *
 * Created by nollymar on 9/16/16.
 */
public class TestInitialContext extends InitialContext {

    private BasicDataSource dataSource;
    private Properties prop;
    private static TestInitialContext context;

    private TestInitialContext() throws NamingException {
        String dbType = "";

        if (System.getProperty("databaseType")!=null){
            dbType = System.getProperty("databaseType") + ".";
        }

        loadProperties();
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName(prop.getProperty(dbType + "db.driver"));
        dataSource.setUrl(prop.getProperty(dbType + "db.base.url"));
        dataSource.setUsername(prop.getProperty(dbType + "db.username"));
        dataSource.setPassword(prop.getProperty(dbType + "db.password"));
        dataSource.setRemoveAbandoned(true);
        dataSource.setLogAbandoned(true);
        dataSource.setMaxIdle(Integer.parseInt(prop.getProperty(dbType + "db.max.idle")));
        dataSource.setMaxActive(Integer.parseInt(prop.getProperty(dbType + "db.max.total")));
    }

    public static TestInitialContext getInstance() throws NamingException {
        if (context == null) {
            context = new TestInitialContext();
        }

        return context;
    }

    @Override
    public Object lookup(String name) throws NamingException {

        if (name != null && name.equals(Constants.DATABASE_DEFAULT_DATASOURCE)) { // init datasources
            return dataSource;
        }

        throw new NamingException("Unable to find datasource: " + name);
    }

    /**
     * Load properties file with DB connection details
     * @throws NamingException
     */
    private void loadProperties() throws NamingException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        prop = new Properties();
        try (InputStream resourceStream = loader.getResourceAsStream("db-config.properties")) {
            prop.load(resourceStream);
        } catch (IOException e) {
            throw new NamingException("Unable to get properties from db-config.properties");
        }
    }
}
