package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.haltMigration;
import static com.dotcms.content.index.IndexConfigHelper.isMigrationComplete;
import static com.dotcms.content.index.IndexConfigHelper.isMigrationNotStarted;
import static com.dotcms.content.index.IndexConfigHelper.isMigrationStarted;
import static com.dotcms.content.index.IndexConfigHelper.isReadEnabled;
import static com.dotcms.content.index.IndexConfigHelper.logShadowWriteFailure;
import static com.dotmarketing.util.StringUtils.builder;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.content.business.ContentMappingAPI;
import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.util.MappingHelper;
import com.dotcms.content.index.ContentletIndexOperations;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.IndexAPIImpl;
import com.dotcms.content.index.opensearch.IndexStartupValidator;
import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.PhaseRouter;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.VersionedIndicesAPI;
import com.dotcms.content.index.VersionedIndicesImpl;
import com.dotcms.content.index.domain.ImmutableIndexStartResult;
import com.dotcms.content.index.domain.IndexBulkListener;
import com.dotcms.content.index.domain.IndexBulkProcessor;
import com.dotcms.content.index.domain.IndexBulkRequest;
import com.dotcms.content.index.domain.IndexStartResult;
import com.dotcms.content.index.opensearch.ContentletIndexOperationsOS;
import com.dotcms.content.model.annotation.IndexLibraryIndependent;
import com.dotcms.content.model.annotation.IndexRouter;
import com.dotcms.content.model.annotation.IndexRouter.IndexAccess;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.BulkProcessorListener;
import com.dotmarketing.common.reindex.ReindexEntry;
import com.dotmarketing.common.reindex.ReindexQueueAPI;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.ReindexRunnable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ThreadUtils;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.control.Try;
import java.io.IOException;
import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Phase-aware router implementation of {@link ContentletIndexAPI}.
 *
 * <h2>Responsibility</h2>
 * <p>This class orchestrates <strong>all document-level write operations</strong>
 * (index, delete, bulk) across the two search backends during the ES → OS migration.
 * It does NOT contain vendor-specific logic; that lives in
 * {@link ContentletIndexOperationsES} and
 * {@link com.dotcms.content.index.opensearch.ContentletIndexOperationsOS}.</p>
 *
 * <h2>Routing table</h2>
 * <pre>
 * Phase                     | Read provider | Write providers
 * --------------------------|---------------|-----------------
 * 0 — not started           | ES            | [ES]
 * 1 — dual-write, ES reads  | ES            | [ES, OS]
 * 2 — dual-write, OS reads  | OS            | [ES, OS]
 * 3 — OS only               | OS            | [OS]
 * </pre>
 * <p>All phase decisions are delegated to {@link PhaseRouter} ({@code router} field).
 * Callers of this class never need to know which backend is active.</p>
 *
 * <h2>Write paths</h2>
 * <ol>
 *   <li><strong>Synchronous bulk-request path</strong> (primary):
 *       {@link #createBulkRequest()} → {@link #appendBulkRequest} →
 *       {@link #putToIndex(IndexBulkRequest)}. In dual-write phases, {@code createBulkRequest}
 *       returns a {@link DualIndexBulkRequest} that carries one native batch per provider.
 *       Subsequent calls on that handle are transparently fanned out to both providers.</li>
 *   <li><strong>Asynchronous bulk-processor path</strong> (ReindexThread):
 *       {@link #createBulkProcessor} → {@link #appendBulkRequest(IndexBulkProcessor, ReindexEntry)}.
 *       {@code createBulkProcessor} creates one inner processor per active <em>write provider</em>
 *       and wraps them in a {@link CompositeBulkProcessor}. In dual-write phases (1 and 2) both
 *       ES and OS processors receive writes; OS failures are fire-and-forget (shadow entries).
 *       In single-provider phases (0 and 3) the composite degenerates to a single entry.</li>
 * </ol>
 *
 * <h2>Index-name resolution</h2>
 * <p>ES and OS use different physical index names:
 * ES names come from {@link IndiciesAPI} (legacy); OS names come from
 * {@link VersionedIndicesAPI} (versioned). The {@link ProviderIndices} inner class
 * carries the four slots (working / live / reindex-working / reindex-live) for one provider,
 * loaded just-in-time by {@link #loadProviderIndices(ContentletIndexOperations)}.
 * Physical names never leak to callers.</p>
 *
 * @author Fabrizio Araya
 * @see PhaseRouter
 * @see ContentletIndexOperations
 * @see DualIndexBulkRequest
 */
@IndexLibraryIndependent
@IndexRouter(access = {IndexAccess.READ, IndexAccess.WRITE})
public class ContentletIndexAPIImpl implements ContentletIndexAPI {

    private static final String SELECT_CONTENTLET_VERSION_INFO =
            "select working_inode,live_inode from contentlet_version_info where identifier IN (%s)";
    @Nullable
    private final ReindexQueueAPI queueApi;
    private final IndexAPI indexAPI;
    private final IndiciesAPI legacyIndiciesAPI;
    private final VersionedIndicesAPI versionedIndicesAPI;
    private final AtomicReference<ContentMappingAPI> mappingAPI = new AtomicReference<>();

    /** ES vendor implementation — receives writes in phases 0, 1, 2; reads in phases 0, 1. */
    private final ContentletIndexOperations operationsES;
    /** OS vendor implementation — receives writes in phases 1, 2, 3; reads in phases 2, 3. */
    private final ContentletIndexOperations operationsOS;
    /**
     * Encapsulates all routing decisions.  Provides {@code readProvider()} (single provider)
     * and {@code writeProviders()} (one or two providers) based on the current migration phase.
     */
    private final PhaseRouter<ContentletIndexOperations> router;

    private static final ObjectMapper objectMapper = DotObjectMapperProvider.createDefaultMapper();

    public ContentletIndexAPIImpl() {
        this(new ContentletIndexOperationsES(),
             CDIUtils.getBeanThrows(ContentletIndexOperationsOS.class));
    }

    /** Package-private constructor for testing: injects only the two provider operations.
     *  Still calls APILocator for the remaining dependencies. */
    ContentletIndexAPIImpl(final ContentletIndexOperations operationsES,
            final ContentletIndexOperations operationsOS) {
        this.operationsES = operationsES;
        this.operationsOS = operationsOS;
        this.router       = new PhaseRouter<>(operationsES, operationsOS);
        queueApi = APILocator.getReindexQueueAPI();
        indexAPI = APILocator.getESIndexAPI();
        legacyIndiciesAPI = APILocator.getIndiciesAPI();
        versionedIndicesAPI = APILocator.getVersionedIndicesAPI();
        // mappingAPI is intentionally NOT initialized here to avoid a circular
        // dependency: ContentletIndexAPIImpl → ESMappingAPIImpl → FolderAPIImpl
        // → ContentletAPI → ESContentletAPIImpl → ContentletIndexAPIImpl (cycle).
        // Use getMappingAPI() for lazy initialization at first use.
    }

    /**
     * Full constructor for unit testing — injects all dependencies without calling
     * {@link com.dotmarketing.business.APILocator}, allowing fully isolated tests.
     *
     * @param operationsES  ES write operations provider
     * @param operationsOS  OS write operations provider
     * @param indexAPI       phase-aware index management API (controls list/cluster operations)
     * @param legacyIndiciesAPI  ES index pointer store (working/live slots)
     * @param versionedIndicesAPI  OS index pointer store (working/live slots)
     */
    ContentletIndexAPIImpl(
            final ContentletIndexOperations operationsES,
            final ContentletIndexOperations operationsOS,
            final IndexAPI indexAPI,
            final IndiciesAPI legacyIndiciesAPI,
            final VersionedIndicesAPI versionedIndicesAPI) {
        this.operationsES       = operationsES;
        this.operationsOS       = operationsOS;
        this.router             = new PhaseRouter<>(operationsES, operationsOS);
        this.queueApi           = null; // not needed for the methods under test
        this.indexAPI           = indexAPI;
        this.legacyIndiciesAPI  = legacyIndiciesAPI;
        this.versionedIndicesAPI = versionedIndicesAPI;
    }

    /**
     * Lazy initializer avoids circular reference Stackoverflow error.
     * Thread-safe: uses {@link AtomicReference#updateAndGet} to ensure
     * at most one instance is visible — the factory may execute more than once under
     * contention but the same singleton is always returned.
     *
     * @return ContentIndexMappingAPI
     */
    private ContentMappingAPI getMappingAPI() {
        return mappingAPI.updateAndGet(
                current -> current != null ? current : APILocator.getContentMappingAPI());
    }

    /**
     * Loads the working/live/reindex index names for a specific provider.
     *
     * <p>ES names come from {@link IndiciesAPI}; OS names come from
     * {@link VersionedIndicesAPI}. Returns {@code null} when the provider is OS
     * but no versioned-indices record exists yet.</p>
     */
    private ProviderIndices loadProviderIndices(final ContentletIndexOperations ops)
            throws DotDataException {
        if (ops == operationsES) {
            final IndiciesInfo info = legacyIndiciesAPI.loadIndicies();
            return new ProviderIndices(
                    info.getWorking(), info.getLive(),
                    info.getReindexWorking(), info.getReindexLive());
        }
        // OS provider
        final Optional<VersionedIndices> opt =
                Try.of(versionedIndicesAPI::loadDefaultVersionedIndices)
                   .getOrElse(Optional.empty());
        if (opt.isEmpty()) {
            Logger.debug(this, "OS provider: no versioned-indices record — skipping OS write");
            return null;
        }
        final VersionedIndices vi = opt.get();
        return new ProviderIndices(
                vi.working().orElse(null),
                vi.live().orElse(null),
                vi.reindexWorking().orElse(null),
                vi.reindexLive().orElse(null));
    }

    /**
     * Immutable carrier for the four index names (working, live, reindex-working, reindex-live)
     * that belong to a single provider.
     *
     * <p>Any slot may be {@code null} when the corresponding index is not active
     * (e.g., no reindex in progress, or OS reindex not yet supported).</p>
     */
    private static final class ProviderIndices {
        final String working;
        final String live;
        final String reindexWorking;
        final String reindexLive;

        ProviderIndices(final String working, final String live,
                final String reindexWorking, final String reindexLive) {
            this.working       = working;
            this.live          = live;
            this.reindexWorking = reindexWorking;
            this.reindexLive   = reindexLive;
        }

        /** All non-null active index names in this snapshot. */
        List<String> activeIndices() {
            final List<String> indices = new ArrayList<>(4);
            if (working       != null) indices.add(working);
            if (live          != null) indices.add(live);
            if (reindexWorking != null) indices.add(reindexWorking);
            if (reindexLive   != null) indices.add(reindexLive);
            return indices;
        }
    }

    /**
     * Like {@link #loadProviderIndices} but swallows checked exceptions and returns
     * {@code null} on failure so that it is safe to call inside lambdas.
     */
    private ProviderIndices loadProviderIndicesQuietly(final ContentletIndexOperations ops) {
        return Try.of(() -> loadProviderIndices(ops))
                .onFailure(e -> Logger.warn(this.getClass(),
                        "Could not load provider indices for " + ops.getClass().getSimpleName()
                                + " — writes to this provider will be skipped. Cause: " + e.getMessage()))
                .getOrElse((ProviderIndices) null);
    }

    /**
     * Dual-write wrapper: holds one {@link IndexBulkRequest} per provider so that
     * the router can keep ES and OS operations in separate native batches while
     * exposing a single handle to callers.
     *
     * <p>Only created by the router's {@link #createBulkRequest()} when in a
     * dual-write phase. Callers should treat it as opaque.</p>
     */
    static final class DualIndexBulkRequest implements IndexBulkRequest {
        final IndexBulkRequest esReq;
        final IndexBulkRequest osReq;

        DualIndexBulkRequest(final IndexBulkRequest esReq, final IndexBulkRequest osReq) {
            this.esReq = esReq;
            this.osReq = osReq;
        }

        /**
         * Returns the logical document count for this batch.
         *
         * <p>In dual-write phases both sub-requests hold operations for the same logical
         * documents — {@code esReq} and {@code osReq} are mirrors of each other.
         * Summing both would inflate the count by 2×, which would corrupt metrics,
         * thresholds, and log output. In Phase 1 (ES reads) {@code esReq.size()} is the
         * canonical count; once OS reads are enabled (Phase 2+) {@code osReq.size()} is
         * used instead. Both values track the same documents so the result is identical.</p>
         */
        @Override
        public int size() {
            if(isMigrationComplete() || isReadEnabled()) {
                return osReq.size();
            }
            return esReq.size();
        }
    }

    /**
     * Composite bulk processor that holds one inner {@link IndexBulkProcessor} per active
     * write provider, enabling dual-write in Phases 1 and 2 of the ES→OS migration.
     *
     * <p>Each entry is an {@code (ops, proc)} pair so that index operations are always
     * submitted through the {@link ContentletIndexOperations} instance that originally
     * created the processor — the vendor-specific client enforces this coupling.</p>
     *
     * <p>On {@link #close()}, all inner processors are flushed in order. If multiple
     * providers throw on close, the first exception is re-thrown and the rest are added
     * as suppressed exceptions.</p>
     */
    static final class CompositeBulkProcessor implements IndexBulkProcessor {

        static final class Entry {
            final ContentletIndexOperations ops;
            final IndexBulkProcessor proc;
            /**
             * {@code true} when OS is acting as the <em>shadow index</em> (Phases 1 and 2).
             *
             * <p>The shadow index replicates every ES write but is not yet the source of truth.
             * It transitions to the primary index in Phase 3, at which point this flag is
             * {@code false} and failures propagate normally.</p>
             *
             * <p>While {@code true}: failures are fire-and-forget — logged at warn level but
             * never re-thrown, so an OS flush error cannot mask a successful ES flush.</p>
             */
            final boolean shadow;

            Entry(final ContentletIndexOperations ops, final IndexBulkProcessor proc,
                    final boolean shadow) {
                this.ops    = ops;
                this.proc   = proc;
                this.shadow = shadow;
            }
        }

        private final List<Entry> entries;

        CompositeBulkProcessor(final List<Entry> entries) {
            this.entries = List.copyOf(entries);
        }

        List<Entry> entries() {
            return entries;
        }

        @Override
        public void close() throws Exception {
            Exception primaryFailure = null;
            for (final Entry entry : entries) {
                try {
                    entry.proc.close();
                } catch (final Exception e) {
                    if (entry.shadow) {
                        // OS shadow write — fire-and-forget: log divergence, do not propagate.
                        logShadowWriteFailure(CompositeBulkProcessor.class,
                                "OS shadow processor failed to flush on close — ES flush succeeded; "
                                        + "OS index may diverge until next reindex. Cause: "
                                        + e.getMessage(), e);
                    } else {
                        if (primaryFailure == null) {
                            primaryFailure = e;
                        } else {
                            primaryFailure.addSuppressed(e);
                        }
                    }
                }
            }
            if (primaryFailure != null) {
                throw primaryFailure;
            }
        }
    }

    /**
     * This checks to make sure that we have good live and working indexes set in the db and that
     * are available in the ES cluster
     *
     * @return
     * @throws DotDataException
     */
    @VisibleForTesting
    @CloseDBIfOpened
    public synchronized boolean indexReady() throws DotDataException {
        if (isMigrationNotStarted()) {
            return indexReadyES();
        }
        if (isMigrationComplete()) {
            // Phase 3: ES is decommissioned — only OS must be ready.
            // Calling indexReadyES() here would query a decommissioned cluster and
            // incorrectly trigger bootstrapAndPoint, recreating ES indices.
            return indexReadyOS();
        }
        // Phases 1 and 2: dual-write — both providers must be ready.
        return indexReadyES() && indexReadyOS();
    }

    /**
     * Returns {@code true} when the ES cluster holds both the working and live indices
     * recorded in {@link IndiciesAPI}.
     *
     * <p>Existence is verified directly against the ES provider, bypassing the
     * read-path {@link PhaseRouter}. This ensures the result reflects the actual
     * ES cluster state regardless of the current migration phase (0, 1, or 2).</p>
     *
     * <p>Applies to phases 0, 1, and 2.</p>
     */
    // ── LEGACY ES — remove after Phase 3 migration ────────────────────────────
    private boolean indexReadyES() throws DotDataException {
        final IndiciesInfo info = legacyIndiciesAPI.loadIndicies();
        final IndexAPIImpl impl = (IndexAPIImpl) indexAPI;

        final boolean hasWorking = Try.of(() -> impl.esImpl().indexExists(info.getWorking()))
                .getOrElse(false);
        final boolean hasLive    = Try.of(() -> impl.esImpl().indexExists(info.getLive()))
                .getOrElse(false);

        if (!hasWorking) {
            Logger.debug(this.getClass(), "-- ES: WORKING INDEX DOES NOT EXIST");
        }
        if (!hasLive) {
            Logger.debug(this.getClass(), "-- ES: LIVE INDEX DOES NOT EXIST");
        }
        return hasWorking && hasLive;
    }
    // ── END LEGACY ES ─────────────────────────────────────────────────────────

    /**
     * Returns {@code true} when the OS cluster holds both the working and live indices
     * recorded in {@link VersionedIndicesAPI}.
     *
     * <p>Existence is verified directly against the OS provider, bypassing the
     * read-path {@link PhaseRouter}. This ensures the result reflects the actual
     * OS cluster state regardless of the current migration phase (1, 2, or 3).
     * In Phase 1, the read provider is ES; without this bypass, the check would
     * silently query the ES cluster and return a false positive for OS indices.</p>
     *
     * <p>Applies to phases 1, 2, and 3.</p>
     */
    private boolean indexReadyOS() {
        final Optional<VersionedIndices> indicesOpt;
        indicesOpt = Try.of(versionedIndicesAPI::loadDefaultVersionedIndices)
                .getOrElse(Optional.empty());

        if (indicesOpt.isEmpty()) {
            Logger.debug(this.getClass(), "-- OS: NO VERSIONED INDICES RECORD FOUND");
            return false;
        }

        final IndexAPIImpl impl = (IndexAPIImpl) indexAPI;
        final VersionedIndices indices = indicesOpt.get();
        final boolean hasWorking = indices.working()
                .map(name -> Try.of(() -> impl.osImpl().indexExists(
                        operationsOS.toPhysicalName(name))).getOrElse(false))
                .orElse(false);
        final boolean hasLive = indices.live()
                .map(name -> Try.of(() -> impl.osImpl().indexExists(
                        operationsOS.toPhysicalName(name))).getOrElse(false))
                .orElse(false);

        if (!hasWorking) {
            Logger.debug(this.getClass(), "-- OS: WORKING INDEX DOES NOT EXIST");
        }
        if (!hasLive) {
            Logger.debug(this.getClass(), "-- OS: LIVE INDEX DOES NOT EXIST");
        }
        return hasWorking && hasLive;
    }

    /**
     * Inits the indexes and starts the reindex process if no indexes are found
     */
    @CloseDBIfOpened
    public synchronized void checkAndInitializeIndex() {
        try {
            if (isMigrationStarted()) {
                if (!IndexStartupValidator.validateIndexingConfig()) {
                    if (isMigrationComplete()) {
                        // Phase 3: ES is decommissioned. Rolling back to Phase 0 would route
                        // reads/writes to a potentially stale ES index — safer to abort loudly
                        // and let the operator decide rather than silently serve stale data.
                        throw new DotRuntimeException(
                                "OpenSearch startup validation failed in PHASE_3_OPENSEARCH_ONLY."
                                + " Cannot auto-rollback to ES (ES may be decommissioned or stale)."
                                + " Restore OS connectivity or manually reset FEATURE_FLAG_OPEN_SEARCH_PHASE,"
                                + " then restart dotCMS.");
                    }

                    Logger.error(this.getClass(), "OpenSearch migration halted: invalid configuration detected at startup."
                            + " Verify OS_ENDPOINTS, OS version, and FEATURE_FLAG_OPEN_SEARCH_PHASE,"
                            + " then restart dotCMS.");
                    haltMigration();
                }
            }

            // if we don't have a working index, create it
            if (!indexReady()) {
                Logger.info(this.getClass(), "No indexes found, creating live and working indexes");
                initIndex();
            }

            if(Config.getBooleanProperty("REINDEX_IF_NO_INDEXES_FOUND", true)) {
                reindexIfNoIndicesFound();
            }

        } catch (Exception e) {
            Logger.fatal(this.getClass(), "Failed to create new indexes:" + e.getMessage(),e);

            // During an active OpenSearch migration a half-initialised index store is worse
            // than a hard stop: the read provider would serve an unfinished or missing index
            // (surfacing downstream as "all shards failed"), and a Phase 3 validation failure
            // intentionally throws above (line ~530) only to be swallowed here. Fail loudly so
            // the operator restores connectivity/config and restarts, rather than silently
            // coming up broken. Legacy ES-only installs (migration not started) keep the
            // historical best-effort behaviour and still boot.
            if (isMigrationStarted()) {
                throw new DotRuntimeException(
                        "Index initialization failed during OpenSearch migration; aborting startup"
                        + " to avoid serving an unfinished index store. Cause: " + e.getMessage(), e);
            }
        }
    }

    private void reindexIfNoIndicesFound() throws DotDataException {

        // if there are indexes, but they are empty, start a reindex process
        if(hasEmptyIndices()) {
            DotConcurrentFactory.getInstance().getSubmitter().submit(() -> {
                try {
                    Logger.info(this.getClass(),
                            "No content found in index, starting reindex process in background thread.");
                    final ReindexQueueAPI reindexQueueAPI = APILocator.getReindexQueueAPI();
                    reindexQueueAPI.deleteFailedRecords();
                    reindexQueueAPI.addAllToReindexQueue();

                } catch (Throwable e) { // nosonar

                    Logger.error(this.getClass(), "Error starting reindex process", e);
                }
            });
        }
    }

    private boolean hasEmptyIndices() throws DotDataException {
        if (isMigrationComplete()) {
            // Phase 3: ES is decommissioned — only OS emptiness matters.
            return isOsWorkingIndexEmpty();
        }
        // Phases 0–2: only ES emptiness triggers a reindex. In phases 1/2 the OS index is
        // expectedly empty during catchup — triggering off it would cause an unnecessary full
        // reindex; the OS catchup happens via normal dual-write as content changes.
        return isEsWorkingIndexEmpty();
    }

    private boolean isEsWorkingIndexEmpty() throws DotDataException {
        final String workingIndex = legacyIndiciesAPI.loadIndicies().getWorking();
        return getIndexDocumentCount(workingIndex, IndexTag.ES) == 0;
    }

    private boolean isOsWorkingIndexEmpty() throws DotDataException {
        if (isMigrationNotStarted()) {
            return false;
        }
        return versionedIndicesAPI
                .loadDefaultVersionedIndices()
                .flatMap(VersionedIndices::working)
                .map(workingIndex -> getIndexDocumentCount(workingIndex, IndexTag.OS) == 0)
                .orElse(false);
    }

    public synchronized boolean createContentIndex(String indexName)
            throws IOException {
        return createContentIndex(indexName, 0);
    }

    @Override
    public synchronized boolean createContentIndex(final String indexName, final int shards)
            throws IOException {
        // Track the primary provider's result independently so a shadow failure in dual-write
        // phases (ES primary ok, OS shadow fails) does NOT prevent addCustomMapping from running
        // against the successfully-created primary index.
        final ContentletIndexOperations primary = router.readProvider();
        boolean primaryResult = false;
        for (final ContentletIndexOperations ops : router.writeProviders()) {
            final String physicalName = ops.toPhysicalName(indexName);
            try {
                final boolean r = ops.createContentIndex(physicalName, shards);
                if (ops == primary) {
                    primaryResult = r;
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), "Error while creating content index " + physicalName, e);
                if (ops == primary) {
                    primaryResult = false;
                }
                // shadow failures are fire-and-forget in dual-write phases
            }
        }
        if (primaryResult) {
            MappingHelper.getInstance().addCustomMapping(indexName);
        }
        return primaryResult;
    }


    /**
     * Create an index exclusively in one of the SE Providers.
     *
     * <p><b>Idempotent bootstrap.</b> If the physical index already exists in the target
     * cluster it is an <em>orphan</em> — present in the cluster but missing from the index
     * store — left behind when a previous bootstrap created the index but never committed its
     * store pointer (e.g. the OS {@code VersionedIndices} row, or after a partial/interrupted
     * startup). Without handling this, the restart re-derives the same logical name, the create
     * fails with {@code resource_already_exists}, and {@code checkAndInitializeIndex()} aborts —
     * leaving the instance half-initialised.</p>
     *
     * <p>The orphan is handled by document count, so a populated index is never discarded:</p>
     * <ul>
     *   <li><b>Empty orphan (0 docs)</b> — deleted and recreated from scratch. In-place reuse
     *       cannot fully repair a bare orphan: the content mapping references a custom analyzer
     *       ({@code my_analyzer}) defined in the provider settings file, and analyzers are
     *       <em>static</em> index settings that can only be applied at creation time — so a
     *       {@code putMapping}-only re-assert against a bare orphan fails with {@code HTTP 400}
     *       (analyzer not found) and leaves the index half-mapped (issue #36237, QA TC-003). An
     *       empty index has no data and no reindex progress, so recreating it costs nothing
     *       operationally and yields a clean index with full settings + base mapping. If the
     *       delete cannot be confirmed and the index is still present, bootstrap fails loudly
     *       rather than register a half-mapped index.</li>
     *   <li><b>Populated orphan (&gt; 0 docs), or count unknown</b> — reused in place, untouched.
     *       A populated orphan was created by dotCMS itself, so it already carries the full
     *       settings + base mapping + custom mapping; nothing needs to be (re)applied. The index is
     *       never deleted here: discarding it would throw away its contents (including partial
     *       reindex progress) and force a full reindex, which can run for hours and degrade search
     *       consistency — not justified to clean up an orphan. On any uncertainty (the count probe
     *       fails) we err toward reuse for the same reason.</li>
     * </ul>
     *
     * <p>The caller's {@code point()} then registers the index in the store.</p>
     *
     * @param indexName logical index name (no cluster prefix, no vendor tag)
     * @param shards    number of shards to create with (ignored when the index already exists)
     * @param tag       target provider ({@link IndexTag#ES} or {@link IndexTag#OS})
     * @return {@code true} when the index exists (reused) or was created successfully
     * @throws IOException on a hard creation failure
     */
    private boolean createContentIndex(final String indexName, final int shards, IndexTag tag)
            throws IOException {
        final IndexAPIImpl impl = (IndexAPIImpl) indexAPI;

        final ContentletIndexOperations ops = tag == IndexTag.OS ? router.osImpl() : router.esImpl();
        final IndexAPI providerApi = tag == IndexTag.OS ? impl.osImpl() : impl.esImpl();

        return createContentIndex(indexName, shards, tag, ops, providerApi,
                MappingHelper.getInstance());
    }

    /**
     * Idempotent-bootstrap core of {@link #createContentIndex(String, int, IndexTag)}, with the
     * provider collaborators injected so the orphan-reuse decision can be unit-tested without a
     * running cluster or the {@link MappingHelper} singleton. See the public-facing overload's
     * javadoc for the behaviour contract.
     *
     * @param indexName   logical index name (no cluster prefix, no vendor tag)
     * @param shards      number of shards to create with (ignored when the index already exists)
     * @param tag         target provider ({@link IndexTag#ES} or {@link IndexTag#OS})
     * @param ops         vendor write operations for {@code tag} (creation + physical-name mapping)
     * @param providerApi vendor index API for {@code tag} (existence probe)
     * @param helper      mapping helper used to (re)assert the custom mapping
     * @return {@code true} when the index exists (reused) or was created successfully
     * @throws IOException on a hard creation failure
     */
    boolean createContentIndex(final String indexName, final int shards, final IndexTag tag,
            final ContentletIndexOperations ops, final IndexAPI providerApi,
            final MappingHelper helper) throws IOException {
        final String physicalName = ops.toPhysicalName(indexName);

        // Reuse an orphaned cluster index rather than failing the create (see method javadoc).
        // The existence probe is best-effort: any failure is treated as "does not exist" so we
        // fall through to the create path rather than aborting bootstrap on a transient error.
        // The failure is logged at DEBUG so a real connectivity/config problem (which would then
        // also surface on the create attempt) stays traceable instead of being silently swallowed.
        final boolean alreadyExists = Try.of(() -> providerApi.indexExists(physicalName))
                .onFailure(e -> Logger.debug(this,
                        "Bootstrap existence probe failed for " + physicalName
                        + " — treating as 'does not exist' and attempting create: "
                        + e.getMessage(), e))
                .getOrElse(false);
        if (alreadyExists) {
            // Orphan: exists in cluster, missing from store (see method javadoc). Decide by doc
            // count so a populated index — including partial reindex progress — is never discarded.
            // The count probe is best-effort: any failure is treated as "has data" (-1) so we err
            // toward reuse and never delete on uncertainty.
            final long docCount = Try.of(() -> ops.getIndexDocumentCount(physicalName))
                    .onFailure(e -> Logger.warn(this,
                            "Orphan doc-count probe failed for " + physicalName
                            + " — treating as populated and reusing in place: "
                            + e.getMessage(), e))
                    .getOrElse(-1L);

            if (docCount != 0L) {
                // Populated (or unknown): reuse in place, untouched. A dotCMS-created index already
                // carries the full settings + base mapping + custom mapping, so nothing needs to be
                // (re)applied. Deleting it would force a full reindex (hours, degraded search) —
                // not justified to clean up an orphan.
                Logger.info(this, String.format(
                        "Bootstrap: orphaned %s index found with %s document(s); reusing in place"
                        + " (not deleting, not remapping): %s",
                        tag, docCount < 0 ? "an unknown number of" : docCount, physicalName));
                return true;
            }

            // Empty orphan: delete so the create below rebuilds a clean index with full settings +
            // base mapping. An empty index has no data and no reindex progress, so this is safe and
            // costs nothing operationally (issue #36237 — repairs a bare orphan that reuse cannot).
            Logger.info(this, String.format(
                    "Bootstrap: empty orphaned %s index found (in cluster, missing from store);"
                    + " deleting and recreating with full settings + mapping: %s",
                    tag, physicalName));
            final boolean deleted = Try.of(() -> providerApi.delete(physicalName))
                    .onFailure(e -> Logger.warn(this,
                            "Failed to delete empty orphaned index " + physicalName
                            + ": " + e.getMessage(), e))
                    .getOrElse(false);
            if (!deleted) {
                // Delete not acknowledged. Re-probe: it may have taken effect without an ack, in
                // which case we can still recreate cleanly. If the index is genuinely still there
                // we must NOT proceed — recreating would throw resource_already_exists, and reusing
                // it would register a bare orphan whose mapping cannot be repaired (the custom
                // analyzer is a create-time-only setting). Fail loud instead of leaving a
                // half-mapped index in the store. This is an abnormal cluster state, not the
                // orphan-name collision this method otherwise resolves.
                final boolean stillExists = Try.of(() -> providerApi.indexExists(physicalName))
                        .getOrElse(true);
                if (stillExists) {
                    throw new IOException("Empty orphaned " + tag + " index " + physicalName
                            + " could not be deleted and still exists; aborting bootstrap to avoid"
                            + " registering a half-mapped index. Check the search cluster health"
                            + " and restart.");
                }
                Logger.warn(this, "Empty orphaned index " + physicalName + " delete was not"
                        + " acknowledged, but the index is gone; proceeding to recreate.");
            }
        }

        final boolean contentIndex = ops.createContentIndex(physicalName, shards);
        if (contentIndex) {
            helper.addCustomMapping(List.of(indexName), tag);
        }
        return contentIndex;
    }

    /**
     * Ensures that working and live indices exist for all applicable providers.
     *
     * <p>Generates a single timestamp and delegates to
     * {@link #bootstrapAndPoint(String, boolean, boolean)}.
     * Returns {@link IndexStartResult#empty()} immediately when all required indices already exist.</p>
     *
     * <ul>
     *   <li>Phase 0: creates 2 ES indices; {@code indexSuffixOS} = {@code ""}.</li>
     *   <li>Phases 1/2/3 (fresh install): creates 2 ES + 2 OS indices sharing the same timestamp.</li>
     *   <li>Migration catchup (ES exists, OS missing): creates only the 2 missing OS indices.</li>
     * </ul>
     *
     * @return index-creation metadata; empty if all indices were already present
     * @throws DotDataException on persistence or creation failure
     */
    private synchronized IndexStartResult initIndex() throws DotDataException {
        if (indexReady()) {
            return IndexStartResult.empty();
        }

        // Phase 3: ES is decommissioned — never create ES indices, even if they are absent.
        final boolean esNeeded = !isMigrationComplete() && !indexReadyES();
        final boolean osNeeded = isMigrationStarted() && !indexReadyOS();

        if (!esNeeded && osNeeded) {
            // Migration catchup: ES already has indices — derive OS names from ES to avoid
            // timestamp divergence (e.g. working_20240101 → working_20240101.os, not _20240606).
            return initOSCatchup();
        }

        final String ts = ContentletIndexAPI.threadSafeTimestampFormatter.format(LocalDateTime.now());
        bootstrapAndPoint(ts, esNeeded, osNeeded);

        return ImmutableIndexStartResult.builder()
                .indexSuffixES(esNeeded ? ts : "")
                .indexSuffixOS(osNeeded ? ts : "")
                .build();
    }

    /**
     * Creates and registers working/live indices for the providers that need them.
     *
     * <p>The two guard flags allow independent control over each provider so that a
     * migration-catchup run (ES already present, OS missing) only touches the missing
     * provider without attempting to re-create indices that already exist.</p>
     *
     * <p>Invariant: the same {@code ts} is used for all indices created in this call,
     * guaranteeing that all four physical index names share an identical timestamp suffix.</p>
     *
     * <p>Applies to all phases.</p>
     *
     * @param ts       timestamp string produced by {@link ContentletIndexAPI#threadSafeTimestampFormatter}
     * @param needsES  {@code true} when ES working/live indices must be created
     * @param needsOS  {@code true} when OS working/live indices must be created
     * @throws DotDataException on persistence or creation failure
     */
    private void bootstrapAndPoint(final String ts,
                                   final boolean needsES,
                                   final boolean needsOS) throws DotDataException {

        final String workingName = IndexType.WORKING.getPrefix() + "_" + ts;
        final String liveName    = IndexType.LIVE.getPrefix()    + "_" + ts;
        if(needsES) {
            bootstrapAndPointES(workingName, liveName);
        }
        if(needsOS) {
            bootstrapAndPointOS(workingName, liveName);
        }
    }


    /**
     * Recovers or bootstraps OS indices when they are absent from the cluster.
     *
     * <p>Three cases in priority order:</p>
     * <ol>
     *   <li><b>OS DB record exists, cluster index missing</b> (Phase 3 disaster-recovery or
     *       restart after partial failure): recreate the OS index using the name already
     *       registered in {@link VersionedIndicesAPI}. This is the authoritative source in
     *       Phase 3 — reading from the ES store would give a stale or cleaned-up name.</li>
     *   <li><b>OS DB empty, ES DB has a record</b> (migration catchup, Phases 1/2): mirror
     *       the ES logical name so both indices share the same base name.  This prevents
     *       timestamp divergence (e.g. {@code working_20240101} ES → {@code working_20240101.os},
     *       not {@code working_20260528.os}).</li>
     *   <li><b>Both empty</b> (fresh install or data-loss): generate a new timestamp and
     *       log a warning so the operator can investigate.</li>
     * </ol>
     */
    private IndexStartResult initOSCatchup() throws DotDataException {
        // Case 1: OS DB record exists → recreate the cluster index with the registered name.
        // loadDefaultVersionedIndices() returns the canonical .os-tagged form; stripClusterPrefix
        // removes only the cluster_X. prefix and preserves the tag (the name identity).
        final Optional<VersionedIndices> osDbRecord =
                Try.of(versionedIndicesAPI::loadDefaultVersionedIndices)
                   .getOrElse(Optional.empty());
        final Optional<String> osWorking = osDbRecord.flatMap(VersionedIndices::working);
        if (osWorking.isPresent()) {
            final String workingLogical = stripClusterPrefix(osWorking.get());
            final String liveLogical = osDbRecord.flatMap(VersionedIndices::live)
                    .map(ContentletIndexAPIImpl::stripClusterPrefix)
                    .orElse(workingLogical);
            Logger.info(this, "OS recovery: recreating missing OS cluster index — working="
                    + workingLogical + ", live=" + liveLogical);
            bootstrapAndPointOS(workingLogical, liveLogical);
            // indexSuffixOS is a pure timestamp — strip the .os tag locally before parsing it out.
            final String workingBase = IndexTag.strip(workingLogical);
            final int lastUnder = workingBase.lastIndexOf('_');
            final String suffix = lastUnder >= 0 ? workingBase.substring(lastUnder + 1) : "";
            return ImmutableIndexStartResult.builder()
                    .indexSuffixES("").indexSuffixOS(suffix).build();
        }

        // Case 2: OS DB empty — mirror ES names (migration catchup, Phases 1/2).
        final IndiciesInfo esInfo = legacyIndiciesAPI.loadIndicies();
        if (esInfo != null && UtilMethods.isSet(esInfo.getWorking())) {
            final String workingLogical = stripClusterPrefix(esInfo.getWorking());
            final String liveLogical = UtilMethods.isSet(esInfo.getLive())
                    ? stripClusterPrefix(esInfo.getLive()) : workingLogical;
            Logger.info(this, "OS catchup: mirroring ES names — working=" + workingLogical
                    + ", live=" + liveLogical);
            bootstrapAndPointOS(workingLogical, liveLogical);
            final int lastUnder = workingLogical.lastIndexOf('_');
            final String suffix = lastUnder >= 0 ? workingLogical.substring(lastUnder + 1) : "";
            return ImmutableIndexStartResult.builder()
                    .indexSuffixES("").indexSuffixOS(suffix).build();
        }

        // Case 3: no record in either store — fresh install or data-loss scenario.
        Logger.warn(this, "OS catchup: no OS or ES index record found;"
                + " bootstrapping OS with fresh timestamp");
        final String ts = ContentletIndexAPI.threadSafeTimestampFormatter
                .format(LocalDateTime.now());
        bootstrapAndPointOS(IndexType.WORKING.getPrefix() + "_" + ts,
                            IndexType.LIVE.getPrefix()    + "_" + ts);
        return ImmutableIndexStartResult.builder()
                .indexSuffixES("").indexSuffixOS(ts).build();
    }

    /**
     * Strips the {@code cluster_XXXXXXXXXX.} prefix from a DB-stored physical index name,
     * returning the logical name used for OS/ES client calls.
     *
     * <p>Example: {@code cluster_e0f4fa027f.working_20240101} → {@code working_20240101}</p>
     */
    private static String stripClusterPrefix(final String physicalName) {
        final int dot = physicalName != null ? physicalName.indexOf('.') : -1;
        return dot >= 0 ? physicalName.substring(dot + 1) : physicalName;
    }

    /**
     * Creates the ES working and live indices for the given logical names and registers
     * them as the active indices in the ES index store ({@link IndiciesAPI}).
     *
     * <p>Index creation is targeted directly at the ES provider via {@link IndexTag#ES} —
     * no phase fan-out occurs here. This is intentional: bootstrap must be idempotent per
     * provider so that a migration-catchup run that only needs to initialise OS never
     * re-creates ES indices that already exist.</p>
     *
     * <p>If either {@link #createContentIndex} call returns {@code false} (soft failure),
     * the error is logged but execution continues and {@link #pointES} is still called.
     * A hard failure (e.g. cluster unreachable) propagates as {@link DotDataException}.</p>
     *
     * @param workingName logical working index name (no cluster prefix, no vendor tag)
     * @param liveName    logical live index name (no cluster prefix, no vendor tag)
     * @throws DotDataException if index creation throws {@link IOException} or the ES
     *                          store cannot be updated
     */
    private void bootstrapAndPointES(final String workingName, final String liveName)
            throws DotDataException {
        boolean result;
        try {
            // Targeted: executed directly against this provider only. No phase fan-out here.
            result = createContentIndex(workingName, 1, IndexTag.ES);
            result &= createContentIndex(liveName, 1, IndexTag.ES);
        } catch (IOException e) {
            throw new DotDataException(String.format(
                    "Error creating content indices for indices[ %s ,%s ] with message: %s ",
                    workingName, liveName, e.getMessage()), e);
        }
        if (!result) {
            Logger.error(getClass(), String.format(
                    "Unable to Bootstrap: There was a problem creating one of the indices on ES %s & %s",
                    workingName, liveName));
        }
        pointES(operationsES.toPhysicalName(workingName),
                operationsES.toPhysicalName(liveName), null, null);
    }

    /**
     * Creates the OS working and live indices for the given logical names and registers
     * them as the active indices in the OS index store ({@link VersionedIndicesAPI}).
     *
     * <p>Mirror of {@link #bootstrapAndPointES} for the OpenSearch provider.
     * Index creation is targeted directly at the OS provider via {@link IndexTag#OS} —
     * no phase fan-out occurs here. Called only when {@code needsOS} is {@code true} in
     * {@link #bootstrapAndPoint}, i.e. when the migration has started and OS indices are
     * not yet initialised.</p>
     *
     * <p>If either {@link #createContentIndex} call returns {@code false} (soft failure),
     * the error is logged but execution continues and {@link #pointOS} is still called.
     * A hard failure propagates as {@link DotDataException}.</p>
     *
     * @param workingName logical working index name (no cluster prefix, no vendor tag)
     * @param liveName    logical live index name (no cluster prefix, no vendor tag)
     * @throws DotDataException if index creation throws {@link IOException} or the OS
     *                          store cannot be updated
     */
    private void bootstrapAndPointOS(final String workingName, final String liveName)
            throws DotDataException {

        // Separation gate (issue #36419): OS must be a SEPARATE cluster from ES. Config-only,
        // no network I/O, so it runs before the connection gate. Placing it at this single
        // chokepoint closes the window where the empty-DB starter-load path created .os indices
        // before InitServlet's later validateIndexingConfig() caught the ES==OS overlap. On
        // overlap we halt the migration (ES-only) and skip OS bootstrap entirely.
        // Phase-aware: in Phase 3 (ES decommissioned, ES_ENDPOINTS not required) the check is
        // skipped inside endpointsAreSeparate(), so this branch never fires there and the
        // haltMigration() fallback below only ever runs in dual-write phases where ES is live.
        if (!IndexStartupValidator.endpointsAreSeparate()) {
            Logger.warn(this.getClass(),
                    "Skipping OpenSearch index bootstrap (working=" + workingName
                    + ", live=" + liveName + "): OS migration configuration rejected"
                    + " (see the preceding error for the cause — e.g. ES/OS endpoint overlap or"
                    + " an unresolved OS config). Migration halted (now ES-only).");
            haltMigration();
            return;
        }

        // Connection gate (issue #36244): verify OS reachability BEFORE creating OS indices.
        // This is the single chokepoint for OS working/live index bootstrap (fresh-install and
        // migration catchup), so both startup paths — populated-DB (InitServlet) and empty-DB
        // (Task00004LoadStarter) — pass through the same phase-aware gate instead of failing
        // late and opaquely with a transport exception deep inside createContentIndex.
        // (Reindex-slot creation via initAndPointReindex does NOT pass through here — see the
        // runtime phase-flip caveat in PR #36421.)
        //
        // operationsOS.indexAPI() is the OS-specific IndexAPI, so the gate always probes OS
        // regardless of the current read provider (in Phase 1 the read provider is ES). The
        // phase-aware outcome lives in OSIndexAPIImpl.waitUtilIndexReady(): Phase 3 aborts the
        // JVM with an actionable message; Phase 1/2 halts the migration (ES-only fallback) and
        // returns false — in which case we must NOT create OS indices.
        if (!operationsOS.indexAPI().waitUtilIndexReady()) {
            Logger.warn(this.getClass(),
                    "Skipping OpenSearch index bootstrap (working=" + workingName
                    + ", live=" + liveName + "): OS was unreachable and the migration was halted"
                    + " (now ES-only). OS indices will be created on a later restart once OS is"
                    + " reachable and the migration phase is re-enabled.");
            return;
        }

        boolean result;
        try {
            // Targeted: executed directly against this provider only. No phase fan-out here.
            result = createContentIndex(workingName, 1, IndexTag.OS);
            result &= createContentIndex(liveName, 1, IndexTag.OS);
        } catch (IOException e) {
            throw new DotDataException(String.format(
                    "Error creating content indices for indices[ %s ,%s ] with message: %s",
                    workingName, liveName, e.getMessage()), e);
        }
        if (!result) {
            Logger.error(getClass(), String.format(
                    "Unable to Bootstrap: There was a problem creating one of the indices on OS %s & %s",
                    workingName, liveName));
        }
        pointOS(operationsOS.toPhysicalName(workingName),
                operationsOS.toPhysicalName(liveName), null, null);
    }

    /**
     * Loads the current ES index store, overrides only the non-null slot arguments,
     * preserves everything else (including {@code siteSearch}), and persists.
     *
     * @param working        new working index name, or {@code null} to preserve existing
     * @param live           new live index name, or {@code null} to preserve existing
     * @param reindexWorking new reindex-working index name, or {@code null} to preserve existing
     * @param reindexLive    new reindex-live index name, or {@code null} to preserve existing
     */
    private void pointES(final String working, final String live,
            final String reindexWorking, final String reindexLive) throws DotDataException {
        final IndiciesInfo oldInfo = legacyIndiciesAPI.loadIndicies();
        final IndiciesInfo.Builder builder = new IndiciesInfo.Builder();
        builder.setWorking(working != null ? working
                : (oldInfo != null ? oldInfo.getWorking() : null));
        builder.setLive(live != null ? live
                : (oldInfo != null ? oldInfo.getLive() : null));
        builder.setReindexWorking(reindexWorking != null ? reindexWorking
                : (oldInfo != null ? oldInfo.getReindexWorking() : null));
        builder.setReindexLive(reindexLive != null ? reindexLive
                : (oldInfo != null ? oldInfo.getReindexLive() : null));
        if (oldInfo != null && oldInfo.getSiteSearch() != null) {
            builder.setSiteSearch(oldInfo.getSiteSearch());
        }
        legacyIndiciesAPI.point(builder.build());
    }

    /**
     * Loads the current OS index store, overrides only the non-null slot arguments,
     * preserves everything else, and persists.
     *
     * <p><strong>Tagging on save:</strong> persistence goes through
     * {@link com.dotcms.content.index.VersionedIndicesAPI#saveIndices}, which applies the
     * {@code .os} tag ({@link com.dotcms.content.index.IndexTag#OS}) to each index name before
     * the DB write. The save is idempotent on already-tagged names — in the normal flow the
     * names arrive pre-tagged via {@code toPhysicalName}, so this is a belt-and-suspenders
     * guard. The {@code .os} suffix is the DB uniqueness artifact that keeps OS rows from
     * colliding with ES rows on the same primary key in the shared {@code indicies} table;
     * stripping it before saving is rejected by {@code requireOSTagged}.</p>
     *
     * @param working        new working index name, or {@code null} to preserve existing
     * @param live           new live index name, or {@code null} to preserve existing
     * @param reindexWorking new reindex-working index name, or {@code null} to preserve existing
     * @param reindexLive    new reindex-live index name, or {@code null} to preserve existing
     */
    private void pointOS(final String working, final String live,
            final String reindexWorking, final String reindexLive) throws DotDataException {
        final Optional<VersionedIndices> existingOpt =
                versionedIndicesAPI.loadDefaultVersionedIndices();
        final VersionedIndicesImpl.Builder builder = VersionedIndicesImpl.builder();
        if (working != null) {
            builder.working(working);
        } else {
            existingOpt.flatMap(VersionedIndices::working).ifPresent(builder::working);
        }
        if (live != null) {
            builder.live(live);
        } else {
            existingOpt.flatMap(VersionedIndices::live).ifPresent(builder::live);
        }
        if (reindexWorking != null) {
            builder.reindexWorking(reindexWorking);
        } else {
            existingOpt.flatMap(VersionedIndices::reindexWorking).ifPresent(builder::reindexWorking);
        }
        if (reindexLive != null) {
            builder.reindexLive(reindexLive);
        } else {
            existingOpt.flatMap(VersionedIndices::reindexLive).ifPresent(builder::reindexLive);
        }
        versionedIndicesAPI.saveIndices(builder.build());
    }


    /**
     * Stops the current re-indexation process and switches the current index to the new one. The
     * main goal of this method is to allow users to switch to the new index even if one or more
     * contents could not be re-indexed.
     * <p>
     * This is very useful because the new index can be created and used immediately. The user can
     * have the new re-indexed content available and then work on the conflicting contents, which
     * can be either fixed or removed from the database.
     * </p>
     *
     * @throws DotDataException     The process to switch to the new failed.
     * @throws InterruptedException The established pauses to switch to the new index failed.
     */
    @Override
    @CloseDBIfOpened
    public void stopFullReindexationAndSwitchover() throws DotDataException {
        try {
            ReindexThread.pause();
            queueApi.deleteReindexRecords();
            this.reindexSwitchover(true);
        } finally {
            ReindexThread.unpause();
        }
    }

    /**
     * Switches the current index structure to the new re-indexed data. This method also allows
     * users to switch to the new re-indexed data even if there are still remaining contents in the
     * {@code dist_reindex_journal} table.
     *
     * @param forceSwitch - If {@code true}, the new index will be used, even if there are contents
     *                    that could not be processed. Otherwise, set to {@code false} and the index
     *                    switch will only happen if ALL contents were re-indexed.
     * @return
     * @throws DotDataException     The process to switch to the new failed.
     * @throws InterruptedException The established pauses to switch to the new index failed.
     */
    @Override
    @CloseDBIfOpened
    public boolean reindexSwitchover(boolean forceSwitch) throws DotDataException {

        // We double-check again. Only one node will enter this critical
        // region, then others will enter just to see that the switchover is
        // done

        if (forceSwitch || queueApi.recordsInQueue() == 0) {
            Logger.info(this, "Running Reindex Switchover");
            // Wait a bit while all records gets flushed to index
            return this.fullReindexSwitchover(forceSwitch);
            // Wait a bit while elasticsearch flushes it state
        }
        return false;
    }

    /**
     * Creates new reindex (working and live) indices so that a full reindex can proceed in the
     * background while the current indices remain live.  The reindex-slot names are derived from
     * a single timestamp so that ES and OS reindex indices always share the same suffix.
     *
     * <p>When the main indices are not yet ready (or a reindex is already in progress) this method
     * delegates to {@link #initIndex()} instead of starting a second reindex cycle.</p>
     *
     * <p>Applies to all phases.  The returned {@link IndexStartResult} always satisfies
     * {@code indexSuffixES().equals(indexSuffixOS())} when both providers are active.</p>
     *
     * @return creation metadata; {@code indexSuffixES()} and {@code indexSuffixOS()} are equal when
     *         both providers were written to
     * @throws DotDataException on persistence or creation failure
     */
    @Override
    @WrapInTransaction
    public synchronized IndexStartResult fullReindexStart() throws DotDataException {
        if (indexReady() && !isInFullReindex()) {
            final User currentUser = Try.of(
                    () -> PortalUtil.getUser(HttpServletRequestThreadLocal.INSTANCE.getRequest()))
                    .getOrNull();
            if (currentUser != null) {
                Logger.info(this, "Full reindex started by user: "
                        + currentUser.getUserId() + " (" + currentUser.getEmailAddress()
                        + ") at " + new java.util.Date());
            } else {
                Logger.info(this, "Full reindex started by system user at " + new java.util.Date());
            }

            final String ts = ContentletIndexAPI.threadSafeTimestampFormatter
                    .format(LocalDateTime.now());
            initAndPointReindex(ts);

            return ImmutableIndexStartResult.builder()
                    .indexSuffixES(ts)
                    .indexSuffixOS(isMigrationNotStarted() ? "" : ts)
                    .build();
        } else {
            return initIndex();
        }
    }

    /**
     * Creates reindex working and live indices for all applicable providers from a single
     * timestamp, then updates both stores so that each provider points to the new reindex slots
     * while preserving its existing working/live pointers.
     *
     * <p>Name transformation rules (same as {@link #createContentIndex}):</p>
     * <ul>
     *   <li>ES: cluster-prefixed physical name</li>
     *   <li>OS: plain name</li>
     * </ul>
     *
     * <p>Applies to all phases; OS store is skipped in phase 0.</p>
     *
     * @param ts timestamp string produced by {@link ContentletIndexAPI#threadSafeTimestampFormatter}
     * @throws DotDataException on persistence or creation failure
     */
    private void initAndPointReindex(final String ts) throws DotDataException {
        final String reindexWorkingName = IndexType.REINDEX_WORKING.getPrefix() + "_" + ts;
        final String reindexLiveName    = IndexType.REINDEX_LIVE.getPrefix()    + "_" + ts;

        // Physical index creation — router fan-out applies name transformation per provider
        try {
            createContentIndex(reindexWorkingName, 0);
            createContentIndex(reindexLiveName, 0);
        } catch (IOException e) {
            throw new DotDataException(
                    "Error creating reindex indices for ts=" + ts + ": " + e.getMessage(), e);
        }
        // ── LEGACY ES — remove after Phase 3 migration ────────────────────────
        // Persist reindex slots to the legacy ES store, preserving existing working/live pointers.
        // Skipped in Phase 3: ES is decommissioned, so writing ES reindex pointers here would only
        // create orphan NULL-version rows that the OS-only switchover never cleans up (#36077).
        if (!isMigrationComplete()) {
            pointES(null, null,
                    operationsES.toPhysicalName(reindexWorkingName),
                    operationsES.toPhysicalName(reindexLiveName));
        }
        // ── END LEGACY ES ─────────────────────────────────────────────────────

        // Persist OS reindex slots, preserving existing working/live pointers.
        if (isMigrationStarted()) {
            pointOS(null, null,
                    operationsOS.toPhysicalName(reindexWorkingName),
                    operationsOS.toPhysicalName(reindexLiveName));
        }
    }

    @CloseDBIfOpened
    public boolean isInFullReindex() throws DotDataException {
        if (queueApi.hasReindexRecords()) {
            return true;
        }
        if (isReadEnabled() || isMigrationComplete()) {
            // Phase 2+: check OS reindex slots.
            return versionedIndicesAPI.loadDefaultVersionedIndices()
                    .map(vi -> vi.reindexWorking().isPresent() && vi.reindexLive().isPresent())
                    .orElse(false);
        }
        // Phase 0/1: check ES reindex slots.
        final IndiciesInfo info = legacyIndiciesAPI.loadIndicies();
        return info.getReindexWorking() != null && info.getReindexLive() != null;
    }

    @CloseDBIfOpened
    public boolean fullReindexSwitchover(final boolean forceSwitch) {
        return fullReindexSwitchover(DbConnectionFactory.getConnection(), forceSwitch);
    }

    /**
     * Promotes the current reindex indices to active, completing a full-reindex cycle.
     *
     * <h3>Phase-aware behavior</h3>
     * <ul>
     *   <li><strong>Phase 3 (migration complete)</strong> — ES is decommissioned.
     *       The method delegates entirely to {@link #fullReindexSwitchoverOS(boolean)}, which
     *       reads and writes only {@code versionedIndicesAPI}. {@code legacyIndiciesAPI} is
     *       never touched.</li>
     *   <li><strong>Phases 0, 1, 2 (ES active)</strong> — {@code legacyIndiciesAPI} is the
     *       primary store. After the ES store is updated, Phases 1 and 2 also mirror the
     *       promotion to {@code versionedIndicesAPI} on a best-effort basis: a failure in the
     *       OS mirror must never abort the ES switchover.</li>
     * </ul>
     *
     * <h3>Cluster coordination (all phases)</h3>
     * <p>In a multi-node cluster, only the oldest server ("lucky server") performs the actual
     * store update. All other nodes sleep briefly, clear their index-name cache, and return
     * {@code false}. On their next scheduler tick they will pick up the new names from the
     * store and stop reporting a full reindex in progress.</p>
     *
     * <h3>Minimum-runtime guard (all phases)</h3>
     * <p>If the reindex working index was created less than
     * {@code REINDEX_THREAD_MINIMUM_RUNTIME_IN_SEC} seconds ago the method returns
     * {@code false} and sleeps briefly to let the index settle. The timestamp is parsed
     * from the reindex-working index name suffix; both ES and OS use the same
     * {@code ..._yyyyMMddHHmmss} convention.</p>
     */
    @CloseDBIfOpened
    public synchronized boolean fullReindexSwitchover(Connection conn, final boolean forceSwitch) {

        // ── Guard: minimum reindex runtime ───────────────────────────────────
        // reindexTimeElapsedInLong() is itself phase-aware: Phase 3 reads from
        // versionedIndicesAPI, all other phases read from legacyIndiciesAPI.
        if (reindexTimeElapsedInLong()
                < Config.getLongProperty("REINDEX_THREAD_MINIMUM_RUNTIME_IN_SEC", 30) * 1000) {
            final Optional<String> reindexTimeElapsed = reindexTimeElapsed();
            if (reindexTimeElapsed.isPresent()) {
                Logger.info(this.getClass(),
                        "Reindex has been running only " + reindexTimeElapsed.orElse("n/a")
                                + ". Letting the reindex settle.");
            } else {
                Logger.info(this.getClass(), "Reindex Time Elapsed not set.");
            }
            ThreadUtils.sleep(3000);
            return false;
        }

        try {
            // ── Phase 3: OS only ─────────────────────────────────────────────
            // legacyIndiciesAPI must not be consulted — ES is decommissioned.
            if (isMigrationComplete()) {
                return fullReindexSwitchoverOS(forceSwitch);
            }

            // ── Phases 0, 1, 2: ES-primary path ─────────────────────────────

            // Load current ES index pointers (working, live, reindexWorking, reindexLive,
            // siteSearch). These are what will be promoted or preserved below.
            final IndiciesInfo oldInfo = legacyIndiciesAPI.loadIndicies();
            final String luckyServer = Try.of(() -> APILocator.getServerAPI().getOldestServer())
                    .getOrElse(ConfigUtils.getServerId());

            if (!forceSwitch) {
                if (!isInFullReindex()) {
                    return false;
                }
                // Another node is the designated switchover server. Sleep and let it act;
                // clear local cache so this node picks up the new pointers on the next tick.
                if (!luckyServer.equals(ConfigUtils.getServerId())) {
                    logSwitchover(oldInfo, luckyServer);
                    DateUtil.sleep(5000);
                    CacheLocator.getIndiciesCache().clearCache();
                    return false;
                }
            }

            // Build the new ES index-pointer record:
            //   working  ← reindexWorking  (the freshly built index)
            //   live     ← reindexLive
            //   siteSearch is preserved unchanged
            //   reindexWorking / reindexLive are left null → cleared from the store
            final IndiciesInfo newInfo = new IndiciesInfo.Builder()
                    .setLive(oldInfo.getReindexLive())
                    .setWorking(oldInfo.getReindexWorking())
                    .setSiteSearch(oldInfo.getSiteSearch())
                    .build();

            logSwitchover(oldInfo, luckyServer);
            legacyIndiciesAPI.point(newInfo); // ← atomic store update in ES

            // ── OS mirror (Phases 1 and 2, best-effort) ──────────────────────
            // The OS shadow index has its own reindex slots that must be promoted
            // in lock-step with ES. A failure here must never abort the ES result.
            List<String> newActiveOs = List.of();
            if (isMigrationStarted()) {
                try {
                    final Optional<VersionedIndices> osExisting =
                            versionedIndicesAPI.loadDefaultVersionedIndices();
                    final Optional<String> osWorking =
                            osExisting.flatMap(VersionedIndices::reindexWorking);
                    final Optional<String> osLive =
                            osExisting.flatMap(VersionedIndices::reindexLive);
                    final VersionedIndicesImpl.Builder osBuilder = VersionedIndicesImpl.builder();
                    // Promote OS reindex slots → active; omitting reindexWorking/reindexLive
                    // from the builder clears them to Optional.empty() in the store.
                    osWorking.ifPresent(osBuilder::working);
                    osLive.ifPresent(osBuilder::live);
                    versionedIndicesAPI.saveIndices(osBuilder.build());
                    // Capture the promoted OS names (.os-tagged, from the OS store) so they can be
                    // optimized on the OS provider directly — see optimizeNewActiveIndicesAsync.
                    if (osWorking.isPresent() && osLive.isPresent()) {
                        newActiveOs = List.of(osWorking.get(), osLive.get());
                    }
                } catch (Exception osEx) {
                    Logger.warn(this, "Could not mirror reindex switchover to OS store", osEx);
                }
            }

            // Async: merge index segments and expand replicas on the newly active indices,
            // optimizing EACH provider with the names it actually holds — ES bare names from
            // newInfo, OS .os-tagged names from the store. Routing through the phase-aware
            // optimize() would send the ES names to the OS read provider in Phase 2 and hit
            // index_not_found (then fall back to ES), which is noisy and skips the OS optimize.
            optimizeNewActiveIndicesAsync(
                    List.of(newInfo.getWorking(), newInfo.getLive()), newActiveOs);

            notifyAdminsOfFailedReindex();

        } catch (Exception e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
        return true;
    }

    /**
     * Phase 3 switchover: promotes OS reindex slots to active using <em>only</em>
     * {@code versionedIndicesAPI}. {@code legacyIndiciesAPI} is never consulted.
     *
     * <p>Applies the same cluster-coordination and minimum-runtime guards as the ES path.</p>
     *
     * @return {@code true} if the switchover was performed; {@code false} if it was deferred or
     *         aborted due to missing state.
     */
    private boolean fullReindexSwitchoverOS(final boolean forceSwitch) throws Exception {
        final Optional<VersionedIndices> osExisting =
                versionedIndicesAPI.loadDefaultVersionedIndices();

        // Safety: if there is nothing to promote, bail out rather than writing empty state.
        if (osExisting.isEmpty()
                || osExisting.get().reindexWorking().isEmpty()
                || osExisting.get().reindexLive().isEmpty()) {
            Logger.warn(this, "OS reindex slots are empty — nothing to switchover");
            return false;
        }

        final VersionedIndices existing = osExisting.get();
        final String luckyServer = Try.of(() -> APILocator.getServerAPI().getOldestServer())
                .getOrElse(ConfigUtils.getServerId());

        if (!forceSwitch) {
            if (!isInFullReindex()) {
                return false;
            }
            // Same cluster-coordination as the ES path: only the oldest node acts.
            if (!luckyServer.equals(ConfigUtils.getServerId())) {
                Logger.info(this, "OS switchover: waiting for lucky server " + luckyServer);
                DateUtil.sleep(5000);
                CacheLocator.getIndiciesCache().clearCache();
                return false;
            }
        }

        // Log what is being switched so operators can correlate with ES behaviour.
        Logger.info(this, "-------------------------------");
        reindexTimeElapsed().ifPresent(d -> Logger.info(this, "Reindex took        : " + d));
        Logger.info(this, "Switching Server Id : " + luckyServer);
        Logger.info(this, "Old OS indicies     : ["
                + existing.working().map(indexAPI::removeClusterIdFromName).orElse("none") + ","
                + existing.live().map(indexAPI::removeClusterIdFromName).orElse("none") + "]");
        Logger.info(this, "New OS indicies     : ["
                + existing.reindexWorking().map(indexAPI::removeClusterIdFromName).orElse("none") + ","
                + existing.reindexLive().map(indexAPI::removeClusterIdFromName).orElse("none") + "]");
        Logger.info(this, "-------------------------------");

        // Promote OS reindex slots → active; omitting reindexWorking/reindexLive from the
        // builder clears them to Optional.empty() — there is no longer an ongoing reindex.
        final VersionedIndicesImpl.Builder osBuilder = VersionedIndicesImpl.builder();
        existing.reindexWorking().ifPresent(osBuilder::working);
        existing.reindexLive().ifPresent(osBuilder::live);
        versionedIndicesAPI.saveIndices(osBuilder.build());

        // Purge leftover legacy ES content-index rows (NULL version) from the indicies table:
        // old live/working plus any transient reindex_live/reindex_working that predate Phase 3.
        // ES is decommissioned, so these rows are pure orphans — without this the table would
        // accumulate stale ES rows on every Phase-3 reindex (#36077). DB-only: never contacts the
        // ES cluster (which may be down). Best-effort — the OS promotion above already succeeded
        // and must not be undone by a housekeeping failure. Physical ES index deletion is left to
        // the scheduled DeleteInactiveLiveWorkingIndicesJob.
        try {
            versionedIndicesAPI.removeLegacyIndices();
        } catch (Exception cleanupEx) {
            Logger.warn(this, "Phase 3 switchover: could not purge legacy ES indicies rows", cleanupEx);
        }

        // Async: optimize the newly active OS indices (merge segments, adjust replicas).
        // reindexWorking/reindexLive presence was validated at the top of this method.
        // ES is decommissioned in Phase 3, so only the OS provider is optimized.
        final String newWorking = existing.reindexWorking().orElseThrow();
        final String newLive    = existing.reindexLive().orElseThrow();
        optimizeNewActiveIndicesAsync(List.of(), List.of(newWorking, newLive));

        notifyAdminsOfFailedReindex();
        return true;
    }

    /**
     * Optimizes (force-merges) the newly-promoted indices after a reindex switchover, targeting
     * each provider with the names it actually holds: ES with its bare names, OS with its
     * {@code .os}-tagged names. Both calls go to the vendor implementation directly
     * ({@code operationsES/OS.indexAPI()}) rather than the phase-aware {@code optimize()} router,
     * because the router routes optimize through the read provider — in Phase 2 that is OS, so the
     * ES names would be sent to OS, miss (the OS index carries the {@code .os} tag), and only
     * succeed via the ES fallback while logging a misleading error and skipping the OS optimize.
     *
     * <p>Runs asynchronously and is best-effort per provider: a force-merge failure (including an
     * OS shadow failure in dual-write phases) is logged and swallowed and never affects the
     * completed switchover.</p>
     *
     * @param esNames ES physical (bare) names to optimize, or empty to skip ES (e.g. Phase 3)
     * @param osNames OS physical ({@code .os}-tagged) names to optimize, or empty to skip OS
     */
    private void optimizeNewActiveIndicesAsync(final List<String> esNames,
            final List<String> osNames) {
        DotConcurrentFactory.getInstance().getSubmitter().submit(() -> {
            if (esNames != null && !esNames.isEmpty()) {
                try {
                    Logger.info(this.getClass(), "Updating and optimizing ElasticSearch Indexes");
                    operationsES.indexAPI().optimize(esNames);
                } catch (Exception e) {
                    Logger.warnAndDebug(this.getClass(),
                            "unable to optimize ES indices:" + e.getMessage(), e);
                }
            }
            if (osNames != null && !osNames.isEmpty()) {
                try {
                    Logger.info(this.getClass(), "Updating and optimizing OpenSearch Indexes");
                    operationsOS.indexAPI().optimize(osNames);
                } catch (Exception e) {
                    Logger.warnAndDebug(this.getClass(),
                            "unable to optimize OS indices:" + e.getMessage(), e);
                }
            }
        });
    }

    /**
     * Sends a system-wide warning message to CMS administrators if any content documents
     * failed to be reindexed during the last full-reindex run.
     */
    private void notifyAdminsOfFailedReindex() {
        try {
            final long failedRecords = queueApi.getFailedReindexRecords().size();
            if (failedRecords == 0) {
                return;
            }
            final String message = LanguageUtil.get(
                            APILocator.getCompanyAPI().getDefaultCompany(),
                            "Contents-Failed-Reindex-message")
                    .replace("{0}", String.valueOf(failedRecords));
            final SystemMessage systemMessage = new SystemMessageBuilder()
                    .setMessage(message)
                    .setType(MessageType.SIMPLE_MESSAGE)
                    .setSeverity(MessageSeverity.WARNING)
                    .setLife(3600000)
                    .create();
            final List<String> users = APILocator.getRoleAPI()
                    .findUserIdsForRole(APILocator.getRoleAPI().loadCMSAdminRole());
            SystemMessageEventUtil.getInstance().pushMessage(systemMessage, users);
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), "Could not send failed-reindex notification: " + e.getMessage(), e);
        }
    }

    /**
     * Returns how many milliseconds have elapsed since the reindex-working index was created,
     * by parsing the timestamp suffix from its name ({@code ..._yyyyMMddHHmmss}).
     *
     * <p><strong>Phase-aware</strong>: in Phase 3 reads from {@code versionedIndicesAPI};
     * in all other phases reads from {@code legacyIndiciesAPI}.</p>
     *
     * @return elapsed millis, or {@code 0} if no reindex is in progress or the name
     *         cannot be parsed.
     */
    private long reindexTimeElapsedInLong() {
        if (isMigrationComplete()) {
            // Phase 3: no ES store — read the OS reindex-working index name instead.
            try {
                return versionedIndicesAPI.loadDefaultVersionedIndices()
                        .flatMap(VersionedIndices::reindexWorking)
                        .map(this::elapsedSinceIndexCreated)
                        .orElse(0L);
            } catch (Exception e) {
                Logger.debug(this, "unable to parse reindex time from OS store: " + e, e);
                return 0;
            }
        }
        // Phases 0/1/2: read from the ES legacy store.
        try {
            final IndiciesInfo oldInfo = legacyIndiciesAPI.loadIndicies();
            if (oldInfo.getReindexWorking() != null) {
                return oldInfo.getIndexTimeStamp(IndexType.REINDEX_WORKING);
            }
        } catch (Exception e) {
            Logger.debug(this, "unable to parse time:" + e, e);
        }
        return 0;
    }

    /**
     * Parses the creation timestamp embedded in an index name and returns how many
     * milliseconds have elapsed since then.
     *
     * <p>Both ES and OS index names share the same suffix convention:
     * {@code cluster_<id>.<type>_yyyyMMddHHmmss}. The timestamp is extracted from
     * everything after the last {@code _} character.</p>
     *
     * @param indexName a physical index name with a timestamp suffix
     * @return elapsed millis since the index was created, or {@code 0} on parse failure
     */
    private long elapsedSinceIndexCreated(final String indexName) {
        try {
            // The timestamp parser cannot consume a trailing .os tag — strip it locally
            // before parsing (the name identity is unaffected; see OPENSEARCH_MIGRATION.md).
            final String base = IndexTag.strip(indexName);
            final String ts = base.substring(base.lastIndexOf('_') + 1);
            final Date startTime = IndiciesInfo.timestampFormatter.parse(ts);
            return System.currentTimeMillis() - startTime.getTime();
        } catch (Exception e) {
            Logger.debug(this, "unable to parse timestamp from index name '" + indexName + "': " + e, e);
            return 0;
        }
    }


    @Override
    public Optional<String> reindexTimeElapsed() {
        try {

            long elapsedTime = reindexTimeElapsedInLong();
            if (elapsedTime > 0) {
                return Optional.of(
                        DateUtil.humanReadableFormat(Duration.ofMillis(reindexTimeElapsedInLong()))
                                .toLowerCase());
            }
        } catch (Exception e) {
            Logger.debug(this, "unable to parse time:" + e, e);
        }
        return Optional.empty();
    }

    private void logSwitchover(final IndiciesInfo oldInfo, final String luckyServer) {
        Logger.info(this, "-------------------------------");
        final String myServerId = APILocator.getServerAPI().readServerId();
        final Optional<String> duration = reindexTimeElapsed();
        if (duration.isPresent()) {
            Logger.info(this, "Reindex took        : " + duration.get());
        }

        Logger.info(this, "Switching Server Id : " + luckyServer + (luckyServer.equals(myServerId)
                ? " (this server) " : " (NOT this server)"));

        Logger.info(this, "Old indicies        : [" + indexAPI
                .removeClusterIdFromName(oldInfo.getWorking()) + "," + indexAPI
                .removeClusterIdFromName(oldInfo.getLive()) + "]");
        Logger.info(this, "New indicies        : [" + indexAPI
                .removeClusterIdFromName(oldInfo.getReindexWorking()) + "," + indexAPI
                .removeClusterIdFromName(oldInfo.getReindexLive()) + "]");
        Logger.info(this, "-------------------------------");

    }

    /**
     * When {@code true}, bypasses the guard that blocks deletion of an active
     * (working/live) or building (reindex) index. Off by default: deleting an in-use
     * index leaves the site with nothing to serve reads from or reindex into. Intended
     * only for emergency/scripted maintenance.
     *
     * <p>The flag name is centralized in {@link FeatureFlagName}.</p>
     */
    public static final String FF_ALLOW_ACTIVE_INDEX_DELETE =
            FeatureFlagName.FEATURE_FLAG_ALLOW_ACTIVE_INDEX_DELETE;

    /**
     * Rejects a destructive operation ({@code operation}, e.g. {@code "deleted"} / {@code "cleared"})
     * on an index that is currently active (working/live) or being rebuilt (a reindex slot). The
     * protected set is collected phase-aware and, in the dual-write phases, from <strong>both</strong>
     * the ES and OS stores (see {@link #collectProtectedLogicalNames()}) so a divergently-named
     * active OS index — the one actually serving reads in Phase 2 — is protected too. The UI hides
     * the Delete/Clear option for these indices; a direct REST/AJAX call previously bypassed that
     * and could wipe the only index (see issue #35640, TC-018).
     *
     * <p>Bypass with the {@value #FF_ALLOW_ACTIVE_INDEX_DELETE} feature flag.</p>
     *
     * @throws DotStateException if the index is active/building (bypass off), or if the active set
     *                           cannot be resolved (fail closed — a destructive op must not proceed
     *                           on an unknown state).
     */
    @Override
    public void assertIndexNotActive(final String indexName, final String operation) {
        if (Config.getBooleanProperty(FF_ALLOW_ACTIVE_INDEX_DELETE, false)) {
            return;
        }
        // Compare on the LOGICAL (cluster-stripped, untagged) name on BOTH sides. The active set is
        // bare in Phases 0-2 and .os-tagged in Phase 3, and the caller may pass either the bare or
        // the .os name — normalizing both to the logical form makes the guard match regardless of
        // phase or which twin's name was given (issue #35640).
        final String requested = IndexTag.OS.untag(indexAPI.removeClusterIdFromName(indexName));
        final Set<String> protectedIndices;
        try {
            protectedIndices = collectProtectedLogicalNames();
        } catch (final DotDataException e) {
            throw new DotStateException("Unable to verify whether index '" + indexName
                    + "' is active before it is " + operation + "; refusing to proceed.", e);
        }
        if (protectedIndices.contains(requested)) {
            throw new DotStateException("Index '" + indexName
                    + "' is active or being rebuilt and cannot be " + operation + ". Deactivate it"
                    + " first (or set " + FF_ALLOW_ACTIVE_INDEX_DELETE + "=true to override).");
        }
    }

    /**
     * Collects the logical (cluster-stripped, untagged) names of every active (working/live) and
     * building (reindex) index that must be protected from destructive ops, unioning the stores
     * authoritative for the current phase:
     * <ul>
     *   <li>Phase 0 — ES store only (the OS store is not written yet).</li>
     *   <li>Phases 1/2 — <strong>both</strong> stores: reads resolve from the OS store while the
     *       ES store / UI may carry different names, and the two can diverge silently, so
     *       protecting only one store would leave the live OS index deletable
     *       (issue #35640, swicken review).</li>
     *   <li>Phase 3 — OS store only (ES decommissioned).</li>
     * </ul>
     */
    private Set<String> collectProtectedLogicalNames() throws DotDataException {
        final Set<String> protectedIndices = new HashSet<>();
        // ES store — authoritative through Phase 2.
        if (!isMigrationComplete()) {
            final IndiciesInfo es = legacyIndiciesAPI.loadIndicies();
            addLogical(protectedIndices, es.getWorking());
            addLogical(protectedIndices, es.getLive());
            addLogical(protectedIndices, es.getReindexWorking());
            addLogical(protectedIndices, es.getReindexLive());
        }
        // OS store — populated from Phase 1 on.
        if (isMigrationStarted()) {
            versionedIndicesAPI.loadDefaultVersionedIndices().ifPresent(os -> {
                os.working().ifPresent(n -> addLogical(protectedIndices, n));
                os.live().ifPresent(n -> addLogical(protectedIndices, n));
                os.reindexWorking().ifPresent(n -> addLogical(protectedIndices, n));
                os.reindexLive().ifPresent(n -> addLogical(protectedIndices, n));
            });
        }
        return protectedIndices;
    }

    /** Adds the logical (cluster-stripped, untagged) form of {@code name} to {@code set} if set. */
    private void addLogical(final Set<String> set, final String name) {
        if (name != null) {
            set.add(IndexTag.OS.untag(indexAPI.removeClusterIdFromName(name)));
        }
    }

    public boolean delete(String indexName) {
        // Guard first: never delete an active/building index (issue #35640, TC-018).
        assertIndexNotActive(indexName, "deleted");

        // Transparent-mirror: the operator sees one index, so deleting by either the ES (bare)
        // or the OS (.os) name removes the index in EVERY engine that holds it — the mirror is
        // never left half-deleted. Broadcast the LOGICAL (untagged) name to every write provider
        // so each re-derives its OWN physical name (ES → bare, OS → .os). Track the read
        // provider's result as the primary (issue #35640).
        final ContentletIndexOperations primary = router.readProvider();
        final List<ContentletIndexOperations> targets = router.writeProviders();
        final String nameForTargets = IndexTag.OS.untag(indexName);

        // The logical (cluster-stripped, untagged) name is used to find and clear DB pointers.
        final String logicalName = IndexTag.OS.untag(indexAPI.removeClusterIdFromName(indexName));

        boolean primaryResult = false;
        for (final ContentletIndexOperations ops : targets) {
            // Resolve the per-provider physical name (ES → bare, OS → .os tag) via
            // ops.toPhysicalName so the delete targets the REAL index. Routing a bare
            // logical name straight to OSIndexAPIImpl.delete would hit an untagged name
            // that does not exist and orphan the actual .os index.
            final String physicalName = ops.toPhysicalName(nameForTargets);
            // (1) Delete the cluster index. A failure here must NOT abort the operation —
            // keep going so the remaining engine and the DB pointers are still handled.
            try {
                final boolean r = ops.indexAPI().delete(physicalName);
                if (ops == primary) {
                    primaryResult = r;
                }
            } catch (final Exception e) {
                Logger.error(this.getClass(), "Error while deleting index " + physicalName, e);
                if (ops == primary) {
                    primaryResult = false;
                }
                // shadow failures are fire-and-forget in dual-write phases
            }
            // (2) Always remove the indicies-table pointer for this engine, even if the cluster
            // delete above failed, so no DB row is left dangling at a deleted index.
            try {
                clearStorePointer(ops, logicalName);
            } catch (final Exception e) {
                Logger.warn(this.getClass(),
                        "Could not clear the indicies DB pointer for " + physicalName, e);
            }
        }
        return primaryResult;
    }

    /**
     * Removes any {@code indicies} row that points at {@code logicalName} in the store that backs
     * {@code ops}: the OpenSearch provider clears the versioned store, any other provider clears
     * the legacy ES store. Best-effort — the caller wraps this so a failure never aborts the
     * delete (issue #35640).
     */
    private void clearStorePointer(final ContentletIndexOperations ops, final String logicalName)
            throws DotDataException {
        if (ops == router.osImpl()) {
            clearOsStorePointer(logicalName);
        } else {
            clearEsStorePointer(logicalName);
        }
    }

    /**
     * True when {@code storedName} resolves to the same logical (cluster-stripped, untagged) name
     * as {@code logicalName}. Lets a DB slot be matched regardless of the prefix/tag form it was
     * stored in.
     */
    private boolean matchesLogical(final String storedName, final String logicalName) {
        return storedName != null
                && logicalName.equals(IndexTag.OS.untag(indexAPI.removeClusterIdFromName(storedName)));
    }

    /** Clears any legacy ES-store ({@link IndiciesInfo}) slot pointing at {@code logicalName}. */
    private void clearEsStorePointer(final String logicalName) throws DotDataException {
        final IndiciesInfo info = legacyIndiciesAPI.loadIndicies();
        final IndiciesInfo.Builder builder = IndiciesInfo.Builder.copy(info);
        boolean changed = false;
        if (matchesLogical(info.getWorking(), logicalName))        { builder.setWorking(null);        changed = true; }
        if (matchesLogical(info.getLive(), logicalName))           { builder.setLive(null);           changed = true; }
        if (matchesLogical(info.getReindexWorking(), logicalName)) { builder.setReindexWorking(null); changed = true; }
        if (matchesLogical(info.getReindexLive(), logicalName))    { builder.setReindexLive(null);    changed = true; }
        if (matchesLogical(info.getSiteSearch(), logicalName))     { builder.setSiteSearch(null);     changed = true; }
        if (changed) {
            legacyIndiciesAPI.point(builder.build());
        }
    }

    /** Clears any OS versioned-store ({@link VersionedIndices}) slot pointing at {@code logicalName}. */
    private void clearOsStorePointer(final String logicalName) throws DotDataException {
        final Optional<VersionedIndices> existingOpt = versionedIndicesAPI.loadDefaultVersionedIndices();
        if (existingOpt.isEmpty()) {
            return;
        }
        final VersionedIndices existing = existingOpt.get();
        final VersionedIndicesImpl.Builder builder = VersionedIndicesImpl.builder();
        boolean changed = false;
        // Re-add every slot except the one(s) that resolve to the deleted logical name.
        if (existing.working().isPresent()) {
            if (matchesLogical(existing.working().get(), logicalName)) { changed = true; }
            else { builder.working(existing.working().get()); }
        }
        if (existing.live().isPresent()) {
            if (matchesLogical(existing.live().get(), logicalName)) { changed = true; }
            else { builder.live(existing.live().get()); }
        }
        if (existing.reindexWorking().isPresent()) {
            if (matchesLogical(existing.reindexWorking().get(), logicalName)) { changed = true; }
            else { builder.reindexWorking(existing.reindexWorking().get()); }
        }
        if (existing.reindexLive().isPresent()) {
            if (matchesLogical(existing.reindexLive().get(), logicalName)) { changed = true; }
            else { builder.reindexLive(existing.reindexLive().get()); }
        }
        if (existing.siteSearch().isPresent()) {
            if (matchesLogical(existing.siteSearch().get(), logicalName)) { changed = true; }
            else { builder.siteSearch(existing.siteSearch().get()); }
        }
        if (changed) {
            final VersionedIndices rebuilt = builder.build();
            if (rebuilt.hasAnyIndex()) {
                versionedIndicesAPI.saveIndices(rebuilt);
            } else {
                // The deleted index held the LAST populated slot. saveIndices contractually
                // rejects an empty record ("At least one index must be specified",
                // IndicesFactoryImpl), which would throw and leave the very pointer this method
                // exists to clear dangling — and on the next restart initOSCatchup would treat
                // that stale row as authoritative and recreate the deleted index empty. Remove
                // the store row instead (issue #35640, swicken review).
                versionedIndicesAPI.removeVersion(VersionedIndices.OPENSEARCH_3X);
            }
        }
    }

    public boolean optimize(List<String> indexNames) {
        return indexAPI.optimize(indexNames);
    }

    @Override
    public void addContentToIndex(final Contentlet content) throws DotDataException {
        addContentToIndex(content, true);
    }

    @Override
    public void addContentToIndex(final Contentlet parentContenlet,
            final boolean includeDependencies)
            throws DotDataException {

        if (null == parentContenlet || !UtilMethods.isSet(parentContenlet.getIdentifier())) {
            return;
        }

        Logger.info(this,
                "Indexing: ContentletIdentifier:" + parentContenlet.getIdentifier() + " " +
                        "ContentletInode: " + parentContenlet.getInode() + " " +
                        "ContentletTitle: " + parentContenlet.getTitle() + " " +
                        ", includeDependencies: " + includeDependencies +
                        ", policy: " + parentContenlet.getIndexPolicy());

        final List<Contentlet> contentToIndex = includeDependencies
                ? ImmutableList.<Contentlet>builder()
                .add(parentContenlet)
                .addAll(
                        loadDeps(parentContenlet)
                                .stream()
                                .peek((dep) -> dep.setIndexPolicy(
                                        parentContenlet.getIndexPolicyDependencies()))
                                .collect(Collectors.toList()))
                .build()
                : List.of(parentContenlet);

        if (parentContenlet.getIndexPolicy() == IndexPolicy.DEFER) {
            queueApi.addContentletsReindex(contentToIndex);
        } else if (!DbConnectionFactory.inTransaction()) {
            addContentToIndex(contentToIndex);
        } else {
            HibernateUtil.addSyncCommitListener(() -> addContentToIndex(contentToIndex));
        }
    }

    /**
     * Stops the full re-indexation process. This means clearing up the content queue and the
     * reindex journal.
     *
     * @throws DotDataException
     */
    @Override
    @WrapInTransaction
    public void stopFullReindexation() throws DotDataException {
        try {
            ReindexThread.pause();
            queueApi.deleteReindexRecords();
            fullReindexAbort();
        } finally {
            ReindexThread.unpause();
        }
    }

    @Override
    public void addContentToIndex(final List<Contentlet> contentToIndex) {

        // split the list on three possible subset, one with the default refresh strategy, second one is the
        // wait for and finally the immediate
        final List<List<Contentlet>> partitions =
                CollectionsUtils.partition(contentToIndex,
                        contentlet -> contentlet.getIndexPolicy() == IndexPolicy.DEFER,
                        contentlet -> contentlet.getIndexPolicy() == IndexPolicy.WAIT_FOR,
                        contentlet -> contentlet.getIndexPolicy() == IndexPolicy.FORCE);

        if (UtilMethods.isSet(partitions.get(0))) {
            this.indexContentListDefer(partitions.get(0));
        }

        if (UtilMethods.isSet(partitions.get(1))) {
            this.indexContentListWaitFor(partitions.get(1));
        }

        if (UtilMethods.isSet(partitions.get(2))) {
            this.indexContentListNow(partitions.get(2));
        }

    }

    private void indexContentListNow(final List<Contentlet> contentToIndex) {
        final IndexBulkRequest bulkRequest = createBulkRequest(contentToIndex);
        this.setRefreshPolicy(bulkRequest, IndexBulkRequest.RefreshPolicy.IMMEDIATE);
        putToIndex(bulkRequest);
        CacheLocator.getESQueryCache().clearCache();
        CacheLocator.getOSQueryCache().clearCache();
    } // indexContentListNow.

    private void indexContentListWaitFor(final List<Contentlet> contentToIndex) {
        final IndexBulkRequest bulkRequest = createBulkRequest(contentToIndex);
        this.setRefreshPolicy(bulkRequest, IndexBulkRequest.RefreshPolicy.WAIT_FOR);
        putToIndex(bulkRequest);
        CacheLocator.getESQueryCache().clearCache();
        CacheLocator.getOSQueryCache().clearCache();
    } // indexContentListWaitFor.

    private void indexContentListDefer(final List<Contentlet> contentToIndex) {
        final IndexBulkRequest bulkRequest = createBulkRequest(contentToIndex);
        putToIndex(bulkRequest);
    } // indexContentListDefer.

    /**
     * Sets the refresh policy on the batch.
     *
     * <p><strong>Routing:</strong> if the batch is a {@link DualIndexBulkRequest} (dual-write
     * phases) the policy is applied to <em>both</em> the ES and OS sub-batches independently,
     * because each sub-batch is submitted to a different vendor client.
     * In single-provider phases the batch is a plain vendor-specific request and is forwarded
     * to the only active write provider.</p>
     */
    @Override
    public void setRefreshPolicy(final IndexBulkRequest bulkRequest,
            final IndexBulkRequest.RefreshPolicy policy) {
        if (bulkRequest instanceof DualIndexBulkRequest) {
            // Dual-write phase: propagate policy to each provider's own batch
            final DualIndexBulkRequest dual = (DualIndexBulkRequest) bulkRequest;
            operationsES.setRefreshPolicy(dual.esReq, policy);
            operationsOS.setRefreshPolicy(dual.osReq, policy);
        } else {
            // Single-provider phase: forward to the sole active provider
            router.writeProviders().get(0).setRefreshPolicy(bulkRequest, policy);
        }
    }

    /**
     * Submits the accumulated batch synchronously.
     *
     * <p><strong>Routing:</strong> mirrors {@link #setRefreshPolicy} — a
     * {@link DualIndexBulkRequest} fans the submit out to both providers in parallel;
     * a plain batch is submitted to the single active write provider.
     * Each provider's submit is independent: a failure in one does not roll back the other.</p>
     */
    @Override
    public void putToIndex(final IndexBulkRequest bulkRequest) {
        if (bulkRequest instanceof DualIndexBulkRequest) {
            // Dual-write phases (1 and 2): ES is authoritative — its result is what callers observe.
            // OS is a shadow write: a failure must never surface to the caller or roll back the ES
            // commit. Isolate the OS call so divergence is logged but the operation still succeeds.
            final DualIndexBulkRequest dual = (DualIndexBulkRequest) bulkRequest;
            // Capture ES failure so OS is always called regardless — matches PhaseRouter.writeChecked contract.
            RuntimeException esException = null;
            try {
                operationsES.putToIndex(dual.esReq);
            } catch (final RuntimeException e) {
                esException = e;
            } catch (final Exception e) {
                esException = new DotRuntimeException(e.getMessage(), e);
            }
            try {
                operationsOS.putToIndex(dual.osReq);
            } catch (final Exception e) {
                Logger.warnAndDebug(this.getClass(),
                        "OS shadow write failed in putToIndex — "
                                + "OS index may diverge until next reindex. Cause: " + e.getMessage(), e);
            }
            if (esException != null) {
                throw esException;
            }
        } else {
            // Single-provider phase (0 or 3): forward to the sole active provider.
            // Failures propagate normally — there is no secondary provider to fall back to.
            router.writeProviders().get(0).putToIndex(bulkRequest);
        }
    }

    @Override
    public IndexBulkRequest createBulkRequest(final List<Contentlet> contentToIndex) {
        final IndexBulkRequest req = createBulkRequest();
        this.appendBulkRequestFromContentlets(req, contentToIndex);
        return req;
    }

    /**
     * Creates a new empty batch handle.
     *
     * <p><strong>Routing:</strong></p>
     * <ul>
     *   <li><strong>Single-provider phases (0 or 3):</strong> returns the native
     *       {@link IndexBulkRequest} from the sole active provider directly.</li>
     *   <li><strong>Dual-write phases (1 or 2):</strong> returns a {@link DualIndexBulkRequest}
     *       that wraps one native batch per provider (ES + OS). This ensures that index names,
     *       which differ between ES ({@link IndiciesAPI}) and OS ({@link VersionedIndicesAPI}),
     *       are resolved independently for each provider when operations are appended.</li>
     * </ul>
     * <p>Callers always receive an opaque {@link IndexBulkRequest}; the internal type is an
     * implementation detail that only this class and its private helpers inspect.</p>
     */
    @Override
    public IndexBulkRequest createBulkRequest() {
        final List<ContentletIndexOperations> providers = router.writeProviders();
        if (providers.size() == 1) {
            // Single-provider phase: delegate to the one active provider
            return providers.get(0).createBulkRequest();
        }
        // Dual-write phase: wrap one native batch per provider into a single handle
        return new DualIndexBulkRequest(
                operationsES.createBulkRequest(),
                operationsOS.createBulkRequest());
    }

    /**
     * Creates a self-flushing asynchronous bulk processor (used by ReindexThread).
     *
     * <p><strong>Routing:</strong> one inner processor is created per active <em>write provider</em>
     * and wrapped in a {@link CompositeBulkProcessor}. In phases 0 and 3 (single provider) the
     * composite degenerates to a one-entry list with no overhead. In dual-write phases (1 and 2)
     * both ES and OS processors are flushed in sequence on close.</p>
     */
    @Override
    public IndexBulkProcessor createBulkProcessor(final IndexBulkListener bulkListener) {
        // Create one inner processor per active write provider and wrap them in a composite.
        // In Phase 0 and Phase 3 writeProviders() has exactly one entry, so the composite
        // degenerates to a single-provider wrapper with no overhead.
        final List<ContentletIndexOperations> providers = router.writeProviders();
        final boolean isDualWrite = providers.size() > 1;
        final List<CompositeBulkProcessor.Entry> entries = new ArrayList<>();
        for (final ContentletIndexOperations ops : providers) {
            // OS is the shadow index in Phases 1 and 2 (dual-write): it replicates ES writes
            // but is not yet the source of truth. In Phase 3 isDualWrite=false, so shadow=false
            // and OS becomes the primary — failures propagate normally from that point.
            final boolean shadow = isDualWrite && ops == operationsOS;
            // Each provider gets its own listener so counters and log output stay per-provider.
            // The shadow OS listener never touches the reindex queue or triggers a rebuild.
            final IndexBulkListener listenerForOps = shadow
                    ? BulkProcessorListener.forShadowProvider(IndexTag.OS)
                    : bulkListener;
            entries.add(new CompositeBulkProcessor.Entry(ops, ops.createBulkProcessor(listenerForOps), shadow));
        }
        return new CompositeBulkProcessor(entries);
    }

    @Override
    public IndexBulkRequest appendBulkRequest(final IndexBulkRequest bulkRequest,
            final Collection<ReindexEntry> idxs)
            throws DotDataException {

        for (ReindexEntry idx : idxs) {
            appendBulkRequest(bulkRequest, idx);
        }
        return bulkRequest;
    }

    @Override
    public void appendToBulkProcessor(final IndexBulkProcessor bulk,
            final Collection<ReindexEntry> idxs) throws DotDataException {
        for (final ReindexEntry idx : idxs) {
            appendToBulkProcessorEntry(bulk, idx);
        }
    }

    private void appendToBulkProcessorEntry(final IndexBulkProcessor bulk,
            final ReindexEntry idx) throws DotDataException {
        Logger.debug(this, "Indexing document " + idx.getIdentToIndex());
        if (idx.isDelete()) {
            appendBulkRemoveRequestToProcessor(bulk, idx);
        } else {
            appendBulkRequestToProcessor(bulk, idx);
        }
    }

    @Override
    public IndexBulkRequest appendBulkRequest(IndexBulkRequest bulkRequest, final ReindexEntry idx)
            throws DotDataException {
        if (bulkRequest == null) {
            bulkRequest = createBulkRequest();
        }
        Logger.debug(this, "Indexing document " + idx.getIdentToIndex());
        if (idx.isDelete()) {
            appendBulkRemoveRequestInternal(bulkRequest, idx);
        } else {
            appendBulkRequestInternal(bulkRequest, idx);
        }
        return bulkRequest;
    }

    /**
     * Loads and null-filters the contentlet inodes for a reindex entry.
     * If no versions are found, deletes the queue entry and returns an empty map.
     */
    private Map<String, Contentlet> loadVersionInodes(final ReindexEntry idx) throws Exception {
        final List<ContentletVersionInfo> versions = APILocator.getVersionableAPI()
                .findContentletVersionInfos(idx.getIdentToIndex());
        final Map<String, Contentlet> inodes = new HashMap<>();
        for (final ContentletVersionInfo cvi : versions) {
            final String workingInode = cvi.getWorkingInode();
            final String liveInode = cvi.getLiveInode();
            inodes.put(workingInode,
                    APILocator.getContentletAPI().findInDb(workingInode).orElse(null));
            if (UtilMethods.isSet(liveInode) && !inodes.containsKey(liveInode)) {
                inodes.put(liveInode,
                        APILocator.getContentletAPI().findInDb(liveInode).orElse(null));
            }
        }
        inodes.values().removeIf(Objects::isNull);
        if (inodes.isEmpty()) {
            // If there is no content for this entry, delete it to avoid future attempts that will also fail
            APILocator.getReindexQueueAPI().deleteReindexEntry(idx);
            Logger.debug(this, String.format(
                    "Unable to find versions for content id: '%s'. Deleting content " +
                            "reindex entry.", idx.getIdentToIndex()));
        }
        return inodes;
    }

    /**
     * Generates a bulk request that adds the specified {@link ReindexEntry} to the index.
     *
     * @param req The {@link IndexBulkRequest} to append operations to.
     * @param idx  The entry containing the information of the Contentlet that will be indexed.
     * @throws DotDataException An error occurred when processing this request.
     */
    @CloseDBIfOpened
    private void appendBulkRequestInternal(final IndexBulkRequest req, final ReindexEntry idx)
            throws DotDataException {
        try {
            for (final Contentlet contentlet : loadVersionInodes(idx).values()) {
                Logger.debug(this, String.format("Indexing id: '%s', priority: '%s'",
                        contentlet.getInode(), idx.getPriority()));
                contentlet.setIndexPolicy(IndexPolicy.DEFER);
                addBulkRequest(req, List.of(contentlet), idx.isReindex());
            }
        } catch (final Exception e) {
            // An error occurred when trying to reindex the Contentlet. Flag it as "failed"
            APILocator.getReindexQueueAPI().markAsFailed(idx, e.getMessage());
        }
    }

    private void appendBulkRequestToProcessor(final IndexBulkProcessor proc,
            final ReindexEntry idx) throws DotDataException {
        try {
            for (final Contentlet contentlet : loadVersionInodes(idx).values()) {
                Logger.debug(this, String.format("Indexing id: '%s', priority: '%s'",
                        contentlet.getInode(), idx.getPriority()));
                contentlet.setIndexPolicy(IndexPolicy.DEFER);
                addBulkRequestToProcessor(proc, List.of(contentlet), idx.isReindex());
            }
        } catch (final Exception e) {
            APILocator.getReindexQueueAPI().markAsFailed(idx, e.getMessage());
        }
    }

    private void appendBulkRequestFromContentlets(final IndexBulkRequest req,
            final List<Contentlet> contentToIndex) {
        this.addBulkRequest(req, contentToIndex, false);
    }

    /**
     * Appends index operations for a single provider into its native bulk request.
     *
     * <p>Uses the provided {@code ops} and {@code indices} so that the caller controls
     * which provider and which index names receive the operations. Each contentlet in
     * {@code contentToIndex} is processed exactly once (duplicates are expected to have
     * already been removed by the caller).</p>
     */
    private void addBulkRequestToProvider(
            final IndexBulkRequest req,
            final ContentletIndexOperations ops,
            final ProviderIndices indices,
            final Set<Contentlet> contentToIndex,
            final boolean forReindex) {

        for (final Contentlet contentlet : contentToIndex) {
            final String id = contentlet.getIdentifier() + "_" + contentlet.getLanguageId()
                    + "_" + contentlet.getVariantId();
            String mapping = null;
            try {
                if (this.isWorking(contentlet)) {
                    mapping = Try.of(
                                    () -> objectMapper.writeValueAsString(
                                            getMappingAPI().toMap(contentlet)))
                            .getOrElseThrow(DotRuntimeException::new);
                    if (indices.working != null && (!forReindex || indices.reindexWorking == null)) {
                        ops.addIndexOp(req, indices.working, id, mapping);
                    }
                    if (indices.reindexWorking != null) {
                        ops.addIndexOp(req, indices.reindexWorking, id, mapping);
                    }
                }
                if (this.isLive(contentlet)) {
                    if (mapping == null) {
                        mapping = Try.of(
                                        () -> objectMapper.writeValueAsString(
                                                getMappingAPI().toMap(contentlet)))
                                .getOrElseThrow(DotRuntimeException::new);
                    }
                    if (indices.live != null && (!forReindex || indices.reindexLive == null)) {
                        ops.addIndexOp(req, indices.live, id, mapping);
                    }
                    if (indices.reindexLive != null) {
                        ops.addIndexOp(req, indices.reindexLive, id, mapping);
                    }
                }
                contentlet.markAsReindexed();
            } catch (Exception ex) {
                Logger.error(this,
                        "Can't get a mapping for contentlet with id_lang:" + id
                                + " Content data: " + contentlet.getMap(), ex);
                throw ex;
            }
        }
    }

    /**
     * Adds document index operations to {@code req} for every active write provider.
     *
     * <p>In dual-write phases, {@code req} is a {@link DualIndexBulkRequest}; each provider
     * receives its own native sub-request populated with its own index names.  In single-provider
     * phases, {@code req} belongs to the sole write provider and is used directly.</p>
     */
    private void addBulkRequest(final IndexBulkRequest req, final List<Contentlet> contentToIndex,
            final boolean forReindex) {
        if (contentToIndex == null || contentToIndex.isEmpty()) {
            return;
        }
        Logger.debug(this.getClass(),
                "Indexing " + contentToIndex.size() + " contents, starting with identifier [ "
                        + contentToIndex.get(0).getIdentifier() + "]");

        final Set<Contentlet> deduped = new HashSet<>(contentToIndex);
        // Detect whether this is a dual-write batch so we can dispatch each provider's
        // ops to its own native sub-request (with its own index names).
        final DualIndexBulkRequest dualReq =
                req instanceof DualIndexBulkRequest ? (DualIndexBulkRequest) req : null;

        // Fan-out: iterate over 1 provider (phases 0/3) or 2 providers (phases 1/2).
        for (final ContentletIndexOperations ops : router.writeProviders()) {
            final ProviderIndices indices = loadProviderIndicesQuietly(ops);
            if (indices == null) {
                // OS record not yet initialized — skip silently (logged inside loadProviderIndices)
                continue;
            }
            // Resolve which native batch this provider should write into.
            // In dual-write the batch is split; in single-provider phases req IS the batch.
            final IndexBulkRequest providerReq;
            if (dualReq != null) {
                providerReq = (ops == operationsES) ? dualReq.esReq : dualReq.osReq;
            } else {
                providerReq = req;
            }

            Logger.debug(this,
                    () -> "\n*********----------- Indexing via " + ops.getClass().getSimpleName()
                            + " on thread " + Thread.currentThread().getName());
            Logger.debug(this,
                    () -> "*********-----------  " + DbConnectionFactory.getConnection());
            Logger.debug(this, () -> "*********-----------  "
                    + ExceptionUtil.getCurrentStackTraceAsString(
                            Config.getIntProperty("stacktracelimit", 10))
                    + "\n");

            addBulkRequestToProvider(providerReq, ops, indices, deduped, forReindex);
        }
    }

    /**
     * Adds document index operations to the async {@code proc} for the current read provider.
     *
     * <p>The async processor is always owned by a single provider (the one returned by
     * {@link #createBulkProcessor}). Dual-write via the processor path is not yet supported;
     * full dual-write is handled by the synchronous bulk-request path instead.</p>
     */
    private void addBulkRequestToProcessor(final IndexBulkProcessor proc,
            final List<Contentlet> contentToIndex, final boolean forReindex) {
        if (contentToIndex == null || contentToIndex.isEmpty()) {
            return;
        }
        Logger.debug(this.getClass(),
                "Indexing " + contentToIndex.size() + " contents via processor, starting with identifier ["
                        + contentToIndex.get(0).getIdentifier() + "]");

        // Resolve provider targets from the composite processor.
        // CompositeBulkProcessor spans all write providers (1 in Phase 0/3, 2 in Phase 1/2).
        // A non-composite proc (e.g. from test code) is treated as a single-provider fallback.
        final List<CompositeBulkProcessor.Entry> targets = resolveProcessorTargets(proc);

        final Set<Contentlet> deduped = new HashSet<>(contentToIndex);
        for (final Contentlet contentlet : deduped) {
            final String id = contentlet.getIdentifier() + "_" + contentlet.getLanguageId()
                    + "_" + contentlet.getVariantId();
            try {
                final boolean isWorking = this.isWorking(contentlet);
                final boolean isLive    = this.isLive(contentlet);
                if (!isWorking && !isLive) {
                    continue;
                }
                // Compute mapping once; reuse across all providers for the same contentlet.
                final String mapping = Try.of(
                                () -> objectMapper.writeValueAsString(getMappingAPI().toMap(contentlet)))
                        .getOrElseThrow(DotRuntimeException::new);

                for (final CompositeBulkProcessor.Entry target : targets) {
                    final ProviderIndices indices = loadProviderIndicesQuietly(target.ops);
                    if (indices == null) {
                        Logger.warn(this, "No index info for provider — skipping processor indexing");
                        continue;
                    }
                    if (isWorking) {
                        if (indices.working != null && (!forReindex || indices.reindexWorking == null)) {
                            target.ops.addIndexOpToProcessor(target.proc, indices.working, id, mapping);
                        }
                        if (indices.reindexWorking != null) {
                            target.ops.addIndexOpToProcessor(target.proc, indices.reindexWorking, id, mapping);
                        }
                    }
                    if (isLive) {
                        if (indices.live != null && (!forReindex || indices.reindexLive == null)) {
                            target.ops.addIndexOpToProcessor(target.proc, indices.live, id, mapping);
                        }
                        if (indices.reindexLive != null) {
                            target.ops.addIndexOpToProcessor(target.proc, indices.reindexLive, id, mapping);
                        }
                    }
                }
                contentlet.markAsReindexed();
            } catch (Exception ex) {
                Logger.error(this,
                        "Can't get a mapping for contentlet with id_lang:" + id
                                + " Content data: " + contentlet.getMap(), ex);
                throw ex;
            }
        }
    }

    /**
     * Extracts the {@code (ops, proc)} entry list from the given processor.
     *
     * <p>If {@code proc} is a {@link CompositeBulkProcessor} (the normal production case),
     * returns all its entries — one per active write provider. Otherwise wraps {@code proc}
     * in a single-entry list backed by the current read provider, providing a safe fallback
     * for tests or legacy callers that create processors outside of
     * {@link #createBulkProcessor(IndexBulkListener)}.</p>
     */
    private List<CompositeBulkProcessor.Entry> resolveProcessorTargets(
            final IndexBulkProcessor proc) {
        if (proc instanceof CompositeBulkProcessor) {
            return ((CompositeBulkProcessor) proc).entries();
        }
        // Legacy processor (not a CompositeBulkProcessor): wrap as a non-shadow single entry.
        // OS writes are silently dropped in dual-write phases — warn so operators can detect this.
        Logger.warn(this.getClass(),
                "resolveProcessorTargets: non-CompositeBulkProcessor detected — OS writes will " +
                "be dropped in dual-write phases. Use createBulkProcessor(IndexBulkListener) " +
                "to get a phase-aware processor.");
        return List.of(new CompositeBulkProcessor.Entry(router.readProvider(), proc, false));
    }

    private boolean isWorking(final Contentlet contentlet) {

        boolean isWorking = false;

        try {
            isWorking = contentlet.isWorking();
        } catch (Exception e) {
            Logger.debug(this, e.getMessage(), e);
            Logger.warn(this, e.getMessage(), e);
        }

        return isWorking;
    }

    private boolean isLive(final Contentlet contentlet) {

        boolean isLive = false;

        try {
            isLive = contentlet.isLive();
        } catch (Exception e) {
            Logger.debug(this, e.getMessage(), e);
            Logger.warn(this, e.getMessage(), e);
            isLive = false;
        }

        return isLive;
    }

    @CloseDBIfOpened
    @SuppressWarnings("unchecked")
    private List<Contentlet> loadDeps(final Contentlet parentContentlet) {

        final List<String> depsIdentifiers =  Sneaky.sneak(() ->
                getMappingAPI().dependenciesLeftToReindex(parentContentlet));

        if (!UtilMethods.isSet(depsIdentifiers)) {
            return Collections.emptyList();
        }

        final String templateQuery = String.format(SELECT_CONTENTLET_VERSION_INFO,
                String.join(",", Collections.nCopies(depsIdentifiers.size(), "?")));

        final DotConnect dotConnect = new DotConnect().setSQL(templateQuery);
        depsIdentifiers.forEach(dotConnect::addParam);

        final List<Map<String, String>> versionInfoMapResults =
                Sneaky.sneak(dotConnect::loadResults);

        final List<String> inodes = versionInfoMapResults.stream()
                .map(versionInfoMap -> {
                    final String workingInode = versionInfoMap.get("working_inode");
                    final String liveInode = versionInfoMap.get("live_inode");

                    if (UtilMethods.isSet(liveInode) && !workingInode.equals(liveInode)) {
                        return Arrays.asList(workingInode, liveInode);
                    }

                    return Arrays.asList(workingInode);
                })
                .flatMap(Collection::stream)
                .filter(UtilMethods::isSet)
                .distinct()
                .collect(Collectors.toList());

        return  Sneaky.sneak(() -> APILocator.getContentletAPI()
                .findContentlets(inodes));
    }

    public void removeContentFromIndex(final Contentlet content) throws DotHibernateException {
        removeContentFromIndex(content, false);
    }

    /**
     * Appends delete operations for a reindex entry to the batch for every active write provider.
     *
     * <p><strong>Routing:</strong> same fan-out + sub-request resolution pattern as
     * {@link #addBulkRequest}: iterates {@code router.writeProviders()}, resolves each
     * provider's index names via {@link ProviderIndices}, and writes into the correct
     * native sub-batch ({@link DualIndexBulkRequest#esReq} or {@link DualIndexBulkRequest#osReq})
     * in dual-write phases, or directly into {@code req} in single-provider phases.</p>
     */
    private void appendBulkRemoveRequestInternal(final IndexBulkRequest req,
            final ReindexEntry entry) throws DotDataException {
        final List<Language> languages = APILocator.getLanguageAPI().getLanguages();
        final List<Variant> variants = APILocator.getVariantAPI().getVariants();
        // Detect dual-write batch upfront to avoid repeated instanceof checks in the loop
        final DualIndexBulkRequest dualReq = req instanceof DualIndexBulkRequest ? (DualIndexBulkRequest) req : null;

        // Fan-out: delete from every active index of every write provider
        for (final ContentletIndexOperations ops : router.writeProviders()) {
            final ProviderIndices indices = loadProviderIndicesQuietly(ops);
            if (indices == null) {
                continue;
            }
            final IndexBulkRequest providerReq;
            if (dualReq != null) {
                providerReq = (ops == operationsES) ? dualReq.esReq : dualReq.osReq;
            } else {
                providerReq = req;
            }
            for (final Language language : languages) {
                for (final String index : indices.activeIndices()) {
                    for (final Variant variant : variants) {
                        final String id = entry.getIdentToIndex()
                                + StringPool.UNDERLINE + language.getId()
                                + StringPool.UNDERLINE + variant.name();
                        Logger.debug(this.getClass(), "deleting:" + id);
                        ops.addDeleteOp(providerReq, index, id);
                    }
                }
            }
        }
    }

    private void appendBulkRemoveRequestToProcessor(final IndexBulkProcessor proc,
            final ReindexEntry entry) throws DotDataException {
        final List<Language> languages = APILocator.getLanguageAPI().getLanguages();
        final List<Variant> variants   = APILocator.getVariantAPI().getVariants();
        for (final CompositeBulkProcessor.Entry target : resolveProcessorTargets(proc)) {
            final ProviderIndices indices = loadProviderIndicesQuietly(target.ops);
            if (indices == null) {
                continue;
            }
            for (final Language language : languages) {
                for (final String index : indices.activeIndices()) {
                    for (final Variant variant : variants) {
                        final String id = entry.getIdentToIndex()
                                + StringPool.UNDERLINE + language.getId()
                                + StringPool.UNDERLINE + variant.name();
                        Logger.debug(this.getClass(), "deleting:" + id);
                        target.ops.addDeleteOpToProcessor(target.proc, index, id);
                    }
                }
            }
        }
    }

    @Override
    @VisibleForTesting
    public IndexBulkRequest appendBulkRemoveRequest(final IndexBulkRequest bulkRequest,
            final ReindexEntry entry) throws DotDataException {
        appendBulkRemoveRequestInternal(bulkRequest, entry);
        return bulkRequest;
    }

    @WrapInTransaction
    private void removeContentFromIndex(final Contentlet content, final boolean onlyLive,
            final List<Relationship> relationships)
            throws DotHibernateException {

        final boolean indexIsNotDefer = IndexPolicy.DEFER != content.getIndexPolicy();

        try {

            if (indexIsNotDefer) {

                this.handleRemoveIndexNotDefer(content, onlyLive, relationships);
            } else {
                // add a commit listener to index the contentlet if the entire
                // transaction finish clean
                HibernateUtil.addCommitListener(
                        content.getInode() + ReindexRunnable.Action.REMOVING,
                        new RemoveReindexRunnable(content, onlyLive, relationships));
            }
        } catch (DotDataException | DotSecurityException | DotMappingException e1) {
            throw new DotHibernateException(e1.getMessage(), e1);
        }
    } // removeContentFromIndex.

    private void handleRemoveIndexNotDefer(final Contentlet content, final boolean onlyLive,
            final List<Relationship> relationships)
            throws DotSecurityException, DotMappingException, DotDataException {

        removeContentAndProcessDependencies(content, relationships, onlyLive,
                content.getIndexPolicy(),
                content.getIndexPolicyDependencies());
    } // handleRemoveIndexNotDefer.

    /**
     * Remove ReindexRunnable runnable
     */
    private class RemoveReindexRunnable extends ReindexRunnable {

        private final Contentlet contentlet;
        private final boolean onlyLive;
        private final List<Relationship> relationships;

        public RemoveReindexRunnable(final Contentlet contentlet, final boolean onlyLive,
                final List<Relationship> relationships) {

            super(contentlet, ReindexRunnable.Action.REMOVING);
            this.contentlet = contentlet;
            this.onlyLive = onlyLive;
            this.relationships = relationships;
        }

        public void run() {

            try {
                removeContentAndProcessDependencies(this.contentlet, this.relationships,
                        this.onlyLive, IndexPolicy.DEFER,
                        IndexPolicy.DEFER);
            } catch (Exception ex) {
                throw new DotRuntimeException(ex.getMessage(), ex);
            }
        }
    }

    private void removeContentAndProcessDependencies(final Contentlet contentlet,
            final List<Relationship> relationships,
            final boolean onlyLive, final IndexPolicy indexPolicy,
            final IndexPolicy indexPolicyDependencies)
            throws DotDataException, DotSecurityException, DotMappingException {

        final String id = builder(contentlet.getIdentifier(), StringPool.UNDERLINE,
                contentlet.getLanguageId(), StringPool.UNDERLINE, contentlet.getVariantId())
                .toString();
        // Create one bulk request per write provider (DualIndexBulkRequest in dual-write phases)
        final IndexBulkRequest bulkRequest = createBulkRequest();

        // Apply the caller's refresh policy to all providers
        if (indexPolicy == IndexPolicy.FORCE) {
            this.setRefreshPolicy(bulkRequest, IndexBulkRequest.RefreshPolicy.IMMEDIATE);
        } else if (indexPolicy == IndexPolicy.WAIT_FOR) {
            this.setRefreshPolicy(bulkRequest, IndexBulkRequest.RefreshPolicy.WAIT_FOR);
        }

        final DualIndexBulkRequest dualReq = bulkRequest instanceof DualIndexBulkRequest ? (DualIndexBulkRequest) bulkRequest : null;

        for (final ContentletIndexOperations ops : router.writeProviders()) {
            final ProviderIndices indices = loadProviderIndicesQuietly(ops);
            if (indices == null) {
                continue;
            }
            final IndexBulkRequest providerReq;
            if (dualReq != null) {
                providerReq = (ops == operationsES) ? dualReq.esReq : dualReq.osReq;
            } else {
                providerReq = bulkRequest;
            }
            if (indices.live != null) {
                ops.addDeleteOp(providerReq, indices.live, id);
            }
            if (indices.reindexLive != null) {
                ops.addDeleteOp(providerReq, indices.reindexLive, id);
            }
            if (!onlyLive) {
                if (indices.working != null) {
                    ops.addDeleteOp(providerReq, indices.working, id);
                }
                if (indices.reindexWorking != null) {
                    ops.addDeleteOp(providerReq, indices.reindexWorking, id);
                }
            }
        }

        if (!onlyLive && UtilMethods.isSet(relationships)) {
            // Reindex relationship fields pointing to this content to avoid stale refs
            reindexDependenciesForDeletedContent(contentlet, relationships,
                    indexPolicyDependencies);
        }

        putToIndex(bulkRequest);

        //Delete query cache when a new content has been reindexed
        CacheLocator.getESQueryCache().clearCache();
        CacheLocator.getOSQueryCache().clearCache();
    }

    private void reindexDependenciesForDeletedContent(final Contentlet contentlet,
            final List<Relationship> relationships,
            final IndexPolicy indexPolicy)
            throws DotDataException, DotSecurityException, DotMappingException {

        for (final Relationship relationship : relationships) {

            final boolean isSameStructRelationship = APILocator.getRelationshipAPI()
                    .sameParentAndChild(relationship);

            final String query = (isSameStructRelationship)
                    ? builder("+type:content +(", relationship.getRelationTypeValue(), "-parent:",
                    contentlet.getIdentifier(),
                    StringPool.SPACE, relationship.getRelationTypeValue(), "-child:",
                    contentlet.getIdentifier(), ") ").toString()
                    : builder("+type:content +", relationship.getRelationTypeValue(), ":",
                            contentlet.getIdentifier()).toString();

            final List<Contentlet> related =
                    APILocator.getContentletAPI()
                            .search(query, -1, 0, null, APILocator.getUserAPI().getSystemUser(),
                                    false);

            switch (indexPolicy) {

                case WAIT_FOR:
                    indexContentListWaitFor(related);
                    break;
                case FORCE:
                    indexContentListNow(related);
                    break;
                default: // DEFER
                    indexContentListDefer(related);
            }
        }
    }

    @WrapInTransaction
    public void removeContentFromIndex(final Contentlet content, final boolean onlyLive)
            throws DotHibernateException {

        if (content == null || !UtilMethods.isSet(content.getIdentifier())) {
            return;
        }

        List<Relationship> relationships = APILocator.getRelationshipAPI()
                .byContentType(content.getContentType());

        // add a commit listener to index the contentlet if the entire
        // transaction finish clean
        removeContentFromIndex(content, onlyLive, relationships);

    }

    public void removeContentFromLiveIndex(final Contentlet content) throws DotHibernateException {
        removeContentFromIndex(content, true);
    }

    /**
     * Removes all content from the index for the given structure inode
     * this one does go to the db therefore it needs the DB closed annotation
     * @param structureInode
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @CloseDBIfOpened
    @Override
    public void removeContentFromIndexByStructureInode(final String structureInode)
            throws DotDataException, DotSecurityException {
        final ContentType contentType = APILocator.getContentTypeAPI(
                APILocator.systemUser()).find(structureInode);
        if (contentType == null) {
            throw new DotDataException(
                    "ContentType with Inode or VarName: " + structureInode + "not found");
        }
        removeContentFromIndexByContentType(contentType);
    }

    /**
     * Removes all content from the index for the given content type.
     * Does NOT go to the DB, so no {@code @CloseDBIfOpened} annotation is needed.
     *
     * <p><strong>Routing:</strong> fans out to all active write providers so that
     * documents are removed from both ES and OS indices in dual-write phases.</p>
     *
     * @param contentType the content type whose documents should be removed
     * @throws DotDataException if the operation fails for any provider
     */
    @Override
    public void removeContentFromIndexByContentType(final ContentType contentType)
            throws DotDataException {
        for (final ContentletIndexOperations ops : router.writeProviders()) {
            ops.removeContentFromIndexByContentType(contentType);
        }
        CacheLocator.getESQueryCache().clearCache();
        CacheLocator.getOSQueryCache().clearCache();
    }

    /**
     * Aborts an in-progress full reindex by discarding the reindex index slots without
     * promoting them to active. After this call, {@link #isInFullReindex()} returns
     * {@code false} and the system continues serving reads from the previously active indices.
     *
     * <h3>Phase-aware behavior</h3>
     * <ul>
     *   <li><strong>Phase 3 (migration complete)</strong> — Only {@code versionedIndicesAPI}
     *       is cleared. {@code legacyIndiciesAPI} is never consulted.</li>
     *   <li><strong>Phases 1 and 2 (dual-write)</strong> — ES reindex slots are cleared in
     *       {@code legacyIndiciesAPI}, then OS reindex slots are cleared best-effort in
     *       {@code versionedIndicesAPI}. A failure in the OS clear is logged but must not
     *       abort the ES abort.</li>
     *   <li><strong>Phase 0 (ES only)</strong> — Only {@code legacyIndiciesAPI} is cleared.</li>
     * </ul>
     */
    public void fullReindexAbort() {
        try {
            if (!isInFullReindex()) {
                return;
            }

            if (isMigrationComplete()) {
                // ── Phase 3: OS only ─────────────────────────────────────────
                // Preserve active working/live; omit reindexWorking/reindexLive
                // from the builder → they clear to Optional.empty().
                final Optional<VersionedIndices> osExisting =
                        versionedIndicesAPI.loadDefaultVersionedIndices();
                final VersionedIndicesImpl.Builder osBuilder = VersionedIndicesImpl.builder();
                osExisting.flatMap(VersionedIndices::working).ifPresent(osBuilder::working);
                osExisting.flatMap(VersionedIndices::live).ifPresent(osBuilder::live);
                versionedIndicesAPI.saveIndices(osBuilder.build());
                return;
            }

            // ── Phases 0, 1, 2: ES-primary ───────────────────────────────────
            // Preserve active working/live/siteSearch; leave reindexWorking and
            // reindexLive null → cleared from the store.
            final IndiciesInfo info = legacyIndiciesAPI.loadIndicies();
            final IndiciesInfo newInfo = new IndiciesInfo.Builder()
                    .setWorking(info.getWorking())
                    .setLive(info.getLive())
                    .setSiteSearch(info.getSiteSearch())
                    .build();
            legacyIndiciesAPI.point(newInfo);

            // ── OS mirror (Phases 1 and 2, best-effort) ──────────────────────
            // Clear OS reindex slots in lock-step with ES. A failure here must
            // not roll back the ES abort — OS is a shadow copy.
            if (isMigrationStarted()) {
                try {
                    final Optional<VersionedIndices> osExisting =
                            versionedIndicesAPI.loadDefaultVersionedIndices();
                    final VersionedIndicesImpl.Builder osBuilder = VersionedIndicesImpl.builder();
                    osExisting.flatMap(VersionedIndices::working).ifPresent(osBuilder::working);
                    osExisting.flatMap(VersionedIndices::live).ifPresent(osBuilder::live);
                    // reindexWorking / reindexLive intentionally omitted → cleared
                    versionedIndicesAPI.saveIndices(osBuilder.build());
                } catch (Exception osEx) {
                    Logger.warn(this, "Could not clear OS reindex slots during abort", osEx);
                }
            }

        } catch (Exception e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    public boolean isDotCMSIndexName(final String indexName) {
        return IndexType.WORKING.is(indexName) || IndexType.LIVE.is(indexName);
    }

    /**
     * Returns the combined list of closed dotCMS indices across all active write providers.
     *
     * <p><strong>Phase-aware</strong>: delegates to {@link IndexAPIImpl#getClosedIndexes()},
     * which returns ES-only in Phase 0, aggregates ES + OS in Phases 1 and 2, and returns
     * OS-only in Phase 3.</p>
     */
    public List<String> listDotCMSClosedIndices() {
        return indexAPI.getClosedIndexes();
    }

    /**
     * Returns the combined list of open dotCMS working and live indices across all active
     * write providers.
     *
     * <p><strong>Phase-aware</strong>: delegates to {@link IndexAPIImpl#getIndices(boolean, boolean)},
     * which returns ES-only in Phase 0, aggregates ES + OS in Phases 1 and 2, and returns
     * OS-only in Phase 3.</p>
     */
    public List<String> listDotCMSIndices() {
        return indexAPI.getIndices(true, false);
    }


    /**
     * Points the active index slot (working or live) to the given index name, clearing the
     * corresponding reindex slot if the promoted index was the one pending reindex.
     *
     * <h3>Phase-aware behavior</h3>
     * <ul>
     *   <li><strong>Phase 3</strong> — Only {@code versionedIndicesAPI} is updated.
     *       Failures propagate as exceptions (this is the primary store).</li>
     *   <li><strong>Phases 1 and 2</strong> — ES store is updated first (primary), then
     *       OS store is mirrored best-effort. A failure in the OS mirror is logged but
     *       does not roll back the ES update.</li>
     *   <li><strong>Phase 0</strong> — Only ES store is updated.</li>
     * </ul>
     */
    public void activateIndex(final String indexName) throws DotDataException {
        if (indexName == null) {
            throw new DotRuntimeException("Index cannot be null");
        }

        // Audit log: runs regardless of phase so the activation is always traceable.
        final User currentUser = Try.of(() -> PortalUtil.getUser(
                HttpServletRequestThreadLocal.INSTANCE.getRequest())).getOrNull();
        if (currentUser != null) {
            Logger.info(this, "Index activation (" + indexName + ") performed by user: "
                    + currentUser.getUserId() + " (" + currentUser.getEmailAddress()
                    + ") at " + new Date());
        } else {
            Logger.info(this, "Index activation (" + indexName
                    + ") performed by system user at " + new Date());
        }

        if (isMigrationComplete()) {
            // ── Phase 3: OS only ─────────────────────────────────────────────
            // legacyIndiciesAPI must not be consulted — ES is decommissioned.
            // Failure is fatal: this is the primary store, not a shadow copy.
            final String osPhysical = operationsOS.toPhysicalName(indexName);
            final Optional<VersionedIndices> osExisting =
                    versionedIndicesAPI.loadDefaultVersionedIndices();
            final VersionedIndicesImpl.Builder osBuilder = VersionedIndicesImpl.builder();
            if (IndexType.WORKING.is(indexName)) {
                osBuilder.working(osPhysical);
                osExisting.flatMap(VersionedIndices::live).ifPresent(osBuilder::live);
                // Clear reindexWorking if it was the promoted index
                osExisting.flatMap(VersionedIndices::reindexWorking)
                        .filter(rw -> !rw.equals(osPhysical))
                        .ifPresent(osBuilder::reindexWorking);
                osExisting.flatMap(VersionedIndices::reindexLive).ifPresent(osBuilder::reindexLive);
            } else if (IndexType.LIVE.is(indexName)) {
                osBuilder.live(osPhysical);
                osExisting.flatMap(VersionedIndices::working).ifPresent(osBuilder::working);
                osExisting.flatMap(VersionedIndices::reindexWorking).ifPresent(osBuilder::reindexWorking);
                // Clear reindexLive if it was the promoted index
                osExisting.flatMap(VersionedIndices::reindexLive)
                        .filter(rl -> !rl.equals(osPhysical))
                        .ifPresent(osBuilder::reindexLive);
            }
            versionedIndicesAPI.saveIndices(osBuilder.build());
            return;
        }

        // ── Phases 0, 1, 2: ES-primary ───────────────────────────────────────
        final IndiciesInfo info = legacyIndiciesAPI.loadIndicies();
        final IndiciesInfo.Builder builder = IndiciesInfo.Builder.copy(info);
        if (IndexType.WORKING.is(indexName)) {
            builder.setWorking(indexAPI.getNameWithClusterIDPrefix(indexName));
            if (indexAPI.getNameWithClusterIDPrefix(indexName).equals(info.getReindexWorking())) {
                builder.setReindexWorking(null);
            }
        } else if (IndexType.LIVE.is(indexName)) {
            builder.setLive(indexAPI.getNameWithClusterIDPrefix(indexName));
            if (indexAPI.getNameWithClusterIDPrefix(indexName).equals(info.getReindexLive())) {
                builder.setReindexLive(null);
            }
        }
        legacyIndiciesAPI.point(builder.build());

        // ── OS mirror (Phases 1 and 2, best-effort) ──────────────────────────
        // Index names may differ between providers in a migration-catchup scenario.
        // Failure is non-fatal: OS is a shadow copy during these phases.
        if (isMigrationStarted()) {
            try {
                final String osPhysical = operationsOS.toPhysicalName(indexName);
                final Optional<VersionedIndices> osExisting =
                        versionedIndicesAPI.loadDefaultVersionedIndices();
                final VersionedIndicesImpl.Builder osBuilder = VersionedIndicesImpl.builder();
                if (IndexType.WORKING.is(indexName)) {
                    osBuilder.working(osPhysical);
                    osExisting.flatMap(VersionedIndices::live).ifPresent(osBuilder::live);
                    osExisting.flatMap(VersionedIndices::reindexWorking)
                            .filter(rw -> !rw.equals(osPhysical))
                            .ifPresent(osBuilder::reindexWorking);
                    osExisting.flatMap(VersionedIndices::reindexLive).ifPresent(osBuilder::reindexLive);
                } else if (IndexType.LIVE.is(indexName)) {
                    osBuilder.live(osPhysical);
                    osExisting.flatMap(VersionedIndices::working).ifPresent(osBuilder::working);
                    osExisting.flatMap(VersionedIndices::reindexWorking).ifPresent(osBuilder::reindexWorking);
                    osExisting.flatMap(VersionedIndices::reindexLive)
                            .filter(rl -> !rl.equals(osPhysical))
                            .ifPresent(osBuilder::reindexLive);
                }
                versionedIndicesAPI.saveIndices(osBuilder.build());
            } catch (Exception e) {
                Logger.warn(this, "Could not mirror index activation to OS store for index: "
                        + indexName, e);
            }
        }
    }

    /**
     * Clears the index slot (working, live, reindexWorking, or reindexLive) that corresponds
     * to the given index name, effectively disconnecting it from the active index set.
     *
     * <h3>Phase-aware behavior</h3>
     * <ul>
     *   <li><strong>Phase 3</strong> — Only {@code versionedIndicesAPI} is updated.
     *       Failures propagate as exceptions (this is the primary store).</li>
     *   <li><strong>Phases 1 and 2</strong> — ES store is updated first (primary), then
     *       OS store is mirrored best-effort. A failure in the OS mirror is logged but
     *       does not roll back the ES update.</li>
     *   <li><strong>Phase 0</strong> — Only ES store is updated.</li>
     * </ul>
     */
    public void deactivateIndex(String indexName) throws DotDataException, IOException {
        if (isMigrationComplete()) {
            // ── Phase 3: OS only ─────────────────────────────────────────────
            // Copy all existing OS slots, skipping the one being deactivated.
            // An unset slot defaults to Optional.empty() in VersionedIndicesImpl.
            // Failure is fatal: this is the primary store, not a shadow copy.
            final Optional<VersionedIndices> osExisting =
                    versionedIndicesAPI.loadDefaultVersionedIndices();
            final VersionedIndicesImpl.Builder osBuilder = VersionedIndicesImpl.builder();
            if (!IndexType.WORKING.is(indexName)) {
                osExisting.flatMap(VersionedIndices::working).ifPresent(osBuilder::working);
            }
            if (!IndexType.LIVE.is(indexName)) {
                osExisting.flatMap(VersionedIndices::live).ifPresent(osBuilder::live);
            }
            if (!IndexType.REINDEX_WORKING.is(indexName)) {
                osExisting.flatMap(VersionedIndices::reindexWorking).ifPresent(osBuilder::reindexWorking);
            }
            if (!IndexType.REINDEX_LIVE.is(indexName)) {
                osExisting.flatMap(VersionedIndices::reindexLive).ifPresent(osBuilder::reindexLive);
            }
            versionedIndicesAPI.saveIndices(osBuilder.build());
            return;
        }

        // ── Phases 0, 1, 2: ES-primary ───────────────────────────────────────
        final IndiciesInfo info = legacyIndiciesAPI.loadIndicies();
        final IndiciesInfo.Builder builder = IndiciesInfo.Builder.copy(info);
        if (IndexType.WORKING.is(indexName)) {
            builder.setWorking(null);
        } else if (IndexType.LIVE.is(indexName)) {
            builder.setLive(null);
        } else if (IndexType.REINDEX_WORKING.is(indexName)) {
            builder.setReindexWorking(null);
        } else if (IndexType.REINDEX_LIVE.is(indexName)) {
            builder.setReindexLive(null);
        }
        legacyIndiciesAPI.point(builder.build());

        // ── OS mirror (Phases 1 and 2, best-effort) ──────────────────────────
        // Copy all existing OS slots, skipping the slot that matches the deactivated index type.
        // Failure is non-fatal: OS is a shadow copy during these phases.
        if (isMigrationStarted()) {
            try {
                final Optional<VersionedIndices> osExisting =
                        versionedIndicesAPI.loadDefaultVersionedIndices();
                final VersionedIndicesImpl.Builder osBuilder = VersionedIndicesImpl.builder();
                if (!IndexType.WORKING.is(indexName)) {
                    osExisting.flatMap(VersionedIndices::working).ifPresent(osBuilder::working);
                }
                if (!IndexType.LIVE.is(indexName)) {
                    osExisting.flatMap(VersionedIndices::live).ifPresent(osBuilder::live);
                }
                if (!IndexType.REINDEX_WORKING.is(indexName)) {
                    osExisting.flatMap(VersionedIndices::reindexWorking).ifPresent(osBuilder::reindexWorking);
                }
                if (!IndexType.REINDEX_LIVE.is(indexName)) {
                    osExisting.flatMap(VersionedIndices::reindexLive).ifPresent(osBuilder::reindexLive);
                }
                versionedIndicesAPI.saveIndices(osBuilder.build());
            } catch (Exception e) {
                Logger.warn(this, "Could not mirror index deactivation to OS store for index: "
                        + indexName, e);
            }
        }
    }

    /**
     * Returns the number of documents in the given index.
     *
     * <p><strong>Routing:</strong> delegates to the current <em>read provider</em> only.
     * In phases 0/1 that is ES; in phases 2/3 that is OS. This is a point-in-time
     * observation from the authoritative read backend, not an aggregate across providers.</p>
     */
    @Override
    public long getIndexDocumentCount(final String indexName) {
        return router.readProvider().getIndexDocumentCount(
                indexAPI.getNameWithClusterIDPrefix(indexName));
    }

    /**
     * Returns the document count for the given index on the provider identified by {@code tag}.
     * This method forces the use of a specific index determined by IndexTag Parameter.
     * The default index to hit is ES
     */
    long getIndexDocumentCount(final String indexName, final IndexTag tag) {
        final ContentletIndexOperations operations =
                tag == IndexTag.OS ? router.osImpl() : router.esImpl();
        return operations.getIndexDocumentCount(operations.toPhysicalName(indexName));
    }

    /**
     * Whether the status/display getters ({@link #getCurrentIndex()}, {@link #getNewIndex()}
     * and {@link #getActiveIndexName(String)}) report indices from the OS store.
     *
     * <p><strong>This is a display contract, not the read path.</strong> The actual read
     * routing ({@code router.readProvider()}) switches to OS in Phase&nbsp;2; these getters
     * only drive what the maintenance dashboard marks as <em>active</em>/<em>building</em> and
     * what the index REST/AJAX status endpoints report. Per product decision they keep
     * reporting the ES pointers until the migration is complete (Phase&nbsp;3), even though OS
     * already serves reads in Phase&nbsp;2.</p>
     *
     * <p>This keeps the dashboard's flags aligned with the indices it actually shows:
     * {@code MigrationIndexVisibility} hides {@code .os} indices before Phase&nbsp;3, so marking
     * an OS index "active" in Phase&nbsp;2 would point at a row the operator cannot see. <strong>Do
     * not change this back to {@code isReadEnabled()} to "match" the read provider</strong> — the
     * divergence between display and read path in Phase&nbsp;2 is intentional.</p>
     */
    private boolean displayUsesOsStore() {
        return isMigrationComplete();
    }

    public synchronized List<String> getCurrentIndex() throws DotDataException {

        final LinkedHashSet<String> result = new LinkedHashSet<>();

        if (displayUsesOsStore()) {
            versionedIndicesAPI.loadDefaultVersionedIndices().ifPresent(vi -> {
                vi.working().map(indexAPI::removeClusterIdFromName).ifPresent(result::add);
                vi.live().map(indexAPI::removeClusterIdFromName).ifPresent(result::add);
            });
        } else {
            final IndiciesInfo info = legacyIndiciesAPI.loadIndicies();
            Optional.ofNullable(info.getWorking())
                    .map(indexAPI::removeClusterIdFromName).ifPresent(result::add);
            Optional.ofNullable(info.getLive())
                    .map(indexAPI::removeClusterIdFromName).ifPresent(result::add);
        }

        return new ArrayList<>(result);
    }

    public synchronized List<String> getNewIndex() throws DotDataException {
        final LinkedHashSet<String> result = new LinkedHashSet<>();

        if (displayUsesOsStore()) {
            // Phase 3: report OS reindex slots (see displayUsesOsStore).
            versionedIndicesAPI.loadDefaultVersionedIndices().ifPresent(vi -> {
                vi.reindexWorking().map(indexAPI::removeClusterIdFromName).ifPresent(result::add);
                vi.reindexLive().map(indexAPI::removeClusterIdFromName).ifPresent(result::add);
            });
        } else {
            // Phases 0/1/2: report ES reindex slots for display.
            final IndiciesInfo info = legacyIndiciesAPI.loadIndicies();
            Optional.ofNullable(info.getReindexWorking())
                    .map(indexAPI::removeClusterIdFromName).ifPresent(result::add);
            Optional.ofNullable(info.getReindexLive())
                    .map(indexAPI::removeClusterIdFromName).ifPresent(result::add);
        }

        return new ArrayList<>(result);
    }

    public String getActiveIndexName(final String type) throws DotDataException {
        if (displayUsesOsStore()) {
            // Phase 3: resolve from the OS store (see displayUsesOsStore).
            final Optional<VersionedIndices> vi =
                    versionedIndicesAPI.loadDefaultVersionedIndices();
            if (IndexType.WORKING.is(type)) {
                return vi.flatMap(VersionedIndices::working)
                        .map(indexAPI::removeClusterIdFromName).orElse(null);
            } else if (IndexType.LIVE.is(type)) {
                return vi.flatMap(VersionedIndices::live)
                        .map(indexAPI::removeClusterIdFromName).orElse(null);
            }
        } else {
            // Phases 0/1/2: resolve from the ES store for display.
            final IndiciesInfo info = legacyIndiciesAPI.loadIndicies();
            if (IndexType.WORKING.is(type)) {
                return indexAPI.removeClusterIdFromName(info.getWorking());
            } else if (IndexType.LIVE.is(type)) {
                return indexAPI.removeClusterIdFromName(info.getLive());
            }
        }

        return null;
    }

}
