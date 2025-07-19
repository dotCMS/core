package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for content type JSON string responses.
 * Contains JSON representation of content type data for specific formatting needs.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityContentTypeJsonView extends ResponseEntityView<String> {
    public ResponseEntityContentTypeJsonView(final String entity) {
        super(entity);
    }
}