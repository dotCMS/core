package com.dotcms.filters;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import io.vavr.Lazy;
import io.vavr.control.Try;

/**
 * This filter will only allow backend access to requests coming on a specific host
 *
 */
public class ManagementRequestFilter implements Filter {


    // Default list of disallowed paths
    private static final String[] MANAGEMENT_REQUEST_REQUIRED_URIS = {
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



    // Default list of Allowed Domains
    private static final String[] MANAGEMENT_REQUEST_DOMAINS = {
            "dotcms.com",
            "dotcms.site",
            "dotcms.io",
            "dotcms.host",
            "dotcms.cloud",
            "dotcmscloud.com"};



    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
                    throws IOException, ServletException {

        final HttpServletRequest request = ((HttpServletRequest) servletRequest);
        final HttpServletResponse response = ((HttpServletResponse) servletResponse);
        if (managementRequestRequired(request) && !allowManagementRequest(request)) {
            response.sendError(404);
            return;
        }
        
        if (managementRequestRequired(request) ) {
            response.addHeader("X-Robots-Tag",  "noindex, nofollow");
        }


        filterChain.doFilter(servletRequest, servletResponse);
    }



    boolean managementRequestRequired(HttpServletRequest request) {

        String uri = request.getRequestURI().toLowerCase();

        for (String test : managementUrls.get()) {
            if (uri.startsWith(test)) {
                return true;
            }
        }
        return false;
    }


    boolean allowManagementRequest(HttpServletRequest request) {

        if (!request.isSecure()) {
            Logger.warn(getClass(),
                            "Requests to the dotCMS backend can only be made through a port marked secure, e.g. 8082 or 8443");
            return false;
        }



        final String urlString = request.getRequestURL().toString();

        URL url = Try.of(() -> new URL(urlString)).getOrElseThrow(DotRuntimeException::new);

        for (String test : managementHosts.get()) {
            if (url.getHost().endsWith(test)) {
                request.setAttribute(WebKeys.MANAGEMENT_REQUEST_VALIDATED, true);
                return true;
            }
        }


        return false;
    }



    final Lazy<String[]> managementUrls = Lazy.of(() -> Config.getStringArrayProperty("MANAGEMENT_REQUEST_REQUIRED_URIS", MANAGEMENT_REQUEST_REQUIRED_URIS));

    final Lazy<String[]> managementHosts = Lazy.of(() -> {

        
        List<String> allowedHosts = Arrays.asList(Config.getStringArrayProperty("MANAGEMENT_REQUEST_DOMAINS", MANAGEMENT_REQUEST_DOMAINS));

        final String portalUrlString = APILocator.getCompanyAPI().getDefaultCompany().getPortalURL().contains("://")
            ? APILocator.getCompanyAPI().getDefaultCompany().getPortalURL()
            : "https://" + APILocator.getCompanyAPI().getDefaultCompany().getPortalURL();



        URL portalUrl = Try.of(() -> new URL(portalUrlString)).getOrElseThrow(DotRuntimeException::new);

        allowedHosts.add(0, portalUrl.getHost().toLowerCase());
        
        return allowedHosts.toArray(new String[allowedHosts.size()]);

    });

    @Override
    public void init(FilterConfig filterConfig) throws ServletException { /* Nothing to do here */ }

    @Override
    public void destroy() { /* Nothing to do here */ }



}
