package com.dotmarketing.util;

import com.dotcms.repackage.org.apache.commons.dbcp.BasicDataSource;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Singleton that defines a context that provides a datasource for testing purpose
 *
 * Created by nollymar on 9/16/16.
 */
public class TestInitialContextH2 extends InitialContext {

    private final String driver = "org.h2.Driver";
    private final String url = "jdbc:h2:WEB-INF/H2_DATABASE/h2_dotcms_data;MVCC=TRUE;LOCK_TIMEOUT=15000";
    private final String username = "sa";
    private final String password = "sa";
    private final int maxTotal = 60;
    private final int maxIdle = 10;
    private static TestInitialContextH2 context;

    private BasicDataSource dataSource;

    private TestInitialContextH2() throws NamingException {
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

    public static TestInitialContextH2 getInstance() throws NamingException {
        if (context == null) {
            context = new TestInitialContextH2();
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
