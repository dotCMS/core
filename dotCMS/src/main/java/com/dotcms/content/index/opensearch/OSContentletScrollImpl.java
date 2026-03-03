package com.dotcms.content.index.opensearch;

import static com.dotcms.content.index.opensearch.ContentFactoryIndexOperationsOS.addBuilderSort;
import com.dotcms.content.index.IndexContentletScroll;
import com.dotcms.content.elasticsearch.business.TranslatedQuery;
import com.dotcms.content.index.domain.SearchHit;
import com.dotcms.content.index.domain.SearchHits;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryStringQuery;
import org.opensearch.client.opensearch.core.ClearScrollRequest;
import org.opensearch.client.opensearch.core.ScrollRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.HitsMetadata;

/**
 * OpenSearch implementation of ESContentletScroll that encapsulates all OpenSearch
 * Scroll API logic in one place.
 *
 * <p>This class provides scroll-based query functionality for OpenSearch, allowing
 * efficient retrieval of large result sets that exceed the max_result_window limit.</p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>
 * try (OSContentletScrollImpl scroll = new OSContentletScrollImpl(query, user, false, 100, "title asc")) {
 *     List&lt;ContentletSearch&gt; batch;
 *     while ((batch = scroll.nextBatch()) != null && !batch.isEmpty()) {
 *         // process batch
 *     }
 * }
 * </pre>
 *
 * @author Fabrizio Araya
 */
public class OSContentletScrollImpl implements IndexContentletScroll {

    private static final Lazy<Integer> SCROLL_KEEP_ALIVE_MINUTES = Lazy.of(() ->
            Config.getIntProperty("OS_SCROLL_KEEP_ALIVE_MINUTES", 5));
    private static final Lazy<Integer> SCROLL_BATCH_SIZE = Lazy.of(() ->
            Config.getIntProperty("OS_SCROLL_BATCH_SIZE", 1000));

    // State fields
    private String scrollId;
    private long totalHits = 0;
    private boolean hasMoreResults = false;
    private boolean firstBatchReturned = false;
    private List<ContentletSearch> firstBatch;
    private OpenSearchClient osClient;
    private ContentFactoryIndexOperationsOS indexOperations;

    /**
     * Creates a new scroll query instance and initializes the scroll context.
     * The first batch is fetched immediately and cached for the first {@link #nextBatch()} call.
     *
     * @param luceneQuery Lucene query string
     * @param user User for permission checking (only used during initialization)
     * @param respectFrontendRoles Whether to respect frontend roles (only used during initialization)
     * @param batchSize Number of results to retrieve per batch
     * @param sortBy Sort criteria (e.g., "title asc", "moddate desc")
     * @throws DotRuntimeException if scroll initialization fails
     */
    public OSContentletScrollImpl(final String luceneQuery, final User user, final boolean respectFrontendRoles,
            final int batchSize, final String sortBy) {
        this.osClient = new OpenSearchDefaultClientProvider().getClient();
        this.indexOperations = new ContentFactoryIndexOperationsOS();

        // Initialize scroll- and fetch-first batch
        this.firstBatch = Try.of(() -> {
            // Translate query to OpenSearch format using the service
            final TranslatedQuery translatedQuery = TranslatedQuery.translateQuery(luceneQuery, sortBy);
            final String formattedQuery = translatedQuery.getQuery(); // OpenSearch handles date formatting internally

            // Determine which index to query using the service
            final String indexToHit = indexOperations.inferIndexToHit(luceneQuery);

            // Build search request for OpenSearch
            SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder()
                    .index(indexToHit)
                    .query(createQuery(formattedQuery))
                    .size(batchSize)
                    .scroll(Time.of(t -> t.time(SCROLL_KEEP_ALIVE_MINUTES.get() + "m")))
                    .source(src -> src.filter(f -> f.includes(List.of("inode", "identifier"))));

            // Apply sorting
            applySorting(sortBy, searchRequestBuilder);

            // Set track total hits
            indexOperations.setTrackHits(searchRequestBuilder);

            final SearchRequest searchRequest = searchRequestBuilder.build();

            // Execute initial search
            final SearchResponse<Object> response = osClient.search(searchRequest, Object.class);
            this.scrollId = response.scrollId();
            final HitsMetadata<Object> osSearchHits = response.hits();

            // Convert to domain SearchHits
            final SearchHits searchHits = SearchHits.from(osSearchHits);
            this.totalHits = Objects.requireNonNull(searchHits.totalHits()).value();

            // Convert hits to ContentletSearch
            final List<ContentletSearch> results = getContentletSearchFromSearchHits(searchHits);
            this.hasMoreResults = (searchHits.hits() != null && !searchHits.hits().isEmpty());

            Logger.debug(this.getClass(),
                    () -> String.format("OpenSearch scroll initialized: scrollId=%s, totalHits=%d, firstBatchSize=%d",
                            scrollId, totalHits, results.size()));

            return results;

        }).getOrElseThrow(e -> {
            if (e instanceof DotRuntimeException) {
                return (DotRuntimeException) e;
            }
            return new DotRuntimeException("Error initializing OpenSearch scroll API: " + e.getMessage(), e);
        });
    }

    @Override
    public List<ContentletSearch> nextBatch() throws DotDataException {
        // On the first call, return the cached first batch
        if (!firstBatchReturned) {
            firstBatchReturned = true;
            Logger.debug(this.getClass(),
                    () -> String.format("Returning first batch: size=%d", firstBatch.size()));
            return firstBatch;
        }

        // No more results
        if (!hasMoreResults) {
            return new ArrayList<>();
        }

        // Fetch the next batch from the scroll
        return Try.of(() -> {
            final ScrollRequest scrollRequest = ScrollRequest.of(sr -> sr
                    .scrollId(scrollId)
                    .scroll(Time.of(t -> t.time(SCROLL_KEEP_ALIVE_MINUTES.get() + "m"))));

            final SearchResponse<Object> response = osClient.scroll(scrollRequest, Object.class);
            final HitsMetadata<Object> osSearchHits = response.hits();

            // Convert to domain SearchHits
            final SearchHits searchHits = SearchHits.from(osSearchHits);
            final List<ContentletSearch> results = getContentletSearchFromSearchHits(searchHits);
            this.hasMoreResults = (searchHits.hits() != null && !searchHits.hits().isEmpty());

            Logger.debug(this.getClass(),
                    () -> String.format("OpenSearch scroll next batch: batchSize=%d, hasMore=%b",
                            results.size(), hasMoreResults));

            return results;

        }).getOrElseThrow(e -> {
            if (e instanceof DotDataException) {
                return (DotDataException) e;
            }
            return new DotDataException("Error continuing OpenSearch scroll API: " + e.getMessage(), e);
        });
    }

    @Override
    public long getTotalHits() {
        return totalHits;
    }

    @Override
    public boolean hasMoreResults() {
        // If we haven't returned the first batch yet and it has results, there are more
        if (!firstBatchReturned && firstBatch != null && !firstBatch.isEmpty()) {
            return true;
        }
        return hasMoreResults;
    }

    @Override
    public void close() {
        if (scrollId != null && osClient != null) {
            Try.run(() -> {
                final ClearScrollRequest clearScrollRequest = ClearScrollRequest.of(csr -> csr
                        .scrollId(scrollId));
                osClient.clearScroll(clearScrollRequest);
                Logger.debug(this.getClass(), () -> "Cleared OpenSearch scroll context: " + scrollId);
            }).onFailure(e ->
                    Logger.error(this.getClass(), "Error clearing OpenSearch scroll context: " + e.getMessage(), e)
            );
            scrollId = null;
        }
    }

    /**
     * Converts SearchHits to ContentletSearch objects.
     */
    private List<ContentletSearch> getContentletSearchFromSearchHits(final SearchHits searchHits) {
        PaginatedArrayList<ContentletSearch> list = new PaginatedArrayList<>();
        list.setTotalResults(searchHits.totalHits().value());

        for (SearchHit sh : searchHits.hits()) {
            try {
                Map<String, Object> sourceMap = sh.sourceAsMap();
                ContentletSearch conwrapper = new ContentletSearch();
                conwrapper.setId(sh.id());
                conwrapper.setIndex(sh.index());
                conwrapper.setIdentifier(sourceMap.get("identifier").toString());
                conwrapper.setInode(sourceMap.get("inode").toString());
                conwrapper.setScore(sh.score());

                list.add(conwrapper);
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
                throw e;
            }
        }
        return list;
    }

    /**
     * Creates a Query object from the query string.
     * All sorting logic is handled in applySorting() method.
     */
    private Query createQuery(final String query) {
        return Query.of(q -> q.queryString(QueryStringQuery.of(qs -> qs.query(query))));
    }


    /**
     * Applies sorting to the search request builder based on sortBy parameter.
     */
    private void applySorting(String sortBy, SearchRequest.Builder searchRequestBuilder) {
        if (UtilMethods.isSet(sortBy)) {
            sortBy = sortBy.toLowerCase();

            if (sortBy.startsWith("score")) {
                String[] sortByCriteria = sortBy.split("[,|\\s+]");
                String defaultSecondarySort = "moddate";
                SortOrder defaultSecondaryOrder = SortOrder.Desc;

                if (sortByCriteria.length > 2) {
                    defaultSecondaryOrder = sortByCriteria[2].equalsIgnoreCase("desc")
                            ? SortOrder.Desc : SortOrder.Asc;
                }
                if (sortByCriteria.length > 1) {
                    defaultSecondarySort = sortByCriteria[1];
                }

                final String finalDefaultSecondarySort = defaultSecondarySort;
                final SortOrder finalDefaultSecondaryOrder = defaultSecondaryOrder;
                searchRequestBuilder.sort(SortOptions.of(so -> so.score(s -> s.order(SortOrder.Desc))));
                searchRequestBuilder.sort(SortOptions.of(so -> so.field(FieldSort.of(fs -> fs
                        .field(finalDefaultSecondarySort)
                        .order(finalDefaultSecondaryOrder)))));
            } else if (!sortBy.startsWith("undefined") && !sortBy.startsWith("undefined_dotraw")
                    && !sortBy.equals("random")) {
                addBuilderSort(sortBy, searchRequestBuilder);
            }
        } else {
            searchRequestBuilder.sort(SortOptions.of(so -> so.field(FieldSort.of(fs -> fs
                    .field("moddate")
                    .order(SortOrder.Desc)))));
        }
    }
}