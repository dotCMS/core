package com.dotcms.content.elasticsearch.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.tika.TikaUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.business.journal.DistributedJournalAPI;
import com.dotmarketing.common.db.DotConnect;
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
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.gson.Gson;
import com.liferay.util.StringPool;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.dotmarketing.util.StringUtils.builder;

public class ESContentletIndexAPI implements ContentletIndexAPI{

	private static final ThreadLocal<Set<String>> contentIndexedTrackingLocal = new ThreadLocal< >();

	private static final int    TIMEOUT_INDEX_WAIT_FOR_DEFAULT = 30000;
	private static final String TIMEOUT_INDEX_WAIT_FOR         = "TIMEOUT_INDEX_WAIT_FOR";
	private static final int    TIME_INDEX_FORCE_DEFAULT 	   = 30000;
	private static final String TIMEOUT_INDEX_FORCE      	   = "TIMEOUT_INDEX_FORCE";
	private static DistributedJournalAPI<String> journalAPI = null;
	private static final ESIndexAPI esIndexApi       = new ESIndexAPI();
    private static final ESMappingAPIImpl mappingAPI = new ESMappingAPIImpl();

    public static final SimpleDateFormat timestampFormatter=new SimpleDateFormat("yyyyMMddHHmmss");

    public DistributedJournalAPI<String> getJournalAPI () {

    	if (null == journalAPI) {

    		synchronized (this) {

				if (null == journalAPI) {

					journalAPI = APILocator.getDistributedJournalAPI();
				}
			}
		}

    	return this.journalAPI;
	}

	public synchronized void getRidOfOldIndex() throws DotDataException {
	    IndiciesInfo idxs=APILocator.getIndiciesAPI().loadIndicies();
	    if(idxs.working!=null)
	        delete(idxs.working);
	    if(idxs.live!=null)
	        delete(idxs.live);
	    if(idxs.reindex_working!=null)
	        delete(idxs.reindex_working);
	    if(idxs.reindex_live!=null)
	        delete(idxs.reindex_live);
	}

	private Set<String> getContentIndexedTracking () {

    	Set<String> set = contentIndexedTrackingLocal.get();
    	if (null == set) {

    		set = new HashSet<>();
    		contentIndexedTrackingLocal.set(set);
		}

		return set;
	}

	/**
	 * Tells if at least we have a "working_XXXXXX" index
	 * @return
	 * @throws DotDataException
	 */
	private synchronized boolean indexReady() throws DotDataException {
	   IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
	   return info.working!=null && info.live!=null;
	}



	/**
	 * Inits the indexs
	 */
	public  synchronized void checkAndInitialiazeIndex() {
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
		try{
			url 	= classLoader.getResource("es-content-settings.json");
			settings = new String(com.liferay.util.FileUtil.getBytes(new File(url.getPath())));
		}
		catch(Exception e){
			Logger.error(this.getClass(), "cannot load es-content-settings.json file, skipping", e);
		}

		url = classLoader.getResource("es-content-mapping.json");
		String mapping = new String(com.liferay.util.FileUtil.getBytes(new File(url.getPath())));

		CreateIndexResponse cir = esIndexApi.createIndex(indexName, settings, shards);


		int i = 0;
		while(!cir.isAcknowledged()){

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if(i++ > 300){
				throw new ElasticsearchException("index timed out creating");
			}
		}

		mappingAPI.putMapping(indexName, "content", mapping);



		return true;
	}

	/**
	 * Creates new indexes /working_TIMESTAMP (aliases working_read, working_write and workinglive)
	 * and /live_TIMESTAMP with (aliases live_read, live_write, workinglive)
	 *
	 * @return the timestamp string used as suffix for indices
	 * @throws ElasticsearchException if Murphy comes arround
	 * @throws DotDataException
	 */
	private synchronized String initIndex() throws ElasticsearchException, DotDataException {
	    if(indexReady()) return "";
		try {
		    final String timeStamp=timestampFormatter.format(new Date());

		    final String workingIndex=ES_WORKING_INDEX_NAME+"_"+timeStamp;
		    final String liveIndex=ES_LIVE_INDEX_NAME+ "_" + timeStamp;

            ESClient esClient = new ESClient();
            final IndicesAdminClient iac = esClient.getClient().admin().indices();

            createContentIndex(workingIndex,0);
            createContentIndex(liveIndex,0);

            IndiciesInfo info=new IndiciesInfo();
            info.working=workingIndex;
            info.live=liveIndex;
            APILocator.getIndiciesAPI().point(info);

            return timeStamp;
		} catch (Exception e) {
			throw new ElasticsearchException(e.getMessage(), e);
		}

	}

	/**
	 * creates new working and live indexes with reading aliases pointing to old index
	 * and write aliases pointing to both old and new indexes
	 * @return the timestamp string used as suffix for indices
	 * @throws DotDataException
	 * @throws ElasticsearchException
	 */
	public synchronized String setUpFullReindex() throws ElasticsearchException, DotDataException {
	    if(indexReady()) {
    	    try {

                final String timeStamp=timestampFormatter.format(new Date());

                // index names for new index
                final String workingIndex=ES_WORKING_INDEX_NAME + "_" + timeStamp;
                final String liveIndex=ES_LIVE_INDEX_NAME + "_" + timeStamp;

                final IndicesAdminClient iac = new ESClient().getClient().admin().indices();

                createContentIndex(workingIndex);
                createContentIndex(liveIndex);

                IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
                IndiciesInfo newinfo=new IndiciesInfo();
                newinfo.working=info.working;
                newinfo.live=info.live;
                newinfo.reindex_working=workingIndex;
                newinfo.reindex_live=liveIndex;
                APILocator.getIndiciesAPI().point(newinfo);

                return timeStamp;
            } catch (Exception e) {
                throw new ElasticsearchException(e.getMessage(), e);
            }
	    }
	    else
	        return initIndex();
	}

	@CloseDBIfOpened
	public boolean isInFullReindex() throws DotDataException {
	    return isInFullReindex(DbConnectionFactory.getConnection());
	}
	
	public boolean isInFullReindex(Connection conn) throws DotDataException {
	    IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies(conn);
	    return info.reindex_working!=null && info.reindex_live!=null;
	}

	@CloseDBIfOpened
	public synchronized void fullReindexSwitchover() {
	    fullReindexSwitchover(DbConnectionFactory.getConnection());
	}
	
	/**
	 * This will drop old index and will point read aliases to new index.
	 * This method should be called after call to {@link #setUpFullReindex()}
	 * @return
	 */
	public synchronized void fullReindexSwitchover(Connection conn) {
    	try {
    	    if(!isInFullReindex()) return;

            IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies(conn);

            Logger.info(this, "Executing switchover from old index ["
                   +info.working+","+info.live+"] and new index ["
                   +info.reindex_working+","+info.reindex_live+"]");

            final String oldw=info.working;
            final String oldl=info.live;

            IndiciesInfo newinfo=new IndiciesInfo();
            newinfo.working=info.reindex_working;
            newinfo.live=info.reindex_live;
            APILocator.getIndiciesAPI().point(conn,newinfo);

            esIndexApi.moveIndexBackToCluster(newinfo.working);
            esIndexApi.moveIndexBackToCluster(newinfo.live);

            ArrayList<String> list=new ArrayList<String>();
            list.add(newinfo.working);
            list.add(newinfo.live);
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

	public void addContentToIndex(final Contentlet content) throws DotHibernateException {
	    addContentToIndex(content, true);
	}

	public void addContentToIndex(final Contentlet content, final boolean deps) throws DotHibernateException {
	    addContentToIndex(content,deps,false);
	}

	public void addContentToIndex(final Contentlet content, final boolean deps, boolean indexBeforeCommit) throws DotHibernateException {
	    addContentToIndex(content,deps,indexBeforeCommit,false);
	}

	public void addContentToIndex(final Contentlet content, final boolean deps, boolean indexBeforeCommit, final boolean reindexOnly) throws DotHibernateException {
	    addContentToIndex(content,deps,indexBeforeCommit,reindexOnly,null);
	}

	@WrapInTransaction
	public void addContentToIndex(final Contentlet content,
								  final boolean includeDependencies,
								  final boolean indexBeforeCommit,
								  final boolean reindexOnly,
								  final BulkRequestBuilder bulk) throws DotHibernateException {

	    if (null == content || !UtilMethods.isSet(content.getIdentifier())) {
	    	return;
		}

		this.addContentIndexTracking (content);

        // http://jira.dotmarketing.net/browse/DOTCMS-6886
        // check for related content to reindex
		List<Contentlet> contentDependencies  = null;
		final boolean    indexIsNotDefer   	  = IndexPolicy.DEFER != content.getIndexPolicy();
        final List<Contentlet> contentToIndex = new ArrayList<>();

        contentToIndex.add(content);

		try {

			if(includeDependencies){

				final List<Contentlet> dependencies  = loadDeps(content);
				dependencies.forEach(contentlet -> contentlet.setIndexPolicy(content.getIndexPolicyDependencies()));
				if (indexIsNotDefer) {
					contentDependencies = new ArrayList<>(dependencies);
				} else {
					contentToIndex.addAll(dependencies);
				}
			}

	   		if(bulk!=null || indexBeforeCommit) {

				if (indexIsNotDefer) {

					this.handleIndexNotDefer(content, reindexOnly, bulk, contentDependencies, contentToIndex, false);
				} else {

					this.indexContentList(contentToIndex, bulk, reindexOnly);
				}
			} else {

				if (indexIsNotDefer) {

					this.handleIndexNotDefer(content, reindexOnly, bulk, contentDependencies, contentToIndex, true);
				} else {
					// add a commit listener to index the contentlet if the entire
					// transaction finish clean
					HibernateUtil.addCommitListener(content.getInode() + ReindexRunnable.Action.ADDING,
							new AddReindexRunnable(contentToIndex, ReindexRunnable.Action.ADDING, bulk, reindexOnly));
				}
			}
		} catch (DotDataException | DotSecurityException e1) {
			throw new DotHibernateException(e1.getMessage(), e1);
		}
	}

	private void addContentIndexTracking(final Contentlet content) {

		this.getContentIndexedTracking().add(content.getIdentifier());
	}

	@Override
	public boolean isContentAlreadyIndexed(final Contentlet contentlet) {

		return this.isContentAlreadyIndexed(contentlet.getIdentifier());
	}

	@Override
	public boolean isContentAlreadyIndexed(final String contentletIdentifier) {

		boolean isIndexed = false;

		if (null != contentIndexedTrackingLocal.get()) {
			isIndexed = contentIndexedTrackingLocal.get().contains(contentletIdentifier);
		}

		return isIndexed;
	}

	private void handleIndexNotDefer(final Contentlet content,
									 final boolean reindexOnly,
									 final BulkRequestBuilder bulk,
									 final List<Contentlet> contentDependencies,
									 final List<Contentlet> contentToIndex,
									 final boolean addRollBackListener) throws DotDataException {

		// we do right now the reindex of the simple contentlet without dependencies
		if (content.getIndexPolicy() == IndexPolicy.WAIT_FOR) {
			this.indexContentListWaitFor(contentToIndex, bulk, reindexOnly);
		} else {
			this.indexContentListNow(contentToIndex, bulk, reindexOnly);
		}

		if (addRollBackListener) {
			// in case the transaction failed we reindex the latest committed version
			HibernateUtil.addRollbackListener(()-> {
				try {
					this.getJournalAPI().addReindexHighPriority(content.getIdentifier());
				} catch (DotDataException e) {
					throw new RuntimeException(e);
				}
			});
		}

		// if dependencies, we add them at the end with the highest priority
		if (UtilMethods.isSet(contentDependencies)) {

			switch (content.getIndexPolicyDependencies()) {

				case WAIT_FOR:
					this.indexContentListWaitFor(contentDependencies, bulk, reindexOnly);
					break;
				case FORCE:
					this.indexContentListNow(contentDependencies, bulk, reindexOnly);
					break;
				default: // DEFER
					HibernateUtil.addCommitListener(content.getInode() + ReindexRunnable.Action.ADDING,
							new AddReindexRunnable(contentDependencies, ReindexRunnable.Action.ADDING, bulk, reindexOnly));
					break;
			}
		}
	}

	/**
	 * Add ReindexRunnable runnable
	 */
	private class AddReindexRunnable extends ReindexRunnable {

		public AddReindexRunnable(final List<Contentlet> reindexIds, final Action action, final BulkRequestBuilder bulk, final boolean reindexOnly) {
			super(reindexIds, action, bulk, reindexOnly);
		}
	}

	@Override
	public void indexContentList(final List<Contentlet> contentToIndex,
                                 final BulkRequestBuilder bulk,
                                 final boolean reindexOnly) throws  DotDataException {

    	if (contentToIndex==null || contentToIndex.size()==0) {
    		return;
    	}

		if (null == bulk) {

		    // split the list on three possible subset, one with the default refresh strategy, second one is the wait for and finally the immediate
		    final List<List<Contentlet>> partitions = CollectionsUtils.partition(contentToIndex,
					(contentlet -> contentlet.getIndexPolicy() == IndexPolicy.DEFER),
					(contentlet -> contentlet.getIndexPolicy() == IndexPolicy.WAIT_FOR),
					(contentlet -> contentlet.getIndexPolicy() == IndexPolicy.FORCE));

			if (UtilMethods.isSet(partitions.get(0))) {

				this.runIndexBulk(partitions.get(0), new ESClient().getClient().prepareBulk(), reindexOnly);
			}

			if (UtilMethods.isSet(partitions.get(1))) {

				this.indexContentListWaitFor(partitions.get(1), null, reindexOnly);
			}

			if (UtilMethods.isSet(partitions.get(2))) {

				this.indexContentListNow(partitions.get(2), null, reindexOnly);
			}
		} else {

			this.runIndexBulk(contentToIndex, bulk, reindexOnly);
		}
	}

	private void runIndexBulk(final List<Contentlet> contentToIndex,
							  final BulkRequestBuilder bulk,
							  final boolean reindexOnly) throws DotDataException {
		try {

			indexContentletList(bulk, contentToIndex, reindexOnly);
			if (bulk.numberOfActions() > 0) {
				bulk.execute().actionGet();
			}
		} catch (DotStateException | DotSecurityException | DotMappingException e) {
			throw new DotDataException(e.getMessage(), e);
		}
	}

	@Override
	public void indexContentListNow(final List<Contentlet> contentToIndex,
									final BulkRequestBuilder bulk,
									final boolean reindexOnly) throws DotDataException {

		final BulkRequestBuilder bulkRequestBuilder = (bulk==null)?
				new ESClient().getClient().prepareBulk() : bulk;

		final long timeOutMillis                    = Config
				.getLongProperty(TIMEOUT_INDEX_FORCE, TIME_INDEX_FORCE_DEFAULT);

		// we want to wait until the content is already indexed
		bulkRequestBuilder.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
		bulkRequestBuilder.setTimeout(TimeValue.timeValueMillis(timeOutMillis));
		this.runIndexBulk(contentToIndex, bulkRequestBuilder, reindexOnly);
	} // indexContentListNow.


	@Override
	public void indexContentListWaitFor(final List<Contentlet> contentToIndex,
										final BulkRequestBuilder bulk,
										final boolean reindexOnly) throws DotDataException {

		final BulkRequestBuilder bulkRequestBuilder = (bulk==null)?
				new ESClient().getClient().prepareBulk() : bulk;
		final long timeOutMillis                    = Config
				.getLongProperty(TIMEOUT_INDEX_WAIT_FOR, TIMEOUT_INDEX_WAIT_FOR_DEFAULT);

		// we want to wait until the content is already indexed
		bulkRequestBuilder.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
		bulkRequestBuilder.setTimeout(TimeValue.timeValueMillis(timeOutMillis));
		this.runIndexBulk(contentToIndex, bulkRequestBuilder, reindexOnly);
	} // indexContentListWaitFor.

	@Override
	public void indexContentListDeferred(final List<Contentlet> contentToIndex) throws DotHibernateException {

		HibernateUtil.addCommitListener(()-> {
			try {

				this.getJournalAPI().addReindexHighPriority
						(contentToIndex.stream().map(Contentlet::getIdentifier).collect(Collectors.toSet()));
			} catch (DotDataException e) {

				Logger.error(ESContentletIndexAPI.class, e.getMessage(), e);
			}
		});
	} // indexContentListDeferred.


	@Override
	public void indexContentList(final List<Contentlet> contentToIndex,
								 final BulkRequestBuilder bulk,
								 final boolean reindexOnly,
								 ActionListener<BulkResponse> listener) throws  DotDataException {

		if(contentToIndex==null || contentToIndex.size()==0){
			return;
		}

		final BulkRequestBuilder req = (bulk==null) ? new ESClient().getClient().prepareBulk() : bulk;
		try {
			indexContentletList(req, contentToIndex, reindexOnly);
			if(bulk==null && req.numberOfActions()>0) {
				req.execute(listener);
			}
		} catch (DotStateException | DotSecurityException | DotMappingException e) {
			throw new DotDataException (e.getMessage(), e);
		}
	}

	private void indexContentletList(BulkRequestBuilder req, List<Contentlet> contentToIndex, boolean reindexOnly) throws DotStateException, DotDataException, DotSecurityException, DotMappingException {

		if ( contentToIndex != null && !contentToIndex.isEmpty() ) {
			Logger.debug(this.getClass(), "Indexing " + contentToIndex.size() +
					" contents, starting with identifier [ " + contentToIndex.get(0).getMap().get("identifier") + "]");
		}

		// eliminate dups
		Set<Contentlet> contentToIndexSet = new HashSet<>(contentToIndex);

		//Verify if it is enabled the option to regenerate missing metadata files on reindex
		boolean regenerateMissingMetadata = Config
				.getBooleanProperty("regenerate.missing.metadata.on.reindex", true);
		/*
		Verify if it is enabled the option to always regenerate metadata files on reindex,
		enabling this could affect greatly the performance of a reindex process.
		 */
		boolean alwaysRegenerateMetadata = Config
				.getBooleanProperty("always.regenerate.metadata.on.reindex", false);

		for(Contentlet con : contentToIndexSet) {
            String id=con.getIdentifier()+"_"+con.getLanguageId();

            Logger.debug(this, ()->"\n*********----------- Indexing : " + Thread.currentThread().getName() + ", id: " + con.getIdentifier());
			Logger.debug(this, ()->"*********-----------  " + DbConnectionFactory.getConnection());
			Logger.debug(this, ()->"*********-----------  " + ExceptionUtil.getCurrentStackTraceAsString(Config.getIntProperty("stacktracelimit", 10)) + "\n");

            IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
            Gson gson=new Gson(); // todo why do we create a new Gson everytime
            String mapping=null;
            try {

				if (con.isLive() || con.isWorking()) {
					if (alwaysRegenerateMetadata) {
						new TikaUtils().generateMetaData(con, true);
					} else if (regenerateMissingMetadata) {
						new TikaUtils().generateMetaData(con);
					}
				}

				if (con.isWorking()) {
                    mapping=gson.toJson(mappingAPI.toMap(con));
                    
                    if(!reindexOnly)
                        req.add(new IndexRequest(info.working, "content", id)
                                    .source(mapping, XContentType.JSON));
                    if(info.reindex_working!=null)
                        req.add(new IndexRequest(info.reindex_working, "content", id)
                                    .source(mapping, XContentType.JSON));
                }
    
                if(con.isLive()) {
                    if(mapping==null)
                        mapping=gson.toJson(mappingAPI.toMap(con));
                    
                    if(!reindexOnly)
                        req.add(new IndexRequest(info.live, "content", id)
                                .source(mapping, XContentType.JSON));
                    if(info.reindex_live!=null)
                        req.add(new IndexRequest(info.reindex_live, "content", id)
                                .source(mapping, XContentType.JSON));
                }
            }
            catch(DotMappingException ex) {
				Logger.error(this, "Can't get a mapping for contentlet with id_lang:" + id + " Content data: " + con.getMap(), ex);
				throw ex;
            }
        }
		
	}

	@CloseDBIfOpened
	@SuppressWarnings("unchecked")
	public List<Contentlet> loadDeps(Contentlet content) throws DotDataException, DotSecurityException {
	    List<Contentlet> contentToIndex=new ArrayList<Contentlet>();
	    List<String> depsIdentifiers=mappingAPI.dependenciesLeftToReindex(content);
        for(String ident : depsIdentifiers) {
            // get working and live version for all languages based on the identifier
//            String sql = "select distinct inode from contentlet join contentlet_version_info " +
//                    " on (inode=live_inode or inode=working_inode) and contentlet.identifier=?";
            String sql = "select working_inode,live_inode from contentlet_version_info where identifier=?";
    	    
            DotConnect dc = new DotConnect();
            dc.setSQL(sql);
            dc.addParam(ident);
            List<Map<String,String>> ret = dc.loadResults();
            List<String> inodes = new ArrayList<String>(); 
            for(Map<String,String> m : ret) {
            	String workingInode = m.get("working_inode");
            	String liveInode = m.get("live_inode");
            	inodes.add(workingInode);
            	if(UtilMethods.isSet(liveInode) && !workingInode.equals(liveInode)){
            		inodes.add(liveInode);
            	}
            }
            
            for(String inode : inodes) {
                Contentlet con=APILocator.getContentletAPI().find(inode, APILocator.getUserAPI().getSystemUser(), false);
                contentToIndex.add(con);
            }
        }
        return contentToIndex;
	}

	public void removeContentFromIndex(final Contentlet content) throws DotHibernateException {
	    removeContentFromIndex(content, false);
	}

	private void removeContentFromIndex(final Contentlet content, final boolean onlyLive, final List<Relationship> relationships) throws DotHibernateException {

		final boolean    indexIsNotDefer   	  = IndexPolicy.DEFER != content.getIndexPolicy();

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

	private void handleRemoveIndexNotDefer(final Contentlet content,
										   final boolean onlyLive,
										   final List<Relationship> relationships)
			throws DotSecurityException, DotMappingException, DotDataException {

		removeContentAndProcessDependencies(content, relationships,
				onlyLive, content.getIndexPolicy(), content.getIndexPolicyDependencies());
	} // handleRemoveIndexNotDefer.

	/**
	 * Remove ReindexRunnable runnable
	 */
	private class RemoveReindexRunnable extends ReindexRunnable {

		private final Contentlet         contentlet;
		private final boolean            onlyLive;
		private final List<Relationship> relationships;

		public RemoveReindexRunnable(final Contentlet contentlet, final boolean onlyLive,
									 final List<Relationship> relationships) {

			super(contentlet, ReindexRunnable.Action.REMOVING, null);
			this.contentlet    = contentlet;
			this.onlyLive      = onlyLive;
			this.relationships = relationships;
		}

		public void run() {

			try {
				removeContentAndProcessDependencies(this.contentlet, this.relationships,
						this.onlyLive, IndexPolicy.DEFER, IndexPolicy.DEFER);
			} catch(Exception ex) {
				throw new ElasticsearchException(ex.getMessage(),ex);
			}
		}
	}

	private void removeContentAndProcessDependencies(final Contentlet contentlet, final List<Relationship> relationships,
													 final boolean onlyLive, final IndexPolicy indexPolicy, final IndexPolicy indexPolicyDependencies)
			throws DotDataException, DotSecurityException, DotMappingException {

		final String id         = builder(contentlet.getIdentifier(), StringPool.UNDERLINE, contentlet.getLanguageId()).toString();
		final Client client     = new ESClient().getClient();
		final IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
		final BulkRequestBuilder bulk = client.prepareBulk();


		// we want to wait until the content is already indexed
		switch (indexPolicy) {
			case FORCE:
				bulk.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
				bulk.setTimeout(TimeValue.timeValueMillis(Config
						.getLongProperty(TIMEOUT_INDEX_FORCE, TIME_INDEX_FORCE_DEFAULT)));
				break;

			case WAIT_FOR:
				bulk.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
				bulk.setTimeout(TimeValue.timeValueMillis(Config
						.getLongProperty(TIMEOUT_INDEX_WAIT_FOR, TIMEOUT_INDEX_WAIT_FOR_DEFAULT)));
				break;
		}

		bulk.add(client.prepareDelete(info.live, "content", id));

		if (info.reindex_live != null) {

			bulk.add(client.prepareDelete(info.reindex_live, "content", id));
		}

		if(!onlyLive) {

			// here we search for relationship fields pointing to this
			// content to be deleted. Those contentlets are reindexed
			// to avoid left those fields making noise in the index
			if (UtilMethods.isSet(relationships)) {
				reindexDependenciesForDeletedContent(contentlet, relationships,
						bulk, indexPolicyDependencies);
			}

			bulk.add(client.prepareDelete(info.working, "content", id));
			if(info.reindex_working!=null) {
				bulk.add(client.prepareDelete(info.reindex_working, "content", id));
			}
		}

		bulk.execute().actionGet();
	}

	private void reindexDependenciesForDeletedContent(final Contentlet contentlet,
			final List<Relationship> relationships,
			final BulkRequestBuilder bulk, final IndexPolicy indexPolicy)
			throws DotDataException, DotSecurityException, DotMappingException {

		for (final Relationship relationship : relationships) {

			final boolean isSameStructRelationship = FactoryLocator.getRelationshipFactory()
					.sameParentAndChild(relationship);

			final String query = (isSameStructRelationship) ?
					builder("+type:content +(", relationship.getRelationTypeValue(), "-parent:", contentlet.getIdentifier(), StringPool.SPACE,
							relationship.getRelationTypeValue(), "-child:", contentlet.getIdentifier(), ") ").toString() :
					builder("+type:content +", relationship.getRelationTypeValue(), ":", contentlet.getIdentifier()).toString();

			final List<Contentlet> related = APILocator.getContentletAPI().search
					(query, -1, 0, null, APILocator.getUserAPI().getSystemUser(), false);

			switch (indexPolicy) {

				case WAIT_FOR:
					indexContentListWaitFor(related, bulk, false);
					break;
				case FORCE:
					indexContentListNow(related, bulk, false);
					break;
				default: // DEFER
					indexContentletList(bulk, related, false);
					break;
			}
		}
	}

	@CloseDBIfOpened
	public void removeContentFromIndex(final Contentlet content, final boolean onlyLive) throws DotHibernateException {

	    if(content==null || !UtilMethods.isSet(content.getIdentifier())) return;

	    List<Relationship> relationships = FactoryLocator.getRelationshipFactory().byContentType(content.getStructure());

	    // add a commit listener to index the contentlet if the entire
        // transaction finish clean
        removeContentFromIndex(content, onlyLive, relationships);
       
	}

	public void removeContentFromLiveIndex(final Contentlet content) throws DotHibernateException {
        removeContentFromIndex(content, true);
    }

	public void removeContentFromIndexByStructureInode(String structureInode) throws DotDataException {
	    String structureName=CacheLocator.getContentTypeCache().getStructureByInode(structureInode).getVelocityVarName();
	    IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();

	    // collecting indexes
	    List<String> idxs=new ArrayList<String>();
	    idxs.add(info.working);
	    idxs.add(info.live);
	    if(info.reindex_working!=null)
	        idxs.add(info.reindex_working);
	    if(info.reindex_live!=null)
	        idxs.add(info.reindex_live);
	    String[] idxsArr=new String[idxs.size()];
	    idxsArr=idxs.toArray(idxsArr);

        BulkByScrollResponse response =
            DeleteByQueryAction.INSTANCE.newRequestBuilder(new ESClient().getClient())
                .filter(QueryBuilders.queryStringQuery("+structurename:" + structureName))
                .source(idxsArr)
                .get();

        Logger.debug(this, "Records deleted: " + response.getDeleted());
    }

    public void fullReindexAbort() {
        try {
            if(!isInFullReindex()) return;

            IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();

            final String rew=info.reindex_working;
            final String rel=info.reindex_live;

            IndiciesInfo newinfo=new IndiciesInfo();
            newinfo.working=info.working;
            newinfo.live=info.live;
            APILocator.getIndiciesAPI().point(newinfo);

            esIndexApi.moveIndexBackToCluster(rew);
            esIndexApi.moveIndexBackToCluster(rel);

        } catch (Exception e) {
            throw new ElasticsearchException(e.getMessage(), e);
        }
    }

    public boolean isDotCMSIndexName(String indexName) {
        return indexName.startsWith(ES_WORKING_INDEX_NAME+"_") || indexName.startsWith(ES_LIVE_INDEX_NAME+"_");
    }

    public List<String> listDotCMSClosedIndices() {
        List<String> indexNames=new ArrayList<String>();
        List<String> list=APILocator.getESIndexAPI().getClosedIndexes();
        for(String idx : list)
            if(isDotCMSIndexName(idx))
                indexNames.add(idx);
        return indexNames;
    }

    /**
     * Returns a list of dotcms working and live indices.
     * @return
     */
    public List<String> listDotCMSIndices() {
        Client client=new ESClient().getClient();
        Map<String,IndexStats> indices=APILocator.getESIndexAPI().getIndicesAndStatus();
        List<String> indexNames=new ArrayList<String>();

        for(String idx : indices.keySet())
            if(isDotCMSIndexName(idx))
                indexNames.add(idx);

        List<String> existingIndex=new ArrayList<String>();
        for(String idx : indexNames)
            if(client.admin().indices().exists(new IndicesExistsRequest(idx)).actionGet().isExists())
                existingIndex.add(idx);
        indexNames=existingIndex;

        List<String> indexes = new ArrayList<String>();
        indexes.addAll(indexNames);
        Collections.sort(indexes, new IndexSortByDate());

        return indexes;
    }

    public void activateIndex(String indexName) throws DotDataException {
        IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
        IndiciesInfo newinfo=new IndiciesInfo();
        newinfo.working=info.working;
        newinfo.live=info.live;
        newinfo.reindex_working=info.reindex_working;
        newinfo.reindex_live=info.reindex_live;
        newinfo.site_search=info.site_search;
        if(indexName.startsWith(ES_WORKING_INDEX_NAME)) {
            newinfo.working=indexName;
        }
        else if(indexName.startsWith(ES_LIVE_INDEX_NAME)) {
            newinfo.live=indexName;
        }
        APILocator.getIndiciesAPI().point(newinfo);
    }

    public void deactivateIndex(String indexName) throws DotDataException, IOException {
        IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
        IndiciesInfo newinfo=new IndiciesInfo();
        newinfo.working=info.working;
        newinfo.live=info.live;
        newinfo.reindex_working=info.reindex_working;
        newinfo.reindex_live=info.reindex_live;
        newinfo.site_search=info.site_search;
        if(indexName.equals(info.working)) {
            newinfo.working=null;
        }
        else if(indexName.equals(info.live)) {
            newinfo.live=null;
        }
        else if(indexName.equals(info.reindex_working)) {
            esIndexApi.moveIndexBackToCluster(info.reindex_working);
            newinfo.reindex_working=null;
        }
        else if(indexName.equals(info.reindex_live)) {
            esIndexApi.moveIndexBackToCluster(info.reindex_live);
            newinfo.reindex_live=null;
        }
        APILocator.getIndiciesAPI().point(newinfo);
    }



    public synchronized List<String> getCurrentIndex() throws DotDataException {
        List<String> newIdx = new ArrayList<String>();
        IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
        newIdx.add(info.working);
        newIdx.add(info.live);
        return newIdx;
    }

    public synchronized List<String> getNewIndex() throws DotDataException {
        List<String> newIdx = new ArrayList<String>();
        IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
        if(info.reindex_working!=null)
            newIdx.add(info.reindex_working);
        if(info.reindex_live!=null)
            newIdx.add(info.reindex_live);
        return newIdx;
    }

    private class IndexSortByDate implements Comparator<String> {
        public int compare(String o1, String o2) {
            if(o1 == null || o2==null ){
                return 0;
            }
            if(o1.indexOf("_") <0 ){
                return 1;
            }
            if(o2.indexOf("_") <0 ){
                return -1;
            }
            String one = o1.split("_")[1];
            String two = o2.split("_")[1];
            return two.compareTo(one);
        }
    }

    public String getActiveIndexName(String type) throws DotDataException {
        IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
        if(type.equalsIgnoreCase(ES_WORKING_INDEX_NAME)) {
           return info.working;
        }
        else if(type.equalsIgnoreCase(ES_LIVE_INDEX_NAME)) {
           return info.live;
        }
        return null;
    }

}
