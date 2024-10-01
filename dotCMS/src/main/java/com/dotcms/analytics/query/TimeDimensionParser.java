package com.dotcms.analytics.query;

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

    public static class TimeDimension {

        private final String dimension;
        private final String granularity;
        private final String dateRange;

        public TimeDimension(final String dimension, final String granularity) {
            this.dimension = dimension;
            this.granularity = granularity;
            this.dateRange = null;
        }

        public TimeDimension(final String dimension, final String granularity, final String dateRange) {
            this.dimension = dimension;
            this.granularity = granularity;
            this.dateRange = dateRange;
        }

        public String getDimension() {
            return dimension;
        }

        public String getGranularity() {
            return granularity;
        }

        public String getDateRange() {
            return dateRange;
        }

        @Override
        public String toString() {
            return "TimeDimension{" +
                    "dimension='" + dimension + '\'' +
                    ", granularity='" + granularity + '\'' +
                    ", dateRange='" + dateRange + '\'' +
                    '}';
        }
    }

    public static TimeDimension parseTimeDimension(final String expression) throws IllegalArgumentException {
        // cache and checked
        final Matcher matcher = PATTERN.matcher(expression.trim());

        if (matcher.matches()) {

            final String dimension = matcher.group(1);   // Ex: Events.day
            final String granularity = matcher.group(2);  // Ex: day
            final String dateRange = matcher.group(3);  // Ex: date range

            return new TimeDimension(dimension, granularity, dateRange);
        } else {
            throw new IllegalArgumentException("The expression is not valid. This should be the format 'Term Field'.");
        }
    }
}
