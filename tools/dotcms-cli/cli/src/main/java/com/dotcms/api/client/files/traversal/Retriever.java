package com.dotcms.api.client.files.traversal;

import com.dotcms.api.AssetAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.model.asset.AssetVersionsView;
import com.dotcms.model.asset.FolderView;
import com.dotcms.model.asset.SearchByPathRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

import static com.dotcms.common.AssetsUtils.BuildRemoteAssetURL;
import static com.dotcms.common.AssetsUtils.BuildRemoteURL;

/**
 * Utility class for retrieving folder and asset information from the remote server using REST calls.
 */
@ApplicationScoped
public class Retriever {

    @Inject
    protected RestClientFactory clientFactory;

    /**
     * Retrieves folder information from the remote server.
     *
     * @param siteName   the name of the site
     * @param folderPath the folder path
     * @return the FolderView object representing the folder information
     */
    @ActivateRequestContext
    public FolderView retrieveFolderInformation(String siteName, String folderPath) {

        final AssetAPI assetAPI = this.clientFactory.getClient(AssetAPI.class);

        final var remoteFolderPath = BuildRemoteURL(siteName, folderPath);

        // Execute the REST call to retrieve folder contents
        var response = assetAPI.folderByPath(SearchByPathRequest.builder().assetPath(remoteFolderPath).build());
        return response.entity();
    }

    /**
     * Retrieves asset information from the remote server.
     *
     * @param siteName   the name of the site
     * @param folderPath the folder path
     * @param assetName  the name of the asset
     * @return the AssetVersionsView object representing the asset information
     */
    @ActivateRequestContext
    public AssetVersionsView retrieveAssetInformation(final String siteName, String folderPath, final String assetName) {

        final AssetAPI assetAPI = this.clientFactory.getClient(AssetAPI.class);

        final var remoteAssetPath = BuildRemoteAssetURL(siteName, folderPath, assetName);

        // Execute the REST call to retrieve asset information
        var response = assetAPI.assetByPath(SearchByPathRequest.builder().assetPath(remoteAssetPath).build());
        return response.entity();
    }

}

