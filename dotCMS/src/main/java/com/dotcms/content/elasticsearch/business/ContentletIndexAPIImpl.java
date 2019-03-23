package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;
import static com.dotmarketing.util.StringUtils.builder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexQueueAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.ReindexRunnable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;

public class ContentletIndexAPIImpl implements ContentletIndexAPI {

    private static final int TIMEOUT_INDEX_WAIT_FOR_DEFAULT = 30000;
    private static final String TIMEOUT_INDEX_WAIT_FOR = "TIMEOUT_INDEX_WAIT_FOR";
    private static final int TIME_INDEX_FORCE_DEFAULT = 30000;
    private static final String TIMEOUT_INDEX_FORCE = "TIMEOUT_INDEX_FORCE";

    private static final String SELECT_CONTENTLET_VERSION_INFO =
            "select working_inode,live_inode from contentlet_version_info where identifier=?";
    private static ReindexQueueAPI journalAPI = null;
    private static final ESIndexAPI esIndexApi = new ESIndexAPI();
    private static final ESMappingAPIImpl mappingAPI = new ESMappingAPIImpl();

    public static final SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyyMMddHHmmss");

    private static long fullReindexStartTime = 0;

    public ContentletIndexAPIImpl() {
        journalAPI = APILocator.getReindexQueueAPI();
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
            Logger.fatal("ESUil.checkAndInitialiazeIndex", e.getMessage());

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

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (i++ > 300) {
                throw new ElasticsearchException("index timed out creating");
            }
        }

        mappingAPI.putMapping(indexName, "content", mapping);

        return true;
    }

    /**
     * Creates new indexes /working_TIMESTAMP (aliases working_read, working_write and workinglive) and
     * /live_TIMESTAMP with (aliases live_read, live_write, workinglive)
     *
     * @return the timestamp string used as suffix for indices
     * @throws ElasticsearchException if Murphy comes arround
     * @throws DotDataException
     */
    private synchronized String initIndex() throws ElasticsearchException, DotDataException {
        if (indexReady())
            return "";
        try {
            final String timeStamp = timestampFormatter.format(new Date());

            final String workingIndex = ES_WORKING_INDEX_NAME + "_" + timeStamp;
            final String liveIndex = ES_LIVE_INDEX_NAME + "_" + timeStamp;

            ESClient esClient = new ESClient();
            final IndicesAdminClient iac = esClient.getClient().admin().indices();

            createContentIndex(workingIndex, 0);
            createContentIndex(liveIndex, 0);

            IndiciesInfo info = new IndiciesInfo();
            info.working = workingIndex;
            info.live = liveIndex;
            APILocator.getIndiciesAPI().point(info);

            return timeStamp;
        } catch (Exception e) {
            throw new ElasticsearchException(e.getMessage(), e);
        }

    }

    public long getReindexStartTime() {
        return fullReindexStartTime;
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
        if (indexReady()) {
            try {
                this.fullReindexStartTime = System.currentTimeMillis();

                final String timeStamp = timestampFormatter.format(new Date());

                // index names for new index
                final String workingIndex = ES_WORKING_INDEX_NAME + "_" + timeStamp;
                final String liveIndex = ES_LIVE_INDEX_NAME + "_" + timeStamp;

                final IndicesAdminClient iac = new ESClient().getClient().admin().indices();

                createContentIndex(workingIndex);
                createContentIndex(liveIndex);

                IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
                IndiciesInfo newinfo = new IndiciesInfo();
                newinfo.working = info.working;
                newinfo.live = info.live;
                newinfo.reindex_working = workingIndex;
                newinfo.reindex_live = liveIndex;
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
        return isInFullReindex(DbConnectionFactory.getConnection());
    }

    @CloseDBIfOpened
    public boolean isInFullReindex(Connection conn) throws DotDataException {
        IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies(conn);
        return info.reindex_working != null && info.reindex_live != null;
    }

    @CloseDBIfOpened
    public void fullReindexSwitchover() {
        fullReindexSwitchover(DbConnectionFactory.getConnection());
    }

    /**
     * This will drop old index and will point read aliases to new index. This method should be called
     * after call to {@link #setUpFullReindex()}
     * 
     * @return
     */
    @WrapInTransaction
    public void fullReindexSwitchover(Connection conn) {
        try {
            if (!isInFullReindex())
                return;

            final IndiciesInfo oldInfo = APILocator.getIndiciesAPI().loadIndicies();
            final IndiciesInfo newInfo = new IndiciesInfo();
            String dateStr = oldInfo.reindex_working.replace("working_", "");

            Date startTime = timestampFormatter.parse(dateStr);

            Logger.info(this, "-------------------------------");
            Logger.info(this, "Executing switchover from old index [" + oldInfo.working + "," + oldInfo.live + "] to new index ["
                    + oldInfo.reindex_working + "," + oldInfo.reindex_live + "]");

            long timeTook = System.currentTimeMillis() - startTime.getTime();
            if (timeTook > 0) {
                String duration = Duration.ofMillis(timeTook).toString().substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase();

                Logger.info(this, "Reindex took [" + duration + "]");

            }
            Logger.info(this, "-------------------------------");

            APILocator.getIndiciesAPI().point(newInfo);

            esIndexApi.moveIndexBackToCluster(newInfo.working);
            esIndexApi.moveIndexBackToCluster(newInfo.live);

            ArrayList<String> list = new ArrayList<String>();
            list.add(newInfo.working);
            list.add(newInfo.live);
            optimize(list);

        } catch (Exception e) {
            throw new ElasticsearchException(e.getMessage(), e);
        }
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
    public void addContentToIndex(final Contentlet content, final boolean deps) throws DotDataException {
        addContentToIndex(content, deps, false);
    }

    @Override
    public void addContentToIndex(final Contentlet parentContenlet, final boolean includeDependencies, final boolean indexBeforeCommit)
            throws DotDataException {

        if (null == parentContenlet || !UtilMethods.isSet(parentContenlet.getIdentifier())) {
            return;
        }
        // parentContenlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
        final List<Contentlet> contentToIndex =
                (includeDependencies) ? ImmutableList.<Contentlet>builder().add(parentContenlet).addAll(loadDeps(parentContenlet)).build()
                        : ImmutableList.of(parentContenlet);

        if (indexBeforeCommit == false && DbConnectionFactory.inTransaction()) {
            journalAPI.addContentletsReindex(contentToIndex);
        }

        addContentToIndex(contentToIndex);

    }

    @Override
    public void addContentToIndex(final List<Contentlet> contentToIndex) {

        // split the list on three possible subset, one with the default refresh strategy, second one is the
        // wait for and finally the immediate
        final List<List<Contentlet>> partitions =
                CollectionsUtils.partition(contentToIndex, (contentlet -> contentlet.getIndexPolicy() == IndexPolicy.DEFER),
                        (contentlet -> contentlet.getIndexPolicy() == IndexPolicy.WAIT_FOR),
                        (contentlet -> contentlet.getIndexPolicy() == IndexPolicy.FORCE));

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
                bulk.execute().actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
            }

        }
    }

    @Override
    public void putToIndex(final BulkRequestBuilder bulk) {
        this.putToIndex(bulk, null);
    }

    @Override
    public BulkRequestBuilder createBulkRequest(List<Contentlet> contentToIndex) {
        return this.appendBulkRequest(createBulkRequest(), contentToIndex);
    }

    @Override
    public BulkRequestBuilder createBulkRequest() {
        return new ESClient().getClient().prepareBulk().setRefreshPolicy(RefreshPolicy.NONE);
    }

    @Override
    public BulkRequestBuilder appendReindexRequest(final BulkRequestBuilder bulk, final List<Contentlet> contentToIndex) {

        return this.addToBulkRequest(bulk, contentToIndex, true);
    }

    @Override
    public BulkRequestBuilder appendBulkRequest(final BulkRequestBuilder bulk, final List<Contentlet> contentToIndex) {

        return this.addToBulkRequest(bulk, contentToIndex, false);
    }

    private BulkRequestBuilder addToBulkRequest(final BulkRequestBuilder bulk, final List<Contentlet> contentToIndex,
            final boolean forReindex) {
        if (contentToIndex != null && !contentToIndex.isEmpty()) {
            Logger.debug(this.getClass(), "Indexing " + contentToIndex.size() + " contents, starting with identifier [ "
                    + contentToIndex.get(0).getMap().get("identifier") + "]");
        }

        // eliminate dups
        Set<Contentlet> contentToIndexSet = new HashSet<>(contentToIndex);

        for (final Contentlet contentlet : contentToIndexSet) {

            final String id = contentlet.getIdentifier() + "_" + contentlet.getLanguageId();
            Logger.debug(this, () -> "\n*********----------- Indexing : " + Thread.currentThread().getName() + ", id: "
                    + contentlet.getIdentifier() + ", identityHashCode: " + System.identityHashCode(contentlet));
            Logger.debug(this, () -> "*********-----------  " + DbConnectionFactory.getConnection());
            Logger.debug(this, () -> "*********-----------  "
                    + ExceptionUtil.getCurrentStackTraceAsString(Config.getIntProperty("stacktracelimit", 10)) + "\n");

            final IndiciesInfo info = Sneaky.sneak(() -> APILocator.getIndiciesAPI().loadIndicies());
            final Gson gson = new Gson(); // todo why do we create a new Gson everytime
            String mapping = null;

            try {

                if (Sneaky.sneak(() -> contentlet.isWorking())) {
                    mapping = gson.toJson(mappingAPI.toMap(contentlet));
                    if (!forReindex || info.reindex_working == null) {
                        bulk.add(new IndexRequest(info.working, "content", id).source(mapping, XContentType.JSON));
                    }
                    if (info.reindex_working != null) {
                        bulk.add(new IndexRequest(info.reindex_working, "content", id).source(mapping, XContentType.JSON));
                    }
                }

                if (Sneaky.sneak(() -> contentlet.isLive())) {
                    if (mapping == null) {
                        mapping = gson.toJson(mappingAPI.toMap(contentlet));
                    }
                    if (!forReindex || info.reindex_live == null) {
                        bulk.add(new IndexRequest(info.live, "content", id).source(mapping, XContentType.JSON));
                    }
                    if (info.reindex_live != null) {
                        bulk.add(new IndexRequest(info.reindex_live, "content", id).source(mapping, XContentType.JSON));
                    }
                }

                contentlet.markAsReindexed();
            } catch (Exception ex) {
                Logger.error(this, "Can't get a mapping for contentlet with id_lang:" + id + " Content data: " + contentlet.getMap());
                throw ex;
            }
        }
        return bulk;
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
                reindexDependenciesForDeletedContent(contentlet, relationships, bulk, indexPolicyDependencies);
            }

            bulk.add(client.prepareDelete(info.working, "content", id));
            if (info.reindex_working != null) {
                bulk.add(client.prepareDelete(info.reindex_working, "content", id));
            }
        }

        bulk.execute().actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
    }

    private void reindexDependenciesForDeletedContent(final Contentlet contentlet, final List<Relationship> relationships,
            final BulkRequestBuilder bulk, final IndexPolicy indexPolicy)
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
                    putToIndex(appendBulkRequest(bulk, related));
                    break;
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

    public void removeContentFromIndexByStructureInode(String structureInode) throws DotDataException {
        String structureName = CacheLocator.getContentTypeCache().getStructureByInode(structureInode).getVelocityVarName();
        IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();

        // collecting indexes
        List<String> idxs = new ArrayList<String>();
        idxs.add(info.working);
        idxs.add(info.live);
        if (info.reindex_working != null)
            idxs.add(info.reindex_working);
        if (info.reindex_live != null)
            idxs.add(info.reindex_live);
        String[] idxsArr = new String[idxs.size()];
        idxsArr = idxs.toArray(idxsArr);

        BulkByScrollResponse response = DeleteByQueryAction.INSTANCE.newRequestBuilder(new ESClient().getClient())
                .filter(QueryBuilders.queryStringQuery("+structurename:" + structureName)).source(idxsArr).get();

        Logger.debug(this, "Records deleted: " + response.getDeleted());
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
