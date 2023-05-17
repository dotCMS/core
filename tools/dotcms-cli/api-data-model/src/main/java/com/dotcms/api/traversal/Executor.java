package com.dotcms.api.traversal;

import com.dotcms.api.AssetAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.model.asset.FolderView;
import com.dotcms.model.asset.SearchByPathRequest;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

/**
 * An application-scoped bean that provides a method to retrieve folder contents via REST API.
 */
@ApplicationScoped
public class Executor {

    @Inject
    protected RestClientFactory clientFactory;

    /**
     * Retrieves the contents of a folder
     *
     * @param siteName         the name of the site containing the folder
     * @param parentFolderName the name of the parent folder containing the folder
     * @param folderName       the name of the folder to retrieve metadata for
     * @param level            the hierarchical level of the folder
     * @param include          if according to the provided glob patterns the folder should be
     *                         included
     * @return an {@code FolderView} object containing the metadata for the requested folder
     */
    @ActivateRequestContext
    public FolderView restCall(String siteName, String parentFolderName,
            String folderName, final int level, final boolean include) {

        // Determine if the parent folder and folder names are empty or null
        var emptyParent = parentFolderName == null
                || parentFolderName.isEmpty()
                || parentFolderName.equals("/");

        var emptyFolder = folderName == null
                || folderName.isEmpty()
                || folderName.equals("/");

        // Remove firsts and last slash from folder name
        if (!emptyParent) {
            parentFolderName = parentFolderName.replaceAll("^/", "");
            parentFolderName = parentFolderName.replaceAll("/$", "");
        }

        // Build the folder path based on the input parameters
        final String folderPath;
        if (emptyParent && !emptyFolder) {
            folderPath = String.format("//%s/%s", siteName, folderName);
        } else if (emptyParent) {
            folderPath = String.format("//%s/", siteName);
        } else {
            folderPath = String.format("//%s/%s/%s", siteName, parentFolderName, folderName);
        }

        final AssetAPI assetAPI = this.clientFactory.getClient(AssetAPI.class);

        // Execute the REST call to retrieve folder contents
        var response = assetAPI.byPath(SearchByPathRequest.builder().assetPath(folderPath).build());

        var foundFolder = response.entity();
        foundFolder = foundFolder.withLevel(level);
        foundFolder = foundFolder.withInclude(include);

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
