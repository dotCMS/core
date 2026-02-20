package com.dotcms.graphql.datafetcher;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.graphql.exception.CustomGraphQLException;
import com.dotcms.graphql.exception.PermissionDeniedGraphQLException;
import com.dotcms.graphql.exception.ResourceNotFoundException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

/**
 * DataFetcher for the {@code DotFolderByPath} GraphQL query.
 * <p>
 * Returns the single folder at the given path. Sub-folders are available
 * through the recursive {@code children} field.
 * <p>
 * <b>Performance note:</b> Children are eagerly loaded up to a configurable max depth
 * ({@code GRAPHQL_FOLDER_COLLECTION_MAX_DEPTH}, default 3). For folder trees with many
 * children at each level, this can result in a large number of database queries.
 * <p>
 * Example query:
 * <pre>
 * {
 *   DotFolderByPath(path: "/application/") {
 *     folderTitle
 *     folderPath
 *     children {
 *       folderTitle
 *       folderPath
 *     }
 *   }
 * }
 * </pre>
 */
public class FolderCollectionDataFetcher implements DataFetcher<Map<String, Object>> {

    static final int DEFAULT_MAX_DEPTH = 3;

    @Override
    public Map<String, Object> get(final DataFetchingEnvironment environment)
            throws Exception {

        final DotGraphQLContext context = environment.getContext();
        final User user = context.getUser();
        final HttpServletRequest request = context.getHttpServletRequest();

        final String path = environment.getArgument("path");

        if (!UtilMethods.isSet(path)) {
            throw new CustomGraphQLException("The 'path' argument is required");
        }

        final String site = environment.getArgument("site");
        if (UtilMethods.isSet(site)) {
            request.setAttribute(Host.HOST_VELOCITY_VAR_NAME, site);
        }

        final Host host = WebAPILocator.getHostWebAPI().getHost(request);

        if (host == null) {
            throw new ResourceNotFoundException("Host",
                    UtilMethods.isSet(site) ? site : "default");
        }

        final Folder folder = APILocator.getFolderAPI()
                .findFolderByPath(path, host, user, true);

        if (folder == null || !UtilMethods.isSet(folder.getInode())) {
            throw new ResourceNotFoundException("Folder", path);
        }

        final int maxDepth = Config.getIntProperty(
                "GRAPHQL_FOLDER_COLLECTION_MAX_DEPTH", DEFAULT_MAX_DEPTH);

        return buildFolderMap(folder, user, 1, maxDepth);
    }

    /**
     * Builds a folder map including recursive children.
     * Children are eagerly loaded so GraphQL can resolve nested children queries.
     * The depth parameter guards against excessively deep folder trees.
     *
     * @param folder   the folder to transform
     * @param user     the current user for permission checks
     * @param depth    the current recursion depth
     * @param maxDepth the maximum allowed recursion depth
     * @return a map representing the folder with its children
     * @throws PermissionDeniedGraphQLException if the user lacks permission to access children
     * @throws DotRuntimeException if a data error occurs while loading children
     */
    Map<String, Object> buildFolderMap(final Folder folder, final User user,
            final int depth, final int maxDepth) {

        final Map<String, Object> map = new HashMap<>();
        map.put("folderId", folder.getInode());
        map.put("folderFileMask", folder.getFilesMasks());
        map.put("folderSortOrder", folder.getSortOrder());
        map.put("folderName", folder.getName());
        map.put("folderPath", folder.getPath());
        map.put("folderTitle", folder.getTitle());
        map.put("folderDefaultFileType", folder.getDefaultFileType());

        if (depth >= maxDepth) {
            Logger.debug(this, () -> "Max folder depth (" + maxDepth
                    + ") reached for folder: " + folder.getPath());
            map.put("children", Collections.emptyList());
            return map;
        }

        try {
            final List<Folder> childFolders = APILocator.getFolderAPI()
                    .findSubFolders(folder, user, true);
            final List<Map<String, Object>> childMaps = childFolders.stream()
                    .map(child -> buildFolderMap(child, user, depth + 1, maxDepth))
                    .collect(Collectors.toList());
            map.put("children", childMaps);
        } catch (DotSecurityException e) {
            Logger.error(this, "Permission denied loading children for folder: "
                    + folder.getPath(), e);
            throw new PermissionDeniedGraphQLException(
                    "Permission denied accessing children of folder: " + folder.getPath());
        } catch (DotDataException e) {
            Logger.error(this, "Data error loading children for folder: "
                    + folder.getPath(), e);
            throw new DotRuntimeException(
                    "Error loading children for folder: " + folder.getPath(), e);
        }

        return map;
    }
}
