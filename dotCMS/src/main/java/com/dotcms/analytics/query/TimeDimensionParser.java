package com.dotcms.analytics.query;

import com.dotcms.cube.CubeJSQuery;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Time Dimension Parser
 * Example:
 * <pre>
 *     TimeDimensionParser.parseTimeDimension("Events.day day");
 * </pre>
 *
 * This should return Events.day and day (term and field)
 *  Note: this is not support intervals for dates, but will introduce on the future
 * @author jsanca
 */
public class TimeDimensionParser {

    private TimeDimensionParser() {
        // singleton
    }

    private static final String FIELD_REGEX = "^(\\w+\\.\\w+)\\s+(\\w+)(?:\\s+(.+))?$";
    private static final Pattern PATTERN = Pattern.compile(FIELD_REGEX);

    public static CubeJSQuery.TimeDimension parseTimeDimension(final String expression) throws IllegalArgumentException {
        // cache and checked
        final Matcher matcher = PATTERN.matcher(expression.trim());

        if (matcher.matches()) {

            final String dimension = matcher.group(1);   // Ex: Events.day
            final String granularity = matcher.group(2);  // Ex: day
            final String dateRange = matcher.group(3);  // Ex: date range

            return new CubeJSQuery.TimeDimension(dimension, granularity, dateRange);
        } else {
            throw new IllegalArgumentException("The expression is not valid. This should be the format 'Term Field'.");
        }
    }
}
