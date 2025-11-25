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

    /**
     * returns the configured datasource
     */
    public static final Lazy<DataSource> datasource = Lazy.of(PgVectorDataSource::resolveDataSource);

    /**
     * Checks if a specific AI datasource is provided, if not, returns the standard dotCMS datasource
     *
     * @return
     */
    private static DataSource resolveDataSource() {
        if (UtilMethods.isEmpty(Config.getStringProperty("AI_DB_BASE_URL", null))) {
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
        final String userName = Config.getStringProperty("AI_DB_USERNAME");
        final String password = Config.getStringProperty("AI_DB_PASSWORD");
        final String dbUrl = Config.getStringProperty("AI_DB_BASE_URL");
        final int maxConnections = Config.getIntProperty("AI_DB_MAX_TOTAL", 50);

        final HikariConfig config = new HikariConfig();
        config.setUsername(userName);
        config.setPassword(password);
        config.setJdbcUrl(dbUrl);
        config.setMaximumPoolSize(maxConnections);
        return new HikariDataSource(config);
    }

}
