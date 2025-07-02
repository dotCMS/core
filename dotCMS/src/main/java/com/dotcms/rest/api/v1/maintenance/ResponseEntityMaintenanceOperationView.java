package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for maintenance operation responses.
 * Contains maintenance operation confirmation messages.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityMaintenanceOperationView extends ResponseEntityView<String> {
    public ResponseEntityMaintenanceOperationView(final String entity) {
        super(entity);
    }
}
