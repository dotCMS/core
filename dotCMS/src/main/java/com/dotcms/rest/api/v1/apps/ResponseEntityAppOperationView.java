package com.dotcms.rest.api.v1.apps;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for app operation responses.
 * Contains operation confirmation messages for app actions.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityAppOperationView extends ResponseEntityView<String> {
    public ResponseEntityAppOperationView(final String entity) {
        super(entity);
    }
}
