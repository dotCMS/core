package com.dotcms.rest.api.v1.asset.view;

import com.dotcms.rest.ResponseEntityView;

/**
 * Typical dotCMS resource fashion EntityView wrapper class
 */
public class WebAssetEntityView extends ResponseEntityView<WebAssetView> {
    public WebAssetEntityView(final WebAssetView entity) {
        super(entity);
    }

}
