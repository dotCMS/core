package com.dotcms.api.client.pull.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.ResponseEntityView;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Iterator to fetch all the content types from the remote server
 */
public class ContentTypeIterator implements Iterator<List<ContentType>> {

    private final RestClientFactory clientFactory;
    private final int pageSize;

    private int currentPage = 1;
    private boolean hasMorePages = true;

    /**
     * Constructs a new ContentTypeIterator with the given RestClientFactory and page size.
     *
     * @param clientFactory the RestClientFactory to use for creating REST clients
     * @param pageSize      the page size to use for fetching sites
     */
    public ContentTypeIterator(final RestClientFactory clientFactory, final int pageSize) {
        this.clientFactory = clientFactory;
        this.pageSize = pageSize;
    }

    /**
     * Returns true if there are more pages of content types to fetch.
     *
     * @return true if there are more pages of content types to fetch
     */
    @Override
    public boolean hasNext() {
        return hasMorePages;
    }

    /**
     * Fetches the next page of content types from the remote server.
     *
     * @return the next page of content types
     */
    @Override
    public List<ContentType> next() {

        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        final var contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

        ResponseEntityView<List<ContentType>> contentTypesResponse = contentTypeAPI.getContentTypes(
                null,
                currentPage,
                pageSize,
                "variable",
                null,
                null,
                null
        );

        if (contentTypesResponse.entity() == null || contentTypesResponse.entity().isEmpty()) {
            hasMorePages = false;
            return Collections.emptyList();
        }

        currentPage++;
        return contentTypesResponse.entity();
    }

}
