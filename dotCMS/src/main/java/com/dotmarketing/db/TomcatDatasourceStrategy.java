package com.dotmarketing.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.liferay.util.JNDIUtil;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * Singleton class that obtains a datasource from a <b>context.xml</b> file
 * @author nollymar
 */
public class TomcatDatasourceStrategy implements DotDatasourceStrategy {

    private TomcatDatasourceStrategy(){}

    private static class SingletonHelper{
        private static TomcatDatasourceStrategy INSTANCE = new TomcatDatasourceStrategy();
    }

    public static TomcatDatasourceStrategy getInstance(){
        return SingletonHelper.INSTANCE;
    }

    @Override
    public DataSource getDatasource() {
        try {
            final InitialContext ctx = new InitialContext();
            final HikariConfig config = new HikariConfig();
            config.setDataSource((DataSource) JNDIUtil.lookup(ctx, Constants.DATABASE_DEFAULT_DATASOURCE));
            return new HikariDataSource(config);
        } catch (Throwable e) {
            Logger.error(TomcatDatasourceStrategy.class,
                    "---------- Error getting dbconnection " + Constants.DATABASE_DEFAULT_DATASOURCE + " from context.xml",
                    e);
            if(Config.getBooleanProperty("SYSTEM_EXIT_ON_STARTUP_FAILURE", true)){
                e.printStackTrace();
                System.exit(1);
            }

            throw new DotRuntimeException(e.toString());
        }
    }
}
