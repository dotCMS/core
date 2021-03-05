package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Theme;
import com.dotmarketing.business.ThemeAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;

import java.util.Map;

/**
 * Handle theme pagination
 */
public class ThemePaginator implements Paginator<Map<String, Object>> {

    public static final String ID_PARAMETER = "id";
    public static final String HOST_ID_PARAMETER_NAME = "host";
    public static final String SEARCH_PARAMETER = "search-parameter";

    @VisibleForTesting
    static final String BASE_LUCENE_QUERY = "+parentpath:/application/themes/* +title:template.vtl ";

    private ContentletAPI contentletAPI;
    private ThemeAPI themeAPI;
    private FolderAPI folderAPI;

    public ThemePaginator() {
        this(APILocator.getContentletAPI(), APILocator.getFolderAPI(), APILocator.getThemeAPI());
    }

    @VisibleForTesting
    ThemePaginator(final ContentletAPI contentletAPI, final FolderAPI folderAPI, final ThemeAPI themeAPI) {
        this.contentletAPI = contentletAPI;
        this.folderAPI     = folderAPI;
        this.themeAPI = themeAPI;
    }

    @Override
    public PaginatedArrayList<Map<String, Object>> getItems(final User user, final int limit, final int offset,
            final Map<String, Object> params) throws PaginationException {

        final PaginatedArrayList<Map<String, Object>> result = new PaginatedArrayList();

        final OrderDirection direction =  params != null && params.get(ORDER_DIRECTION_PARAM_NAME) != null ?
                (OrderDirection) params.get(ORDER_DIRECTION_PARAM_NAME) :
                OrderDirection.ASC;

        final String hostId       = params != null ? (String) params.get(HOST_ID_PARAMETER_NAME) : null;
        final String searchParams = params != null ? (String) params.get(SEARCH_PARAMETER) : null;
        final String searchById   = params != null ? (String) params.get(ID_PARAMETER) : null;

        this.themeAPI.findThemes(searchById, user, -1, 0, hostId, direction, searchParams, false)
                .stream().map(Theme::getMap).forEach(result::add);

        return result;
    }


}