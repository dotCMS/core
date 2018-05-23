package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import java.util.Map;
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
            showSystem = (Boolean) Boolean.valueOf(String.valueOf(extraParams.get(SYSTEM_PARAMETER_NAME)));
        }

        final String sanitizedFilter = "all".equals(filter) ? StringUtils.EMPTY : filter;

        PaginatedArrayList<Host> hosts = null;

        if (showArchived != null && showLive != null){
            hosts = this.hostAPI.search(sanitizedFilter, showArchived, !showLive, showSystem, limit, offset,
                    user, false);
        } else if (showArchived != null){
            hosts = this.hostAPI.search(sanitizedFilter, showArchived, showSystem, limit, offset,
                    user, false);
        } else if (showLive != null) {
            hosts = this.hostAPI.searchByStopped(sanitizedFilter, !showLive, showSystem, limit, offset,
                    user, false);
        } else {
            hosts = this.hostAPI.search(sanitizedFilter, showSystem, limit, offset, user, false);
        }

        return hosts;
    }
}
