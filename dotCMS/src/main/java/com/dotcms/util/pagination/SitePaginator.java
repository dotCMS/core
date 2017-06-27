package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;

import java.util.Collection;

/**
 * Paginator util for Host
 */
public class SitePaginator extends Paginator<Host> {
    private final HostAPI hostAPI;
    private long totalResults;

    @VisibleForTesting
    public SitePaginator(HostAPI hostAPI) {
        this.hostAPI = hostAPI;
    }

    public SitePaginator() {
        this(APILocator.getHostAPI());
    }

    @Override
    public long getTotalRecords(String condition) {
        return totalResults;
    }

    @Override
    public Collection<Host> getItems(User user, String filter, boolean showArchived, int limit, int offset,
                                     String orderby, OrderDirection direction) {

        final String sanitizedFilter = "all".equals(filter) ? StringUtils.EMPTY : filter;

        PaginatedArrayList<Host> hosts = this.hostAPI.search(sanitizedFilter, showArchived, Boolean.FALSE, limit, offset,
                user, false);

        totalResults = hosts.getTotalResults();
        return hosts;
    }
}
