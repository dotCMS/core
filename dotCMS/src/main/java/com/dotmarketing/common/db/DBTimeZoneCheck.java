package com.dotmarketing.common.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.TimeZone;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.zaxxer.hikari.HikariDataSource;
import io.vavr.control.Try;

public class DBTimeZoneCheck {


    /**
     * This code checks to see if the timezone id being passed in is a valid id both with java and with
     * the db
     * 
     * @param timezone
     * @return
     */
    public boolean timeZoneValid(final String timezone) {
        Connection connection = null;
        final TimeZone defaultTimeZone = TimeZone.getDefault();
        try {
            final HikariDataSource hikari = (HikariDataSource) DbConnectionFactory.getDataSource();


            final String dbDriverClazz = hikari.getDriverClassName();


            final TimeZone testingTimeZone = TimeZone.getTimeZone(timezone);

            if (!testingTimeZone.getID().equals(timezone.toUpperCase())) {
                throw new DotRuntimeException("Invalid Timezone: " + timezone);
            }

            TimeZone.setDefault(testingTimeZone);

            Class.forName(dbDriverClazz);
            connection = DriverManager.getConnection(hikari.getJdbcUrl(), hikari.getUsername(), hikari.getPassword());

            final PreparedStatement statement = connection.prepareStatement("select * from inode where idate > ?");
            final Timestamp stamp = new Timestamp(System.currentTimeMillis());


            statement.setTimestamp(1, stamp);
            ResultSet results = statement.executeQuery();
            results.next();
            return true;


        } catch (Exception e) {
            Logger.error(DBTimeZoneCheck.class, "Timezone + '" + timezone + "' failed :" + e.getMessage(), e);
            return false;
        } finally {
            final Connection finalConnection = connection;
            Try.run(() -> TimeZone.setDefault(defaultTimeZone))
                            .onFailure(e -> Logger.warnAndDebug(DBTimeZoneCheck.class, e));
            Try.run(() -> finalConnection.close());
        }

    }


}
