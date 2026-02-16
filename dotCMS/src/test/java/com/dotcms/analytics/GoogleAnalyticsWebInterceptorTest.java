package com.dotcms.analytics;

import com.dotcms.UnitTestBase;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.rest.api.v1.site.SiteResource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GoogleAnalyticsWebInterceptor
 * 
 * @author dotCMS
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Config.class, WebAPILocator.class, PageMode.class})
public class GoogleAnalyticsWebInterceptorTest extends UnitTestBase {

    private GoogleAnalyticsWebInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Host site;
    private HostWebAPI hostWebAPI;

    @Before
    public void setUp() {
        interceptor = new GoogleAnalyticsWebInterceptor();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        site = mock(Host.class);
        hostWebAPI = mock(HostWebAPI.class);

        // Setup PowerMock statics
        PowerMockito.mockStatic(Config.class);
        PowerMockito.mockStatic(WebAPILocator.class);
        PowerMockito.mockStatic(PageMode.class);

        // Default mocks
        when(WebAPILocator.getHostWebAPI()).thenReturn(hostWebAPI);
        when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(site);
        when(Config.getBooleanProperty("GOOGLE_ANALYTICS_AUTO_INJECT", true)).thenReturn(true);
    }

    /**
     * Test that interceptor returns NEXT when auto-injection is disabled
     */
    @Test
    public void test_intercept_whenDisabled_returnsNext() throws Exception {
        // Given: Auto-injection is disabled
        when(Config.getBooleanProperty("GOOGLE_ANALYTICS_AUTO_INJECT", true)).thenReturn(false);

        // When: Interceptor is called
        final Result result = interceptor.intercept(request, response);

        // Then: Should return NEXT without wrapping
        assertEquals(Result.NEXT, result);
    }

    /**
     * Test that interceptor skips injection in EDIT_MODE
     */
    @Test
    public void test_intercept_inEditMode_returnsNext() throws Exception {
        // Given: Request is in EDIT_MODE
        when(PageMode.get(request)).thenReturn(PageMode.EDIT_MODE);

        // When: Interceptor is called
        final Result result = interceptor.intercept(request, response);

        // Then: Should return NEXT without wrapping
        assertEquals(Result.NEXT, result);
    }

    /**
     * Test that interceptor skips injection in PREVIEW_MODE
     */
    @Test
    public void test_intercept_inPreviewMode_returnsNext() throws Exception {
        // Given: Request is in PREVIEW_MODE
        when(PageMode.get(request)).thenReturn(PageMode.PREVIEW_MODE);

        // When: Interceptor is called
        final Result result = interceptor.intercept(request, response);

        // Then: Should return NEXT without wrapping
        assertEquals(Result.NEXT, result);
    }

    /**
     * Test that interceptor skips injection when site is null
     */
    @Test
    public void test_intercept_whenNoSite_returnsNext() throws Exception {
        // Given: No site is found
        when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(null);
        when(PageMode.get(request)).thenReturn(PageMode.LIVE);

        // When: Interceptor is called
        final Result result = interceptor.intercept(request, response);

        // Then: Should return NEXT without wrapping
        assertEquals(Result.NEXT, result);
    }

    /**
     * Test that interceptor skips injection when GA tracking ID is not set
     */
    @Test
    public void test_intercept_whenNoTrackingId_returnsNext() throws Exception {
        // Given: Site has no GA tracking ID
        when(site.getStringProperty(SiteResource.GOOGLE_ANALYTICS)).thenReturn(null);
        when(PageMode.get(request)).thenReturn(PageMode.LIVE);

        // When: Interceptor is called
        final Result result = interceptor.intercept(request, response);

        // Then: Should return NEXT without wrapping
        assertEquals(Result.NEXT, result);
    }

    /**
     * Test that interceptor wraps response when all conditions are met
     */
    @Test
    public void test_intercept_withValidConditions_wrapsResponse() throws Exception {
        // Given: Valid conditions for injection
        when(site.getStringProperty(SiteResource.GOOGLE_ANALYTICS)).thenReturn("G-XXXXXXXXXX");
        when(PageMode.get(request)).thenReturn(PageMode.LIVE);

        // When: Interceptor is called
        final Result result = interceptor.intercept(request, response);

        // Then: Should wrap the response
        assertNotNull(result);
        assertNotNull(result.getResponse());
    }

    /**
     * Test GA4 tracking script generation
     */
    @Test
    public void test_generateTrackingScript_ga4Format() {
        // Given: GA4 tracking ID
        final String trackingId = "G-XXXXXXXXXX";

        // When: Generating script
        final String script = GoogleAnalyticsWebInterceptor.generateTrackingScript(trackingId);

        // Then: Should contain GA4 script elements
        assertTrue(script.contains("gtag.js?id=" + trackingId));
        assertTrue(script.contains("gtag('config', '" + trackingId + "')"));
        assertTrue(script.contains("window.dataLayer"));
    }

    /**
     * Test tracking script generation with any format
     */
    @Test
    public void test_generateTrackingScript_anyFormat() {
        // Given: Any tracking ID format
        final String trackingId = "G-ABC123XYZ";

        // When: Generating script
        final String script = GoogleAnalyticsWebInterceptor.generateTrackingScript(trackingId);

        // Then: Should use GA4 format
        assertTrue(script.contains("gtag.js?id=" + trackingId));
        assertTrue(script.contains("gtag('config', '" + trackingId + "')"));
    }

    /**
     * Test HTML injection before closing body tag
     */
    @Test
    public void test_injectTrackingCode_injectsBeforeBodyClose() {
        // Given: HTML with body tag and GA4 tracking ID
        final String originalHtml = "<html><head><title>Test</title></head><body><h1>Content</h1></body></html>";
        final String trackingId = "G-XXXXXXXXXX";

        // When: Injecting tracking code
        final String modifiedHtml = GoogleAnalyticsWebInterceptor.GAResponseWrapper.injectTrackingCode(
                originalHtml, trackingId);

        // Then: Script should be injected before </body>
        assertTrue(modifiedHtml.contains("gtag.js"));
        assertTrue(modifiedHtml.indexOf("gtag.js") < modifiedHtml.indexOf("</body>"));
        assertTrue(modifiedHtml.indexOf("<h1>Content</h1>") < modifiedHtml.indexOf("gtag.js"));
    }

    /**
     * Test HTML injection with no body tag
     */
    @Test
    public void test_injectTrackingCode_noBodyTag_returnsOriginal() {
        // Given: HTML without body tag
        final String originalHtml = "<html><head><title>Test</title></head></html>";
        final String trackingId = "G-XXXXXXXXXX";

        // When: Injecting tracking code
        final String modifiedHtml = GoogleAnalyticsWebInterceptor.GAResponseWrapper.injectTrackingCode(
                originalHtml, trackingId);

        // Then: HTML should be unchanged
        assertEquals(originalHtml, modifiedHtml);
    }

    /**
     * Test HTML injection with mixed case body tag
     */
    @Test
    public void test_injectTrackingCode_mixedCaseBodyTag() {
        // Given: HTML with mixed case body tag
        final String originalHtml = "<html><head><title>Test</title></head><BODY><h1>Content</h1></BODY></html>";
        final String trackingId = "G-XXXXXXXXXX";

        // When: Injecting tracking code
        final String modifiedHtml = GoogleAnalyticsWebInterceptor.GAResponseWrapper.injectTrackingCode(
                originalHtml, trackingId);

        // Then: Script should be injected (case-insensitive search)
        assertTrue(modifiedHtml.contains("gtag.js"));
        assertTrue(modifiedHtml.indexOf("gtag.js") < modifiedHtml.toLowerCase().indexOf("</body>"));
    }

    /**
     * Test tracking script with empty ID
     */
    @Test
    public void test_generateTrackingScript_emptyId() {
        // Given: Empty tracking ID
        final String trackingId = "";

        // When: Generating script
        final String script = GoogleAnalyticsWebInterceptor.generateTrackingScript(trackingId);

        // Then: Should still generate GA4 format script
        assertTrue(script.contains("gtag.js"));
        assertTrue(script.contains("gtag('config', '')"));
    }
}
