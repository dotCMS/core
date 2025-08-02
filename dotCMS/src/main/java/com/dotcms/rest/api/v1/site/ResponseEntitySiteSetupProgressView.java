package com.dotcms.rest.api.v1.site;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for site setup progress responses.
 * Contains site setup progress information during background operations.
 * 
 * @author Steve Bolton
 */
public class ResponseEntitySiteSetupProgressView extends ResponseEntityView<Object> {
    public ResponseEntitySiteSetupProgressView(final Object entity) {
        super(entity);
    }
}
