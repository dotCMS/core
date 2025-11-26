package com.dotcms.rest.api.v2.contenttype;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for field deletion responses.
 * Contains confirmation of field deletion operations.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityFieldDeletionView extends ResponseEntityView<String> {
    public ResponseEntityFieldDeletionView(final String entity) {
        super(entity);
    }
}