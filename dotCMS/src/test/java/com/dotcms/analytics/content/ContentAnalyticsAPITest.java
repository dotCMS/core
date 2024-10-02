package com.dotcms.analytics.content;

import com.dotcms.analytics.model.ResultSetItem;
import com.dotcms.analytics.query.AnalyticsQuery;
import com.liferay.portal.model.User;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for the {@link ContentAnalyticsAPI} class.
 *
 * @author Jose Castro
 * @since Sep 19th, 2024
 */
public class ContentAnalyticsAPITest {

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link ContentAnalyticsAPI#runReport(AnalyticsQuery, User)}
     *     </li>
     *     <li><b>Given Scenario: </b>Run a report for a given query.</li>
     *     <li><b>Expected Result: </b>The simulated results must adhere to the data specified in
     *     the query.</li>
     * </ul>
     */
    @Test
    public void getContentAnalyticsReport() {
        // ╔════════════════════════╗
        // ║  Generating Test Data  ║
        // ╚════════════════════════╝
        final List<ResultSetItem> dataList = List.of(
                new ResultSetItem(Map.of(
                        "request.count", "4",
                        "request.pageId", "9c5f42da-31b1-4935-9df6-153f5de1bdf2",
                        "request.pageTitle", "Blogs",
                        "request.url", "/blog/"
                )),
                new ResultSetItem(Map.of(
                        "request.count", "3",
                        "request.pageId", "44a076ad-affa-49d4-97b3-6caa3824e7e8",
                        "request.pageTitle", "Destinations",
                        "request.url", "/destinations/"
                )),
                new ResultSetItem(Map.of(
                        "request.count", "2",
                        "request.pageId", "a9f30020-54ef-494e-92ed-645e757171c2",
                        "request.pageTitle", "home",
                        "request.url", "/"
                ))
        );

        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final AnalyticsQuery analyticsQuery = new AnalyticsQuery.Builder()
                .measures(Set.of("request.count"))
                .orders("request.count DESC")
                .dimensions(Set.of("request.url", "request.pageId", "request.pageTitle"))
                .filters("request.whatAmI = ['PAGE']")
                .limit(100)
                .offset(1)
                .build();
        final User systemUser = new User();
        ContentAnalyticsFactory mockContentAnalyticsFactory = Mockito.mock(ContentAnalyticsFactory.class);
        Mockito.when(mockContentAnalyticsFactory.getReport(analyticsQuery, systemUser)).thenReturn(new ReportResponse(dataList));
        final ContentAnalyticsAPI contentAnalyticsAPI = new ContentAnalyticsAPIImpl(mockContentAnalyticsFactory);
        final ReportResponse report = contentAnalyticsAPI.runReport(analyticsQuery, systemUser);

        // ╔══════════════╗
        // ║  Assertions  ║
        // ╚══════════════╝
        assertNotNull("The result list must never be empty", report.getResults());
        assertEquals("There must be " + dataList.size() + " results in the list", dataList.size(), report.getResults().size());
        int i = 0;
        for (final ResultSetItem resultSetItem : report.getResults()) {
            final ResultSetItem expectedResultSetItem = dataList.get(i);
            assertEquals("Value of request.count is not the expected one", expectedResultSetItem.get("request.count").get(), resultSetItem.get("request.count").get());
            assertEquals("Value of request.pageId is not the expected one", expectedResultSetItem.get("request.pageId").get(), resultSetItem.get("request.pageId").get());
            assertEquals("Value of request.pageTitle is not the expected one", expectedResultSetItem.get("request.pageTitle").get(), resultSetItem.get("request.pageTitle").get());
            assertEquals("Value of request.url is not the expected one", expectedResultSetItem.get("request.url").get(), resultSetItem.get("request.url").get());
            i++;
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link ContentAnalyticsAPI#runRawReport(String, User)}
     *     </li>
     *     <li><b>Given Scenario: </b>Run a report for a given query.</li>
     *     <li><b>Expected Result: </b>The simulated results must adhere to the data specified in
     *     the query.</li>
     * </ul>
     */
    @Test
    public void getContentAnalyticsReportRaw() {
        // ╔════════════════════════╗
        // ║  Generating Test Data  ║
        // ╚════════════════════════╝
        final List<ResultSetItem> dataList = List.of(
                new ResultSetItem(Map.of(
                        "request.count", "4",
                        "request.pageId", "9c5f42da-31b1-4935-9df6-153f5de1bdf2",
                        "request.pageTitle", "Blogs",
                        "request.url", "/blog/"
                )),
                new ResultSetItem(Map.of(
                        "request.count", "3",
                        "request.pageId", "44a076ad-affa-49d4-97b3-6caa3824e7e8",
                        "request.pageTitle", "Destinations",
                        "request.url", "/destinations/"
                )),
                new ResultSetItem(Map.of(
                        "request.count", "2",
                        "request.pageId", "a9f30020-54ef-494e-92ed-645e757171c2",
                        "request.pageTitle", "home",
                        "request.url", "/"
                ))
        );

        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final String analyticsQuery = "{\n" +
                "  \"order\": {\n" +
                "    \"request.createdAt\": \"asc\"\n" +
                "  },\n" +
                "  \"dimensions\": [\n" +
                "    \"request.requestId\"\n" +
                "  ],\n" +
                "  \"measures\": [\n" +
                "    \"request.count\"\n" +
                "  ],\n" +
                "  \"timeDimensions\": [\n" +
                "    {\n" +
                "      \"dimension\": \"request.createdAt\",\n" +
                "      \"granularity\": \"day\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        final User systemUser = new User();
        ContentAnalyticsFactory mockContentAnalyticsFactory = Mockito.mock(ContentAnalyticsFactory.class);
        Mockito.when(mockContentAnalyticsFactory.getRawReport(analyticsQuery, systemUser)).thenReturn(new ReportResponse(dataList));
        final ContentAnalyticsAPI contentAnalyticsAPI = new ContentAnalyticsAPIImpl(mockContentAnalyticsFactory);
        final ReportResponse report = contentAnalyticsAPI.runRawReport(analyticsQuery, systemUser);

        // ╔══════════════╗
        // ║  Assertions  ║
        // ╚══════════════╝
        assertNotNull("The result list must never be empty", report.getResults());
        assertEquals("There must be " + dataList.size() + " results in the list", dataList.size(), report.getResults().size());
        int i = 0;
        for (final ResultSetItem resultSetItem : report.getResults()) {
            final ResultSetItem expectedResultSetItem = dataList.get(i);
            assertEquals("Value of request.count is not the expected one", expectedResultSetItem.get("request.count").get(), resultSetItem.get("request.count").get());
            assertEquals("Value of request.pageId is not the expected one", expectedResultSetItem.get("request.pageId").get(), resultSetItem.get("request.pageId").get());
            assertEquals("Value of request.pageTitle is not the expected one", expectedResultSetItem.get("request.pageTitle").get(), resultSetItem.get("request.pageTitle").get());
            assertEquals("Value of request.url is not the expected one", expectedResultSetItem.get("request.url").get(), resultSetItem.get("request.url").get());
            i++;
        }
    }

}
