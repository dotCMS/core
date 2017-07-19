package com.dotcms.vanityurl.business;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.util.List;

/**
 * This API provides access to the information related to Vanity URLs
 * in dotCMS. Vanity URLs are alternate reference paths to
 * internal or external URL's. Vanity URLs are most commonly used to give
 * visitors to the website a more user-friendly or memorable way of reaching an
 * HTML page or File, that might actually live “buried” in a much deeper path.
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 12, 2017
 */
public interface VanityUrlAPI {

    public static final String CACHE_404_VANITY_URL = "CACHE_404_VANITY_URL";

    /**
     * Searches for live VanityURLs, each VanityURL found is added into the cache.
     * <br>
     * Note this method does not uses cache, always does the ES search, the intention of this method
     * is mainly to populate the cache with the found data.
     *
     * @param user The current user
     * @return a List of all Cached Vanity URLs contentlets live
     */
    List<VanityUrl> getActiveVanityUrls(final User user);

    /**
     * Searches for live VanityURLs by Site and Language id, each VanityURL found is added into
     * the cache.
     * <br>
     * Note this method does not uses cache, always does the ES search, the intention of this method
     * is mainly to populate the cache with the found data.
     *
     * @param user The current user
     * @return a List of all Cached Vanity URLs contentlets live
     */
    List<VanityUrl> getActiveVanityUrlsBySiteAndLanguage(final String siteId, final long languageId,
                                                         final User user);

    /**
     * Return the live version of the Cached vanityurl URL contentlet with the specified URI
     *
     * @param uri The URI of the vanityurl URL
     * @param host The current host
     * @param languageId The current language Id
     * @param user The current user
     * @return the live version of the vanityurl URL contentlet
     */
    CachedVanityUrl getLiveCachedVanityUrl(final String uri, final Host site, final long languageId,
                                           final User user);

    /**
     * Convert the contentlet into a Vanity URL object
     *
     * @param con the contentlet
     * @return Vanity URL
     */
    VanityUrl getVanityUrlFromContentlet(final Contentlet con);

    /**
     * This method checks that the Vanity Url URI is using a valid regular expression.
     *
     * @param contentlet The Vanity Url Contentlet
     */
    void validateVanityUrl(Contentlet contentlet);

}