package com.dotcms.graphql.datafetcher;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.folders.model.Folder;
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
 * DataFetcher for the {@code DotFolder} GraphQL query.
 * <p>
 * Returns the single folder at the given path. Sub-folders are available
 * through the recursive {@code children} field.
 * <p>
 * Example query:
 * <pre>
 * {
 *   DotFolder(path: "/application/") {
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

    static final int DEFAULT_MAX_DEPTH = 5;

    @Override
    public Map<String, Object> get(final DataFetchingEnvironment environment)
            throws Exception {

        final DotGraphQLContext context = environment.getContext();
        final User user = context.getUser();
        final HttpServletRequest request = context.getHttpServletRequest();

        final String path = environment.getArgument("path");

        if (!UtilMethods.isSet(path)) {
            return null;
        }

        final Host host = WebAPILocator.getHostWebAPI().getHost(request);

        final Folder folder = APILocator.getFolderAPI()
                .findFolderByPath(path, host, user, true);

        if (folder == null || !UtilMethods.isSet(folder.getInode())) {
            return null;
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
        } catch (Exception e) {
            Logger.error(this, "Error loading children for folder: "
                    + folder.getPath(), e);
            map.put("children", Collections.emptyList());
        }

        return map;
    }
}
