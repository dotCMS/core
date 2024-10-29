package com.dotcms.analytics.query;

import com.dotcms.cube.CubeJSQuery;
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
        final CubeJSQuery.TimeDimension result =
                TimeDimensionParser.parseTimeDimension("Events.day     day");

        Assert.assertNotNull(result);
        Assert.assertEquals("Events.day", result.getDimension());
        Assert.assertEquals("day", result.getGranularity());
        Assert.assertNull(result.getDateRange());
    }

    /**
     * Parse a simple query with  dimension, granularity and dateRange
     * Events.day day This Week
     * should return Events.day and day and This Week
     */
    @Test
    public void test_parseTimeDimension_dimension_plus_granularity_and_dateRange_should_be_OK() throws Exception {
        final CubeJSQuery.TimeDimension result =
                TimeDimensionParser.parseTimeDimension("Events.day     day This Week");

        Assert.assertNotNull(result);
        Assert.assertEquals("Events.day", result.getDimension());
        Assert.assertEquals("day", result.getGranularity());
        Assert.assertEquals("This Week", result.getDateRange());
    }

    /**
     * Parse a simple query with  dimension, granularity and dateRange
     * Events.day day yesterday
     * should return Events.day and day and yesterday
     */
    @Test
    public void test_parseTimeDimension_dimension_plus_granularity_and_dateRange_yesterday_should_be_OK() throws Exception {
        final CubeJSQuery.TimeDimension result =
                TimeDimensionParser.parseTimeDimension("Events.day day yesterday");

        Assert.assertNotNull(result);
        Assert.assertEquals("Events.day", result.getDimension());
        Assert.assertEquals("day", result.getGranularity());
        Assert.assertEquals("yesterday", result.getDateRange());
    }
    /**
     * Parse a simple query with  dimension, granularity and dateRange
     * Events.day day Last 7 days
     * should return Events.day and day and Last 7 days
     */
    @Test
    public void test_parseTimeDimension_dimension_plus_granularity_and_dateRange_last_7_days_should_be_OK() throws Exception {
        final CubeJSQuery.TimeDimension result =
                TimeDimensionParser.parseTimeDimension("Events.day day Last 7 days");

        Assert.assertNotNull(result);
        Assert.assertEquals("Events.day", result.getDimension());
        Assert.assertEquals("day", result.getGranularity());
        Assert.assertEquals("Last 7 days", result.getDateRange());
    }


}
