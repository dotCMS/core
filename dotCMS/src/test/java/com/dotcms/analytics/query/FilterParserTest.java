package com.dotcms.analytics.query;


import io.vavr.Tuple2;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Test for {@link FilterParser}
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
    public void test_2_tokens_parseFilterExpression_should_be_OK() throws Exception {
        final Tuple2<List<FilterParser.Token>,List<FilterParser.LogicalOperator>> result =
                FilterParser.parseFilterExpression("Events.variant = ['B'] or Events.experiments = ['C']");

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result._1.size()); // two tokens
        Assert.assertEquals(1, result._2.size()); // one operator
        final FilterParser.Token token1 = result._1.get(0);
        final FilterParser.Token token2 = result._1.get(1);
        Assert.assertEquals("Events.variant", token1.member);
        Assert.assertEquals("=", token1.operator);
        Assert.assertEquals("B", token1.values);
        Assert.assertEquals("Events.experiments", token2.member);
        Assert.assertEquals("=", token2.operator);
        Assert.assertEquals("C", token2.values);

        Assert.assertEquals(FilterParser.LogicalOperator.OR, result._2.get(0));
    }

    /**
     * Parse a simple query
     * [Events.variant in ["B"] and Events.experiments != ["C"] or Events.goals !no ["C"]]
     * should return 3 tokens and 2 logical operator
     */
    @Test
    public void test_3_tokens_parseFilterExpression_should_be_OK() throws Exception {
        final Tuple2<List<FilterParser.Token>,List<FilterParser.LogicalOperator>> result =
                FilterParser.parseFilterExpression("Events.variant in ['B'] and Events.experiments != ['C'] or Events.goals !in ['C']");

        Assert.assertNotNull(result);
        Assert.assertEquals(3, result._1.size()); // two tokens
        Assert.assertEquals(2, result._2.size()); // one operator
        final FilterParser.Token token1 = result._1.get(0);
        final FilterParser.Token token2 = result._1.get(1);
        final FilterParser.Token token3 = result._1.get(2);
        Assert.assertEquals("Events.variant", token1.member);
        Assert.assertEquals("in", token1.operator);
        Assert.assertEquals("B", token1.values);
        Assert.assertEquals("Events.experiments", token2.member);
        Assert.assertEquals("!=", token2.operator);
        Assert.assertEquals("C", token2.values);
        Assert.assertEquals("Events.goals", token3.member);
        Assert.assertEquals("!in", token3.operator);
        Assert.assertEquals("C", token3.values);

        Assert.assertEquals(FilterParser.LogicalOperator.AND, result._2.get(0));
        Assert.assertEquals(FilterParser.LogicalOperator.OR, result._2.get(1));
    }


}
