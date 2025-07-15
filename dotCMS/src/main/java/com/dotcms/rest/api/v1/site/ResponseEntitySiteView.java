package com.dotcms.rest.api.v1.site;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.beans.Host;

/**
 * Entity View for site responses.
 * Contains Host entity data for site operations.
 * 
 * @author Steve Bolton
 */
public class ResponseEntitySiteView extends ResponseEntityView<Object> {
    public ResponseEntitySiteView(final Object entity) {
        super(entity);
    }
}
