package com.dotcms.api.traversal;

import com.dotcms.api.AssetAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.model.asset.FolderView;
import com.dotcms.model.asset.SearchByPathRequest;
import com.google.common.collect.ImmutableList;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.dotcms.common.AssetsUtils.BuildRemoteURL;

/**
 * An application-scoped bean that provides a method to retrieve folder contents via REST API.
 */
@ApplicationScoped
public class Retriever {

    @Inject
    protected RestClientFactory clientFactory;

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
    public FolderView retrieveFolderContents(String siteName, String folderPath,
                                             final int level, final boolean implicitGlobInclude,
                                             final boolean explicitGlobInclude, final boolean explicitGlobExclude) {

        final var remoteFolderPath = BuildRemoteURL(siteName, folderPath);

        // Execute the REST call to retrieve folder contents
        final AssetAPI assetAPI = this.clientFactory.getClient(AssetAPI.class);
        var response = assetAPI.folderByPath(SearchByPathRequest.builder().assetPath(remoteFolderPath).build());

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

            // Ordering foundFolder by name
            List<FolderView> sortedFolders = subFolders.stream()
                    .sorted(Comparator.comparing(FolderView::name))
                    .collect(Collectors.toList());

            foundFolder = foundFolder.withSubFolders(ImmutableList.copyOf(sortedFolders));
        }

        return foundFolder;
    }
}
