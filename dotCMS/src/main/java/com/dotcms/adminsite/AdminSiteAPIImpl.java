package com.dotcms.adminsite;


import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
                .get(ADMIN_SITE_CACHE_KEY);
        if (config == null) {
            synchronized (AdminSiteAPIImpl.class) {
                config = (ConcurrentHashMap<String, Object>) CacheLocator.getSystemCache().get(ADMIN_SITE_CACHE_KEY);
                if (config == null) {
                    config = new ConcurrentHashMap<>();
                    CacheLocator.getSystemCache().put(ADMIN_SITE_CACHE_KEY, config);
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
                ADMIN_SITE_REQUEST_HEADERS_DEFAULT);
        if (tmpHeaders == null || tmpHeaders.length == 0) {
            tmpHeaders = new String[0];
        }

        Map<String, String> headers = new HashMap<>(tmpHeaders.length);
        for (int i = 0; i < tmpHeaders.length; i++) {
            headers.put(tmpHeaders[i], tmpHeaders[++i]);
        }
        return Collections.unmodifiableMap(headers);

    }

    ;

    @Override
    public boolean isAdminSiteUri(HttpServletRequest request) {

        String uri = request.getRequestURI().toLowerCase();

        return isAdminSiteUri(uri);
    }

    @Override
    public boolean isAdminSiteUri(String uri) {
        for (String test : getAdminUris()) {
            if (uri.startsWith(test)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAdminSite(HttpServletRequest request) {

        if (request != null && request.getAttribute(ADMIN_SITE_REQUEST_HEADERS) != null) {
            return true;
        }

        String host =
                request.getHeader("host") != null ? request.getHeader("host").toLowerCase() : "local.dotcms.site";

        host = host.indexOf(":") > -1 ? host.substring(0, host.indexOf(":")) : host;
        if (isAdminSite(host)) {
            request.setAttribute(ADMIN_SITE_REQUEST_VALIDATED, true);
            return true;
        }

        return false;
    }

    @Override
    public boolean isAdminSite(String site) {

        for (String test : getAdminDomains()) {
            if (site.endsWith(test)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isAdminAllowed(HttpServletRequest request) {
        return false;
    }


    @Override
    public boolean isAdminSiteEnabled() {
        return (boolean) getConfig().computeIfAbsent(ADMIN_SITE_ENABLED,
                k -> Config.getBooleanProperty(ADMIN_SITE_ENABLED, true));
    }



    @Override
    public Map<String, String> getAdminSiteHeaders() {
        return (Map<String, String>) getConfig().computeIfAbsent(ADMIN_SITE_REQUEST_HEADERS, k -> _requestHeaders());

    }


    @Override
    public boolean allowInsecureRequests() {
        return Config.getBooleanProperty(ADMIN_SITE_REQUESTS_ALLOW_INSECURE, false);
    }

    @Override
    public boolean isAdminSiteConfigured() {
        return Config.getStringProperty(ADMIN_SITE_URL, null) != null;
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
        String adminSiteUrl = Config.getStringProperty(ADMIN_SITE_URL, ADMIN_SITE_URL_DEFAULT);

        while (adminSiteUrl.endsWith("/")) {
            adminSiteUrl = adminSiteUrl.substring(0, adminSiteUrl.length() - 1);
        }

        if (!adminSiteUrl.startsWith("http://") && !adminSiteUrl.startsWith("https://")) {
            Logger.warn(AdminSiteAPI.class, "ADMIN_SITE_URL: '" + adminSiteUrl
                    + "' is not a valid return URL. Please add the protocol, domain and optionally the port to access the dotCMS instance, e.g. DOT_ADMIN_SITE_URL=https://www.yoursite.com or DOT_ADMIN_SITE_URL=https://www.yoursite.com:8443");
            adminSiteUrl = "https://" + adminSiteUrl;
        }

        if (adminSiteUrl.lastIndexOf("/") > 10) {
            Logger.warn(AdminSiteAPI.class,
                    "ADMIN_SITE_URL should not include a uri, e.g it should be set to https://www.yoursite.com, not https://www.yoursite.com/dotAdmin. Please remove any path or uri after the domain/port");
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

    ;

    String[] _adminUris() {
        Set<String> allowedUrls = new HashSet<>(Arrays.asList(AdminSiteAPI.ADMIN_SITE_REQUEST_URIS_DEFAULT));

        allowedUrls.addAll(
                Arrays.asList(Config.getStringArrayProperty(AdminSiteAPI.ADMIN_SITE_REQUEST_URIS, new String[0])));

        Arrays.asList(Config.getStringArrayProperty(AdminSiteAPI.ADMIN_SITE_REQUEST_URIS_EXCLUDE,
                new String[0])).forEach(
                allowedUrls::remove);

        return allowedUrls.toArray(new String[0]);

    }


    public String[] getAdminDomains() {

        final String adminSiteUrl = getAdminSiteUrl();
        return (String[]) getConfig().computeIfAbsent(ADMIN_SITE_REQUEST_DOMAINS, k -> _adminDomains(adminSiteUrl));
    }

    ;


    String[] _adminDomains(String adminSiteUrl) {

        // Get portal URL host first so it can be placed at the beginning
        final URL portalUrl = Try.of(() -> new URL(adminSiteUrl)).getOrNull();
        // Build set with portal host first (if available), then defaults, then configured
        LinkedHashSet<String> allowedHosts = new LinkedHashSet<>();
        if (portalUrl != null) {
            allowedHosts.add(portalUrl.getHost().toLowerCase());
        }
        allowedHosts.addAll(Arrays.asList(AdminSiteAPI.ADMIN_SITE_REQUEST_DOMAINS_DEFAULT));
        allowedHosts.addAll(
                Arrays.asList(Config.getStringArrayProperty(AdminSiteAPI.ADMIN_SITE_REQUEST_DOMAINS,
                        new String[0])));

        // Remove excluded domains
        Arrays.asList(Config.getStringArrayProperty(AdminSiteAPI.ADMIN_SITE_REQUEST_DOMAINS_EXCLUDE,
                new String[0])).forEach(allowedHosts::remove);

        return allowedHosts.toArray(new String[0]);
    }

    ;


    @Override
    public void invalidateCache() {
        CacheLocator.getSystemCache().remove(ADMIN_SITE_CACHE_KEY);
    }


}
