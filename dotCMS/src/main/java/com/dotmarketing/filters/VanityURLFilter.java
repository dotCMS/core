package com.dotmarketing.filters;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.vanityurl.handler.VanityUrlHandler;
import com.dotcms.vanityurl.handler.VanityUrlHandlerResolver;
import com.dotcms.vanityurl.model.VanityUrlResult;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;

/**
 * This Filter handles the vanity url logic
 * 
 * @author Jonathan Gamba 7/27/17
 */
// todo: change this to an interceptor
public class VanityURLFilter implements Filter {

    private final VanityUrlHandlerResolver vanityUrlHandlerResolver;
    private final CMSUrlUtil urlUtil;
    private final HostWebAPI hostWebAPI;
    private final LanguageWebAPI languageWebAPI;
    private final UserWebAPI userWebAPI;

    public VanityURLFilter() {

        this(VanityUrlHandlerResolver.getInstance(), CMSUrlUtil.getInstance(), WebAPILocator.getHostWebAPI(),
                        WebAPILocator.getLanguageWebAPI(), WebAPILocator.getUserWebAPI());
    }

    @VisibleForTesting
    protected VanityURLFilter(final VanityUrlHandlerResolver vanityUrlHandlerResolver, final CMSUrlUtil urlUtil,
                    final HostWebAPI hostWebAPI, final LanguageWebAPI languageWebAPI, final UserWebAPI userWebAPI) {

        this.vanityUrlHandlerResolver = vanityUrlHandlerResolver;
        this.urlUtil = urlUtil;
        this.hostWebAPI = hostWebAPI;
        this.languageWebAPI = languageWebAPI;
        this.userWebAPI = userWebAPI;
    }

    public void init(FilterConfig filterConfig) throws ServletException {}

    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain filterChain)
                    throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        // Get the URI from the request and check for possible XSS hacks
        final String uri = this.urlUtil.getURIFromRequest(request);
        
        final boolean isFiltered = this.urlUtil.isVanityUrlFiltered(uri);
        
        // Getting the site form the request
        final Host site = hostWebAPI.getCurrentHostNoThrow(request);

        // do not run again if the filter has been run
        if (!isFiltered && request.getAttribute(Constants.VANITY_URL_HAS_RUN) == null) {
            request.setAttribute(Constants.VANITY_URL_HAS_RUN, true);
            // Get the user language
            final long languageId = this.languageWebAPI.getLanguage(request).getId();

            // Verify if the given URI is a VanityURL
            if (this.urlUtil.isVanityUrl(uri, site, languageId)) {

                // Find the Vanity URL handler and handle this given URI
                final VanityUrlHandler vanityUrlHandler = this.vanityUrlHandlerResolver.getVanityUrlHandler();
                final VanityUrlResult vanityUrlResult =
                                vanityUrlHandler.handle(uri, response, site, languageId, this.userWebAPI.getUser(request));

                // If the handler already resolved the requested URI we stop the processing here
                if (vanityUrlResult.isResolved()) {
                    return;
                }

                filterChain.doFilter(new VanityUrlRequestWrapper(request, vanityUrlResult), response);
                return;
            }

        }

        filterChain.doFilter(request, response);
    } // doFilter.


    public void destroy() {}



} // E:O:F:VanityURLFilter.
