package com.dotmarketing.util;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;

import com.dotmarketing.db.DbConnectionFactory;

/**
 * Created by nollymar on 9/16/16.
 */
public class TestInitialContextFactory implements InitialContextFactoryBuilder {


    final DbConnectionFactory.DataBaseType type;
    final String dbType = (System.getProperty("test.database.type") != null)
            ? System.getProperty("test.database.type") 
            : System.getenv("test_database_type");

    TestInitialContextFactory() {
        if ("mysql".equalsIgnoreCase(dbType)) {
            this.type = DbConnectionFactory.DataBaseType.MySQL;
        } else if ("mssql".equalsIgnoreCase(dbType)) {
            this.type = DbConnectionFactory.DataBaseType.MSSQL;
        } else if ("oracle".equalsIgnoreCase(dbType)) {
            this.type = DbConnectionFactory.DataBaseType.ORACLE;
        } else if ("h2".equalsIgnoreCase(dbType)) {
            this.type = DbConnectionFactory.DataBaseType.H2;
        } else {
            this.type = DbConnectionFactory.DataBaseType.POSTGRES;
        }
    }


    @Override
    public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment)
            throws NamingException {



        return new InitialContextFactory() {
            @Override
            public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
                switch (type) {
                    case ORACLE:
                        return TestInitialContextOracle.getInstance();
                    case MySQL:
                        return TestInitialContextMySQL.getInstance();
                    case MSSQL:
                        return TestInitialContextMSSQL.getInstance();
                    case H2:
                        return TestInitialContextH2.getInstance();
                    default:
                        return TestInitialContextPostgres.getInstance();
                }
            }
        };

    }



}
