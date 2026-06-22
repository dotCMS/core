package com.dotcms.util.pagination;

import com.dotcms.rest.api.v1.folder.FolderSearchResultView;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.business.FolderSearchParams;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.util.Map;

/**
 * {@link PaginatorOrdered} implementation for the unified folder search endpoint.
 * Delegates to {@link FolderAPI#searchFolders} supporting optional name filtering,
 * path scoping, and recursive depth control.
 *
 * <p>Extra params expected in the map (keyed by the query param names defined in
 * the calling resource): {@code "siteId"}, {@code "path"}, {@code "recursive"}.
 */
public class FolderSearchPaginator implements PaginatorOrdered<FolderSearchResultView> {

    private static final String DEFAULT_ORDER_BY_COLUMN = "folder.name";

    private final FolderAPI folderAPI;

    public FolderSearchPaginator() {
        this(APILocator.getFolderAPI());
    }

    @VisibleForTesting
    public FolderSearchPaginator(final FolderAPI folderAPI) {
        this.folderAPI = folderAPI;
    }

    @Override
    public PaginatedArrayList<FolderSearchResultView> getItems(final User user, final String filter,
            final int limit, final int offset, final String orderBy,
            final OrderDirection direction, final Map<String, Object> extraParams)
            throws PaginationException {

        final var ep = extraParams != null ? extraParams : Map.of();
        final String siteId   = (String) ep.get("siteId");
        final String path     = (String) ep.getOrDefault("path", "/");
        final boolean recursive = Boolean.TRUE.equals(ep.get("recursive"));

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
                    .build();
            return folderAPI.searchFolders(params);
        } catch (final Exception e) {
            throw new PaginationException(e);
        }
    }
}
