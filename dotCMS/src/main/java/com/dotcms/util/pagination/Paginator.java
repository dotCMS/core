package com.dotcms.util.pagination;

import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;

import java.util.Collection;
import java.util.Map;

/**
 * Define the methods to get handle a pagination request
 */
public interface Paginator<T> {


    /**
     * Return a set of items for a page
     *
     * @param user user to filter
     * @param filter extra filter parameter
     * @param limit Number of items to return
     * @param offset offset
     * @param orderby field to order
     * @param direction If the order is Asc or Desc
     * @return
     */
    public abstract PaginatedArrayList<T> getItems(User user, String filter, int limit, int offset,
                                                   String orderby, OrderDirection direction, Map<String, Object> extraParams);

    default PaginatedArrayList<T> getItems(User user, String filter, int limit, int offset){
        return getItems(user, filter,  limit,  offset, null, null);
    }

    default PaginatedArrayList<T> getItems(User user, String filter, int limit, int offset, String orderby,
                                   OrderDirection direction){
        return getItems(user, filter,  limit,  offset, orderby, direction, null);
    }
}
