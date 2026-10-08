package com.dotcms.content.index.opensearch;

import com.dotcms.content.elasticsearch.business.ContentletIndexOperationsES;
import com.dotcms.content.index.ContentletIndexOperations;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.domain.CreateIndexStatus;
import java.io.IOException;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.util.DateUtil;
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
import com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
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
    private static final String OS_SETTINGS_FILE = "os-content-settings.json";
    /** OpenSearch accepts the same content mapping format as Elasticsearch. */
    private static final String CONTENT_MAPPING_FILE = "os-content-mapping.json";

    @Inject
    private OSClientProvider clientProvider;

    @Inject
    private OSIndexAPIImpl osIndexAPI;

    @Inject
    private MappingOperationsOS mappingOps;

    /**
     * No-arg constructor required by CDI for proxy creation.
     * All fields are injected via CDI field injection after construction.
     */
    public ContentletIndexOperationsOS() {
        // CDI no-arg constructor
    }

    /** Package-private constructor for testing. */
    ContentletIndexOperationsOS(final OSClientProvider clientProvider,
            final OSIndexAPIImpl osIndexAPI,
            final MappingOperationsOS mappingOps) {
        this.clientProvider = clientProvider;
        this.osIndexAPI     = osIndexAPI;
        this.mappingOps     = mappingOps;
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
     * Estimated size of a delete operation in a bulk request: it carries only its action line
     * (index and id), no document. Counted so that a run of deletes still contributes to the byte
     * limit, without measuring each one.
     */
    static final long DELETE_OPERATION_SIZE = 100L;

    /**
     * {@link IndexBulkProcessor} for OpenSearch.
     *
     * <p>{@code opensearch-java} 3.x does not ship a built-in BulkProcessor equivalent, so
     * this class implements the same behaviour manually: operations accumulate in a pending
     * list and are flushed to OpenSearch whenever the list reaches {@code maxActions} or
     * {@code maxBytes} of estimated size (as the Elasticsearch {@code BulkProcessor}'s
     * {@code setBulkSize}, #37905), or when {@link #close()} is called. The supplied {@link IndexBulkListener} receives
     * {@code beforeBulk} / {@code afterBulk} callbacks around each flush, mirroring the
     * contract provided by the Elasticsearch {@code BulkProcessor.Listener} adapter in
     * {@link ContentletIndexOperationsES}.</p>
     */
    static final class OSIndexBulkProcessor implements IndexBulkProcessor {

        private final List<BulkOperation> pending = new ArrayList<>();
        private final OSClientProvider clientProvider;
        private final IndexBulkListener listener;
        private final int maxActions;
        /** Byte limit of one batch; {@code <= 0} disables it (count-only batching). */
        private final long maxBytes;
        /** Estimated size of {@link #pending}, in characters counted as bytes. */
        private long pendingSize;
        private final AtomicLong executionIdCounter = new AtomicLong(0);

        /**
         * Creates a processor that sends a batch when it reaches {@code maxActions} operations or
         * {@code maxBytes} of estimated size, whichever comes first (#37905).
         *
         * @param clientProvider supplies the OpenSearch client used to send each batch
         * @param listener       receives {@code beforeBulk} / {@code afterBulk} around each batch
         * @param maxActions     the most operations one batch may hold
         * @param maxBytes       the most estimated bytes one batch may hold; {@code <= 0} disables
         *                       the byte limit
         */
        OSIndexBulkProcessor(final OSClientProvider clientProvider,
                final IndexBulkListener listener, final int maxActions, final long maxBytes) {
            this.clientProvider = clientProvider;
            this.listener = listener;
            this.maxActions = maxActions;
            this.maxBytes = maxBytes;
        }

        /**
         * Queues an operation, sending the pending batch first if this operation would push it
         * over the byte limit, and sending the batch afterwards once it reaches the count or the
         * byte limit. An operation larger than the byte limit on its own is therefore sent in a
         * batch of one; it is never split or dropped.
         *
         * @param op   the operation to queue
         * @param size the operation's estimated size (characters of its JSON counted as bytes)
         */
        synchronized void addAndMaybeFlush(final BulkOperation op, final long size) {
            final boolean byteLimited = maxBytes > 0;
            if (byteLimited && !pending.isEmpty() && pendingSize + size > maxBytes) {
                flush();
            }
            pending.add(op);
            pendingSize += size;
            if (pending.size() >= maxActions || (byteLimited && pendingSize >= maxBytes)) {
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
            pendingSize = 0;

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
    // Index lifecycle
    // =========================================================================


    @Override
    public IndexAPI indexAPI() {
        return osIndexAPI;
    }

    /**
     * Returns the physical OS index name: cluster-ID prefix + logical name + {@code .os} suffix.
     *
     * <p>Example: {@code working_20230101} → {@code cluster_e0f4fa027f.working_20230101.os}</p>
     * <p>Idempotent: names that already end in {@code .os} are returned unchanged.</p>
     */
    @Override
    public String toPhysicalName(final String indexName) {
        final String clustered = indexAPI().getNameWithClusterIDPrefix(indexName);
        return IndexTag.OS.isTagged(clustered) ? clustered : IndexTag.OS.tag(clustered);
    }

    @Override
    public boolean createContentIndex(final String indexName, final int shards)
            throws IOException {
        final String settings = JsonUtil.getJsonFileContentAsString(OS_SETTINGS_FILE);

        final String mapping = JsonUtil.getJsonFileContentAsString(CONTENT_MAPPING_FILE);
        final CreateIndexStatus status = osIndexAPI.createIndex(indexName, settings, shards);

        int i = 0;
        while (!status.acknowledged()) {
            DateUtil.sleep(100);
            if (i++ > 300) {
                throw new IOException("OS index creation timed out for: " + indexName);
            }
        }

        mappingOps.putMapping(CollectionsUtils.list(indexName), mapping);
        return true;
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
            handleBulkResponse(response);
        } catch (final Exception e) {
            Logger.warnAndDebug(ContentletIndexOperationsOS.class, e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Decides what a bulk response means to the caller.
     *
     * <p>Extracted from {@link #putToIndex(IndexBulkRequest)} so the policy can be exercised
     * without a cluster — the HTTP call is not what is interesting here, the verdict is.</p>
     *
     * @param response the response from the bulk call; {@code null} is tolerated
     */
    @VisibleForTesting
    void handleBulkResponse(final BulkResponse response) {
        if (response == null || !response.errors()) {
            return;
        }

        // Same contract as the Elasticsearch provider (#37276, loss point L3): a bulk that
        // returns normally while rejecting items must not read as success to the caller.
        //
        // This matters most in phase 3, where OpenSearch is the sole provider and there is no
        // shadow leg to absorb the loss. In dual-write phases the router already isolates the
        // shadow (ContentletIndexAPIImpl#putToIndex), so raising here keeps ADR-0009 intact:
        // an OS failure is still swallowed while OS is the shadow, and propagates once primary.
        final StringBuilder detail = new StringBuilder();
        for (final BulkResponseItem item : response.items()) {
            if (item.error() != null) {
                final String itemMessage = "OS bulk index operation error — id=" + item.id()
                        + " op=" + item.operationType()
                        + " type=" + item.error().type()
                        + " reason=" + item.error().reason();
                Logger.error(this, itemMessage);
                if (detail.length() > 0) {
                    detail.append("; ");
                }
                detail.append(itemMessage);
            }
        }

        // errors() is expected to imply at least one item carrying an error, but if that ever
        // stops holding we must not hand the caller a blank exception — a failure with no
        // message is barely better than the silent return this replaced.
        throw new DotRuntimeException(detail.length() > 0
                ? detail.toString()
                : "OS bulk reported errors but no item carried an error cause");
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
        return new OSIndexBulkProcessor(clientProvider, listener, maxActions,
                resolveReindexBulkMaxBytes());
    }

    /** Byte limit used when neither OpenSearch nor Elasticsearch sets a positive one, in MB. */
    static final int DEFAULT_REINDEX_BULK_SIZE_MB = 10;

    private static final long BYTES_PER_MB = 1_048_576L;

    /** How often the "byte limit disabled" warning may repeat: once an hour. */
    private static final int DISABLED_LIMIT_WARN_EVERY_MILLIS = 60 * 60 * 1000;

    /**
     * Resolves the byte limit of one OpenSearch reindex bulk request (#37905).
     *
     * <ul>
     *   <li>{@code OS_REINDEX_BULK_SIZE_MB} set and positive: that value.</li>
     *   <li>{@code OS_REINDEX_BULK_SIZE_MB} set to {@code 0} or less: no byte limit (batches close
     *       by count only, as before #37905), logged at WARN at most once an hour.</li>
     *   <li>Not set: {@code REINDEX_THREAD_ELASTICSEARCH_BULK_SIZE} if positive, otherwise
     *       {@value #DEFAULT_REINDEX_BULK_SIZE_MB} MB. A disabled Elasticsearch value ({@code -1})
     *       is deliberately not inherited: it would silently bring back requests large enough for
     *       OpenSearch to reject (HTTP 413) and to exhaust the heap while being built.</li>
     * </ul>
     *
     * <p>Sizes are estimates — each document's JSON characters counted as bytes — so the limit
     * should be set well below the server's {@code http.max_content_length}.</p>
     *
     * @return the limit in bytes, or a value {@code <= 0} when the byte limit is disabled
     */
    static long resolveReindexBulkMaxBytes() {
        final OSIndexProperty property = OSIndexProperty.REINDEX_BULK_SIZE_MB;
        if (Config.getStringProperty(property.osKey, null) != null) {
            final int osValue = Config.getIntProperty(property.osKey, DEFAULT_REINDEX_BULK_SIZE_MB);
            if (osValue <= 0) {
                // A processor is created per reindex iteration: throttle so the warning is seen
                // without repeating every few seconds.
                Logger.warnEvery(ContentletIndexOperationsOS.class, property.osKey,
                        property.osKey + "=" + osValue
                        + " disables the size limit of OpenSearch reindex bulk requests: batches"
                        + " close by document count only and can exceed the server's"
                        + " http.max_content_length (HTTP 413) or exhaust the heap.",
                        DISABLED_LIMIT_WARN_EVERY_MILLIS);
                return osValue;
            }
            return osValue * BYTES_PER_MB;
        }
        if (Config.getStringProperty(property.esFallback, null) != null) {
            final int esValue = Config.getIntProperty(property.esFallback,
                    DEFAULT_REINDEX_BULK_SIZE_MB);
            if (esValue > 0) {
                return esValue * BYTES_PER_MB;
            }
        }
        return DEFAULT_REINDEX_BULK_SIZE_MB * BYTES_PER_MB;
    }

    @Override
    public void addIndexOpToProcessor(final IndexBulkProcessor proc, final String indexName,
            final String docId, final String jsonMapping) {
        asBulkProcessor(proc).addAndMaybeFlush(BulkOperation.of(op -> op
                .index(IndexOperation.of(io -> io
                        .index(indexName)
                        .id(docId)
                        .document(parseJsonToMap(jsonMapping))))),
                jsonMapping.length());
    }

    @Override
    public void addDeleteOpToProcessor(final IndexBulkProcessor proc, final String indexName,
            final String docId) {
        asBulkProcessor(proc).addAndMaybeFlush(BulkOperation.of(op -> op
                .delete(DeleteOperation.of(del -> del
                        .index(indexName)
                        .id(docId)))),
                DELETE_OPERATION_SIZE);
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
                final String physical = toPhysicalName(indexName);
                final org.opensearch.client.opensearch.core.DeleteByQueryRequest deleteByQuery =
                        org.opensearch.client.opensearch.core.DeleteByQueryRequest.of(r -> r
                                .index(physical)
                                // Proceed past version conflicts instead of aborting: a document
                                // updated concurrently (e.g. just published) would otherwise return
                                // HTTP 409 version_conflict_engine_exception and fail the whole
                                // content-type removal, which callers swallow — orphaning downstream
                                // cleanup (e.g. the unique_fields table). Matches ES delete_by_query
                                // tolerance and keeps the operation best-effort.
                                .conflicts(org.opensearch.client.opensearch._types.Conflicts.Proceed)
                                .query(q -> q.queryString(
                                        QueryStringQuery.of(qs -> qs.query(
                                                "contenttype:" + structureName.toLowerCase())))));
                final org.opensearch.client.opensearch.core.DeleteByQueryResponse response =
                        client.deleteByQuery(deleteByQuery);
                Logger.info(this, "OS: Deleted " + response.deleted()
                        + " records of contentType " + structureName
                        + " from index " + physical);
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