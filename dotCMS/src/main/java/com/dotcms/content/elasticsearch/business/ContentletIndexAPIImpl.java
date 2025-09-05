package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;
import static com.dotmarketing.common.reindex.ReindexThread.ELASTICSEARCH_CONCURRENT_REQUESTS;
import static com.dotmarketing.util.StringUtils.builder;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.util.ESMappingUtilHelper;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.JsonUtil;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
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
import com.dotmarketing.portlets.contentlet.business.ContentletFactory;
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
import java.sql.SQLException;
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
import java.util.stream.Collectors;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BackoffPolicy;
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
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public class ContentletIndexAPIImpl implements ContentletIndexAPI {

    private static final int TIMEOUT_INDEX_WAIT_FOR_DEFAULT = 30000;
    private static final String TIMEOUT_INDEX_WAIT_FOR = "TIMEOUT_INDEX_WAIT_FOR";
    private static final int TIME_INDEX_FORCE_DEFAULT = 30000;
    private static final String TIMEOUT_INDEX_FORCE = "TIMEOUT_INDEX_FORCE";

    private static final String SELECT_CONTENTLET_VERSION_INFO =
            "select working_inode,live_inode from contentlet_version_info where identifier IN (%s)";
    private static ReindexQueueAPI queueApi = null;
    private static final ESIndexAPI esIndexApi = new ESIndexAPI();
    private static final ESMappingAPIImpl mappingAPI = new ESMappingAPIImpl();

    private static ObjectMapper objectMapper = DotObjectMapperProvider.createDefaultMapper();

    public ContentletIndexAPIImpl() {
        queueApi = APILocator.getReindexQueueAPI();
    }

    public synchronized void getRidOfOldIndex() throws DotDataException {
        IndiciesInfo idxs = APILocator.getIndiciesAPI().loadIndicies();
        if (idxs.getWorking() != null) {
            delete(idxs.getWorking());
        }
        if (idxs.getLive() != null) {
            delete(idxs.getLive());
        }
        if (idxs.getReindexWorking() != null) {
            delete(idxs.getReindexWorking());
        }
        if (idxs.getReindexLive() != null) {
            delete(idxs.getReindexLive());
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
    public synchronized void checkAndInitialiazeIndex() {
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
        CreateIndexResponse cir = esIndexApi.createIndex(indexName, settings, shards);

        int i = 0;
        while (!cir.isAcknowledged()) {
            DateUtil.sleep(100);

            if (i++ > 300) {
                throw new ElasticsearchException("index timed out creating");
            }
        }

        mappingAPI.putMapping(indexName, mapping);

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
                    Logger.info(this, "Full reindex started by user: " + currentUser.getUserId() + " (" + currentUser.getEmailAddress() + ")");
                } else {
                    Logger.info(this, "Full reindex started by system user");
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
        final BulkRequest bulkRequest = createBulkRequest(contentToIndex);
        bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        putToIndex(bulkRequest);
        CacheLocator.getESQueryCache().clearCache();
    } // indexContentListNow.

    private void indexContentListWaitFor(final List<Contentlet> contentToIndex) {
        final BulkRequest bulkRequest = createBulkRequest(contentToIndex);
        bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
        putToIndex(bulkRequest);
        CacheLocator.getESQueryCache().clearCache();
    } // indexContentListWaitFor.

    private void indexContentListDefer(final List<Contentlet> contentToIndex) {
        final BulkRequest bulkRequest = createBulkRequest(contentToIndex);
        bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.NONE);
        putToIndex(bulkRequest);
    } // indexContentListWaitFor.

    @Override
    public void putToIndex(final BulkRequest bulkRequest,
            final ActionListener<BulkResponse> listener) {

        try {
            if (bulkRequest != null && bulkRequest.numberOfActions() > 0) {
                bulkRequest.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));

                if (listener != null) {
                    RestHighLevelClientProvider.getInstance()
                            .getClient().bulkAsync(bulkRequest, RequestOptions.DEFAULT, listener);
                } else {
                    BulkResponse response = Sneaky.sneak(
                            () -> RestHighLevelClientProvider.getInstance().getClient()
                                    .bulk(bulkRequest, RequestOptions.DEFAULT));

                    if (response != null && response.hasFailures()) {
                        Logger.error(this,
                                "Erro" +
                                        "r reindexing (" + response.getItems().length
                                        + ") content(s) "
                                        + response.buildFailureMessage());
                    }
                }
            }
        } catch (final Exception e) {
            if (ExceptionUtil.causedBy(e, IllegalStateException.class)) {
                ContentletFactory.rebuildRestHighLevelClientIfNeeded(e);
            }
            Logger.warnAndDebug(ContentletIndexAPIImpl.class, e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void putToIndex(final BulkRequest bulkRequest) {
        this.putToIndex(bulkRequest, null);
    }

    @Override
    public BulkRequest createBulkRequest(final List<Contentlet> contentToIndex) {
        final BulkIndexWrapper bulkIndexWrapper = new BulkIndexWrapper(createBulkRequest());
        this.appendBulkRequest(bulkIndexWrapper, contentToIndex);
        return bulkIndexWrapper.getRequestBuilder();
    }

    @Override
    public BulkRequest createBulkRequest() {
        final BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.setRefreshPolicy(RefreshPolicy.NONE);
        return bulkRequest;

    }

    public BulkProcessor createBulkProcessor(final BulkProcessorListener bulkProcessorListener) {
        BulkProcessor.Builder builder = BulkProcessor.builder(
                (request, bulkListener) ->
                        RestHighLevelClientProvider.getInstance().getClient()
                                .bulkAsync(request, RequestOptions.DEFAULT, bulkListener),
                bulkProcessorListener);

        // if running in a cluster reduce the number of concurrent requests in order to not overtax ES
        final int numberToReindexInRequest = Try.of(
                () -> ReindexThread.ELASTICSEARCH_BULK_ACTIONS / APILocator.getServerAPI()
                        .getReindexingServers().size()).getOrElse(10);

        builder.setBulkActions(numberToReindexInRequest)
                .setBulkSize(
                        new ByteSizeValue(ReindexThread.ELASTICSEARCH_BULK_SIZE, ByteSizeUnit.MB))
                .setConcurrentRequests(ELASTICSEARCH_CONCURRENT_REQUESTS)
                .setBackoffPolicy(BackoffPolicy.constantBackoff(TimeValue.timeValueSeconds(
                                ReindexThread.BACKOFF_POLICY_TIME_IN_SECONDS),
                        ReindexThread.BACKOFF_POLICY_MAX_RETRYS));

        return builder.build();
    }

    @Override
    public BulkRequest appendBulkRequest(final BulkRequest bulkRequest,
            final Collection<ReindexEntry> idxs)
            throws DotDataException {

        for (ReindexEntry idx : idxs) {
            appendBulkRequest(bulkRequest, idx);
        }
        return bulkRequest;
    }

    public void appendToBulkProcessor(final BulkProcessor bulk, final Collection<ReindexEntry> idxs)
            throws DotDataException {

        for (ReindexEntry idx : idxs) {
            appendToBulkProcessor(bulk, idx);
        }
    }

    @Override
    public BulkRequest appendBulkRequest(BulkRequest bulkRequest, final ReindexEntry idx)
            throws DotDataException {
        bulkRequest = (bulkRequest == null) ? createBulkRequest() : bulkRequest;
        Logger.debug(this, "Indexing document " + idx.getIdentToIndex());

        BulkIndexWrapper bulkIndexWrapper = new BulkIndexWrapper(bulkRequest);
        if (idx.isDelete()) {
            appendBulkRemoveRequest(bulkIndexWrapper, idx);
        } else {
            appendBulkRequest(bulkIndexWrapper, idx);
        }
        return bulkIndexWrapper.getRequestBuilder();
    }

    /**
     * Generates an ES bulk request that adds the specified {@link ReindexEntry} to the
     * ElasticSearch index.
     *
     * @param bulk The {@link BulkIndexWrapper} object containing the Bulk Index Request.
     * @param idx  The entry containing the information of the Contentlet that will be indexed.
     * @throws DotDataException An error occurred when processing this request.
     */
    @CloseDBIfOpened
    public void appendBulkRequest(final BulkIndexWrapper bulk, final ReindexEntry idx)
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
                                idx
                                        .getPriority()));
                contentlet.setIndexPolicy(IndexPolicy.DEFER);
                addBulkRequest(bulk, List.of(contentlet), idx.isReindex());
            }
        } catch (final Exception e) {
            // An error occurred when trying to reindex the Contentlet. Flag it as "failed"
            APILocator.getReindexQueueAPI().markAsFailed(idx, e.getMessage());
        }
    }

    public BulkProcessor appendToBulkProcessor(BulkProcessor bulk, final ReindexEntry idx)
            throws DotDataException {
        Logger.debug(this, "Indexing document " + idx.getIdentToIndex());

        BulkIndexWrapper bulkIndexWrapper = new BulkIndexWrapper(bulk);
        if (idx.isDelete()) {
            appendBulkRemoveRequest(bulkIndexWrapper, idx);
        } else {
            appendBulkRequest(bulkIndexWrapper, idx);
        }

        return bulkIndexWrapper.getBulkProcessor();
    }

    private void appendBulkRequest(final BulkIndexWrapper bulk,
            final List<Contentlet> contentToIndex) {
        this.addBulkRequest(bulk, contentToIndex, false);
    }

    private void addBulkRequest(final BulkIndexWrapper bulk, final List<Contentlet> contentToIndex,
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
                                    () -> objectMapper.writeValueAsString(mappingAPI.toMap(contentlet)))
                            .getOrElseThrow(
                                    DotRuntimeException::new);
                    if (!forReindex || info.getReindexWorking() == null) {
                        bulk.add(new IndexRequest(info.getWorking(), "_doc", id)
                                .source(mapping, XContentType.JSON));
                    }
                    if (info.getReindexWorking() != null) {
                        bulk.add(new IndexRequest(info.getReindexWorking(), "_doc", id)
                                .source(mapping, XContentType.JSON));
                    }
                }

                if (this.isLive(contentlet)) {
                    if (mapping == null) {
                        mapping = Try.of(
                                        () -> objectMapper.writeValueAsString(mappingAPI.toMap(contentlet)))
                                .getOrElseThrow(
                                        DotRuntimeException::new);
                    }
                    if (!forReindex || info.getReindexLive() == null) {
                        bulk.add(new IndexRequest(info.getLive(), "_doc", id)
                                .source(mapping, XContentType.JSON));
                    }
                    if (info.getReindexLive() != null) {
                        bulk.add(new IndexRequest(info.getReindexLive(), "_doc", id)
                                .source(mapping, XContentType.JSON));
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
                this.mappingAPI.dependenciesLeftToReindex(parentContentlet));

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

    public void appendBulkRemoveRequest(final BulkIndexWrapper bulk, final ReindexEntry entry)
            throws DotDataException {
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
                    bulk.add(new DeleteRequest(index, "_doc", id));
                }
            }
        }
    }

    @Override
    @VisibleForTesting
    public BulkRequest appendBulkRemoveRequest(final BulkRequest bulkRequest,
            final ReindexEntry entry) throws DotDataException {
        final BulkIndexWrapper bulkIndexWrapper = new BulkIndexWrapper(bulkRequest);
        appendBulkRemoveRequest(bulkIndexWrapper, entry);
        return bulkIndexWrapper.getRequestBuilder();
    }

    public BulkProcessor appendBulkRemoveRequest(final BulkProcessor bulk, final ReindexEntry entry)
            throws DotDataException {
        final BulkIndexWrapper bulkIndexWrapper = new BulkIndexWrapper(bulk);
        appendBulkRemoveRequest(bulkIndexWrapper, entry);
        return bulkIndexWrapper.getBulkProcessor();
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
                throw new ElasticsearchException(ex.getMessage(), ex);
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
        final BulkRequest bulkRequest = new BulkRequest();

        // we want to wait until the content is already indexed
        switch (indexPolicy) {
            case FORCE:
                bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
                bulkRequest.timeout(TimeValue.timeValueMillis(
                        Config.getLongProperty(TIMEOUT_INDEX_FORCE, TIME_INDEX_FORCE_DEFAULT)));
                break;

            case WAIT_FOR:
                bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
                bulkRequest.timeout(TimeValue.timeValueMillis(
                        Config.getLongProperty(TIMEOUT_INDEX_WAIT_FOR,
                                TIMEOUT_INDEX_WAIT_FOR_DEFAULT)));
                break;
        }

        bulkRequest.add(new DeleteRequest(info.getLive(), "_doc", id));

        if (info.getReindexLive() != null) {

            bulkRequest.add(new DeleteRequest(info.getReindexLive(), "_doc", id));
        }

        if (!onlyLive) {

            // here we search for relationship fields pointing to this
            // content to be deleted. Those contentlets are reindexed
            // to avoid left those fields making noise in the index
            if (UtilMethods.isSet(relationships)) {
                reindexDependenciesForDeletedContent(contentlet, relationships,
                        indexPolicyDependencies);
            }

            bulkRequest.add(new DeleteRequest(info.getWorking(), "_doc", id));
            if (info.getReindexWorking() != null) {
                bulkRequest.add(new DeleteRequest(info.getReindexWorking(), "_doc", id));
            }
        }

        bulkRequest.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
        BulkResponse response = Sneaky.sneak(
                () -> RestHighLevelClientProvider.getInstance().getClient()
                        .bulk(bulkRequest, RequestOptions.DEFAULT));

        if (response.hasFailures()) {
            Logger.error(this,
                    "Failed to remove content from index: " + response.buildFailureMessage());
        }

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

        final String structureName = contentType.variable();
        final IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();

        // collecting indexes
        final List<String> idxs = new ArrayList<>();
        idxs.add(info.getWorking());
        idxs.add(info.getLive());
        if (info.getReindexWorking() != null) {
            idxs.add(info.getReindexWorking());
        }
        if (info.getReindexLive() != null) {
            idxs.add(info.getReindexLive());
        }
        String[] idxsArr = new String[idxs.size()];
        idxsArr = idxs.toArray(idxsArr);

        DeleteByQueryRequest request = new DeleteByQueryRequest(idxsArr);
        request.setQuery(QueryBuilders.matchQuery("contenttype", structureName.toLowerCase()));
        request.setTimeout(new TimeValue(INDEX_OPERATIONS_TIMEOUT_IN_MS));

        BulkByScrollResponse response = Sneaky.sneak(
                () -> RestHighLevelClientProvider.getInstance().getClient()
                        .deleteByQuery(request, RequestOptions.DEFAULT));

        Logger.info(this, "Records deleted: " +
                response.getDeleted() + " from contentType: " + structureName);

        //Delete query cache when a new content has been reindexed
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
            Logger.info(this, "Index activation (" + indexName + ") performed by user: " + currentUser.getUserId() + " (" + currentUser.getEmailAddress() + ")");
        } else {
            Logger.info(this, "Index activation (" + indexName + ") performed by system user");
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
        final CountRequest countRequest = new CountRequest(
                esIndexApi.getNameWithClusterIDPrefix(indexName));
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        countRequest.source(searchSourceBuilder);

        final CountResponse countResponse = Sneaky
                .sneak(() -> RestHighLevelClientProvider.getInstance().getClient()
                        .count(countRequest, RequestOptions.DEFAULT));

        return countResponse.getCount();
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
