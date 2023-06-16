package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import java.util.Map;

import com.liferay.util.StringPool;
import org.apache.commons.lang.StringUtils;

/**
 * Paginator util for Host
 */
public class SitePaginator implements PaginatorOrdered<Host> {
    public static final String ARCHIVED_PARAMETER_NAME = "archive";
    public static final  String LIVE_PARAMETER_NAME = "live";
    public static final  String SYSTEM_PARAMETER_NAME = "system";

    private final HostAPI hostAPI;

    @VisibleForTesting
    public SitePaginator(HostAPI hostAPI) {
        this.hostAPI = hostAPI;
    }

    public SitePaginator() {
        this(APILocator.getHostAPI());
    }


    @Override
    public PaginatedArrayList<Host> getItems(final User user, final String filter, final int limit, final int offset,
                                     final String orderby, final OrderDirection direction, final Map<String, Object> extraParams) {

        Boolean showArchived = null;
        Boolean showLive = null;
        boolean showSystem = false;

        if (extraParams != null) {
            showArchived = (Boolean) extraParams.get(ARCHIVED_PARAMETER_NAME);
            showLive = (Boolean) extraParams.get(LIVE_PARAMETER_NAME);
            showSystem = Boolean.valueOf(String.valueOf(extraParams.get(SYSTEM_PARAMETER_NAME)));
        }

        String sanitizedFilter = "all".equals(filter) ? StringUtils.EMPTY : filter;
        sanitizedFilter = sanitizedFilter.startsWith(StringPool.STAR) ? sanitizedFilter.replace(StringPool.STAR,
                StringPool.PERCENT) : sanitizedFilter;
        PaginatedArrayList<Host> sites;

        if (showArchived != null && showLive != null) {
            sites = this.hostAPI.search(sanitizedFilter, showArchived, !showLive, showSystem, limit, offset,
                    user, false);
        } else if (showArchived != null) {
            if (!showArchived){
                sites = this.hostAPI.search(sanitizedFilter, false, true, showSystem, limit, offset,
                        user, false);
            }else {
                // If archived Sites must be returned, then the showStopped flag must be "true". Otherwise, it must be "false"
                final boolean showStopped = showArchived ? true : false;
                sites = this.hostAPI.search(sanitizedFilter, showArchived, showStopped, showSystem, limit, offset,
                        user, false);
            }
        } else if (showLive != null) {
            sites = this.hostAPI.searchByStopped(sanitizedFilter, !showLive, showSystem, limit, offset,
                    user, false);
        } else {
            sites = this.hostAPI.search(sanitizedFilter, showSystem, limit, offset, user, false);
        }

        return sites;
    }
}
