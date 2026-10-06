package com.dotcms.analytics.viewtool;

import com.dotcms.analytics.web.AnalyticsWebAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.HostWebAPI;
import com.liferay.util.StringPool;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Test cases for {@link AnalyticsCodeInjecterViewTool}
 */
public class AnalyticsCodeInjecterViewToolTest {

    /**
     * Method to test: {@link AnalyticsCodeInjecterViewTool#code()}
     * Given Scenario: analytics API returns a script tag
     * ExpectedResult: code() returns that script tag
     */
    @Test
    public void test_code_returns_script_when_available() throws Exception {
        final AnalyticsWebAPI analyticsWebAPI = Mockito.mock(AnalyticsWebAPI.class);
        final HostWebAPI hostWebAPI = Mockito.mock(HostWebAPI.class);
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final Host host = Mockito.mock(Host.class);
        final String expectedCode = "<script src=\"/ext/analytics/ca.min.js\"></script>";

        Mockito.when(hostWebAPI.getCurrentHost()).thenReturn(host);
        Mockito.when(analyticsWebAPI.getCode(host, request)).thenReturn(Optional.of(expectedCode));

        final AnalyticsCodeInjecterViewTool tool =
                new AnalyticsCodeInjecterViewTool(analyticsWebAPI, hostWebAPI, () -> request);

        Assert.assertEquals(expectedCode, tool.code());
    }

    /**
     * Method to test: {@link AnalyticsCodeInjecterViewTool#code()}
     * Given Scenario: analytics API returns empty (e.g. not LIVE mode or no app configured)
     * ExpectedResult: code() returns an empty string
     */
    @Test
    public void test_code_returns_empty_when_no_code() throws Exception {
        final AnalyticsWebAPI analyticsWebAPI = Mockito.mock(AnalyticsWebAPI.class);
        final HostWebAPI hostWebAPI = Mockito.mock(HostWebAPI.class);
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final Host host = Mockito.mock(Host.class);

        Mockito.when(hostWebAPI.getCurrentHost()).thenReturn(host);
        Mockito.when(analyticsWebAPI.getCode(host, request)).thenReturn(Optional.empty());

        final AnalyticsCodeInjecterViewTool tool =
                new AnalyticsCodeInjecterViewTool(analyticsWebAPI, hostWebAPI, () -> request);

        Assert.assertEquals(StringPool.BLANK, tool.code());
    }
}
