package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;

import java.util.Collection;

/**
 * Paginator util for Host
 */
public class HostPaginator extends Paginator<Host> {
    private final HostAPI hostAPI;
    private long totalResults;
    private String filter;

    @VisibleForTesting
    public HostPaginator (HostAPI hostAPI) {
        this.hostAPI = hostAPI;
    }

    public HostPaginator () {
        this(APILocator.getHostAPI());
    }

    @Override
    public long getTotalRecords(String condition) {
        return totalResults;
    }

    @Override
    public Collection<Host> getItems(User user, String filter, boolean showArchived, int limit, int offset,
                                     String orderby, OrderDirection direction) {

        final String sanitizedFilter = filter != null && !filter.equals("all") ? filter : StringUtils.EMPTY;

        PaginatedArrayList<Host> hosts = this.hostAPI.search(sanitizedFilter, showArchived, Boolean.FALSE, limit, offset,
                user, false);

        totalResults = hosts.getTotalResults();
        this.filter = filter;
        return hosts;
    }
}
