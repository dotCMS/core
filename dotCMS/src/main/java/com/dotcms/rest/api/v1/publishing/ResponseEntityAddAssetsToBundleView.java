package com.dotcms.rest.api.v1.publishing;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response entity view wrapper for add-assets-to-bundle results.
 * Provides proper type information for OpenAPI/Swagger documentation.
 *
 * @author hassandotcms
 * @since Mar 2026
 */
public class ResponseEntityAddAssetsToBundleView extends ResponseEntityView<AddAssetsToBundleView> {

    /**
     * Creates a response with the add-assets result.
     *
     * @param entity The add-assets result view
     */
    public ResponseEntityAddAssetsToBundleView(final AddAssetsToBundleView entity) {
        super(entity);
    }

}
