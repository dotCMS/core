package com.dotcms.content.elasticsearch.business;

import static com.dotcms.util.DotPreconditions.checkArgument;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import com.dotcms.cluster.ClusterUtils;
import com.dotcms.cluster.business.ClusterAPI;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
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
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.ZipUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import org.apache.commons.lang.StringUtils;
import org.apache.tools.zip.ZipEntry;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest.Level;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.repositories.delete.DeleteRepositoryRequest;
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesRequest;
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesResponse;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotResponse;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsRequest;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequest;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheResponse;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeRequest;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeResponse;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.cluster.health.ClusterIndexHealth;
import org.elasticsearch.cluster.metadata.RepositoryMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.repositories.fs.FsRepository;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.snapshots.SnapshotInfo;

public class ESIndexAPI {

    private  final String MAPPING_MARKER = "mapping=";
    private  final String JSON_RECORD_DELIMITER = "---+||+-+-";
    private static final ESMappingAPIImpl mappingAPI = new ESMappingAPIImpl();

    public static final String BACKUP_REPOSITORY = "backup";
    private final String REPOSITORY_PATH = "path.repo";

	public static final int INDEX_OPERATIONS_TIMEOUT_IN_MS =
			Config.getIntProperty("ES_INDEX_OPERATIONS_TIMEOUT", 15000);

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
		this.iapi = new ContentletIndexAPIImpl();
		this.esIndexHelper = ESIndexHelper.INSTANCE;
		this.serverAPI = APILocator.getServerAPI();
		this.clusterAPI = APILocator.getClusterAPI();
	}

	@VisibleForTesting
	protected ESIndexAPI(final ContentletIndexAPIImpl iapi, final ESIndexHelper esIndexHelper,
						 final ServerAPI serverAPI, final ClusterAPI clusterAPI){
		this.iapi = iapi;
		this.esIndexHelper = esIndexHelper;
		this.serverAPI = serverAPI;
		this.clusterAPI = clusterAPI;
	}

	@SuppressWarnings("unchecked")
	public Map<String, IndexStats> getIndicesStats() {
		final Request request = new Request("GET", "/_stats");
		final Map<String, Object> jsonMap = performLowLevelRequest(request);

		Map<String, IndexStats> indexStatsMap = new HashMap<>();

		final Map<String, Object> indices = (Map<String, Object>)jsonMap.get("indices");

		indices.forEach((key, value)-> {
			Map<String, Object> indexStats = (Map<String, Object>) ((Map<String, Object>)
					indices.get(key)).get("primaries");

			int numOfDocs = (int) ((Map<String, Object>) indexStats.get("docs"))
					.get("count");

			int sizeInBytes = (int) ((Map<String, Object>) indexStats.get("store"))
					.get("size_in_bytes");

			indexStatsMap.put(key, new IndexStats(key, numOfDocs, sizeInBytes));
		});

		return indexStatsMap;
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
	public Map<String, Integer> flushCaches(final List<String> indexNames)  {
		Logger.warn(this.getClass(), "Flushing Elasticsearch index caches:" + indexNames);
		if(indexNames==null || indexNames.isEmpty()) {
			return ImmutableMap.of("failedShards",0, "successfulShards", 0);
		}

		ClearIndicesCacheRequest request = new ClearIndicesCacheRequest(
				indexNames.toArray(new String[0]));

		ClearIndicesCacheResponse clearCacheResponse = Sneaky.sneak(()->(
				RestHighLevelClientProvider.getInstance().getClient().indices()
				.clearCache(request, RequestOptions.DEFAULT)));


		Map<String, Integer> map= ImmutableMap.of("failedShards",
				clearCacheResponse.getFailedShards(), "successfulShards",
				clearCacheResponse.getSuccessfulShards());

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

		BufferedWriter bw;
		try (final ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(toFile.toPath()))){
		    zipOut.setLevel(9);
		    zipOut.putNextEntry(new ZipEntry(toFile.getName()));

			bw = new BufferedWriter(
			        new OutputStreamWriter(zipOut), 500000); // 500K buffer

			final String type=index.startsWith("sitesearch_") ? SiteSearchAPI.ES_SITE_SEARCH_MAPPING
					: "content";
	        final String mapping = mappingAPI.getMapping(index);
	        bw.write(MAPPING_MARKER);
	        bw.write(mapping);
	        bw.newLine();

			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
			searchSourceBuilder.query(QueryBuilders.matchAllQuery());
			searchSourceBuilder.size(100);
			searchSourceBuilder.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
			//_doc has no real use-case besides being the most efficient sort order.
			searchSourceBuilder.sort("_doc", SortOrder.DESC);


			SearchRequest searchRequest = new SearchRequest();
			searchRequest.scroll(TimeValue.timeValueMinutes(2));
			searchRequest.source(searchSourceBuilder);

			final SearchResponse searchResponse = Sneaky.sneak(()->
					RestHighLevelClientProvider.getInstance().getClient().search(searchRequest,
							RequestOptions.DEFAULT));

			String scrollId = searchResponse.getScrollId();


			while (true) {
				// new way
				SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
				scrollRequest.scroll(TimeValue.timeValueMinutes(2));
				SearchResponse searchScrollResponse = RestHighLevelClientProvider.getInstance()
						.getClient().scroll(scrollRequest, RequestOptions.DEFAULT);

				scrollId = searchResponse.getScrollId();

				boolean hitsRead = false;
				for (SearchHit hit : searchResponse.getHits()) {
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
            final ForceMergeRequest forceMergeRequest = new ForceMergeRequest(indexNames.toArray(
					new String[0]));
            final ForceMergeResponse forceMergeResponse =
					RestHighLevelClientProvider.getInstance().getClient()
							.indices().forcemerge(forceMergeRequest, RequestOptions.DEFAULT);

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
			DeleteIndexRequest request = new DeleteIndexRequest(indexName);
			request.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
			AcknowledgedResponse deleteIndexResponse =
					RestHighLevelClientProvider.getInstance().getClient()
							.indices().delete(request, RequestOptions.DEFAULT);

            AdminLogger.log(this.getClass(), "delete", "Index: " + indexName + " deleted.");

            return deleteIndexResponse.isAcknowledged();
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
            ArrayList<String> jsons = new ArrayList<>();
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
								BulkRequest request = new BulkRequest();
								request.timeout(TimeValue.
										timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
                                for (String raw : jsons) {
                                    int delimidx = raw.indexOf(JSON_RECORD_DELIMITER);
                                    if (delimidx > 0) {
                                        String id = raw.substring(0, delimidx);
                                        String json = raw.substring(
                                                delimidx + JSON_RECORD_DELIMITER.length());
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
											request.add(new IndexRequest(index, type, id)
                                                    .source(mapper.writeValueAsString(newMap)));
                                        }
                                    }
                                }
                                if (request.numberOfActions() > 0) {
                                	RestHighLevelClientProvider.getInstance().getClient()
											.bulk(request, RequestOptions.DEFAULT);
                                }
                            } finally {
                                jsons = new ArrayList<>();
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
		final GetIndexRequest request = new GetIndexRequest("*");
		final GetIndexResponse response = Sneaky.sneak(()->(
				RestHighLevelClientProvider.getInstance().getClient()
						.indices().get(request, RequestOptions.DEFAULT)));

		return new HashSet<>(Arrays.asList(response.getIndices()));
	}

	/**
	 * Returns close status of an index
	 * @return
	 */
	// TODO replace with high level client
	public boolean isIndexClosed(String index) {
		return getClosedIndexes().contains(index);
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
	private void moveIndexToLocalNode(final String index) throws IOException {
		UpdateSettingsRequest request = new UpdateSettingsRequest(index);
		request.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));

		final String nodeName="dotCMS_" + APILocator.getServerAPI().readServerId();

		request.settings(jsonBuilder().startObject()
				.startObject("index")
				.field("number_of_replicas",0)
				.field("routing.allocation.include._name",nodeName)
				.endObject()
				.endObject().toString(), XContentType.JSON);

		RestHighLevelClientProvider.getInstance().getClient()
				.indices().putSettings(request, RequestOptions.DEFAULT);
    }

	/**
	 * clusters an index, including changing the routing
	 * @param index
	 * @throws IOException
	 */
    public void moveIndexBackToCluster(final String index) throws IOException {
		final XContentBuilder builder = jsonBuilder().startObject().startObject("index");

		UpdateSettingsRequest request = new UpdateSettingsRequest(index);
		request.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
		request.settings(builder.endObject().endObject().toString(), XContentType.JSON);
		RestHighLevelClientProvider.getInstance().getClient()
				.indices().putSettings(request, RequestOptions.DEFAULT);
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

		AdminLogger.log(this.getClass(), "createIndex",
			"Trying to create index: " + indexName + " with shards: " + shards);

		shards = getShardsFromConfigIfNeeded(shards);

		//default settings, if null
		if(settings ==null){
			settings = getDefaultIndexSettings(shards);
		}
		Map map = new ObjectMapper().readValue(settings, LinkedHashMap.class);
		map.put("number_of_shards", shards);
		map.put("index.mapping.total_fields.limit",
			Config.getIntProperty("ES_INDEX_MAPPING_TOTAL_FIELD_LIMITS", 5000));
        map.put("index.mapping.nested_fields.limit",
                Config.getIntProperty("ES_INDEX_MAPPING_NESTED_FIELDS_LIMITS", 5000));

		final CreateIndexRequest request = new CreateIndexRequest(indexName);
		request.settings(map);
		request.setTimeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
		final CreateIndexResponse createIndexResponse =
				RestHighLevelClientProvider.getInstance().getClient()
						.indices().create(request, RequestOptions.DEFAULT);

		AdminLogger.log(this.getClass(), "createIndex",
			"Index created: " + indexName + " with shards: " + shards);

		return createIndexResponse;
	}

	private int getShardsFromConfigIfNeeded(int shards) {
		if(shards <1){
			try{
				shards = Integer.parseInt(System.getProperty("es.index.number_of_shards"));
			}catch(Exception e){
				Logger.warnAndDebug(ESIndexAPI.class, "Unable to parse shards from config", e);
			}
		}
		if(shards <1){
			try{
				shards = Config.getIntProperty("es.index.number_of_shards", 2);
			}catch(Exception e){
				Logger.warnAndDebug(ESIndexAPI.class, "Unable to parse shards from config", e);
			}
		}

		if(shards <0){
			shards=1;
		}
		return shards;
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
          .endObject().toString();
	}

    /**
     * returns cluster health
     * @return
     */
    public Map<String,ClusterIndexHealth> getClusterHealth() {
		ClusterHealthRequest request = new ClusterHealthRequest();
		request.level(Level.INDICES);
		request.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
		ClusterHealthResponse response = Sneaky.sneak(()->
				RestHighLevelClientProvider.getInstance().getClient()
						.cluster().health(request,RequestOptions.DEFAULT));

		return response.getIndices();
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
			final Map<String,Integer> newSettings = new HashMap<>();
	        newSettings.put("number_of_replicas", replicas);

			UpdateSettingsRequest request = new UpdateSettingsRequest(indexName);
			request.settings(newSettings);
			request.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
			Sneaky.sneak(()->RestHighLevelClientProvider.getInstance().getClient()
					.indices().putSettings(request, RequestOptions.DEFAULT));
		}

		AdminLogger.log(this.getClass(), "updateReplicas", "Replicas updated to index: " + indexName);
    }

//    public void putToIndex(String idx, String json, String id){
//	   try{
//		   Client client=new RestHighLevelClientProvider.getInstance().getClient()().getClient();
//
//		   IndexResponse response = client.prepareIndex(idx, SiteSearchAPI.ES_SITE_SEARCH_MAPPING, id)
//			        .setSource(json)
//			        .execute()
//			        .actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
//
//		} catch (Exception e) {
//		    Logger.error(ESIndexAPI.class, e.getMessage(), e);
//
//
//		}
//
//    }

    public void createAlias(String indexName, String alias) {
        try{
            // checking for existing alias
            if(getAliasToIndexMap(APILocator.getSiteSearchAPI().listIndices()).get(alias)==null) {
                IndicesAliasesRequest request = new IndicesAliasesRequest();
				request.addAliasAction(IndicesAliasesRequest.AliasActions.add().alias(alias)
						.index(indexName));
				request.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
				RestHighLevelClientProvider.getInstance().getClient().indices()
						.updateAliases(request, RequestOptions.DEFAULT);
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

    	GetAliasesRequest request = new GetAliasesRequest();
		request.indices(indexNames);
		GetAliasesResponse response = Sneaky.sneak(()->
				RestHighLevelClientProvider.getInstance().getClient()
						.indices().getAlias(request, RequestOptions.DEFAULT));

		Map<String,String> alias=new HashMap<>();

		response.getAliases().forEach((indexName, value) -> {
			if(UtilMethods.isSet(value)) {
				final String aliasName = value.iterator().next().alias();
				alias.put(indexName, aliasName);
			}
		});

		return alias;
    }

    public String getIndexAlias(String indexName) {
        return getIndexAlias(new String[]{indexName}).get(indexName);
    }

    public Map<String,String> getAliasToIndexMap(List<String> indices) {
        Map<String,String> map=getIndexAlias(indices);
        Map<String,String> mapReverse=new HashMap<>();
        for (String idx : map.keySet())
            mapReverse.put(map.get(idx), idx);
        return mapReverse;
    }

    public void closeIndex(String indexName) {
    	AdminLogger.log(this.getClass(), "closeIndex", "Trying to close index: " + indexName);

		final CloseIndexRequest request = new CloseIndexRequest(indexName);
		request.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
        Sneaky.sneak(()->RestHighLevelClientProvider.getInstance().getClient()
				.indices().close(request, RequestOptions.DEFAULT));

        AdminLogger.log(this.getClass(), "closeIndex", "Index: " + indexName + " closed");
    }

    public void openIndex(String indexName) {
    	AdminLogger.log(this.getClass(), "openIndex", "Trying to open index: " + indexName);

        final OpenIndexRequest request = new OpenIndexRequest(indexName);
		request.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
		Sneaky.sneak(()->RestHighLevelClientProvider.getInstance().getClient()
				.indices().open(new OpenIndexRequest(indexName), RequestOptions.DEFAULT));

        AdminLogger.log(this.getClass(), "openIndex", "Index: " + indexName + " opened");
    }

    public List<String> getClosedIndexes() {

    	final Request request = new Request("GET", "/_cluster/state/metadata/");
		final Map<String, Object> jsonMap = performLowLevelRequest(request);

		final Map<String, Object> metadata = (Map<String, Object>) jsonMap.get("metadata");
		final Map<String, Object> indices = (Map<String, Object>)metadata.get("indices");

		final List<String> closedIndices = new ArrayList<>();
		indices.forEach((key, value) -> {
			if (((Map<String, String>) value).get("state").equals("close")) {
				closedIndices.add(key);
			}
		});

		return closedIndices;
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
     *
     * @deprecated Use ES Snapshot via ES REST API instead {@see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.7/modules-snapshots.html">}.
	 */

	@Deprecated
	public File createSnapshot(String repositoryName, String snapshotName, String indexName)
			throws IOException, IllegalArgumentException, DotStateException, ElasticsearchException {
		checkArgument(snapshotName!=null,"There is no valid snapshot name.");
		checkArgument(indexName!=null,"There is no valid index name.");

		// initial repository under the complete path
		createRepository(getESRepositoryPath(), repositoryName, true);
		// if the snapshot exists on the repository
		if (isSnapshotExist(repositoryName, snapshotName)) {
			Logger.warn(this.getClass(), snapshotName + " snapshot already exists");
		} else {
			CreateSnapshotRequest request = new CreateSnapshotRequest(repositoryName,snapshotName);
			request.waitForCompletion(true);
			request.indices(indexName);

			CreateSnapshotResponse response = RestHighLevelClientProvider.getInstance().getClient()
					.snapshot().create(request, RequestOptions.DEFAULT);
			if (response.status().equals(RestStatus.OK)) {
				Logger.debug(this.getClass(), "Snapshot was created:" + snapshotName);
			} else {
				Logger.error(this.getClass(), response.status().toString());
				throw new ElasticsearchException("Could not create snapshot");
			}
		}
		// this will be the zip file using the same name of the directory path

		File toFile = new File(getDotCMSRepoPath());
		if (!toFile.exists()) {
			toFile.mkdirs();
		}

		String fileName = indexName + "_" + DateUtil.format(new Date(), "yyyy-MM-dd_hh-mm-ss");

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
	private boolean restoreSnapshot(String repositoryName, String snapshotName) {
		if (!isSnapshotExist(repositoryName, snapshotName)
				&& ESIndexAPI.BACKUP_REPOSITORY.equals(repositoryName)) {
			snapshotName = BACKUP_REPOSITORY; //When restoring a snapshot created straight from a live index, the snapshotName is also: backup
		}

		if (isRepositoryExist(repositoryName) && isSnapshotExist(repositoryName, snapshotName)) {
			GetSnapshotsRequest getSnapshotsRequest = new GetSnapshotsRequest(repositoryName);
			GetSnapshotsResponse getSnapshotsResponse = Sneaky.sneak(()->
					RestHighLevelClientProvider.getInstance().getClient().snapshot().
					get(getSnapshotsRequest, RequestOptions.DEFAULT));


			final List<SnapshotInfo> snapshots = getSnapshotsResponse.getSnapshots();
			for(SnapshotInfo snapshot: snapshots){
				List<String> indices = snapshot.indices();
				for(String index: indices){
					if(!isIndexClosed(index)){
						throw new DotStateException("Index \"" + index + "\" is not closed and can not be restored");
					}
				}
			}
			RestoreSnapshotRequest restoreSnapshotRequest =
					new RestoreSnapshotRequest(repositoryName, snapshotName).waitForCompletion(true);
			RestoreSnapshotResponse response = Sneaky.sneak(()->
					RestHighLevelClientProvider.getInstance().getClient()
							.snapshot().restore(restoreSnapshotRequest, RequestOptions.DEFAULT));

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
     *
     * @deprecated Use ES Snapshot via ES REST API instead {@see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.7/modules-snapshots.html">}.
	 */
	@Deprecated
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
     *
     * @deprecated Use ES Snapshot via ES REST API instead {@see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.7/modules-snapshots.html">}.
	 */

	@Deprecated
	public boolean uploadSnapshot(InputStream inputFile, boolean cleanRepository)
			throws InterruptedException, ExecutionException, ZipException, IOException {
		File outFile = null;
		AdminLogger.log(this.getClass(), "uploadSnapshot", "Trying to restore snapshot index");
		// creates specific backup path (if it shouldn't exist)
		final String DOTCMS_REPO_PATH = getDotCMSRepoPath();
		File toDirectory = new File(DOTCMS_REPO_PATH);
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
     *
     * @deprecated Use ES Snapshot via ES REST API instead {@see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.7/modules-snapshots.html">}.
	 */

	@Deprecated
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
				createRepository(getESRepositoryPath(), BACKUP_REPOSITORY, true);
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

		GetRepositoriesRequest request = new GetRepositoriesRequest();
		GetRepositoriesResponse response = Sneaky.sneak(()->
				RestHighLevelClientProvider.getInstance().getClient().snapshot()
				.getRepository(request, RequestOptions.DEFAULT));

		List<RepositoryMetaData> repositories = response.repositories();

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

		if (!isRepositoryExist(repositoryName)) {
			Settings settings = Settings.builder().put("location", path).put("compress", compress)
					.build();

			PutRepositoryRequest request = new PutRepositoryRequest();
			request.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
			request.settings(settings);
			request.name(repositoryName);
			request.type(FsRepository.TYPE);

			AcknowledgedResponse response = Sneaky.sneak(()->
					RestHighLevelClientProvider.getInstance().getClient().snapshot()
					.createRepository(request, RequestOptions.DEFAULT));

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
		GetSnapshotsRequest request = new GetSnapshotsRequest(repositoryName);
		request.masterNodeTimeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));

		GetSnapshotsResponse response = Sneaky.sneak(()->
				RestHighLevelClientProvider.getInstance().getClient().snapshot()
				.get(request, RequestOptions.DEFAULT));

		List<SnapshotInfo> snapshotInfo = response.getSnapshots();

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
		if (isRepositoryExist(repositoryName)) {
			try {
				DeleteRepositoryRequest request = new DeleteRepositoryRequest(repositoryName);
				request.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));

				AcknowledgedResponse response =
						RestHighLevelClientProvider.getInstance().getClient()
								.snapshot().deleteRepository(request, RequestOptions.DEFAULT);

				if (response.isAcknowledged()) {
					Logger.info(this.getClass(), repositoryName + " repository has been deleted.");
					result = true;
				}
			} catch (Exception e) {
				Logger.error(this.getClass(), e.getMessage());
			}
			if (cleanUp) {
				final String REPO_PATH = getDotCMSRepoPath();
				File toDelete = new File(REPO_PATH);
				try {
					org.apache.commons.io.FileUtils.cleanDirectory(toDelete);
				} catch (IOException e) {
					Logger.error(this.getClass(), "The files on " + toDelete.getAbsolutePath() + " were not deleted.");
				}
			} else {
				Logger.warn(this.getClass(), "No files were deleted");
			}
		}
		return result;
	}

	public String getESRepositoryPath(){
		ClusterGetSettingsRequest clusterGetSettingsRequest = new ClusterGetSettingsRequest();
		clusterGetSettingsRequest.includeDefaults(true);
		ClusterGetSettingsResponse clusterGetSettingsResponse = Sneaky.sneak(()->
				RestHighLevelClientProvider.getInstance().getClient()
				.cluster().getSettings(clusterGetSettingsRequest, RequestOptions.DEFAULT));

		final String repoPathFromConfig = clusterGetSettingsResponse.getSetting(REPOSITORY_PATH);
		return repoPathFromConfig.substring(1, repoPathFromConfig.length()-1);
	}

	public String getDotCMSRepoPath(){
		return Config.getStringProperty("ES_REPO_PATH",
				ConfigUtils.getDynamicContentPath() + File.separator + "esrepo");
	}

	@SuppressWarnings("unchecked")
	public ClusterStats getClusterStats() {
		final Request request = new Request("GET", "/_nodes/stats");
		final Map<String, Object> jsonMap = performLowLevelRequest(request);

		final String clusterName = (String) jsonMap.get("cluster_name");
		final ClusterStats clusterStats = new ClusterStats(clusterName);
		final Map<String, Object> nodes = (Map<String, Object>)jsonMap.get("nodes");

		nodes.forEach((key, value)-> {
			final NodeStats.Builder builder = new NodeStats.Builder();
			final Map<String, Object> stats = (Map<String, Object>) value;
			builder.name((String) stats.get("name"));
			builder.transportAddress((String) stats.get("transport_address"));
			builder.host((String) stats.get("host"));

			final Map<String, Object> indexStats = (Map<String, Object>) stats.get("indices");
			final int docCount = (int) ((Map<String, Object>) indexStats.get("docs")).get("count");
			final int size = (int) ((Map<String, Object>) indexStats.get("store"))
					.get("size_in_bytes");

			final List<String> roles = (List<String>) stats.get("roles");

			builder.master(roles.contains("master"));
			builder.docCount(docCount);
			builder.size(size);

			clusterStats.addNodeStats(builder.build());
		});

		return clusterStats;
	}

	Map performLowLevelRequest(Request request) {
		return performLowLevelRequest(request, Map.class);
	}

	<T> T performLowLevelRequest(Request request, Class<T> mappingClass) {
		final RestClient lowLevelClient = RestHighLevelClientProvider.getInstance().getClient()
				.getLowLevelClient();
		final Response response = Sneaky.sneak(() -> lowLevelClient.performRequest(request));
		final ObjectMapper mapper = new ObjectMapper();
		return Sneaky.sneak(() -> mapper
				.readValue(response.getEntity().getContent(), mappingClass));
	}

}
