package com.dotcms.analytics;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Simple standalone tests for Google Analytics 4 (GA4) injection logic
 * that don't require PowerMock or full environment setup.
 * 
 * @author dotCMS
 */
public class GoogleAnalyticsWebInterceptorSimpleTest {

    /**
     * Test GA4 script generation
     */
    @Test
    public void testGenerateGA4TrackingScript() {
        String trackingId = "G-ABC123XYZ";
        String script = GoogleAnalyticsWebInterceptor.generateTrackingScript(trackingId);
        
        // Verify GA4 script structure
        assertTrue("Should contain gtag.js script source", 
                  script.contains("gtag.js?id=" + trackingId));
        assertTrue("Should contain gtag config call", 
                  script.contains("gtag('config', '" + trackingId + "')"));
        assertTrue("Should contain dataLayer", 
                  script.contains("window.dataLayer"));
        assertTrue("Should have GA4 comment", 
                  script.contains("<!-- Google tag (gtag.js) -->"));
    }

    /**
     * Test script generation with different GA4 tracking ID
     */
    @Test
    public void testGenerateGA4TrackingScriptDifferentId() {
        String trackingId = "G-REAL123TEST";
        String script = GoogleAnalyticsWebInterceptor.generateTrackingScript(trackingId);
        
        // Verify GA4 script structure
        assertTrue("Should contain gtag.js", 
                  script.contains("gtag.js?id=" + trackingId));
        assertTrue("Should contain gtag config", 
                  script.contains("gtag('config', '" + trackingId + "')"));
        assertTrue("Should have dataLayer", 
                  script.contains("window.dataLayer"));
    }

    /**
     * Test HTML injection at correct location
     */
    @Test
    public void testInjectTrackingCodeBeforeBodyTag() {
        String trackingId = "G-TEST123";
        String originalHtml = "<!DOCTYPE html>\n" +
                             "<html>\n" +
                             "<head><title>Test</title></head>\n" +
                             "<body>\n" +
                             "  <h1>Hello World</h1>\n" +
                             "  <p>Content here</p>\n" +
                             "</body>\n" +
                             "</html>";
        
        String modifiedHtml = GoogleAnalyticsWebInterceptor.GAResponseWrapper.injectTrackingCode(
            originalHtml, trackingId);
        
        // Verify injection occurred
        assertTrue("Should contain tracking script", 
                  modifiedHtml.contains("gtag.js"));
        
        // Verify injection is before </body>
        int scriptIndex = modifiedHtml.indexOf("gtag.js");
        int bodyCloseIndex = modifiedHtml.toLowerCase().lastIndexOf("</body>");
        assertTrue("Script should appear before </body> tag", 
                  scriptIndex < bodyCloseIndex);
        
        // Verify content is preserved
        assertTrue("Should preserve original content", 
                  modifiedHtml.contains("<h1>Hello World</h1>"));
        assertTrue("Should preserve original content", 
                  modifiedHtml.contains("<p>Content here</p>"));
    }

    /**
     * Test HTML without body tag returns unchanged
     */
    @Test
    public void testInjectTrackingCodeNoBodyTag() {
        String trackingId = "G-TEST123";
        String originalHtml = "<!DOCTYPE html>\n<html>\n<head><title>Test</title></head>\n</html>";
        
        String modifiedHtml = GoogleAnalyticsWebInterceptor.GAResponseWrapper.injectTrackingCode(
            originalHtml, trackingId);
        
        // Should be unchanged
        assertEquals("HTML without body tag should be unchanged", 
                    originalHtml, modifiedHtml);
    }

    /**
     * Test case-insensitive body tag detection
     */
    @Test
    public void testInjectTrackingCodeMixedCaseBodyTag() {
        String trackingId = "G-TEST123";
        String originalHtml = "<!DOCTYPE html>\n" +
                             "<HTML>\n" +
                             "<BODY>\n" +
                             "  <H1>Content</H1>\n" +
                             "</BODY>\n" +
                             "</HTML>";
        
        String modifiedHtml = GoogleAnalyticsWebInterceptor.GAResponseWrapper.injectTrackingCode(
            originalHtml, trackingId);
        
        // Verify injection occurred (case-insensitive search)
        assertTrue("Should contain tracking script", 
                  modifiedHtml.contains("gtag.js"));
        
        // Verify injection is before closing body tag
        int scriptIndex = modifiedHtml.indexOf("gtag.js");
        int bodyCloseIndex = modifiedHtml.toLowerCase().lastIndexOf("</body>");
        assertTrue("Script should appear before </BODY> tag", 
                  scriptIndex < bodyCloseIndex);
    }

    /**
     * Test multiple body tags (use last one)
     */
    @Test
    public void testInjectTrackingCodeMultipleBodyTags() {
        String trackingId = "G-TEST123";
        // HTML with nested body tags or multiple body references
        String originalHtml = "<!DOCTYPE html>\n" +
                             "<html>\n" +
                             "<body>\n" +
                             "  <div>Content mentioning </body> tag in text</div>\n" +
                             "  <h1>Actual Content</h1>\n" +
                             "</body>\n" +
                             "</html>";
        
        String modifiedHtml = GoogleAnalyticsWebInterceptor.GAResponseWrapper.injectTrackingCode(
            originalHtml, trackingId);
        
        // Verify injection occurred before the LAST </body> tag
        int scriptIndex = modifiedHtml.indexOf("gtag.js");
        int lastBodyCloseIndex = modifiedHtml.toLowerCase().lastIndexOf("</body>");
        assertTrue("Script should appear before last </body> tag", 
                  scriptIndex < lastBodyCloseIndex);
    }

    /**
     * Test script generation with any ID format (all use GA4)
     */
    @Test
    public void testGenerateTrackingScriptAnyFormat() {
        String trackingId = "CUSTOM-ID-123";
        String script = GoogleAnalyticsWebInterceptor.generateTrackingScript(trackingId);
        
        // Should use GA4 format for any ID
        assertTrue("Should use GA4 format", 
                  script.contains("gtag.js?id=" + trackingId));
        assertTrue("Should contain gtag config", 
                  script.contains("gtag('config', '" + trackingId + "')"));
    }

    /**
     * Test empty tracking ID
     */
    @Test
    public void testEmptyTrackingId() {
        String trackingId = "";
        String script = GoogleAnalyticsWebInterceptor.generateTrackingScript(trackingId);
        
        // Should still generate script (though it won't work)
        assertTrue("Should generate script even with empty ID", 
                  script.contains("gtag.js"));
    }

    /**
     * Test realistic page HTML
     */
    @Test
    public void testRealisticPageHTML() {
        String trackingId = "G-REAL123";
        String originalHtml = "<!DOCTYPE html>\n" +
                             "<html lang=\"en\">\n" +
                             "<head>\n" +
                             "  <meta charset=\"UTF-8\">\n" +
                             "  <title>My dotCMS Site</title>\n" +
                             "  <link rel=\"stylesheet\" href=\"/styles.css\">\n" +
                             "</head>\n" +
                             "<body class=\"home-page\">\n" +
                             "  <header>\n" +
                             "    <nav>Navigation</nav>\n" +
                             "  </header>\n" +
                             "  <main>\n" +
                             "    <h1>Welcome to dotCMS</h1>\n" +
                             "    <p>Content management made easy.</p>\n" +
                             "  </main>\n" +
                             "  <footer>\n" +
                             "    <p>&copy; 2026 dotCMS</p>\n" +
                             "  </footer>\n" +
                             "  <script src=\"/app.js\"></script>\n" +
                             "</body>\n" +
                             "</html>";
        
        String modifiedHtml = GoogleAnalyticsWebInterceptor.GAResponseWrapper.injectTrackingCode(
            originalHtml, trackingId);
        
        // Verify injection
        assertTrue("Should contain GA script", 
                  modifiedHtml.contains("gtag.js?id=" + trackingId));
        
        // Verify placement (after app.js but before </body>)
        int appJsIndex = modifiedHtml.indexOf("<script src=\"/app.js\">");
        int gaScriptIndex = modifiedHtml.indexOf("gtag.js");
        int bodyCloseIndex = modifiedHtml.lastIndexOf("</body>");
        
        assertTrue("GA script should come after other scripts", 
                  gaScriptIndex > appJsIndex);
        assertTrue("GA script should come before </body>", 
                  gaScriptIndex < bodyCloseIndex);
        
        // Verify all original content preserved
        assertTrue(modifiedHtml.contains("Welcome to dotCMS"));
        assertTrue(modifiedHtml.contains("&copy; 2026 dotCMS"));
        assertTrue(modifiedHtml.contains("<script src=\"/app.js\">"));
    }
}
