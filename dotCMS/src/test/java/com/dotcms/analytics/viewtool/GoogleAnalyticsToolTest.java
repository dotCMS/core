package com.dotcms.analytics.viewtool;

import com.dotcms.rest.api.v1.site.SiteResource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.HostWebAPI;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GoogleAnalyticsTool ViewTool
 * 
 * @author dotCMS
 */
public class GoogleAnalyticsToolTest {

    private GoogleAnalyticsTool tool;
    private HostWebAPI hostWebAPI;
    private HttpServletRequest request;
    private Host site;

    @Before
    public void setUp() {
        hostWebAPI = mock(HostWebAPI.class);
        request = mock(HttpServletRequest.class);
        site = mock(Host.class);
        
        tool = new GoogleAnalyticsTool(hostWebAPI);
        
        // Initialize with ViewContext
        ViewContext viewContext = mock(ViewContext.class);
        when(viewContext.getRequest()).thenReturn(request);
        tool.init(viewContext);
    }

    /**
     * Test that tracking ID is retrieved from site when set
     */
    @Test
    public void testGetTrackingId_whenSet() {
        // Given: Site has GA tracking ID
        when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(site);
        when(site.getStringProperty(SiteResource.GOOGLE_ANALYTICS)).thenReturn("G-ABC123XYZ");

        // When: Getting tracking ID
        String trackingId = tool.getTrackingId();

        // Then: Should return the tracking ID
        assertEquals("G-ABC123XYZ", trackingId);
    }

    /**
     * Test that null is returned when tracking ID is not set
     */
    @Test
    public void testGetTrackingId_whenNotSet() {
        // Given: Site has no GA tracking ID
        when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(site);
        when(site.getStringProperty(SiteResource.GOOGLE_ANALYTICS)).thenReturn(null);

        // When: Getting tracking ID
        String trackingId = tool.getTrackingId();

        // Then: Should return null
        assertNull(trackingId);
    }

    /**
     * Test that null is returned when tracking ID is empty string
     */
    @Test
    public void testGetTrackingId_whenEmpty() {
        // Given: Site has empty GA tracking ID
        when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(site);
        when(site.getStringProperty(SiteResource.GOOGLE_ANALYTICS)).thenReturn("");

        // When: Getting tracking ID
        String trackingId = tool.getTrackingId();

        // Then: Should return null
        assertNull(trackingId);
    }

    /**
     * Test that null is returned when site is null
     */
    @Test
    public void testGetTrackingId_whenNoSite() {
        // Given: No site available
        when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(null);

        // When: Getting tracking ID
        String trackingId = tool.getTrackingId();

        // Then: Should return null
        assertNull(trackingId);
    }

    /**
     * Test that tracking code is generated correctly for GA4
     */
    @Test
    public void testGetTrackingCode_ga4Format() {
        // Given: Site has GA4 tracking ID
        when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(site);
        when(site.getStringProperty(SiteResource.GOOGLE_ANALYTICS)).thenReturn("G-ABC123XYZ");

        // When: Getting tracking code
        String trackingCode = tool.getTrackingCode();

        // Then: Should contain GA4 script elements
        assertNotNull(trackingCode);
        assertTrue("Should contain gtag.js script tag", 
                  trackingCode.contains("gtag.js?id=G-ABC123XYZ"));
        assertTrue("Should contain gtag config", 
                  trackingCode.contains("gtag('config', 'G-ABC123XYZ')"));
        assertTrue("Should contain dataLayer", 
                  trackingCode.contains("window.dataLayer"));
        assertTrue("Should contain async attribute", 
                  trackingCode.contains("async"));
        assertTrue("Should have Google tag comment", 
                  trackingCode.contains("<!-- Google tag (gtag.js) -->"));
    }

    /**
     * Test that tracking code contains tracking ID twice (in script src and config)
     */
    @Test
    public void testGetTrackingCode_containsIdTwice() {
        // Given: Site has GA4 tracking ID
        when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(site);
        when(site.getStringProperty(SiteResource.GOOGLE_ANALYTICS)).thenReturn("G-TEST123");

        // When: Getting tracking code
        String trackingCode = tool.getTrackingCode();

        // Then: Should contain tracking ID twice
        int firstOccurrence = trackingCode.indexOf("G-TEST123");
        int secondOccurrence = trackingCode.indexOf("G-TEST123", firstOccurrence + 1);
        
        assertTrue("Should contain tracking ID at least once", firstOccurrence > 0);
        assertTrue("Should contain tracking ID twice", secondOccurrence > firstOccurrence);
    }

    /**
     * Test that empty string is returned when no tracking ID is set
     */
    @Test
    public void testGetTrackingCode_whenNoTrackingId() {
        // Given: Site has no GA tracking ID
        when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(site);
        when(site.getStringProperty(SiteResource.GOOGLE_ANALYTICS)).thenReturn(null);

        // When: Getting tracking code
        String trackingCode = tool.getTrackingCode();

        // Then: Should return empty string
        assertEquals("", trackingCode);
    }

    /**
     * Test that empty string is returned when site is null
     */
    @Test
    public void testGetTrackingCode_whenNoSite() {
        // Given: No site available
        when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(null);

        // When: Getting tracking code
        String trackingCode = tool.getTrackingCode();

        // Then: Should return empty string
        assertEquals("", trackingCode);
    }

    /**
     * Test that tracking code is valid HTML/JavaScript
     */
    @Test
    public void testGetTrackingCode_validHtml() {
        // Given: Site has GA4 tracking ID
        when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(site);
        when(site.getStringProperty(SiteResource.GOOGLE_ANALYTICS)).thenReturn("G-REAL123");

        // When: Getting tracking code
        String trackingCode = tool.getTrackingCode();

        // Then: Should have proper HTML structure
        assertTrue("Should have opening script tag", 
                  trackingCode.contains("<script"));
        assertTrue("Should have closing script tag", 
                  trackingCode.contains("</script>"));
        assertTrue("Should have gtag function definition", 
                  trackingCode.contains("function gtag()"));
        assertTrue("Should push to dataLayer", 
                  trackingCode.contains("dataLayer.push(arguments)"));
    }

    /**
     * Test that tracking code handles special characters in tracking ID
     */
    @Test
    public void testGetTrackingCode_withSpecialCharacters() {
        // Given: Site has tracking ID with hyphens
        when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(site);
        when(site.getStringProperty(SiteResource.GOOGLE_ANALYTICS)).thenReturn("G-ABC-123-XYZ");

        // When: Getting tracking code
        String trackingCode = tool.getTrackingCode();

        // Then: Should contain the full tracking ID
        assertTrue("Should contain full tracking ID", 
                  trackingCode.contains("G-ABC-123-XYZ"));
    }

    /**
     * Test that tool can be initialized without ViewContext
     */
    @Test
    public void testInit_withoutViewContext() {
        // Given: New tool instance
        GoogleAnalyticsTool newTool = new GoogleAnalyticsTool(hostWebAPI);

        // When: Initializing with null
        newTool.init(null);

        // Then: Should not throw exception (request will be null but handled gracefully)
        // This is a safety test - tool should handle null initialization
    }

    /**
     * Test that default constructor works
     */
    @Test
    public void testDefaultConstructor() {
        // When: Creating tool with default constructor
        GoogleAnalyticsTool defaultTool = new GoogleAnalyticsTool();

        // Then: Should not be null
        assertNotNull(defaultTool);
    }
}
