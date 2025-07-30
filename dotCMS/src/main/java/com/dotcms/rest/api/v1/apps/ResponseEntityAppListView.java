package com.dotcms.rest.api.v1.apps;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.apps.view.AppView;
import java.util.List;

/**
 * Entity View for app collection responses.
 * Contains list of AppView entities for app listings.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityAppListView extends ResponseEntityView<List<AppView>> {
    public ResponseEntityAppListView(final List<AppView> entity) {
        super(entity);
    }
}
