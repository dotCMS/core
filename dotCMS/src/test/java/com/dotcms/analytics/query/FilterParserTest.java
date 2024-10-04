package com.dotcms.analytics.query;

import io.vavr.Tuple2;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link FilterParser} class.
 *
 * @author jsanca
 */
public class FilterParserTest  {

    /**
     * Parse a simple query
     * [Events.variant = ["B"] or Events.experiments = ["C"]]
     * should return 2 tokens and 1 logical operator
     */
    @Test
    public void test_2_tokens_parseFilterExpression_should_be_OK() {
        final Tuple2<List<FilterParser.Token>,List<FilterParser.LogicalOperator>> result =
                FilterParser.parseFilterExpression("Events.variant = ['B'] or Events.experiments = ['C']");

        assertNotNull(result);
        assertEquals(2, result._1.size()); // two tokens
        assertEquals(1, result._2.size()); // one operator
        final FilterParser.Token token1 = result._1.get(0);
        final FilterParser.Token token2 = result._1.get(1);
        assertEquals("Events.variant", token1.member);
        assertEquals("=", token1.operator);
        assertEquals("'B'", token1.values);
        assertEquals("Events.experiments", token2.member);
        assertEquals("=", token2.operator);
        assertEquals("'C'", token2.values);

        assertEquals(FilterParser.LogicalOperator.OR, result._2.get(0));
    }

    /**
     * Parse a simple query
     * [Events.variant in ["B"] and Events.experiments != ["C"] or Events.goals !no ["C"]]
     * should return 3 tokens and 2 logical operator
     */
    @Test
    public void test_3_tokens_parseFilterExpression_should_be_OK() {
        final Tuple2<List<FilterParser.Token>,List<FilterParser.LogicalOperator>> result =
                FilterParser.parseFilterExpression("Events.variant in ['B'] and Events.experiments != ['C'] or Events.goals !in ['C']");

        assertNotNull(result);
        assertEquals(3, result._1.size()); // two tokens
        assertEquals(2, result._2.size()); // one operator
        final FilterParser.Token token1 = result._1.get(0);
        final FilterParser.Token token2 = result._1.get(1);
        final FilterParser.Token token3 = result._1.get(2);
        assertEquals("Events.variant", token1.member);
        assertEquals("in", token1.operator);
        assertEquals("'B'", token1.values);
        assertEquals("Events.experiments", token2.member);
        assertEquals("!=", token2.operator);
        assertEquals("'C'", token2.values);
        assertEquals("Events.goals", token3.member);
        assertEquals("!in", token3.operator);
        assertEquals("'C'", token3.values);

        assertEquals(FilterParser.LogicalOperator.AND, result._2.get(0));
        assertEquals(FilterParser.LogicalOperator.OR, result._2.get(1));
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link FilterParser#parseFilterExpression(String)}</li>
     *     <li><b>Given Scenario: </b>Parse a filter that includes multiple values.</li>
     *     <li><b>Expected Result: </b>All values should be returned.</li>
     * </ul>
     */
    @Test
    public void parseExpressionWithMultipleValues() {
        // ╔════════════════════════╗
        // ║  Generating Test Data  ║
        // ╚════════════════════════╝
        final Tuple2<List<FilterParser.Token>,List<FilterParser.LogicalOperator>> result =
                FilterParser.parseFilterExpression("request.whatAmI = ['PAGE','FILE']");

        // ╔══════════════╗
        // ║  Assertions  ║
        // ╚══════════════╝
        assertNotNull(result);
        assertEquals("Only one token is expected", 1, result._1.size());
        assertEquals("No logical operators are expected", 0, result._2.size());
        final FilterParser.Token token1 = result._1.get(0);
        assertEquals("request.whatAmI", token1.member);
        assertEquals("=", token1.operator);
        assertEquals("'PAGE','FILE'", token1.values);
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link FilterParser#parseFilterExpression(String)}</li>
     *     <li><b>Given Scenario: </b>Parse a filter that includes multiple values and two logical
     *     operators.</li>
     *     <li><b>Expected Result: </b>All values and expressions should be returned.</li>
     * </ul>
     */
    @Test
    public void parseExpressionWithMultipleValuesAndTwoLogicalOperators() {
        // ╔════════════════════════╗
        // ║  Generating Test Data  ║
        // ╚════════════════════════╝
        final Tuple2<List<FilterParser.Token>,List<FilterParser.LogicalOperator>> result =
                FilterParser.parseFilterExpression("request.whatAmI = ['PAGE','FILE'] and request.url in ['/blog']");

        // ╔══════════════╗
        // ║  Assertions  ║
        // ╚══════════════╝
        assertNotNull(result);
        assertEquals("Two tokens are expected", 2, result._1.size());
        assertEquals("Only one logical operator is expected", 1, result._2.size());
        final FilterParser.Token token1 = result._1.get(0);
        final FilterParser.Token token2 = result._1.get(1);
        assertEquals("request.whatAmI", token1.member);
        assertEquals("=", token1.operator);
        assertEquals("'PAGE','FILE'", token1.values);
        assertEquals("request.url", token2.member);
        assertEquals("in", token2.operator);
        assertEquals("'/blog'", token2.values);
        assertEquals(FilterParser.LogicalOperator.AND, result._2.get(0));
    }

}
