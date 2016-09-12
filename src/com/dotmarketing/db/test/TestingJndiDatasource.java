package com.dotmarketing.db.test;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;
import javax.sql.DataSource;

import com.dotcms.repackage.org.apache.commons.dbcp.BasicDataSource;

/**
 * This class will set up the data source for testing. The main purpose here
 * is to be able to run the integration tests without the web app container
 * i.e. Tomcat. Database related configuration for testing should be
 * implemented following the same pattern.
 *
 * Running this in your junit tests will set dotCMS up for you:
 * <pre>
 * {@code
 * &#64;BeforeClass
 * public static void setUpBeforeClass() throws Exception {
 *     new TestingJndiDatasource().init();
 *     Config._setupFakeTestingContext();
 * }
 * }
 * </pre>
 *
 * Note: Code snippet above should be useful on testing APIs that don't require ES.
 * If ES is needed, you will need to include also the config for ES.
 */
public class TestingJndiDatasource {

    final String driver;
    final String url;
    final String username;
    final String password;
    final int maxTotal;

    public TestingJndiDatasource() {
        this("com.mysql.jdbc.Driver", "jdbc:mysql://localhost/dotcms?characterEncoding=UTF-8",
                "dotcms", "dotcms", 100);
    }

    public TestingJndiDatasource(String driver, String url, String username, String password) {
        this(driver, url, username, password, 100);
    }


    public TestingJndiDatasource(String driver, String url, String username, String password,
            int maxTotal) {
        super();
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
        this.maxTotal = maxTotal;
    }



    public void init() throws Exception {

        NamingManager.setInitialContextFactoryBuilder(new InitialContextFactoryBuilder() {


            @Override
            public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment)
                    throws NamingException {
                return new InitialContextFactory() {

                    @Override
                    public Context getInitialContext(Hashtable<?, ?> environment)
                            throws NamingException {
                        return new InitialContext() {

                            private Hashtable<String, DataSource> dataSources = new Hashtable<>();

                            @Override
                            public Object lookup(String name) throws NamingException {

                                if (dataSources.isEmpty()) { // init datasources

                                    BasicDataSource dataSource = new BasicDataSource();
                                    dataSource.setDriverClassName(driver);
                                    dataSource.setUrl(url);
                                    dataSource.setUsername(username);
                                    dataSource.setPassword(password);
                                    dataSource.setMaxActive(100);
                                    dataSources.put("jdbc/dotCMSPool", dataSource);

                                }

                                if (dataSources.containsKey(name)) {
                                    return dataSources.get(name);
                                }

                                throw new NamingException("Unable to find datasource: " + name);
                            }
                        };
                    }

                };
            }

        });

    }
}
