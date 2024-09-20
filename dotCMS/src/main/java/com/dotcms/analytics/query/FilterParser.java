package com.dotcms.analytics.query;

import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for a filter expression
 * Example:
 * <pre>
 *     FilterParser.parseFilterExpression("Events.variant = ['B'] or Events.experiments = ['C']");
 * </pre>
 * should return 2 tokens and 1 logical operator
 * Tokens are member, operator and values (Events.variant, =, B) and the operator is 'and' or 'or'
 * @author jsanca
 */
public class FilterParser {

    private static final String EXPRESSION_REGEX = "(\\w+\\.\\w+)\\s*(=|!=|in|!in)\\s*\\['(.*?)'";
    private static final String LOGICAL_OPERATOR_REGEX = "\\s*(and|or)\\s*";
    private static final Pattern TOKEN_PATTERN = Pattern.compile(EXPRESSION_REGEX);
    private static final Pattern LOGICAL_PATTERN = Pattern.compile(LOGICAL_OPERATOR_REGEX);

    static class Token {
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

    enum LogicalOperator {
        AND,
        OR,
        UNKNOWN
    }

    /**
     * This method parser the filter expression such as
     * [Events.variant = [“B”] or Events.experiments = [“B”]]
     * @param expression String
     * @return return the token expression plus the logical operators
     */
    public static Tuple2<List<Token>,List<LogicalOperator>> parseFilterExpression(final String expression) {

        final List<Token> tokens = new ArrayList<>();
        final List<LogicalOperator> logicalOperators = new ArrayList<>();
        // note:Need to use cache here
        final Matcher tokenMatcher = TOKEN_PATTERN.matcher(expression);

        // Extract the tokens (member, operator, values)
        while (tokenMatcher.find()) {
            final String member = tokenMatcher.group(1);  // Example: Events.variant
            final String operator = tokenMatcher.group(2); // Example: =
            final String values = tokenMatcher.group(3);  // Example: "B"
            tokens.add(new Token(member, operator, values));
        }

        // Pattern for logical operators (and, or)
        // Need to use cache here
        final Matcher logicalMatcher = LOGICAL_PATTERN.matcher(expression);

        // Extract logical operators
        while (logicalMatcher.find()) {
            final String logicalOperator = logicalMatcher.group(1);  // Example: or, and
            logicalOperators.add(parseLogicalOperator(logicalOperator));
        }

        // if any unknown should fails
        // note: should validate logical operators should be length - 1  of the tokens???

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
