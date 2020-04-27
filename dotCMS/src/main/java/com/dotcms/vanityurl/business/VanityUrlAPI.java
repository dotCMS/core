package com.dotcms.vanityurl.business;

import java.util.List;
import java.util.Optional;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;

/**
 * This API provides access to the information related to Vanity URLs in dotCMS. Vanity URLs are
 * alternate reference paths to internal or external URL's. Vanity URLs are most commonly used to
 * give visitors to the website a more user-friendly or memorable way of reaching an HTML page or
 * File, that might actually live “buried” in a much deeper path.
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 12, 2017
 */
public interface VanityUrlAPI {
    public static final String DEFAULT_VANITY_URL_STRUCTURE_INODE = "8e850645-bb92-4fda-a765-e67063a59be0";
    public static final String DEFAULT_VANITY_URL_STRUCTURE_VARNAME = "Vanityurl";
    public static final String DEFAULT_VANITY_URL_STRUCTURE_NAME = "Vanity URL";

    void validateVanityUrl(Contentlet contentlet);

    Optional<CachedVanityUrl> resolveVanityUrl(String url, Host host, Language language);

    VanityUrl fromContentlet(Contentlet contentlet);

    void populateAllVanityURLsCache() throws DotDataException;

    void invalidateVanityUrl(Contentlet contentlet);

    List<CachedVanityUrl> findInDb(Host host, Language lang);



}
