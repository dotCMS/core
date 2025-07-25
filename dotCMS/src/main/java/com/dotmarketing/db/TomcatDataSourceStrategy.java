package com.dotmarketing.db;

import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.liferay.util.JNDIUtil;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Singleton class that obtains a datasource from a <b>context.xml</b> file
 * @author nollymar
 */
public class TomcatDataSourceStrategy implements DotDataSourceStrategy {

    @VisibleForTesting
    TomcatDataSourceStrategy(){}

    private static class SingletonHelper{
        private static TomcatDataSourceStrategy INSTANCE = new TomcatDataSourceStrategy();
    }

    public static TomcatDataSourceStrategy getInstance(){
        return SingletonHelper.INSTANCE;
    }

    @Override
    public DataSource apply() {
        try {
            final InitialContext ctx = new InitialContext();
            final HikariConfig config = new HikariConfig();
            config.setDataSource((DataSource) JNDIUtil.lookup(ctx, Constants.DATABASE_DEFAULT_DATASOURCE));
            config.setRegisterMbeans(com.dotmarketing.util.Config.getBooleanProperty("hikari.register.mbeans", true));
            return new HikariDataSource(config);
        } catch (NamingException e) {
            Logger.error(TomcatDataSourceStrategy.class,
                    "---------- Error getting dbconnection " + Constants.DATABASE_DEFAULT_DATASOURCE + " from context.xml",
                    e);

            throw new DotRuntimeException(e.toString(), e);
        }
    }
}
