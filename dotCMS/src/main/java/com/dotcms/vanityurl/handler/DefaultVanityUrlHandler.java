package com.dotcms.vanityurl.handler;

import com.dotcms.vanityurl.model.VanityUrl;
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

    private final static VanityUrlResult DEFAULT_RESULT = new VanityUrlResult(null,null,null,true);
    private final static CmsUrlUtil urlUtil = CmsUrlUtil.getInstance();

    @Override
    public VanityUrlResult handle(final VanityUrl vanityUrl, final HttpServletResponse response, final Host host,
            final long languageId) throws IOException {
        String rewrite = null;
        String queryString = null;
        CMSFilter.IAm iAm = null;

        if (vanityUrl != null) {
            rewrite = InodeUtils.isSet(vanityUrl.getInode()) ? vanityUrl.getForwardTo() : null;

            if (vanityUrl.getAction() == HttpServletResponse.SC_OK) {
                //then forward
                response.setStatus(vanityUrl.getAction());
            } else if (vanityUrl.getAction() == HttpServletResponse.SC_MOVED_PERMANENTLY
                    || vanityUrl.getAction() == HttpServletResponse.SC_FOUND) {
                //redirect
                response.setStatus(vanityUrl.getAction());
                response.sendRedirect(rewrite);

                return DEFAULT_RESULT;
            } else {
                //errors
                response.sendError(vanityUrl.getAction());

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
            if (urlUtil.isFileAsset(rewrite, host, languageId)) {
                iAm = CMSFilter.IAm.FILE;
            } else if (urlUtil.isPageAsset(rewrite, host, languageId)) {
                iAm = CMSFilter.IAm.PAGE;
            } else if (urlUtil.isFolder(rewrite, host)) {
                iAm = CMSFilter.IAm.FOLDER;
            }
        }
        return new VanityUrlResult(rewrite, queryString, iAm, false);
    }
}
