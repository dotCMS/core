package com.dotcms.vanityurl.handler;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrlResult;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

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
    public VanityUrlResult handle(final String uri,
            final HttpServletResponse response, final Host host,
            final long languageId, final User user) throws IOException {

        CachedVanityUrl vanityUrl = APILocator.getVanityUrlAPI()
                .getLiveCachedVanityUrl(("/".equals(uri) ? CMS_HOME_PAGE
                                : uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri), host,
                        languageId, user);

        return handle(vanityUrl, response, host, languageId);
    }

    @Override
    public VanityUrlResult handle(final CachedVanityUrl vanityUrl,
                                  final HttpServletResponse response, final Host host,
                                  final long languageId) throws IOException {
        String rewrite = null;
        String queryString = null;
        VanityUrlResult vanityUrlResult = null;

        if (vanityUrl != null) {
            rewrite = InodeUtils.isSet(vanityUrl.getForwardTo()) ? vanityUrl.getForwardTo() : null;

            vanityUrlResult = processVanityResponse(vanityUrl,rewrite, response);
        }

        if (vanityUrlResult != null) {
            // if a redirect or error ations was processed
            return vanityUrlResult;
        }

        if (UtilMethods.isSet(rewrite) && rewrite.contains("//")) {
            //if the forward is and external url
            response.sendRedirect(rewrite);

            return DEFAULT_RESULT;
        }
        if (UtilMethods.isSet(rewrite)) {
            if (rewrite != null && rewrite.contains("?")) {
                String[] arr = rewrite.split("\\?", LIMIT);
                rewrite = arr[0];
                if (arr.length > 1) {
                    queryString = arr[1];
                }
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