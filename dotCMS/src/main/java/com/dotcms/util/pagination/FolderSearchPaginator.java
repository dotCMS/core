package com.dotcms.util.pagination;

import com.dotcms.rest.api.v1.folder.FolderSearchView;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.business.FolderSearchParams;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.Map;
import org.apache.commons.lang3.BooleanUtils;

/**
 * {@link PaginatorOrdered} implementation for the unified folder search endpoint.
 * Delegates to {@link FolderAPI#searchFolders} supporting optional name filtering,
 * path scoping, and recursive depth control.
 *
 * <p>Extra params expected in the map (keyed by the query param names defined in
 * the calling resource): {@code "siteId"}, {@code "path"}, {@code "recursive"},
 * {@code "includePermissions"}.
 *
 * @param folderAPI the API the search is delegated to; injectable for testing
 */
public record FolderSearchPaginator(FolderAPI folderAPI)
        implements PaginatorOrdered<FolderSearchView> {

    private static final String DEFAULT_ORDER_BY_COLUMN = "folder.name";

    private static final String SITE_ID_PARAM = "siteId";
    private static final String PATH_PARAM = "path";
    private static final String RECURSIVE_PARAM = "recursive";
    private static final String INCLUDE_PERMISSIONS_PARAM = "includePermissions";

    /**
     * Production entry point — the canonical constructor taking a {@link FolderAPI} exists for
     * tests to inject a mock.
     */
    public FolderSearchPaginator() {
        this(APILocator.getFolderAPI());
    }

    @Override
    public PaginatedArrayList<FolderSearchView> getItems(final User user, final String filter,
            final int limit, final int offset, final String orderBy,
            final OrderDirection direction, final Map<String, Object> extraParams)
            throws PaginationException {

        final Map<String, Object> ep = extraParams != null ? extraParams : Map.of();
        final String siteId = (String) ep.get(SITE_ID_PARAM);
        final String path = (String) ep.getOrDefault(PATH_PARAM, "/");
        final boolean recursive = toBoolean(ep.get(RECURSIVE_PARAM));
        final boolean includePermissions = toBoolean(ep.get(INCLUDE_PERMISSIONS_PARAM));

        final String orderByColumn = switch (orderBy) {
            case "mod_date" -> "folder.mod_date";
            case null, default -> DEFAULT_ORDER_BY_COLUMN;
        };
        final String orderDirection = direction == OrderDirection.DESC ? "DESC" : "ASC";

        try {
            final FolderSearchParams params = FolderSearchParams.builder()
                    .name(UtilMethods.isSet(filter) ? filter : null)
                    .path(path)
                    .recursive(recursive)
                    .siteId(siteId)
                    .user(user)
                    .limit(limit)
                    .offset(offset)
                    .orderBy(orderByColumn)
                    .orderDirection(orderDirection)
                    .includePermissions(includePermissions)
                    .build();
            return folderAPI.searchFolders(params);
        } catch (final Exception e) {
            throw new PaginationException(e);
        }
    }

    /**
     * Reads a boolean out of the untyped extra-params map.
     *
     * <p>The resource puts real {@link Boolean} values in, but the map is {@code Map<String, Object>}
     * and other callers may hand over the raw query string, so string forms are accepted too —
     * {@code BooleanUtils.toBoolean(String)} covers "true"/"yes"/"on"/"1" and their variants.
     * Anything unrecognised, including {@code null}, reads as {@code false}.
     */
    private static boolean toBoolean(final Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value != null && BooleanUtils.toBoolean(String.valueOf(value));
    }
}
