package com.dotcms.util.pagination;

import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;

import java.io.Serializable;
import java.util.Map;
import static com.dotcms.util.CollectionsUtils.map;


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
}
