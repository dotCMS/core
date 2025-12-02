package com.dotcms.adminsite;


import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public interface AdminSiteAPI {

    String ADMIN_SITE_ENABLED = "ADMIN_SITE_ENABLED";
    String ADMIN_SITE_CACHE_KEY = "ADMIN_SITE_CACHE_KEY";
    String ADMIN_SITE_URL = "ADMIN_SITE_URL";
    String ADMIN_SITE_URL_DEFAULT = "https://local.dotcms.site:8443";
    String ADMIN_SITE_REQUESTS_ALLOW_INSECURE = "ADMIN_SITE_REQUESTS_ALLOW_INSECURE";
    String ADMIN_SITE_REQUEST_URIS = "ADMIN_SITE_REQUEST_URIS";
    String ADMIN_SITE_REQUEST_URIS_EXCLUDE = "ADMIN_SITE_REQUEST_URIS_EXCLUDE";
    String ADMIN_SITE_REQUEST_DOMAINS_EXCLUDE = "ADMIN_SITE_REQUEST_DOMAINS_EXCLUDE";
    String ADMIN_SITE_REQUEST_DOMAINS = "ADMIN_SITE_REQUEST_DOMAINS";
    String ADMIN_SITE_REQUEST_HEADERS = "ADMIN_SITE_REQUEST_HEADERS";
    String[] ADMIN_SITE_REQUEST_HEADERS_DEFAULT = new String[]{"x-robots-tag", "noindex, nofollow"};
    String ADMIN_SITE_REQUEST_VALIDATED = "ADMIN_SITE_REQUEST_VALIDATED";


    // Default list of management paths
    String[] ADMIN_SITE_REQUEST_URIS_DEFAULT = {
            "/html/",
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


    // Default list of management Domains
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
