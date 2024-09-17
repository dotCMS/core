package com.dotcms.analytics.query;

import com.dotcms.cube.CubeJSQuery;
import com.dotcms.cube.filters.Filter;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link AnalyticsQueryParser}
 * @author jsanca
 */
public class AnalyticsQueryParserTest {

    /**
     * Parse a simple query such as
     * {
     * 	"dimensions": ["Events.referer", "Events.experiment", "Events.variant", "Events.utcTime", "Events.url", "Events.lookBackWindow", "Events.eventType"],
     * 	"measures": ["Events.count", "Events.uniqueCount"],
     * 	"filters": "Events.variant = ['B'] or Events.experiments = ['B']",
     * 	"limit":100,
     * 	"offset":1,
     * 	"timeDimensions":"Events.day day",
     * 	"orders":"Events.day ASC"
     * }
     *
     * 1) parseJsonToQuery should return a valid query based on the parsed
     * 2) parseJsonToCubeQuery returns a valid cube query based on the analytics query
     */
    @Test
    public void test_simple_query() throws Exception {

        final AnalyticsQueryParser analyticsQueryParser = new AnalyticsQueryParser();

        final AnalyticsQuery query = analyticsQueryParser.parseJsonToQuery(
                "{\n" +
                "\t\"dimensions\": [\"Events.referer\", \"Events.experiment\", \"Events.variant\", \"Events.utcTime\", \"Events.url\", \"Events.lookBackWindow\", \"Events.eventType\"],\n" +
                "\t\"measures\": [\"Events.count\", \"Events.uniqueCount\"],\n" +
                "\t\"filters\": \"Events.variant = ['B'] or Events.experiments = ['B']\",\n" +
                "\t\"limit\":100,\n" +
                "\t\"offset\":1,\n" +
                "\t\"timeDimensions\":\"Events.day day\",\n" +
                "\t\"orders\":\"Events.day ASC\"\n" +
                "}");
        Assert.assertNotNull("Query can not be null", query);
        Assert.assertTrue("Dimensions should have Events.experiment:", query.getDimensions().contains("Events.experiment"));
        Assert.assertTrue("Dimensions should have Should Events.referer:", query.getDimensions().contains("Events.referer"));
        Assert.assertTrue("Dimensions should have Should Events.variant:", query.getDimensions().contains("Events.variant"));

        Assert.assertTrue("Measures should have Events.count:", query.getMeasures().contains("Events.count"));
        Assert.assertTrue("Measures should have Events.uniqueCount:", query.getMeasures().contains("Events.uniqueCount"));

        Assert.assertEquals("The query filter is wrong","Events.variant = ['B'] or Events.experiments = ['B']", query.getFilters());
        Assert.assertEquals("Limit should be 100",100, query.getLimit());
        Assert.assertEquals("Offset should be 1", 1, query.getOffset());

        Assert.assertEquals("Time dimensions is wrong", "Events.day day", query.getTimeDimensions());

        Assert.assertEquals("Orders is wrong", "Events.day ASC", query.getOrders());

        final CubeJSQuery cubeJSQuery = analyticsQueryParser.parseQueryToCubeQuery(query);
        Assert.assertNotNull("CubeJSQuery can not be null", cubeJSQuery);

        final Filter[] filters = cubeJSQuery.filters();
        Assert.assertNotNull("filters can not be null", filters);

    }

}
