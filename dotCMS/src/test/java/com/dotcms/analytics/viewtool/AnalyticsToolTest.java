package com.dotcms.analytics.viewtool;

import com.dotcms.analytics.content.ContentAnalyticsAPI;
import com.dotcms.analytics.content.ReportResponse;
import com.dotcms.analytics.query.AnalyticsQueryParser;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.fasterxml.jackson.core.JsonParseException;
import com.liferay.portal.model.User;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

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
}
