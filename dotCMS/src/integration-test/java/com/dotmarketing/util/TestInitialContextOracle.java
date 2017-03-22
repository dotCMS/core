package com.dotmarketing.util;

import com.dotcms.repackage.org.apache.commons.dbcp.BasicDataSource;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Singleton that defines a context that provides a datasource for testing purpose
 *
 * Created by nollymar on 9/16/16.
 */
public class TestInitialContextOracle extends InitialContext {

    private final String driver = "oracle.jdbc.OracleDriver";
    private final String url = "jdbc:oracle:thin:@localhost:1521:XE";
    private final String username = "dotcms";
    private final String password = "dotcms";
    private final int maxTotal = 60;
    private final int maxIdle = 10;
    private static TestInitialContextOracle context;

    private BasicDataSource dataSource;

    private TestInitialContextOracle() throws NamingException {
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

    public static TestInitialContextOracle getInstance() throws NamingException {
        if (context == null) {
            context = new TestInitialContextOracle();
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
