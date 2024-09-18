package com.dotcms.analytics.query;

import com.dotcms.cube.CubeJSQuery;
import com.dotcms.cube.filters.Filter;
import com.dotmarketing.exception.DotRuntimeException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    public void test_parsing_a_good_query() throws Exception {

        final AnalyticsQueryParser analyticsQueryParser = new AnalyticsQueryParser();

        final AnalyticsQuery query = testAnalyticsQueryAndReturn(analyticsQueryParser);

        final CubeJSQuery cubeJSQuery = analyticsQueryParser.parseQueryToCubeQuery(query);
        Assert.assertNotNull("CubeJSQuery can not be null", cubeJSQuery);

        testFilters(cubeJSQuery);

        testOrders(cubeJSQuery);

        Assert.assertEquals("Limit should be 100", 100, cubeJSQuery.limit());
        Assert.assertEquals("Offset should be 1", 1, cubeJSQuery.offset());

        testTimeDimensions(cubeJSQuery);

        final String [] dimensions = cubeJSQuery.dimensions();
        Assert.assertNotNull("dimensions can not be null", dimensions);
        Assert.assertEquals("The dimensions should have 7 elements", 7, dimensions.length);

        Assert.assertTrue("Dimensions should have Events.experiment:", Arrays.asList(dimensions).contains("Events.experiment"));
        Assert.assertTrue("Dimensions should have Events.referer:", Arrays.asList(dimensions).contains("Events.referer"));
        Assert.assertTrue("Dimensions should have Events.variant:", Arrays.asList(dimensions).contains("Events.variant"));

        final String [] measures = cubeJSQuery.measures();

        Assert.assertNotNull("dimensions can not be null", measures);
        Assert.assertEquals("The measures should have 2 elements", 2, measures.length);

        Assert.assertTrue("Measures should have Events.count:", Arrays.asList(measures).contains("Events.count"));
        Assert.assertTrue("Measures should have Events.uniqueCount:", Arrays.asList(measures).contains("Events.uniqueCount"));
    }

    private static void testTimeDimensions(final CubeJSQuery cubeJSQuery) {

        final CubeJSQuery.TimeDimension[] timeDimensions = cubeJSQuery.timeDimensions();
        Assert.assertNotNull("timeDimensions can not be null", timeDimensions);
        Assert.assertEquals("The timeDimensions should have 1 element", 1, timeDimensions.length);
        Assert.assertEquals("The timeDimensions first element, dimension should be Events.day", "Events.day", timeDimensions[0].getDimension());
        Assert.assertEquals("The timeDimensions first element, granularity should be Events.day", "day", timeDimensions[0].getGranularity());
    }

    private static void testOrders(final CubeJSQuery cubeJSQuery) {

        final CubeJSQuery.OrderItem [] orderItems = cubeJSQuery.orders();
        Assert.assertNotNull("orders can not be null", orderItems);
        Assert.assertEquals("The orders should have 1 element", 1, orderItems.length);
        Assert.assertEquals("The order member should be Events.day", "Events.day", orderItems[0].getOrderBy());
        Assert.assertEquals("The order direction should be ASC", "ASC", orderItems[0].getOrder().name());
    }

    private static void testFilters(final CubeJSQuery cubeJSQuery) {

        final Filter[] filters = cubeJSQuery.filters();
        Assert.assertNotNull("filters can not be null", filters);

        Assert.assertEquals("The filters should have 1 element", 1, filters.length);
        Assert.assertTrue("First filter element type should be an OR", filters[0].asMap().containsKey("or"));
        final List<Map<String, Object>> filterValues = (List<Map<String, Object>>) filters[0].asMap().get("or");
        Assert.assertNotNull("Filter values can not be null", filterValues);
        Assert.assertEquals("Filter values should have 2 elements", 2, filterValues.size());

        Assert.assertEquals("On the first filter element, member should be Events.variant", "Events.variant", filterValues.get(0).get("member"));
        Assert.assertEquals("On the first filter element, operator should be equals", "equals", filterValues.get(0).get("operator"));
        Assert.assertEquals("On the first filter element, values should be B", "B", ((Object[])filterValues.get(0).get("values"))[0]);

        Assert.assertEquals("On the second filter element, member should be Events.experiments", "Events.experiments", filterValues.get(1).get("member"));
        Assert.assertEquals("On the second filter element, operator should be equals", "equals", filterValues.get(1).get("operator"));
        Assert.assertEquals("On the second filter element, values should be B", "B", ((Object[])filterValues.get(1).get("values"))[0]);
    }

    private static AnalyticsQuery testAnalyticsQueryAndReturn(AnalyticsQueryParser analyticsQueryParser) {
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
        return query;
    }

    /**
     * Parse a query with sintax errors
     * {
     * 	"dimensions": ["Events.referer", "Events.experiment", "Events.variant", "Events.utcTime", "Events.url", "Events.lookBackWindow", "Events.eventType"],
     * 	"measures": ["Events.count", "Events.uniqueCount",
     * 	"filters": "Events.variant = ['B'] or Events.experiments = ['B']",
     * 	"limit":100,
     * 	"offset":1,
     * 	"timeDimensions":Events.day day",
     * 	"orders":"Events.day ASC"
     * }
     *
     * should throw an DotRuntimeException
     */
    @Test(expected = DotRuntimeException.class)
    public void test_parsing_a_bad_sintax_query() throws Exception {

        final AnalyticsQueryParser analyticsQueryParser = new AnalyticsQueryParser();

        final AnalyticsQuery query = analyticsQueryParser.parseJsonToQuery(
                "{\n" +
                        "\t\"dimensions\": [\"Events.referer\", \"Events.experiment\", \"Events.variant\", \"Events.utcTime\", \"Events.url\", \"Events.lookBackWindow\", \"Events.eventType\"],\n" +
                        "\t\"measures\": [\"Events.count\", \"Events.uniqueCount\",\n" +
                        "\t\"filters\": \"Events.variant = ['B'] or Events.experiments = ['B']\",\n" +
                        "\t\"limit\":100,\n" +
                        "\t\"offset\":1,\n" +
                        "\t\"timeDimensions\":Events.day day\",\n" +
                        "\t\"orders\":\"Events.day ASC\"\n" +
                        "}");

    }

    /**
     * Parse a valid json query with sintax inside the order errors
     * {
     * 	"dimensions": ["Events.referer", "Events.experiment", "Events.variant", "Events.utcTime", "Events.url", "Events.lookBackWindow", "Events.eventType"],
     * 	"measures": ["Events.count", "Events.uniqueCount"],
     * 	"filters": "Events.variant = ['B'] or Events.experiments = ['B']",
     * 	"limit":100,
     * 	"offset":1,
     * 	"timeDimensions":"Events.day day",
     * 	"orders":"Events.day XXX"
     * }
     *
     * should throw an DotRuntimeException
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_parsing_a_bad_sintax_on_query() throws Exception {

        final AnalyticsQueryParser analyticsQueryParser = new AnalyticsQueryParser();

        final CubeJSQuery cubeJSQuery = analyticsQueryParser.parseJsonToCubeQuery(
                "{\n" +
                        "\t\"dimensions\": [\"Events.referer\", \"Events.experiment\", \"Events.variant\", \"Events.utcTime\", \"Events.url\", \"Events.lookBackWindow\", \"Events.eventType\"],\n" +
                        "\t\"measures\": [\"Events.count\", \"Events.uniqueCount\"],\n" +
                        "\t\"filters\": \"Events.variant = ['B'] or Events.experiments = ['B']\",\n" +
                        "\t\"limit\":100,\n" +
                        "\t\"offset\":1,\n" +
                        "\t\"timeDimensions\":\"Events.day day\",\n" +
                        "\t\"orders\":\"Events.day XXX\"\n" +
                        "}");

    }

    /**
     * Parsing null should be {@link IllegalArgumentException}
     */
    @Test (expected = IllegalArgumentException.class)
    public void test_parseJsonToQuery_null_query() throws Exception {

        final AnalyticsQueryParser analyticsQueryParser = new AnalyticsQueryParser();
        analyticsQueryParser.parseJsonToQuery(null);
    }

    /**
     * Parsing null should be {@link IllegalArgumentException}
     */
    @Test (expected = IllegalArgumentException.class)
    public void test_parseQueryToCubeQuery_null_query() throws Exception {

        final AnalyticsQueryParser analyticsQueryParser = new AnalyticsQueryParser();
        analyticsQueryParser.parseQueryToCubeQuery(null);
    }

    /**
     * Parsing null should be {@link IllegalArgumentException}
     */
    @Test (expected = IllegalArgumentException.class)
    public void test_parseJsonToCubeQuery_null_query() throws Exception {

        final AnalyticsQueryParser analyticsQueryParser = new AnalyticsQueryParser();
        analyticsQueryParser.parseJsonToCubeQuery(null);
    }

}
