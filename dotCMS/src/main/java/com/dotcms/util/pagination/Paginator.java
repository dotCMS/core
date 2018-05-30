package com.dotcms.util.pagination;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import java.util.Map;


/**
 * Define the methods to get handle a pagination request
 */
public interface Paginator<T> {

    public static final String ORDER_BY_PARAM_NAME = "ORDER_BY";
    public static final String ORDER_DIRECTION_PARAM_NAME = "ORDER_DIRECTION";
    public static final String DEFAULT_FILTER_PARAM_NAME = "FILTER";
    
    /**
     * Return a set of items for a page
     *
     * @param user user to filter
     * @param limit Number of items to return
     * @param offset offset
     * @param params extra params for pagination, if include field_name: value then the result is filter for the value
     *               also can include orderBy and orderDirection parameters
     * @return
     */
    public abstract PaginatedArrayList<T> getItems(User user, int limit, int offset, Map<String, Object> params)
            throws PaginationException;

    /**
     * default Pagination instantiator.
     * Allows the possibility to instantiate different constructs in the the descendants
     * @return
     */
    default Pagination createPagination(final User user, final Map<String,Object> paginationValuesMap) throws DotDataException {

        final String link = (String)paginationValuesMap.get(Pagination.LINK);
        final Number perPage = (Number)paginationValuesMap.get(Pagination.PER_PAGE);
        final Number currentPage = (Number)paginationValuesMap.get(Pagination.CURRENT_PAGE);
        final Number linkPages = (Number) paginationValuesMap.get(Pagination.LINK_PAGES);
        final Number totalRecords = (Number) paginationValuesMap.get(Pagination.TOTAL_RECORDS);

        return new Pagination(link, perPage, currentPage, linkPages, totalRecords);
    }
}
