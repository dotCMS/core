package com.dotcms.vanityurl.handler;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrlResult;
import com.dotmarketing.beans.Host;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.filters.CmsUrlUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
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

    private static final VanityUrlResult DEFAULT_RESULT = new VanityUrlResult(null, null,
            CMSFilter.IAm.NOTHING_IN_THE_CMS, true);

    private static final CmsUrlUtil urlUtil = CmsUrlUtil.getInstance();

    @Override
    public VanityUrlResult handle(final CachedVanityUrl vanityUrl,
            final HttpServletResponse response, final Host host,
            final long languageId) throws IOException {
        String rewrite = null;
        String queryString = null;
        CMSFilter.IAm iAm = CMSFilter.IAm.NOTHING_IN_THE_CMS;

        if (vanityUrl != null) {
            rewrite = InodeUtils.isSet(vanityUrl.getForwardTo()) ? vanityUrl.getForwardTo() : null;

            if (vanityUrl.getResponse() == HttpServletResponse.SC_OK) {
                //then forward
                response.setStatus(vanityUrl.getResponse());
            } else if (vanityUrl.getResponse() == HttpServletResponse.SC_MOVED_PERMANENTLY
                    || vanityUrl.getResponse() == HttpServletResponse.SC_FOUND) {
                //redirect
                response.setStatus(vanityUrl.getResponse());
                response.sendRedirect(rewrite);

                return DEFAULT_RESULT;
            } else {
                //errors
                response.sendError(vanityUrl.getResponse());

                return DEFAULT_RESULT;
            }
        }
        if (UtilMethods.isSet(rewrite) && rewrite.contains("//")) {
            response.sendRedirect(rewrite);

            return DEFAULT_RESULT;
        }
        if (UtilMethods.isSet(rewrite)) {
            if (rewrite != null && rewrite.contains("?")) {
                String[] arr = rewrite.split("\\?", 2);
                rewrite = arr[0];
                if (arr.length > 1) {
                    queryString = arr[1];
                }
            }
            iAm = rewriteIs(rewrite, host, languageId);
        }
        return new VanityUrlResult(rewrite, queryString, iAm, false);
    }

    /**
     * Identify what type of CMSFiltet.IAm object is the rewrite url (file asset, page, folder o
     * nothing)
     *
     * @param rewrite The current URL to forward/redirect
     * @param host The current host
     * @param languageId The current language id
     * @return CMSFiltet.IAm object
     */
    private CMSFilter.IAm rewriteIs(final String rewrite, Host host, final long languageId) {
        CMSFilter.IAm iAm = CMSFilter.IAm.NOTHING_IN_THE_CMS;

        if (urlUtil.isFileAsset(rewrite, host, languageId)) {
            iAm = CMSFilter.IAm.FILE;
        } else if (urlUtil.isPageAsset(rewrite, host, languageId)) {
            iAm = CMSFilter.IAm.PAGE;
        } else if (urlUtil.isFolder(rewrite, host)) {
            iAm = CMSFilter.IAm.FOLDER;
        }

        return iAm;
    }
}
