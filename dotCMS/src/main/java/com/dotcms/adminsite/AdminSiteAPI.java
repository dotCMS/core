package com.dotcms.adminsite;


import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public interface AdminSiteAPI {

    // turns Admin site functionality on or off
    String ADMIN_SITE_ENABLED = "ADMIN_SITE_ENABLED";

    // key to store the admin site config in the dotCMS system cache.
    String ADMIN_SITE_CACHE_KEY = "ADMIN_SITE_CACHE_KEY";

    // the primary admin url that will be used to administrate your dotCMS instance.
    String ADMIN_SITE_URL = "ADMIN_SITE_URL";

    // the default admin url if one is not set
    String ADMIN_SITE_URL_DEFAULT = "https://local.dotcms.site:8443";

    // config to for all admin requests to use SSL
    String ADMIN_SITE_REQUESTS_ALLOW_INSECURE = "ADMIN_SITE_REQUESTS_ALLOW_INSECURE";

    // comma separated list of URIs to that should be blocked on non-admin sites
    String ADMIN_SITE_REQUEST_URIS = "ADMIN_SITE_REQUEST_URIS";

    // comma separated list of URIs to exclude from being blocked (can disable the defaults)
    String ADMIN_SITE_REQUEST_URIS_EXCLUDE = "ADMIN_SITE_REQUEST_URIS_EXCLUDE";

    // comma separated list of domains that should be excluded as admin domains (can disable the defaults)
    String ADMIN_SITE_REQUEST_DOMAINS_EXCLUDE = "ADMIN_SITE_REQUEST_DOMAINS_EXCLUDE";

    /**
     * comma separated list of admin domains - automatically wildcarded in the front for matches for the end of the
     * string, e.g. `mysite.com` would match admin.mysite.com and www.mysite.com
     */
    String ADMIN_SITE_REQUEST_DOMAINS = "ADMIN_SITE_REQUEST_DOMAINS";


    // comma separated list of headers to add for admin domains.  In a `key1,value1,key2,value2` format.
    String ADMIN_SITE_REQUEST_HEADERS = "ADMIN_SITE_REQUEST_HEADERS";

    // default header added to all admin requests
    String[] ADMIN_SITE_REQUEST_HEADERS_DEFAULT = new String[]{"x-robots-tag", "noindex, nofollow"};

    String ADMIN_SITE_HOST_REQUESTED = "ADMIN_SITE_HOST_REQUESTED";


    // Default list of "admin" paths
    String[] ADMIN_SITE_REQUEST_URIS_DEFAULT = {
            "/html/",
            "/admin/",
            "/c/",
            "/servlets/",
            "/categoriesservlet/",
            "/dwr/",
            "/dotajaxdirector",
            "/dotscheduledjobs",
            "/dotadmin/",
            "/jsontags/",
            "/edit/",
            "/servlet/"};


    // Default list of "admin" Domains.  These can be added or removed by using the
    // ADMIN_SITE_REQUEST_DOMAINS and ADMIN_SITE_REQUEST_DOMAINS_EXCLUDE variables
    // automatically wildcarded in front, e.g. *dotcms.com matches any dotcms.com subdomain.
    String[] ADMIN_SITE_REQUEST_DOMAINS_DEFAULT = {
            "dotcms.com",
            "dotcms.site",
            "dotcms.io",
            "dotcms.host",
            "dotcms.cloud",
            "dotcmscloud.com",
            "localhost"};

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
     * checks
     *
     * @param request
     * @return
     */
    boolean isAdminAllowed(HttpServletRequest request);


    /**
     * returns a set of headers to be included on any site that has been marked as an Admin site.
     * The default set is "x-robots-tag: noindex, nofollow" to prevent dotcms.cloud domains from being
     * indexed
     * @return
     */
    Map<String,String> getAdminSiteHeaders();

    boolean allowInsecureRequests();

    boolean isAdminSiteConfigured();

    String getAdminSiteUrl();

    void invalidateCache();

    boolean isAdminSiteEnabled();
}
