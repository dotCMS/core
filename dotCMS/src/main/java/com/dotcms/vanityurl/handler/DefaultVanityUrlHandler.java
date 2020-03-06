package com.dotcms.vanityurl.handler;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrlResult;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

/**
 * This class implements the methods defined in the {@link VanityUrlHandler}
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 16, 2017
 */
public class DefaultVanityUrlHandler implements VanityUrlHandler {

    private static final String CMS_HOME_PAGE = "/cmsHomePage";

    
    private static final VanityUrlResult DEFAULT_RESULT = new VanityUrlResult(null, null, true);

    private static final int LIMIT = 2;

    @Override
    public VanityUrlResult handle(final String uriIn,
            final HttpServletResponse response, final Host host,
            final Language language, final User user) throws IOException {

        final Optional<CachedVanityUrl> vanityUrl = APILocator.getVanityUrlAPI()
                .resolveVanityUrl((StringPool.SLASH.equals(uriIn) ? CMS_HOME_PAGE
                                : uriIn), host,
                    language);

        return handle(vanityUrl, uriIn, response);
    }

    @Override
    public VanityUrlResult handle(final Optional<CachedVanityUrl> optVanity, final String uriIn,
                                  final HttpServletResponse response) throws IOException {
        String rewrite = null;
        String queryString = null;
        VanityUrlResult vanityUrlResult = null;

        if (optVanity.isPresent()) {
            CachedVanityUrl vanityUrl=optVanity.get();
            rewrite = vanityUrl.processForward(uriIn);
            vanityUrlResult = processVanityResponse(vanityUrl,rewrite, response);
        }

        if (vanityUrlResult != null) {
            // if a redirect or error actions was processed
            return vanityUrlResult;
        }

        if (UtilMethods.isSet(rewrite) && rewrite.contains("//")) {
            //if the forward is and external url
            new CircuitBreakerUrl(rewrite).doOut(response);

            return DEFAULT_RESULT;
        }
        //Search for a query string
        if (UtilMethods.isSet(rewrite) && rewrite.contains("?")) {
            String[] arr = rewrite.split("\\?", LIMIT);
            rewrite = arr[0];
            if (arr.length > 1) {
                queryString = arr[1];
            }
        }
        return new VanityUrlResult(rewrite, queryString, false);
    }

    /**
     * This method process the response actions for the cache and return
     * a VanityUrlResult object
     *
     * @param vanityUrl The Cached Vanityurl URL object
     * @param rewrite The url to forward
     * @param response The HttpServletResponse
     * @return a VanityUrlResult object
     * @throws IOException
     */
    private VanityUrlResult processVanityResponse(final CachedVanityUrl vanityUrl,
                                                  final String rewrite, final HttpServletResponse response) throws IOException {
        VanityUrlResult vanityUrlResult = null;

        if (HttpServletResponse.SC_OK == vanityUrl.getResponse()) {
            //then forward
            response.setStatus(vanityUrl.getResponse());
        } else if (HttpServletResponse.SC_MOVED_PERMANENTLY == vanityUrl.getResponse()
                || HttpServletResponse.SC_FOUND == vanityUrl.getResponse()) {
            //redirect
            response.setStatus(vanityUrl.getResponse());
            if (HttpServletResponse.SC_MOVED_PERMANENTLY == vanityUrl.getResponse()) {
                response.setHeader("Location", rewrite);
            } else {
                response.sendRedirect(rewrite);
            }

            vanityUrlResult = DEFAULT_RESULT;
        } else {
            //errors
            response.sendError(vanityUrl.getResponse());

            vanityUrlResult = DEFAULT_RESULT;
        }
        return vanityUrlResult;
    }

}