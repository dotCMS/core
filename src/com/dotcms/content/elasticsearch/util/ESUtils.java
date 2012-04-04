package com.dotcms.content.elasticsearch.util;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.zip.ZipEntry;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.health.ClusterIndexHealth;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.settings.UpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.UpdateSettingsResponse;
import org.elasticsearch.action.admin.indices.status.IndexStatus;
import org.elasticsearch.action.admin.indices.status.IndicesStatusRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.action.bulk.BulkRequestBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;

import edu.emory.mathcs.backport.java.util.Arrays;

public class ESUtils {
	private static final String MAPPING_MARKER = "mapping=";

    private static final String JSON_RECORD_DELIMITER = "---+||+-+-";

	private static ESClient esclient = new ESClient();
	private static ESIndexAPI iapi = new ESIndexAPI();
	public static synchronized void checkAndInitialiazeIndex() {
		ESIndexAPI iapi = new ESIndexAPI();
		new ESClient().getClient(); // this will call initNode
		try {
			// if we don't have a working index, create it
			if (!iapi.indexReady())
				iapi.initIndex();
		} catch (Exception e) {
			Logger.fatal("ESUil.checkAndInitialiazeIndex", e.getMessage());

		}
	}

	// Query util methods
	private static final String[] SPECIAL_CHARS = new String[] { "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "?",
			":", "\\" };

	public static String escape(String text) {
		for (int i = SPECIAL_CHARS.length - 1; i >= 0; i--) {
			text = StringUtils.replace(text, SPECIAL_CHARS[i], "\\" + SPECIAL_CHARS[i]);
		}

		return text;
	}

	public static File backupIndex(String index) throws IOException {
		return backupIndex(index, null);
	}

	public static File backupIndex(String index, File toFile) throws IOException {

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
	        final String mapping=client.admin().cluster().state(new ClusterStateRequest())
	                      .actionGet().state().metaData().indices()
	                      .get(index).mapping("content").source().string();
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
		    Logger.error(ESUtils.class, "Can't export index",e);
			throw new IOException(e.getMessage(),e);
		} finally {
			if (bw != null) {
				bw.close();
			}
		}
	}

	public static void restoreIndex(File backupFile, String index) throws IOException {
		BufferedReader br = null;

		boolean indexExists = indexExists(index);

		Client client = new ESClient().getClient();
		
		final Semaphore completedReqs=new Semaphore(0);
        int totalReqs=0;
		
		try {
			if (!indexExists) {
				final IndicesAdminClient iac = new ESClient().getClient().admin().indices();

				iapi.createNewIndex(iac, index);
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
    			String m=mapping.substring(MAPPING_MARKER.length());
			    client.admin().indices().putMapping(
    			        new PutMappingRequest(index).type("content").source(m)
    			).get();
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
    				        totalReqs++;
    				        req.execute(new ActionListener<BulkResponse>() {
                                @Override
                                public void onFailure(Throwable ex) {
                                    Logger.warn(ESUtils.class,"filed to add some content to the index",ex);
                                    completedReqs.release();
                                }
                                @Override
                                public void onResponse(BulkResponse arg0) {
                                    completedReqs.release();
                                }
    				        });
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
			
			try {
                completedReqs.acquire(totalReqs);
            } catch (InterruptedException e) {
                Logger.warn(ESUtils.class, "got interrupted while waiting for docs to be indexed",e);
            }
			
			// back to the original configuration for number_of_replicas
			// also let it go other servers
			moveIndexBackToCluster(index);
            
            ArrayList<String> list=new ArrayList<String>();
            list.add(index);
            iapi.optimize(list);
		}
	}

	public static Set<String> listIndices() {
		Client client = esclient.getClient();
		Map<String, IndexStatus> indices = client.admin().indices().status(new IndicesStatusRequest()).actionGet().getIndices();
		return indices.keySet();
	}

	public static boolean indexExists(String indexName) {
		return listIndices().contains(indexName);
	}
	
	public static void  createIndex(String indexName) throws DotStateException, IOException{

		if(indexExists(indexName)){
			throw new DotStateException("Index" + indexName + " already exists");
		}
        final IndicesAdminClient iac = esclient.getClient().admin().indices();
        
	    CreateIndexResponse res = iapi.createNewIndex(iac, indexName);
        
    	int i = 0 ;
    	while(!  res.acknowledged() ){
    		try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				//Logger.error(ESUtils.class,e.getMessage(),e);
			}
    		if(i++ > 60){
    			throw new IOException("ES timed out creating a new index:" + indexName);
    		}
    	}
	}
		
	public static void clearIndex(String indexName) throws DotStateException, IOException{
		if(indexName == null || !indexExists(indexName)){
			throw new DotStateException("Index" + indexName + " does not exist");
		}
		iapi.delete(indexName);
		createIndex(indexName);
	}
	
	public static void moveIndexToLocalNode(String index) throws IOException {
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
    
    public static void moveIndexBackToCluster(String index) throws IOException {
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
}
