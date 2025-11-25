package com.dotcms.analytics.query;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Order parser
 * Example:
 * <pre>
 *     OrderParser.parseOrder("Events.day     ASC");
 * </pre>
 *
 * should return Events.day and ASC (term and order)
 * if the order is not ASC or DESC or is missing will throw {@link IllegalArgumentException}
 * @author jsanca
 */
public class OrderParser {

    private OrderParser () {
        // singleton
    }
    // Expression for order
    private static final String ORDER_REGEX = "(\\w+\\.\\w+)\\s+(ASC|DESC)";

    public static class ParsedOrder {
        private String term;
        private String order;

        public ParsedOrder(final String term, final String order) {
            this.term = term;
            this.order = order;
        }

        public String getTerm() {
            return term;
        }

        public String getOrder() {
            return order;
        }

        @Override
        public String toString() {
            return "Term: " + term + ", Order: " + order;
        }
    }

    public static ParsedOrder parseOrder(final String expression) throws IllegalArgumentException {

        if (Objects.isNull(expression)) {
            throw new IllegalArgumentException("The expression can not be null.");
        }

        // this should be cached and checked
        final Pattern pattern = Pattern.compile(ORDER_REGEX, Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(expression.trim());

        if (matcher.matches()) {
            String term = matcher.group(1);   // Ex: Events.day
            String order = matcher.group(2).toUpperCase();  // Ex: ASC o DESC

            return new ParsedOrder(term, order);
        } else {
            throw new IllegalArgumentException("The expression is not valid. The format should be 'Term ASC' or 'Term DESC'.");
        }
    }
}
