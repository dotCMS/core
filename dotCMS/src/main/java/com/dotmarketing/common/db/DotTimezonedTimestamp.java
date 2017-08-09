package com.dotmarketing.common.db;


import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.google.common.base.Preconditions;

import java.sql.Timestamp;
import java.util.Date;

public class DotTimezonedTimestamp {
    private final String timezone;
    private final Timestamp timestamp;

    private DotTimezonedTimestamp(Date date, String timezone) {
        Preconditions.checkArgument(UtilMethods.isSet(date), "Provided date cannot be null or empty");
        Preconditions.checkArgument(UtilMethods.isSet(timezone), "Provided timezone cannot be null or empty");
        this.timezone = timezone;
        this.timestamp = new Timestamp(date.getTime());
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getTimezone() {
        return timezone;
    }

    public static DotTimezonedTimestamp now() {
        return new DotTimezonedTimestamp(new Date(), Config.getStringProperty("db.datestimezone","UTC"));
    }
}
