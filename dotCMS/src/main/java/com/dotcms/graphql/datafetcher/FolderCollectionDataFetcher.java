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
import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

/**
 * DataFetcher for the {@code DotFolderByPath} GraphQL query.
 * <p>
 * Returns the single folder at the given path. Sub-folders are available
 * through the recursive {@code children} field.
 * <p>
 * <b>Performance note:</b> Children are eagerly loaded up to the minimum of the
 * query-requested depth and a configurable max depth
 * ({@code GRAPHQL_FOLDER_COLLECTION_MAX_DEPTH}, default 3). For folder trees with many
 * children at each level, this can result in a large number of database queries.
 * <p>
 * <b>Permission handling:</b> Child folders that the user cannot access are excluded from
 * the result, but permission errors are surfaced in the GraphQL {@code errors} array
 * only at levels the query actually requests.
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
public class FolderCollectionDataFetcher implements DataFetcher<DataFetcherResult<Map<String, Object>>> {

    static final int DEFAULT_MAX_DEPTH = 3;

    @Override
    public DataFetcherResult<Map<String, Object>> get(final DataFetchingEnvironment environment)
            throws Exception {

        final DotGraphQLContext context = environment.getContext();
        final User user = context.getUser();
        final HttpServletRequest request = context.getHttpServletRequest();

        final String path = environment.getArgument("path");

        if (!UtilMethods.isSet(path)) {
            throw new CustomGraphQLException("The 'path' argument is required");
        }

        final String site = environment.getArgument("site");

        final Host host;
        if (UtilMethods.isSet(site)) {
            // Validate the site explicitly instead of relying on host resolution
            // which silently falls back to the default site
            final Optional<Host> siteOpt = APILocator.getHostAPI()
                    .findByIdOrKey(site, user, true);
            if (siteOpt.isEmpty()) {
                throw new ResourceNotFoundException(
                        "Site '" + site + "' not found", "Site", site);
            }
            host = siteOpt.get();
        } else {
            host = WebAPILocator.getHostWebAPI().getHost(request);
            if (host == null) {
                throw new ResourceNotFoundException(
                        "Could not determine site from request",
                        "Site", "default");
            }
        }

        final Folder folder;
        try {
            folder = APILocator.getFolderAPI()
                    .findFolderByPath(path, host, user, true);
        } catch (DotSecurityException e) {
            throw new PermissionDeniedGraphQLException(
                    "You do not have permission to access folder '" + path
                            + "' on site '" + host.getHostname() + "'");
        }

        if (folder == null || !UtilMethods.isSet(folder.getInode())) {
            throw new ResourceNotFoundException(
                    "Folder not found at path '" + path + "' on site '"
                            + host.getHostname() + "'",
                    "Folder",
                    path);
        }

        final int configMaxDepth = Config.getIntProperty(
                "GRAPHQL_FOLDER_COLLECTION_MAX_DEPTH", DEFAULT_MAX_DEPTH);

        // Only load as deep as the query actually requests children
        final int requestedDepth = computeRequestedDepth(environment.getField());
        final int maxDepth = Math.min(requestedDepth, configMaxDepth);

        final List<GraphQLError> errors = new ArrayList<>();
        final Map<String, Object> result = buildFolderMap(folder, user, 1, maxDepth, errors);

        return DataFetcherResult.<Map<String, Object>>newResult()
                .data(result)
                .errors(errors)
                .build();
    }

    /**
     * Computes how many levels deep the query requests the {@code children} field.
     * <p>
     * For example, {@code { folderTitle }} returns 1 (no children requested).
     * {@code { folderTitle children { folderTitle } }} returns 2.
     * {@code { children { children { folderTitle } } }} returns 3.
     *
     * @param field the root field from the query
     * @return the depth of nested children requests (minimum 1)
     */
    static int computeRequestedDepth(final Field field) {

        final SelectionSet selectionSet = field.getSelectionSet();
        if (selectionSet == null) {
            return 1;
        }

        return selectionSet.getSelections().stream()
                .filter(Field.class::isInstance)
                .map(Field.class::cast)
                .filter(f -> "children".equals(f.getName()))
                .findFirst()
                .map(childField -> 1 + computeRequestedDepth(childField))
                .orElse(1);
    }

    /**
     * Builds a folder map including recursive children.
     * Children are eagerly loaded so GraphQL can resolve nested children queries.
     * The depth parameter guards against excessively deep folder trees.
     * <p>
     * Child folders are discovered using the system user so that permission-denied
     * folders are surfaced as errors rather than silently filtered out.
     * Permission errors are only reported at levels the query actually requests.
     *
     * @param folder   the folder to transform
     * @param user     the current user for permission checks
     * @param depth    the current recursion depth
     * @param maxDepth the maximum allowed recursion depth
     * @param errors   collects permission errors for child folders the user cannot access
     * @return a map representing the folder with its children
     * @throws PermissionDeniedGraphQLException if the user lacks permission to access children
     * @throws DotRuntimeException if a data error occurs while loading children
     */
    Map<String, Object> buildFolderMap(final Folder folder, final User user,
            final int depth, final int maxDepth, final List<GraphQLError> errors) {

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
            // Get children the user can access (permission-filtered by the API)
            final List<Folder> permittedChildren = APILocator.getFolderAPI()
                    .findSubFolders(folder, user, true);
            final List<Map<String, Object>> childMaps = new ArrayList<>();
            for (final Folder child : permittedChildren) {
                childMaps.add(buildFolderMap(child, user, depth + 1, maxDepth, errors));
            }
            map.put("children", childMaps);

            // Discover ALL children using system user to find which ones were
            // denied, and surface those as errors instead of silently hiding them
            final User systemUser = APILocator.getUserAPI().getSystemUser();
            final List<Folder> allChildren = APILocator.getFolderAPI()
                    .findSubFolders(folder, systemUser, false);

            final Set<String> permittedInodes = permittedChildren.stream()
                    .map(Folder::getInode)
                    .collect(Collectors.toSet());

            final long deniedCount = allChildren.stream()
                    .filter(child -> !permittedInodes.contains(child.getInode()))
                    .count();

            if (deniedCount > 0) {
                errors.add(new PermissionDeniedGraphQLException(
                        "You do not have permission to access " + deniedCount
                                + " subfolder(s) under this path"));
            }
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
