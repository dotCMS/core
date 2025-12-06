package com.dotcms.adminsite;


import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This filter will only allow backend access to requests coming on a specific host
 *
 */
public class AdminSiteRequestFilter implements Filter {

    private final AdminSiteAPI adminSiteAPI;


    public AdminSiteRequestFilter(AdminSiteAPI adminSiteAPI) {
        this.adminSiteAPI = adminSiteAPI;
    }

    public AdminSiteRequestFilter() {
        this(APILocator.getAdminSiteAPI());
    }




    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        if (!adminSiteAPI.isAdminSiteEnabled()) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }



        final HttpServletRequest request = ((HttpServletRequest) servletRequest);
        final HttpServletResponse response = ((HttpServletResponse) servletResponse);

        boolean adminURI = adminSiteAPI.isAdminSiteUri(request);
        boolean adminSite = adminSiteAPI.isAdminSite(request);
        boolean httpsOk = isHTTPSOk(request);

        if (adminURI && !adminSite) {
            Logger.warn(getClass(),
                    "Admin URI blocked from non-admin domain: " + request.getRequestURI() + " from host: "
                            + request.getHeader("host"));

            response.addHeader("x-dot-admin-site", "true");
            response.sendError(404);
            return;
        }

        if (adminURI && !httpsOk) {
            Logger.warn(getClass(),
                    "Requests to the dotCMS admin backend can only be made through a port marked secure, e.g. 8082 or 8443");
            response.sendError(426, "Upgrade to HTTPS");
            return;
        }

        if (adminSite) {
            for (Map.Entry<String, String> header : adminSiteAPI.getAdminSiteHeaders().entrySet()) {
                response.addHeader(header.getKey(), header.getValue());
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }


    boolean isHTTPSOk(HttpServletRequest request) {
        return adminSiteAPI.allowInsecureRequests() || request.isSecure();
    }


    @Override
    public void init(FilterConfig filterConfig) throws ServletException { /* Nothing to do here */ }

    @Override
    public void destroy() { /* Nothing to do here */ }


}
