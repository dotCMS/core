package com.dotcms.rest.api.v1.system;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for configuration operation responses.
 * Contains operation confirmation messages for configuration actions.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityConfigurationOperationView extends ResponseEntityView<String> {
    public ResponseEntityConfigurationOperationView(final String entity) {
        super(entity);
    }
}