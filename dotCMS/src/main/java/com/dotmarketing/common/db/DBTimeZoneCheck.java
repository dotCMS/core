package com.dotmarketing.common.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import com.dotcms.util.CloseUtils;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.zaxxer.hikari.HikariDataSource;
import io.vavr.control.Try;

public class DBTimeZoneCheck {
    private static final AtomicReference<Class> DRIVER_REF = new AtomicReference<>(null);

    /**
     * Driver class loaded to avoid call the loading method.
     * @param dataSource
     * @return
     * @throws ClassNotFoundException
     */
    private static Class resolveDriverClass(final HikariDataSource dataSource) throws ClassNotFoundException {
        DRIVER_REF.compareAndSet(null, Class.forName(dataSource.getDriverClassName()));
        return DRIVER_REF.get();
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
        PreparedStatement statement = null;
        ResultSet resultSet = null;
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

            statement = connection.prepareStatement("SELECT * FROM inode WHERE idate > ?");
            statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));

            resultSet = statement.executeQuery();
            resultSet.next();

            return true;
        } catch (Exception e) {
            Logger.error(DBTimeZoneCheck.class, "Timezone + '" + timezone + "' failed : " + e.getMessage(), e);

            return false;
        } finally {
            Try.run(() -> TimeZone.setDefault(defaultTimeZone))
                    .onFailure(e -> Logger.warnAndDebug(DBTimeZoneCheck.class, e));
            CloseUtils.closeQuietly(resultSet, statement, connection);
        }
    }
}
