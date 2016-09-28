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

    private final String driver = "com.mysql.jdbc.Driver";
    private final String url = "jdbc:mysql://localhost/dotcms?characterEncoding=UTF-8";
    private final String username = "dotcms";
    private final String password = "dotcms";

    private static TestInitialContext context;

    private BasicDataSource dataSource;

    private TestInitialContext() throws NamingException {

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
            dataSource = new BasicDataSource();
            dataSource.setDriverClassName(driver);
            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            dataSource.setRemoveAbandoned(true);
            dataSource.setLogAbandoned(true);

            return dataSource;
        }

        throw new NamingException("Unable to find datasource: " + name);
    }
}
