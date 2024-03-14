package com.dotcms.util.pagination;

import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;

import java.util.Map;

/**
 * This class implements the {@link Paginator} class, and provides a way to paginate and order
 * results coming from the dotCMS APIs. Default values for the different filtering parameters can be
 * added as need for every specific class implementation. For instance:
 * <ul>
 *     <li>{@link ContentTypesPaginator}</li>
 *     <li>{@link ContainerPaginator}</li>
 *     <li>{@link TemplatePaginator}</li>
 *     <li>And so on.</li>
 * </ul>
 *
 * @author Freddy Rodriguez
 * @since May 21st, 2018
 */
public interface PaginatorOrdered<T> extends Paginator<T> {

    /**
     * Returns a list of paginated elements based on the specified search criteria.
     *
     * @param user   The {@link User} that is using this method.
     * @param limit  The maximum number of returned items in the result set, for pagination
     *               purposes.
     * @param offset The requested page number of the result set, for pagination purposes.
     * @param params A {@link Map} including any extra parameters that may be required by the
     *               implementation class that are not part of this method's signature.
     *
     * @return A {@link PaginatedArrayList} of items matching the specified search criteria.
     *
     * @throws PaginationException An error occurred when retrieving information from the database.
     */
    default PaginatedArrayList<T> getItems(final User user, final int limit, final int offset, final Map<String, Object> params)
            throws PaginationException {

        if (!(params.get(ORDER_DIRECTION_PARAM_NAME) instanceof OrderDirection)) {
            throw new IllegalArgumentException("The 'ORDER_DIRECTION' parameter must be a OrderDirection instance");
        }

        final String filter = (String) params.get(DEFAULT_FILTER_PARAM_NAME);
        final String orderBy = (String) params.get(ORDER_BY_PARAM_NAME);
        final OrderDirection direction = (OrderDirection) params.get(ORDER_DIRECTION_PARAM_NAME);

        return getItems(user, filter, limit, offset,orderBy, direction, params);
    }

    /**
     * Returns a list of paginated elements based on the specified search criteria.
     *
     * @param user        The {@link User} that is using this method.
     * @param filter      Allows you to add more conditions to the query via SQL code. It's very
     *                    important that you always sanitize the code in order to avoid SQL
     *                    injection attacks. The complexity of the allows SQL code will depend on
     *                    how the Paginator class is implemented.
     * @param limit       The maximum number of returned items in the result set, for pagination
     *                    purposes.
     * @param offset      The requested page number of the result set, for pagination purposes.
     * @param orderBy     The order-by clause, which must always be sanitized as well.
     * @param direction   The direction of the order-by clause for the results.
     * @param extraParams A {@link Map} including any extra parameters that may be required by the
     *                    implementation class that are not part of this method's signature.
     *
     * @return A {@link PaginatedArrayList} of items matching the specified search criteria.
     *
     * @throws PaginationException An error occurred when retrieving information from the database.
     */
    PaginatedArrayList<T> getItems(final User user, final String filter, final int limit, final int offset, final String orderBy, final OrderDirection direction, final Map<String, Object> extraParams) throws PaginationException;

    /**
     * @param user   The {@link User} that is using this method.
     * @param filter Allows you to add more conditions to the query via SQL code. It's very
     *               important that you always sanitize the code in order to avoid SQL injection
     *               attacks. The complexity of the allows SQL code will depend on how the Paginator
     *               class is implemented.
     * @param limit  The maximum number of returned items in the result set, for pagination
     *               purposes.
     * @param offset The requested page number of the result set, for pagination purposes.
     *
     * @return A {@link PaginatedArrayList} of items matching the specified search criteria.
     */
    default PaginatedArrayList<T> getItems(final User user, final String filter, final int limit, final int offset) {
        return getItems(user, filter,  limit,  offset, null, null);
    }

    /**
     * @param user      The {@link User} that is using this method.
     * @param filter    Allows you to add more conditions to the query via SQL code. It's very
     *                  important that you always sanitize the code in order to avoid SQL injection
     *                  attacks. The complexity of the allows SQL code will depend on how the
     *                  Paginator class is implemented.
     * @param limit     The maximum number of returned items in the result set, for pagination
     *                  purposes.
     * @param offset    The requested page number of the result set, for pagination purposes.
     * @param orderBy   The order-by clause, which must always be sanitized as well.
     * @param direction The direction of the order-by clause for the results.
     *
     * @return A {@link PaginatedArrayList} of items matching the specified search criteria.
     */
    default PaginatedArrayList<T> getItems(final User user, final String filter, final int limit, final int offset, final String orderBy, final OrderDirection direction) {
        return getItems(user, filter,  limit,  offset, orderBy, direction, null);
    }

}
