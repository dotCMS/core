package com.dotcms.rest.api.v1.index;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for index operation responses.
 * Contains operation results for index management actions.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityIndexOperationView extends ResponseEntityView<Object> {
    public ResponseEntityIndexOperationView(final Object entity) {
        super(entity);
    }
}
