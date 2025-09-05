package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.tag.RestTag;
import com.dotcms.rest.tag.TagsResourceHelper;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Paginator for Tags results with filtering, global tag support, and site-specific queries.
 * 
 * @author hassan
 */
public class TagsPaginator implements Paginator<RestTag> {

    private final TagAPI tagAPI;
    
    // Parameter names for pagination
    public static final String FILTER_PARAM = "filter";
    public static final String SITE_ID_PARAM = "siteId";
    public static final String GLOBAL_PARAM = "global";

    @VisibleForTesting
    public TagsPaginator(final TagAPI tagAPI) {
        this.tagAPI = tagAPI;
    }

    public TagsPaginator() {
        this(APILocator.getTagAPI());
    }

    @Override
    public PaginatedArrayList<RestTag> getItems(final User user, final int limit,
            final int offset, final Map<String, Object> params) throws PaginationException {
        try {
            final PaginatedArrayList<RestTag> result = new PaginatedArrayList<>();
            
            // Extract parameters
            final String filter = (String) params.get(FILTER_PARAM);
            final String siteId = (String) params.get(SITE_ID_PARAM);
            final Boolean global = (Boolean) params.getOrDefault(GLOBAL_PARAM, false);
            final String orderBy = (String) params.getOrDefault(ORDER_BY_PARAM_NAME, "tagname");
            final OrderDirection direction = (OrderDirection) params.getOrDefault(ORDER_DIRECTION_PARAM_NAME, OrderDirection.ASC);
            
            Logger.debug(this, () -> String.format(
                "TagsPaginator: filter='%s', siteId='%s', global=%s, orderBy='%s', direction='%s', offset=%d, limit=%d",
                filter, siteId, global, orderBy, direction, offset, limit
            ));

            // Build order string for TagAPI (uses "-" prefix for DESC)
            final String orderString = OrderDirection.DESC.equals(direction) ? "-" + orderBy : orderBy;
            
            // Get filtered tags using original API
            final List<Tag> tags = tagAPI.getFilteredTags(
                UtilMethods.isSet(filter) ? filter : "",
                siteId != null ? siteId : "",
                global,
                orderString,
                offset,
                limit
            );

            // Get total count using original API                
            final long totalCount = tagAPI.getFilteredTagsCount(
                UtilMethods.isSet(filter) ? filter : "",
                siteId != null ? siteId : "",
                global
            );

            // Convert to RestTag list
            final List<RestTag> restTags = tags.stream()
                .map(TagsResourceHelper::toRestTag)
                .collect(Collectors.toList());

            result.addAll(restTags);
            result.setTotalResults(totalCount);
            
            return result;
            
        } catch (DotDataException e) {
            Logger.error(this, "Error getting tags for pagination", e);
            throw new DotRuntimeException("Error retrieving tags", e);
        }
    }
}