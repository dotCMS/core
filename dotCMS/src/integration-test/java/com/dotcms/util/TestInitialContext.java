package com.dotcms.util;

import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

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

        dataSource = hikariDataSource(dbType);
    }

    private DataSource hikariDataSource(final String dbType) {

        // https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby
        HikariConfig config = new HikariConfig();

        config.setPoolName("jdbc/dotCMSPool");
        config.setDriverClassName(prop.getProperty(dbType + "db.driver"));
        config.setJdbcUrl(prop.getProperty(dbType + "db.base.url"));
        config.setUsername(prop.getProperty(dbType + "db.username"));
        config.setPassword(prop.getProperty(dbType + "db.password"));

        // Lowest acceptable connection timeout is 250 ms. Default: 30000 (30 seconds)
        //config.setConnectionTimeout(30000);

        // The minimum allowed value is 10000ms (10 seconds). Default: 600000 (10 minutes)
        //config.setIdleTimeout(600000);

        // We strongly recommend setting this value, and it should be several seconds shorter than any database
        // or infrastructure imposed connection time limit.
        // A value of 0 indicates no maximum lifetime (infinite lifetime), subject of course to the idleTimeout
        // setting. Default: 1800000 (30 minutes)
        //config.setMaxLifetime(1800000);

        // This property controls the amount of time that a connection can be out of the pool before a message
        // is logged indicating a possible connection leak. A value of 0 means leak detection is disabled.
        // Lowest acceptable value for enabling leak detection is 2000 (2 seconds). Default: 0
        config.setLeakDetectionThreshold(60000);

        //config.addDataSourceProperty("cachePrepStmts", "true");
        //config.addDataSourceProperty("prepStmtCacheSize", "250");
        //config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return new HikariDataSource(config);
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

    public DataSource getDataSource(){
        return dataSource;
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
