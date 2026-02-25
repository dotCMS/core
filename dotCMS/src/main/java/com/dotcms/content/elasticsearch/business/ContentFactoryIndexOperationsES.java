package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;

import com.dotcms.content.elasticsearch.ESQueryCache;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.content.index.ContentFactoryIndexOperations;
import com.dotcms.content.index.IndexContentletScroll;
import com.dotcms.cost.RequestCost;
import com.dotcms.cost.RequestPrices.Price;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.ContentletFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.TotalHits.Relation;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.RandomScoreFunctionBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.jetbrains.annotations.NotNull;

public class ContentFactoryIndexOperationsES implements ContentFactoryIndexOperations {

    private static final SearchHits ERROR_HIT = new SearchHits(new SearchHit[] {}, new TotalHits(0, Relation.EQUAL_TO), 0);
    public static final int ES_TRACK_TOTAL_HITS_DEFAULT = 10000000;
    public static final String ES_TRACK_TOTAL_HITS = "ES_TRACK_TOTAL_HITS";
    private static final String[] ES_FIELDS = {"inode", "identifier"};

    private final ESQueryCache queryCache;

    public ContentFactoryIndexOperationsES() {
        this.queryCache = CacheLocator.getESQueryCache();
    }

    public ContentFactoryIndexOperationsES(ESQueryCache queryCache) {
        this.queryCache = queryCache;
    }

    private final boolean useQueryCache = Config.getBooleanProperty(
            "ES_CACHE_SEARCH_QUERIES", true);

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
     * if enabled SearchRequests are executed and then cached
     * @param searchRequest
     * @return
     */
     private SearchHits cachedIndexSearch(final SearchRequest searchRequest) {

        final Optional<SearchHits> optionalHits = shouldQueryCache() ? queryCache.get(searchRequest) : Optional.empty();
        if(optionalHits.isPresent()) {
            return optionalHits.get();
        }
        try {
            APILocator.getRequestCostAPI()
                    .incrementCost(Price.ES_QUERY, ContentFactoryIndexOperationsES.class, "cachedIndexSearch",
                            new Object[]{searchRequest});
            SearchResponse response = RestHighLevelClientProvider.getInstance().getClient().search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits  = response.getHits();
            if(shouldQueryCache()) {
                queryCache.put(searchRequest, hits);
            }
            return hits;
        } catch (final ElasticsearchStatusException | IndexNotFoundException | SearchPhaseExecutionException e) {
            final String exceptionMsg = (null != e.getCause() ? e.getCause().getMessage() : e.getMessage());
            Logger.warn(this.getClass(), "----------------------------------------------");
            Logger.warn(this.getClass(), String.format("Elasticsearch SEARCH error in index '%s'", (searchRequest.indices()!=null) ? String.join(",", searchRequest.indices()): "unknown"));
            Logger.warn(this.getClass(), String.format("Thread: %s", Thread.currentThread().getName() ));
            Logger.warn(this.getClass(), String.format("ES Query: %s", String.valueOf(searchRequest.source()) ));
            Logger.warn(this.getClass(), String.format("Class %s: %s", e.getClass().getName(), exceptionMsg));
            Logger.warn(this.getClass(), "----------------------------------------------");
            if(shouldQueryCache(exceptionMsg)) {
                queryCache.put(searchRequest, ERROR_HIT);
            }
            return ERROR_HIT;
        } catch(final IllegalStateException e) {
            ContentletFactory.rebuildRestHighLevelClientIfNeeded(e);
            Logger.warnAndDebug(ContentFactoryIndexOperationsES.class, e);
            throw new DotRuntimeException(e);
        } catch (final Exception e) {
            if(ExceptionUtil.causedBy(e, IllegalStateException.class)) {
                ContentletFactory.rebuildRestHighLevelClientIfNeeded(e);
            }
            final String errorMsg = String.format("An error occurred when executing the Lucene Query [ %s ] : %s",
                    searchRequest.source().toString(), e.getMessage());
            Logger.warnAndDebug(ContentFactoryIndexOperationsES.class, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }
    }


    /**
     * The track_total_hits parameter allows you to control how the total number of hits should be tracked.
     * The default is set to 10K. This means that requests will count the total hit accurately up to 10,000 hits.
     * If the param is absent from the properties, it still defaults to 10000000. The param can also be set to a true|false
     * if set to true, it'll track as many items as there are. If set to false, no tracking will be performed at all.
     * So it's better if it isn't set to false ever.
     * @param searchSourceBuilder
     */
    @VisibleForTesting
    public void setTrackHits(final SearchSourceBuilder searchSourceBuilder){
        final int trackTotalHits = Config.getIntProperty(ES_TRACK_TOTAL_HITS, ES_TRACK_TOTAL_HITS_DEFAULT);
        searchSourceBuilder.trackTotalHitsUpTo(trackTotalHits);
    }

    /**
     * if enabled CountRequest are executed and then cached
     * @param countRequest
     * @return
     */
    @RequestCost(Price.ES_CACHE)
    public Long cachedIndexCount(final CountRequest countRequest) {

        final Optional<Long> optionalCount = shouldQueryCache() ? queryCache.get(countRequest) : Optional.empty();
        if(optionalCount.isPresent()) {
            return optionalCount.get();
        }
        try {

            APILocator.getRequestCostAPI().incrementCost(Price.ES_COUNT, ContentFactoryIndexOperationsES.class, "cachedIndexCount",
                    new Object[]{countRequest});


            final CountResponse response = RestHighLevelClientProvider.getInstance().getClient().count(countRequest, RequestOptions.DEFAULT);
            final long count = response.getCount();
            if(shouldQueryCache()) {
                queryCache.put(countRequest, count);
            }
            return count;
        } catch (final ElasticsearchStatusException | IndexNotFoundException | SearchPhaseExecutionException e) {
            final String exceptionMsg = (null != e.getCause() ? e.getCause().getMessage() : e.getMessage());
            Logger.warn(this.getClass(), "----------------------------------------------");
            Logger.warn(this.getClass(), String.format("Elasticsearch error in index '%s'", (countRequest.indices()!=null) ? String.join(",", countRequest.indices()): "unknown"));
            Logger.warn(this.getClass(), String.format("ES Query: %s", String.valueOf(countRequest.source()) ));
            Logger.warn(this.getClass(), String.format("Class %s: %s", e.getClass().getName(), exceptionMsg));
            Logger.warn(this.getClass(), "----------------------------------------------");
            if(shouldQueryCache(exceptionMsg)) {
                queryCache.put(countRequest, -1L);
            }
            return -1L;
        } catch(final IllegalStateException e) {
            ContentletFactory.rebuildRestHighLevelClientIfNeeded(e);
            Logger.warnAndDebug(ContentFactoryIndexOperationsES.class, e);
            throw new DotRuntimeException(e);
        } catch (final Exception e) {
            if(ExceptionUtil.causedBy(e, IllegalStateException.class)) {
                ContentletFactory.rebuildRestHighLevelClientIfNeeded(e);
            }
            final String errorMsg = String.format("An error occurred when executing the Lucene Query [ %s ] : %s",
                    countRequest.source().toString(), e.getMessage());
            Logger.warnAndDebug(ContentFactoryIndexOperationsES.class, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public com.dotcms.content.index.domain.SearchHits searchHits(String query, int limit,
            int offset, String sortBy) {

        // we check the query to figure out which indexes to hit
        final String indexToHit;
        try {
            indexToHit = inferIndexToHit(query);
            if (indexToHit == null) {
                return com.dotcms.content.index.domain.SearchHits.empty();
            }
        } catch (Exception e) {
            Logger.error(this, "Can't get indices information.", e);
            return com.dotcms.content.index.domain.SearchHits.empty();
        }

        return com.dotcms.content.index.domain.SearchHits.from(
                internalSearchHits(query, limit, offset, sortBy, indexToHit));
    }

    /**
     * Executes an internal search query with the specified parameters, constructs the search request,
     * and applies sorting, pagination, and timeout configurations.
     *
     * @param query The search query string used to filter and match documents.
     * @param limit The maximum number of results to return. A value of 0 means no limit.
     * @param offset The starting position of the results to fetch. A value of 0 means the results start from the beginning.
     * @param sortBy The field or criteria to sort the results, e.g., "score" or "moddate".
     * @param indexToHit The name of the index to perform the search operation on.
     * @return A {@code SearchHits} object containing the search results from the query execution.
     */
    private SearchHits internalSearchHits(String query, int limit, int offset, String sortBy,
            String indexToHit) {
        final SearchRequest searchRequest = new SearchRequest();
        final SearchSourceBuilder searchSourceBuilder = createSearchSourceBuilder(query, sortBy);
        setTrackHits(searchSourceBuilder);

        searchSourceBuilder.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
        searchRequest.indices(indexToHit);

        if(limit >0) {
            searchSourceBuilder.size(limit);
        }
        if(offset >0) {
            searchSourceBuilder.from(offset);
        }

        // Handle sorting
        if(UtilMethods.isSet(sortBy)) {
            addSorting(sortBy, searchSourceBuilder);
        }else{
            searchSourceBuilder.sort("moddate", SortOrder.DESC);
        }
        searchRequest.source(searchSourceBuilder);
        return cachedIndexSearch(searchRequest);
    }

    private static void addSorting(String sortBy, SearchSourceBuilder searchSourceBuilder) {
        sortBy = sortBy.toLowerCase();

        if (sortBy.startsWith("score")) {
            String[] sortByCriteria = sortBy.split("[,|\\s+]");
            String defaultSecondarySort = "moddate";
            SortOrder defaultSecondardOrder = SortOrder.DESC;

            if (sortByCriteria.length > 2) {
                if (sortByCriteria[2].equalsIgnoreCase("desc")) {
                    defaultSecondardOrder = SortOrder.DESC;
                } else {
                    defaultSecondardOrder = SortOrder.ASC;
                }
            }
            if (sortByCriteria.length > 1) {
                defaultSecondarySort = sortByCriteria[1];
            }

            searchSourceBuilder.sort("_score", SortOrder.DESC);
            searchSourceBuilder.sort(defaultSecondarySort, defaultSecondardOrder);
        } else if (!sortBy.startsWith("undefined") && !sortBy.startsWith("undefined_dotraw")
                && !sortBy.equals("random") && !sortBy.equals(SortOrder.ASC.toString())
                && !sortBy.equals(SortOrder.DESC.toString())) {
            addBuilderSort(sortBy, searchSourceBuilder);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> search(String query, int limit, int offset) {
        try {
            final SearchRequest searchRequest = new SearchRequest();
            final SearchSourceBuilder searchSourceBuilder = createSearchSourceBuilder(query).size(
                    limit).from(offset);
            searchRequest.source(searchSourceBuilder);
            final SearchHits hits = cachedIndexSearch(searchRequest);
            return Stream.of(hits.getHits()).map(SearchHit::getSourceAsMap)
                    .map(map -> map.get("inode").toString()).collect(
                            Collectors.toList());
        } catch (Exception e) {
            throw new ElasticsearchException(e.getMessage(), e);
        }
    }

    /**
     * It will call createRequest with null as sortBy parameter
     *
     * @param query
     * @return
     */

    private SearchSourceBuilder createSearchSourceBuilder(final String query) {
        return createSearchSourceBuilder(query, null);
    }

    /**
     *
     * @param query
     * @param sortBy i.e. "random" or null object.
     * @return
     */
    SearchSourceBuilder createSearchSourceBuilder(final String query, final String sortBy) {

        final SearchSourceBuilder searchSourceBuilder = SearchSourceBuilder.searchSource();

        QueryBuilder queryBuilder;
        QueryBuilder postFilter = null;

        searchSourceBuilder.fetchSource(ES_FIELDS, null);

        if(Config.getBooleanProperty("ELASTICSEARCH_USE_FILTERS_FOR_SEARCHING",false)
                && sortBy!=null && ! sortBy.toLowerCase().startsWith("score")) {

            if("random".equals(sortBy)){
                queryBuilder = QueryBuilders.functionScoreQuery(QueryBuilders.matchAllQuery()
                        , new RandomScoreFunctionBuilder());
            } else {
                queryBuilder = QueryBuilders.matchAllQuery();
            }

            postFilter = QueryBuilders.queryStringQuery(query);

        } else {
            queryBuilder = QueryBuilders.queryStringQuery(query);
        }

        searchSourceBuilder.query(queryBuilder);
        searchSourceBuilder.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));

        if(UtilMethods.isSet(postFilter)) {
            searchSourceBuilder.postFilter(postFilter);
        }

        return searchSourceBuilder;
    }

    public static void addBuilderSort(@NotNull String sortBy, SearchSourceBuilder srb) {
        String[] sortByArr = sortBy.split(",");
        for (String sort : sortByArr) {
            String[] x = sort.trim().split(" ");
            srb.sort(SortBuilders.fieldSort(x[0].toLowerCase() + "_dotraw")
                    .order(x.length > 1 && x[1].equalsIgnoreCase("desc") ?
                            SortOrder.DESC : SortOrder.ASC));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String inferIndexToHit(String query) {
        final IndiciesInfo info;
        try {
            info = APILocator.getIndiciesAPI().loadIndicies();
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }

        final String indexToHit;
        if(query.contains("+live:true") && !query.contains("+deleted:true")) {
            indexToHit = info.getLive();
        } else {
            indexToHit = info.getWorking();
        }
        return indexToHit;
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
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.queryStringQuery(queryString));
        final CountRequest countRequest = new CountRequest(inferIndexToHit(queryString));
        countRequest.source(sourceBuilder);
        return countRequest;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaginatedArrayList<ContentletSearch> indexSearchScroll(final String query, String sortBy, int scrollBatchSize) {
        PaginatedArrayList<ContentletSearch> contentletSearchList = new PaginatedArrayList<>();

        // Use the ESContentletScrollImpl inner class to handle all scroll logic
        // Using configurable batch size instead of MAX_LIMIT for better memory management
        try (IndexContentletScroll contentletScroll = createScrollQuery(query, APILocator.systemUser(),
                false, scrollBatchSize, sortBy)) {

            contentletSearchList.setTotalResults(contentletScroll.getTotalHits());

            // Fetch all batches (first batch is returned on first nextBatch() call)
            List<ContentletSearch> batch;
            while ((batch = contentletScroll.nextBatch()) != null && !batch.isEmpty()) {
                contentletSearchList.addAll(batch);
            }

            Logger.debug(this.getClass(),
                    () -> String.format("indexSearchScroll completed: totalResults=%d, query=%s",
                            contentletSearchList.getTotalResults(), query));

        } catch (final ElasticsearchStatusException | IndexNotFoundException | SearchPhaseExecutionException e) {
            final String exceptionMsg = (null != e.getCause() ? e.getCause().getMessage() : e.getMessage());
            Logger.warn(this.getClass(), "----------------------------------------------");
            Logger.warn(this.getClass(), String.format("Elasticsearch error for query: %s", query));
            Logger.warn(this.getClass(), String.format("Class %s: %s", e.getClass().getName(), exceptionMsg));
            Logger.warn(this.getClass(), "----------------------------------------------");
            return new PaginatedArrayList<>();
        } catch (final IllegalStateException e) {
            ContentletFactory.rebuildRestHighLevelClientIfNeeded(e);
            Logger.warnAndDebug(ContentFactoryIndexOperationsES.class, e);
            throw new DotRuntimeException(e);
        } catch (final Exception e) {
            if (ExceptionUtil.causedBy(e, IllegalStateException.class)) {
                ContentletFactory.rebuildRestHighLevelClientIfNeeded(e);
            }
            final String errorMsg = String.format("An error occurred when executing the Lucene Query [ %s ] : %s",
                    query, e.getMessage());
            Logger.warnAndDebug(ContentFactoryIndexOperationsES.class, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }

        return contentletSearchList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexContentletScroll createScrollQuery(final String luceneQuery, final User user,
            final boolean respectFrontendRoles, final int batchSize,
            final String sortBy) {
        return new ESContentletScrollImpl(luceneQuery, user, respectFrontendRoles, batchSize, sortBy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexContentletScroll createScrollQuery(final String luceneQuery, final User user,
            final boolean respectFrontendRoles, final int batchSize) {
        return createScrollQuery(luceneQuery, user, respectFrontendRoles, batchSize, "title asc");
    }


}
