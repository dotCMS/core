package com.dotcms.content.elasticsearch.business;

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
import com.dotcms.content.index.domain.CreateIndexStatus;
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
import com.dotcms.util.JsonUtil;
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
import org.elasticsearch.ElasticsearchException;

@IndexLibraryIndependent
@IndexRouter(access = IndexAccess.READ_WRITE)
public class ContentletIndexAPIImpl implements ContentletIndexAPI {

    private static final String SELECT_CONTENTLET_VERSION_INFO =
            "select working_inode,live_inode from contentlet_version_info where identifier IN (%s)";
    private ReindexQueueAPI queueApi = null;
    private IndexAPI esIndexApi = null;
    private final AtomicReference<ContentIndexMappingAPI> mappingAPI = new AtomicReference<>();

    private final ContentletIndexOperations operationsES;
    private final ContentletIndexOperations operationsOS;

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
        queueApi = APILocator.getReindexQueueAPI();
        esIndexApi = APILocator.getESIndexAPI();
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

    public ContentletIndexOperations operationsES() {
        return operationsES;
    }

    public ContentletIndexOperations operationsOS() {
        return operationsOS;
    }

    /**
     * Migration-phase-aware operations delegate.
     * Routes to OpenSearch when migration is complete or read is enabled,
     * otherwise routes to Elasticsearch.
     */
    ContentletIndexOperations getProvider() {
        //TODO: For now always default to ES
        return operationsES;
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
        return indexReadyES();
    }

    private boolean indexReadyES() throws DotDataException {
        final IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();

        final boolean hasWorking  = Try.of(()->APILocator.getESIndexAPI().indexExists(info.getWorking()))
                .getOrElse(false);
        final  boolean hasLive  = Try.of(()->APILocator.getESIndexAPI().indexExists(info.getLive()))
                .getOrElse(false);

        if(!hasWorking){
            Logger.debug(this.getClass(), "-- WORKING INDEX DOES NOT EXIST");
        }
        if(!hasLive){
            Logger.debug(this.getClass(), "-- LIVE INDEX DOES NOT EXIST");
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


            // if there are indexes but they are empty, start reindex process
            if(Config.getBooleanProperty("REINDEX_IF_NO_INDEXES_FOUND", true)
                    && getIndexDocumentCount(APILocator.getIndiciesAPI().loadIndicies().getWorking())==0
            ){
                DotConcurrentFactory.getInstance().getSubmitter().submit(()->{
                    try {
                        Logger.info(this.getClass(), "No content found in index, starting reindex process in background thread.");
                        APILocator.getReindexQueueAPI().deleteFailedRecords();
                        APILocator.getReindexQueueAPI().addAllToReindexQueue();

                    } catch (Throwable e) { // nosonar

                        Logger.error(this.getClass(), "Error starting reindex process", e);
                    }
                });

            }


        } catch (Exception e) {
            Logger.fatal(this.getClass(), "Failed to create new indexes:" + e.getMessage(),e);

        }
    }

    public synchronized boolean createContentIndex(String indexName)
            throws ElasticsearchException, IOException {
        boolean result = createContentIndex(indexName, 0);
        ESMappingUtilHelper.getInstance().addCustomMapping(indexName);

        return result;
    }

    @Override
    public synchronized boolean createContentIndex(String indexName, int shards)
            throws ElasticsearchException, IOException {
        String settings = null;

        try {
            settings = JsonUtil.getJsonFileContentAsString("es-content-settings.json");
        } catch (Exception e) {
            Logger.error(this.getClass(), "cannot load es-content-settings.json file, skipping", e);
        }

        final String mapping = JsonUtil.getJsonFileContentAsString("es-content-mapping.json");
        CreateIndexStatus cir = esIndexApi.createIndex(indexName, settings, shards);

        int i = 0;
        while (!cir.acknowledged()) {
            DateUtil.sleep(100);

            if (i++ > 300) {
                throw new ElasticsearchException("index timed out creating");
            }
        }

        getMappingAPI().putMapping(indexName, mapping);

        return true;
    }


    /**
     * Creates new indexes /working_TIMESTAMP (aliases working_read, working_write and workinglive)
     * and /live_TIMESTAMP with (aliases live_read, live_write, workinglive)
     *
     * @return the timestamp string used as suffix for indices
     * @throws ElasticsearchException if Murphy comes around
     * @throws DotDataException
     */
    private synchronized String initIndex() throws ElasticsearchException, DotDataException {
        if (indexReady()) {
            return "";
        }
        try {

            final IndiciesInfo.Builder builder = new IndiciesInfo.Builder();
            final IndiciesInfo oldInfo = APILocator.getIndiciesAPI().loadIndicies();

            if (oldInfo != null && oldInfo.getSiteSearch() != null) {
                builder.setSiteSearch(oldInfo.getSiteSearch());
            }

            final IndiciesInfo info = builder.build();
            final String timeStamp = info.createNewIndiciesName(IndexType.WORKING, IndexType.LIVE);

            createContentIndex(info.getWorking(), 0);
            createContentIndex(info.getLive(), 0);

            APILocator.getIndiciesAPI().point(info);

            ESMappingUtilHelper.getInstance()
                    .addCustomMapping(info.getWorking(), info.getLive());
            return timeStamp;
        } catch (Exception e) {
            throw new ElasticsearchException(e.getMessage(), e);
        }

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
     * @throws SQLException         An error occurred when interacting with the database.
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
     * @throws SQLException         An error occurred when interacting with the database.
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
     * @throws ElasticsearchException
     */
    @WrapInTransaction
    public synchronized String fullReindexStart() throws ElasticsearchException, DotDataException {
        if (indexReady() && !isInFullReindex()) {
            try {

                final IndiciesInfo.Builder builder = new IndiciesInfo.Builder();
                final IndiciesInfo oldInfo = APILocator.getIndiciesAPI().loadIndicies();

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

                APILocator.getIndiciesAPI().point(info);

                ESMappingUtilHelper.getInstance()
                        .addCustomMapping(info.getReindexWorking(), info.getReindexLive());

                return timeStamp;
            } catch (Exception e) {
                throw new ElasticsearchException(e.getMessage(), e);
            }
        } else {
            return initIndex();
        }
    }

    @CloseDBIfOpened
    public boolean isInFullReindex() throws DotDataException {
        IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
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
            final IndiciesInfo oldInfo = APILocator.getIndiciesAPI().loadIndicies();
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
            APILocator.getIndiciesAPI().point(newInfo);

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
            final IndiciesInfo oldInfo = APILocator.getIndiciesAPI().loadIndicies();
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

        Logger.info(this, "Old indicies        : [" + esIndexApi
                .removeClusterIdFromName(oldInfo.getWorking()) + "," + esIndexApi
                .removeClusterIdFromName(oldInfo.getLive()) + "]");
        Logger.info(this, "New indicies        : [" + esIndexApi
                .removeClusterIdFromName(oldInfo.getReindexWorking()) + "," + esIndexApi
                .removeClusterIdFromName(oldInfo.getReindexLive()) + "]");
        Logger.info(this, "-------------------------------");

    }

    public boolean delete(String indexName) {
        return esIndexApi.delete(indexName);
    }

    public boolean optimize(List<String> indexNames) {
        return esIndexApi.optimize(indexNames);
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
        getProvider().setRefreshPolicy(bulkRequest, IndexBulkRequest.RefreshPolicy.IMMEDIATE);
        putToIndex(bulkRequest);
        CacheLocator.getESQueryCache().clearCache();
    } // indexContentListNow.

    private void indexContentListWaitFor(final List<Contentlet> contentToIndex) {
        final IndexBulkRequest bulkRequest = createBulkRequest(contentToIndex);
        getProvider().setRefreshPolicy(bulkRequest, IndexBulkRequest.RefreshPolicy.WAIT_FOR);
        putToIndex(bulkRequest);
        CacheLocator.getESQueryCache().clearCache();
    } // indexContentListWaitFor.

    private void indexContentListDefer(final List<Contentlet> contentToIndex) {
        final IndexBulkRequest bulkRequest = createBulkRequest(contentToIndex);
        putToIndex(bulkRequest);
    } // indexContentListDefer.

    @Override
    public void setRefreshPolicy(final IndexBulkRequest bulkRequest,
            final IndexBulkRequest.RefreshPolicy policy) {
        getProvider().setRefreshPolicy(bulkRequest, policy);
    }

    @Override
    public void putToIndex(final IndexBulkRequest bulkRequest) {
        getProvider().putToIndex(bulkRequest);
    }

    @Override
    public IndexBulkRequest createBulkRequest(final List<Contentlet> contentToIndex) {
        final IndexBulkRequest req = createBulkRequest();
        this.appendBulkRequestFromContentlets(req, contentToIndex);
        return req;
    }

    @Override
    public IndexBulkRequest createBulkRequest() {
        return getProvider().createBulkRequest();
    }

    @Override
    public IndexBulkProcessor createBulkProcessor(final IndexBulkListener bulkListener) {
        return getProvider().createBulkProcessor(bulkListener);
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

    private void addBulkRequest(final IndexBulkRequest req, final List<Contentlet> contentToIndex,
            final boolean forReindex) {
        if (contentToIndex != null && !contentToIndex.isEmpty()) {
            Logger.debug(this.getClass(),
                    "Indexing " + contentToIndex.size() + " contents, starting with identifier [ "
                            + contentToIndex.get(0).getIdentifier() + "]");
        }

        // eliminate dups
        final Set<Contentlet> contentToIndexSet = new HashSet<>(contentToIndex);

        for (final Contentlet contentlet : contentToIndexSet) {

            final String id = contentlet.getIdentifier() + "_" + contentlet.getLanguageId()
                    + "_" + contentlet.getVariantId();
            Logger.debug(this,
                    () -> "\n*********----------- Indexing : " + Thread.currentThread().getName()
                            + ", id: "
                            + contentlet.getIdentifier() + ", identityHashCode: " + System
                            .identityHashCode(contentlet));
            Logger.debug(this,
                    () -> "*********-----------  " + DbConnectionFactory.getConnection());
            Logger.debug(this, () -> "*********-----------  "
                    + ExceptionUtil
                    .getCurrentStackTraceAsString(Config.getIntProperty("stacktracelimit", 10))
                    + "\n");

            final IndiciesInfo info = Sneaky
                    .sneak(() -> APILocator.getIndiciesAPI().loadIndicies());
            String mapping = null;

            try {

                if (this.isWorking(contentlet)) {

                    mapping = Try.of(
                                    () -> objectMapper.writeValueAsString(getMappingAPI().toMap(contentlet)))
                            .getOrElseThrow(
                                    DotRuntimeException::new);
                    if (!forReindex || info.getReindexWorking() == null) {
                        getProvider().addIndexOp(req, info.getWorking(), id, mapping);
                    }
                    if (info.getReindexWorking() != null) {
                        getProvider().addIndexOp(req, info.getReindexWorking(), id, mapping);
                    }
                }

                if (this.isLive(contentlet)) {
                    if (mapping == null) {
                        mapping = Try.of(
                                        () -> objectMapper.writeValueAsString(getMappingAPI().toMap(contentlet)))
                                .getOrElseThrow(
                                        DotRuntimeException::new);
                    }
                    if (!forReindex || info.getReindexLive() == null) {
                        getProvider().addIndexOp(req, info.getLive(), id, mapping);
                    }
                    if (info.getReindexLive() != null) {
                        getProvider().addIndexOp(req, info.getReindexLive(), id, mapping);
                    }
                }

                contentlet.markAsReindexed();
            } catch (Exception ex) {
                Logger.error(this,
                        "Can't get a mapping for contentlet with id_lang:" + id + " Content data: "
                                + contentlet.getMap(), ex);
                throw ex;
            }
        }
    }

    private void addBulkRequestToProcessor(final IndexBulkProcessor proc,
            final List<Contentlet> contentToIndex, final boolean forReindex) {
        if (contentToIndex != null && !contentToIndex.isEmpty()) {
            Logger.debug(this.getClass(),
                    "Indexing " + contentToIndex.size() + " contents via processor, starting with identifier [ "
                            + contentToIndex.get(0).getIdentifier() + "]");
        }

        final Set<Contentlet> contentToIndexSet = new HashSet<>(contentToIndex);

        for (final Contentlet contentlet : contentToIndexSet) {

            final String id = contentlet.getIdentifier() + "_" + contentlet.getLanguageId()
                    + "_" + contentlet.getVariantId();

            final IndiciesInfo info = Sneaky
                    .sneak(() -> APILocator.getIndiciesAPI().loadIndicies());
            String mapping = null;

            try {

                if (this.isWorking(contentlet)) {

                    mapping = Try.of(
                                    () -> objectMapper.writeValueAsString(getMappingAPI().toMap(contentlet)))
                            .getOrElseThrow(
                                    DotRuntimeException::new);
                    if (!forReindex || info.getReindexWorking() == null) {
                        getProvider().addIndexOpToProcessor(proc, info.getWorking(), id, mapping);
                    }
                    if (info.getReindexWorking() != null) {
                        getProvider().addIndexOpToProcessor(proc, info.getReindexWorking(), id, mapping);
                    }
                }

                if (this.isLive(contentlet)) {
                    if (mapping == null) {
                        mapping = Try.of(
                                        () -> objectMapper.writeValueAsString(getMappingAPI().toMap(contentlet)))
                                .getOrElseThrow(
                                        DotRuntimeException::new);
                    }
                    if (!forReindex || info.getReindexLive() == null) {
                        getProvider().addIndexOpToProcessor(proc, info.getLive(), id, mapping);
                    }
                    if (info.getReindexLive() != null) {
                        getProvider().addIndexOpToProcessor(proc, info.getReindexLive(), id, mapping);
                    }
                }

                contentlet.markAsReindexed();
            } catch (Exception ex) {
                Logger.error(this,
                        "Can't get a mapping for contentlet with id_lang:" + id + " Content data: "
                                + contentlet.getMap(), ex);
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

    private void appendBulkRemoveRequestInternal(final IndexBulkRequest req,
            final ReindexEntry entry) throws DotDataException {
        final List<Language> languages = APILocator.getLanguageAPI().getLanguages();
        final List<Variant> variants = APILocator.getVariantAPI().getVariants();
        final IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();

        // delete for every language and in every index
        for (Language language : languages) {
            for (final String index : info.asMap().values()) {
                for (final Variant variant : variants) {
                    final String id =
                            entry.getIdentToIndex() + StringPool.UNDERLINE + language.getId()
                                    + StringPool.UNDERLINE + variant.name();

                    Logger.debug(this.getClass(),"deleting:" + id);
                    getProvider().addDeleteOp(req, index, id);
                }
            }
        }
    }

    private void appendBulkRemoveRequestToProcessor(final IndexBulkProcessor proc,
            final ReindexEntry entry) throws DotDataException {
        final List<Language> languages = APILocator.getLanguageAPI().getLanguages();
        final List<Variant> variants = APILocator.getVariantAPI().getVariants();
        final IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();

        // delete for every language and in every index
        for (Language language : languages) {
            for (final String index : info.asMap().values()) {
                for (final Variant variant : variants) {
                    final String id =
                            entry.getIdentToIndex() + StringPool.UNDERLINE + language.getId()
                                    + StringPool.UNDERLINE + variant.name();

                    Logger.debug(this.getClass(),"deleting:" + id);
                    getProvider().addDeleteOpToProcessor(proc, index, id);
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
        final IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
        final IndexBulkRequest bulkRequest = getProvider().createBulkRequest();

        // we want to wait until the content is already indexed
        switch (indexPolicy) {
            case FORCE:
                getProvider().setRefreshPolicy(bulkRequest, IndexBulkRequest.RefreshPolicy.IMMEDIATE);
                break;

            case WAIT_FOR:
                getProvider().setRefreshPolicy(bulkRequest, IndexBulkRequest.RefreshPolicy.WAIT_FOR);
                break;
        }

        getProvider().addDeleteOp(bulkRequest, info.getLive(), id);

        if (info.getReindexLive() != null) {
            getProvider().addDeleteOp(bulkRequest, info.getReindexLive(), id);
        }

        if (!onlyLive) {

            // here we search for relationship fields pointing to this
            // content to be deleted. Those contentlets are reindexed
            // to avoid left those fields making noise in the index
            if (UtilMethods.isSet(relationships)) {
                reindexDependenciesForDeletedContent(contentlet, relationships,
                        indexPolicyDependencies);
            }

            getProvider().addDeleteOp(bulkRequest, info.getWorking(), id);
            if (info.getReindexWorking() != null) {
                getProvider().addDeleteOp(bulkRequest, info.getReindexWorking(), id);
            }
        }

        getProvider().putToIndex(bulkRequest);

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
     * Removes all content from the index for the given content type
     * this one does NOT go to the db therefore it does NOT need the DB closed annotation
     * @param contentType
     * @throws DotDataException
     */
    @Override
    public void removeContentFromIndexByContentType(final ContentType contentType)
            throws DotDataException {
        getProvider().removeContentFromIndexByContentType(contentType);
        CacheLocator.getESQueryCache().clearCache();
    }

    public void fullReindexAbort() {
        try {
            if (!isInFullReindex()) {
                return;
            }

            IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();

            final IndiciesInfo.Builder builder = new IndiciesInfo.Builder();
            builder.setWorking(info.getWorking());
            builder.setLive(info.getLive());
            builder.setSiteSearch(info.getSiteSearch());

            IndiciesInfo newinfo = builder.build();

            info.getReindexWorking();
            info.getReindexLive();

            APILocator.getIndiciesAPI().point(newinfo);
        } catch (Exception e) {
            throw new ElasticsearchException(e.getMessage(), e);
        }
    }

    public boolean isDotCMSIndexName(final String indexName) {
        return IndexType.WORKING.is(indexName) || IndexType.LIVE.is(indexName);
    }

    public List<String> listDotCMSClosedIndices() {
        return esIndexApi.getClosedIndexes();
    }

    /**
     * Returns a list of dotcms working and live indices.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<String> listDotCMSIndices() {

        return esIndexApi.getIndices(true, false);
    }


    public void activateIndex(final String indexName) throws DotDataException {
        final IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
        final IndiciesInfo.Builder builder = IndiciesInfo.Builder.copy(info);
        if (indexName == null) {
            throw new DotRuntimeException("Index cannot be null");
        }
        if (IndexType.WORKING.is(indexName)) {
            builder.setWorking(esIndexApi.getNameWithClusterIDPrefix(indexName));
            if (esIndexApi.getNameWithClusterIDPrefix(indexName).equals(info.getReindexWorking())) {
                builder.setReindexWorking(null);
            }
        } else if (IndexType.LIVE.is(indexName)) {
            builder.setLive(esIndexApi.getNameWithClusterIDPrefix(indexName));
            if (esIndexApi.getNameWithClusterIDPrefix(indexName).equals(info.getReindexLive())) {
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

        APILocator.getIndiciesAPI().point(builder.build());
    }

    public void deactivateIndex(String indexName) throws DotDataException, IOException {
        final IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
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
        APILocator.getIndiciesAPI().point(builder.build());
    }

    @Override
    public long getIndexDocumentCount(final String indexName) {
        return getProvider().getIndexDocumentCount(
                esIndexApi.getNameWithClusterIDPrefix(indexName));
    }

    public synchronized List<String> getCurrentIndex() throws DotDataException {
        final List<String> newIdx = new ArrayList<>();
        final IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
        newIdx.add(esIndexApi.removeClusterIdFromName(info.getWorking()));
        newIdx.add(esIndexApi.removeClusterIdFromName(info.getLive()));
        return newIdx;
    }

    public synchronized List<String> getNewIndex() throws DotDataException {
        final List<String> newIdx = new ArrayList<>();
        final IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();

        if (info.getReindexWorking() != null) {
            newIdx.add(esIndexApi.removeClusterIdFromName(info.getReindexWorking()));
        }
        if (info.getReindexLive() != null) {
            newIdx.add(esIndexApi.removeClusterIdFromName(info.getReindexLive()));
        }
        return newIdx;
    }

    public String getActiveIndexName(final String type) throws DotDataException {
        final IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();

        if (IndexType.WORKING.is(type)) {
            return esIndexApi.removeClusterIdFromName(info.getWorking());
        } else if (IndexType.LIVE.is(type)) {
            return esIndexApi.removeClusterIdFromName(info.getLive());
        }

        return null;
    }

}