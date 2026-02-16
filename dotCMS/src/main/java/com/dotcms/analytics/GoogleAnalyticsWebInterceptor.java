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
 * Web Interceptor that automatically injects Google Analytics 4 (GA4) tracking code into HTML pages
 * when the googleAnalytics field is populated on a site.
 * 
 * Supports Google Analytics 4 (GA4) format: G-XXXXXXXXXX
 * 
 * Configuration:
 * - GOOGLE_ANALYTICS_AUTO_INJECT: Enable/disable auto-injection (default: true)
 * 
 * The tracking code is injected before the closing </body> tag for optimal performance.
 * Injection is skipped in EDIT_MODE and PREVIEW_MODE to avoid tracking during content editing.
 * 
 * Note: Universal Analytics (UA) was sunset by Google in July 2023. Only GA4 is supported.
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
        
        // Store tracking ID in request attribute for use in afterIntercept
        request.setAttribute("GA_TRACKING_ID", gaTrackingId);
        
        // Wrap response to capture and modify HTML output
        final GAResponseWrapper wrappedResponse = new GAResponseWrapper(response, gaTrackingId);
        
        return Result.wrap(request, wrappedResponse);
    }
    
    @Override
    public boolean afterIntercept(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            // Check if we have a wrapped response with content to inject
            if (response instanceof GAResponseWrapper) {
                final GAResponseWrapper wrappedResponse = (GAResponseWrapper) response;
                wrappedResponse.finishResponse();
            }
        } catch (Exception e) {
            Logger.error(this, "Error finalizing Google Analytics injection: " + e.getMessage(), e);
        }
        return true;
    }
    
    /**
     * Generates Google Analytics 4 (GA4) tracking script
     * 
     * @param trackingId The GA4 tracking ID (format: G-XXXXXXXXXX)
     * @return The formatted GA4 tracking script HTML
     */
    @VisibleForTesting
    static String generateTrackingScript(final String trackingId) {
        return String.format(GA4_SCRIPT_TEMPLATE, trackingId, trackingId);
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
        
        /**
         * Finalizes the response by injecting GA tracking code and writing to the actual response.
         * Should be called from afterIntercept().
         */
        public void finishResponse() throws IOException {
            if (!isHtmlResponse || outputStream == null) {
                return;
            }
            
            // Flush any pending writes
            if (writer != null) {
                writer.flush();
            }
            
            // Get the captured HTML content
            final String originalHtml = outputStream.toString(StandardCharsets.UTF_8.name());
            
            // Inject tracking code if HTML contains </body> tag
            final String modifiedHtml = injectTrackingCode(originalHtml, trackingId);
            
            // Write the modified HTML to the actual response
            try {
                final ServletOutputStream realOutputStream = ((HttpServletResponse) getResponse()).getOutputStream();
                realOutputStream.write(modifiedHtml.getBytes(StandardCharsets.UTF_8));
                realOutputStream.flush();
            } catch (IOException e) {
                Logger.error(GoogleAnalyticsWebInterceptor.class, 
                           "Failed to write Google Analytics injected content: " + e.getMessage(), e);
                throw e;
            }
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
