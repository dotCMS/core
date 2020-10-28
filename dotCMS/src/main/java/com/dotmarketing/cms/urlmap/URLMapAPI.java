package com.dotmarketing.cms.urlmap;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import java.util.Optional;

/**
 * API for resolve {@link com.dotcms.contenttype.model.type.ContentType}, s URL patterns
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