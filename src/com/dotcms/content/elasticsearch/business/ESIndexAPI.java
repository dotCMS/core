package com.dotcms.content.elasticsearch.business;

import static com.dotcms.repackage.org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.repackage.org.elasticsearch.common.collect.ImmutableOpenMap;
import com.dotcms.repackage.org.elasticsearch.common.hppc.cursors.ObjectCursor;
import com.dotmarketing.util.*;
import org.apache.tools.zip.ZipEntry;
import com.dotcms.repackage.org.elasticsearch.ElasticsearchException;
import com.dotcms.repackage.org.elasticsearch.action.ActionFuture;
import com.dotcms.repackage.org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import com.dotcms.repackage.org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import com.dotcms.repackage.org.elasticsearch.action.admin.cluster.health.ClusterIndexHealth;
import com.dotcms.repackage.org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import com.dotcms.repackage.org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import com.dotcms.repackage.org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import com.dotcms.repackage.org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import com.dotcms.repackage.org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import com.dotcms.repackage.org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import com.dotcms.repackage.org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import com.dotcms.repackage.org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import com.dotcms.repackage.org.elasticsearch.action.admin.indices.optimize.OptimizeRequest;
import com.dotcms.repackage.org.elasticsearch.action.admin.indices.optimize.OptimizeResponse;
import com.dotcms.repackage.org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import com.dotcms.repackage.org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequestBuilder;
import com.dotcms.repackage.org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsResponse;
import com.dotcms.repackage.org.elasticsearch.action.admin.indices.status.IndexStatus;
import com.dotcms.repackage.org.elasticsearch.action.admin.indices.status.IndicesStatusRequest;
import com.dotcms.repackage.org.elasticsearch.action.bulk.BulkRequestBuilder;
import com.dotcms.repackage.org.elasticsearch.action.index.IndexRequest;
import com.dotcms.repackage.org.elasticsearch.action.index.IndexResponse;
import com.dotcms.repackage.org.elasticsearch.action.search.SearchResponse;
import com.dotcms.repackage.org.elasticsearch.action.search.SearchType;
import com.dotcms.repackage.org.elasticsearch.client.AdminClient;
import com.dotcms.repackage.org.elasticsearch.client.Client;
import com.dotcms.repackage.org.elasticsearch.client.IndicesAdminClient;
import com.dotcms.repackage.org.elasticsearch.client.Requests;
import com.dotcms.repackage.org.elasticsearch.cluster.metadata.AliasMetaData;
import com.dotcms.repackage.org.elasticsearch.cluster.metadata.IndexMetaData;
import com.dotcms.repackage.org.elasticsearch.cluster.metadata.IndexMetaData.State;
import com.dotcms.repackage.org.elasticsearch.cluster.metadata.MetaData;
import com.dotcms.repackage.org.elasticsearch.common.unit.TimeValue;
import com.dotcms.repackage.org.elasticsearch.index.query.QueryBuilders;
import com.dotcms.repackage.org.elasticsearch.search.SearchHit;

import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;

public class ESIndexAPI {

    private  final String MAPPING_MARKER = "mapping=";
    private  final String JSON_RECORD_DELIMITER = "---+||+-+-";
    private static final ESMappingAPIImpl mappingAPI = new ESMappingAPIImpl();

	private  ESClient esclient = new ESClient();
	private  ESContentletIndexAPI iapi = new ESContentletIndexAPI();

	public enum Status { ACTIVE("active"), INACTIVE("inactive"), PROCESSING("processing");
		private final String status;

		Status(String status) {
			this.status = status;
		}

		public String getStatus() {
			return status;
		}
	};

    /**
     * returns all indicies and status
     * @return
     */
    public Map<String,IndexStatus> getIndicesAndStatus() {
        Client client=new ESClient().getClient();
        return client.admin().indices().status(new IndicesStatusRequest()).actionGet().getIndices();
    }

	/**
	 * Writes an index to a backup file
	 * @param index
	 * @return
	 * @throws IOException
	 */
	public  File backupIndex(String index) throws IOException {
		return backupIndex(index, null);
	}

	/**
	 * writes an index to a backup file
	 * @param index
	 * @param toFile
	 * @return
	 * @throws IOException
	 */
	public  File backupIndex(String index, File toFile) throws IOException {
		
		AdminLogger.log(this.getClass(), "backupIndex", "Trying to backup index: " + index);

	    boolean indexExists = indexExists(index);
        if (!indexExists) {
            throw new IOException("Index :" + index + " does not exist");
        }

		String date = new java.text.SimpleDateFormat("yyyy-MM-dd_hh-mm-ss").format(new Date());
		if (toFile == null) {
			toFile = new File(ConfigUtils.getBackupPath());
			if(!toFile.exists()){
				toFile.mkdirs();
			}
			toFile = new File(ConfigUtils.getBackupPath() + File.separator + index + "_" + date + ".json");
		}

		Client client = esclient.getClient();

		BufferedWriter bw = null;
		try {
		    ZipOutputStream zipOut=new ZipOutputStream(new FileOutputStream(toFile));
		    zipOut.setLevel(9);
		    zipOut.putNextEntry(new ZipEntry(toFile.getName()));

			bw = new BufferedWriter(
			        new OutputStreamWriter(zipOut), 500000); // 500K buffer

			final String type=index.startsWith("sitesearch_") ? SiteSearchAPI.ES_SITE_SEARCH_MAPPING : "content";
	        final String mapping = mappingAPI.getMapping(index, type);
	        bw.write(MAPPING_MARKER);
	        bw.write(mapping);
	        bw.newLine();

	        // setting up the search for all content
			SearchResponse scrollResp = client.prepareSearch(index).setSearchType(SearchType.SCAN).setQuery(QueryBuilders.matchAllQuery())
					.setSize(100).setScroll(TimeValue.timeValueMinutes(2)).execute().actionGet();
			while (true) {
				scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(TimeValue.timeValueMinutes(2)).execute()
						.actionGet();
				boolean hitsRead = false;
				for (SearchHit hit : scrollResp.getHits()) {
					bw.write(hit.getId());
					bw.write(JSON_RECORD_DELIMITER);
					bw.write(hit.sourceAsString());
					bw.newLine();
					hitsRead = true;
				}
				if (!hitsRead) {
					break;
				}
			}
			return toFile;
		} catch (Exception e) {
		    Logger.error(this.getClass(), "Can't export index",e);
			throw new IOException(e.getMessage(),e);
		} finally {
			if (bw != null) {
				bw.close();
			}
			AdminLogger.log(this.getClass(), "backupIndex", "Back up for index: " + index + " done.");
		}
	}

	public boolean optimize(List<String> indexNames) {
		try {
			IndicesAdminClient iac = new ESClient().getClient().admin().indices();

			OptimizeRequest req = new OptimizeRequest(indexNames.toArray(new String[indexNames.size()]));

			OptimizeResponse res = iac.optimize(req).get();

			Logger.info(this.getClass(), "Optimizing " + indexNames + " :" + res.getSuccessfulShards() + "/" + res.getTotalShards()
					+ " shards optimized");
			return true;
		} catch (Exception e) {
			throw new ElasticsearchException(e.getMessage());
		}
	}

	public boolean delete(String indexName) {
		if(indexName==null) {
			Logger.error(this.getClass(), "Failed to delete a null ES index");
			return true;
		}

		try {
            AdminLogger.log(this.getClass(), "delete", "Trying to delete index: " + indexName);

			IndicesAdminClient iac = new ESClient().getClient().admin().indices();
			DeleteIndexRequest req = new DeleteIndexRequest(indexName);
			DeleteIndexResponse res = iac.delete(req).actionGet();

            AdminLogger.log(this.getClass(), "delete", "Index: " + indexName + " deleted.");

            return res.isAcknowledged();
		} catch (Exception e) {
			throw new ElasticsearchException(e.getMessage());
		}
	}

	/**
	 * Restores an index from a backup file
	 * @param backupFile
	 * @param index
	 * @throws IOException
	 */
	public  void restoreIndex(File backupFile, String index) throws IOException {

        AdminLogger.log(this.getClass(), "restoreIndex", "Trying to restore index: " + index);

		BufferedReader br = null;

		boolean indexExists = indexExists(index);



		try {
			if (!indexExists) {
				final IndicesAdminClient iac = new ESClient().getClient().admin().indices();

				createIndex(index);
			}

			ZipInputStream zipIn=new ZipInputStream(new FileInputStream(backupFile));
			zipIn.getNextEntry();
			br = new BufferedReader(new InputStreamReader(zipIn));

			// setting number_of_replicas=0 to improve the indexing while restoring
			// also we restrict the index to the current server
			moveIndexToLocalNode(index);

			// wait a bit for the changes be made
			Thread.sleep(1000L);

			// setting up mapping
			String mapping=br.readLine();
			boolean mappingExists=mapping.startsWith(MAPPING_MARKER);
			String type="content";
			ArrayList<String> jsons = new ArrayList<String>();
			if(mappingExists) {

			    String patternStr = "^"+MAPPING_MARKER+"\\s*\\{\\s*\"(\\w+)\"";
			    Pattern pattern = Pattern.compile(patternStr);
			    Matcher matcher = pattern.matcher(mapping);
			    boolean matchFound = matcher.find();
			    if (matchFound){
			        type = matcher.group(1); 

			// we recover the line that wasn't a mapping so it should be content

			ObjectMapper mapper = new ObjectMapper();
			while(br.ready()){
				//read in 100 lines
				for (int i = 0; i < 100; i++){
					if(!br.ready()){
						break;
					}
					jsons.add(br.readLine());
				}
				
				if (jsons.size() > 0) {
				    try {
						Client client = new ESClient().getClient();
    				    BulkRequestBuilder req = client.prepareBulk();
    				    for (String raw : jsons) {
    					    int delimidx=raw.indexOf(JSON_RECORD_DELIMITER);
    					    if(delimidx>0) {
        						String id = raw.substring(0, delimidx);
        						String json = raw.substring(delimidx + JSON_RECORD_DELIMITER.length(), raw.length());
        						if (id != null){
        							@SuppressWarnings("unchecked")
									Map<String, Object> oldMap= mapper.readValue(json, HashMap.class);
        							Map<String, Object> newMap = new HashMap<String, Object>();
        							
        							for(String key : oldMap.keySet()){
        								Object val = oldMap.get(key);
        								if(val!= null && UtilMethods.isSet(val.toString())){
        									newMap.put(key, oldMap.get(key));
        								}
        							}
            						req.add(new IndexRequest(index, type, id).source(mapper.writeValueAsString(newMap)));
        						}
    					    }
    					}
    				    if(req.numberOfActions()>0) {
    				        req.execute().actionGet();
    				    }
				    }
				    finally {
				    	jsons = new ArrayList<String>();
				    }
				}
			}
		}
		}
		} catch (Exception e) {
			throw new IOException(e.getMessage(),e);
		} finally {
			if (br != null) {
				br.close();
			}

			// back to the original configuration for number_of_replicas
			// also let it go other servers
			moveIndexBackToCluster(index);

            ArrayList<String> list=new ArrayList<String>();
            list.add(index);
            iapi.optimize(list);

            AdminLogger.log(this.getClass(), "restoreIndex", "Index restored: " + index);
		}
	}

	/**
	 * List of all indicies
	 * @return
	 */
	public  Set<String> listIndices() {
		Client client = esclient.getClient();
		Map<String, IndexStatus> indices = client.admin().indices().status(new IndicesStatusRequest()).actionGet().getIndices();
		return indices.keySet();
	}

	/**
	 *
	 * @param indexName
	 * @return
	 */
	public  boolean indexExists(String indexName) {
		return listIndices().contains(indexName.toLowerCase());
	}

	/**
	 * Creates an index with default settings
	 * @param indexName
	 * @throws DotStateException
	 * @throws IOException
	 */
	public  void  createIndex(String indexName) throws DotStateException, IOException{

		createIndex(indexName, null, 0);
	}

	/**
	 * Creates an index with default settings. If shards<1 then shards will be default
	 * @param indexName
	 * @param shards
	 * @return
	 * @throws DotStateException
	 * @throws IOException
	 */
	public  CreateIndexResponse  createIndex(String indexName, int shards) throws DotStateException, IOException{

		return createIndex(indexName, null, shards);
	}

	/**
	 * deletes and recreates an index
	 * @param indexName
	 * @throws DotStateException
	 * @throws IOException
	 * @throws DotDataException
	 */
	public  void clearIndex(String indexName) throws DotStateException, IOException, DotDataException{
		if(indexName == null || !indexExists(indexName)){
			throw new DotStateException("Index" + indexName + " does not exist");
		}

        AdminLogger.log(this.getClass(), "clearIndex", "Trying to clear index: " + indexName);

        Map<String, ClusterIndexHealth> map = getClusterHealth();
		ClusterIndexHealth cih = map.get(indexName);
		int shards = cih.getNumberOfShards();
		int replicas = cih.getNumberOfReplicas();

		String alias=getIndexAlias(indexName);

		iapi.delete(indexName);

		if(UtilMethods.isSet(indexName) && indexName.indexOf("sitesearch") > -1) {
			APILocator.getSiteSearchAPI().createSiteSearchIndex(indexName, alias, shards);
		} else {
			CreateIndexResponse res=createIndex(indexName, shards);

			try {
				int w=0;
				while(!res.isAcknowledged() && ++w<100)
					Thread.sleep(100);
			}
			catch(InterruptedException ex) {
				Logger.warn(this, ex.getMessage(), ex);
			}
		}

		if(UtilMethods.isSet(alias)) {
		    createAlias(indexName, alias);
		}

		if(replicas > 0){
			APILocator.getESIndexAPI().updateReplicas(indexName, replicas);
		}

        AdminLogger.log(this.getClass(), "clearIndex", "Index: " + indexName + " cleared");
	}

	/**
	 * unclusters an index, including changing the routing to all local
	 * @param index
	 * @throws IOException
	 */
	public  void moveIndexToLocalNode(String index) throws IOException {
        Client client=new ESClient().getClient();
//        String nodeName="dotCMS_" + Config.getStringProperty("DIST_INDEXATION_SERVER_ID");
        String nodeName="dotCMS_" + APILocator.getServerAPI().readServerId();
        UpdateSettingsResponse resp=client.admin().indices().updateSettings(
          new UpdateSettingsRequest(index).settings(
                jsonBuilder().startObject()
                     .startObject("index")
                        .field("number_of_replicas",0)
                        .field("routing.allocation.include._name",nodeName)
                     .endObject()
               .endObject().string()
        )).actionGet();
    }

	/**
	 * clusters an index, including changing the routing
	 * @param index
	 * @throws IOException
	 */
    public  void moveIndexBackToCluster(String index) throws IOException {
        Client client=new ESClient().getClient();
        int nreplicas=Config.getIntProperty("es.index.number_of_replicas",0);
        UpdateSettingsResponse resp=client.admin().indices().updateSettings(
          new UpdateSettingsRequest(index).settings(
                jsonBuilder().startObject()
                     .startObject("index")
                        .field("number_of_replicas",nreplicas)
                        .field("routing.allocation.include._name","*")
                     .endObject()
               .endObject().string()
        )).actionGet();
    }

    /**
     * Creates a new index.  If settings is null, the getDefaultIndexSettings() will be applied,
     * if shards <1, then the default # of shards will be set
     * @param indexName
     * @param settings
     * @param shards
     * @return
     * @throws ElasticsearchException
     * @throws IOException
     */
	public synchronized CreateIndexResponse createIndex(String indexName, String settings, int shards) throws ElasticsearchException, IOException {

		AdminLogger.log(this.getClass(), "createIndex",
                "Trying to create index: " + indexName + " with shards: " + shards);

        IndicesAdminClient iac = new ESClient().getClient().admin().indices();

		if(shards <1){
			try{
				shards = Integer.parseInt(System.getProperty("es.index.number_of_shards"));
			}catch(Exception e){}
		}
		if(shards <1){
			try{
				shards = Config.getIntProperty("es.index.number_of_shards");
			}catch(Exception e){}
		}

		if(shards <0){
			shards=1;
		}

		//default settings, if null
		if(settings ==null){
			settings = getDefaultIndexSettings(shards);
		}
		Map map = new ObjectMapper().readValue(settings, LinkedHashMap.class);
		map.put("number_of_shards", shards);

		/*
		 If CLUSTER_AUTOWIRE AND auto_expand_replicas are false we will specify the number of replicas to use
		 */
		if ( !Config.getBooleanProperty("CLUSTER_AUTOWIRE", true) &&
				!Config.getBooleanProperty("es.index.auto_expand_replicas", false) ) {

			//Getting the number of replicas
			int replicas = Config.getIntProperty("es.index.number_of_replicas", 0);

			map.put("auto_expand_replicas", "false");
			map.put("number_of_replicas", replicas);
		} else {
			map.put("auto_expand_replicas", "0-all");
		}

        // create actual index
		CreateIndexRequestBuilder cirb = iac.prepareCreate(indexName).setSettings(map);
        CreateIndexResponse createIndexResponse = cirb.execute().actionGet();

        AdminLogger.log(this.getClass(), "createIndex",
                "Index created: " + indexName + " with shards: " + shards);

		return createIndexResponse;
	}

	public synchronized CreateIndexResponse createIndex(String indexName, String settings, int shards, String type, String mapping) throws ElasticsearchException, IOException {

		//Seems like the method is not longer used
		// but I still will add the log just in case
        AdminLogger.log(this.getClass(), "createIndex",
                "Trying to create index: " + indexName + " with shards: " + shards);

        IndicesAdminClient iac = new ESClient().getClient().admin().indices();

		if(shards <1){
			try{
				shards = Integer.parseInt(System.getProperty("es.index.number_of_shards"));
			}catch(Exception e){}
		}
		if(shards <1){
			try{
				shards = Config.getIntProperty("es.index.number_of_shards");
			}catch(Exception e){}
		}

		if(shards <0){
			shards=1;
		}

		//default settings, if null
		if(settings ==null){
			settings = getDefaultIndexSettings(shards);
		}


        // create actual index
		iac.prepareCreate(indexName).setSettings(settings).addMapping(type, mapping).execute();

        AdminLogger.log(this.getClass(), "createIndex",
                "Index created: " + indexName + " with shards: " + shards);

		return null;
	}

	/**
	 * Returns the json (String) for
	 * the defualt ES index settings
	 * @param shards
	 * @return
	 * @throws IOException
	 */
	public String getDefaultIndexSettings(int shards) throws IOException{
		return jsonBuilder().startObject()
            .startObject("index")
           	.field("number_of_shards",shards+"")
            .startObject("analysis")
             .startObject("analyzer")
              .startObject("default")
               .field("type", "Whitespace")
              .endObject()
             .endObject()
            .endObject()
           .endObject()
          .endObject().string();


	}

    /**
     * returns cluster health
     * @return
     */
    public Map<String,ClusterIndexHealth> getClusterHealth() {
        AdminClient client=new ESClient().getClient().admin();

        ClusterHealthRequest req = new ClusterHealthRequest();
        ActionFuture<ClusterHealthResponse> chr = client.cluster().health(req);

        ClusterHealthResponse res  = chr.actionGet();
        Map<String,ClusterIndexHealth> map  = res.getIndices();

        return map;
    }

	/**
	 * This method will update the number of
	 * replicas on a given index
	 * @param indexName
	 * @param replicas
	 * @throws DotDataException
	 */
    public  synchronized void updateReplicas (String indexName, int replicas) throws DotDataException {

    	AdminLogger.log(this.getClass(), "updateReplicas", "Trying to update replicas to index: " + indexName);
    	
    	Map<String,ClusterIndexHealth> idxs = getClusterHealth();
		ClusterIndexHealth health = idxs.get( indexName);
		if(health ==null){
			return;
		}
		int curReplicas = health.getNumberOfReplicas();

		if(curReplicas != replicas){


			Map newSettings = new HashMap();
	        newSettings.put("index.number_of_replicas", replicas+"");


			UpdateSettingsRequestBuilder usrb = new ESClient().getClient().admin().indices().prepareUpdateSettings(indexName);
			usrb.setSettings(newSettings);
			usrb.execute().actionGet();
		}
		
		AdminLogger.log(this.getClass(), "updateReplicas", "Replicas updated to index: " + indexName);
    }

    public void putToIndex(String idx, String json, String id){
	   try{
		   Client client=new ESClient().getClient();

		   IndexResponse response = client.prepareIndex(idx, SiteSearchAPI.ES_SITE_SEARCH_MAPPING, id)
			        .setSource(json)
			        .execute()
			        .actionGet();

		} catch (Exception e) {
		    Logger.error(ESIndexAPI.class, e.getMessage(), e);


		}

    }

    public void createAlias(String indexName, String alias) {
        try{
            // checking for existing alias
            if(getAliasToIndexMap(APILocator.getSiteSearchAPI().listIndices()).get(alias)==null) {
                Client client=new ESClient().getClient();
                IndicesAliasesRequest req=new IndicesAliasesRequest();
                req.addAlias(alias, indexName);
                client.admin().indices().aliases(req).actionGet(30000L);
            }
         } catch (Exception e) {
             Logger.error(ESIndexAPI.class, e.getMessage(), e);
             throw new RuntimeException(e);
         }
    }

    public Map<String,String> getIndexAlias(List<String> indexNames) {
        return getIndexAlias(indexNames.toArray(new String[indexNames.size()]));
    }

    public Map<String,String> getIndexAlias(String[] indexNames) {
        Map<String,String> alias=new HashMap<String,String>();
        try{
            Client client=new ESClient().getClient();
            ClusterStateRequest clusterStateRequest = Requests.clusterStateRequest()
                    .routingTable(true)
                    .nodes( true )
                    .indices( indexNames );
            MetaData md=client.admin().cluster().state(clusterStateRequest)
                                                .actionGet(30000).getState().metaData();

            for ( IndexMetaData imd : md ) {
                for ( ObjectCursor<AliasMetaData> aliasCursor : imd.aliases().values() ) {
                    alias.put( imd.index(), aliasCursor.value.alias() );
                }
            }

            return alias;
         } catch (Exception e) {
             Logger.error(ESIndexAPI.class, e.getMessage(), e);
             throw new RuntimeException(e);
         }
    }

    public String getIndexAlias(String indexName) {
        return getIndexAlias(new String[]{indexName}).get(indexName);
    }

    public Map<String,String> getAliasToIndexMap(List<String> indices) {
        Map<String,String> map=getIndexAlias(indices);
        Map<String,String> mapReverse=new HashMap<String,String>();
        for (String idx : map.keySet())
            mapReverse.put(map.get(idx), idx);
        return mapReverse;
    }

    public void closeIndex(String indexName) {
    	AdminLogger.log(this.getClass(), "closeIndex", "Trying to close index: " + indexName);
    	
        Client client=new ESClient().getClient();
        client.admin().indices().close(new CloseIndexRequest(indexName)).actionGet();
        
        AdminLogger.log(this.getClass(), "closeIndex", "Index: " + indexName + " closed");
    }

    public void openIndex(String indexName) {
    	AdminLogger.log(this.getClass(), "openIndex", "Trying to open index: " + indexName);
    	
        Client client=new ESClient().getClient();
        client.admin().indices().open(new OpenIndexRequest(indexName)).actionGet();
        
        AdminLogger.log(this.getClass(), "openIndex", "Index: " + indexName + " opened");
    }

    public List<String> getClosedIndexes() {
        Client client=new ESClient().getClient();
        ImmutableOpenMap<String,IndexMetaData> indexState=client.admin().cluster().prepareState().execute().actionGet()
                                                           .getState().getMetaData().indices();
        List<String> closeIdx=new ArrayList<String>();
        for ( ObjectCursor<String> idx : indexState.keys() ) {
            IndexMetaData idxM = indexState.get( idx.value );
            if ( idxM.getState().equals( State.CLOSE ) ) {
                closeIdx.add( idx.value );
            }
        }
        return closeIdx;
    }

    public Status getIndexStatus(String indexName) throws DotDataException {
    	List<String> currentIdx = iapi.getCurrentIndex();
		List<String> newIdx =iapi.getNewIndex();

		boolean active =currentIdx.contains(indexName);
		boolean building =newIdx.contains(indexName);

		if(active) return Status.ACTIVE;
		else if(building) return Status.PROCESSING;
		else return Status.INACTIVE;

    }
}
