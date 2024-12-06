package com.dotcms.analytics.content;

import com.dotcms.util.JsonUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.dotcms.analytics.content.ContentAnalyticsQuery.DATE_RANGE_ATTR;
import static com.dotcms.analytics.content.ContentAnalyticsQuery.DIMENSIONS_ATTR;
import static com.dotcms.analytics.content.ContentAnalyticsQuery.FILTERS_ATTR;
import static com.dotcms.analytics.content.ContentAnalyticsQuery.GRANULARITY_ATTR;
import static com.dotcms.analytics.content.ContentAnalyticsQuery.MEASURES_ATTR;
import static com.dotcms.analytics.content.ContentAnalyticsQuery.MEMBER_ATTR;
import static com.dotcms.analytics.content.ContentAnalyticsQuery.OPERATOR_ATTR;
import static com.dotcms.analytics.content.ContentAnalyticsQuery.ORDER_ATTR;
import static com.dotcms.analytics.content.ContentAnalyticsQuery.TIME_DIMENSIONS_ATTR;
import static com.dotcms.analytics.content.ContentAnalyticsQuery.TIME_DIMENSIONS_DIMENSION_ATTR;
import static com.dotcms.analytics.content.ContentAnalyticsQuery.VALUES_ATTR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the {@link ContentAnalyticsQuery} class is working as expected.
 *
 * @author Jose Castro
 * @since Dec 5th, 2024
 */
public class ContentAnalyticsQueryTest {

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link ContentAnalyticsQuery.Builder#build(Map)}</li>
     *     </li>
     *     <li><b>Given Scenario: </b>Transform the parameters of a CubeJS query as simple Strings
     *     into an actual CubeJS query.</li>
     *     <li><b>Expected Result: </b>The generated CubeJS JSON query must contain the exact same
     *     parameters as the original ones, even if their formatting is different.</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getCubeJSQueryFromSimpleString() throws IOException {
        // ╔════════════════════════╗
        // ║  Generating Test Data  ║
        // ╚════════════════════════╝
        final String testQueryParams = "{\n" +
                "    \"measures\": \"count,totalSessions\",\n" +
                "    \"dimensions\": \"host,whatAmI,url\",\n" +
                "    \"timeDimensions\": \"createdAt,day:Last month\",\n" +
                "    \"filters\": \"totalRequest gt 0,whatAmI contains PAGE||FILE\",\n" +
                "    \"order\": \"count asc,createdAt asc\",\n" +
                "    \"limit\": 5,\n" +
                "    \"offset\": 0\n" +
                "}";
        final Map<String, Object> queryParamsMap = JsonUtil.getJsonFromString(testQueryParams);

        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final ContentAnalyticsQuery contentAnalyticsQuery =
                new ContentAnalyticsQuery.Builder().build(queryParamsMap);
        final String cubeJsQuery = JsonUtil.getJsonStringFromObject(contentAnalyticsQuery);
        final Map<String, Object> mapFromGeneratedQuery = JsonUtil.getJsonFromString(cubeJsQuery);

        // ╔══════════════╗
        // ║  Assertions  ║
        // ╚══════════════╝
        // Checking measures
        boolean expectedMeasures = false;
        for (final String measure : contentAnalyticsQuery.measures()) {
            expectedMeasures = ((List<String>) mapFromGeneratedQuery.get(MEASURES_ATTR)).contains(measure);
            if (!expectedMeasures) {
                break;
            }
        }
        assertTrue(expectedMeasures, "Generated measures don't match the expected ones");

        // Checking dimensions
        boolean expectedDimensions = false;
        for (final String dimension : contentAnalyticsQuery.dimensions()) {
            expectedDimensions = ((List<String>) mapFromGeneratedQuery.get(DIMENSIONS_ATTR)).contains(dimension);
            if (!expectedDimensions) {
                break;
            }
        }
        assertTrue(expectedDimensions, "Generated dimensions don't match the expected ones");

        // Checking time dimensions
        boolean expectedTimeDimensions = false;
        for (final Map<String, String> timeDimension : contentAnalyticsQuery.timeDimensions()) {
            final Map<String, String> generatedTimeDimension =
                    ((List<Map<String, String>>) mapFromGeneratedQuery.get(TIME_DIMENSIONS_ATTR)).get(0);
            expectedTimeDimensions =
                    generatedTimeDimension.containsKey(TIME_DIMENSIONS_DIMENSION_ATTR)
                    && generatedTimeDimension.containsKey(GRANULARITY_ATTR)
                    && generatedTimeDimension.containsKey(DATE_RANGE_ATTR)
                    && generatedTimeDimension.get(TIME_DIMENSIONS_DIMENSION_ATTR).equals(timeDimension.get(TIME_DIMENSIONS_DIMENSION_ATTR))
                    && generatedTimeDimension.get(GRANULARITY_ATTR).equals(timeDimension.get(GRANULARITY_ATTR))
                    && generatedTimeDimension.get(DATE_RANGE_ATTR).equals(timeDimension.get(DATE_RANGE_ATTR));
            if (!expectedTimeDimensions) {
                break;
            }
        }
        assertTrue(expectedTimeDimensions, "Generated time dimensions don't match the expected ones");

        // Checking filters and values for each filter
        boolean expectedFilters = false;
        for (final Map<String, Object> filter : contentAnalyticsQuery.filters()) {
            for (final Map<String, Object> generatedFilter : (List<Map<String, Object>>) mapFromGeneratedQuery.get(FILTERS_ATTR)) {
                if (generatedFilter.get(MEMBER_ATTR).equals(filter.get(MEMBER_ATTR))) {
                    final String generatedValues = generatedFilter.get(VALUES_ATTR).toString();
                    final String expectedValues =
                            Arrays.asList((String[]) filter.get(VALUES_ATTR)).toString();
                    expectedFilters = generatedFilter.containsKey(MEMBER_ATTR)
                            && generatedFilter.containsKey(OPERATOR_ATTR)
                            && generatedFilter.containsKey(VALUES_ATTR)
                            && generatedFilter.get(MEMBER_ATTR).equals(filter.get(MEMBER_ATTR))
                            && generatedFilter.get(OPERATOR_ATTR).equals(filter.get(OPERATOR_ATTR))
                            && generatedValues.equals(expectedValues);
                    if (expectedFilters) {
                        continue;
                    }
                    break;
                }
            }
        }
        assertTrue(expectedFilters, "Generated filters don't match the expected ones");

        // Checking order
        boolean expectedOrder = false;
        for (final String[] order : contentAnalyticsQuery.order()) {
            final String key = order[0];
            for (final List<String> generatedOrder : (List<List<String>>)mapFromGeneratedQuery.get(ORDER_ATTR)) {
                expectedOrder = generatedOrder.get(0).equals(key) && generatedOrder.get(1).equals(order[1]);
                if (expectedOrder) {
                    break;
                }
            }
            if (!expectedOrder) {
                break;
            }
        }
        assertTrue(expectedOrder, "Generated order values don't match the expected ones");

        // Checking limit
        assertEquals(contentAnalyticsQuery.limit(), Integer.parseInt(mapFromGeneratedQuery.get("limit").toString()), "Generated limit value doesn't match the expected one");

        // Checking offset
        assertEquals(contentAnalyticsQuery.offset(), Integer.parseInt(mapFromGeneratedQuery.get("offset").toString()), "Generated offset value doesn't match the expected one");
    }

}
