package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;
import static com.dotmarketing.common.reindex.ReindexThread.ELASTICSEARCH_CONCURRENT_REQUESTS;

import com.dotcms.content.index.ContentletIndexOperations;
import com.dotcms.content.index.domain.IndexBulkProcessor;
import com.dotcms.content.index.domain.IndexBulkRequest;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.APILocator;
import com.dotcms.content.index.domain.ImmutableIndexBulkItemResult;
import com.dotcms.content.index.domain.IndexBulkItemResult;
import com.dotcms.content.index.domain.IndexBulkListener;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.ContentletFactory;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Logger;
import com.dotcms.contenttype.model.type.ContentType;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;

/**
 * Elasticsearch implementation of {@link ContentletIndexOperations}.
 *
 * <p>All Elasticsearch-specific types ({@code BulkRequest}, {@code BulkProcessor},
 * {@code IndexRequest}, {@code DeleteRequest}, etc.) are confined to this class.
 * The public API exposes only {@link IndexBulkRequest} and {@link IndexBulkProcessor}
 * handles to callers.</p>
 *
 * <p>Extracted from {@code ContentletIndexAPIImpl} as part of the vendor-neutral
 * router pattern established in {@link ESContentFactoryImpl}.</p>
 *
 * @author Fabrizio Araya
 * @see ContentletIndexOperationsOS
 */
public class ContentletIndexOperationsES implements ContentletIndexOperations {

    // =========================================================================
    // Inner wrappers — vendor types stay inside this class
    // =========================================================================

    /** Wraps an ES {@link BulkRequest} behind the neutral {@link IndexBulkRequest} handle. */
    static final class ESIndexBulkRequest implements IndexBulkRequest {
        final BulkRequest delegate;

        ESIndexBulkRequest(final BulkRequest delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return delegate.numberOfActions();
        }
    }

    /** Wraps an ES {@link BulkProcessor} behind the neutral {@link IndexBulkProcessor} handle. */
    static final class ESIndexBulkProcessor implements IndexBulkProcessor {
        final BulkProcessor delegate;

        ESIndexBulkProcessor(final BulkProcessor delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() throws Exception {
            delegate.close();
        }
    }

    // =========================================================================
    // Batch write path
    // =========================================================================

    @Override
    public IndexBulkRequest createBulkRequest() {
        final BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.setRefreshPolicy(RefreshPolicy.NONE);
        return new ESIndexBulkRequest(bulkRequest);
    }

    @Override
    public void addIndexOp(final IndexBulkRequest req, final String indexName,
            final String docId, final String jsonMapping) {
        asBulkRequest(req).add(
                new IndexRequest(indexName, "_doc", docId)
                        .source(jsonMapping, XContentType.JSON));
    }

    @Override
    public void addDeleteOp(final IndexBulkRequest req, final String indexName,
            final String docId) {
        asBulkRequest(req).add(new DeleteRequest(indexName, "_doc", docId));
    }

    @Override
    public void setRefreshPolicy(final IndexBulkRequest req,
            final IndexBulkRequest.RefreshPolicy policy) {
        final WriteRequest.RefreshPolicy esPolicy;
        switch (policy) {
            case IMMEDIATE: esPolicy = WriteRequest.RefreshPolicy.IMMEDIATE;  break;
            case WAIT_FOR:  esPolicy = WriteRequest.RefreshPolicy.WAIT_UNTIL; break;
            default:        esPolicy = WriteRequest.RefreshPolicy.NONE;
        }
        asBulkRequest(req).setRefreshPolicy(esPolicy);
    }

    @Override
    public void putToIndex(final IndexBulkRequest req) {
        final BulkRequest bulkRequest = asBulkRequest(req);
        if (bulkRequest.numberOfActions() == 0) {
            return;
        }
        bulkRequest.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
        try {
            final BulkResponse response = RestHighLevelClientProvider.getInstance()
                    .getClient().bulk(bulkRequest, RequestOptions.DEFAULT);
            if (response != null && response.hasFailures()) {
                Logger.error(this,
                        "Error reindexing (" + response.getItems().length + ") content(s): "
                                + response.buildFailureMessage());
            }
        } catch (final Exception e) {
            if (ExceptionUtil.causedBy(e, IllegalStateException.class)) {
                ContentletFactory.rebuildRestHighLevelClientIfNeeded(e);
            }
            Logger.warnAndDebug(ContentletIndexOperationsES.class, e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    // =========================================================================
    // Async bulk-processor write path
    // =========================================================================

    @Override
    public IndexBulkProcessor createBulkProcessor(final IndexBulkListener listener) {
        final int numberToReindexInRequest = Try.of(
                () -> ReindexThread.ELASTICSEARCH_BULK_ACTIONS
                        / APILocator.getServerAPI().getReindexingServers().size())
                .getOrElse(10);

        // Adapter: maps ES BulkProcessor.Listener callbacks → neutral IndexBulkListener
        final BulkProcessor.Listener esAdapter = new BulkProcessor.Listener() {
            @Override
            public void beforeBulk(final long executionId, final BulkRequest request) {
                listener.beforeBulk(executionId, request.numberOfActions());
            }

            @Override
            public void afterBulk(final long executionId, final BulkRequest request,
                    final BulkResponse response) {
                final List<IndexBulkItemResult> results = new ArrayList<>();
                for (final BulkItemResponse item : response) {
                    final DocWriteResponse itemResponse = item.getResponse();
                    if (item.isFailed() || itemResponse == null) {
                        results.add(ImmutableIndexBulkItemResult.builder()
                                .id(item.getFailure().getId())
                                .failed(true)
                                .failureMessage(item.getFailure().getMessage())
                                .build());
                    } else {
                        results.add(ImmutableIndexBulkItemResult.builder()
                                .id(itemResponse.getId())
                                .failed(false)
                                .build());
                    }
                }
                listener.afterBulk(executionId, results);
            }

            @Override
            public void afterBulk(final long executionId, final BulkRequest request,
                    final Throwable failure) {
                listener.afterBulk(executionId, failure);
            }
        };

        final BulkProcessor processor = BulkProcessor.builder(
                (request, bulkListener) ->
                        RestHighLevelClientProvider.getInstance().getClient()
                                .bulkAsync(request, RequestOptions.DEFAULT, bulkListener),
                esAdapter)
                .setBulkActions(numberToReindexInRequest)
                .setBulkSize(new ByteSizeValue(ReindexThread.ELASTICSEARCH_BULK_SIZE, ByteSizeUnit.MB))
                .setConcurrentRequests(ELASTICSEARCH_CONCURRENT_REQUESTS)
                .setBackoffPolicy(BackoffPolicy.constantBackoff(
                        TimeValue.timeValueSeconds(ReindexThread.BACKOFF_POLICY_TIME_IN_SECONDS),
                        ReindexThread.BACKOFF_POLICY_MAX_RETRYS))
                .build();

        return new ESIndexBulkProcessor(processor);
    }

    @Override
    public void addIndexOpToProcessor(final IndexBulkProcessor proc, final String indexName,
            final String docId, final String jsonMapping) {
        asBulkProcessor(proc).add(
                new IndexRequest(indexName, "_doc", docId)
                        .source(jsonMapping, XContentType.JSON));
    }

    @Override
    public void addDeleteOpToProcessor(final IndexBulkProcessor proc, final String indexName,
            final String docId) {
        asBulkProcessor(proc).add(new DeleteRequest(indexName, "_doc", docId));
    }

    // =========================================================================
    // Other write operations
    // =========================================================================

    @Override
    public void removeContentFromIndexByContentType(final ContentType contentType)
            throws DotDataException {
        final String structureName = contentType.variable();
        final IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();

        final List<String> idxs = new java.util.ArrayList<>();
        idxs.add(info.getWorking());
        idxs.add(info.getLive());
        if (info.getReindexWorking() != null) {
            idxs.add(info.getReindexWorking());
        }
        if (info.getReindexLive() != null) {
            idxs.add(info.getReindexLive());
        }

        final DeleteByQueryRequest request = new DeleteByQueryRequest(
                idxs.toArray(new String[0]));
        request.setQuery(QueryBuilders.matchQuery("contenttype", structureName.toLowerCase()));
        request.setTimeout(new TimeValue(INDEX_OPERATIONS_TIMEOUT_IN_MS));

        final BulkByScrollResponse response = Sneaky.sneak(
                () -> RestHighLevelClientProvider.getInstance().getClient()
                        .deleteByQuery(request, RequestOptions.DEFAULT));

        Logger.info(this, "Records deleted: " + response.getDeleted()
                + " from contentType: " + structureName);
    }

    @Override
    public long getIndexDocumentCount(final String indexName) {
        final CountRequest countRequest = new CountRequest(indexName);
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        countRequest.source(searchSourceBuilder);

        final CountResponse countResponse = Sneaky.sneak(
                () -> RestHighLevelClientProvider.getInstance().getClient()
                        .count(countRequest, RequestOptions.DEFAULT));

        return countResponse.getCount();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private static BulkRequest asBulkRequest(final IndexBulkRequest req) {
        return ((ESIndexBulkRequest) req).delegate;
    }

    private static BulkProcessor asBulkProcessor(final IndexBulkProcessor proc) {
        return ((ESIndexBulkProcessor) proc).delegate;
    }
}