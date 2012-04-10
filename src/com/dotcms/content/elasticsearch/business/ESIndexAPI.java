package com.dotcms.content.elasticsearch.business;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

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
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.tools.zip.ZipEntry;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterIndexHealth;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.optimize.OptimizeRequest;
import org.elasticsearch.action.admin.indices.optimize.OptimizeResponse;
import org.elasticsearch.action.admin.indices.settings.UpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.UpdateSettingsRequestBuilder;
import org.elasticsearch.action.admin.indices.settings.UpdateSettingsResponse;
import org.elasticsearch.action.admin.indices.status.IndexStatus;
import org.elasticsearch.action.admin.indices.status.IndicesStatusRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
public class ESIndexAPI {
	private  final String MAPPING_MARKER = "mapping=";

    private  final String JSON_RECORD_DELIMITER = "---+||+-+-";
    private static final ESMappingAPIImpl mappingAPI = new ESMappingAPIImpl();
	private  ESClient esclient = new ESClient();
	private  ESContentletIndexAPI iapi = new ESContentletIndexAPI();
	
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
			
			// getting mapping for "content"
	        final String mapping = mappingAPI.getMapping(index, "content");
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
			throw new ElasticSearchException(e.getMessage());
		}
	}
	
	public boolean delete(String indexName) {
		try {
			IndicesAdminClient iac = new ESClient().getClient().admin().indices();
			DeleteIndexRequest req = new DeleteIndexRequest(indexName);
			DeleteIndexResponse res = iac.delete(req).actionGet();
			return res.acknowledged();
		} catch (Exception e) {
			throw new ElasticSearchException(e.getMessage());
		}
	}
	/**
	 * Restores an index from a backup file
	 * @param backupFile
	 * @param index
	 * @throws IOException
	 */
	public  void restoreIndex(File backupFile, String index) throws IOException {
		BufferedReader br = null;

		boolean indexExists = indexExists(index);

		Client client = new ESClient().getClient();
		
		try {
			if (!indexExists) {
				final IndicesAdminClient iac = new ESClient().getClient().admin().indices();

				createIndex(index);
			}

			ZipInputStream zipIn=new ZipInputStream(new FileInputStream(backupFile));
			zipIn.getNextEntry();
			br = new BufferedReader(
			        new InputStreamReader(zipIn),500000);
			
			// setting number_of_replicas=0 to improve the indexing while restoring
			// also we restrict the index to the current server
			moveIndexToLocalNode(index);
			
			// wait a bit for the changes be made
			Thread.sleep(1000L);
			
			// setting up mapping
			String mapping=br.readLine();
			boolean mappingExists=mapping.startsWith(MAPPING_MARKER);
			if(mappingExists) {
    			
			}
			
			// reading content
			ArrayList<String> jsons = new ArrayList<String>();
			
			// we recover the line that wasn't a mapping so it should be content
			if(!mappingExists)
			    jsons.add(mapping);
			
			for (int x = 0; x < 10000000; x++) {
				for (int i = 0; i < 100; i++) 
					while (br.ready()) 
						jsons.add(br.readLine());
						
				if (jsons.size() > 0) {
				    try {
    				    BulkRequestBuilder req = client.prepareBulk();
    				    for (String raw : jsons) {
    					    int delimidx=raw.indexOf(JSON_RECORD_DELIMITER);
    					    if(delimidx>0) {
        						String id = raw.substring(0, delimidx);
        						String json = raw.substring(delimidx + JSON_RECORD_DELIMITER.length(), raw.length());
        						if (id != null)
        						    req.add(new IndexRequest(index, "content", id).source(json));
    					    }
    					}
    				    if(req.numberOfActions()>0) {
    				        req.execute().actionGet();
    				        //client.admin().indices().flush(new FlushRequest(index)).actionGet();
    				    }
				    }
				    finally {
				        jsons.clear();
				    }
				} else {
					break;
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
		return listIndices().contains(indexName);
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
	 * @throws DotStateException
	 * @throws IOException
	 */
	public  void  createIndex(String indexName, int shards) throws DotStateException, IOException{

		createIndex(indexName, null, shards);
	}
	
	
	/**
	 * deletes and recreates an index
	 * @param indexName
	 * @throws DotStateException
	 * @throws IOException
	 */
	public  void clearIndex(String indexName) throws DotStateException, IOException{
		if(indexName == null || !indexExists(indexName)){
			throw new DotStateException("Index" + indexName + " does not exist");
		}
		Map<String, ClusterIndexHealth> map = getClusterHealth();
		ClusterIndexHealth cih = map.get(indexName);
		int shards = cih.getNumberOfShards();
		int replicas = cih.getNumberOfReplicas();
		
		iapi.delete(indexName);
		createIndex(indexName, shards);
	}
	
	/**
	 * unclusters an index, including changing the routing to all local
	 * @param index
	 * @throws IOException
	 */
	public  void moveIndexToLocalNode(String index) throws IOException {
        Client client=new ESClient().getClient();
        String nodeName="dotCMS_" + Config.getStringProperty("DIST_INDEXATION_SERVER_ID");
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
     * @throws ElasticSearchException
     * @throws IOException
     */
	public synchronized CreateIndexResponse createIndex(String indexName, String settings, int shards) throws ElasticSearchException, IOException {
		
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
			
	
        // create actual index
		CreateIndexRequestBuilder cirb = iac.prepareCreate(indexName).setSettings(map);

		return cirb.execute().actionGet();
		
	}
	
	
	
	
	
	public synchronized CreateIndexResponse createIndex(String indexName, String settings, int shards, String type, String mapping) throws ElasticSearchException, IOException {
		
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
    }
    
    
    
    public void putToIndex(String idx, String json, String id){
	   try{
		   Client client=new ESClient().getClient();
		   
		   IndexResponse response = client.prepareIndex(idx, "dot_site_search", id)
			        .setSource(json)
			        .execute()
			        .actionGet();
		   
		} catch (Exception e) {
		    Logger.error(ESIndexAPI.class, e.getMessage(), e);
		
		
		}

    }
    
    
    
    
}
