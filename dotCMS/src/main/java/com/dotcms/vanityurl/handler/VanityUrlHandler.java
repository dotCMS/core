package com.dotcms.vanityurl.handler;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrlResult;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;

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
     * This method handle the logic to process the Vanity URLs and returns a
     * VanityUrlResult Object
     *
     * @param vanityUrl The vanityurl URL object
     * @param response The HttpServletResponse
     * @param host The current host
     * @param languageId The current languageId
     * @return a VanityURLResult Object
     */

    VanityUrlResult handle(CachedVanityUrl vanityUrl, String uriIn, HttpServletResponse response) throws IOException;
    
    

    
}
