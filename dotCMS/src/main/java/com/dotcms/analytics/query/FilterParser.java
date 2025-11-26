package com.dotcms.analytics.query;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class exposes a parser for the {@code filters} attribute in our Analytics Query object. For
 * example, the following expression:
 * <pre>
 *     {@code FilterParser.parseFilterExpression("Events.variant = ['B'] or Events.experiments = ['C']");}
 * </pre>
 * <p>Should return 2 tokens and 1 logical operator. Tokens are composed of a member, an operator,
 * and one or more values.</p>
 *
 * @author jsanca
 * @since Sep 19th, 2024
 */
public class FilterParser {

    private static final String EXPRESSION_REGEX = "(\\w+\\.\\w+)\\s*(=|!=|in|!in)\\s*\\[\\s*((?:'([^']*)'|\\d+)(?:\\s*,\\s*(?:'([^']*)'|\\d+))*)\\s*]";
    private static final String LOGICAL_OPERATOR_REGEX = "\\s*(and|or)\\s*";

    private static final Pattern TOKEN_PATTERN = Pattern.compile(EXPRESSION_REGEX);
    private static final Pattern LOGICAL_PATTERN = Pattern.compile(LOGICAL_OPERATOR_REGEX);

    public static class Token {

        String member;
        String operator;
        String values;

        public Token(final String member,
                     final String operator,
                     final String values) {
            this.member = member;
            this.operator = operator;
            this.values = values;
        }

    }

    public enum LogicalOperator {
        AND,
        OR,
        UNKNOWN
    }

    /**
     * Parses the value of the {@code filter} attribute, allowing both single and multiple values.
     * For instance, the following expression:
     * <pre>
     *     {@code request.whatAmI = ['PAGE','FILE'] and request.url in ['/blog']}
     * </pre>
     * <p>Allows you to retrieve results for both HTML Pages and Files whose URL contains the
     * String {@code "/blog"}.</p>
     *
     * @param expression The value of the {@code filter} attribute.
     *
     * @return A {@link Tuple2} object containing token expression plus the logical operators.
     */
    public static Tuple2<List<Token>,List<LogicalOperator>> parseFilterExpression(final String expression) {
        final List<Token> tokens = new ArrayList<>();
        final List<LogicalOperator> logicalOperators = new ArrayList<>();
        if (UtilMethods.isNotSet(expression)) {
            return Tuple.of(tokens, logicalOperators);
        }
        // TODO: We need to use cache here
        final Matcher tokenMatcher = TOKEN_PATTERN.matcher(expression);

        while (tokenMatcher.find()) {
            final String member = tokenMatcher.group(1);  // Example: Events.variant
            final String operator = tokenMatcher.group(2); // Example: =
            final String values = tokenMatcher.group(3);  // Example: "'B'", or multiple values such as "'A', 'B'"
            tokens.add(new Token(member, operator, values));
        }

        // Pattern for logical operators (and, or)
        // TODO: Need to use cache here
        final Matcher logicalMatcher = LOGICAL_PATTERN.matcher(expression);

        // Extract logical operators
        while (logicalMatcher.find()) {
            final String logicalOperator = logicalMatcher.group(1);  // Example: or, and
            logicalOperators.add(parseLogicalOperator(logicalOperator));
        }

        if (tokens.isEmpty() && logicalOperators.isEmpty()) {
            Logger.warn(FilterParser.class, String.format("Filter expression failed to be parsed: %s", expression));
        }
        return Tuple.of(tokens, logicalOperators);
    }

    private static LogicalOperator parseLogicalOperator(final String logicalOperator) {

        switch (logicalOperator.toLowerCase()) {
            case "and":
                return LogicalOperator.AND;
            case "or":
                return LogicalOperator.OR;
            default:
                return LogicalOperator.UNKNOWN;
        }
    }

}
