package com.dotcms.rest.api.v1.apps;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.apps.view.AppView;

/**
 * Entity View for single app responses.
 * Contains AppView entity data for individual app details.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityAppView extends ResponseEntityView<AppView> {
    public ResponseEntityAppView(final AppView entity) {
        super(entity);
    }
}
