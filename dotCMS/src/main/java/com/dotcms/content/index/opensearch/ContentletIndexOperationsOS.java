package com.dotcms.content.index.opensearch;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.elasticsearch.business.ContentletIndexOperationsES;
import com.dotcms.content.index.ContentletIndexOperations;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.domain.ImmutableIndexBulkItemResult;
import com.dotcms.content.index.domain.IndexBulkItemResult;
import com.dotcms.content.index.domain.IndexBulkListener;
import com.dotcms.content.index.domain.IndexBulkProcessor;
import com.dotcms.content.index.domain.IndexBulkRequest;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.query_dsl.QueryStringQuery;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.core.CountResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.bulk.DeleteOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;

/**
 * OpenSearch implementation of {@link ContentletIndexOperations}.
 *
 * <p>All OpenSearch-specific types ({@code org.opensearch.client.*}) are confined to this
 * class. The public API exposes only {@link IndexBulkRequest} and {@link IndexBulkProcessor}
 * handles to callers, keeping the router and callers fully library-agnostic.</p>
 *
 * <p>Methods that have no OpenSearch equivalent yet are stubbed with
 * {@link UnsupportedOperationException} and flagged for follow-up implementation.</p>
 *
 * @author Fabrizio Araya
 * @see ContentletIndexOperationsES
 */
@ApplicationScoped
public class ContentletIndexOperationsOS implements ContentletIndexOperations {

    private static final ObjectMapper OBJECT_MAPPER = DotObjectMapperProvider.createDefaultMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Inject
    private OSClientProvider clientProvider;

    /**
     * No-arg constructor required by CDI for proxy creation.
     * {@code clientProvider} is injected via field injection after construction.
     */
    public ContentletIndexOperationsOS() {
        // CDI no-arg constructor
    }

    /** Package-private constructor for testing. */
    ContentletIndexOperationsOS(final OSClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    private OSClientProvider getClientProvider() {
        return clientProvider;
    }

    // =========================================================================
    // Inner wrappers — vendor types stay inside this class
    // =========================================================================

    /**
     * Wraps an OpenSearch bulk request builder behind the neutral {@link IndexBulkRequest} handle.
     * The builder is mutable: operations are accumulated before the final
     * {@link BulkRequest} is built and submitted in {@link #putToIndex}.
     */
    static final class OSIndexBulkRequest implements IndexBulkRequest {
        final List<BulkOperation> operations = new ArrayList<>();
        Refresh refresh = null; // null means "use server default" (no refresh)

        @Override
        public int size() {
            return operations.size();
        }
    }

    /**
     * {@link IndexBulkProcessor} for OpenSearch.
     *
     * <p>{@code opensearch-java} 3.x does not ship a built-in BulkProcessor equivalent, so
     * this class implements the same behaviour manually: operations accumulate in a pending
     * list and are flushed to OpenSearch whenever the list reaches {@code maxActions} or when
     * {@link #close()} is called. The supplied {@link IndexBulkListener} receives
     * {@code beforeBulk} / {@code afterBulk} callbacks around each flush, mirroring the
     * contract provided by the Elasticsearch {@code BulkProcessor.Listener} adapter in
     * {@link ContentletIndexOperationsES}.</p>
     */
    static final class OSIndexBulkProcessor implements IndexBulkProcessor {

        private final List<BulkOperation> pending = new ArrayList<>();
        private final OSClientProvider clientProvider;
        private final IndexBulkListener listener;
        private final int maxActions;
        private final AtomicLong executionIdCounter = new AtomicLong(0);

        OSIndexBulkProcessor(final OSClientProvider clientProvider,
                final IndexBulkListener listener, final int maxActions) {
            this.clientProvider = clientProvider;
            this.listener = listener;
            this.maxActions = maxActions;
        }

        synchronized void addAndMaybeFlush(final BulkOperation op) {
            pending.add(op);
            if (pending.size() >= maxActions) {
                flush();
            }
        }

        /**
         * Submits the current pending batch to OpenSearch and fires listener callbacks.
         * No-op when the pending list is empty. Synchronized on the same monitor as
         * {@link #addAndMaybeFlush} to prevent {@link #close()} from racing with an
         * auto-flush: without the lock, ops added after close() drains the list
         * would silently never reach OpenSearch.
         */
        synchronized void flush() {
            if (pending.isEmpty()) {
                return;
            }
            final long executionId = executionIdCounter.incrementAndGet();
            final List<BulkOperation> batch = new ArrayList<>(pending);
            pending.clear();

            listener.beforeBulk(executionId, batch.size());

            try {
                final OpenSearchClient client = clientProvider.getClient();
                final BulkResponse response = client.bulk(
                        BulkRequest.of(b -> b.operations(batch)));

                final List<IndexBulkItemResult> results = new ArrayList<>(response.items().size());
                for (final BulkResponseItem item : response.items()) {
                    final boolean failed = item.error() != null;
                    final String failureMessage = failed
                            ? item.error().type() + ": " + item.error().reason()
                            : null;
                    results.add(ImmutableIndexBulkItemResult.builder()
                            .id(item.id() != null ? item.id() : "")
                            .failed(failed)
                            .failureMessage(failureMessage)
                            .build());
                }
                listener.afterBulk(executionId, results);

            } catch (final Exception e) {
                listener.afterBulk(executionId, e);
            }
        }

        @Override
        public void close() throws Exception {
            flush();
        }
    }

    // =========================================================================
    // Batch write path
    // =========================================================================

    @Override
    public IndexBulkRequest createBulkRequest() {
        return new OSIndexBulkRequest();
    }

    @Override
    public void addIndexOp(final IndexBulkRequest req, final String indexName,
            final String docId, final String jsonMapping) {
        asBulkRequest(req).operations.add(BulkOperation.of(op -> op
                .index(IndexOperation.of(io -> io
                        .index(indexName)
                        .id(docId)
                        .document(parseJsonToMap(jsonMapping))))));
    }

    @Override
    public void addDeleteOp(final IndexBulkRequest req, final String indexName,
            final String docId) {
        asBulkRequest(req).operations.add(BulkOperation.of(op -> op
                .delete(DeleteOperation.of(del -> del
                        .index(indexName)
                        .id(docId)))));
    }

    @Override
    public void setRefreshPolicy(final IndexBulkRequest req,
            final IndexBulkRequest.RefreshPolicy policy) {
        final Refresh osRefresh;
        switch (policy) {
            case IMMEDIATE: osRefresh = Refresh.True;    break;
            case WAIT_FOR:  osRefresh = Refresh.WaitFor; break;
            default:        osRefresh = Refresh.False;   break;
        }
        asBulkRequest(req).refresh = osRefresh;
    }

    @Override
    public void putToIndex(final IndexBulkRequest req) {
        final OSIndexBulkRequest osReq = asBulkRequest(req);
        if (osReq.operations.isEmpty()) {
            return;
        }
        try {
            final OpenSearchClient client = getClientProvider().getClient();
            final Refresh refresh = osReq.refresh;
            final BulkResponse response = client.bulk(
                    BulkRequest.of(b -> {
                        b.operations(osReq.operations);
                        if (refresh != null) {
                            b.refresh(refresh);
                        }
                        return b;
                    }));
            if (response.errors()) {
                Logger.error(this,
                        "OS bulk putToIndex: errors in response for "
                                + osReq.operations.size() + " operations");
            }
        } catch (final Exception e) {
            Logger.warnAndDebug(ContentletIndexOperationsOS.class, e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    // =========================================================================
    // Async bulk-processor write path
    // =========================================================================

    @Override
    public IndexBulkProcessor createBulkProcessor(final IndexBulkListener listener) {
        int maxActions = ReindexThread.ELASTICSEARCH_BULK_ACTIONS;
        try {
            final int servers = APILocator.getServerAPI().getReindexingServers().size();
            if (servers > 0) {
                maxActions = ReindexThread.ELASTICSEARCH_BULK_ACTIONS / servers;
            }
        } catch (final Exception e) {
            Logger.warnAndDebug(ContentletIndexOperationsOS.class,
                    "Could not determine reindexing server count; using default bulk actions: "
                            + maxActions, e);
        }
        return new OSIndexBulkProcessor(clientProvider, listener, maxActions);
    }

    @Override
    public void addIndexOpToProcessor(final IndexBulkProcessor proc, final String indexName,
            final String docId, final String jsonMapping) {
        asBulkProcessor(proc).addAndMaybeFlush(BulkOperation.of(op -> op
                .index(IndexOperation.of(io -> io
                        .index(indexName)
                        .id(docId)
                        .document(parseJsonToMap(jsonMapping))))));
    }

    @Override
    public void addDeleteOpToProcessor(final IndexBulkProcessor proc, final String indexName,
            final String docId) {
        asBulkProcessor(proc).addAndMaybeFlush(BulkOperation.of(op -> op
                .delete(DeleteOperation.of(del -> del
                        .index(indexName)
                        .id(docId)))));
    }

    // =========================================================================
    // Other write operations
    // =========================================================================

    @Override
    public void removeContentFromIndexByContentType(final ContentType contentType)
            throws DotDataException {
        final String structureName = contentType.variable();
        final VersionedIndices info =
                APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices()
                        .orElseThrow(() -> new DotDataException(
                                "No versioned indices found — cannot remove content type '"
                                        + contentType.variable() + "' from OS index"));

        final List<String> indices = new ArrayList<>();
        info.working().ifPresent(indices::add);
        info.live().ifPresent(indices::add);
        info.reindexWorking().ifPresent(indices::add);
        info.reindexLive().ifPresent(indices::add);

        try {
            final OpenSearchClient client = getClientProvider().getClient();
            for (final String indexName : indices) {
                final org.opensearch.client.opensearch.core.DeleteByQueryRequest deleteByQuery =
                        org.opensearch.client.opensearch.core.DeleteByQueryRequest.of(r -> r
                                .index(indexName)
                                .query(q -> q.queryString(
                                        QueryStringQuery.of(qs -> qs.query(
                                                "contenttype:" + structureName.toLowerCase())))));
                final org.opensearch.client.opensearch.core.DeleteByQueryResponse response =
                        client.deleteByQuery(deleteByQuery);
                Logger.info(this, "OS: Deleted " + response.deleted()
                        + " records of contentType " + structureName
                        + " from index " + indexName);
            }
        } catch (final Exception e) {
            throw new DotDataException("Error removing content type from OS index: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public long getIndexDocumentCount(final String indexName) {
        try {
            final OpenSearchClient client = getClientProvider().getClient();
            final CountResponse response = client.count(
                    CountRequest.of(r -> r.index(indexName)));
            return response.count();
        } catch (final Exception e) {
            Logger.warnAndDebug(ContentletIndexOperationsOS.class, e);
            throw new DotRuntimeException(
                    "Error getting document count from OS index: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private static OSIndexBulkRequest asBulkRequest(final IndexBulkRequest req) {
        if (!(req instanceof OSIndexBulkRequest)) {
            throw new DotRuntimeException(
                    "Expected OSIndexBulkRequest but got: " + req.getClass().getName());
        }
        return (OSIndexBulkRequest) req;
    }

    private static OSIndexBulkProcessor asBulkProcessor(final IndexBulkProcessor proc) {
        if (!(proc instanceof OSIndexBulkProcessor)) {
            throw new DotRuntimeException(
                    "Expected OSIndexBulkProcessor but got: " + proc.getClass().getName());
        }
        return (OSIndexBulkProcessor) proc;
    }

    /**
     * Parses a JSON string into a {@code Map<String,Object>} for use as the OpenSearch
     * document source. Uses the same Jackson mapper already available in the codebase.
     */
    private static Map<String, Object> parseJsonToMap(final String jsonMapping) {
        return Sneaky.sneak(() -> OBJECT_MAPPER.readValue(jsonMapping, MAP_TYPE));
    }
}