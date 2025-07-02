package com.dotcms.rest.api.v1.container;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for container operation confirmation responses.
 * Contains boolean values or operation result messages for container actions.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityContainerOperationView extends ResponseEntityView<Boolean> {
    public ResponseEntityContainerOperationView(final Boolean entity) {
        super(entity);
    }
}