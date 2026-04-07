package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response wrapper for the clean assets status endpoints.
 *
 * @author hassandotcms
 */
public class ResponseEntityCleanAssetsStatusView extends ResponseEntityView<CleanAssetsStatusView> {

    public ResponseEntityCleanAssetsStatusView(final CleanAssetsStatusView entity) {
        super(entity);
    }
}
