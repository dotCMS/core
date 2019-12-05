package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;
import static com.dotmarketing.common.reindex.ReindexThread.ELASTICSEARCH_CONCURRENT_REQUESTS;
import static com.dotmarketing.util.StringUtils.builder;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldFactory;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.RelationshipAPI;
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
import com.dotmarketing.util.json.JSONObject;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;

public class ContentletIndexAPIImpl implements ContentletIndexAPI {

    private static final int TIMEOUT_INDEX_WAIT_FOR_DEFAULT = 30000;
    private static final String TIMEOUT_INDEX_WAIT_FOR = "TIMEOUT_INDEX_WAIT_FOR";
    private static final int TIME_INDEX_FORCE_DEFAULT = 30000;
    private static final String TIMEOUT_INDEX_FORCE = "TIMEOUT_INDEX_FORCE";

    private static final String SELECT_CONTENTLET_VERSION_INFO =
            "select working_inode,live_inode from contentlet_version_info where identifier=?";
    private static ReindexQueueAPI queueApi = null;
    private static final ESIndexAPI esIndexApi = new ESIndexAPI();
    private static final ESMappingAPIImpl mappingAPI = new ESMappingAPIImpl();

    public static final SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyyMMddHHmmss");

    public ContentletIndexAPIImpl() {
        queueApi = APILocator.getReindexQueueAPI();
    }

    public synchronized void getRidOfOldIndex() throws DotDataException {
        IndiciesInfo idxs = APILocator.getIndiciesAPI().loadIndicies();
        if (idxs.working != null)
            delete(idxs.working);
        if (idxs.live != null)
            delete(idxs.live);
        if (idxs.reindex_working != null)
            delete(idxs.reindex_working);
        if (idxs.reindex_live != null)
            delete(idxs.reindex_live);
    }

    /**
     * Tells if at least we have a "working_XXXXXX" index
     * 
     * @return
     * @throws DotDataException
     */
    private synchronized boolean indexReady() throws DotDataException {
        IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
        return info.working != null && info.live != null;
    }

    /**
     * Inits the indexs
     */
    public synchronized void checkAndInitialiazeIndex() {
        new ESClient().getClient(); // this will call initNode
        try {
            // if we don't have a working index, create it
            if (!indexReady())
                initIndex();
        } catch (Exception e) {
            Logger.fatal("ESUil.checkAndInitializeIndex", e.getMessage());

        }
    }

    public synchronized boolean createContentIndex(String indexName) throws ElasticsearchException, IOException {
        return createContentIndex(indexName, 0);
    }

    @Override
    public synchronized boolean createContentIndex(String indexName, int shards) throws ElasticsearchException, IOException {
        ClassLoader classLoader = null;
        URL url = null;
        classLoader = Thread.currentThread().getContextClassLoader();
        String settings = null;
        try {
            url = classLoader.getResource("es-content-settings.json");
            settings = new String(com.liferay.util.FileUtil.getBytes(new File(url.getPath())));
        } catch (Exception e) {
            Logger.error(this.getClass(), "cannot load es-content-settings.json file, skipping", e);
        }

        url = classLoader.getResource("es-content-mapping.json");
        String mapping = new String(com.liferay.util.FileUtil.getBytes(new File(url.getPath())));

        CreateIndexResponse cir = esIndexApi.createIndex(indexName, settings, shards);

        int i = 0;
        while (!cir.isAcknowledged()) {
            DateUtil.sleep(100);

            if (i++ > 300) {
                throw new ElasticsearchException("index timed out creating");
            }
        }

        mappingAPI.putMapping(indexName, "content", mapping);
        addCustomMapping(indexName);

        return true;
    }

    /**
     * Sets a custom index mapping for relationships and also for mapping defined on field variables
     * using `esCustomMapping` property
     * @param indexName - Index where mapping will be updated
     */
    private void addCustomMapping(final String indexName)  {

        final RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();

        final Set<String> mappedRelationships = addCustomMappingFromFieldVariables(indexName,
                relationshipAPI);

        addCustomMappingForRelationships(indexName, relationshipAPI, mappedRelationships);
    }

    /**
     * Sets a mapping for all relationships except for those that contains its custom mapping using field variables
     * @param indexName - Index where mapping will be updated
     * @param relationshipAPI
     * @param mappedRelationships - Mapping already set for relationships through field variables
     * View {@link ContentletIndexAPIImpl#addCustomMappingFromFieldVariables(String, RelationshipAPI)}
     */
    private void addCustomMappingForRelationships(final String indexName, final RelationshipAPI relationshipAPI,
            final Set<String> mappedRelationships) {
        final List<Relationship> relationships = relationshipAPI.dbAll();

        for(final Relationship relationship: relationships){
            final String relationshipName = relationship.getRelationTypeValue().toLowerCase();
            if (!mappedRelationships.contains(relationshipName)) {
                final JSONObject properties = new JSONObject();
                try{
                    properties.put("properties", new JSONObject()
                            .put(relationshipName,
                                    new JSONObject("{\n"
                                            + "\"type\":  \"keyword\",\n"
                                            + "\"ignore_above\": 8191\n"
                                            + "}")));
                    mappingAPI.putMapping(indexName, "content", properties.toString());
                } catch (Exception e) {
                    Logger.warn(this,
                            "Error updating ES mapping for relationship " + relationshipName
                                    + ". Custom mapping will be ignored.", e);
                }
            }
        }
    }

    /**
     * Sets a mapping defined on field variables
     * @param indexName - Index where mapping will be updated
     * @param relationshipAPI
     * @return Collection of relationship names whose mapping was set
     */
    private Set<String> addCustomMappingFromFieldVariables(final String indexName,
            final RelationshipAPI relationshipAPI) {
        final FieldFactory fieldFactory = FactoryLocator.getFieldFactory();
        final Set<String> mappedRelationships = new HashSet<>();

        final User user;
        try {
            user = APILocator.getUserAPI().getSystemUser();

            //Find field variables
            final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
            final List<FieldVariable> fieldVariables = fieldFactory
                    .byFieldVariableKey(FieldVariable.ES_CUSTOM_MAPPING_KEY);

            for (final FieldVariable fieldVariable : fieldVariables) {
                Field field = null;
                ContentType type = null;
                try {
                    field = fieldFactory.byId(fieldVariable.fieldId());
                    type = contentTypeAPI.find(field.contentTypeId());
                    final JSONObject jsonObject = new JSONObject();
                    final JSONObject properties = new JSONObject();

                    jsonObject.put(type.variable().toLowerCase(),
                            new JSONObject()
                                    .put("properties", new JSONObject()
                                            .put(field.variable()
                                                            .toLowerCase(),
                                                    new JSONObject(fieldVariable.value()))));
                    properties.put("properties", jsonObject);
                    mappingAPI.putMapping(indexName, "content", properties.toString());

                    if (field instanceof RelationshipField) {
                        final Relationship relationship = relationshipAPI
                                .getRelationshipFromField(field, user);
                        mappedRelationships.add(relationship.getRelationTypeValue().toLowerCase());
                    }
                } catch (Exception e) {
                    String message = "Error setting custom index mapping from field variable " + fieldVariable.key();

                    if (field != null){
                        message += ". Field: " + field.name();
                    }

                    if (type != null) {
                        message += ". Content Type: " + type.name();
                    }

                    message += ". Custom mapping will be ignored.";
                    Logger.warn(this, message, e);
                }
            }
        } catch (DotDataException e) {
            Logger.warn(this, "Error setting custom index mapping for index " + indexName, e);
        }
        return mappedRelationships;
    }

    /**
     * Creates new indexes /working_TIMESTAMP (aliases working_read, working_write and workinglive) and
     * /live_TIMESTAMP with (aliases live_read, live_write, workinglive)
     *
     * @return the timestamp string used as suffix for indices
     * @throws ElasticsearchException if Murphy comes around
     * @throws DotDataException
     */
    private synchronized String initIndex() throws ElasticsearchException, DotDataException {
        if (indexReady())
            return "";
        try {
            final String timeStamp = timestampFormatter.format(new Date());

            final String workingIndex = ES_WORKING_INDEX_NAME + "_" + timeStamp;
            final String liveIndex = ES_LIVE_INDEX_NAME + "_" + timeStamp;

            createContentIndex(workingIndex, 0);
            createContentIndex(liveIndex, 0);

            IndiciesInfo info = new IndiciesInfo();
            info.working = workingIndex;
            info.live = liveIndex;

            IndiciesInfo oldInfo = APILocator.getIndiciesAPI().loadIndicies();

            if (oldInfo != null && oldInfo.site_search != null){
                info.site_search = oldInfo.site_search;
            }
            APILocator.getIndiciesAPI().point(info);

            return timeStamp;
        } catch (Exception e) {
            throw new ElasticsearchException(e.getMessage(), e);
        }

    }
    /**
     * Stops the current re-indexation process and switches the current index to the new one. The main
     * goal of this method is to allow users to switch to the new index even if one or more contents
     * could not be re-indexed.
     * <p>
     * This is very useful because the new index can be created and used immediately. The user can have
     * the new re-indexed content available and then work on the conflicting contents, which can be
     * either fixed or removed from the database.
     * </p>
     *
     * @throws SQLException An error occurred when interacting with the database.
     * @throws DotDataException The process to switch to the new failed.
     * @throws InterruptedException The established pauses to switch to the new index failed.
     */
    @Override
    @CloseDBIfOpened
    public void stopFullReindexationAndSwitchover() throws  DotDataException {
        try {
            ReindexThread.pause();
            queueApi.deleteReindexAndFailedRecords();
            this.reindexSwitchover(true);
        } finally {
            ReindexThread.unpause();
        }
    }

    /**
     * Switches the current index structure to the new re-indexed data. This method also allows users to
     * switch to the new re-indexed data even if there are still remaining contents in the
     * {@code dist_reindex_journal} table.
     *
     * @param forceSwitch - If {@code true}, the new index will be used, even if there are contents that
     *        could not be processed. Otherwise, set to {@code false} and the index switch will only
     *        happen if ALL contents were re-indexed.
     * @throws SQLException An error occurred when interacting with the database.
     * @throws DotDataException The process to switch to the new failed.
     * @throws InterruptedException The established pauses to switch to the new index failed.
     */
    @Override
    @CloseDBIfOpened
    public void reindexSwitchover(boolean forceSwitch) throws DotDataException {

        // We double check again. Only one node will enter this critical
        // region, then others will enter just to see that the switchover is
        // done

        if (forceSwitch || queueApi.recordsInQueue() == 0) {
            Logger.info(this, "Running Reindex Switchover");
            // Wait a bit while all records gets flushed to index
            this.fullReindexSwitchover(forceSwitch);
            // Wait a bit while elasticsearch flushes it state
        }

    }

    /**
     * creates new working and live indexes with reading aliases pointing to old index and write aliases
     * pointing to both old and new indexes
     *
     * @return the timestamp string used as suffix for indices
     * @throws DotDataException
     * @throws ElasticsearchException
     */
    @WrapInTransaction
    public synchronized String fullReindexStart() throws ElasticsearchException, DotDataException {
        if (indexReady() && !isInFullReindex()) {
            try {

                final String timeStamp = timestampFormatter.format(new Date());

                // index names for new index
                final String workingIndex = ES_WORKING_INDEX_NAME + "_" + timeStamp;
                final String liveIndex = ES_LIVE_INDEX_NAME + "_" + timeStamp;

                createContentIndex(workingIndex);
                createContentIndex(liveIndex);

                IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
                IndiciesInfo newinfo = new IndiciesInfo();
                newinfo.working = info.working;
                newinfo.live = info.live;
                newinfo.reindex_working = workingIndex;
                newinfo.reindex_live = liveIndex;
                newinfo.site_search = info.site_search;
                APILocator.getIndiciesAPI().point(newinfo);

                return timeStamp;
            } catch (Exception e) {
                throw new ElasticsearchException(e.getMessage(), e);
            }
        } else
            return initIndex();
    }

    @CloseDBIfOpened
    public boolean isInFullReindex() throws DotDataException {
        IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
        return info.reindex_working != null && info.reindex_live != null;
    }

    @CloseDBIfOpened
    public boolean fullReindexSwitchover(final boolean forceSwitch) {
        return fullReindexSwitchover(DbConnectionFactory.getConnection(), forceSwitch);
    }

    /**
     * This will drop old index and will point read aliases to new index. This method should be called
     * after call to {@link #fullReindexStart()}
     *
     * @return
     */
    @CloseDBIfOpened
    public boolean fullReindexSwitchover(Connection conn, final boolean forceSwitch) {


        if(reindexTimeElapsedInLong()<Config.getLongProperty("REINDEX_THREAD_MINIMUM_RUNTIME_IN_SEC", 15)*1000) {
          Logger.info(this.getClass(), "Reindex has been running only " +reindexTimeElapsed().get() + ". Letting the reindex settle.");
          ThreadUtils.sleep(3000);
          return false;
        }
        try {
            final IndiciesInfo oldInfo = APILocator.getIndiciesAPI().loadIndicies();
            final String luckyServer = Try.of(() -> APILocator.getServerAPI().getOldestServer()).getOrElse(ConfigUtils.getServerId());
            if (!forceSwitch) {
                if (!isInFullReindex()) {
                    return false;
                }
                if (!luckyServer.equals(ConfigUtils.getServerId())) {
                    Logger.info(this.getClass(), "fullReindexSwitchover: Letting server [" + luckyServer + "] make the switch. My id : ["
                            + ConfigUtils.getServerId() + "]");
                    DateUtil.sleep(4000);
                    return false;
                }
            }

            final IndiciesInfo newInfo = new IndiciesInfo();
            newInfo.live = oldInfo.reindex_live;
            newInfo.working = oldInfo.reindex_working;
            newInfo.site_search = oldInfo.site_search;
            logSwitchover(oldInfo);
            APILocator.getIndiciesAPI().point(newInfo);

            DotConcurrentFactory.getInstance().getSubmitter().submit(() -> {
                try {
                    Logger.info(this.getClass(), "Updating and optimizing ElasticSearch Indexes");
                    esIndexApi.moveIndexBackToCluster(newInfo.working);
                    esIndexApi.moveIndexBackToCluster(newInfo.live);
                    optimize(ImmutableList.of(newInfo.working, newInfo.live));
                } catch (Exception e) {
                    Logger.warnAndDebug(this.getClass(), "unable to expand ES replicas:" + e.getMessage(), e);
                }
            });

        } catch (Exception e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
        return true;
    }


    private long reindexTimeElapsedInLong() {
        try {
            final IndiciesInfo oldInfo = APILocator.getIndiciesAPI().loadIndicies();
            if (oldInfo.reindex_working != null) {
                Date startTime = timestampFormatter.parse(oldInfo.reindex_working.replace("working_", ""));
                return System.currentTimeMillis() - startTime.getTime();
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
        return Optional.ofNullable(
            Duration.ofMillis(reindexTimeElapsedInLong()).toString().substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase());
      }
    } catch (Exception e) {
      Logger.debug(this, "unable to parse time:" + e, e);
    }
    return Optional.empty();
  }

    private void logSwitchover(IndiciesInfo oldInfo) {
        Logger.info(this, "-------------------------------");
        Optional<String> duration = reindexTimeElapsed();
        if (duration.isPresent()) {
            Logger.info(this, "Reindex took        : " + duration.get() );
        }
        Logger.info(this, "Switching Server Id : " + ConfigUtils.getServerId() );
        Logger.info(this, "Old indicies        : [" + oldInfo.working + "," + oldInfo.live + "]");
        Logger.info(this, "New indicies        : [" + oldInfo.reindex_working + "," + oldInfo.reindex_live + "]");
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
    public void addContentToIndex(final Contentlet parentContenlet, final boolean includeDependencies)
            throws DotDataException {

        if (null == parentContenlet || !UtilMethods.isSet(parentContenlet.getIdentifier())) {
            return;
        }

        Logger.info(this, "Indexing: " + parentContenlet.getIdentifier()
                + ", includeDependencies: " + includeDependencies +
                ", policy: " + parentContenlet.getIndexPolicy());

        final List<Contentlet> contentToIndex = includeDependencies
                ? ImmutableList.<Contentlet>builder()
                .add(parentContenlet)
                .addAll(
                        loadDeps(parentContenlet)
                                .stream()
                                .peek((dep)-> dep.setIndexPolicy(parentContenlet.getIndexPolicyDependencies()))
                                .collect(Collectors.toList()))
                .build()
                : ImmutableList.of(parentContenlet);


        if(parentContenlet.getIndexPolicy()==IndexPolicy.DEFER) {
            queueApi.addContentletsReindex(contentToIndex);
        } else if (!DbConnectionFactory.inTransaction()) {
            addContentToIndex(contentToIndex);
        } else {
            HibernateUtil.addSyncCommitListener(() -> addContentToIndex(contentToIndex));
        }
    }

    /**
     * Stops the full re-indexation process. This means clearing up the content queue and the reindex
     * journal.
     *
     * @throws DotDataException
     */
    @Override
    @WrapInTransaction
    public void stopFullReindexation() throws DotDataException {
        try {
            ReindexThread.pause();
            queueApi.deleteReindexAndFailedRecords();
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
                CollectionsUtils.partition(contentToIndex, contentlet -> contentlet.getIndexPolicy() == IndexPolicy.DEFER,
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
        final BulkRequestBuilder bulk = createBulkRequest(contentToIndex);
        bulk.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        putToIndex(bulk);
    } // indexContentListNow.

    private void indexContentListWaitFor(final List<Contentlet> contentToIndex) {

        final BulkRequestBuilder bulk = createBulkRequest(contentToIndex);
        bulk.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
        putToIndex(bulk);
    } // indexContentListWaitFor.

    private void indexContentListDefer(final List<Contentlet> contentToIndex) {
        final BulkRequestBuilder bulk = createBulkRequest(contentToIndex);
        bulk.setRefreshPolicy(WriteRequest.RefreshPolicy.NONE);
        putToIndex(bulk);
    } // indexContentListWaitFor.

    @Override
    public void putToIndex(final BulkRequestBuilder bulk, ActionListener<BulkResponse> listener) {
        if (bulk != null && bulk.numberOfActions() > 0) {
            if (listener != null) {
                bulk.setTimeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS)).execute(listener);
            } else {
                BulkResponse response = bulk
                        .setTimeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS))
                        .execute().actionGet();

                if (response != null && response.hasFailures()) {
                    Logger.error(this,
                            "Error reindexing (" + response.getItems().length + ") content(s) "
                                    + response.buildFailureMessage());
                }
            }

        }
    }

    @Override
    public void putToIndex(final BulkRequestBuilder bulk) {
        this.putToIndex(bulk, null);
    }

    @Override
    public BulkRequestBuilder createBulkRequest(final List<Contentlet> contentToIndex) {
        final BulkIndexWrapper bulkIndexWrapper = new BulkIndexWrapper(createBulkRequest());
        this.appendBulkRequest(bulkIndexWrapper, contentToIndex);
        return bulkIndexWrapper.getRequestBuilder();
    }

    @Override
    public BulkRequestBuilder createBulkRequest() {
        return new ESClient().getClient().prepareBulk().setRefreshPolicy(RefreshPolicy.NONE);
    }

    public BulkProcessor createBulkProcessor(final BulkProcessorListener bulkListener) {
        final BulkProcessor.Builder builder = BulkProcessor.builder(
                new ESClient().getClient(), bulkListener);
        builder.setBulkActions(ReindexThread.ELASTICSEARCH_BULK_ACTIONS)
                .setBulkSize(new ByteSizeValue(ReindexThread.ELASTICSEARCH_BULK_SIZE, ByteSizeUnit.MB))
                .setConcurrentRequests(ELASTICSEARCH_CONCURRENT_REQUESTS);

        return builder.build();
    }

    @Override
    public BulkRequestBuilder appendBulkRequest(final BulkRequestBuilder bulk, final Collection<ReindexEntry> idxs)
            throws DotDataException {

        for (ReindexEntry idx : idxs) {
            appendBulkRequest(bulk, idx);
        }
        return bulk;
    }

    public void appendToBulkProcessor(final BulkProcessor bulk, final Collection<ReindexEntry> idxs)
            throws DotDataException {

        for (ReindexEntry idx : idxs) {
            appendToBulkProcessor(bulk, idx);
        }
    }

    @Override
    public BulkRequestBuilder appendBulkRequest(BulkRequestBuilder bulk, final ReindexEntry idx)  throws DotDataException {
        bulk = (bulk == null) ? createBulkRequest() : bulk;
        Logger.debug(this, "Indexing document " + idx.getIdentToIndex());

        BulkIndexWrapper bulkIndexWrapper = new BulkIndexWrapper(bulk);
        if (idx.isDelete()) {
            appendBulkRemoveRequest(bulkIndexWrapper, idx);
        } else {
            appendBulkRequest(bulkIndexWrapper, idx);
        }
        return bulkIndexWrapper.getRequestBuilder();
    }

    @CloseDBIfOpened
    public void appendBulkRequest(BulkIndexWrapper bulk, final ReindexEntry idx) throws DotDataException {
        List<ContentletVersionInfo> versions = APILocator.getVersionableAPI().findContentletVersionInfos(idx.getIdentToIndex());

        final Map<String, Contentlet> inodes = new HashMap<>();

        for (ContentletVersionInfo cvi : versions) {
            final String workingInode = cvi.getWorkingInode();
            final String liveInode = cvi.getLiveInode();
            inodes.put(workingInode, APILocator.getContentletAPI().findInDb(workingInode).orElse(null));
            if (UtilMethods.isSet(liveInode) && !inodes.containsKey(liveInode)) {
                inodes.put(liveInode, APILocator.getContentletAPI().findInDb(liveInode).orElse(null));
            }
        }
        inodes.values().removeIf(Objects::isNull);
        if (inodes.isEmpty()) {
            //If there is no content for this entry, it should be deleted to avoid future attempts that will fail also
            APILocator.getReindexQueueAPI().deleteReindexEntry(idx);
            Logger.debug(this, "unable to find versions for content id:" + idx.getIdentToIndex());
        }
        for (Contentlet contentlet : inodes.values()) {
            Logger.debug(this, "indexing: id:" + contentlet.getInode() + " priority: " + idx.getPriority());
            contentlet.setIndexPolicy(IndexPolicy.DEFER);

            try {
                addBulkRequest(bulk, ImmutableList.of(contentlet), idx.isReindex());

            } catch (Exception e) {
                APILocator.getReindexQueueAPI().markAsFailed(idx, e.getMessage());

            }
        }
    }

    public BulkProcessor appendToBulkProcessor(BulkProcessor bulk, final ReindexEntry idx) throws DotDataException {
        Logger.debug(this, "Indexing document " + idx.getIdentToIndex());

        BulkIndexWrapper bulkIndexWrapper = new BulkIndexWrapper(bulk);
        if (idx.isDelete()) {
            appendBulkRemoveRequest(bulkIndexWrapper, idx);
        } else {
            appendBulkRequest(bulkIndexWrapper, idx);
        }

        return bulkIndexWrapper.getBulkProcessor();
    }

    private void appendBulkRequest(final BulkIndexWrapper bulk, final List<Contentlet> contentToIndex) {
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

            final String id = contentlet.getIdentifier() + "_" + contentlet.getLanguageId();
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
            final Gson gson = new Gson(); // todo why do we create a new Gson everytime
            String mapping = null;

            try {

                if (this.isWorking(contentlet)) {
                    mapping = gson.toJson(mappingAPI.toMap(contentlet));
                    if (!forReindex || info.reindex_working == null) {
                        bulk.add(new IndexRequest(info.working, "content", id)
                                .source(mapping, XContentType.JSON));
                    }
                    if (info.reindex_working != null) {
                        bulk.add(new IndexRequest(info.reindex_working, "content", id)
                                .source(mapping, XContentType.JSON));
                    }
                }

                if (this.isLive(contentlet)) {
                    if (mapping == null) {
                        mapping = gson.toJson(mappingAPI.toMap(contentlet));
                    }
                    if (!forReindex || info.reindex_live == null) {
                        bulk.add(new IndexRequest(info.live, "content", id)
                                .source(mapping, XContentType.JSON));
                    }
                    if (info.reindex_live != null) {
                        bulk.add(new IndexRequest(info.reindex_live, "content", id)
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

    private boolean isWorking (final Contentlet contentlet) {

        boolean isWorking = false;

        try {
            isWorking = contentlet.isWorking();
        }catch (Exception e) {
            Logger.debug(this, e.getMessage(), e);
            Logger.warn(this, e.getMessage(), e);
            isWorking = false;
        }

        return isWorking;
    }

    private boolean isLive (final Contentlet contentlet) {

        boolean isLive = false;

        try {
            isLive = contentlet.isLive();
        }catch (Exception e) {
            Logger.debug(this, e.getMessage(), e);
            Logger.warn(this, e.getMessage(), e);
            isLive = false;
        }

        return isLive;
    }

    @CloseDBIfOpened
    @SuppressWarnings("unchecked")
    private List<Contentlet> loadDeps(final Contentlet parentContentlet) {

        final List<Contentlet> contentToIndex = new ArrayList<Contentlet>();
        final List<String> depsIdentifiers = Sneaky.sneak(() -> this.mappingAPI.dependenciesLeftToReindex(parentContentlet));
        for (final String identifier : depsIdentifiers) {

            // get working and live version for all languages based on the identifier
            final List<Map<String, String>> versionInfoMapResults =
                    Sneaky.sneak(() -> new DotConnect().setSQL(SELECT_CONTENTLET_VERSION_INFO).addParam(identifier).loadResults());
            final List<String> inodes = new ArrayList<>();
            for (final Map<String, String> versionInfoMap : versionInfoMapResults) {

                final String workingInode = versionInfoMap.get("working_inode");
                final String liveInode = versionInfoMap.get("live_inode");
                inodes.add(workingInode);
                if (UtilMethods.isSet(liveInode) && !workingInode.equals(liveInode)) {
                    inodes.add(liveInode);
                }
            }

            for (final String inode : inodes) {

                final Contentlet contentlet =
                        Sneaky.sneak(() -> APILocator.getContentletAPI().find(inode, APILocator.getUserAPI().getSystemUser(), false));
                contentlet.setIndexPolicy(IndexPolicy.DEFER);
                contentToIndex.add(contentlet);
            }
        }
        return contentToIndex;
    }

    public void removeContentFromIndex(final Contentlet content) throws DotHibernateException {
        removeContentFromIndex(content, false);
    }

    public void appendBulkRemoveRequest(final BulkIndexWrapper bulk, final ReindexEntry entry)
            throws DotDataException {
        final List<Language> languages = APILocator.getLanguageAPI().getLanguages();
        final IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
        final Client client = new ESClient().getClient();

        // delete for every language and in every index
        for (Language language : languages) {
            for (final String index : info.asMap().values()) {
                final String id = entry.getIdentToIndex() + "_" + language.getId();

                System.err.println("deleting:" + id);
                bulk.add(client.prepareDelete(index, "content", id).request());
            }
        }
    }

    @Override
    @VisibleForTesting
    public BulkRequestBuilder appendBulkRemoveRequest(final BulkRequestBuilder bulk,
            final ReindexEntry entry) throws DotDataException {
        final BulkIndexWrapper bulkIndexWrapper = new BulkIndexWrapper(bulk);
        appendBulkRemoveRequest(bulkIndexWrapper, entry);
        return bulkIndexWrapper.getRequestBuilder();
    }

    public BulkProcessor appendBulkRemoveRequest(final BulkProcessor bulk, final ReindexEntry entry) throws DotDataException {
        final BulkIndexWrapper bulkIndexWrapper = new BulkIndexWrapper(bulk);
        appendBulkRemoveRequest(bulkIndexWrapper, entry);
        return bulkIndexWrapper.getBulkProcessor();
    }

    private void removeContentFromIndex(final Contentlet content, final boolean onlyLive, final List<Relationship> relationships)
            throws DotHibernateException {

        final boolean indexIsNotDefer = IndexPolicy.DEFER != content.getIndexPolicy();

        try {

            if (indexIsNotDefer) {

                this.handleRemoveIndexNotDefer(content, onlyLive, relationships);
            } else {
                // add a commit listener to index the contentlet if the entire
                // transaction finish clean
                HibernateUtil.addCommitListener(content.getInode() + ReindexRunnable.Action.REMOVING,
                        new RemoveReindexRunnable(content, onlyLive, relationships));
            }
        } catch (DotDataException | DotSecurityException | DotMappingException e1) {
            throw new DotHibernateException(e1.getMessage(), e1);
        }
    } // removeContentFromIndex.

    private void handleRemoveIndexNotDefer(final Contentlet content, final boolean onlyLive, final List<Relationship> relationships)
            throws DotSecurityException, DotMappingException, DotDataException {

        removeContentAndProcessDependencies(content, relationships, onlyLive, content.getIndexPolicy(),
                content.getIndexPolicyDependencies());
    } // handleRemoveIndexNotDefer.

    /**
     * Remove ReindexRunnable runnable
     */
    private class RemoveReindexRunnable extends ReindexRunnable {

        private final Contentlet contentlet;
        private final boolean onlyLive;
        private final List<Relationship> relationships;

        public RemoveReindexRunnable(final Contentlet contentlet, final boolean onlyLive, final List<Relationship> relationships) {

            super(contentlet, ReindexRunnable.Action.REMOVING);
            this.contentlet = contentlet;
            this.onlyLive = onlyLive;
            this.relationships = relationships;
        }

        public void run() {

            try {
                removeContentAndProcessDependencies(this.contentlet, this.relationships, this.onlyLive, IndexPolicy.DEFER,
                        IndexPolicy.DEFER);
            } catch (Exception ex) {
                throw new ElasticsearchException(ex.getMessage(), ex);
            }
        }
    }

    private void removeContentAndProcessDependencies(final Contentlet contentlet, final List<Relationship> relationships,
            final boolean onlyLive, final IndexPolicy indexPolicy, final IndexPolicy indexPolicyDependencies)
            throws DotDataException, DotSecurityException, DotMappingException {

        final String id = builder(contentlet.getIdentifier(), StringPool.UNDERLINE, contentlet.getLanguageId()).toString();
        final Client client = new ESClient().getClient();
        final IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
        final BulkRequestBuilder bulk = client.prepareBulk();

        // we want to wait until the content is already indexed
        switch (indexPolicy) {
            case FORCE:
                bulk.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
                bulk.setTimeout(TimeValue.timeValueMillis(Config.getLongProperty(TIMEOUT_INDEX_FORCE, TIME_INDEX_FORCE_DEFAULT)));
                break;

            case WAIT_FOR:
                bulk.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
                bulk.setTimeout(TimeValue.timeValueMillis(Config.getLongProperty(TIMEOUT_INDEX_WAIT_FOR, TIMEOUT_INDEX_WAIT_FOR_DEFAULT)));
                break;
        }

        bulk.add(client.prepareDelete(info.live, "content", id));

        if (info.reindex_live != null) {

            bulk.add(client.prepareDelete(info.reindex_live, "content", id));
        }

        if (!onlyLive) {

            // here we search for relationship fields pointing to this
            // content to be deleted. Those contentlets are reindexed
            // to avoid left those fields making noise in the index
            if (UtilMethods.isSet(relationships)) {
                reindexDependenciesForDeletedContent(contentlet, relationships, indexPolicyDependencies);
            }

            bulk.add(client.prepareDelete(info.working, "content", id));
            if (info.reindex_working != null) {
                bulk.add(client.prepareDelete(info.reindex_working, "content", id));
            }
        }

        bulk.execute().actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
    }

    private void reindexDependenciesForDeletedContent(final Contentlet contentlet, final List<Relationship> relationships,
            final IndexPolicy indexPolicy)
            throws DotDataException, DotSecurityException, DotMappingException {

        for (final Relationship relationship : relationships) {

            final boolean isSameStructRelationship = FactoryLocator.getRelationshipFactory().sameParentAndChild(relationship);

            final String query = (isSameStructRelationship)
                    ? builder("+type:content +(", relationship.getRelationTypeValue(), "-parent:", contentlet.getIdentifier(),
                            StringPool.SPACE, relationship.getRelationTypeValue(), "-child:", contentlet.getIdentifier(), ") ").toString()
                    : builder("+type:content +", relationship.getRelationTypeValue(), ":", contentlet.getIdentifier()).toString();

            final List<Contentlet> related =
                    APILocator.getContentletAPI().search(query, -1, 0, null, APILocator.getUserAPI().getSystemUser(), false);

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

    @CloseDBIfOpened
    public void removeContentFromIndex(final Contentlet content, final boolean onlyLive) throws DotHibernateException {

        if (content == null || !UtilMethods.isSet(content.getIdentifier()))
            return;

        List<Relationship> relationships = FactoryLocator.getRelationshipFactory().byContentType(content.getStructure());

        // add a commit listener to index the contentlet if the entire
        // transaction finish clean
        removeContentFromIndex(content, onlyLive, relationships);

    }

    public void removeContentFromLiveIndex(final Contentlet content) throws DotHibernateException {
        removeContentFromIndex(content, true);
    }

    public void removeContentFromIndexByStructureInode(final String structureInode)
            throws DotDataException, DotSecurityException {
        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.getUserAPI().getSystemUser()).find(structureInode);
        if(contentType==null){
            throw new DotDataException("ContentType with Inode or VarName: " + structureInode + "not found");
        }
        final String structureName = contentType.variable();
        final IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();

        // collecting indexes
        final List<String> idxs = new ArrayList<String>();
        idxs.add(info.working);
        idxs.add(info.live);
        if (info.reindex_working != null)
            idxs.add(info.reindex_working);
        if (info.reindex_live != null)
            idxs.add(info.reindex_live);
        String[] idxsArr = new String[idxs.size()];
        idxsArr = idxs.toArray(idxsArr);

        final BulkByScrollResponse response = DeleteByQueryAction.INSTANCE.newRequestBuilder(new ESClient().getClient())
                .filter(QueryBuilders.matchQuery("contenttype",structureName.toLowerCase())).source(idxsArr).get(new TimeValue(INDEX_OPERATIONS_TIMEOUT_IN_MS));

        Logger.info(this, "Records deleted: " + response.getDeleted() + " from contentType: " + structureName);
    }

    public void fullReindexAbort() {
        try {
            if (!isInFullReindex())
                return;

            IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();

            final String rew = info.reindex_working;
            final String rel = info.reindex_live;

            IndiciesInfo newinfo = new IndiciesInfo();
            newinfo.working = info.working;
            newinfo.live = info.live;
            newinfo.site_search = info.site_search;
            APILocator.getIndiciesAPI().point(newinfo);

            esIndexApi.moveIndexBackToCluster(rew);
            esIndexApi.moveIndexBackToCluster(rel);

        } catch (Exception e) {
            throw new ElasticsearchException(e.getMessage(), e);
        }
    }

    public boolean isDotCMSIndexName(String indexName) {
        return indexName.startsWith(ES_WORKING_INDEX_NAME + "_") || indexName.startsWith(ES_LIVE_INDEX_NAME + "_");
    }

    public List<String> listDotCMSClosedIndices() {
        List<String> indexNames = new ArrayList<String>();
        List<String> list = APILocator.getESIndexAPI().getClosedIndexes();
        for (String idx : list)
            if (isDotCMSIndexName(idx))
                indexNames.add(idx);
        return indexNames;
    }

    /**
     * Returns a list of dotcms working and live indices.
     * 
     * @return
     */
    public List<String> listDotCMSIndices() {
        Client client = new ESClient().getClient();
        Map<String, IndexStats> indices = APILocator.getESIndexAPI().getIndicesAndStatus();
        List<String> indexNames = new ArrayList<String>();

        for (String idx : indices.keySet())
            if (isDotCMSIndexName(idx))
                indexNames.add(idx);

        List<String> existingIndex = new ArrayList<String>();
        for (String idx : indexNames)
            if (client.admin().indices().exists(new IndicesExistsRequest(idx)).actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS).isExists())
                existingIndex.add(idx);
        indexNames = existingIndex;

        List<String> indexes = new ArrayList<String>();
        indexes.addAll(indexNames);
        Collections.sort(indexes, new IndexSortByDate());

        return indexes;
    }

    public void activateIndex(String indexName) throws DotDataException {
        IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
        IndiciesInfo newinfo = new IndiciesInfo();
        newinfo.working = info.working;
        newinfo.live = info.live;
        newinfo.reindex_working = info.reindex_working;
        newinfo.reindex_live = info.reindex_live;
        newinfo.site_search = info.site_search;
        if (indexName.startsWith(ES_WORKING_INDEX_NAME)) {
            newinfo.working = indexName;
        } else if (indexName.startsWith(ES_LIVE_INDEX_NAME)) {
            newinfo.live = indexName;
        }
        APILocator.getIndiciesAPI().point(newinfo);
    }

    public void deactivateIndex(String indexName) throws DotDataException, IOException {
        IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
        IndiciesInfo newinfo = new IndiciesInfo();
        newinfo.working = info.working;
        newinfo.live = info.live;
        newinfo.reindex_working = info.reindex_working;
        newinfo.reindex_live = info.reindex_live;
        newinfo.site_search = info.site_search;
        if (indexName.equals(info.working)) {
            newinfo.working = null;
        } else if (indexName.equals(info.live)) {
            newinfo.live = null;
        } else if (indexName.equals(info.reindex_working)) {
            esIndexApi.moveIndexBackToCluster(info.reindex_working);
            newinfo.reindex_working = null;
        } else if (indexName.equals(info.reindex_live)) {
            esIndexApi.moveIndexBackToCluster(info.reindex_live);
            newinfo.reindex_live = null;
        }
        APILocator.getIndiciesAPI().point(newinfo);
    }

    public synchronized List<String> getCurrentIndex() throws DotDataException {
        List<String> newIdx = new ArrayList<String>();
        IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
        newIdx.add(info.working);
        newIdx.add(info.live);
        return newIdx;
    }

    public synchronized List<String> getNewIndex() throws DotDataException {
        List<String> newIdx = new ArrayList<String>();
        IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
        if (info.reindex_working != null)
            newIdx.add(info.reindex_working);
        if (info.reindex_live != null)
            newIdx.add(info.reindex_live);
        return newIdx;
    }

    private class IndexSortByDate implements Comparator<String> {
        public int compare(String o1, String o2) {
            if (o1 == null || o2 == null) {
                return 0;
            }
            if (o1.indexOf("_") < 0) {
                return 1;
            }
            if (o2.indexOf("_") < 0) {
                return -1;
            }
            String one = o1.split("_")[1];
            String two = o2.split("_")[1];
            return two.compareTo(one);
        }
    }

    public String getActiveIndexName(String type) throws DotDataException {
        IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
        if (type.equalsIgnoreCase(ES_WORKING_INDEX_NAME)) {
            return info.working;
        } else if (type.equalsIgnoreCase(ES_LIVE_INDEX_NAME)) {
            return info.live;
        }
        return null;
    }

}
