package com.dotcms.vanityurl.business;

import com.dotcms.vanityurl.filters.VanityUrlRequestWrapper;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotcms.vanityurl.model.VanityUrlResult;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;

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

    String VANITY_URL_RESPONSE_HEADER = "X-DOT-VanityUrl";

    /**
     * Verifies that the Vanity URL as Contentlet has all the required fields. the list of mandatory fields can be
     * verified in the Content Type's definition.
     *
     * @param contentlet The Vanity URL as {@link Contentlet}.
     *
     * @throws DotContentletValidationException At least one required Vanity URL parameter was either not found, or was
     *                                          null/empty.
     */
    void validateVanityUrl(Contentlet contentlet);

    /**
     * Resolves a the url based on the url, host and language passed.
     * If it has several fallbacks:
     *      * First tries cache
     *      * If not found, tries using the host and language passed
     *      * If not found, tries using the host and the default language
     *      * If not found, tries using the System_Host and the language passed
     *      * If not found, checks that if the url passed is the cmsHomePage
     *
     * @param url url to be resolved
     * @param host host were the vanity should live
     * @param language language of the vanity
     * @return a {@link CachedVanityUrl} object
     */
    Optional<CachedVanityUrl> resolveVanityUrl(String url, Host host, Language language);

    /**
     * Transforms a given {@link Contentlet} object into a {@link VanityUrl} object.
     *
     * @param contentlet Contentlet to transform to VanityUrl
     * @return a {@link VanityUrl} Object
     */
    VanityUrl fromContentlet(Contentlet contentlet);

    /**
     * Populates the cache with all the VanityUrls of all the sites including System_Host
     * @throws DotDataException
     */
    void populateAllVanityURLsCache() throws DotDataException;

    /**
     * Removes the VanityUrl of the cache
     * @param contentlet VanityUrl to invalidate
     */
    void invalidateVanityUrl(Contentlet contentlet);

    /**
     * Executes a SQL query that will return all the Vanity URLs that belong to a specific Site. This
     * method moved from using the ES index to using a SQL query in order to avoid situations where the
     * index was not fully updated when reading new data.
     *
     * @param host The Site whose Vanity URLs will be retrieved.
     * @param language The language used to created the Vanity URLs.
     *
     * @return The list of Vanity URLs.
     */
    List<CachedVanityUrl> findInDb(Host host, Language language);

    /**
     * Executes a SQL query that will return all the Vanity URLs that belong to a specific Site. This
     * method moved from using the ES index to using a SQL query in order to avoid situations where the
     * index was not fully updated when reading new data.
     *
     * @param host The Site whose Vanity URLs will be retrieved.
     *
     * @return The list of Vanity URLs.
     */
    List<CachedVanityUrl> findInDb(Host host);


    /**
    * Product of refactoring handling 301 and 302 previously executed by CachedVanityUrl
    *
    * @return whether or not the redirect was handled
    */
    boolean handleVanityURLRedirects(VanityUrlRequestWrapper request, HttpServletResponse response,
                VanityUrlResult vanityUrlResult);


    /**
     * Look all the {@link VanityUrl} that are equals to forward
     *
     * @param host {@link VanityUrl}'s Host
     * @param language {@link VanityUrl}'s Language
     * @param forward forward to look for
     * @param language action to look for
     * @return
     */
    List<CachedVanityUrl> findByForward(Host host, Language language, String forward, int action);

    /**
     *
     * @param vanityUrl
     * @param uri
     * @return
     */
    boolean isSelfReferenced(final CachedVanityUrl vanityUrl, final String uri);

}
