package com.dotcms.util.pagination;

import com.dotcms.rest.api.v1.content.ContentReportParams;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;

import java.util.Map;

/**
 * This implementation of the {@link PaginatorOrdered} interface provides a way to paginate through
 * the records of the Content Report View.
 *
 * @author Jose Castro
 * @since Mar 7th, 2024
 */
public abstract class ContentReportPaginator<T> implements PaginatorOrdered<T> {

    public static final String FOLDER_PARAM = "folder";
    public static final String SITE_PARAM = "site";

    @Override
    public PaginatedArrayList<T> getItems(final User user, final String filter,
                                                          final int limit, final int offset,
                                                          final String orderBy,
                                                          final OrderDirection direction,
                                                          final Map<String, Object> extraParams) throws PaginationException {
        final ContentReportParams params = new ContentReportParams.Builder()
                .user(user)
                .filter(filter)
                .page(offset)
                .perPage(limit)
                .orderBy(orderBy)
                .orderDirection(direction)
                .extraParams(extraParams)
                .build();
        return getItems(params);
    }

    /**
     * Returns a list of Content Report Views based on the specified parameters, and living under a
     * specific dotCMS object. The criteria to define how deep the report should go and what
     * additional information is required must be addressed by every implementation.
     *
     * @param params The {@link ContentReportParams} object with the filtering criteria used to
     *               generate the Content Report.
     *
     * @return A paginated list of the {@link ContentReportView} objects.
     *
     * @throws PaginationException An error occurred when generating the Content Report.
     */
    public abstract PaginatedArrayList<T> getItems(final ContentReportParams params) throws PaginationException;

}
