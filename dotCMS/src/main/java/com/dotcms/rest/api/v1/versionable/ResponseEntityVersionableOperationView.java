package com.dotcms.rest.api.v1.versionable;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for versionable operation responses.
 * Contains operation confirmation messages for versionable actions.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityVersionableOperationView extends ResponseEntityView<String> {
    public ResponseEntityVersionableOperationView(final String entity) {
        super(entity);
    }
}
