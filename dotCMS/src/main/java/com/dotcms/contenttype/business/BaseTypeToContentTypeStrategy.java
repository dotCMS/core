package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;

import java.util.Map;
import java.util.Optional;

/**
 * Strategy to resolve a content type based on a base type.
 * @author jsanca
 */
public interface BaseTypeToContentTypeStrategy {

    /**
     * Applies the strategy to resolve the content type
     * @param baseContentType {@link BaseContentType}
     * @param contextMap      {@link Map} optional extra information passed to resolve the content type
     * @return Optional ContentType
     */
    Optional<ContentType> apply(final BaseContentType baseContentType, final Map<String, Object> contextMap);
}
