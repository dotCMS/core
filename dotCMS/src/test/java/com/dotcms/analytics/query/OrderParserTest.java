package com.dotcms.analytics.query;


import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link OrderParser}
 *
 * @author jsanca
 */
public class OrderParserTest {

    /**
     * Parse a simple query
     * Events.day ASC
     * should return Events.day and ASC
     */
    @Test
    public void test_parseOrder_ASC_should_be_OK() throws Exception {
        final OrderParser.ParsedOrder result =
                OrderParser.parseOrder("Events.day     ASC");

        Assert.assertNotNull(result);
        Assert.assertEquals("Events.day", result.getTerm());
        Assert.assertEquals("ASC", result.getOrder());
    }

    /**
     * Parse a simple query
     * Events.day ASC
     * should return Events.day and DESC
     */
    @Test
    public void test_parseOrder_DESC_should_be_OK() throws Exception {
        final OrderParser.ParsedOrder result =
                OrderParser.parseOrder("Events.day         DESC");

        Assert.assertNotNull(result);
        Assert.assertEquals("Events.day", result.getTerm());
        Assert.assertEquals("DESC", result.getOrder());
    }

    /**
     * Parse a wrong query
     * Events.day X
     */
    @Test (expected = IllegalArgumentException.class)
    public void test_parseOrder_wrong_order() throws Exception {

        OrderParser.parseOrder("Events.day         X");
    }

    /**
     * Parse a null query
     * Events.day X
     */
    @Test (expected = IllegalArgumentException.class)
    public void test_parseOrder_null_order() throws Exception {

        OrderParser.parseOrder(null);
    }

    /**
     * Parse a wrong query
     * Events.day X
     */
    @Test (expected = IllegalArgumentException.class)
    public void test_parseOrder_wrong_term_order() throws Exception {

        OrderParser.parseOrder("         ASC ");
    }

}
