package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.rest.ResponseEntityView;
import com.google.common.collect.ImmutableMap;

/**
 * Entity View for content type operation responses.
 * Contains operation results like copy operations with dependencies and mappings.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityContentTypeOperationView extends ResponseEntityView<ImmutableMap<Object, Object>> {
    public ResponseEntityContentTypeOperationView(final ImmutableMap<Object, Object> entity) {
        super(entity);
    }
}