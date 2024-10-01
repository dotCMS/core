package com.dotcms.analytics.query;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link TimeDimensionParser}
 * @author jsanca
 */
public class TimeDimensionParserTest {

    /**
     * Parse a simple query with only dimension and granularity
     * Events.day day
     * should return Events.day and day
     */
    @Test
    public void test_parseTimeDimension_dimension_plus_granularity_should_be_OK() throws Exception {
        final TimeDimensionParser.TimeDimension result =
                TimeDimensionParser.parseTimeDimension("Events.day     day");

        Assert.assertNotNull(result);
        Assert.assertEquals("Events.day", result.getDimension());
        Assert.assertEquals("day", result.getGranularity());
        Assert.assertNull(result.getDateRange());
    }

    /**
     * Parse a simple query with only dimension and granularity
     * Events.day day
     * should return Events.day and day
     */
    @Test
    public void test_parseTimeDimension_dimension_plus_granularity_and_dateRange_should_be_OK() throws Exception {
        final TimeDimensionParser.TimeDimension result =
                TimeDimensionParser.parseTimeDimension("Events.day     day This Week");

        Assert.assertNotNull(result);
        Assert.assertEquals("Events.day", result.getDimension());
        Assert.assertEquals("day", result.getGranularity());
        Assert.assertEquals("This Week", result.getDateRange());
    }
}
