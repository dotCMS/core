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
     * Legacy Vanity URL URI used as the fallback home page. When the incoming
     * request path is "/" and no other Vanity URL matches, implementations
     * resolve this URI instead to support the historical cmsHomePage behavior.
     */
    String LEGACY_CMS_HOME_PAGE = "/cmsHomePage";

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
     * Look up all published {@link VanityUrl}s whose {@code forwardTo} matches the
     * given {@code forward} and whose action equals {@code action}.
     *
     * <p>When {@code includeSystemHost} is {@code true} the result also contains
     * vanities published on {@code SYSTEM_HOST}, mirroring the host-resolution
     * fallback in {@link #resolveVanityUrl}. Callers that only care about the
     * given host should pass {@code false}. The flag is explicit (rather than a
     * default) so the widened result scope is visible at every call site.
     *
     * <p><b>Authorization:</b> This method is intended for system-user / internal
     * routing contexts (e.g. the Experiments URL pattern engine) where the caller
     * represents the platform itself rather than an end user. It performs no
     * permission check. Do not use it where the caller lacks {@code READ}
     * permission on the host, or where results are exposed directly to an
     * end user — especially when {@code includeSystemHost} is {@code true}.
     *
     * @param host {@link VanityUrl}'s Host
     * @param language {@link VanityUrl}'s Language
     * @param forward forward target to look for
     * @param action HTTP action code to look for (e.g. 200, 301, 302)
     * @param includeSystemHost if {@code true}, also return vanities published on {@code SYSTEM_HOST}
     * @return the matching {@link CachedVanityUrl}s from the given host, and optionally from {@code SYSTEM_HOST}
     */
    List<CachedVanityUrl> findByForward(Host host, Language language, String forward, int action,
                                        boolean includeSystemHost);

    /**
     * Backward-compatible overload of
     * {@link #findByForward(Host, Language, String, int, boolean)} that only
     * searches the specified host ({@code includeSystemHost = false}). Preserves
     * the behavior that existed before the {@code SYSTEM_HOST} fallback was
     * added. New callers should prefer the 5-arg form and choose the flag
     * explicitly.
     *
     * @param host     {@link VanityUrl}'s Host
     * @param language {@link VanityUrl}'s Language
     * @param forward  forward target to look for
     * @param action   HTTP action code to look for (e.g. 200, 301, 302)
     * @return the matching {@link CachedVanityUrl}s from the given host only
     */
    default List<CachedVanityUrl> findByForward(final Host host, final Language language,
                                                final String forward, final int action) {
        return findByForward(host, language, forward, action, false);
    }

    /**
     *
     * @param vanityUrl
     * @param uri
     * @return
     */
    boolean isSelfReferenced(final CachedVanityUrl vanityUrl, final String uri);

}
