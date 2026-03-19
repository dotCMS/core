package com.dotcms.adminsite;


import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Config;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public interface AdminSiteAPI {


    /**
     * ------------------------------------------------------------------------ CONFIG VARS
     * ------------------------------------------------------------------------
     */

    // turns Admin site functionality on or off (true|false). Defaults to false
    String ADMIN_SITE_ENABLED = "ADMIN_SITE_ENABLED";


    // config the primary admin url that will be used to administrate your dotCMS instance.
    // it is protocol :// host (:optional port), e.g. https://admin.dotcms.com
    String ADMIN_SITE_URL = "ADMIN_SITE_URL";

    // config to force all admin requests to use SSL (true|false), defaults to true (allow insecure)
    String ADMIN_SITE_REQUESTS_FORCE_SECURE = "ADMIN_SITE_REQUESTS_FORCE_SECURE";


    String ADMIN_SITE_ALLOW_BACKEND_LOGINS = "ADMIN_SITE_ALLOW_BACKEND_LOGINS";


    // comma separated list of headers to add for admin domains.  In a `key1,value1,key2,value2` format.
    String ADMIN_SITE_REQUEST_HEADERS = "ADMIN_SITE_REQUEST_HEADERS";

    // comma separated list of  admin URIs to that should be blocked on non-admin sites
    String ADMIN_SITE_REQUEST_URIS = "ADMIN_SITE_REQUEST_URIS";

    // comma separated list of admin URIs to exclude from being blocked (can disable the defaults)
    String ADMIN_SITE_REQUEST_URIS_EXCLUDE = "ADMIN_SITE_REQUEST_URIS_EXCLUDE";

    /**
     * comma separated list of admin domains - automatically wildcarded in the front for matches for the end of the
     * string, e.g. `mysite.com` would match admin.mysite.com and www.mysite.com
     */
    String ADMIN_SITE_REQUEST_DOMAINS = "ADMIN_SITE_REQUEST_DOMAINS";

    // comma separated list of domains that should be excluded as admin domains (can disable the defaults)
    String ADMIN_SITE_REQUEST_DOMAINS_EXCLUDE = "ADMIN_SITE_REQUEST_DOMAINS_EXCLUDE";


    /**
     * ------------------------------------------------------------------------
     * DEFAULTS (Not configurable)
     * ------------------------------------------------------------------------
     */
    // the default admin url if one is not set
    String _ADMIN_SITE_URL_DEFAULT = "https://local.dotcms.site:8443";

    // default header added to all admin requests
    String[] _ADMIN_SITE_REQUEST_HEADERS_DEFAULT = new String[]{"x-robots-tag", "noindex, nofollow"};

    // request attribute added for any ADMIN_SITE request
    String _ADMIN_SITE_HOST_REQUESTED = "_ADMIN_SITE_HOST_REQUESTED";

    // key to store the admin site config in the dotCMS system cache.
    String _ADMIN_SITE_CACHE_KEY = "_ADMIN_SITE_CACHE_KEY";

    /**
     * Default list of "admin" paths.  These can be added to or removed by setting the ADMIN_SITE_REQUEST_URIS (to add)
     * and/or  ADMIN_SITE_REQUEST_URIS_EXCLUDE to remove. These are automatically wildcarded at the end, .e.g. /html/*
     */
    String[] _ADMIN_SITE_REQUEST_URIS_DEFAULT = {
            "/html/",
            "/admin/",
            "/dwr/",
            "/c/",
            "/servlets/",
            "/categoriesservlet/",
            "/dwr/",
            "/dotajaxdirector",
            "/dotscheduledjobs",
            "/dotadmin/",
            "/api/v1/appconfiguration",
            "/jsontags/",
            "/edit/",
            "/servlet/"};

    /**
     * Default list of admin Domains.  These can be added or removed by using the ADMIN_SITE_REQUEST_DOMAINS and
     * ADMIN_SITE_REQUEST_DOMAINS_EXCLUDE variables automatically wildcarded in front, e.g. *dotcms.com matches any
     * dotcms.com subdomain.
     */
    String[] _ADMIN_SITE_REQUEST_DOMAINS_DEFAULT = {
            "dotcms.com",
            "dotcms.site",
            "dotcms.io",
            "dotcms.host",
            "dotcms.cloud",
            "dotcmscloud.com",
            "localhost",
            "127.0.0.1"
    };




    /**
     * checks the host value to validate that it is on the "admin site" host list
     *
     * @param host
     * @return
     */
    boolean isAdminSite(String host);


    /**
     * checks the request to validate that it is on the "admin site" host list
     *
     * @param request
     * @return
     */
    boolean isAdminSite(HttpServletRequest request);


    /**
     * checks the uri to validate if it should be considered a "backend" or admin request
     *
     * @param uri
     * @return
     */
    boolean isAdminSiteUri(String uri);

    /**
     * checks the uri to validate if it should be considered a "backend" or admin request
     *
     * @param request
     * @return
     */
    boolean isAdminSiteUri(HttpServletRequest request);


    /**
     * whether backend users can call the /api/v1/authentication api
     * on non-admin sites
     * @return
     */
    default boolean allowBackendLoginsOnNonAdminSites() {
        return Config.getBooleanProperty(ADMIN_SITE_ALLOW_BACKEND_LOGINS, false);
    }

    /**
     * returns a set of headers to be included on any site that has been marked as an Admin site.
     * The default set is "x-robots-tag: noindex, nofollow" to prevent dotcms.cloud domains from being
     * indexed
     * @return
     */
    Map<String,String> getAdminSiteHeaders();

    /**
     * returns if insecure requests are allowed to the dotCMS admin functionality
     * @return
     */
    default boolean allowInsecureRequests() {
        return Config.getBooleanProperty(ADMIN_SITE_REQUESTS_FORCE_SECURE,
                true);
    }

    /**
     * returns if the ADMIN_SITE_URL has been configured
     * @return
     */
    default boolean isAdminSiteConfigured() {
        return Config.getStringProperty(ADMIN_SITE_URL, null) != null;
    }

    /**
     * returns a cleaned up version of the admin site url
     * @return
     */
    String getAdminSiteUrl();

    /**
     * clears the admin site cache and allows the config to reload
     */
    default void invalidateCache() {
        CacheLocator.getSystemCache().remove(_ADMIN_SITE_CACHE_KEY);
    }


    /**
     * Returns if the admin site functionality has been enabled.
     * @return
     */

    default boolean isAdminSiteEnabled() {
        return Config.getBooleanProperty(ADMIN_SITE_ENABLED, false);
    }

}
