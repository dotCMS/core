package com.dotcms.adminsite;


import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vavr.control.Try;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;

@ApplicationScoped
public class AdminSiteAPIImpl implements AdminSiteAPI {


    /***
     * Caffeine cache that holds the calculated configuration for the AdminSite functionality.
     * The Caffeine `cache.get(key, mappingFunction)` is used as a thread-safe computeIfAbsent, so
     * expensive config calculations are done lazily, once, no matter how many concurrent requests hit them.
     * When a value changes in the system_table, the {@link AdminSiteKeyListener} invalidates this cache
     * allowing the configuration to reload across the cluster.
     */
    private final transient Cache<String, Object> adminSiteConfig = Caffeine.newBuilder().build();

    /**
     * Thread-safe lazy accessor for the calculated AdminSite configuration values.
     */
    @SuppressWarnings("unchecked")
    private <T> T getConfigValue(final String key, final Function<String, T> supplier) {
        return (T) adminSiteConfig.get(key, k -> supplier.apply(k));
    }


    public AdminSiteAPIImpl() {
    }


    @PostConstruct
    public void init() {
        // listens to config changes and flushes cache when needed
        APILocator.getLocalSystemEventsAPI().subscribe(SystemTableUpdatedKeyEvent.class, new AdminSiteKeyListener());
    }

    @Override
    public void invalidateCache() {
        adminSiteConfig.invalidateAll();
        CacheLocator.getSystemCache().remove(_ADMIN_SITE_CACHE_KEY);
    }


    private Map<String, String> _requestHeaders() {
        String[] tmpHeaders = Config.getStringArrayProperty(ADMIN_SITE_REQUEST_HEADERS,
                _ADMIN_SITE_REQUEST_HEADERS_DEFAULT);
        if (tmpHeaders == null || tmpHeaders.length == 0) {
            tmpHeaders = new String[0];
        }

        Map<String, String> headers = new HashMap<>(tmpHeaders.length / 2);
        for (int i = 0; i + 1 < tmpHeaders.length; i += 2) {
            headers.put(tmpHeaders[i], tmpHeaders[i + 1]);
        }
        return Collections.unmodifiableMap(headers);
    }

    @Override
    public boolean isAdminSiteUri(final HttpServletRequest request) {
        // isAdminSiteUri(String) normalizes the uri, so no need to pre-normalize here
        return isAdminSiteUri(request.getRequestURI());
    }

    @Override
    public boolean isAdminSiteUri(final String uri) {
        final String normalizedUri = normalizeUri(uri);
        for (String test : getAdminUris()) {
            if (normalizedUri.startsWith(test)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normalizes a URI to prevent bypass attacks via URL encoding or path traversal. - Decodes URL-encoded characters
     * (e.g., %64 -> d), repeatedly until stable, so double-encoded values (e.g., %252e%252e) cannot slip through
     * - Normalizes path traversal sequences (e.g., /foo/../bar -> /bar) - Converts to lowercase for
     * case-insensitive matching
     *
     * @param uri the URI to normalize
     * @return the normalized, lowercase URI
     */
    String normalizeUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "";
        }

        // Decode URL-encoded characters, repeatedly until stable, to prevent
        // double-encoding bypasses (e.g. %252e%252e -> %2e%2e -> ..)
        String decoded = uri;
        for (int i = 0; i < 3; i++) {
            final String current = decoded;
            final String next = Try.of(() -> URLDecoder.decode(current, StandardCharsets.UTF_8))
                    .getOrElse(current);
            if (next.equals(current)) {
                break;
            }
            decoded = next;
        }

        // Normalize path traversal (e.g., /foo/../bar -> /bar, /foo/./bar -> /foo/bar).
        // The multi-arg URI constructor quotes characters that are illegal in a URI (e.g. '[' or ']')
        // instead of throwing, so a malformed-but-common input cannot skip the normalization.
        final String normalizedInput = decoded;
        String normalized = Try.of(() -> new URI(null, null, normalizedInput, null).normalize().getPath())
                .getOrElse(normalizedInput);

        // Ensure we have a valid result
        if (normalized == null || normalized.isEmpty()) {
            normalized = decoded;
        }

        return normalized.toLowerCase();
    }


    @Override
    public boolean isAdminSite(final HttpServletRequest request) {
        if (request == null) {
            // no request (like running in a threadpool) - fail open
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

        final String hostHeader = request.getHeader("host");
        if (!UtilMethods.isSet(hostHeader)) {
            // No Host header (HTTP/1.0 client or malformed request) - fail closed instead of
            // guessing that the request came from an admin domain.
            request.setAttribute(_ADMIN_SITE_HOST_REQUESTED, false);
            return false;
        }

        final String host = hostHeader.toLowerCase();

        if (isAdminSite(host)) {
            request.setAttribute(_ADMIN_SITE_HOST_REQUESTED, true);
            return true;
        }

        request.setAttribute(_ADMIN_SITE_HOST_REQUESTED, false);
        return false;
    }

    @Override
    public boolean isAdminSite(final String site) {
        if (!UtilMethods.isSet(site)) {
            return false;
        }
        final String host = stripPort(site.toLowerCase());

        // the host of the configured ADMIN_SITE_URL is always considered an admin domain
        final String adminUrlHost = Try.of(() -> new URL(getAdminSiteUrl()).getHost().toLowerCase()).getOrNull();
        if (UtilMethods.isSet(adminUrlHost) && matchesDomain(host, adminUrlHost)) {
            return true;
        }

        for (String test : getAdminDomains()) {
            if (matchesDomain(host, test)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Matches a host against an admin domain on exact match or dot-boundary subdomain match,
     * e.g. "admin.dotcms.com" and "dotcms.com" match "dotcms.com", but "mydotcms.com" does not.
     */
    private static boolean matchesDomain(final String host, final String domain) {
        return host.equals(domain) || host.endsWith("." + domain);
    }

    private static String stripPort(final String site) {
        return site.contains(":") ? site.substring(0, site.indexOf(":")) : site;
    }


    @Override
    public Map<String, String> getAdminSiteHeaders() {
        return (Map<String, String>) getConfigValue(ADMIN_SITE_REQUEST_HEADERS, k -> _requestHeaders());
    }



    @Override
    public String getAdminSiteUrl() {
        // cached whether or not ADMIN_SITE_URL is set: the value always comes from config
        // and/or the company's stored portal url - never from the request - so it is safe to
        // pin in the cache. When the config changes, the AdminSiteKeyListener invalidates it.
        return (String) getConfigValue(ADMIN_SITE_URL, k -> _baseAdminSiteDomain());
    }

    /**
     * calculates the admin site url based on config properties. Only runs on cache misses,
     * so each log below is naturally "once per config change".
     *
     * @return
     */
    String _baseAdminSiteDomain() {
        if (!isAdminSiteConfigured()) {
            Logger.warn(AdminSiteAPI.class,
                    "ADMIN_SITE_URL is not configured.  This is the url that is used to access dotCMS. Please add it to your system's environmental variables, e.g. DOT_ADMIN_SITE_URL=https://www.siteadmin.com or DOT_ADMIN_SITE_URL=https://www.siteadmin.com:8443");
        }

        // NOTE: We intentionally do NOT fall back to the request's Host header here.
        // Deriving the admin url from the client-controlled Host header would allow
        // Host-header poisoning of getAdminSiteUrl/getAdminDomains.
        // We also do not fall back to the legacy company portal url: the admin url always
        // resolves to ADMIN_SITE_URL when set, and to the dotCMS default otherwise.
        String adminSiteUrl = Config.getStringProperty(ADMIN_SITE_URL, _ADMIN_SITE_URL_DEFAULT);

        while (adminSiteUrl.endsWith("/")) {
            adminSiteUrl = adminSiteUrl.substring(0, adminSiteUrl.length() - 1);
        }

        if (!adminSiteUrl.startsWith("http://") && !adminSiteUrl.startsWith("https://")) {
            Logger.info(AdminSiteAPI.class, "ADMIN_SITE_URL: '" + adminSiteUrl
                    + "' is not a valid return URL - adding https:// to the ADMIN_SITE_URL.  This should be part of the configuration, e.g. DOT_ADMIN_SITE_URL=https://www.yoursite.com or DOT_ADMIN_SITE_URL=https://www.yoursite.com:8443");
            adminSiteUrl = "https://" + adminSiteUrl;
        }

        // Remove any path from the URL (keep only protocol://host:port)
        int protocolEnd = adminSiteUrl.indexOf("://");
        if (protocolEnd > 0) {
            int pathStart = adminSiteUrl.indexOf("/", protocolEnd + 3);
            if (pathStart > 0) {
                Logger.info(AdminSiteAPI.class,
                        "ADMIN_SITE_URL should not include a path, e.g it should be set to https://www.yoursite.com, not https://www.yoursite.com/dotAdmin. Removing the path or uri after the domain/port");
                adminSiteUrl = adminSiteUrl.substring(0, pathStart);
            }
        }

        Logger.info(AdminSiteAPI.class, "*********************");
        Logger.info(AdminSiteAPI.class, "* Setting ADMIN_SITE_URL to " + adminSiteUrl);
        Logger.info(AdminSiteAPI.class,
                "* - this url will be used to build internal links back to your dotCMS administrative instance.");
        Logger.info(AdminSiteAPI.class, "*********************");

        return adminSiteUrl;

    }

    String[] getAdminUris() {
        return (String[]) getConfigValue(ADMIN_SITE_REQUEST_URIS, k -> _adminUris());
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


    String[] getAdminDomains() {
        return (String[]) getConfigValue(ADMIN_SITE_REQUEST_DOMAINS, k -> _adminDomains());
    }


    String[] _adminDomains() {

        // NOTE: the host of the ADMIN_SITE_URL is matched dynamically in isAdminSite(String)
        // and is intentionally not baked into this cached list - it may vary when ADMIN_SITE_URL
        // is not configured.

        // Add defaults (lowercased)
        Set<String> allowedHosts = new HashSet<>();
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


    /**
     * Whether backend users can call the /api/v1/authentication api on non-admin sites.
     * This is checked on every login attempt, so the value is lazily computed once and cached
     * (invalidated by {@link #invalidateCache()} when the system_table changes).
     */
    @Override
    public boolean allowBackendLoginsOnNonAdminSites() {
        return (Boolean) getConfigValue(ADMIN_SITE_ALLOW_BACKEND_LOGINS_ANY_SITE,
                k -> Config.getBooleanProperty(ADMIN_SITE_ALLOW_BACKEND_LOGINS_ANY_SITE, false));
    }

}
