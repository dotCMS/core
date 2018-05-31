package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import static com.dotcms.util.CollectionsUtils.map;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handle theme pagination
 */
public class ThemePaginator implements Paginator<Contentlet> {

    public final static String HOST_ID_PARAMETER_NAME = "host-id";
    public final static String SEARCH_PARAMETER = "search-parameter";

    private static final String LUCENE_QUERY_WITH_HOST = "+parentpath:/application/themes/* +title:template.vtl +conhost:%s";
    private static final String LUCENE_QUERY_WITHOUT_HOST = "+parentpath:/application/themes/* +title:template.vtl";

    private ContentletAPI contentletAPI;

    public ThemePaginator() {
        this(APILocator.getContentletAPI());
    }

    @VisibleForTesting
    ThemePaginator(final ContentletAPI contentletAPI) {
        this.contentletAPI = contentletAPI;
    }

    @Override
    public PaginatedArrayList<Contentlet> getItems(final User user, final int limit, final int offset,
                                                   final Map<String, Object> params) throws PaginationException {

        final OrderDirection direction =  params != null && params.get(ORDER_DIRECTION_PARAM_NAME) != null ?
                (OrderDirection) params.get(ORDER_DIRECTION_PARAM_NAME) :
                OrderDirection.ASC;

        final String hostId       = params != null ? (String) params.get(HOST_ID_PARAMETER_NAME) : null;
        final String searchParams = params != null ? (String) params.get(SEARCH_PARAMETER) : null;
        StringBuilder query       = new StringBuilder(String.format(hostId == null ? LUCENE_QUERY_WITHOUT_HOST : LUCENE_QUERY_WITH_HOST, hostId));


        if (UtilMethods.isSet(searchParams)){
            query.append("+catchall:");
            query.append(searchParams);
            query.append("*");
        }

        final String sortBy = String.format("parentPath %s", direction.toString().toLowerCase());

        try {

            final PaginatedArrayList<ContentletSearch> contentletSearches = (PaginatedArrayList<ContentletSearch>)
                    contentletAPI.searchIndex(query.toString(), limit, offset, sortBy, user, false);

            final List<String>  inodes = contentletSearches.stream()
                    .map(contentletSearch -> contentletSearch.getInode())
                    .collect(Collectors.toList());

            final PaginatedArrayList<Contentlet> result = new PaginatedArrayList();
            result.setTotalResults(contentletSearches.getTotalResults());
            result.setQuery(query.toString());
            result.addAll(contentletAPI.findContentlets(inodes));
            return result;
        } catch (DotSecurityException | DotDataException e) {
            throw new PaginationException(e);
        }
    }
}
