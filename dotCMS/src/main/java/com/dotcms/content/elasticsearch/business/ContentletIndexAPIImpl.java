package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.isMigrationComplete;
import static com.dotcms.content.index.IndexConfigHelper.isMigrationNotStarted;
import static com.dotcms.content.index.IndexConfigHelper.isMigrationStarted;
import static com.dotcms.content.index.IndexConfigHelper.isReadEnabled;
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
import com.dotcms.content.business.ContentIndexMappingAPI;
import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.util.ESMappingUtilHelper;
import com.dotcms.content.index.ContentletIndexOperations;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.PhaseRouter;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.VersionedIndicesAPI;
import com.dotcms.content.index.VersionedIndicesImpl;
import com.dotcms.content.index.domain.IndexBulkListener;
import com.dotcms.content.index.domain.IndexBulkProcessor;
import com.dotcms.content.index.domain.IndexBulkRequest;
import com.dotcms.content.index.opensearch.ContentletIndexOperationsOS;
import com.dotcms.content.model.annotation.IndexLibraryIndependent;
import com.dotcms.content.model.annotation.IndexRouter;
import com.dotcms.content.model.annotation.IndexRouter.IndexAccess;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
 *       The processor is <em>always</em> owned by the current <em>read</em> provider.
 *       Full dual-write on the async path is intentionally deferred.</li>
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
    private final ReindexQueueAPI queueApi;
    private final IndexAPI indexAPI;
    private final IndiciesAPI legacyIndiciesAPI;
    private final VersionedIndicesAPI versionedIndicesAPI;
    private final AtomicReference<ContentIndexMappingAPI> mappingAPI = new AtomicReference<>();

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

    /** Package-private constructor for testing. */
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
     * Lazy initializer avoids circular reference Stackoverflow error.
     * Thread-safe: uses {@link AtomicReference#updateAndGet} to ensure
     * exactly one instance is published without synchronization overhead.
     *
     * @return ContentIndexMappingAPI
     */
    private ContentIndexMappingAPI getMappingAPI() {
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
                null,   // OS reindex-working not yet implemented
                null);  // OS reindex-live not yet implemented
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
        return Try.of(() -> loadProviderIndices(ops)).getOrElse((ProviderIndices) null);
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

        @Override
        public int size() {
            return esReq.size() + osReq.size();
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
        if(isMigrationNotStarted()) {
            return indexReadyES();
        }
        return indexReadyES() && indexReadyOS();
    }

    private boolean indexReadyES() throws DotDataException {
        final IndiciesInfo info = legacyIndiciesAPI.loadIndicies();

        final boolean hasWorking  = Try.of(()-> indexAPI.indexExists(info.getWorking()))
                .getOrElse(false);
        final  boolean hasLive  = Try.of(()-> indexAPI.indexExists(info.getLive()))
                .getOrElse(false);

        if(!hasWorking){
            Logger.debug(this.getClass(), "-- WORKING INDEX DOES NOT EXIST");
        }
        if(!hasLive){
            Logger.debug(this.getClass(), "-- LIVE INDEX DOES NOT EXIST");
        }
        return hasWorking && hasLive;
    }

    private boolean indexReadyOS() {
        final Optional<VersionedIndices> indicesOpt;
        indicesOpt = Try.of(versionedIndicesAPI::loadDefaultVersionedIndices)
                .getOrElse(Optional.empty());

        if (indicesOpt.isEmpty()) {
            Logger.debug(this.getClass(), "-- OS: NO VERSIONED INDICES RECORD FOUND");
            return false;
        }

        final VersionedIndices indices = indicesOpt.get();
        final boolean hasWorking = indices.working()
                .map(name -> Try.of(() -> indexAPI.indexExists(name)).getOrElse(false))
                .orElse(false);
        final boolean hasLive = indices.live()
                .map(name -> Try.of(() -> indexAPI.indexExists(name)).getOrElse(false))
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

        }
    }

    private void reindexIfNoIndicesFound() throws DotDataException {

        // if there are indexes, but they are empty, start reindex process
        if(hasEmptyIndices()) {
            DotConcurrentFactory.getInstance().getSubmitter().submit(() -> {
                try {
                    Logger.info(this.getClass(),
                            "No content found in index, starting reindex process in background thread.");
                    APILocator.getReindexQueueAPI().deleteFailedRecords();
                    APILocator.getReindexQueueAPI().addAllToReindexQueue();

                } catch (Throwable e) { // nosonar

                    Logger.error(this.getClass(), "Error starting reindex process", e);
                }
            });
        }
    }

    private boolean hasEmptyIndices() throws DotDataException {
        return isEsWorkingIndexEmpty() || isOsWorkingIndexEmpty();
    }

    private boolean isEsWorkingIndexEmpty() throws DotDataException {
        final String workingIndex = legacyIndiciesAPI.loadIndicies().getWorking();
        return getIndexDocumentCount(workingIndex) == 0;
    }

    private boolean isOsWorkingIndexEmpty() throws DotDataException {
        if (isMigrationNotStarted()) {
            return false;
        }
        return versionedIndicesAPI
                .loadDefaultVersionedIndices()
                .flatMap(VersionedIndices::working)
                .map(workingIndex -> getIndexDocumentCount(workingIndex) == 0)
                .orElse(false);
    }

    public synchronized boolean createContentIndex(String indexName)
            throws IOException {
        boolean result = createContentIndex(indexName, 0);
        ESMappingUtilHelper.getInstance().addCustomMapping(indexName);

        return result;
    }

    @Override
    public synchronized boolean createContentIndex(final String indexName, final int shards)
            throws IOException {
        // Each provider loads its own settings file and applies its own mapping,
        // so the router can fan out uniformly without knowing which backend is active.
        boolean result = true;
        for (final ContentletIndexOperations ops : router.writeProviders()) {
            result &= ops.createContentIndex(indexName, shards);
        }
        return result;
    }


    /**
     * Creates new indexes /working_TIMESTAMP (aliases working_read, working_write and workinglive)
     * and /live_TIMESTAMP with (aliases live_read, live_write, workinglive)
     *
     * @return the timestamp string used as suffix for indices
     * @throws DotDataException
     */
    private synchronized String initIndex() throws DotDataException {
        if (indexReady()) {
            return "";
        }

        if(isReadEnabled() || isMigrationComplete()) {
           return initIndexOS();
        }

        if(isMigrationStarted()){
            initIndexOS();
        }

        return initIndexES();
    }

    /**
     * ES index Initializer
     * @return
     */
    private String initIndexES() {
        try {

            final IndiciesInfo.Builder builder = new IndiciesInfo.Builder();
            final IndiciesInfo oldInfo = legacyIndiciesAPI.loadIndicies();

            if (oldInfo != null && oldInfo.getSiteSearch() != null) {
                builder.setSiteSearch(oldInfo.getSiteSearch());
            }

            final IndiciesInfo info = builder.build();
            final String timeStamp = info.createNewIndiciesName(IndexType.WORKING, IndexType.LIVE);

            createContentIndex(info.getWorking(), 0);
            createContentIndex(info.getLive(), 0);

            legacyIndiciesAPI.point(info);

            ESMappingUtilHelper.getInstance()
                    .addCustomMapping(info.getWorking(), info.getLive());
            return timeStamp;
        } catch (Exception e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * OS index initializer
     * @return
     * @throws DotDataException
     */
    private String initIndexOS() throws DotDataException {
        final String timeStamp = ContentletIndexAPI.timestampFormatter.format(LocalDateTime.now());

        final String workingName = IndexType.WORKING.getPrefix() + "_" + timeStamp;
        final String liveName    = IndexType.LIVE.getPrefix()    + "_" + timeStamp;

        try {
            indexAPI.createIndex(workingName, 0);
            indexAPI.createIndex(liveName, 0);
        } catch (final Exception e) {
            throw new DotDataException("Error creating OS indices: " + e.getMessage(), e);
        }

        final VersionedIndices osInfo = VersionedIndicesImpl.builder()
                .working(workingName)
                .live(liveName)
                .build();
        versionedIndicesAPI.saveIndices(osInfo);

        ESMappingUtilHelper.getInstance().addCustomMapping(workingName, liveName);
        return timeStamp;
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

        // We double check again. Only one node will enter this critical
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
     * creates new working and live indexes with reading aliases pointing to old index and write
     * aliases pointing to both old and new indexes
     *
     * @return the timestamp string used as suffix for indices
     * @throws DotDataException
     */
    @WrapInTransaction
    public synchronized String fullReindexStart() throws DotDataException {
        if (indexReady() && !isInFullReindex()) {
            try {

                final IndiciesInfo.Builder builder = new IndiciesInfo.Builder();
                final IndiciesInfo oldInfo = legacyIndiciesAPI.loadIndicies();

                builder.setWorking(oldInfo.getWorking());
                builder.setLive(oldInfo.getLive());
                builder.setSiteSearch(oldInfo.getSiteSearch());


                final User currentUser = Try.of(() -> PortalUtil.getUser(HttpServletRequestThreadLocal.INSTANCE.getRequest()))
                        .getOrNull();
                if (currentUser != null) {
                    Logger.info(this, "Full reindex started by user: " + currentUser.getUserId() + " (" + currentUser.getEmailAddress() + ") at " + new java.util.Date());
                } else {
                    Logger.info(this, "Full reindex started by system user at " + new java.util.Date());
                }

                final IndiciesInfo info = builder.build();
                final String timeStamp = info.createNewIndiciesName(IndexType.REINDEX_WORKING,
                        IndexType.REINDEX_LIVE);

                createContentIndex(info.getReindexWorking(), 0);
                createContentIndex(info.getReindexLive(), 0);

                legacyIndiciesAPI.point(info);

                ESMappingUtilHelper.getInstance()
                        .addCustomMapping(info.getReindexWorking(), info.getReindexLive());

                return timeStamp;
            } catch (Exception e) {
                throw new DotRuntimeException(e.getMessage(), e);
            }
        } else {
            return initIndex();
        }
    }

    @CloseDBIfOpened
    public boolean isInFullReindex() throws DotDataException {
        IndiciesInfo info = legacyIndiciesAPI.loadIndicies();
        return queueApi.hasReindexRecords() || (info.getReindexWorking() != null
                && info.getReindexLive() != null);

    }

    @CloseDBIfOpened
    public boolean fullReindexSwitchover(final boolean forceSwitch) {
        return fullReindexSwitchover(DbConnectionFactory.getConnection(), forceSwitch);
    }

    /**
     * This will drop old index and will point read aliases to new index. This method should be
     * called after call to {@link #fullReindexStart()}
     *
     * @return
     */
    @CloseDBIfOpened
    public boolean fullReindexSwitchover(Connection conn, final boolean forceSwitch) {

        if (reindexTimeElapsedInLong()
                < Config.getLongProperty("REINDEX_THREAD_MINIMUM_RUNTIME_IN_SEC", 30) * 1000) {
            if (reindexTimeElapsed().isPresent()) {
                Logger.info(this.getClass(),
                        "Reindex has been running only " + (reindexTimeElapsed().isPresent() ? reindexTimeElapsed().get() : "n/a")
                                + ". Letting the reindex settle.");
            } else {
                Logger.info(this.getClass(), "Reindex Time Elapsed not set.");
            }
            ThreadUtils.sleep(3000);
            return false;
        }
        try {
            final IndiciesInfo oldInfo = legacyIndiciesAPI.loadIndicies();
            final String luckyServer = Try.of(() -> APILocator.getServerAPI().getOldestServer())
                    .getOrElse(ConfigUtils.getServerId());
            if (!forceSwitch) {
                if (!isInFullReindex()) {
                    return false;
                }
                if (!luckyServer.equals(ConfigUtils.getServerId())) {
                    logSwitchover(oldInfo, luckyServer);
                    DateUtil.sleep(5000);
                    CacheLocator.getIndiciesCache().clearCache();
                    return false;
                }
            }

            final IndiciesInfo.Builder builder = new IndiciesInfo.Builder();

            builder.setLive(oldInfo.getReindexLive());
            builder.setWorking(oldInfo.getReindexWorking());
            builder.setSiteSearch(oldInfo.getSiteSearch());

            final IndiciesInfo newInfo = builder.build();

            logSwitchover(oldInfo, luckyServer);
            legacyIndiciesAPI.point(newInfo);

            DotConcurrentFactory.getInstance().getSubmitter().submit(() -> {
                try {
                    Logger.info(this.getClass(), "Updating and optimizing ElasticSearch Indexes");
                    optimize(List.of(newInfo.getWorking(), newInfo.getLive()));
                } catch (Exception e) {
                    Logger.warnAndDebug(this.getClass(),
                            "unable to expand ES replicas:" + e.getMessage(), e);
                }
            });

            long failedRecords = queueApi.getFailedReindexRecords().size();
            if (failedRecords > 0) {
                final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();

                final String message = LanguageUtil.get(
                                APILocator.getCompanyAPI().getDefaultCompany(),
                                "Contents-Failed-Reindex-message")
                        .replace("{0}", String.valueOf(failedRecords));

                SystemMessage systemMessage = systemMessageBuilder.setMessage(message)
                        .setType(MessageType.SIMPLE_MESSAGE)
                        .setSeverity(MessageSeverity.WARNING)
                        .setLife(3600000)
                        .create();
                List<String> users = APILocator.getRoleAPI()
                        .findUserIdsForRole(APILocator.getRoleAPI().loadCMSAdminRole());
                SystemMessageEventUtil.getInstance().pushMessage(systemMessage, users);
            }


        } catch (Exception e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
        return true;
    }


    private long reindexTimeElapsedInLong() {
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

    public boolean delete(String indexName) {
        return indexAPI.delete(indexName);
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
    } // indexContentListNow.

    private void indexContentListWaitFor(final List<Contentlet> contentToIndex) {
        final IndexBulkRequest bulkRequest = createBulkRequest(contentToIndex);
        this.setRefreshPolicy(bulkRequest, IndexBulkRequest.RefreshPolicy.WAIT_FOR);
        putToIndex(bulkRequest);
        CacheLocator.getESQueryCache().clearCache();
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
            // Dual-write phase: submit each provider's sub-batch independently
            final DualIndexBulkRequest dual = (DualIndexBulkRequest) bulkRequest;
            operationsES.putToIndex(dual.esReq);
            operationsOS.putToIndex(dual.osReq);
        } else {
            // Single-provider phase: forward to the sole active provider
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
     * <p><strong>Routing:</strong> the processor is always bound to the current
     * <em>read provider</em>, not the write providers. Rationale: the async processor
     * owns a long-lived connection and background flushing thread that cannot be
     * trivially duplicated per provider. Full dual-write on the async path is
     * tracked separately. The synchronous bulk-request path ({@link #createBulkRequest()})
     * is the correct choice when dual-write fidelity is required.</p>
     */
    @Override
    public IndexBulkProcessor createBulkProcessor(final IndexBulkListener bulkListener) {
        // Always bound to the read provider — dual-write via processor not yet supported
        return router.readProvider().createBulkProcessor(bulkListener);
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
     * Generates a bulk request that adds the specified {@link ReindexEntry} to the index.
     *
     * @param req The {@link IndexBulkRequest} to append operations to.
     * @param idx  The entry containing the information of the Contentlet that will be indexed.
     * @throws DotDataException An error occurred when processing this request.
     */
    @CloseDBIfOpened
    private void appendBulkRequestInternal(final IndexBulkRequest req, final ReindexEntry idx)
            throws DotDataException {
        final List<ContentletVersionInfo> versions = APILocator.getVersionableAPI()
                .findContentletVersionInfos(idx.getIdentToIndex());
        final Map<String, Contentlet> inodes = new HashMap<>();
        try {
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
                // If there is no content for this entry, it should be deleted to avoid future attempts that will fail also
                APILocator.getReindexQueueAPI().deleteReindexEntry(idx);
                Logger.debug(this, String.format(
                        "Unable to find versions for content id: '%s'. Deleting content " +
                                "reindex entry.", idx.getIdentToIndex()));
            }
            for (final Contentlet contentlet : inodes.values()) {
                Logger.debug(this,
                        String.format("Indexing id: '%s', priority: '%s'", contentlet.getInode(),
                                idx.getPriority()));
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
        final List<ContentletVersionInfo> versions = APILocator.getVersionableAPI()
                .findContentletVersionInfos(idx.getIdentToIndex());
        final Map<String, Contentlet> inodes = new HashMap<>();
        try {
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
                APILocator.getReindexQueueAPI().deleteReindexEntry(idx);
                Logger.debug(this, String.format(
                        "Unable to find versions for content id: '%s'. Deleting content " +
                                "reindex entry.", idx.getIdentToIndex()));
            }
            for (final Contentlet contentlet : inodes.values()) {
                Logger.debug(this,
                        String.format("Indexing id: '%s', priority: '%s'", contentlet.getInode(),
                                idx.getPriority()));
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
                // OS record not yet initialised — skip silently (logged inside loadProviderIndices)
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
                "Indexing " + contentToIndex.size() + " contents via processor, starting with identifier [ "
                        + contentToIndex.get(0).getIdentifier() + "]");

        final ContentletIndexOperations ops = router.readProvider();
        final ProviderIndices indices = loadProviderIndicesQuietly(ops);
        if (indices == null) {
            Logger.warn(this, "No index info for read provider — skipping processor indexing");
            return;
        }

        final Set<Contentlet> deduped = new HashSet<>(contentToIndex);
        for (final Contentlet contentlet : deduped) {
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
                        ops.addIndexOpToProcessor(proc, indices.working, id, mapping);
                    }
                    if (indices.reindexWorking != null) {
                        ops.addIndexOpToProcessor(proc, indices.reindexWorking, id, mapping);
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
                        ops.addIndexOpToProcessor(proc, indices.live, id, mapping);
                    }
                    if (indices.reindexLive != null) {
                        ops.addIndexOpToProcessor(proc, indices.reindexLive, id, mapping);
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

    private boolean isWorking(final Contentlet contentlet) {

        boolean isWorking = false;

        try {
            isWorking = contentlet.isWorking();
        } catch (Exception e) {
            Logger.debug(this, e.getMessage(), e);
            Logger.warn(this, e.getMessage(), e);
            isWorking = false;
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
        depsIdentifiers.stream().forEach(dotConnect::addParam);

        final List<Map<String, String>> versionInfoMapResults =
                Sneaky.sneak(() -> dotConnect.loadResults());

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
        final DualIndexBulkRequest dualReq =
                req instanceof DualIndexBulkRequest ? (DualIndexBulkRequest) req : null;

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
        final List<Variant> variants = APILocator.getVariantAPI().getVariants();
        // Processor path belongs to the read provider — same limitation as addBulkRequestToProcessor
        final ContentletIndexOperations ops = router.readProvider();
        final ProviderIndices indices = loadProviderIndicesQuietly(ops);
        if (indices == null) {
            return;
        }
        for (final Language language : languages) {
            for (final String index : indices.activeIndices()) {
                for (final Variant variant : variants) {
                    final String id = entry.getIdentToIndex()
                            + StringPool.UNDERLINE + language.getId()
                            + StringPool.UNDERLINE + variant.name();
                    Logger.debug(this.getClass(), "deleting:" + id);
                    ops.addDeleteOpToProcessor(proc, index, id);
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

        final DualIndexBulkRequest dualReq =
                bulkRequest instanceof DualIndexBulkRequest ? (DualIndexBulkRequest) bulkRequest : null;

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
    }

    public void fullReindexAbort() {
        try {
            if (!isInFullReindex()) {
                return;
            }

            IndiciesInfo info = legacyIndiciesAPI.loadIndicies();

            final IndiciesInfo.Builder builder = new IndiciesInfo.Builder();
            builder.setWorking(info.getWorking());
            builder.setLive(info.getLive());
            builder.setSiteSearch(info.getSiteSearch());

            IndiciesInfo newinfo = builder.build();

            info.getReindexWorking();
            info.getReindexLive();

            legacyIndiciesAPI.point(newinfo);
        } catch (Exception e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    public boolean isDotCMSIndexName(final String indexName) {
        return IndexType.WORKING.is(indexName) || IndexType.LIVE.is(indexName);
    }

    public List<String> listDotCMSClosedIndices() {
        return indexAPI.getClosedIndexes();
    }

    /**
     * Returns a list of dotcms working and live indices.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<String> listDotCMSIndices() {

        return indexAPI.getIndices(true, false);
    }


    public void activateIndex(final String indexName) throws DotDataException {
        final IndiciesInfo info = legacyIndiciesAPI.loadIndicies();
        final IndiciesInfo.Builder builder = IndiciesInfo.Builder.copy(info);
        if (indexName == null) {
            throw new DotRuntimeException("Index cannot be null");
        }
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

        final User currentUser = Try.of(() -> PortalUtil.getUser(HttpServletRequestThreadLocal.INSTANCE.getRequest()))
                .getOrNull();
        if (currentUser != null) {
            Logger.info(this, "Index activation (" + indexName + ") performed by user: " + currentUser.getUserId() + " (" + currentUser.getEmailAddress() + ") at " + new java.util.Date());
        } else {
            Logger.info(this, "Index activation (" + indexName + ") performed by system user at " + new java.util.Date());
        }

        legacyIndiciesAPI.point(builder.build());
    }

    public void deactivateIndex(String indexName) throws DotDataException, IOException {
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

    public synchronized List<String> getCurrentIndex() throws DotDataException {
        final List<String> newIdx = new ArrayList<>();
        final IndiciesInfo info = legacyIndiciesAPI.loadIndicies();
        newIdx.add(indexAPI.removeClusterIdFromName(info.getWorking()));
        newIdx.add(indexAPI.removeClusterIdFromName(info.getLive()));
        return newIdx;
    }

    public synchronized List<String> getNewIndex() throws DotDataException {
        final List<String> newIdx = new ArrayList<>();
        final IndiciesInfo info = legacyIndiciesAPI.loadIndicies();

        if (info.getReindexWorking() != null) {
            newIdx.add(indexAPI.removeClusterIdFromName(info.getReindexWorking()));
        }
        if (info.getReindexLive() != null) {
            newIdx.add(indexAPI.removeClusterIdFromName(info.getReindexLive()));
        }
        return newIdx;
    }

    public String getActiveIndexName(final String type) throws DotDataException {
        final IndiciesInfo info = legacyIndiciesAPI.loadIndicies();

        if (IndexType.WORKING.is(type)) {
            return indexAPI.removeClusterIdFromName(info.getWorking());
        } else if (IndexType.LIVE.is(type)) {
            return indexAPI.removeClusterIdFromName(info.getLive());
        }

        return null;
    }

}