package com.dotcms.vanity;

import com.dotcms.content.model.VanityUrl;
import com.dotmarketing.beans.Host;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This class have all the method required to handle the Vanity URL in the {@link com.dotmarketing.filters.CMSFilter}
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 16, 2017
 */
public interface VanityUrlHandler {

    /**
     * This method handle the logic to process the Vanity URLs and returns a
     * VanityUrlResult Object
     * @param vanityUrl The vanity URL object
     * @param response The HttpServletResponse
     * @param host The current host
     * @param languageId The current languageId
     * @return a VanityURLResult Object
     * @throws IOException
     */
    VanityUrlResult handle(VanityUrl vanityUrl, HttpServletResponse response, Host host, long languageId) throws IOException;
}
