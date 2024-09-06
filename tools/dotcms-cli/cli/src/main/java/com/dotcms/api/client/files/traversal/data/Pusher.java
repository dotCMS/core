package com.dotcms.api.client.files.traversal.data;

import static com.dotcms.common.AssetsUtils.buildRemoteAssetURL;
import static com.dotcms.common.AssetsUtils.buildRemoteURL;
import static com.dotcms.common.AssetsUtils.statusToBoolean;
import static com.dotcms.common.LocationUtils.localPathFromAssetData;

import com.dotcms.api.AssetAPI;
import com.dotcms.api.FolderAPI;
import com.dotcms.api.LanguageAPI;
import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.ByPathRequest;
import com.dotcms.model.asset.FileUploadData;
import com.dotcms.model.asset.FileUploadDetail;
import com.dotcms.model.language.Language;
import com.dotcms.model.site.CreateUpdateSiteRequest;
import com.dotcms.model.site.SiteView;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class Pusher implements Serializable {

    @Inject
    protected RestClientFactory clientFactory;

    @Inject
    Logger logger;

    /**
     * Creates a language in the remote server.
     *
     * @param language the language to create
     * @return the created language
     */
    @ActivateRequestContext
    public Language createLanguage(String language) {

        final LanguageAPI languageAPI = this.clientFactory.getClient(LanguageAPI.class);

        // Execute the REST call to create the language
        final var response = languageAPI.create(language);
        return response.entity();
    }

    /**
     * Archives an asset in the remote server.
     *
     * @param siteName   the name of the site
     * @param folderPath the folder path
     * @param assetName  the name of the asset
     * @return true if the asset was archived successfully, false otherwise
     */
    @ActivateRequestContext
    public Boolean archive(final String siteName, String folderPath, final String assetName) {

        final AssetAPI assetAPI = this.clientFactory.getClient(AssetAPI.class);

        final var remoteAssetPath = buildRemoteAssetURL(siteName, folderPath, assetName);

        // Execute the REST call to archive the asset
        var response = assetAPI.archive(ByPathRequest.builder().assetPath(remoteAssetPath).build());
        return response.entity();
    }

    /**
     * Creates a folder in the remote server.
     *
     * @param siteName   the name of the site
     * @param folderPath the folder path
     * @return the list of created folders
     */
    @ActivateRequestContext
    public List<Map<String, Object>> createFolder(String siteName, String folderPath) {

        final FolderAPI folderAPI = this.clientFactory.getClient(FolderAPI.class);

        // Execute the REST call to create the folder
        final ResponseEntityView<List<Map<String, Object>>> response = folderAPI.makeFolders(
                List.of(folderPath),
                siteName);

        return response.entity();
    }

    /**
     * Removes a folder from the remote server.
     *
     * @param siteName   the name of the site
     * @param folderPath the folder path
     * @return true if the folder was deleted successfully, false otherwise
     */
    @ActivateRequestContext
    public boolean deleteFolder(String siteName, String folderPath) {

        final AssetAPI assetAPI = this.clientFactory.getClient(AssetAPI.class);

        final var remoteFolderPath = buildRemoteURL(siteName, folderPath);

        // Execute the REST call to delete the folder
        var response = assetAPI.deleteFolder(ByPathRequest.builder().assetPath(remoteFolderPath).build());
        return response.entity();
    }

    /**
     * Creates a new site in the remote server
     *
     * @param siteName the name of the site
     * @param status   the current status to handle
     * @return the SiteView of the created site
     */
    @ActivateRequestContext
    public SiteView pushSite(final String siteName, final String status) {

        final var siteAPI = this.clientFactory.getClient(SiteAPI.class);

        var live = statusToBoolean(status);
        var newSiteRequest = CreateUpdateSiteRequest.builder().siteName(siteName).build();

        // Execute the REST call to push the site
        var response = siteAPI.create(newSiteRequest);

        // Publish the site if we are in the live folder
        if (live) {
            response = siteAPI.publish(response.entity().identifier());
        }

        return response.entity();
    }

    /**
     * Pushes an asset to the remote server.
     *
     * @param workspace  the workspace path
     * @param status     the status of the asset
     * @param language   the language of the asset
     * @param siteName   the name of the site
     * @param folderPath the folder path
     * @param assetName  the name of the asset
     * @return the AssetView of the pushed asset
     */
    @ActivateRequestContext
    public AssetView push(final String workspace, final String status, final String language,
                          final String siteName, String folderPath, final String assetName) {

        final AssetAPI assetAPI = this.clientFactory.getClient(AssetAPI.class);

        // Building the remote asset path
        final var remoteAssetPath = buildRemoteAssetURL(siteName, folderPath, assetName);

        // Calculating the local asset path
        var localAssetPath = localPathFromAssetData(workspace, status, language, siteName,
                folderPath, assetName);

        // Reading the file and preparing the data to be pushed
        try (InputStream inputStream = Files.newInputStream(localAssetPath)) {

            var uploadForm = new FileUploadData();
            uploadForm.setAssetPath(remoteAssetPath);
            uploadForm.setDetail(new FileUploadDetail(
                    remoteAssetPath,
                    language,
                    statusToBoolean(status)
            ));
            uploadForm.setFile(inputStream);

            // Pushing the file
            var response = assetAPI.push(uploadForm);
            return response.entity();
        } catch (IOException e) {
            logger.error(String.format("Error pushing asset %s", localAssetPath), e);
            throw new IllegalStateException(e);
        }
    }

}
