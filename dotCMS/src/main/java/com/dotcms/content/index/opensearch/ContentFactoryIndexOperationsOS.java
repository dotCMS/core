package com.dotcms.content.index.opensearch;

import static com.dotcms.content.index.opensearch.OpenSearchIndexAPI.INDEX_OPERATIONS_TIMEOUT;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.elasticsearch.business.ESContentletScroll;
import com.dotcms.content.index.ContentFactoryIndexOperations;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.domain.SearchHits;
import com.dotcms.cost.RequestCost;
import com.dotcms.cost.RequestPrices.Price;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.FunctionScoreQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchAllQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryStringQuery;
import org.opensearch.client.opensearch._types.query_dsl.RandomScoreFunction;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.core.CountResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.HitsMetadata;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;

/**
 * Class that centralize all the index related operations used in ContentFactoryIndex
 */
public class ContentFactoryIndexOperationsOS implements ContentFactoryIndexOperations {

    private static final HitsMetadata<Object> ERROR_HIT = HitsMetadata.of(hm -> hm
            .hits(List.of())
            .total(th -> th.value(0L).relation(TotalHitsRelation.Eq))
            .maxScore(0F));

    public static final int OS_TRACK_TOTAL_HITS_DEFAULT = 10000000;
    public static final String OS_TRACK_TOTAL_HITS = "OS_TRACK_TOTAL_HITS";
    private static final String[] OS_FIELDS = {"inode", "identifier"};

    private final OSQueryCache queryCache;
    private final OpenSearchDefaultClientProvider clientProvider;

    public ContentFactoryIndexOperationsOS() {
        this.queryCache = new OSQueryCache();  // Use our OSQueryCache
        this.clientProvider = CDIUtils.getBeanThrows(OpenSearchDefaultClientProvider.class);
    }

    public ContentFactoryIndexOperationsOS(OSQueryCache queryCache, OpenSearchDefaultClientProvider clientProvider) {
        this.queryCache = queryCache;
        this.clientProvider = clientProvider;
    }

    private final boolean useQueryCache = Lazy.of(()->Config.getBooleanProperty(
            "OS_CACHE_SEARCH_QUERIES", true)).get();

    private boolean shouldQueryCache() {
        return useQueryCache;
    }

    private boolean shouldQueryCache(final String exceptionMsg) {
        if(!shouldQueryCache() || null == exceptionMsg) {
            return false;
        }
        final String exception = exceptionMsg.toLowerCase();
        return exception.contains("parse_exception") ||
                exception.contains("search_phase_execution_exception");
    }

    /**
     * If enabled SearchRequests are executed and then cached
     */
    private HitsMetadata<Object> cachedIndexSearch(final SearchRequest searchRequest) {

        final Optional<HitsMetadata<Object>> optionalHits = shouldQueryCache() ? queryCache.get(searchRequest) : Optional.empty();
        if(optionalHits.isPresent()) {
            return optionalHits.get();
        }
        try {
            APILocator.getRequestCostAPI()
                    .incrementCost(Price.ES_QUERY, ContentFactoryIndexOperationsOS.class, "cachedIndexSearch",
                            new Object[]{searchRequest});

            OpenSearchClient client = clientProvider.getClient();
            SearchResponse<Object> response = client.search(searchRequest, Object.class);
            HitsMetadata<Object> hits = response.hits();

            if(shouldQueryCache()) {
                queryCache.put(searchRequest, hits);
            }
            return hits;
        } catch (final OpenSearchException e) {
            final String exceptionMsg = (null != e.getCause() ? e.getCause().getMessage() : e.getMessage());
            Logger.warn(this.getClass(), "----------------------------------------------");
            Logger.warn(this.getClass(), String.format("OpenSearch SEARCH error in index '%s'",
                    (searchRequest.index() != null) ? String.join(",", searchRequest.index()) : "unknown"));
            Logger.warn(this.getClass(), String.format("Thread: %s", Thread.currentThread().getName()));
            Logger.warn(this.getClass(), String.format("OS Query: %s", String.valueOf(searchRequest)));
            Logger.warn(this.getClass(), String.format("Class %s: %s", e.getClass().getName(), exceptionMsg));
            Logger.warn(this.getClass(), "----------------------------------------------");
            if(shouldQueryCache(exceptionMsg)) {
                queryCache.put(searchRequest, ERROR_HIT);
            }
            return ERROR_HIT;
        } catch(final IllegalStateException e) {
            // ContentletFactory.rebuildOpenSearchClientIfNeeded(e); // Equivalent to ES method
            Logger.warnAndDebug(ContentFactoryIndexOperationsOS.class, e);
            throw new DotRuntimeException(e);
        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when executing the Lucene Query [ %s ] : %s",
                    searchRequest.toString(), e.getMessage());
            Logger.warnAndDebug(ContentFactoryIndexOperationsOS.class, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }
    }

    /**
     * The track_total_hits parameter allows you to control how the total number of hits should be tracked.
     * The default is set to 10K. This means that requests will count the total hit accurately up to 10,000 hits.
     * If the param is absent from the properties, it still defaults to 10000000. The param can also be set to a true|false
     * if set to true, it'll track as many items as there are. If set to false, no tracking will be performed at all.
     * So it's better if it isn't set to false ever.
     */
    @VisibleForTesting
    @CanIgnoreReturnValue
    public SearchRequest.Builder setTrackHits(final SearchRequest.Builder searchRequestBuilder){
        final int trackTotalHits = Config.getIntProperty(OS_TRACK_TOTAL_HITS, OS_TRACK_TOTAL_HITS_DEFAULT);
        searchRequestBuilder.trackTotalHits(th -> th.count(trackTotalHits));
        return searchRequestBuilder;
    }

    /**
     * If enabled CountRequest are executed and then cached
     */
    @RequestCost(Price.ES_CACHE)
    public Long cachedIndexCount(final CountRequest countRequest) {

        final Optional<Long> optionalCount = shouldQueryCache() ? queryCache.get(countRequest) : Optional.empty();
        if(optionalCount.isPresent()) {
            return optionalCount.get();
        }
        try {

            APILocator.getRequestCostAPI().incrementCost(Price.ES_COUNT, ContentFactoryIndexOperationsOS.class, "cachedIndexCount",
                    new Object[]{countRequest});

            OpenSearchClient client = clientProvider.getClient();
            final CountResponse response = client.count(countRequest);
            final long count = response.count();
            if(shouldQueryCache()) {
                queryCache.put(countRequest, count);
            }
            return count;
        } catch (final OpenSearchException e) {
            final String exceptionMsg = (null != e.getCause() ? e.getCause().getMessage() : e.getMessage());
            Logger.warn(this.getClass(), "----------------------------------------------");
            Logger.warn(this.getClass(), String.format("OpenSearch error in index '%s'",countRequest.index()));
            Logger.warn(this.getClass(), String.format("OS Query: %s", countRequest));
            Logger.warn(this.getClass(), String.format("Class %s: %s", e.getClass().getName(), exceptionMsg));
            Logger.warn(this.getClass(), "----------------------------------------------");
            if(shouldQueryCache(exceptionMsg)) {
                queryCache.put(countRequest, -1L);
            }
            return -1L;
        } catch(final IllegalStateException e) {
            Logger.warnAndDebug(ContentFactoryIndexOperationsOS.class, e);
            throw new DotRuntimeException(e);
        } catch (final Exception e) {
            if(ExceptionUtil.causedBy(e, IllegalStateException.class)) {
                // Handle client rebuild if needed
            }
            final String errorMsg = String.format("An error occurred when executing the Lucene Query [ %s ] : %s",
                    countRequest.toString(), e.getMessage());
            Logger.warnAndDebug(ContentFactoryIndexOperationsOS.class, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchHits searchHits(String query, int limit, int offset, String sortBy) {

        // we check the query to figure out which indexes to hit
        final String indexToHit;
        try {
            indexToHit = inferIndexToHit(query);
            if (indexToHit == null) {
                return SearchHits.empty();
            }
        } catch (Exception e) {
            Logger.error(this, "Can't get indices information.", e);
            return SearchHits.empty();
        }

        return SearchHits.from(
                internalSearchHits(query, limit, offset, sortBy, indexToHit));
    }

    /**
     * Executes an internal search query with the specified parameters, constructs the search request,
     * and applies sorting, pagination, and timeout configurations.
     */
    private HitsMetadata<Object> internalSearchHits(String query, int limit, int offset, String sortBy,
            String indexToHit) {

        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        searchRequestBuilder.index(indexToHit);

        // Build the query
        Query searchQuery = createQuery(query, sortBy);
        searchRequestBuilder.query(searchQuery);

        // Set timeout
        searchRequestBuilder.timeout(INDEX_OPERATIONS_TIMEOUT);

        // Set source fields
        searchRequestBuilder.source(src -> src.filter(f -> f.includes(List.of(OS_FIELDS))));

        // Set track total hits
        setTrackHits(searchRequestBuilder);

        if(limit > 0) {
            searchRequestBuilder.size(limit);
        }
        if(offset > 0) {
            searchRequestBuilder.from(offset);
        }

        // Handle sorting
        if(UtilMethods.isSet(sortBy)) {
            addSorting(searchRequestBuilder, sortBy);
        } else {
            searchRequestBuilder.sort(SortOptions.of(so -> so.field(FieldSort.of(fs -> fs.field("moddate").order(SortOrder.Desc)))));
        }

        SearchRequest searchRequest = searchRequestBuilder.build();
        return cachedIndexSearch(searchRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> search(String query, int limit, int offset) {

            SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();

            Query searchQuery = createQuery(query, null);
            searchRequestBuilder.query(searchQuery)
                    .size(limit)
                    .from(offset)
                    .source(src -> src.filter(f -> f.includes(List.of(OS_FIELDS))));

            SearchRequest searchRequest = searchRequestBuilder.build();
            final HitsMetadata<Object> hits = cachedIndexSearch(searchRequest);

            return hits.hits().stream()
                    .map(Hit::source)
                    .filter(source -> source instanceof java.util.Map)
                    .map(source -> (java.util.Map<String, Object>) source)
                    .map(map -> map.get("inode").toString())
                    .collect(Collectors.toList());

    }

    /**
     * Creates a Query object from the query string and sort parameters
     */
    private Query createQuery(final String query, final String sortBy) {

        if(Config.getBooleanProperty("OPENSEARCH_USE_FILTERS_FOR_SEARCHING", false)
                && sortBy != null && !sortBy.toLowerCase().startsWith("score")) {

            if("random".equals(sortBy)){
                return Query.of(q -> q.functionScore(FunctionScoreQuery.of(fsq -> fsq
                        .query(Query.of(maq -> maq.matchAll(MatchAllQuery.of(ma -> ma))))
                        .functions(fsf -> fsf.randomScore(RandomScoreFunction.of(rs -> rs)))
                )));
            } else {
                // Use match_all with post_filter (this would need to be implemented differently in OpenSearch Java client)
                return Query.of(q -> q.queryString(QueryStringQuery.of(qs -> qs.query(query))));
            }

        } else {
            return Query.of(q -> q.queryString(QueryStringQuery.of(qs -> qs.query(query))));
        }
    }

    /**
     * Adds sorting to the search request builder
     */
    private void addSorting(SearchRequest.Builder searchRequestBuilder, String sortBy) {
        sortBy = sortBy.toLowerCase();

        if(sortBy.startsWith("score")){
            String[] sortByCriteria = sortBy.split("[,|\\s+]");
            String defaultSecondarySort = "moddate";
            SortOrder defaultSecondaryOrder = SortOrder.Desc;

            if(sortByCriteria.length > 2){
                if(sortByCriteria[2].equalsIgnoreCase("desc")) {
                    defaultSecondaryOrder = SortOrder.Desc;
                } else {
                    defaultSecondaryOrder = SortOrder.Asc;
                }
            }
            if(sortByCriteria.length > 1){
                defaultSecondarySort = sortByCriteria[1];
            }

            final String finalDefaultSecondarySort = defaultSecondarySort;
            final SortOrder finalSecondaryOrder = defaultSecondaryOrder;
            searchRequestBuilder.sort(SortOptions.of(builder ->  builder.field(FieldSort.of(fs -> fs.field(finalDefaultSecondarySort).order(finalSecondaryOrder)))));

        } else if(!sortBy.startsWith("undefined") && !sortBy.startsWith("undefined_dotraw") && !sortBy.equals("random")
                && !sortBy.equals(SortOrder.Asc.toString())  && !sortBy.equals(SortOrder.Desc.toString())) {
            addBuilderSort(sortBy, searchRequestBuilder);
        }
    }

    public static void addBuilderSort(@NotNull String sortBy, SearchRequest.Builder searchRequestBuilder) {
        String[] sortByArr = sortBy.split(",");
        for (String sort : sortByArr) {
            String[] x = sort.trim().split(" ");
            SortOrder order = x.length > 1 && x[1].equalsIgnoreCase("desc") ? SortOrder.Desc : SortOrder.Asc;
            searchRequestBuilder.sort(SortOptions.of(so -> so.field(FieldSort.of(fs -> fs
                    .field(x[0].toLowerCase() + "_dotraw")
                    .order(order)))));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String inferIndexToHit(String query) {
        String indexNameToHit = null;
        final Optional<VersionedIndices> optional = Try.of(()->APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices()).getOrElse(Optional.empty());
        if (optional.isEmpty()){
            throw new DotRuntimeException("Unable to load default versioned indices, falling back to default index");
        } else {
            final VersionedIndices versionedIndices = optional.get();
            final Optional<String> live = versionedIndices.live();
            final Optional<String> working = versionedIndices.working();
            if(query.contains("+live:true") && !query.contains("+deleted:true")) {
                if(live.isPresent()){
                    indexNameToHit = live.get();
                } else {
                    Logger.warn(this, "No live index found when inferring index for query: " + query);
                }
            } else {
                if(working.isPresent()){
                    indexNameToHit = working.get();
                } else {
                    Logger.warn(this, "No working index found when inferring index for query: " + query);
                }
            }
            return indexNameToHit;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long indexCount(final String query){
         CountRequest countRequest = getCountRequest(query);
         return cachedIndexCount(countRequest);
    }

    @NotNull
    private CountRequest getCountRequest(final String queryString) {
        Query query = Query.of(q -> q.queryString(QueryStringQuery.of(qs -> qs.query(queryString))));

        return CountRequest.of(cr -> cr
                .index(inferIndexToHit(queryString))
                .query(query));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaginatedArrayList<ContentletSearch> indexSearchScroll(String query, String sortBy,
            int scrollBatchSize) {
        // TODO: Implement proper OpenSearch scroll functionality
        // For now, throw UnsupportedOperationException to indicate this needs implementation
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ESContentletScroll createScrollQuery(String luceneQuery, User user,
            boolean respectFrontendRoles, int batchSize, String sortBy) {
        // TODO: Implement proper OpenSearch scroll functionality
        // For now, throw UnsupportedOperationException to indicate this needs implementation
        throw new UnsupportedOperationException("OpenSearch scroll queries not yet implemented. " +
                "Use indexSearchScroll() method for batch processing.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ESContentletScroll createScrollQuery(String luceneQuery, User user,
            boolean respectFrontendRoles, int batchSize) {
        return createScrollQuery(luceneQuery, user, respectFrontendRoles, batchSize, "title asc");
    }
}