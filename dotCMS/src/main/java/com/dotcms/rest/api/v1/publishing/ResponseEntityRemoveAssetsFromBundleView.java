package com.dotcms.rest.api.v1.publishing;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;

/**
 * Response entity view wrapper for remove-assets-from-bundle results.
 * Provides proper type information for OpenAPI/Swagger documentation.
 *
 * @author hassandotcms
 * @since Mar 2026
 */
public class ResponseEntityRemoveAssetsFromBundleView extends ResponseEntityView<List<RemoveAssetResultView>> {

    /**
     * Creates a response with the list of per-asset removal results.
     *
     * @param entity The list of removal results
     */
    public ResponseEntityRemoveAssetsFromBundleView(final List<RemoveAssetResultView> entity) {
        super(entity);
    }

}
