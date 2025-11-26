package com.dotcms.adminsite;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AdminSiteConfig {


    private AdminSiteConfig(){
        // use this class statically
    }


    final static String ADMIN_SITE_REQUESTS_ALLOW_INSECURE="ADMIN_SITE_REQUESTS_ALLOW_INSECURE";
    final static String ADMIN_SITE_REQUEST_URIS="ADMIN_SITE_REQUEST_URIS";
    final static String ADMIN_SITE_REQUEST_URIS_EXCLUDE="ADMIN_SITE_REQUEST_URIS_EXCLUDE";
    final static String ADMIN_SITE_REQUEST_DOMAINS_EXCLUDE="ADMIN_SITE_REQUEST_DOMAINS_EXCLUDE";
    final static String ADMIN_SITE_REQUEST_DOMAINS="ADMIN_SITE_REQUEST_DOMAINS";
    final static String ADMIN_SITE_REQUEST_HEADERS="ADMIN_SITE_REQUEST_HEADERS";
    final static String[] ADMIN_SITE_REQUEST_HEADERS_DEFAULT=new String[]{"x-robots-tag", "noindex, nofollow"};

    public static final String ADMIN_SITE_REQUEST_VALIDATED = "ADMIN_SITE_REQUEST_VALIDATED";


    // Default list of management paths
    static final String[] ADMIN_SITE_REQUEST_URIS_DEFAULT = {
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
    static final String[] ADMIN_SITE_REQUEST_DOMAINS_DEFAULT = {
            "dotcms.com",
            "dotcms.site",
            "dotcms.io",
            "dotcms.host",
            "dotcms.cloud",
            "dotcmscloud.com",
            "localhost"};



    final static Lazy<String[]> adminUrls = Lazy.of(() -> {
        Set<String> allowedUrls = new HashSet<>(Arrays.asList(ADMIN_SITE_REQUEST_URIS_DEFAULT));

        allowedUrls.addAll(Arrays.asList(Config.getStringArrayProperty(ADMIN_SITE_REQUEST_URIS, new String[0])));

        allowedUrls.removeAll(
                Arrays.asList(Config.getStringArrayProperty(ADMIN_SITE_REQUEST_URIS_EXCLUDE,
                        new String[0])));

        return allowedUrls.toArray(new String[0]);
    });


    final static Lazy<String[]> adminDomains = Lazy.of(() -> {
        Set<String> allowedHosts = new HashSet<>(Arrays.asList(AdminSiteConfig.ADMIN_SITE_REQUEST_DOMAINS_DEFAULT));
        allowedHosts.addAll(
                Arrays.asList(Config.getStringArrayProperty(ADMIN_SITE_REQUEST_DOMAINS,
                        new String[0])));

        allowedHosts.removeAll(
                Arrays.asList(Config.getStringArrayProperty(ADMIN_SITE_REQUEST_DOMAINS_EXCLUDE,
                        new String[0])));

        final String portalUrlString = APILocator.getCompanyAPI().getDefaultCompany().getPortalURL().contains("://")
                ? APILocator.getCompanyAPI().getDefaultCompany().getPortalURL()
                : "https://" + APILocator.getCompanyAPI().getDefaultCompany().getPortalURL();

        URL portalUrl = Try.of(() -> new URL(portalUrlString)).getOrElseThrow(DotRuntimeException::new);

        allowedHosts.add(portalUrl.getHost().toLowerCase());

        return allowedHosts.toArray(new String[allowedHosts.size()]);
    });




}
