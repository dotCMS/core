package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;


import com.liferay.util.StringPool;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handle theme pagination
 */
public class ThemePaginator implements Paginator<Folder> {

    public static final String ID_PARAMETER = "id";
    public static final String HOST_ID_PARAMETER_NAME = "host";
    public static final String SEARCH_PARAMETER = "search-parameter";


    private static final String BASE_LUCENE_QUERY = "+parentpath:/application/themes/* +title:template.vtl ";

    @VisibleForTesting
    static final String THEME_PNG = "theme.png";

    private ContentletAPI contentletAPI;

    private FolderAPI folderAPI;

    public ThemePaginator() {
        this(APILocator.getContentletAPI(), APILocator.getFolderAPI());
    }

    @VisibleForTesting
    ThemePaginator(final ContentletAPI contentletAPI, FolderAPI folderAPI) {
        this.contentletAPI = contentletAPI;
        this.folderAPI     = folderAPI;
    }

    @Override
    public PaginatedArrayList<Folder> getItems(final User user, final int limit, final int offset,
                                                   final Map<String, Object> params) throws PaginationException {

        PaginatedArrayList<Folder> result = new PaginatedArrayList();

        final OrderDirection direction =  params != null && params.get(ORDER_DIRECTION_PARAM_NAME) != null ?
                (OrderDirection) params.get(ORDER_DIRECTION_PARAM_NAME) :
                OrderDirection.ASC;

        final String hostId       = params != null ? (String) params.get(HOST_ID_PARAMETER_NAME) : null;
        final String searchParams = params != null ? (String) params.get(SEARCH_PARAMETER) : null;
        final String searchById   = params != null ? (String) params.get(ID_PARAMETER) : null;
        StringBuilder query       = new StringBuilder();

        query.append(BASE_LUCENE_QUERY);

        if (UtilMethods.isSet(searchById)){
            query.append("+conFolder");
            query.append(StringPool.COLON);
            query.append(searchById);
        }else if(UtilMethods.isSet(hostId)) {
            query.append("+conhost");
            query.append(StringPool.COLON);
            query.append(hostId);
        }

        final String sortBy = String.format("parentPath %s", direction.toString().toLowerCase());

        try {

            final List<ContentletSearch> contentletSearches = (PaginatedArrayList<ContentletSearch>)
                    contentletAPI.searchIndex(query.toString(), limit, offset, sortBy, user, false);

            final List<String>  inodes = contentletSearches.stream()
                    .map(contentletSearch -> contentletSearch.getInode())
                    .collect(Collectors.toList());

            for (Contentlet contentlet :contentletAPI.findContentlets(inodes)) {
                Folder folder = folderAPI.find(contentlet.getFolder(), user, false);
                folder.setIdentifier(getThemeIdentifier(folder, user));
                result.add(folder);
            }

            if(UtilMethods.isSet(searchParams)){
                Iterator<Folder> folderIterator = result.iterator();
                while(folderIterator.hasNext()){
                    Folder folder = folderIterator.next();
                    if(!folder.getName().contains(searchParams) || !folder.getTitle().contains(searchParams)){
                        folderIterator.remove();
                    }
                }
            }

            result.setTotalResults(result.size());
            result.setQuery(query.toString());
            return result;
        } catch (DotSecurityException | DotDataException e) {
            throw new PaginationException(e);
        }
    }

    @VisibleForTesting
    String getThemeIdentifier(Folder folder, User user) throws DotSecurityException, DotDataException {

        StringBuilder query = new StringBuilder();
        query.append("+conFolder:");
        query.append(folder.getInode());
        query.append(" +title:");
        query.append(THEME_PNG);
        List<Contentlet> results = contentletAPI
                .search(query.toString(), -1, 0, null, user, false);

        return UtilMethods.isSet(results) ? results.get(0).getIdentifier() : null;

    }
}
