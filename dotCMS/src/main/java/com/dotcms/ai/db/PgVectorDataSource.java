package com.dotcms.ai.db;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.vavr.Lazy;

import javax.sql.DataSource;

/**
 * This class will return the Datasource that is configured for the ai plugin, or will return the default datasource if no specific DB is configured for AI
 */
class PgVectorDataSource {

    private static final String AI_DB_BASE_URL_KEY = "AI_DB_BASE_URL";
    private static final String AI_DB_USERNAME_KEY = "AI_DB_USERNAME";
    private static final String AI_DB_PASSWORD_KEY = "AI_DB_PASSWORD";
    private static final String AI_DB_MAX_TOTAL_KEY = "AI_DB_MAX_TOTAL";

    /**
     * returns the configured datasource
     */
    public static final Lazy<DataSource> datasource = Lazy.of(PgVectorDataSource::resolveDataSource);

    private PgVectorDataSource() {
    }

    /**
     * Checks if a specific AI datasource is provided, if not, returns the standard dotCMS datasource
     *
     * @return
     */
    private static DataSource resolveDataSource() {
        if (UtilMethods.isEmpty(Config.getStringProperty(AI_DB_BASE_URL_KEY, null))) {
            return DbConnectionFactory.getDataSource();
        }

        return internalDatasource();
    }

    /**
     * Builds the ai specific datasource.
     *
     * @return
     */
    private static DataSource internalDatasource() {
        final String userName = Config.getStringProperty(AI_DB_USERNAME_KEY, null);
        final String password = Config.getStringProperty(AI_DB_PASSWORD_KEY, null);
        final String dbUrl = Config.getStringProperty(AI_DB_BASE_URL_KEY, null);
        final int maxConnections = Config.getIntProperty(AI_DB_MAX_TOTAL_KEY, 50);

        final HikariConfig config = new HikariConfig();
        config.setUsername(userName);
        config.setPassword(password);
        config.setJdbcUrl(dbUrl);
        config.setMaximumPoolSize(maxConnections);
        config.setRegisterMbeans(Config.getBooleanProperty("hikari.register.mbeans", true));
        return new HikariDataSource(config);
    }

}
