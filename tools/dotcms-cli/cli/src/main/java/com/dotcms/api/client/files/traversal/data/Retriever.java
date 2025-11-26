package com.dotcms.api.client.files.traversal.data;

import static com.dotcms.common.AssetsUtils.buildRemoteAssetURL;
import static com.dotcms.common.AssetsUtils.buildRemoteURL;

import com.dotcms.api.AssetAPI;
import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.model.asset.AssetVersionsView;
import com.dotcms.model.asset.ByPathRequest;
import com.dotcms.model.asset.FolderView;
import com.dotcms.model.language.Language;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

/**
 * Utility class for retrieving folder and asset information from the remote server using REST calls.
 */
@ApplicationScoped
public class Retriever {

    @Inject
    protected RestClientFactory clientFactory;

    /**
     * Retrieves a language from the remote server.
     *
     * @param language the language to retrieve
     * @return the retrieved language
     */
    @ActivateRequestContext
    public Language retrieveLanguage(String language) {

        final LanguageAPI languageAPI = this.clientFactory.getClient(LanguageAPI.class);

        // Execute the REST call to retrieve the language from the language code
        var response = languageAPI.getFromLanguageIsoCode(language);
        return response.entity();
    }

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

        final var remoteFolderPath = buildRemoteURL(siteName, folderPath);

        // Execute the REST call to retrieve folder contents
        var response = assetAPI.folderByPath(ByPathRequest.builder().assetPath(remoteFolderPath).build());
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

        final var remoteAssetPath = buildRemoteAssetURL(siteName, folderPath, assetName);

        // Execute the REST call to retrieve asset information
        var response = assetAPI.assetByPath(ByPathRequest.builder().assetPath(remoteAssetPath).build());
        return response.entity();
    }

    /**
     * Retrieves the contents of a folder
     *
     * @param siteName            the name of the site containing the folder
     * @param folderPath          the folder path
     * @param level               the hierarchical level of the folder
     * @param implicitGlobInclude This property represents whether a folder should be implicitly included based on the
     *                            absence of any include patterns. When implicitGlobInclude is set to true, it means
     *                            that there are no include patterns specified, so all folders should be included by
     *                            default. In other words, if there are no specific include patterns defined, the
     *                            filter assumes that all folders should be included unless explicitly excluded.
     * @param explicitGlobInclude This property represents whether a folder should be explicitly included based on the
     *                            configured includes patterns for folders. When explicitGlobInclude is set to true,
     *                            it means that the folder has matched at least one of the include patterns and should
     *                            be included in the filtered result. The explicit inclusion takes precedence over other
     *                            rules. If a folder is explicitly included, it will be included regardless of any other
     *                            rules or patterns.
     * @param explicitGlobExclude This property represents whether a folder should be explicitly excluded based on the
     *                            configured excludes patterns for folders. When explicitGlobExclude is set to true, it
     *                            means that the folder has matched at least one of the exclude patterns and should be
     *                            excluded from the filtered result. The explicit exclusion takes precedence over other
     *                            rules. If a folder is explicitly excluded, it will be excluded regardless of any other
     *                            rules or patterns.
     * @return an {@code FolderView} object containing the metadata for the requested folder
     */
    @ActivateRequestContext
    public FolderView retrieveFolderInformation(String siteName, String folderPath,
                                                final int level, final boolean implicitGlobInclude,
                                                final boolean explicitGlobInclude, final boolean explicitGlobExclude) {

        final var remoteFolderPath = buildRemoteURL(siteName, folderPath);

        // Execute the REST call to retrieve folder contents
        final AssetAPI assetAPI = this.clientFactory.getClient(AssetAPI.class);
        var response = assetAPI.folderByPath(ByPathRequest.builder().assetPath(remoteFolderPath).build());

        var foundFolder = response.entity();
        foundFolder = foundFolder.withLevel(level);
        foundFolder = foundFolder.withImplicitGlobInclude(implicitGlobInclude);
        foundFolder = foundFolder.withExplicitGlobExclude(explicitGlobExclude);
        foundFolder = foundFolder.withExplicitGlobInclude(explicitGlobInclude);

        if (foundFolder.subFolders() != null && !foundFolder.subFolders().isEmpty()) {

            List<FolderView> subFolders = new ArrayList<>();
            for (var child : foundFolder.subFolders()) {
                subFolders.add(child.withLevel(level + 1));
            }

            foundFolder = foundFolder.withSubFolders(ImmutableList.copyOf(subFolders));
        }

        return foundFolder;
    }

}

