package com.dotcms.rest.api.v1.site;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for site deletion responses.
 * Contains site deletion operation results.
 * 
 * @author Steve Bolton
 */
public class ResponseEntitySiteDeleteView extends ResponseEntityView<Object> {
    public ResponseEntitySiteDeleteView(final Object entity) {
        super(entity);
    }
}
