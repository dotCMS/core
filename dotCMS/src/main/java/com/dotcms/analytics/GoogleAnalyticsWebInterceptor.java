package com.dotcms.analytics;

import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.rest.api.v1.site.SiteResource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Web Interceptor that automatically injects Google Analytics tracking code into HTML pages
 * when the googleAnalytics field is populated on a site.
 * 
 * Supports both:
 * - Google Analytics 4 (GA4) format: G-XXXXXXXXXX
 * - Universal Analytics (UA) format: UA-XXXXXXXXXX-X
 * 
 * Configuration:
 * - GOOGLE_ANALYTICS_AUTO_INJECT: Enable/disable auto-injection (default: true)
 * 
 * The tracking code is injected before the closing </body> tag for optimal performance.
 * Injection is skipped in EDIT_MODE and PREVIEW_MODE to avoid tracking during content editing.
 * 
 * @author dotCMS
 */
public class GoogleAnalyticsWebInterceptor implements WebInterceptor {

    private static final String CONFIG_AUTO_INJECT = "GOOGLE_ANALYTICS_AUTO_INJECT";
    private static final String CONTENT_TYPE_HTML = "text/html";
    private static final String BODY_CLOSE_TAG = "</body>";
    
    // GA4 tracking code template
    private static final String GA4_SCRIPT_TEMPLATE = 
        "<!-- Google tag (gtag.js) -->\n" +
        "<script async src=\"https://www.googletagmanager.com/gtag/js?id=%s\"></script>\n" +
        "<script>\n" +
        "  window.dataLayer = window.dataLayer || [];\n" +
        "  function gtag(){dataLayer.push(arguments);}\n" +
        "  gtag('js', new Date());\n" +
        "  gtag('config', '%s');\n" +
        "</script>\n";
    
    // Universal Analytics tracking code template
    private static final String UA_SCRIPT_TEMPLATE = 
        "<!-- Google Analytics -->\n" +
        "<script>\n" +
        "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){\n" +
        "(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),\n" +
        "m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)\n" +
        "})(window,document,'script','https://www.google-analytics.com/analytics.js','ga');\n" +
        "ga('create', '%s', 'auto');\n" +
        "ga('send', 'pageview');\n" +
        "</script>\n";

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) 
            throws IOException {
        
        // Check if auto-injection is enabled via configuration
        if (!Config.getBooleanProperty(CONFIG_AUTO_INJECT, true)) {
            Logger.debug(this, "Google Analytics auto-injection is disabled");
            return Result.NEXT;
        }
        
        // Skip injection in edit/preview modes
        final PageMode pageMode = PageMode.get(request);
        if (pageMode.isAdmin || pageMode == PageMode.EDIT_MODE || pageMode == PageMode.PREVIEW_MODE) {
            Logger.debug(this, () -> "Skipping GA injection in " + pageMode + " mode");
            return Result.NEXT;
        }
        
        // Get current site and check for GA tracking ID
        final Host site = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        if (site == null) {
            return Result.NEXT;
        }
        
        final String gaTrackingId = site.getStringProperty(SiteResource.GOOGLE_ANALYTICS);
        if (!UtilMethods.isSet(gaTrackingId)) {
            return Result.NEXT;
        }
        
        Logger.debug(this, () -> "Google Analytics tracking ID found for site '" + 
                    site.getHostname() + "': " + gaTrackingId);
        
        // Wrap response to capture and modify HTML output
        final GAResponseWrapper wrappedResponse = new GAResponseWrapper(response, gaTrackingId);
        
        return Result.wrap(request, wrappedResponse);
    }
    
    /**
     * Determines the appropriate tracking script based on the tracking ID format
     * 
     * @param trackingId The Google Analytics tracking ID
     * @return The formatted tracking script HTML
     */
    @VisibleForTesting
    static String generateTrackingScript(final String trackingId) {
        if (trackingId.startsWith("G-")) {
            // GA4 format
            return String.format(GA4_SCRIPT_TEMPLATE, trackingId, trackingId);
        } else if (trackingId.startsWith("UA-")) {
            // Universal Analytics format
            return String.format(UA_SCRIPT_TEMPLATE, trackingId);
        } else {
            // Default to GA4 format if format is unclear
            Logger.warn(GoogleAnalyticsWebInterceptor.class, 
                       "Unknown Google Analytics tracking ID format: " + trackingId + 
                       ". Using GA4 format.");
            return String.format(GA4_SCRIPT_TEMPLATE, trackingId, trackingId);
        }
    }
    
    /**
     * Response wrapper that captures HTML output and injects GA tracking code before </body>
     */
    private static class GAResponseWrapper extends HttpServletResponseWrapper {
        
        private final String trackingId;
        private ByteArrayOutputStream outputStream;
        private ServletOutputStream servletOutputStream;
        private PrintWriter writer;
        private boolean isHtmlResponse = false;
        
        public GAResponseWrapper(final HttpServletResponse response, final String trackingId) {
            super(response);
            this.trackingId = trackingId;
        }
        
        @Override
        public void setContentType(final String type) {
            super.setContentType(type);
            if (type != null && type.toLowerCase().contains(CONTENT_TYPE_HTML)) {
                this.isHtmlResponse = true;
                this.outputStream = new ByteArrayOutputStream();
            }
        }
        
        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (writer != null) {
                throw new IllegalStateException("getWriter() has already been called");
            }
            
            if (!isHtmlResponse) {
                return super.getOutputStream();
            }
            
            if (servletOutputStream == null) {
                servletOutputStream = new ServletOutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        outputStream.write(b);
                    }
                    
                    @Override
                    public boolean isReady() {
                        return true;
                    }
                    
                    @Override
                    public void setWriteListener(WriteListener writeListener) {
                        // Not implemented for this use case
                    }
                };
            }
            
            return servletOutputStream;
        }
        
        @Override
        public PrintWriter getWriter() throws IOException {
            if (servletOutputStream != null) {
                throw new IllegalStateException("getOutputStream() has already been called");
            }
            
            if (!isHtmlResponse) {
                return super.getWriter();
            }
            
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            }
            
            return writer;
        }
        
        @Override
        public void flushBuffer() throws IOException {
            if (isHtmlResponse && outputStream != null) {
                injectTrackingCodeAndWrite();
            }
            super.flushBuffer();
        }
        
        /**
         * Called when the response is being finalized.
         * Injects the GA tracking code before </body> and writes to the actual response.
         */
        private void injectTrackingCodeAndWrite() throws IOException {
            if (writer != null) {
                writer.flush();
            }
            
            final String originalHtml = outputStream.toString(StandardCharsets.UTF_8.name());
            final String modifiedHtml = injectTrackingCode(originalHtml, trackingId);
            
            // Write the modified HTML to the actual response
            final ServletOutputStream realOutputStream = getResponse().getOutputStream();
            realOutputStream.write(modifiedHtml.getBytes(StandardCharsets.UTF_8));
            realOutputStream.flush();
            
            // Clear the buffer to avoid double-writing
            outputStream.reset();
        }
        
        /**
         * Injects the Google Analytics tracking code before the closing </body> tag
         * 
         * @param html The original HTML content
         * @param trackingId The Google Analytics tracking ID
         * @return The modified HTML with tracking code injected
         */
        @VisibleForTesting
        static String injectTrackingCode(final String html, final String trackingId) {
            final int bodyCloseIndex = html.toLowerCase().lastIndexOf(BODY_CLOSE_TAG);
            
            if (bodyCloseIndex < 0) {
                Logger.debug(GoogleAnalyticsWebInterceptor.class, 
                           "No </body> tag found, skipping GA injection");
                return html;
            }
            
            final String trackingScript = generateTrackingScript(trackingId);
            
            return html.substring(0, bodyCloseIndex) + 
                   trackingScript + 
                   html.substring(bodyCloseIndex);
        }
    }
}
