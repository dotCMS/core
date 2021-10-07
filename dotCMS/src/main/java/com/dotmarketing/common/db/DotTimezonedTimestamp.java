package com.dotmarketing.common.db;


import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.base.Preconditions;

import java.sql.Timestamp;
import java.util.Date;

public class DotTimezonedTimestamp {
    private final String timezone;
    private final Date date;

    private DotTimezonedTimestamp(Date date, String timezone) {
        Preconditions.checkArgument(UtilMethods.isSet(date), "Provided date cannot be null or empty");
        Preconditions.checkArgument(UtilMethods.isSet(timezone), "Provided timezone cannot be null or empty");
        this.timezone = timezone;
        this.date = date;
    }

    public Timestamp getTimestamp() {
        return new Timestamp(date.getTime());
    }

    public String getTimezone() {
        return timezone;
    }

    public static DotTimezonedTimestamp now() {
        return new DotTimezonedTimestamp(new Date(), Config.getStringProperty("db.datestimezone",
                DateUtil.UTC));
    }
}
