package com.dotmarketing.filters;

import static com.dotmarketing.filters.Constants.CMS_FILTER_QUERY_STRING_OVERRIDE;
import static com.dotmarketing.filters.Constants.CMS_FILTER_URI_OVERRIDE;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.vanityurl.handler.VanityUrlHandler;
import com.dotcms.vanityurl.handler.VanityUrlHandlerResolver;
import com.dotcms.vanityurl.model.VanityUrlResult;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This Filter handles the vanity url logic
 * @author Jonathan Gamba 7/27/17
 */
// todo: change this to an interceptor
public class VanityURLFilter implements Filter {

    private final VanityUrlHandlerResolver vanityUrlHandlerResolver;
    private final CMSUrlUtil urlUtil;
    private final HostWebAPI hostWebAPI;
    private final LanguageWebAPI languageWebAPI;
    private final UserWebAPI  userWebAPI;

    public VanityURLFilter() {

        this (VanityUrlHandlerResolver.getInstance(), CMSUrlUtil.getInstance(),
                WebAPILocator.getHostWebAPI(), WebAPILocator.getLanguageWebAPI(),
                WebAPILocator.getUserWebAPI());
    }

    @VisibleForTesting
    protected VanityURLFilter(final VanityUrlHandlerResolver vanityUrlHandlerResolver,
                           final CMSUrlUtil urlUtil,
                           final HostWebAPI hostWebAPI,
                           final LanguageWebAPI languageWebAPI,
                           final UserWebAPI  userWebAPI) {

        this.vanityUrlHandlerResolver = vanityUrlHandlerResolver;
        this.urlUtil                  = urlUtil;
        this.hostWebAPI               = hostWebAPI;
        this.languageWebAPI           = languageWebAPI;
        this.userWebAPI               = userWebAPI;
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(final ServletRequest req, final ServletResponse res,
            final FilterChain filterChain) throws IOException, ServletException {

        final HttpServletRequest  request  = (HttpServletRequest)  req;
        final HttpServletResponse response = (HttpServletResponse) res;

        //Get the URI from the request and check for possible XSS hacks
        final String uri         = this.urlUtil.getURIFromRequest(request);
        final boolean isFiltered = this.urlUtil.isVanityUrlFiltered (uri);
        //Getting the site form the request
        final Host site = this.getHost(request, uri); // note: this should be call here to include the host in the request

        if (!isFiltered) {

            //Get the user language
            final long languageId = this.languageWebAPI.getLanguage(request).getId();

            //Verify if the given URI is a VanityURL
            if (this.urlUtil.isVanityUrl(uri, site, languageId)) {

                //Find the Vanity URL handler and handle this given URI
                final VanityUrlHandler vanityUrlHandler = this.vanityUrlHandlerResolver.getVanityUrlHandler();
                final VanityUrlResult vanityUrlResult = vanityUrlHandler
                        .handle(uri, response, site, languageId, this.userWebAPI.getUser(request));

                //If the handler already resolved the requested URI we stop the processing here
                if (vanityUrlResult.isResolved()) {
                    DbConnectionFactory.closeSilently(); // todo: not sure if this is necessary
                    return;
                }

            /*
            If the VanityURL has a query string we need to add it to the request in order to override
            in the other filters.
             */
                if (vanityUrlResult.getQueryString() != null) {
                    request.setAttribute(CMS_FILTER_QUERY_STRING_OVERRIDE,
                            vanityUrlResult.getQueryString());
                }

            /*
            Set into the request the VanityURL we need to use to rewrite the current URI
             */
                final String vanityURLRewrite = vanityUrlResult.getRewrite();
                request.setAttribute(CMS_FILTER_URI_OVERRIDE, vanityURLRewrite);
            }
        }

        filterChain.doFilter(request, response);
    } // doFilter.

    private Host getHost(final HttpServletRequest request, final String uri) throws ServletException {

        Host site;
        try {
            site = this.hostWebAPI.getCurrentHost(request);
            request.setAttribute("host", site);
        } catch (Exception e) {
            Logger.error(this,
                    String.format("Unable to retrieve current Site from request for URI [%s]",
                            uri));
            throw new ServletException(e.getMessage(), e);
        }
        return site;
    }

    public void destroy() {
    }

} // E:O:F:VanityURLFilter.