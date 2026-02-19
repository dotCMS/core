package com.dotcms.rest.api.v1.content;

import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.PaginationException;
import com.dotcms.util.pagination.PaginatorOrdered;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Map;

/**
 * Paginator implementation that retrieves pushed asset history for a given content (asset) id.
 * <p>
 * This paginator delegates to {@link com.dotcms.publisher.assets.business.PushedAssetsAPI} to
 * obtain a paginated list of {@link PushedAssetHistory} entries as well as the total count.
 * It is intended to be used by REST resources to expose push history with standard pagination
 * (limit/offset) and optional ordering parameters.
 * </p>
 *
 * @since 25.09.2025
 */
public class ContentPushHistoryPaginator implements PaginatorOrdered<PushedAssetHistory>  {

    /**
     * Retrieves a page of {@link PushedAssetHistory} items for the given asset id (provided in filter).
     *
     * @param user        the current user (not used for filtering here, but part of paginator contract)
     * @param filter      the asset identifier used to filter push history records
     * @param limit       maximum number of records to return
     * @param offset      zero-based index of the first record to return
     * @param orderBy     field to order by (ignored; results are returned in push date order)
     * @param direction   order direction (ignored)
     * @param extraParams extra parameters map (ignored)
     * @return a {@link PaginatedArrayList} containing the current page of results and the total count
     * @throws PaginationException if an error occurs while accessing the underlying data
     */
    @Override
    public PaginatedArrayList<PushedAssetHistory> getItems(final User user,
                                                           final String filter,
                                                           final int limit,
                                                           final int offset,
                                                           final String orderBy,
                                                           final OrderDirection direction,
                                                           final Map<String, Object> extraParams) throws PaginationException {


        try {
            final PaginatedArrayList<PushedAssetHistory> result = new PaginatedArrayList<>();

            final List<PushedAssetHistory> pushedAssets = APILocator.getPushedAssetsAPI().getPushedAssets(
                    filter, offset, limit);

            result.addAll(pushedAssets);
            result.setTotalResults(APILocator.getPushedAssetsAPI().getTotalPushedAssets(filter));

            return result;
        } catch (DotDataException e) {
            throw new PaginationException(e);
        }
    }
}
