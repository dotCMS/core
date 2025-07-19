package com.dotcms.rest.api.v1.system.storage;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for storage status responses.
 * Contains storage system status information.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityStorageStatusView extends ResponseEntityView<String> {
    public ResponseEntityStorageStatusView(final String entity) {
        super(entity);
    }
}