package com.dotcms.util;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import java.io.InputStream;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

/**
 * Singleton that defines a context that provides a datasource for testing purpose
 *
 * Created by nollymar on 9/16/16.
 */
public class TestInitialContext extends InitialContext {

    private DataSource dataSource;
    private Properties prop;
    private static TestInitialContext context;

    private TestInitialContext() throws NamingException {
        String dbType = "postgres.";

        if (System.getProperty("databaseType")!=null){
            dbType = System.getProperty("databaseType") + ".";
        } else if(System.getenv("databaseType")!=null){
        	dbType = System.getenv("databaseType") + ".";
        }

        loadProperties();

        System.out.println("dbType = " + dbType);

        PoolProperties properties = new PoolProperties();
        properties.setName("jdbc/dotCMSPool");
        properties.setDriverClassName(prop.getProperty(dbType + "db.driver"));
        properties.setUrl(prop.getProperty(dbType + "db.base.url"));
        properties.setUsername(prop.getProperty(dbType + "db.username"));
        properties.setPassword(prop.getProperty(dbType + "db.password"));
        properties.setMaxActive(Integer.parseInt(prop.getProperty(dbType + "db.max.total")));
        properties.setMaxIdle(Integer.parseInt(prop.getProperty(dbType + "db.max.idle")));
        properties.setMaxWait(60000);
        properties.setRemoveAbandoned(true);
        properties.setRemoveAbandonedTimeout(600);
        properties.setLogAbandoned(true);
        properties
                .setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ResetAbandonedTimer");
        properties.setTimeBetweenEvictionRunsMillis(30000);
        properties.setValidationQuery(prop.getProperty(dbType + "db.validation.query"));
        properties.setTestOnBorrow(Boolean.TRUE);
        properties.setTestWhileIdle(Boolean.TRUE);
        properties.setAbandonWhenPercentageFull(50);
        properties.setDefaultTransactionIsolation(
                Integer.parseInt(prop.getProperty(dbType + "db.default.transaction.isolation")));

        try {
            dataSource = new DataSource(properties);
            //initialise the pool itself
            dataSource.createPool();
        } catch (Exception e) {
            throw new DotRuntimeException("Error creating tests data source", e);
        }

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
