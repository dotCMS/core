package com.dotcms.vanityurl.handler;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrlResult;
import com.dotmarketing.beans.Host;
import com.liferay.portal.model.User;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

/**
 * This class have all the method required to handle the Vanity URL in the {@link
 * com.dotmarketing.filters.CMSFilter}
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 16, 2017
 */
public interface VanityUrlHandler {

    /**
     * This method handle the logic to process the Vanity URLs and returns a VanityUrlResult Object
     *
     * @param uri The uri to handle
     * @param response The HttpServletResponse
     * @param host The current host
     * @param languageId The current languageId
     * @param user The user that requested the given uri
     * @return a VanityURLResult Object
     */
    VanityUrlResult handle(final String uri,
            final HttpServletResponse response, final Host host,
            final long languageId, final User user) throws IOException;

    /**
     * This method handle the logic to process the Vanity URLs and returns a
     * VanityUrlResult Object
     *
     * @param vanityUrl The vanityurl URL object
     * @param response The HttpServletResponse
     * @param host The current host
     * @param languageId The current languageId
     * @return a VanityURLResult Object
     */
    VanityUrlResult handle(CachedVanityUrl vanityUrl, HttpServletResponse response, Host host,
            long languageId) throws IOException;
}
