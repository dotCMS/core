package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ThemeAPI;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;


import com.liferay.util.StringPool;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dotmarketing.business.ThemeAPI.THEME_THUMBNAIL_KEY;

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
        final StringBuilder query = new StringBuilder();

        query.append(BASE_LUCENE_QUERY);

        if (UtilMethods.isSet(searchById)){
            query.append("+conFolder").append(StringPool.COLON).append(searchById);
        }

        if(UtilMethods.isSet(hostId)) {
            query.append("+conhost").append(StringPool.COLON).append(hostId);
        }

        if (UtilMethods.isSet(searchParams)){
            query.append(" +catchall:*").append(searchParams).append("*");
        }

        final String sortBy = String.format("parentPath %s", direction.toString().toLowerCase());

        try {

            final List<ContentletSearch> totalResults =
                    contentletAPI.searchIndex(query.toString(), -1, 0, sortBy, user, false);

            final List<ContentletSearch> contentletSearches;

            if (limit !=-1 || offset!=0) {

                contentletSearches =
                        contentletAPI.searchIndex(query.toString(), limit, offset, sortBy, user, false);
                result.setTotalResults(totalResults.size());
            } else {

                contentletSearches = totalResults;
                result.add(this.themeAPI.systemTheme().getMap());
                result.setTotalResults(totalResults.size() + 1);
            }

            final List<String>  inodes = contentletSearches.stream()
                    .map(ContentletSearch::getInode).collect(Collectors.toList());


            for (final Contentlet contentlet : this.contentletAPI.findContentlets(inodes)) {

                final Folder folder = folderAPI.find(contentlet.getFolder(), user, false);
                final Map<String, Object> map = new HashMap<>(folder.getMap());
                map.put(THEME_THUMBNAIL_KEY, themeAPI.getThemeThumbnail(folder, user));
                result.add(map);
            }

            result.setQuery(query.toString());
            return result;
        } catch (DotSecurityException | DotDataException e) {

            Logger.error(this, e.getMessage(), e);
            throw new PaginationException(e);
        }
    }
}
