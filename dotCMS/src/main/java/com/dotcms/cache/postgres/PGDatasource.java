package com.dotcms.cache.postgres;

import javax.sql.DataSource;
import com.dotcms.repackage.com.zaxxer.hikari.HikariConfig;
import com.dotcms.repackage.com.zaxxer.hikari.HikariDataSource;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Constants;

public class PGDatasource {
    static final String CONNECTION_DB_DRIVER = "PG_DB_DRIVER";
    static final String CONNECTION_DB_BASE_URL = "PG_DB_BASE_URL";
    static final String CONNECTION_DB_USERNAME = "PG_DB_USERNAME";
    static final String CONNECTION_DB_PASSWORD = "PG_DB_PASSWORD";
    static final String CONNECTION_DB_MAX_WAIT = "PG_DB_MAXWAIT";
    static final String CONNECTION_DB_MAX_TOTAL = "PG_DB_MAX_TOTAL";
    static final String CONNECTION_DB_MIN_IDLE = "PG_DB_MIN_IDLE";
    static final String CONNECTION_DB_VALIDATION_QUERY = "PG_DB_VALIDATION_QUERY";
    static final String CONNECTION_DB_LEAK_DETECTION_THRESHOLD = "PG_DB_LEAK_DETECTION_THRESHOLD";
    static final String CONNECTION_DB_DEFAULT_TRANSACTION_ISOLATION = "PG_DB_DEFAULT_TRANSACTION_ISOLATION";



    DataSource datasource() {



        if (DbConnectionFactory.isPostgres()) {
            return DbConnectionFactory.getDataSource();

        }


        final HikariConfig config = new HikariConfig();

        config.setPoolName(Constants.DATABASE_DEFAULT_DATASOURCE);

        config.setDriverClassName(System.getenv(CONNECTION_DB_DRIVER) != null
            ? System.getenv(CONNECTION_DB_DRIVER)
            : "org.postgresql.Driver");

        config.setJdbcUrl(System.getenv(CONNECTION_DB_BASE_URL) != null
            ? System.getenv(CONNECTION_DB_BASE_URL)
            : "jdbc:postgresql://db.dotcms.site/dotcms");

        config.setUsername(System.getenv(CONNECTION_DB_USERNAME));

        config.setPassword(System.getenv(CONNECTION_DB_PASSWORD));

        config.setMaximumPoolSize(Integer.parseInt(
                        System.getenv(CONNECTION_DB_MAX_TOTAL) != null ? System.getenv(CONNECTION_DB_MAX_TOTAL) : "60"));

        config.setMinimumIdle(Integer
                        .parseInt(System.getenv(CONNECTION_DB_MIN_IDLE) != null ? System.getenv(CONNECTION_DB_MIN_IDLE) : "1"));


        config.setIdleTimeout(Integer.parseInt(
                        System.getenv(CONNECTION_DB_MIN_IDLE) != null ? System.getenv(CONNECTION_DB_MIN_IDLE) : "10") * 1000);

        config.setMaxLifetime(Integer.parseInt(
                        System.getenv(CONNECTION_DB_MAX_WAIT) != null ? System.getenv(CONNECTION_DB_MAX_WAIT) : "60000"));

        config.setConnectionTestQuery(System.getenv(CONNECTION_DB_VALIDATION_QUERY));

        // This property controls the amount of time that a connection can be out of the pool before a
        // message
        // is logged indicating a possible connection leak. A value of 0 means leak detection is disabled.
        // Lowest acceptable value for enabling leak detection is 2000 (2 seconds). Default: 0
        config.setLeakDetectionThreshold(Integer.parseInt(System.getenv(CONNECTION_DB_LEAK_DETECTION_THRESHOLD) != null
            ? System.getenv(CONNECTION_DB_LEAK_DETECTION_THRESHOLD)
            : "300000"));

        config.setTransactionIsolation(System.getenv(CONNECTION_DB_DEFAULT_TRANSACTION_ISOLATION));


        return new HikariDataSource(config);


    };
}


