package com.dotcms.adminsite;


import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;

@ApplicationScoped
public class AdminSiteAPIImpl implements AdminSiteAPI {


    /***
     * Holds the calculated configuration for the AdminSite functionality.  It is stored in cache so that
     * when a value changes in the system_table, it can be reloaded across the cluster.
     * @return
     */
    private ConcurrentHashMap<String, Object> getConfig() {
        ConcurrentHashMap<String, Object> config = (ConcurrentHashMap<String, Object>) CacheLocator.getSystemCache()
                .get(_ADMIN_SITE_CACHE_KEY);
        if (config == null) {
            synchronized (AdminSiteAPIImpl.class) {
                config = (ConcurrentHashMap<String, Object>) CacheLocator.getSystemCache().get(_ADMIN_SITE_CACHE_KEY);
                if (config == null) {
                    config = new ConcurrentHashMap<>();
                    CacheLocator.getSystemCache().put(_ADMIN_SITE_CACHE_KEY, config);
                }
            }
        }
        return config;
    }


    public AdminSiteAPIImpl() {
    }


    @PostConstruct
    public void init() {
        // listens to config changes and flushes cache when needed
        APILocator.getLocalSystemEventsAPI().subscribe(SystemTableUpdatedKeyEvent.class, new AdminSiteKeyListener());
    }


    private Map<String, String> _requestHeaders() {
        String[] tmpHeaders = Config.getStringArrayProperty(ADMIN_SITE_REQUEST_HEADERS,
                _ADMIN_SITE_REQUEST_HEADERS_DEFAULT);
        if (tmpHeaders == null || tmpHeaders.length == 0) {
            tmpHeaders = new String[0];
        }

        Map<String, String> headers = new HashMap<>(tmpHeaders.length);
        for (int i = 0; i < tmpHeaders.length; i++) {
            headers.put(tmpHeaders[i], tmpHeaders[++i]);
        }
        return Collections.unmodifiableMap(headers);
    }

    @Override
    public boolean isAdminSiteUri(@Nonnull HttpServletRequest request) {

        String uri = request.getRequestURI().toLowerCase();

        return isAdminSiteUri(uri);
    }

    @Override
    public boolean isAdminSiteUri(@Nonnull String uri) {
        final String lowerUri = uri.toLowerCase();
        for (String test : getAdminUris()) {
            if (lowerUri.startsWith(test)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean isAdminSite(@Nonnull HttpServletRequest request) {
        if (request == null) {
            return true;
        }

        if (request.getAttribute(_ADMIN_SITE_HOST_REQUESTED) != null) {
            return (boolean) request.getAttribute(_ADMIN_SITE_HOST_REQUESTED);
        }

        // if the admin site functionality is not enabled,
        // anything can go
        if (!isAdminSiteEnabled()) {
            request.setAttribute(_ADMIN_SITE_HOST_REQUESTED, true);
            return true;
        }

        String host = request.getHeader("host") != null
                ? request.getHeader("host").toLowerCase()
                : "local.dotcms.site";

        host = host.contains(":") ? host.substring(0, host.indexOf(":")) : host;
        if (isAdminSite(host)) {
            request.setAttribute(_ADMIN_SITE_HOST_REQUESTED, true);
            return true;
        }
        request.setAttribute(_ADMIN_SITE_HOST_REQUESTED, false);
        return false;
    }

    @Override
    public boolean isAdminSite(@Nonnull String site) {
        final String lowerSite = site.toLowerCase();
        for (String test : getAdminDomains()) {
            if (lowerSite.endsWith(test)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public Map<String, String> getAdminSiteHeaders() {
        return (Map<String, String>) getConfig().computeIfAbsent(ADMIN_SITE_REQUEST_HEADERS, k -> _requestHeaders());

    }



    @Override
    public String getAdminSiteUrl() {
        return (String) getConfig().computeIfAbsent(ADMIN_SITE_URL, k -> _baseAdminSiteDomain());
    }

    // Tracks the last logged URL to avoid duplicate log messages
    private volatile String lastLoggedAdminSiteUrl = null;
    private volatile boolean notConfiguredWarningLogged = false;

    /**
     * calculates the admin site url based on config properties.
     *
     * @return
     */
    String _baseAdminSiteDomain() {
        if (!isAdminSiteConfigured()) {
            if (!notConfiguredWarningLogged) {
                Logger.warn(AdminSiteAPI.class,
                        "ADMIN_SITE_URL is not configured.  This is the url that is used to access dotCMS. Please add it to your system's environmental variables, e.g. DOT_ADMIN_SITE_URL=https://www.siteadmin.com or DOT_ADMIN_SITE_URL=https://www.siteadmin.com:8443");
                notConfiguredWarningLogged = true;
            }
        } else {
            // Reset the warning flag if it becomes configured
            notConfiguredWarningLogged = false;
        }
        HttpServletRequest req = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        String oldHost = req != null && UtilMethods.isSet(req.getHeader("host"))
                ? req.getHeader("host").toLowerCase()
                : APILocator.getCompanyAPI().getDefaultCompany().getOldPortalURL();

        String adminSiteUrl = Config.getStringProperty(ADMIN_SITE_URL, oldHost);

        while (adminSiteUrl.endsWith("/")) {
            adminSiteUrl = adminSiteUrl.substring(0, adminSiteUrl.length() - 1);
        }

        if (!adminSiteUrl.startsWith("http://") && !adminSiteUrl.startsWith("https://")) {
            Logger.info(AdminSiteAPI.class, "ADMIN_SITE_URL: '" + adminSiteUrl
                    + "' is not a valid return URL - adding https:// to the ADMIN_SITE_URL.  This should be part of the configuration, e.g. DOT_ADMIN_SITE_URL=https://www.yoursite.com or DOT_ADMIN_SITE_URL=https://www.yoursite.com:8443");
            adminSiteUrl = "https://" + adminSiteUrl;
        }

        if (adminSiteUrl.lastIndexOf("/") > 10) {
            Logger.info(AdminSiteAPI.class,
                    "ADMIN_SITE_URL should not include a path, e.g it should be set to https://www.yoursite.com, not https://www.yoursite.com/dotAdmin. Removing the path or uri after the domain/port");
            while (adminSiteUrl.lastIndexOf("/") > 9) {
                adminSiteUrl = adminSiteUrl.substring(0, adminSiteUrl.lastIndexOf("/") - 1);
            }
        }

        // Only log when the URL actually changes
        if (!adminSiteUrl.equals(lastLoggedAdminSiteUrl)) {
            Logger.info(AdminSiteAPI.class, "*********************");
            Logger.info(AdminSiteAPI.class, "* Setting ADMIN_SITE_URL to " + adminSiteUrl);
            Logger.info(AdminSiteAPI.class,
                    "* - this url will be used to build internal links back to your dotCMS administrative instance.");
            Logger.info(AdminSiteAPI.class, "*********************");
            lastLoggedAdminSiteUrl = adminSiteUrl;
        }
        return lastLoggedAdminSiteUrl;

    }

    public String[] getAdminUris() {
        return (String[]) getConfig().computeIfAbsent(ADMIN_SITE_REQUEST_URIS, k -> _adminUris());
    }

    String[] _adminUris() {
        Set<String> allowedUrls = new HashSet<>();

        // Add defaults (lowercased)
        for (String uri : AdminSiteAPI._ADMIN_SITE_REQUEST_URIS_DEFAULT) {
            allowedUrls.add(uri.toLowerCase());
        }

        // Add configured URIs (lowercased)
        for (String uri : Config.getStringArrayProperty(AdminSiteAPI.ADMIN_SITE_REQUEST_URIS, new String[0])) {
            allowedUrls.add(uri.toLowerCase());
        }

        // Remove excluded URIs (lowercased for matching)
        for (String uri : Config.getStringArrayProperty(AdminSiteAPI.ADMIN_SITE_REQUEST_URIS_EXCLUDE, new String[0])) {
            allowedUrls.remove(uri.toLowerCase());
        }

        return allowedUrls.toArray(new String[0]);
    }


    public String[] getAdminDomains() {
        final String adminSiteUrl = getAdminSiteUrl();
        return (String[]) getConfig().computeIfAbsent(ADMIN_SITE_REQUEST_DOMAINS, k -> _adminDomains(adminSiteUrl));
    }


    String[] _adminDomains(@Nonnull String adminSiteUrl) {

        // Get portal URL host first so it can be placed at the beginning
        final URL portalUrl = Try.of(() -> new URL(adminSiteUrl)).getOrNull();
        // Build set with portal host first (if available), then defaults, then configured
        LinkedHashSet<String> allowedHosts = new LinkedHashSet<>();
        if (portalUrl != null) {
            allowedHosts.add(portalUrl.getHost().toLowerCase());
        }

        // Add defaults (lowercased)
        for (String domain : AdminSiteAPI._ADMIN_SITE_REQUEST_DOMAINS_DEFAULT) {
            allowedHosts.add(domain.toLowerCase());
        }

        // Add configured domains (lowercased)
        for (String domain : Config.getStringArrayProperty(AdminSiteAPI.ADMIN_SITE_REQUEST_DOMAINS, new String[0])) {
            allowedHosts.add(domain.toLowerCase());
        }

        // Remove excluded domains (lowercased for matching)
        for (String domain : Config.getStringArrayProperty(AdminSiteAPI.ADMIN_SITE_REQUEST_DOMAINS_EXCLUDE,
                new String[0])) {
            allowedHosts.remove(domain.toLowerCase());
        }

        return allowedHosts.toArray(new String[0]);
    }


}
