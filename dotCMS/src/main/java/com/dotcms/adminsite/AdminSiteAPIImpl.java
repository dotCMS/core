package com.dotcms.adminsite;

import static com.dotcms.adminsite.AdminSiteConfig.ADMIN_SITE_REQUEST_HEADERS;
import static com.dotcms.adminsite.AdminSiteConfig.ADMIN_SITE_REQUEST_HEADERS_DEFAULT;
import static com.dotcms.adminsite.AdminSiteConfig.ADMIN_SITE_REQUEST_VALIDATED;
import static com.dotcms.adminsite.AdminSiteConfig.adminDomains;
import static com.dotcms.adminsite.AdminSiteConfig.adminUrls;

import com.dotmarketing.util.Config;
import io.vavr.Lazy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;

@ApplicationScoped
public class AdminSiteAPIImpl implements AdminSiteAPI {

    private static Lazy<Map<String, String>> requestHeaders = Lazy.of(() -> {
        String[] tmpHeaders = Config.getStringArrayProperty(ADMIN_SITE_REQUEST_HEADERS,
                ADMIN_SITE_REQUEST_HEADERS_DEFAULT);
        if (tmpHeaders == null || tmpHeaders.length == 0) {
            tmpHeaders = new String[0];
        }
        // headers need to come in pairs
        if (tmpHeaders.length % 2 == 1) {
            tmpHeaders = Arrays.copyOfRange(tmpHeaders, 0, tmpHeaders.length - 1);
        }
        Map<String, String> headers = new HashMap<>(tmpHeaders.length);
        for (int i = 0; i < tmpHeaders.length; i++) {
            headers.put(tmpHeaders[i], tmpHeaders[++i]);
        }
        return Collections.unmodifiableMap(headers);

    });

    @Override
    public boolean isAdminSiteUri(HttpServletRequest request) {

        String uri = request.getRequestURI().toLowerCase();

        return isAdminSiteUri(uri);
    }

    @Override
    public boolean isAdminSiteUri(String uri) {
        for (String test : adminUrls.get()) {
            if (uri.startsWith(test)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAdminSite(HttpServletRequest request) {

        final String host =
                request.getHeader("host") != null ? request.getHeader("host").toLowerCase() : "local.dotcms.site";

        if (isAdminSite(host)) {
            request.setAttribute(ADMIN_SITE_REQUEST_VALIDATED, true);
            return true;
        }

        return false;
    }

    @Override
    public boolean isAdminSite(String site) {

        for (String test : adminDomains.get()) {
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
    public Map<String, String> getAdminSiteHeaders() {
        return requestHeaders.get();
    }

}
