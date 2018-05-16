package com.dotmarketing.portlets.htmlpages.theme.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default {@link ThemeAPI} implementation
 */
public class ThemeAPIImpl implements ThemeAPI {
    private static final String LUCENE_QUERY_WITH_HOST = "+parentpath:/application/themes/* +title:template.vtl +conhost:%s";
    private static final String LUCENE_QUERY_WITH_OUT_HOST = "+parentpath:/application/themes/* +title:template.vtl";
    private final ContentletAPI contentletAPI;

    public ThemeAPIImpl() {
        this(APILocator.getContentletAPI());
    }

    @VisibleForTesting
    ThemeAPIImpl(final ContentletAPI contentletAPI) {
        this.contentletAPI = contentletAPI;
    }

    @Override
    public List<Contentlet> findAll(final User user, final String hostId) throws DotSecurityException, DotDataException {
        final String query = String.format(hostId == null ? LUCENE_QUERY_WITH_OUT_HOST : LUCENE_QUERY_WITH_HOST, hostId);
        return contentletAPI.search(query, 0, -1, null, user, false);
    }

    @Override
    public List<Contentlet> find(User user, String hostId, int limit, int offset, OrderDirection direction)
            throws DotSecurityException, DotDataException {
        final String query = String.format(hostId == null ? LUCENE_QUERY_WITH_OUT_HOST : LUCENE_QUERY_WITH_HOST, hostId);
        final String sortBy = String.format("parentPath %s", direction.toString().toLowerCase());
        List<String> inodes = contentletAPI.searchIndex(query, limit, offset, sortBy, user, false)
                .stream()
                .map(contentletSearch -> contentletSearch.getInode())
                .collect(Collectors.toList());

        return contentletAPI.findContentlets(inodes);
    }
}
