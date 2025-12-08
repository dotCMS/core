package com.dotmarketing.util.contentet.pagination;

import com.dotcms.content.elasticsearch.business.ESContentletScroll;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * It is a {@link Iterable} of {@link Contentlet}.
 * The {@link Contentlet} are got from Elastic Search using a lucene query using pagination,
 * the size of each page can be set by {@link PaginatedContentlets#perPage} attribute.
 * Just the {@link Contentlet}'s Inode are storage into memory and before return each of them the
 * {@link Contentlet} object is load from cache or database.
 *
 * <p>For large result sets (configurable via ES_SCROLL_API_THRESHOLD), this class automatically
 * switches to ElasticSearch Scroll API to avoid deep pagination issues.</p>
 *
 * <p><strong>IMPORTANT:</strong> This class implements {@link AutoCloseable}. Always use
 * try-with-resources to ensure Scroll contexts are properly cleaned up:</p>
 * <pre>
 * try (PaginatedContentlets contentlets = APILocator.getContentletAPI().findContentletsPaginatedByHost(host, user, false)) {
 *     for (Contentlet contentlet : contentlets) {
 *         // process contentlet
 *     }
 * }
 * </pre>
 */
public class PaginatedContentlets implements Iterable<Contentlet>, AutoCloseable {

    /**
     * Configuration property to control when to use Scroll API instead of offset pagination.
     * If the total result count exceeds this threshold, Scroll API will be used automatically.
     * Default: 10000
     */
    private static final Lazy<Integer> SCROLL_API_THRESHOLD = Lazy.of(() ->
            Config.getIntProperty("ES_SCROLL_API_THRESHOLD", 10000));

    private static final int NOT_LOAD = -1;
    private final User user;
    private final ContentletAPI contentletAPI;
    private final String luceneQuery;
    private final boolean respectFrontendRoles;

    private final String SORT_BY = "title asc";
    private final int perPage;

    private long totalHits = NOT_LOAD;
    private List<String> currentPageContentletInodes = null;

    // Scroll API state
    private boolean useScrollApi = false;
    private ESContentletScroll esContentletScroll = null;

    /**
     * Create a PaginatedContentlet
     *
     * @param luceneQuery lucene query to get the contentlets
     * @param user User to check permission
     * @param respectFrontendRoles true if you want to respect Front end roles
     * @param perPage Page size limit
     * @param contentletAPI ContentletAPI instance
     */
    PaginatedContentlets(final String luceneQuery, final User user, final boolean respectFrontendRoles,
            final int perPage, final ContentletAPI contentletAPI) {
        this.user = user;
        this.luceneQuery = luceneQuery;
        this.contentletAPI = contentletAPI;
        this.respectFrontendRoles = respectFrontendRoles;
        this.perPage = perPage;

        try {
            currentPageContentletInodes = loadFirstPage();

            if (currentPageContentletInodes == null || currentPageContentletInodes.isEmpty()) {
                if (totalHits > 0) {
                    Logger.error(this.getClass(),
                        String.format("First page is empty but totalHits is %d! useScrollApi: %b, query: %s. " +
                            "This indicates a Scroll API query issue. Resetting totalHits to 0 to prevent iteration.",
                            totalHits, useScrollApi, luceneQuery));
                    // Reset totalHits to prevent iterator from trying to access empty list
                    totalHits = 0;
                }
                currentPageContentletInodes = new ArrayList<>();

                if (totalHits == 0) {
                    Logger.debug(this.getClass(), () -> "Query returned no results or first batch was empty");
                }

                // Clear scroll if it was created but returned no results
                if (esContentletScroll != null && totalHits == 0) {
                    Logger.debug(this.getClass(), "Clearing unused scroll context since no results were returned");
                    Try.run(this::close);
                }
            }

            Logger.debug(this.getClass(),
                () -> String.format("PaginatedContentlets initialized: %d total hits, %d in first page, useScrollApi: %b",
                    totalHits, currentPageContentletInodes.size(), useScrollApi));

        } catch (DotSecurityException | DotDataException e) {
            Logger.error(this.getClass(), "Error initializing PaginatedContentlets: " + e.getMessage(), e);
            currentPageContentletInodes = new ArrayList<>();
            totalHits = 0; // CRITICAL: Reset totalHits so iterator won't try to iterate
            // Clear scroll if it was created before failure
            if (esContentletScroll != null) {
                Try.run(this::close);
            }
        }
    }

    PaginatedContentlets(final String luceneQuery, final User user, final boolean respectFrontendRoles, final int perPage) {
        this(luceneQuery, user, respectFrontendRoles, perPage, APILocator.getContentletAPI());
    }

    @Override
    public Iterator<Contentlet> iterator() {
        return new ContentletIterator();
    }

    /**
     * Loads the first page and determines whether to use Scroll API based on total results.
     * Uses indexCount for efficient count retrieval before deciding on pagination strategy.
     */
    private List<String> loadFirstPage() throws DotSecurityException, DotDataException {
        // First, get the total count efficiently using indexCount (no need to fetch documents)
        totalHits = this.contentletAPI.indexCount(this.luceneQuery, this.user, this.respectFrontendRoles);

        Logger.debug(this.getClass(),
            () -> String.format("Query result count: %d (threshold: %d)", totalHits, SCROLL_API_THRESHOLD.get()));

        // Decide whether to use Scroll API based on total hits
        if (totalHits > SCROLL_API_THRESHOLD.get()) {
            Logger.debug(this.getClass(),
                String.format("Result set size (%d) exceeds threshold (%d). Using Scroll API for query: %s",
                    totalHits, SCROLL_API_THRESHOLD.get(), luceneQuery));
            useScrollApi = true;

            // CRITICAL: Use ContentletAPI to create scroll query with proper permissions applied
            // DO NOT bypass the API by going directly to the factory, as that would skip
            // permission checks and create a security vulnerability
            this.esContentletScroll = this.contentletAPI.createScrollQuery(
                    luceneQuery, user, respectFrontendRoles, perPage, SORT_BY);

            return initializeScrollAndLoadFirstPage();
        } else {
            Logger.debug(this.getClass(),
                () -> String.format("Using standard pagination for %d results (threshold: %d)",
                    totalHits, SCROLL_API_THRESHOLD.get()));
            // Load first page using standard pagination
            return loadNextPageWithOffset(0);
        }
    }

    /**
     * Loads next page using standard offset pagination.
     */
    private List<String> loadNextPageWithOffset(final int offset) throws DotSecurityException, DotDataException {
        final PaginatedArrayList<ContentletSearch> paginatedArrayList = (PaginatedArrayList) this.contentletAPI
                .searchIndex(this.luceneQuery, perPage, offset, SORT_BY, this.user, this.respectFrontendRoles);

        return paginatedArrayList.stream()
                .map(ContentletSearch::getInode)
                .collect(Collectors.toList());
    }

    /**
     * Loads the first page from the Scroll context (already initialized in constructor).
     * Delegates to ESContentletScroll.
     */
    private List<String> initializeScrollAndLoadFirstPage() throws DotDataException {
        // Scroll is already initialized in constructor, just get first batch
        final List<ContentletSearch> batch = esContentletScroll.nextBatch();

        Logger.debug(this.getClass(),
                String.format("Scroll API ready: totalHits=%d, firstBatchSize=%d",
                        esContentletScroll.getTotalHits(), batch.size()));

        return batch.stream()
                .map(ContentletSearch::getInode)
                .collect(Collectors.toList());
    }

    /**
     * Continues an existing Scroll and loads the next page.
     * Delegates to ESContentletScroll.
     */
    private List<String> loadNextPageWithScroll() throws DotDataException {
        final List<ContentletSearch> batch = esContentletScroll.nextBatch();

        Logger.debug(this.getClass(),
                () -> String.format("Scroll API next batch: size=%d", batch.size()));

        return batch.stream()
                .map(ContentletSearch::getInode)
                .collect(Collectors.toList());
    }

    /**
     * Loads the next page using the appropriate method (offset or scroll).
     */
    private List<String> loadNextPage(final int offset) throws DotSecurityException, DotDataException {
        if (useScrollApi) {
            return loadNextPageWithScroll();
        } else {
            return loadNextPageWithOffset(offset);
        }
    }

    public long size(){
        return totalHits;
    }

    /**
     * Returns whether this instance is using Scroll API.
     */
    public boolean isUsingScrollApi() {
        return useScrollApi;
    }

    /**
     * Clears the Scroll context if one exists. This should always be called when done
     * processing results to free server resources.
     * Delegates to ContentletScrollAPI.
     */
    @Override
    public void close() {
        if (esContentletScroll != null) {
            esContentletScroll.close();
            esContentletScroll = null;
        }
    }

    private class ContentletIterator implements Iterator<Contentlet> {

        private int currentIndex = -1;
        private int totalIndex = 0;

        @Override
        public boolean hasNext() {
            return totalIndex < totalHits;
        }

        @Override
        public Contentlet next() {
            try {

                currentIndex++;
                if (currentIndex >= currentPageContentletInodes.size()) {
                    currentPageContentletInodes = loadNextPage(totalIndex);
                    currentIndex = 0;

                    // Check if loaded page is empty when we expected results
                    if (currentPageContentletInodes.isEmpty()) {
                        Logger.error(PaginatedContentlets.this.getClass(),
                            String.format("CRITICAL: loadNextPage returned empty list but hasNext was true! " +
                                "totalIndex=%d, totalHits=%d, useScrollApi=%b, query=%s",
                                totalIndex, totalHits, useScrollApi, luceneQuery));
                        throw new NoSuchElementException("Pagination returned empty batch when results were expected");
                    }
                }

                final String inode = currentPageContentletInodes.get(currentIndex);
                totalIndex++;
                return PaginatedContentlets.this.contentletAPI.find(inode, user, respectFrontendRoles);
            } catch (DotSecurityException | DotDataException e) {
                Logger.error(ContentletIterator.class, e.getMessage());
                throw new NoSuchElementException(e.getMessage());
            }
        }

        @Override
        public void remove() {
            currentPageContentletInodes.remove(currentIndex);
            currentIndex--;
        }
    }
}
