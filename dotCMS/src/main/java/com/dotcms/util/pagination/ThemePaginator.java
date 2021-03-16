package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Theme;
import com.dotmarketing.business.ThemeAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handle theme pagination
 */
public class ThemePaginator implements Paginator<Map<String, Object>> {

    public static final String ID_PARAMETER = "id";
    public static final String HOST_ID_PARAMETER_NAME = "host";
    public static final String SEARCH_PARAMETER = "search-parameter";
    private ThemeAPI themeAPI;

    public ThemePaginator() {
        this(APILocator.getThemeAPI());
    }

    @VisibleForTesting
    ThemePaginator(final ThemeAPI themeAPI) {
        this.themeAPI = themeAPI;
    }

    @Override
    public PaginatedArrayList<Map<String, Object>> getItems(final User user, final int limit,
            final int offset,
            final Map<String, Object> params) {

        final PaginatedArrayList<Map<String, Object>> themesFound = new PaginatedArrayList();

        final OrderDirection direction =
                params != null && params.get(ORDER_DIRECTION_PARAM_NAME) != null ?
                        (OrderDirection) params.get(ORDER_DIRECTION_PARAM_NAME) :
                        OrderDirection.ASC;

        final String hostId = params != null ? (String) params.get(HOST_ID_PARAMETER_NAME) : null;
        final String searchParams = params != null ? (String) params.get(SEARCH_PARAMETER) : null;
        final String searchById = params != null ? (String) params.get(ID_PARAMETER) : null;

        try {
            final PaginatedArrayList<Theme> themes =
                    (PaginatedArrayList<Theme>) this.themeAPI
                            .findThemes(searchById, user, limit, offset,
                                    hostId, direction, searchParams, false);

            themesFound.addAll(themes.stream().map(Theme::getMap).collect(Collectors.toList()));
            themesFound.setQuery(themes.getQuery());
            themesFound.setTotalResults(this.themeAPI.findThemes(searchById, user, -1, 0,
                    hostId, direction, searchParams, false).size());

            return themesFound;
        } catch (DotSecurityException | DotDataException e) {

            Logger.error(this, e.getMessage(), e);
            throw new PaginationException(e);
        }
    }


}