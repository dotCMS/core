package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;

import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.content.index.domain.SearchHit;
import com.dotcms.content.index.domain.SearchHits;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.builder.SearchSourceBuilder;

/**
 * Private implementation of ESContentletScroll that encapsulates all ElasticSearch
 * Scroll API logic in one place.
 */
class ESContentletScrollImpl implements ESContentletScroll {

    private static final Lazy<Integer> SCROLL_KEEP_ALIVE_MINUTES = Lazy.of(() ->
            Config.getIntProperty("ES_SCROLL_KEEP_ALIVE_MINUTES", 5));
    private static final Lazy<Integer> SCROLL_BATCH_SIZE = Lazy.of(() ->
            Config.getIntProperty("ES_SCROLL_BATCH_SIZE", 1000));

    // State fields
    private String scrollId;
    private long totalHits = 0;
    private boolean hasMoreResults = false;
    private boolean firstBatchReturned = false;
    private List<ContentletSearch> firstBatch;
    private RestHighLevelClient esClient;
    private ContentFactoryIndexOperationsES indexOperations;

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
    ESContentletScrollImpl(final String luceneQuery, final User user, final boolean respectFrontendRoles,
            final int batchSize, final String sortBy) {
        this.esClient = RestHighLevelClientProvider.getInstance().getClient();
        this.indexOperations = new ContentFactoryIndexOperationsES();

        // Initialize scroll and fetch first batch
        this.firstBatch = Try.of(() -> {
            // Translate a query to ES format using the service
            final TranslatedQuery translatedQuery = TranslatedQuery.translateQuery(luceneQuery, sortBy);
            final String formattedQuery = LuceneQueryDateTimeFormatter
                    .findAndReplaceQueryDates(translatedQuery.getQuery());

            // Determine which index to query using the service
            final String indexToHit = indexOperations.inferIndexToHit(luceneQuery);

            // Build search request using the service
            final SearchSourceBuilder sourceBuilder = indexOperations.createSearchSourceBuilder(formattedQuery, sortBy);
            sourceBuilder.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
            sourceBuilder.size(batchSize);

            // Apply sorting using the service
            indexOperations.addBuilderSort(sortBy, sourceBuilder);

            final SearchRequest searchRequest = new SearchRequest()
                    .indices(indexToHit)
                    .source(sourceBuilder)
                    .scroll(TimeValue.timeValueMinutes(SCROLL_KEEP_ALIVE_MINUTES.get()));

            // Execute initial search
            final SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);
            this.scrollId = response.getScrollId();
            final org.elasticsearch.search.SearchHits esSearchHits = response.getHits();

            // Convert to domain SearchHits
            final SearchHits searchHits = SearchHits.from(esSearchHits);
            this.totalHits = Objects.requireNonNull(searchHits.totalHits()).value();

            // Convert hits to ContentletSearch
            final List<ContentletSearch> results = getContentletSearchFromSearchHits(searchHits);
            this.hasMoreResults = (searchHits.hits() != null && !searchHits.hits().isEmpty());

            Logger.debug(this.getClass(),
                    () -> String.format("Scroll initialized: scrollId=%s, totalHits=%d, firstBatchSize=%d",
                            scrollId, totalHits, results.size()));

            return results;

        }).getOrElseThrow(e -> {
            if (e instanceof DotRuntimeException) {
                return (DotRuntimeException) e;
            }
            return new DotRuntimeException("Error initializing scroll API: " + e.getMessage(), e);
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
            final SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId)
                    .scroll(TimeValue.timeValueMinutes(SCROLL_KEEP_ALIVE_MINUTES.get()));

            final SearchResponse response = esClient.scroll(scrollRequest, RequestOptions.DEFAULT);
            final org.elasticsearch.search.SearchHits esSearchHits = response.getHits();

            // Convert to domain SearchHits
            final SearchHits searchHits = SearchHits.from(esSearchHits);
            final List<ContentletSearch> results = getContentletSearchFromSearchHits(searchHits);
            this.hasMoreResults = (searchHits.hits() != null && !searchHits.hits().isEmpty());

            Logger.debug(this.getClass(),
                    () -> String.format("Scroll next batch: batchSize=%d, hasMore=%b",
                            results.size(), hasMoreResults));

            return results;

        }).getOrElseThrow(e -> {
            if (e instanceof DotDataException) {
                return (DotDataException) e;
            }
            return new DotDataException("Error continuing scroll API: " + e.getMessage(), e);
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
        if (scrollId != null && esClient != null) {
            Try.run(() -> {
                final ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
                clearScrollRequest.addScrollId(scrollId);
                esClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
                Logger.debug(this.getClass(), () -> "Cleared scroll context: " + scrollId);
            }).onFailure(e ->
                    Logger.error(this.getClass(), "Error clearing scroll context: " + e.getMessage(), e)
            );
            scrollId = null;
        }
    }


    private List<ContentletSearch> getContentletSearchFromSearchHits(final SearchHits searchHits) {
        PaginatedArrayList<ContentletSearch> list=new PaginatedArrayList<>();
        list.setTotalResults(searchHits.totalHits().value());

        for (SearchHit sh : searchHits.hits()) {
            try{
                Map<String, Object> sourceMap = sh.sourceAsMap();
                ContentletSearch conwrapper= new ContentletSearch();
                conwrapper.setId(sh.id());
                conwrapper.setIndex(sh.index());
                conwrapper.setIdentifier(sourceMap.get("identifier").toString());
                conwrapper.setInode(sourceMap.get("inode").toString());
                conwrapper.setScore(sh.score());

                list.add(conwrapper);
            }
            catch(Exception e){
                Logger.error(this,e.getMessage(),e);
                throw e;
            }

        }
        return list;
    }

}

