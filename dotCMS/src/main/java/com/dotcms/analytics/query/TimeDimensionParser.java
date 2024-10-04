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

    private static final String FIELD_REGEX = "(\\w+\\.\\w+)\\s+(\\w+)";

    public static class TimeDimension {
        private String term;
        private String field;

        public TimeDimension(final String term, final String field) {
            this.term = term;
            this.field = field;
        }

        public String getTerm() {
            return term;
        }

        public String getField() {
            return field;
        }

        @Override
        public String toString() {
            return "Term: " + term + ", Field: " + field;
        }
    }

    public static TimeDimension parseTimeDimension(final String expression) throws IllegalArgumentException {
        // cache and checked
        final Pattern pattern = Pattern.compile(FIELD_REGEX);
        final Matcher matcher = pattern.matcher(expression.trim());

        if (matcher.matches()) {

            final String term = matcher.group(1);   // Ex: Events.day
            final String field = matcher.group(2);  // Ex: day

            return new TimeDimension(term, field);
        } else {
            throw new IllegalArgumentException("The expression is not valid. This should be the format 'Term Field'.");
        }
    }
}
