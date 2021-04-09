package com.dotmarketing.cms.urlmap;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import java.util.Optional;

/**
 * This API resolves {@link com.dotcms.contenttype.model.type.ContentType} objects based off of URL patterns. <p> The
 * URL Map feature on dotCMS Content Types is an easy way to automatically create friendly URL's for Search Engine
 * Optimization (SEO) as the content is contributed. When you create a URL Map for a Content Type, each individual
 * content item can be viewed from the front-end of your site as if it had it's own separate page, without you having to
 * create a separate page for each content item. </p>
 *
 * @author Freddy Rodriguez
 * @since Feb 22, 2019
 */
public interface URLMapAPI {

    /**
     * Resolve a page through the {@link com.dotcms.contenttype.model.type.ContentType}'s URL Pattern
     *
     * @param context context to resolve a page
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    Optional<URLMapInfo> processURLMap(final UrlMapContext context)
            throws DotSecurityException, DotDataException;

    /**
     * Return true if the given {@link UrlMapContext#getUri()} is an URLMap and in order to do that
     * the requested URI needs to be evaluated against all the existing URLMaps patterns, if a match
     * is found the URI will be used to find the associated contentlet to that URLMap pattern.
     *
     * @param urlMapContext
     * @return True if the requested URI is an URLMap
     * @throws DotDataException
     * @throws DotSecurityException
     */
    boolean isUrlPattern(final UrlMapContext urlMapContext)
            throws DotDataException, DotSecurityException;

}