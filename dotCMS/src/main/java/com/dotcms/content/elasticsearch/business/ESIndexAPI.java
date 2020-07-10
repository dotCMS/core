package com.dotcms.content.elasticsearch.business;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.dotcms.cluster.ClusterUtils;
import com.dotcms.cluster.business.ClusterAPI;
import com.dotcms.cluster.business.ReplicasMode;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.dts.spell.utils.FileUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.ZipUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.zip.ZipEntry;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.repositories.delete.DeleteRepositoryResponse;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryResponse;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotResponse;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsRequest;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotResponse;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequestBuilder;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheResponse;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeRequest;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeResponse;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequestBuilder;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.health.ClusterIndexHealth;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.IndexMetaData.State;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.metadata.RepositoryMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.snapshots.SnapshotInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.dotcms.util.DotPreconditions.checkArgument;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class ESIndexAPI {

    private  final String MAPPING_MARKER = "mapping=";
    private  final String JSON_RECORD_DELIMITER = "---+||+-+-";
    private static final ESMappingAPIImpl mappingAPI = new ESMappingAPIImpl();

    public static final String BACKUP_REPOSITORY = "backup";
    private final String REPOSITORY_PATH = "path.repo";

	public static final int INDEX_OPERATIONS_TIMEOUT_IN_MS =
			Config.getIntProperty("ES_INDEX_OPERATIONS_TIMEOUT", 15000);

	final private ESClient esclient;
	final private ContentletIndexAPI iapi;
	final private ESIndexHelper esIndexHelper;
	private final ServerAPI serverAPI;
	private final ClusterAPI clusterAPI;

	public enum Status { ACTIVE("active"), INACTIVE("inactive"), PROCESSING("processing");
		private final String status;

		Status(String status) {
			this.status = status;
		}

		public String getStatus() {
			return status;
		}
	}

	public ESIndexAPI(){
		this.esclient = new ESClient();
		this.iapi = new ContentletIndexAPIImpl();
		this.esIndexHelper = ESIndexHelper.INSTANCE;
		this.serverAPI = APILocator.getServerAPI();
		this.clusterAPI = APILocator.getClusterAPI();
	}

	@VisibleForTesting
	protected ESIndexAPI(final ESClient esclient, final ContentletIndexAPIImpl iapi, final ESIndexHelper esIndexHelper,
						 final ServerAPI serverAPI, final ClusterAPI clusterAPI){
		this.esclient = esclient;
		this.iapi = iapi;
		this.esIndexHelper = esIndexHelper;
		this.serverAPI = serverAPI;
		this.clusterAPI = clusterAPI;
	}

    /**
     * returns all indicies and status
     * @return
     */
    public Map<String,IndexStats> getIndicesAndStatus() {
        final Client client = esclient.getClient();
        final IndicesStatsResponse
            indicesStatsResponse =
                client.admin().indices().prepareStats().setStore(true).execute().actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);

        return indicesStatsResponse.getIndices();
    }

    
    
    /**
     * This method will flush ElasticSearches field and filter 
     * caches.  The operation can take up to a minute to complete
     * 
     * @param indexNames
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public Map<String, Integer> flushCaches(final List<String> indexNames) throws InterruptedException, ExecutionException {
      Logger.warn(this.getClass(), "Flushing Elasticsearch index caches:" + indexNames);
      if(indexNames==null || indexNames.isEmpty()) {
        return ImmutableMap.of("failedShards",0, "successfulShards", 0);
      }
      final Client client = esclient.getClient();
      final ClearIndicesCacheRequestBuilder requestBuilder =
          client.admin().indices().prepareClearCache(indexNames.toArray(new String[indexNames.size()]));

      final ClearIndicesCacheResponse res = esclient.getClient().admin().indices().clearCache(requestBuilder.request()).get();
      Map<String, Integer> map= ImmutableMap.of("failedShards",res.getFailedShards(), "successfulShards", res.getSuccessfulShards());
 
      Logger.warn(this.getClass(), "Flushed Elasticsearch index caches:" + map);
      return map;
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

		BufferedWriter bw;
		try (final ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(toFile.toPath()))){
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
            SearchResponse scrollResp =
                client
                    .prepareSearch(index)
                    .setQuery(QueryBuilders.matchAllQuery())
                    .setSize(100)
                    .setScroll(TimeValue.timeValueMinutes(2))
                    //_doc has no real use-case besides being the most efficient sort order.
                    .addSort("_doc", SortOrder.DESC)
                    .execute()
                    .actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);

			while (true) {
				scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(TimeValue.timeValueMinutes(2)).execute()
						.actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
				boolean hitsRead = false;
				for (SearchHit hit : scrollResp.getHits()) {
					bw.write(hit.getId());
					bw.write(JSON_RECORD_DELIMITER);
					bw.write(hit.getSourceAsString());
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
			AdminLogger.log(this.getClass(), "backupIndex", "Back up for index: " + index + " done.");
		}
	}

	public boolean optimize(List<String> indexNames) {
		try {
            final ForceMergeRequest forceMergeRequest = new ForceMergeRequest(indexNames.toArray(new String[indexNames.size()]));
            final ForceMergeResponse forceMergeResponse =
                esclient.getClient().admin().indices().forceMerge(forceMergeRequest).get();

            Logger.info(this.getClass(),
                "Optimizing " + indexNames + " :" + forceMergeResponse.getSuccessfulShards()
                    + "/" + forceMergeResponse.getTotalShards() + " shards optimized");

			return true;
		} catch (Exception e) {
			throw new ElasticsearchException(e.getMessage(),e);
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
			DeleteIndexResponse res = iac.delete(req).actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);

            AdminLogger.log(this.getClass(), "delete", "Index: " + indexName + " deleted.");

            return res.isAcknowledged();
		} catch (Exception e) {
			throw new ElasticsearchException(e.getMessage(),e);
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

                createIndex(index);
            }

            final ZipInputStream zipIn = new ZipInputStream(
                    Files.newInputStream(backupFile.toPath()));
            zipIn.getNextEntry();
            br = new BufferedReader(new InputStreamReader(zipIn));

            // setting number_of_replicas=0 to improve the indexing while restoring
            // also we restrict the index to the current server
            moveIndexToLocalNode(index);

            // wait a bit for the changes be made
            Thread.sleep(1000L);

            // setting up mapping
            String mapping = br.readLine();
            boolean mappingExists = mapping.startsWith(MAPPING_MARKER);
            String type = "content";
            ArrayList<String> jsons = new ArrayList<String>();
            if (mappingExists) {

                String patternStr = "^" + MAPPING_MARKER + "\\s*\\{\\s*\"(\\w+)\"";
                Pattern pattern = Pattern.compile(patternStr);
                Matcher matcher = pattern.matcher(mapping);
                boolean matchFound = matcher.find();
                if (matchFound) {
                    type = matcher.group(1);

                    // we recover the line that wasn't a mapping so it should be content

                    ObjectMapper mapper = new ObjectMapper();
                    while (br.ready()) {
                        //read in 100 lines
                        for (int i = 0; i < 100; i++) {
                            if (!br.ready()) {
                                break;
                            }
                            jsons.add(br.readLine());
                        }

                        if (jsons.size() > 0) {
                            try {
                                Client client = new ESClient().getClient();
                                BulkRequestBuilder req = client.prepareBulk();
                                for (String raw : jsons) {
                                    int delimidx = raw.indexOf(JSON_RECORD_DELIMITER);
                                    if (delimidx > 0) {
                                        String id = raw.substring(0, delimidx);
                                        String json = raw.substring(
                                                delimidx + JSON_RECORD_DELIMITER.length(),
                                                raw.length());
                                        if (id != null) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> oldMap = mapper
                                                    .readValue(json, HashMap.class);
                                            Map<String, Object> newMap = new HashMap<String, Object>();

                                            for (String key : oldMap.keySet()) {
                                                Object val = oldMap.get(key);
                                                if (val != null && UtilMethods
                                                        .isSet(val.toString())) {
                                                    newMap.put(key, oldMap.get(key));
                                                }
                                            }
                                            req.add(new IndexRequest(index, type, id)
                                                    .source(mapper.writeValueAsString(newMap)));
                                        }
                                    }
                                }
                                if (req.numberOfActions() > 0) {
                                    req.execute().actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
                                }
                            } finally {
                                jsons = new ArrayList<String>();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            if (br != null) {
                br.close();
            }

            // back to the original configuration for number_of_replicas
            // also let it go other servers
            moveIndexBackToCluster(index);

            final List<String> list = new ArrayList<>();
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
		return getIndicesAndStatus().keySet();
	}

	/**
	 * Returns close status of an index
	 * @return
	 */
	public boolean isIndexClosed(String index) {
		Client client = esclient.getClient();
		ImmutableOpenMap<String,IndexMetaData> indices = client.admin().cluster()
			    .prepareState().get().getState()
			    .getMetaData().getIndices();
		IndexMetaData indexMetaData = indices.get(index);
		if(indexMetaData != null)
			return indexMetaData.getState() == State.CLOSE;
		return true;
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
	public  void clearIndex(final String indexName) throws DotStateException, IOException, DotDataException{
		if(indexName == null || !indexExists(indexName)){
			throw new DotStateException("Index" + indexName + " does not exist");
		}

        AdminLogger.log(this.getClass(), "clearIndex", "Trying to clear index: " + indexName);

		final ClusterIndexHealth cih = getClusterHealth().get(indexName);
		final int shards = cih.getNumberOfShards();
		final String alias=getIndexAlias(indexName);

		iapi.delete(indexName);

		if(UtilMethods.isSet(indexName) && indexName.indexOf("sitesearch") > -1) {
			APILocator.getSiteSearchAPI().createSiteSearchIndex(indexName, alias, shards);
		} else {
			final CreateIndexResponse res=createIndex(indexName, shards);

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

        AdminLogger.log(this.getClass(), "clearIndex", "Index: " + indexName + " cleared");
	}

	/**
	 * unclusters an index, including changing the routing to all local
	 * @param index
	 * @throws IOException
	 */
	public  void moveIndexToLocalNode(final String index) throws IOException {
        final Client client=new ESClient().getClient();
        final String nodeName="dotCMS_" + APILocator.getServerAPI().readServerId();

		client.admin().indices().updateSettings(
          new UpdateSettingsRequest(index).settings(
                jsonBuilder().startObject()
                     .startObject("index")
                        .field("number_of_replicas",0)
                        .field("routing.allocation.include._name",nodeName)
                     .endObject()
               .endObject().string(), XContentType.JSON)).actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
    }

	/**
	 * clusters an index, including changing the routing
	 * @param index
	 * @throws IOException
	 */
    public void moveIndexBackToCluster(final String index) throws IOException {
        final Client client=new ESClient().getClient();
        final ReplicasMode replicasMode = clusterAPI.getReplicasMode();

		final XContentBuilder builder = jsonBuilder().startObject().startObject("index");

		if(replicasMode.getNumberOfReplicas()>-1) {
			builder.field("number_of_replicas", replicasMode.getNumberOfReplicas());
		}
		builder.field("auto_expand_replicas",replicasMode.getAutoExpandReplicas());

        client.admin().indices().updateSettings(
          new UpdateSettingsRequest(index).settings(builder
				  .field("routing.allocation.include._name","*").endObject()
				  .endObject().string(), XContentType.JSON)).actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
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
	public synchronized CreateIndexResponse createIndex(final String indexName, String settings,
			int shards) throws ElasticsearchException, IOException {

		final ReplicasMode replicasMode = clusterAPI.getReplicasMode();

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
				shards = Config.getIntProperty("es.index.number_of_shards", 2);
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
		if (!map.containsKey("index.mapping.total_fields.limit")) {
            map.put("index.mapping.total_fields.limit", 10000);
        }
        if (!map.containsKey("index.mapping.nested_fields.limit")) {
            map.put("index.mapping.nested_fields.limit", 10000);
        }

        //TODO: Uncomment when ES version used is at least 7.0
        /*map.put("index.mapping.nested_objects.limit",
                Config.getIntProperty("ES_INDEX_MAPPING_NESTED_OBJECTS_LIMITS", 25000));*/


		if(replicasMode.getNumberOfReplicas()>-1) {
			map.put("number_of_replicas", replicasMode.getNumberOfReplicas());
		}
		map.put("auto_expand_replicas",replicasMode.getAutoExpandReplicas());

		// create actual index
		CreateIndexRequestBuilder cirb = iac.prepareCreate(indexName).setSettings(map);
		CreateIndexResponse createIndexResponse = cirb.execute().actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);

		AdminLogger.log(this.getClass(), "createIndex",
			"Index created: " + indexName + " with shards: " + shards);

		return createIndexResponse;
	}


	public synchronized CreateIndexResponse createIndex(final String indexName, String settings,
			int shards, final String type, final String mapping)
			throws ElasticsearchException, IOException {

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
				shards = Config.getIntProperty("es.index.number_of_shards", 2);
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
		iac.prepareCreate(indexName).setSettings(settings, XContentType.JSON)
				.addMapping(type, mapping).setTimeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS)).execute();

        AdminLogger.log(this.getClass(), "createIndex",
                "Index created: " + indexName + " with shards: " + shards);

		return null;
	}

	/**
	 * Returns the json (String) for
	 * the default ES index settings
	 * @param shards
	 * @return
	 * @throws IOException
	 */
	public String getDefaultIndexSettings(int shards) throws IOException{
		return jsonBuilder().startObject()
            .startObject("index")
           	.field("number_of_shards",shards)
            .startObject("analysis")
             .startObject("analyzer")
              .startObject("default")
               .field("type", "whitespace")
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

        ClusterHealthResponse res  = chr.actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
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
    public synchronized void updateReplicas (final String indexName, final int replicas) throws DotDataException {

		if (!ClusterUtils.isReplicasSet() || !StringUtils
				.isNumeric(Config.getStringProperty("ES_INDEX_REPLICAS", null))) {
			AdminLogger.log(this.getClass(), "updateReplicas",
					"Replicas can only be updated when an Enterprise License is used and ES_INDEX_REPLICAS is set to a specific value.");
			throw new DotDataException(
					"Replicas can only be updated when an Enterprise License is used and ES_INDEX_REPLICAS is set to a specific value.");
		}

    	AdminLogger.log(this.getClass(), "updateReplicas", "Trying to update replicas to index: " + indexName);

		final ClusterIndexHealth health = getClusterHealth().get( indexName);
		if(health ==null){
			return;
		}

		final int curReplicas = health.getNumberOfReplicas();

		if(curReplicas != replicas){
			final Map newSettings = new HashMap();
	        newSettings.put("number_of_replicas", replicas);

			UpdateSettingsRequestBuilder usrb = new ESClient().getClient().admin().indices().prepareUpdateSettings(indexName);
			usrb.setSettings(newSettings);
			usrb.execute().actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
		}

		AdminLogger.log(this.getClass(), "updateReplicas", "Replicas updated to index: " + indexName);
    }

    public void putToIndex(String idx, String json, String id){
	   try{
		   Client client=new ESClient().getClient();

		   IndexResponse response = client.prepareIndex(idx, SiteSearchAPI.ES_SITE_SEARCH_MAPPING, id)
			        .setSource(json)
			        .execute()
			        .actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);

		} catch (Exception e) {
		    Logger.error(ESIndexAPI.class, e.getMessage(), e);


		}

    }

    public void createAlias(String indexName, String alias) {
        try{
            // checking for existing alias
            if(getAliasToIndexMap(APILocator.getSiteSearchAPI().listIndices()).get(alias)==null) {
                Client client = esclient.getClient();
                IndicesAliasesRequest req = new IndicesAliasesRequest();
                req.addAliasAction(IndicesAliasesRequest.AliasActions.add().alias(alias).index(indexName));
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
            Client client = esclient.getClient();
            ClusterStateRequest clusterStateRequest = Requests.clusterStateRequest()
                    .routingTable(true)
                    .nodes( true )
                    .indices( indexNames );
            MetaData md=client.admin().cluster().state(clusterStateRequest)
                                                .actionGet(30000).getState().metaData();

            for ( IndexMetaData imd : md ) {
                for ( ObjectCursor<AliasMetaData> aliasCursor : imd.getAliases().values() ) {
                    alias.put( imd.getIndex().getName(), aliasCursor.value.alias() );
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
        client.admin().indices().close(new CloseIndexRequest(indexName)).actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);

        AdminLogger.log(this.getClass(), "closeIndex", "Index: " + indexName + " closed");
    }

    public void openIndex(String indexName) {
    	AdminLogger.log(this.getClass(), "openIndex", "Trying to open index: " + indexName);

        Client client=new ESClient().getClient();
        client.admin().indices().open(new OpenIndexRequest(indexName)).actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);

        AdminLogger.log(this.getClass(), "openIndex", "Index: " + indexName + " opened");
    }

    public List<String> getClosedIndexes() {
        Client client = new ESClient().getClient();
        ImmutableOpenMap<String, IndexMetaData>
            indexState =
            client.admin().cluster().prepareState().execute().actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS)
                .getState().getMetaData().indices();

        List<String> closeIdx = new ArrayList<>();

        for (ObjectCursor<String> idx : indexState.keys()) {
            IndexMetaData idxM = indexState.get(idx.value);
            if (idxM.getState().equals(State.CLOSE)) {
                closeIdx.add(idx.value);
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

	/**
	 * Creates a snapshot zip file using the index and creating a repository on
	 * the path.repo location. This file structure will remain on the file system.
	 * The snapshot name is usually the same as the index name.
	 *
	 * @param repositoryName
	 *            repository name
	 * @param snapshotName
	 *            snapshot name
	 * @param indexName
	 *            index name
	 * @return
	 *            zip file with the repository and snapshot
	 * @throws IllegalArgumentException
	 *            for invalid repository and snapshot names
	 * @throws IOException
	 *            for problems writing the files to the repository path
	 */
	public File createSnapshot(String repositoryName, String snapshotName, String indexName)
			throws IOException, IllegalArgumentException, DotStateException, ElasticsearchException {
		checkArgument(snapshotName!=null,"There is no valid snapshot name.");
		checkArgument(indexName!=null,"There is no valid index name.");
		Client client = esclient.getClient();
		String fileName = indexName + "_" + DateUtil.format(new Date(), "yyyy-MM-dd_hh-mm-ss");
		File toFile = null;
		// creates specific backup path (if it shouldn't exist)

		toFile = new File(client.settings().get(REPOSITORY_PATH));
		if (!toFile.exists()) {
			toFile.mkdirs();
		}
		// initial repository under the complete path
		createRepository(toFile.getPath(), repositoryName, true);
		// if the snapshot exists on the repository
		if (isSnapshotExist(repositoryName, snapshotName)) {
			Logger.warn(this.getClass(), snapshotName + " snapshot already exists");
		} else {
			CreateSnapshotResponse response = client.admin().cluster()
					.prepareCreateSnapshot(repositoryName, snapshotName).setWaitForCompletion(true)
					.setIndices(indexName).get();
			if (response.status().equals(RestStatus.OK)) {
				Logger.debug(this.getClass(), "Snapshot was created:" + snapshotName);
			} else {
				Logger.error(this.getClass(), response.status().toString());
				throw new ElasticsearchException("Could not create snapshot");
			}
		}
		// this will be the zip file using the same name of the directory path

		File toZipFile = new File(toFile.getParent() + File.separator + fileName + ".zip");
		try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(toZipFile.toPath()))) {
			ZipUtil.zipDirectory(toFile.getAbsolutePath(), zipOut);
			return toZipFile;
		}
	}

	/**
	 * Restores snapshot validating that such snapshot name exists on the
	 * repository
	 *
	 * @param repositoryName
	 *            Repository name
	 * @param snapshotName
	 *            Snapshot name, most exists on the repository
	 * @return Is true if the snapshot was restored
	 * @throws InterruptedException
	 *             if the current thread was interrupted while waiting
	 * @throws ExecutionException
	 *             if the computation threw an exception
	 */
	private boolean restoreSnapshot(String repositoryName, String snapshotName)
			throws InterruptedException, ExecutionException {
		Client client = esclient.getClient();
		if (!isSnapshotExist(repositoryName, snapshotName) && ESIndexAPI.BACKUP_REPOSITORY.equals(repositoryName)) {
			snapshotName = BACKUP_REPOSITORY; //When restoring a snapshot created straight from a live index, the snapshotName is also: backup
		}
		if (isRepositoryExist(repositoryName) && isSnapshotExist(repositoryName, snapshotName)) {
			GetSnapshotsRequest getSnapshotsRequest = new GetSnapshotsRequest(repositoryName);
			GetSnapshotsResponse getSnapshotsResponse = client.admin().cluster().getSnapshots(getSnapshotsRequest).get();
			final List<SnapshotInfo> snapshots = getSnapshotsResponse.getSnapshots();
			for(SnapshotInfo snapshot: snapshots){
				List<String> indices = snapshot.indices();
				for(String index: indices){
					if(!isIndexClosed(index)){
						throw new DotStateException("Index \"" + index + "\" is not closed and can not be restored");
					}
				}
			}
			RestoreSnapshotRequest restoreSnapshotRequest = new RestoreSnapshotRequest(repositoryName, snapshotName).waitForCompletion(true);
			RestoreSnapshotResponse response = client.admin().cluster().restoreSnapshot(restoreSnapshotRequest).get();
			if (response.status() != RestStatus.OK) {
				Logger.error(this.getClass(),
						"Problems restoring snapshot " + snapshotName + " with status: " + response.status().name());
			} else {
				Logger.debug(this.getClass(), "Snapshot was restored.");
				return true;
			}
		}
		return false;
	}


	/**
	 * Uploads and restore a snapshot by using a zipped repository from a input
	 * stream as source. The file name most comply to the format
	 * <index_name>.zip as the <index_name> will be used to identify the index
	 * name to be restored. The zip file contains the repository information,
	 * this includes the snapshot name. The index name is used to restore the
	 * snapshot, a repository might contain several snapshot thus the need to
	 * identify a snapshot by index name.  The repository is deleted after the
	 * restore is done.
	 *
	 * @param inputFile
	 *            stream with the zipped repository file
     *
	 * @return true if the snapshot was restored
	 * @throws InterruptedException
	 *             if the current thread was interrupted while waiting
	 * @throws ExecutionException
	 *             if the computation threw an exception
	 * @throws ZipException
	 * 			   for problems during the zip extraction process
	 * @throws IOException
	 *             for problems writing the temporal zip file or the temporal zip contents
	 */
	public boolean uploadSnapshot(InputStream inputFile)
			throws InterruptedException, ExecutionException, ZipException, IOException {
		return uploadSnapshot(inputFile, true);
	}

	/**
	 * Uploads and restore a snapshot by using a zipped repository from a input
	 * stream as source. The file name most comply to the format
	 * <index_name>.zip as the <index_name> will be used to identify the index
	 * name to be restored. The zip file contains the repository information,
	 * this includes the snapshot name. The index name is used to restore the
	 * snapshot, a repository might contain several snapshot thus the need to
	 * identify a snapshot by index name.
	 *
	 * @param inputFile
	 *            stream with the zipped repository file
	 * @param cleanRepository
	 * 	          defines if the respository should be deleted after the restore.
	 *
	 * @return true if the snapshot was restored
	 * @throws InterruptedException
	 *             if the current thread was interrupted while waiting
	 * @throws ExecutionException
	 *             if the computation threw an exception
	 * @throws ZipException
	 * 			   for problems during the zip extraction process
	 * @throws IOException
	 *             for problems writing the temporal zip file or the temporal zip contents
	 */
	public boolean uploadSnapshot(InputStream inputFile, boolean cleanRepository)
			throws InterruptedException, ExecutionException, ZipException, IOException {
		File outFile = null;
		AdminLogger.log(this.getClass(), "uploadSnapshot", "Trying to restore snapshot index");
		// creates specific backup path (if it shouldn't exist)
		File toDirectory = new File(esclient.getClient().settings().get(REPOSITORY_PATH));
		if (!toDirectory.exists()) {
			toDirectory.mkdirs();
		}
		// zip file extraction
		outFile = File.createTempFile("snapshot", null, toDirectory.getParentFile());
		//File outFile = new File(toDirectory.getParent() + File.separator + snapshotName);
		FileUtils.copyStreamToFile(outFile, inputFile, null);
		ZipFile zipIn = new ZipFile(outFile);
		return uploadSnapshot(zipIn, toDirectory.getAbsolutePath(), cleanRepository);
	}

	/**
	 * Uploads and restore a snapshot using a zipped repository file and the
	 * index name to restore from the repository.
	 *
	 * @param zip
	 *            zip file containing the repository file structure
	 * @param toDirectory
	 *            place to extract the zip file
     *
	 * @return true if the snapshot was restored
     *
	 * @throws InterruptedException
	 *             if the current thread was interrupted while waiting
	 * @throws ExecutionException
	 *             if the computation threw an exception
	 * @throws ZipException
	 * 			   for problems during the zip extraction process
	 * @throws IOException
	 *             for problems writing the temporal zip file or the temporal zip contents
	 */
	public boolean uploadSnapshot(ZipFile zip, String toDirectory, boolean cleanRepository)
			throws InterruptedException, ExecutionException, ZipException, IOException {
		ZipUtil.extract(zip, new File(toDirectory));
		File zipDirectory = null;
		try{
			zipDirectory = new File(toDirectory);
			String snapshotName = esIndexHelper.findSnapshotName(zipDirectory);
			if (snapshotName == null) {
				Logger.error(this.getClass(), "No snapshot file on the zip.");
				throw new ElasticsearchException("No snapshot file on the zip.");
			}
			if (!isRepositoryExist(BACKUP_REPOSITORY)) {
				// initial repository under the complete path
				createRepository(toDirectory, BACKUP_REPOSITORY, true);
			}
			return restoreSnapshot(BACKUP_REPOSITORY, snapshotName);
		}finally{
			File tempZip = new File(zip.getName());
			if(zip!=null && tempZip.exists()){
				tempZip.delete();
			}
			if(cleanRepository){
				deleteRepository(BACKUP_REPOSITORY);
			}
		}
	}

	/**
	 * Validates if a repository name exists on the ES client, using the data
	 * directory and the path.repo
	 *
	 * @param repositoryName
	 *            valid not null repository name
	 * @return true if the repository exists
	 */
	private boolean isRepositoryExist(String repositoryName) {
		boolean result = false;
		Client client = esclient.getClient();
		List<RepositoryMetaData> repositories = client.admin().cluster().prepareGetRepositories().get()
				.repositories();
		if (repositories.size() > 0) {
			for (RepositoryMetaData repo : repositories) {
				result = repo.name().equals(repositoryName);
				if (result){
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Creates a new repository for snapshots.  The snapshot name is usually the index name.
	 *
	 * @param path
	 *            path to repository, this should be within the repo.path
	 *            location
	 * @param repositoryName
	 *            repository name, if empty "catchall" in used by ES
	 * @param compress
	 *            if the repository should be compressed
	 * @throws IllegalArgumentException
	 *            if the path to the repository doesn't exists
	 * @return
	 *            true if the repository was created
	 */
	private boolean createRepository(String path, String repositoryName, boolean compress)
			throws IllegalArgumentException, DotStateException {
		boolean result = false;
		Path directory = Paths.get(path);
		if (!Files.exists(directory)) {
			throw new IllegalArgumentException("Invalid path to repository while creating the repository.");
		}
		Client client = esclient.getClient();
		if (!isRepositoryExist(repositoryName)) {
			Settings settings = Settings.builder().put("location", path).put("compress", compress)
					.build();
			PutRepositoryResponse response = client.admin().cluster().preparePutRepository(repositoryName).setType("fs").setSettings(settings).get();
			if(result = response.isAcknowledged()){
				Logger.debug(this.getClass(), "Repository was created.");
			}else{
				//throw new DotStateException("Error creating respository on [" + path + "] named " + repositoryName);
				throw new DotIndexRepositoryException("Error creating respository on [" + path + "] named " + repositoryName,"error.creating.index.repository",path,repositoryName);
			}
		} else {
			Logger.warn(this.getClass(), repositoryName + " repository already exists");
		}
		return result;
	}

	/**
	 * Validates if a snapshot exist in a given repository usually the index name
	 *
	 * @param repositoryName
	 *            this repository should exists
	 * @param snapshotName
	 *            snapshot name
	 * @return true is the snapshot exists
	 */
	private boolean isSnapshotExist(String repositoryName, String snapshotName) {
		boolean result = false;
		Client client = esclient.getClient();
		List<SnapshotInfo> snapshotInfo = client.admin().cluster().prepareGetSnapshots(repositoryName).get()
				.getSnapshots();
		if (snapshotInfo.size() > 0) {
			for (SnapshotInfo snapshot : snapshotInfo){
				result = snapshot.snapshotId().getName().equals(snapshotName);
				if(result){
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Deletes repository deleting the file system structure as well, beware
	 * various snapshot might be stored on the repository, this can not be
	 * undone.
	 *
	 * @param repositoryName repository name
	 * @return true if the repository is deleted
	 */
	public boolean deleteRepository(String repositoryName) {
		return deleteRepository(repositoryName, true);
	}

	/**
	 * Deletes repository, by setting cleanUp to true the repository will be
	 * removed from file system, beware various snapshot might be stored on the
	 * repository, this can not be undone.
	 *
	 * @param repositoryName repository name
	 * @param cleanUp true to remove files from file system after deleting the repository
	 *        reference.
	 * @return true if the repository is deleted
	 */
	public boolean deleteRepository(String repositoryName, boolean cleanUp) {

		boolean result = false;
		Client client = esclient.getClient();
		if (isRepositoryExist(repositoryName)) {
			try {
				DeleteRepositoryResponse response = client.admin().cluster().prepareDeleteRepository(repositoryName)
						.execute().actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
				if (response.isAcknowledged()) {
					Logger.info(this.getClass(), repositoryName + " repository has been deleted.");
					result = true;
				}
			} catch (Exception e) {
				Logger.error(this.getClass(), e.getMessage());
			}
			if (cleanUp) {
				File toDelete = new File(client.settings().get(REPOSITORY_PATH));
				try {
					FileUtil.deleteDir(toDelete.getAbsolutePath());
				} catch (IOException e) {
					Logger.error(this.getClass(), "The files on " + toDelete.getAbsolutePath() + " were not deleted.");
				}
			} else {
				Logger.warn(this.getClass(), "No files were deleted");
			}
		}
		return result;
	}

	public String getRepositoryPath(){
		return esclient.getClient().settings().get(REPOSITORY_PATH);
	}
}
