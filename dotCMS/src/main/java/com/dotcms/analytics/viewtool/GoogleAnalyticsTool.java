package com.dotcms.analytics.viewtool;

import com.dotcms.rest.api.v1.site.SiteResource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;

/**
 * ViewTool for generating Google Analytics 4 (GA4) tracking code in Velocity templates.
 * 
 * This tool provides a simple way to include GA4 tracking code in templates by reading
 * the tracking ID from the current site's googleAnalytics field.
 * 
 * Usage in Velocity templates:
 * <pre>
 * ## Include GA4 tracking code
 * $googleAnalytics.trackingCode
 * 
 * ## Or with null check
 * #if($googleAnalytics.trackingId)
 *   $googleAnalytics.trackingCode
 * #end
 * </pre>
 * 
 * Benefits of manual inclusion:
 * - Developers have full control over placement in the template
 * - Can be conditionally included based on user consent
 * - No automatic HTML parsing/modification overhead
 * - More transparent and easier to debug
 * 
 * @author dotCMS
 */
public class GoogleAnalyticsTool implements ViewTool {

    private HttpServletRequest request;
    private final HostWebAPI hostWebAPI;

    /**
     * Default constructor - uses WebAPILocator
     */
    public GoogleAnalyticsTool() {
        this(WebAPILocator.getHostWebAPI());
    }

    /**
     * Constructor for testing/dependency injection
     * 
     * @param hostWebAPI the HostWebAPI to use
     */
    public GoogleAnalyticsTool(final HostWebAPI hostWebAPI) {
        this.hostWebAPI = hostWebAPI;
    }

    @Override
    public void init(final Object initData) {
        if (initData instanceof ViewContext) {
            this.request = ((ViewContext) initData).getRequest();
        }
    }

    /**
     * Gets the Google Analytics tracking ID from the current site.
     * 
     * @return The GA4 tracking ID (e.g., "G-XXXXXXXXXX") or null if not set
     */
    public String getTrackingId() {
        try {
            final Host site = hostWebAPI.getCurrentHostNoThrow(request);
            if (site != null) {
                final String trackingId = site.getStringProperty(SiteResource.GOOGLE_ANALYTICS);
                if (UtilMethods.isSet(trackingId)) {
                    return trackingId;
                }
            }
        } catch (Exception e) {
            Logger.error(this, "Error retrieving Google Analytics tracking ID", e);
        }
        return null;
    }

    /**
     * Generates the complete Google Analytics 4 tracking code.
     * 
     * This includes the gtag.js script tag and initialization code.
     * Place this code in your template where you want the tracking code to appear
     * (typically before the closing &lt;/body&gt; tag).
     * 
     * @return The complete GA4 tracking code HTML, or empty string if no tracking ID is set
     */
    public String getTrackingCode() {
        final String trackingId = getTrackingId();
        
        if (!UtilMethods.isSet(trackingId)) {
            Logger.debug(this, "No Google Analytics tracking ID found for current site");
            return "";
        }

        return generateGA4Script(trackingId);
    }

    /**
     * Generates the GA4 tracking script with the given tracking ID.
     * 
     * @param trackingId the GA4 tracking ID
     * @return the formatted GA4 tracking script
     */
    private String generateGA4Script(final String trackingId) {
        return String.format(
            "<!-- Google tag (gtag.js) -->\n" +
            "<script async src=\"https://www.googletagmanager.com/gtag/js?id=%s\"></script>\n" +
            "<script>\n" +
            "  window.dataLayer = window.dataLayer || [];\n" +
            "  function gtag(){dataLayer.push(arguments);}\n" +
            "  gtag('js', new Date());\n" +
            "  gtag('config', '%s');\n" +
            "</script>",
            trackingId, trackingId
        );
    }
}
