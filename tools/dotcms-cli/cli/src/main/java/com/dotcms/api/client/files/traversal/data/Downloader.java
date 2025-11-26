/**
 * This is a class responsible for handling the downloading of assets
 * from a given path through the AssetAPI.
 */
package com.dotcms.api.client.files.traversal.data;

import com.dotcms.api.AssetAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.model.asset.AssetRequest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import java.io.InputStream;

@ApplicationScoped
public class Downloader {

    @Inject
    protected RestClientFactory clientFactory;

    /**
     * Downloads an asset from the given path using the AssetAPI.
     *
     * @param request The AssetRequest containing necessary parameters
     *                for downloading an asset such as path, language, and
     *                whether to fetch live or working copy.
     * @return InputStream The input stream of the downloaded asset.
     */
    @ActivateRequestContext
    public InputStream download(final AssetRequest request) {

        final AssetAPI assetAPI = this.clientFactory.getClient(AssetAPI.class);

        // Download the file
        return assetAPI.download(request);
    }
}
