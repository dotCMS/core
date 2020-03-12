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

    boolean isUrlPattern(final UrlMapContext urlMapContext)
            throws DotDataException, DotSecurityException;

}