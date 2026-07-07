package com.dotcms.content.elasticsearch.business;

import com.dotcms.content.index.domain.IndexBulkListener;
import com.dotcms.content.index.domain.IndexBulkProcessor;
import com.dotcms.content.index.domain.IndexBulkRequest;
import com.dotcms.content.index.domain.IndexStartResult;
import com.dotcms.content.model.annotation.IndexLibraryIndependent;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.common.reindex.ReindexEntry;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.io.IOException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Vendor-neutral API for contentlet indexing operations.
 *
 * <p>No Elasticsearch or OpenSearch types appear in any method signature.
 * Bulk-write callers use the opaque {@link IndexBulkRequest} and
 * {@link IndexBulkProcessor} handles returned by this interface.</p>
 */

@IndexLibraryIndependent
public interface ContentletIndexAPI {

    @Deprecated(forRemoval = true)
    SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyyMMddHHmmss");

    /** Thread-safe formatter for index timestamp suffixes ({@code yyyyMMddHHmmss}). */
    DateTimeFormatter threadSafeTimestampFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    void checkAndInitializeIndex();

    boolean createContentIndex(String indexName) throws DotIndexException, IOException;

    boolean createContentIndex(String indexName, int shards) throws DotIndexException, IOException;

    /**
     * Creates new working and live indexes with reading aliases pointing to old index and write
     * aliases pointing to both old and new indexes.
     *
     * @return the timestamp string used as suffix for indices
     * @throws DotDataException
     * @throws DotIndexException
     */
    IndexStartResult fullReindexStart() throws DotIndexException, DotDataException;

    /**
     * Returns {@code true} if the system is currently in a full reindex.
     *
     * @throws DotDataException
     */
    boolean isInFullReindex() throws DotDataException;

    /**
     * Drops the old index and points read aliases to the new index.
     * Pass {@code forceSwitch=true} to override the lucky-server check.
     */
    boolean fullReindexSwitchover(final boolean forceSwitch);

    boolean fullReindexSwitchover(Connection conn, final boolean forceSwitch);

    /**
     * Deletes an index by name.
     *
     * @param indexName
     * @return
     */
    boolean delete(String indexName);

    /**
     * Rejects a destructive operation on an index that is currently active (working/live) or being
     * rebuilt (a reindex slot), so that no path — REST, legacy AJAX, or {@code clear} — can wipe the
     * only index dotCMS serves reads from (issue #35640, TC-018). The protected set is collected
     * phase-aware, unioning the ES and OS stores in the dual-write phases. Bypassable via the
     * {@code FEATURE_FLAG_ALLOW_ACTIVE_INDEX_DELETE} feature flag.
     *
     * @param indexName the index being targeted (bare or {@code .os}-tagged, with or without the
     *                  cluster prefix — normalized internally)
     * @param operation past-tense verb for the error message, e.g. {@code "deleted"} / {@code "cleared"}
     * @throws com.dotmarketing.business.DotStateException if the index is active/building (bypass
     *                  off), or if the active set cannot be resolved (fail closed)
     */
    void assertIndexNotActive(String indexName, String operation);

    /**
     * Optimizes shards for a list of indices.
     *
     * @param indexNames
     * @return
     */
    boolean optimize(List<String> indexNames);

    void removeContentFromIndex(final Contentlet content) throws DotDataException;

    void removeContentFromIndex(final Contentlet content, final boolean onlyLive)
            throws DotDataException;

    void removeContentFromLiveIndex(final Contentlet content) throws DotDataException;

    void removeContentFromIndexByStructureInode(String structureInode)
            throws DotDataException, DotSecurityException;

    void removeContentFromIndexByContentType(final ContentType contentType)
            throws DotDataException;

    void fullReindexAbort();

    boolean isDotCMSIndexName(String indexName);

    /**
     * Returns a list of dotCMS working and live indices.
     *
     * @return
     */
    List<String> listDotCMSIndices();

    void activateIndex(String indexName) throws DotDataException;

    void deactivateIndex(String indexName) throws DotDataException, IOException;

    /**
     * Gets the document count of a given index.
     * Throws a runtime exception if the index does not exist.
     *
     * @param indexName
     * @return Documents count - long
     */
    long getIndexDocumentCount(String indexName);

    List<String> getCurrentIndex() throws DotDataException;

    List<String> getNewIndex() throws DotDataException;

    List<String> listDotCMSClosedIndices();

    String getActiveIndexName(String type) throws DotDataException;

    /**
     * Sets the refresh policy on a bulk batch before submission.
     *
     * @param bulkRequest the batch to configure
     * @param policy      one of {@code "NONE"}, {@code "WAIT_FOR"}, or {@code "IMMEDIATE"}
     */
    void setRefreshPolicy(IndexBulkRequest bulkRequest, IndexBulkRequest.RefreshPolicy policy);

    /**
     * Submits a bulk batch synchronously. The batch is obtained from
     * {@link #createBulkRequest()} and populated via {@link #appendBulkRequest}.
     *
     * @param bulkRequest the batch to submit — no-op if empty
     */
    void putToIndex(IndexBulkRequest bulkRequest);

    void addContentToIndex(List<Contentlet> contentToIndex) throws DotDataException;

    void addContentToIndex(Contentlet content) throws DotDataException;

    void addContentToIndex(Contentlet content, boolean deps) throws DotDataException;
    /**
     * Creates a batch pre-populated with the given contentlets.
     *
     * @param contentToIndex list of contentlets to index
     * @return a populated {@link IndexBulkRequest}
     * @throws DotDataException
     */
    IndexBulkRequest createBulkRequest(List<Contentlet> contentToIndex) throws DotDataException;

    /** Creates an empty batch. */
    IndexBulkRequest createBulkRequest();

    IndexBulkRequest appendBulkRequest(IndexBulkRequest bulkRequest,
            Collection<ReindexEntry> idxs) throws DotDataException;

    IndexBulkRequest appendBulkRequest(IndexBulkRequest bulkRequest,
            ReindexEntry idx) throws DotDataException;

    Optional<String> reindexTimeElapsed();

    void stopFullReindexationAndSwitchover() throws DotDataException;

    boolean reindexSwitchover(boolean forceSwitch) throws DotDataException;

    void stopFullReindexation() throws DotDataException;

    IndexBulkRequest appendBulkRemoveRequest(IndexBulkRequest bulkRequest,
            final ReindexEntry entry) throws DotDataException;

    /**
     * Creates a self-flushing async bulk processor for high-throughput reindexing.
     *
     * @param bulkListener receives batch-completion and failure callbacks
     * @return a new {@link IndexBulkProcessor}
     */
    IndexBulkProcessor createBulkProcessor(IndexBulkListener bulkListener);

    void appendToBulkProcessor(final IndexBulkProcessor bulk,
            final Collection<ReindexEntry> idxs) throws DotDataException;
}
