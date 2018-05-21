package com.dotcms.util.pagination;

import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;

import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * It is a {@link Paginator} with Order and Filter
 */
public interface PaginatorOrdered<T> extends Paginator<T> {

    default PaginatedArrayList<T> getItems(User user, int limit, int offset, Map<String, Object> params)
            throws PaginationException {

        if (!(params.get(ORDER_DIRECTION_PARAM_NAME) instanceof OrderDirection)) {
            throw new IllegalArgumentException("Order direction must be a OrderDirection instance");
        }

        String filter = (String) params.get(DEFAULT_FILTER_PARAM_NAME);
        String orderBy = (String) params.get(ORDER_BY_PARAM_NAME);
        OrderDirection direction = (OrderDirection) params.get(ORDER_DIRECTION_PARAM_NAME);

        return getItems(user, filter, limit, offset,orderBy, direction, params);
    }

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
    abstract PaginatedArrayList<T> getItems(User user, String filter, int limit, int offset,
                                           String orderby, OrderDirection direction, Map<String, Object> extraParams)
            throws PaginationException;

    default PaginatedArrayList<T> getItems(User user, String filter, int limit, int offset){
        return getItems(user, filter,  limit,  offset, null, null);
    }

    default PaginatedArrayList<T> getItems(User user, String filter, int limit, int offset, String orderby,
                                           OrderDirection direction){
        return getItems(user, filter,  limit,  offset, orderby, direction, null);
    }
}
