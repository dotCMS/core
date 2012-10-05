package com.dotcms.content.elasticsearch.business;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.status.IndexStatus;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.index.query.QueryBuilders;

import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class ESContentletIndexAPI implements ContentletIndexAPI{
	private static final ESIndexAPI iapi  = new ESIndexAPI();
    private static final ESMappingAPIImpl mappingAPI = new ESMappingAPIImpl();

    public static final SimpleDateFormat timestampFormatter=new SimpleDateFormat("yyyyMMddHHmmss");

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




	public synchronized boolean createContentIndex(String indexName) throws ElasticSearchException, IOException {
		return createContentIndex(indexName, 0);
	}
	@Override
	public synchronized boolean createContentIndex(String indexName, int shards) throws ElasticSearchException, IOException {

		CreateIndexResponse cir = iapi.createIndex(indexName, null, shards);
		int i = 0;
		while(!cir.acknowledged()){

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if(i++ > 300){
				throw new ElasticSearchException("index timed out creating");
			}
		}


		ClassLoader classLoader = null;
		URL url = null;
		classLoader = Thread.currentThread().getContextClassLoader();
		url = classLoader.getResource("es-content-mapping.json");
        // create actual index
		String mapping = new String(com.liferay.util.FileUtil.getBytes(new File(url.getPath())));

		mappingAPI.putMapping(indexName, "content", mapping);



		return true;
	}

	/**
	 * Creates new indexes /working_TIMESTAMP (aliases working_read, working_write and workinglive)
	 * and /live_TIMESTAMP with (aliases live_read, live_write, workinglive)
	 *
	 * @return the timestamp string used as suffix for indices
	 * @throws ElasticSearchException if Murphy comes arround
	 * @throws DotDataException
	 */
	private synchronized String initIndex() throws ElasticSearchException, DotDataException {
	    if(indexReady()) return "";
		try {
		    final String timeStamp=timestampFormatter.format(new Date());

		    final String workingIndex=ES_WORKING_INDEX_NAME+"_"+timeStamp;
		    final String liveIndex=ES_LIVE_INDEX_NAME+ "_" + timeStamp;

            final IndicesAdminClient iac = new ESClient().getClient().admin().indices();

            createContentIndex(workingIndex,0);
            createContentIndex(liveIndex,0);

            IndiciesInfo info=new IndiciesInfo();
            info.working=workingIndex;
            info.live=liveIndex;
            APILocator.getIndiciesAPI().point(info);

            return timeStamp;
		} catch (Exception e) {
			throw new ElasticSearchException(e.getMessage(), e);
		}

	}

	/**
	 * creates new working and live indexes with reading aliases pointing to old index
	 * and write aliases pointing to both old and new indexes
	 * @return the timestamp string used as suffix for indices
	 * @throws DotDataException
	 * @throws ElasticSearchException
	 */
	public synchronized String setUpFullReindex() throws ElasticSearchException, DotDataException {
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

                iapi.moveIndexToLocalNode(workingIndex);
                iapi.moveIndexToLocalNode(liveIndex);

                return timeStamp;
            } catch (Exception e) {
                throw new ElasticSearchException(e.getMessage(), e);
            }
	    }
	    else
	        return initIndex();
	}
	
	public boolean isInFullReindex() throws DotDataException {
	    return isInFullReindex(DbConnectionFactory.getConnection());
	}
	
	public boolean isInFullReindex(Connection conn) throws DotDataException {
	    IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies(conn);
	    return info.reindex_working!=null && info.reindex_live!=null;
	}

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

            iapi.moveIndexBackToCluster(newinfo.working);
            iapi.moveIndexBackToCluster(newinfo.live);

            ArrayList<String> list=new ArrayList<String>();
            list.add(newinfo.working);
            list.add(newinfo.live);
            optimize(list);

	    } catch (Exception e) {
            throw new ElasticSearchException(e.getMessage(), e);
        }
	}

	public boolean delete(String indexName) {
		return iapi.delete(indexName);
	}

	public boolean optimize(List<String> indexNames) {
		return iapi.optimize(indexNames);
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

	public void addContentToIndex(final Contentlet content, final boolean deps, boolean indexBeforeCommit, final boolean reindexOnly, final BulkRequestBuilder bulk) throws DotHibernateException {

	    if(content==null || !UtilMethods.isSet(content.getIdentifier())) return;

	    Runnable indexAction=new Runnable() {
            public void run() {
                try {
                    Client client=new ESClient().getClient();
                    BulkRequestBuilder req = (bulk==null) ? client.prepareBulk() : bulk;

                    // http://jira.dotmarketing.net/browse/DOTCMS-6886
                    // check for related content to reindex
                    List<Contentlet> contentToIndex=new ArrayList<Contentlet>();
                    contentToIndex.add(content);
                    if(deps)
                        contentToIndex.addAll(loadDeps(content));

                    indexContentletList(req, contentToIndex,reindexOnly);

                    if(bulk==null && req.numberOfActions()>0)
                        req.execute().actionGet();

                } catch (Exception e) {
                    Logger.error(ESContentFactoryImpl.class, e.getMessage(), e);
                }
            }
        };

	    if(bulk!=null || indexBeforeCommit) {
	        indexAction.run();
	    }
	    else {
            // add a commit listener to index the contentlet if the entire
            // transaction finish clean
            HibernateUtil.addCommitListener(indexAction);
	    }
	}

	private void indexContentletList(BulkRequestBuilder req, List<Contentlet> contentToIndex, boolean reindexOnly) throws DotStateException, DotDataException, DotSecurityException, DotMappingException {

		for(Contentlet con : contentToIndex) {
            String id=con.getIdentifier()+"_"+con.getLanguageId();
            IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
            String mapping=null;

            if(con.isWorking()) {
                mapping=mappingAPI.toJson(con);
                if(!reindexOnly)
                    req.add(new IndexRequest(info.working, "content", id)
                                .source(mapping));
                if(info.reindex_working!=null)
                    req.add(new IndexRequest(info.reindex_working, "content", id)
                                .source(mapping));
            }

            if(con.isLive()) {
                if(mapping==null)
                    mapping=mappingAPI.toJson(con);
                if(!reindexOnly)
                    req.add(new IndexRequest(info.live, "content", id)
                            .source(mapping));
                if(info.reindex_live!=null)
                    req.add(new IndexRequest(info.reindex_live, "content", id)
                            .source(mapping));
            }
        }
	}

	@SuppressWarnings("unchecked")
	private List<Contentlet> loadDeps(Contentlet content) throws DotDataException, DotSecurityException {
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

	public void removeContentFromIndex(final Contentlet content, final boolean onlyLive) throws DotHibernateException {

	    if(content==null || !UtilMethods.isSet(content.getIdentifier())) return;

	    // add a commit listener to index the contentlet if the entire
        // transaction finish clean
        HibernateUtil.addCommitListener(new Runnable() {
            public void run() {
        	    try {
            	    String id=content.getIdentifier()+"_"+content.getLanguageId();
            	    Client client=new ESClient().getClient();
            	    BulkRequestBuilder bulk=client.prepareBulk();
            	    IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();

            	    bulk.add(client.prepareDelete(info.live, "content", id));
            	    if(info.reindex_live!=null)
            	        bulk.add(client.prepareDelete(info.reindex_live, "content", id));

        	        if(!onlyLive) {

        	            // here we search for relationship fields pointing to this
        	            // content to be deleted. Those contentlets are reindexed
        	            // to avoid left those fields making noise in the index
        	            List<Relationship> relationships = RelationshipFactory.getAllRelationshipsByStructure(content.getStructure());
        	            for(Relationship rel : relationships) {
        	                String q = "";
        	                boolean isSameStructRelationship = rel.getParentStructureInode().equalsIgnoreCase(rel.getChildStructureInode());

        	                if(isSameStructRelationship)
        	                    q = "+type:content +(" + rel.getRelationTypeValue() + "-parent:" + content.getIdentifier() + " " +
        	                        rel.getRelationTypeValue() + "-child:" + content.getIdentifier() + ") ";
        	                else
        	                    q = "+type:content +" + rel.getRelationTypeValue() + ":" + content.getIdentifier();

        	                List<Contentlet> related = APILocator.getContentletAPI().search(q, -1, 0, null, APILocator.getUserAPI().getSystemUser(), false);
        	                indexContentletList(bulk, related, false);
        	            }

        	            bulk.add(client.prepareDelete(info.working, "content", id));
        	            if(info.reindex_working!=null)
        	                bulk.add(client.prepareDelete(info.reindex_working, "content", id));
        	        }

                    bulk.execute().actionGet();

        	    }
        	    catch(Exception ex) {
        	        throw new ElasticSearchException(ex.getMessage(),ex);
        	    }
            }
        });
	}

	public void removeContentFromLiveIndex(final Contentlet content) throws DotHibernateException {
        removeContentFromIndex(content, true);
    }

	public void removeContentFromIndexByStructureInode(String structureInode) throws DotDataException {
	    String structureName=StructureCache.getStructureByInode(structureInode).getVelocityVarName();
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

	    // deleting those with the specified structure inode
	    new ESClient().getClient().prepareDeleteByQuery()
              .setIndices(idxsArr)
              .setQuery(QueryBuilders.queryString("+structurename:"+structureName))
              .execute().actionGet();
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

            iapi.moveIndexBackToCluster(rew);
            iapi.moveIndexBackToCluster(rel);

        } catch (Exception e) {
            throw new ElasticSearchException(e.getMessage(), e);
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
        Map<String,IndexStatus> indices=APILocator.getESIndexAPI().getIndicesAndStatus();
        List<String> indexNames=new ArrayList<String>();

        for(String idx : indices.keySet())
            if(isDotCMSIndexName(idx))
                indexNames.add(idx);

        List<String> existingIndex=new ArrayList<String>();
        for(String idx : indexNames)
            if(client.admin().indices().exists(new IndicesExistsRequest(idx)).actionGet().exists())
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
            iapi.moveIndexBackToCluster(info.reindex_working);
            newinfo.reindex_working=null;
        }
        else if(indexName.equals(info.reindex_live)) {
            iapi.moveIndexBackToCluster(info.reindex_live);
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
