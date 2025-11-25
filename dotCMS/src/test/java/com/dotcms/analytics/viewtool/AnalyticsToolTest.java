package com.dotcms.analytics.viewtool;

import com.dotcms.analytics.content.ContentAnalyticsAPI;
import com.dotcms.analytics.content.ReportResponse;
import com.dotcms.analytics.query.AnalyticsQueryParser;
import com.dotcms.cube.CubeJSQuery;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.liferay.portal.model.User;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test class for AnalyticsTool
 * @author jsanca
 */
public class AnalyticsToolTest {

    /**
     * Method to test: {@link AnalyticsTool#runReportFromJson(String)}
     * Given Scenario: Sending a null as json
     * ExpectedResult: Should throw IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_run_report_from_json_npe() {

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpSession session = Mockito.mock(HttpSession.class);
        final ContentAnalyticsAPI contentAnalyticsAPI = Mockito.mock(ContentAnalyticsAPI.class);
        final AnalyticsQueryParser analyticsQueryParser  = new AnalyticsQueryParser();
        final UserWebAPI userWebAPI = Mockito.mock(UserWebAPI.class);
        final ViewContext viewContext = Mockito.mock(ViewContext.class);
        final AnalyticsTool analyticsTool = new AnalyticsTool(contentAnalyticsAPI,
                    analyticsQueryParser, userWebAPI);
        final User user = new User();

        Mockito.when(viewContext.getRequest()).thenReturn(request);
        Mockito.when(request.getSession(false)).thenReturn(session);
        Mockito.when(userWebAPI.getLoggedInUser(request)).thenReturn(user);

        analyticsTool.init(viewContext);
        analyticsTool.runReportFromJson(null);
    }

    /**
     * Method to test: {@link AnalyticsTool#runReportFromJson(String)}
     * Given Scenario: Sending wrong as json
     * ExpectedResult: Should throw IllegalArgumentException wrapped on DotRuntimeException
     */
    @Test(expected = DotRuntimeException.class)
    public void test_run_report_from_json_bad_json() {

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpSession session = Mockito.mock(HttpSession.class);
        final ContentAnalyticsAPI contentAnalyticsAPI = Mockito.mock(ContentAnalyticsAPI.class);
        final AnalyticsQueryParser analyticsQueryParser  = new AnalyticsQueryParser();
        final UserWebAPI userWebAPI = Mockito.mock(UserWebAPI.class);
        final ViewContext viewContext = Mockito.mock(ViewContext.class);
        final AnalyticsTool analyticsTool = new AnalyticsTool(contentAnalyticsAPI,
                analyticsQueryParser, userWebAPI);
        final User user = new User();

        Mockito.when(viewContext.getRequest()).thenReturn(request);
        Mockito.when(request.getSession(false)).thenReturn(session);
        Mockito.when(userWebAPI.getLoggedInUser(request)).thenReturn(user);

        analyticsTool.init(viewContext);
        analyticsTool.runReportFromJson("{\n" +
                "\t\"dimensions\": [\"Events.referer\", \"Events.experiment\", \"Events.variant\", \"Events.utcTime\", \"Events.url\", \"Events.lookBackWindow\", \"Events.eventType\"],\n" +
                "\t\"measures\": [\"Events.count\", \"Events.uniqueCount\"],\n" +
                "\t\"filters\": \"Events.variant = ['B'] or Events.experiments = ['B']\",\n" +
                "\t\"limit\":100,\n" +
                "\t\"offset\":1,\n" +
                "\t\"timeDimensions\":Events.day day\",\n" + // here is a sintax error
                "\t\"orders\":\"Events.day ASC\"\n" +
                "}");
    }

    /**
     * Method to test: {@link AnalyticsTool#runReportFromJson(String)}
     * Given Scenario: Sending a good as json
     * ExpectedResult: Should a non null ReportResponse
     */
    @Test()
    public void test_run_report_from_json_good_json() {

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpSession session = Mockito.mock(HttpSession.class);
        final ContentAnalyticsAPI contentAnalyticsAPI = Mockito.mock(ContentAnalyticsAPI.class);
        final AnalyticsQueryParser analyticsQueryParser  = new AnalyticsQueryParser();
        final UserWebAPI userWebAPI = Mockito.mock(UserWebAPI.class);
        final ViewContext viewContext = Mockito.mock(ViewContext.class);
        final AnalyticsTool analyticsTool = new AnalyticsTool(contentAnalyticsAPI,
                analyticsQueryParser, userWebAPI);
        final User user = new User();

        Mockito.when(viewContext.getRequest()).thenReturn(request);
        Mockito.when(request.getSession(false)).thenReturn(session);
        Mockito.when(userWebAPI.getLoggedInUser(request)).thenReturn(user);
        Mockito.when(contentAnalyticsAPI.runReport(Mockito.any(), Mockito.eq(user))).thenReturn(new ReportResponse(List.of()));

        analyticsTool.init(viewContext);
        final ReportResponse reportResponse = analyticsTool.runReportFromJson("{\n" +
                "\t\"dimensions\": [\"Events.referer\", \"Events.experiment\", \"Events.variant\", \"Events.utcTime\", \"Events.url\", \"Events.lookBackWindow\", \"Events.eventType\"],\n" +
                "\t\"measures\": [\"Events.count\", \"Events.uniqueCount\"],\n" +
                "\t\"filters\": \"Events.variant = ['B'] or Events.experiments = ['B']\",\n" +
                "\t\"limit\":100,\n" +
                "\t\"offset\":1,\n" +
                "\t\"timeDimensions\":\"Events.day day\",\n" +
                "\t\"orders\":\"Events.day ASC\"\n" +
                "}");

        Assert.assertNotNull(reportResponse);
    }

    /**
     * Method to test: {@link AnalyticsTool#runReportFromMap(Map)}
     * Given Scenario: Sending a null map
     * ExpectedResult: Should throw {@link IllegalArgumentException}
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_run_report_from_map_with_null_map() {

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpSession session = Mockito.mock(HttpSession.class);
        final ContentAnalyticsAPI contentAnalyticsAPI = Mockito.mock(ContentAnalyticsAPI.class);
        final AnalyticsQueryParser analyticsQueryParser  = new AnalyticsQueryParser();
        final UserWebAPI userWebAPI = Mockito.mock(UserWebAPI.class);
        final ViewContext viewContext = Mockito.mock(ViewContext.class);
        final AnalyticsTool analyticsTool = new AnalyticsTool(contentAnalyticsAPI,
                analyticsQueryParser, userWebAPI);
        final User user = new User();

        Mockito.when(viewContext.getRequest()).thenReturn(request);
        Mockito.when(request.getSession(false)).thenReturn(session);
        Mockito.when(userWebAPI.getLoggedInUser(request)).thenReturn(user);
        Mockito.when(contentAnalyticsAPI.runReport(Mockito.any(), Mockito.eq(user))).thenReturn(new ReportResponse(List.of()));

        analyticsTool.init(viewContext);
        final ReportResponse reportResponse = analyticsTool.runReportFromMap(null);

        Assert.assertNotNull(reportResponse);
    }

    /**
     * Method to test: {@link AnalyticsTool#runReportFromMap(Map)}
     * Given Scenario: Sending a good as map
     * ExpectedResult: Should a non null ReportResponse
     */
    @Test()
    public void test_run_report_from_map_good_map() {

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpSession session = Mockito.mock(HttpSession.class);
        final ContentAnalyticsAPI contentAnalyticsAPI = Mockito.mock(ContentAnalyticsAPI.class);
        final AnalyticsQueryParser analyticsQueryParser  = new AnalyticsQueryParser();
        final UserWebAPI userWebAPI = Mockito.mock(UserWebAPI.class);
        final ViewContext viewContext = Mockito.mock(ViewContext.class);
        final AnalyticsTool analyticsTool = new AnalyticsTool(contentAnalyticsAPI,
                analyticsQueryParser, userWebAPI);
        final User user = new User();

        Mockito.when(viewContext.getRequest()).thenReturn(request);
        Mockito.when(request.getSession(false)).thenReturn(session);
        Mockito.when(userWebAPI.getLoggedInUser(request)).thenReturn(user);
        Mockito.when(contentAnalyticsAPI.runReport(Mockito.any(), Mockito.eq(user))).thenReturn(new ReportResponse(List.of()));

        analyticsTool.init(viewContext);
        final Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("dimensions", List.of("Events.referer", "Events.experiment", "Events.variant", "Events.utcTime", "Events.url", "Events.lookBackWindow", "Events.eventType"));
        queryMap.put("measures", List.of("Events.count", "Events.uniqueCount"));
        queryMap.put("filters", "Events.variant = ['B'] or Events.experiments = ['B']");
        queryMap.put("limit", 100);
        queryMap.put("offset", 1);
        queryMap.put("timeDimensions", "Events.day day");
        queryMap.put("orders", "Events.day ASC");
        final ReportResponse reportResponse = analyticsTool.runReportFromMap(queryMap);

        Assert.assertNotNull(reportResponse);
    }

    /**
     * Method to test: {@link AnalyticsTool#runRawReport(CubeJSQuery)}
     * Given Scenario: Sending a null json
     * ExpectedResult: Should throw {@link IllegalArgumentException}
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_run_raw_report_from_json_npe() {

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpSession session = Mockito.mock(HttpSession.class);
        final ContentAnalyticsAPI contentAnalyticsAPI = Mockito.mock(ContentAnalyticsAPI.class);
        final AnalyticsQueryParser analyticsQueryParser  = new AnalyticsQueryParser();
        final UserWebAPI userWebAPI = Mockito.mock(UserWebAPI.class);
        final ViewContext viewContext = Mockito.mock(ViewContext.class);
        final AnalyticsTool analyticsTool = new AnalyticsTool(contentAnalyticsAPI,
                analyticsQueryParser, userWebAPI);
        final User user = new User();

        Mockito.when(viewContext.getRequest()).thenReturn(request);
        Mockito.when(request.getSession(false)).thenReturn(session);
        Mockito.when(userWebAPI.getLoggedInUser(request)).thenReturn(user);
        Mockito.when(contentAnalyticsAPI.runReport(Mockito.any(), Mockito.eq(user))).thenReturn(new ReportResponse(List.of()));

        analyticsTool.init(viewContext);
        analyticsTool.runRawReport(null);
    }

    /**
     * Method to test: {@link AnalyticsTool#runRawReport(CubeJSQuery)}
     * Given Scenario: Sending a good json
     * ExpectedResult: Should return not null ReportResponse
     */
    @Test()
    public void test_run_raw_report_from_json_good_job() {

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpSession session = Mockito.mock(HttpSession.class);
        final ContentAnalyticsAPI contentAnalyticsAPI = Mockito.mock(ContentAnalyticsAPI.class);
        final AnalyticsQueryParser analyticsQueryParser  = new AnalyticsQueryParser();
        final UserWebAPI userWebAPI = Mockito.mock(UserWebAPI.class);
        final ViewContext viewContext = Mockito.mock(ViewContext.class);
        final AnalyticsTool analyticsTool = new AnalyticsTool(contentAnalyticsAPI,
                analyticsQueryParser, userWebAPI);
        final User user = new User();

        Mockito.when(viewContext.getRequest()).thenReturn(request);
        Mockito.when(request.getSession(false)).thenReturn(session);
        Mockito.when(userWebAPI.getLoggedInUser(request)).thenReturn(user);
        Mockito.when(contentAnalyticsAPI.runRawReport(Mockito.any(CubeJSQuery.class), Mockito.eq(user))).thenReturn(new ReportResponse(List.of()));

        analyticsTool.init(viewContext);
        final CubeJSQuery.Builder builder = analyticsTool.createCubeJSQueryBuilder();
        builder.dimensions("Events.experiment", "Events.variant");
        final ReportResponse reportResponse = analyticsTool.runRawReport(builder.build());
        Assert.assertNotNull(reportResponse);
    }
}
