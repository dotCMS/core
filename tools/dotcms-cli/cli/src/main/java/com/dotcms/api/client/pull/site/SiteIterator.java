package com.dotcms.api.client.pull.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.Site;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Iterator to fetch all the sites from the remote server
 */
public class SiteIterator implements Iterator<List<Site>> {

    private final RestClientFactory clientFactory;
    private final int pageSize;

    private int currentPage = 1;
    private boolean hasMorePages = true;

    /**
     * Constructs a new SiteIterator with the given RestClientFactory and page size.
     *
     * @param clientFactory the RestClientFactory to use for creating REST clients
     * @param pageSize      the page size to use for fetching sites
     */
    public SiteIterator(final RestClientFactory clientFactory, final int pageSize) {
        this.clientFactory = clientFactory;
        this.pageSize = pageSize;
    }

    /**
     * Returns true if there are more pages of sites to fetch.
     *
     * @return true if there are more pages of sites to fetch
     */
    @Override
    public boolean hasNext() {
        return hasMorePages;
    }

    /**
     * Fetches the next page of sites from the remote server.
     *
     * @return the next page of sites
     */
    @Override
    public List<Site> next() {

        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        final var siteAPI = clientFactory.getClient(SiteAPI.class);

        ResponseEntityView<List<Site>> sitesResponse = siteAPI.getSites(
                null,
                null,
                false,
                false,
                currentPage,
                pageSize
        );

        if (sitesResponse.entity() == null || sitesResponse.entity().isEmpty()) {
            hasMorePages = false;
            return Collections.emptyList();
        }

        currentPage++;
        return sitesResponse.entity();
    }

}
