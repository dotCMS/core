package com.dotcms.util;

import com.dotcms.repackage.org.apache.commons.dbcp.BasicDataSource;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;

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
        //pp
        String dbType = "postgres.";

        if (System.getProperty("databaseType")!=null){
            dbType = System.getProperty("databaseType") + ".";
        } else if(System.getenv("databaseType")!=null){
        	dbType = System.getenv("databaseType") + ".";
        }

        loadProperties();
        dataSource = new BasicDataSource();
        System.out.println("dbType = " + dbType);
        dataSource.setDriverClassName(prop.getProperty(dbType + "db.driver"));
        dataSource.setUrl(prop.getProperty(dbType + "db.base.url"));
        dataSource.setUsername(prop.getProperty(dbType + "db.username"));
        dataSource.setPassword(prop.getProperty(dbType + "db.password"));
        dataSource.setRemoveAbandoned(true);
        dataSource.setLogAbandoned(true);
        dataSource.setMaxIdle(Integer.parseInt(prop.getProperty(dbType + "db.max.idle")));
        dataSource.setMaxActive(Integer.parseInt(prop.getProperty(dbType + "db.max.total")));
        dataSource.setMaxWait(300000);
        
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
        } catch (Exception e) {
            Logger.info(this.getClass(), "Unable to get properties from file db-config.properties - let's try the environment");
        }
        for(String key :System.getenv().keySet()){
          prop.put(key, System.getenv(key));
        }
    }
}
