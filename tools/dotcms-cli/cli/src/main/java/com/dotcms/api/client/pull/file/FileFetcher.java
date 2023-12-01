package com.dotcms.api.client.pull.file;

import static com.dotcms.api.client.pull.file.OptionConstants.EXCLUDE_ASSET_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.EXCLUDE_FOLDER_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_ASSET_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_FOLDER_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.NON_RECURSIVE;
import static com.dotcms.common.LocationUtils.encodePath;

import com.dotcms.api.AssetAPI;
import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.files.traversal.RemoteTraversalService;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.pull.ContentFetcher;
import com.dotcms.common.LocationUtils;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.asset.AssetVersionsView;
import com.dotcms.model.asset.ByPathRequest;
import com.dotcms.model.site.Site;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

@Dependent
public class FileFetcher implements ContentFetcher<FileTraverseResult>, Serializable {

    private static final long serialVersionUID = -8200625720953134243L;

    @Inject
    RestClientFactory clientFactory;

    @Inject
    RemoteTraversalService remoteTraversalService;

    @ActivateRequestContext
    @Override
    public List<FileTraverseResult> fetch(Map<String, Object> customOptions) {

        // ---
        // First we need to fetch the all the existing sites

        final var siteAPI = clientFactory.getClient(SiteAPI.class);

        final int pageSize = 100;
        int page = 1;

        // Create a list to store all the retrieved sites
        List<Site> allSites = new ArrayList<>();

        while (true) {

            // Retrieve a page of sites
            ResponseEntityView<List<Site>> sitesResponse = siteAPI.getSites(
                    null,
                    null,
                    false,
                    false,
                    page,
                    pageSize
            );

            // Check if the response contains sites
            if (sitesResponse.entity() != null && !sitesResponse.entity().isEmpty()) {

                // Add the sites from the current page to the list
                allSites.addAll(sitesResponse.entity());

                // Increment the page number
                page++;
            } else {
                // Handle the case where the response doesn't contain sites or an error occurred
                break;
            }
        }

        // ---
        // And now for each site we need to fetch the tree under the specified site
        List<FileTraverseResult> results = new ArrayList<>();
        for (Site site : allSites) {
            String sitePath = String.format("//%s", site.hostName());
            results.add(fetch(sitePath, customOptions));
        }

        return results;
    }

    @ActivateRequestContext
    @Override
    public FileTraverseResult fetchByKey(String path, Map<String, Object> customOptions)
            throws NotFoundException {

        return fetch(path, customOptions);
    }

    /**
     * Retrieves file traversal result based on the provided path and custom options.
     *
     * @param path          The path to traverse.
     * @param customOptions The custom options for traversal.
     * @return The file traversal result.
     */
    private FileTraverseResult fetch(String path, Map<String, Object> customOptions) {

        if (LocationUtils.isFolderURL(path)) { // Handling folders

            Set<String> includeFolderPatterns = getSetFromOptions(customOptions,
                    INCLUDE_FOLDER_PATTERNS);
            Set<String> includeAssetPatterns = getSetFromOptions(customOptions,
                    INCLUDE_ASSET_PATTERNS);
            Set<String> excludeFolderPatterns = getSetFromOptions(customOptions,
                    EXCLUDE_FOLDER_PATTERNS);
            Set<String> excludeAssetPatterns = getSetFromOptions(customOptions,
                    EXCLUDE_ASSET_PATTERNS);

            boolean nonRecursive = false;
            if (customOptions != null) {
                nonRecursive = (boolean) customOptions.getOrDefault(NON_RECURSIVE, false);
            }

            // Service to handle the traversal of the folder
            var response = remoteTraversalService.traverseRemoteFolder(
                    path,
                    nonRecursive ? 0 : null,
                    true,
                    includeFolderPatterns,
                    includeAssetPatterns,
                    excludeFolderPatterns,
                    excludeAssetPatterns
            );

            return FileTraverseResult.builder().
                    tree(response.getRight()).
                    exceptions(response.getLeft()).
                    build();

        } else { // Handling single files

            var asset = retrieveAssetInformation(path);
            return FileTraverseResult.builder().asset(asset).build();
        }
    }

    /**
     * Retrieves the asset information
     *
     * @param source The asset path
     * @return The asset information
     */
    AssetVersionsView retrieveAssetInformation(final String source) {

        // Requesting the file info
        final AssetAPI assetAPI = this.clientFactory.getClient(AssetAPI.class);

        // Execute the REST call to retrieve asset information
        String encodedURL = encodePath(source);
        var response = assetAPI.assetByPath(ByPathRequest.builder().assetPath(encodedURL).build());
        return response.entity();
    }

    /**
     * Retrieves the set of values associated with the given key in the provided options map.
     *
     * @param options The map of options
     * @param key     The key to retrieve the values for
     * @return The set of values associated with the key, or null if the options map is null or does
     * not contain the key
     */
    private Set<String> getSetFromOptions(Map<String, Object> options, String key) {

        if (options != null && options.containsKey(key)) {
            return (Set<String>) options.get(key);
        }

        return Collections.emptySet();
    }

}