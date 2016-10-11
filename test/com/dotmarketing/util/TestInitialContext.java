package com.dotmarketing.util;

import com.dotcms.repackage.org.apache.commons.dbcp.BasicDataSource;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Singleton that defines a context that provides a datasource for testing purpose
 *
 * Created by nollymar on 9/16/16.
 */
public class TestInitialContext extends InitialContext {

    private final String driver = "org.postgresql.Driver";
    private final String url = "jdbc:postgresql://localhost/dotcms";
    private final String username = "postgres";
    private final String password = "postgres";
    private final int maxTotal = 60;
    private final int maxIdle = 10;
    private static TestInitialContext context;

    private BasicDataSource dataSource;

    private TestInitialContext() throws NamingException {
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setRemoveAbandoned(true);
        dataSource.setLogAbandoned(true);
        dataSource.setMaxIdle(maxIdle);
        dataSource.setMaxActive(maxTotal);
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
}
