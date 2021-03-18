package com.dotmarketing.common.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.TimeZone;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.zaxxer.hikari.HikariDataSource;
import io.vavr.control.Try;

public class DBTimeZoneCheck {
    private static Class driverClass;

    public static Class resolveDriverClass(final HikariDataSource dataSource) throws ClassNotFoundException {
        if (driverClass == null) {
            driverClass = Class.forName(dataSource.getDriverClassName());
        }
        return driverClass;
    }

    /**
     * This code checks to see if the timezone id being passed in is a valid id both with java and with
     * the db
     * 
     * @param timezone
     * @return
     */
    public static boolean timeZoneValid(final String timezone) {
        Connection connection = null;
        final TimeZone defaultTimeZone = TimeZone.getDefault();
        try {
            final HikariDataSource hikari = (HikariDataSource) DbConnectionFactory.getDataSource();
            resolveDriverClass(hikari);

            final TimeZone testingTimeZone = TimeZone.getTimeZone(timezone);
            if (!testingTimeZone.getID().equalsIgnoreCase(timezone)) {
                throw new DotRuntimeException("Invalid Timezone: " + timezone);
            }
            TimeZone.setDefault(testingTimeZone);

            connection = DriverManager.getConnection(hikari.getJdbcUrl(), hikari.getUsername(), hikari.getPassword());

            final PreparedStatement statement = connection.prepareStatement("SELECT * FROM inode WHERE idate > ?");
            statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            statement.executeQuery().next();

            return true;
        } catch (Exception e) {
            Logger.error(DBTimeZoneCheck.class, "Timezone + '" + timezone + "' failed : " + e.getMessage(), e);
            TimeZone.setDefault(defaultTimeZone);

            return false;
        } finally {
            if (connection != null) {
                Try.run(connection::close);
            }
        }
    }
}
