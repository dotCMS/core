package com.dotmarketing.filters;

import static com.dotmarketing.filters.Constants.CMS_FILTER_QUERY_STRING_OVERRIDE;
import static com.dotmarketing.filters.Constants.CMS_FILTER_URI_OVERRIDE;

import com.dotcms.vanityurl.handler.VanityUrlHandler;
import com.dotcms.vanityurl.handler.VanityUrlHandlerResolver;
import com.dotcms.vanityurl.model.VanityUrlResult;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
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
 * @author Jonathan Gamba 7/27/17
 */
public class VanityURLFilter implements Filter {

    private final String RELATIVE_ASSET_PATH = APILocator.getFileAssetAPI()
            .getRelativeAssetsRootPath();
    private VanityUrlHandlerResolver vanityUrlHandlerResolver = VanityUrlHandlerResolver
            .getInstance();
    private CMSUrlUtil urlUtil = CMSUrlUtil.getInstance();

    public void init(FilterConfig arg0) throws ServletException {
    }

    public void doFilter(ServletRequest req, ServletResponse res,
            FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        //Get the URI from the request and check for possible XSS hacks
        final String uri = urlUtil.getURIFromRequest(request);

        //Getting the site form the request
        Host site;
        try {
            site = WebAPILocator.getHostWebAPI().getCurrentHost(request);
            request.setAttribute("host", site);
        } catch (Exception e) {
            Logger.error(this,
                    String.format("Unable to retrieve current Site from request for URI [%s]",
                            uri));
            throw new ServletException(e.getMessage(), e);
        }

        //Get the user language
        long languageId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();

        //Verify if the given URI is a VanityURL
        if (urlUtil.isVanityUrl(uri, site, languageId)) {

            //Find the Vanity URL handler and handle this given URI
            VanityUrlHandler vanityUrlHandler = vanityUrlHandlerResolver.getVanityUrlHandler();
            VanityUrlResult vanityUrlResult = vanityUrlHandler
                    .handle(uri, response, site, languageId,
                            WebAPILocator.getUserWebAPI().getUser(request));

            if (vanityUrlResult.isResult()) {
                DbConnectionFactory.closeSilently();
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
            String vanityURLRewrite = vanityUrlResult.getRewrite();
            request.setAttribute(CMS_FILTER_URI_OVERRIDE, vanityURLRewrite);
        }

        filterChain.doFilter(request, response);
    }

    public void destroy() {
    }

}