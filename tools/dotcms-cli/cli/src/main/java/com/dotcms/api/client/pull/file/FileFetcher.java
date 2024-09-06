package com.dotcms.api.client.pull.file;

import static com.dotcms.api.client.pull.file.OptionConstants.EXCLUDE_ASSET_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.EXCLUDE_FOLDER_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_ASSET_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_FOLDER_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.NON_RECURSIVE;
import static com.dotcms.common.LocationUtils.encodePath;

import com.dotcms.api.AssetAPI;
import com.dotcms.api.client.files.traversal.RemoteTraversalService;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.pull.ContentFetcher;
import com.dotcms.api.client.pull.exception.PullException;
import com.dotcms.api.client.util.SiteIterator;
import com.dotcms.common.LocationUtils;
import com.dotcms.model.asset.AssetVersionsView;
import com.dotcms.model.asset.ByPathRequest;
import com.dotcms.model.site.Site;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

@Dependent
public class FileFetcher implements ContentFetcher<FileTraverseResult>, Serializable {

    private static final long serialVersionUID = -8200625720953134243L;

    @Inject
    RestClientFactory clientFactory;

    @Inject
    RemoteTraversalService remoteTraversalService;

    @Inject
    Logger logger;

    @ActivateRequestContext
    @Override
    public List<FileTraverseResult> fetch(final boolean failFast,
            final Map<String, Object> customOptions) {

        // ---
        // Fetching the all the existing sites
        final List<Site> allSites = new ArrayList<>();

        final SiteIterator siteIterator = new SiteIterator(clientFactory, 100);
        while (siteIterator.hasNext()) {
            List<Site> sites = siteIterator.next();
            allSites.addAll(sites);
        }

        // ---
        // And now for each site we need to fetch the tree under the specified site
        final List<FileTraverseResult> results = new ArrayList<>();
        for (final Site site : allSites) {
            final String sitePath = String.format("//%s", site.hostName());
            results.add(fetch(sitePath, failFast, customOptions));
        }

        return results;
    }

    @ActivateRequestContext
    @Override
    public FileTraverseResult fetchByKey(final String path, final boolean failFast,
            final Map<String, Object> customOptions)
            throws NotFoundException {

        return fetch(path, failFast, customOptions);
    }

    /**
     * Retrieves file traversal result based on the provided path and custom options.
     *
     * @param path          The path to traverse.
     * @param customOptions The custom options for traversal.
     * @return The file traversal result.
     */
    private FileTraverseResult fetch(final String path, final boolean failFast,
            final Map<String, Object> customOptions) {

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
                    failFast, //We need to be able to instruct the service to fail fast.
                    includeFolderPatterns,
                    includeAssetPatterns,
                    excludeFolderPatterns,
                    excludeAssetPatterns
            );

            return FileTraverseResult.builder().
                    tree(response.treeNode()).
                    exceptions(response.exceptions()).
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

        try {
            // Requesting the file info
            final AssetAPI assetAPI = this.clientFactory.getClient(AssetAPI.class);

            // Execute the REST call to retrieve asset information
            String encodedURL = encodePath(source);
            var response = assetAPI.assetByPath(
                    ByPathRequest.builder().assetPath(encodedURL).build()
            );
            return response.entity();
        } catch (Exception e) {
            var message = String.format(
                    "Error pulling content [%s]",
                    source
            );

            logger.error(message, e);
            throw new PullException(message, e);
        }
    }

    /**
     * Retrieves the set of values associated with the given key in the provided options map.
     *
     * @param options The map of options
     * @param key     The key to retrieve the values for
     * @return The set of values associated with the key, or null if the options map is null or does
     * not contain the key
     */
    @SuppressWarnings("unchecked")
    private Set<String> getSetFromOptions(final Map<String, Object> options, final String key) {

        return Optional.ofNullable(options)
                .map(map -> (Set<String>) map.getOrDefault(key, Collections.emptySet()))
                .orElse(Collections.emptySet());
    }

}