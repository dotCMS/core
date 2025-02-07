/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.sitesearch;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;

import com.dotcms.content.elasticsearch.business.*;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.priv.util.SearchSourceBuilderUtil;
import com.dotcms.publishing.job.SiteSearchJobProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.quartz.TaskRuntimeValues;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.vavr.control.Try;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.quartz.SchedulerException;

public class ESSiteSearchAPI implements SiteSearchAPI{

    private final ESIndexAPI indexApi;
    private final ESMappingAPIImpl mappingAPI;
    private final IndiciesAPI indiciesAPI;
    private ArrayList<Object> list;
    private int indexPosition;

    @VisibleForTesting
    public ESSiteSearchAPI(final ESIndexAPI indexApi,
            final ESMappingAPIImpl mappingAPI,
            final IndiciesAPI indiciesAPI) {
        this.indexApi = indexApi;
        this.mappingAPI = mappingAPI;
        this.indiciesAPI = indiciesAPI;
    }

    public ESSiteSearchAPI() {
       this(APILocator.getESIndexAPI(), new ESMappingAPIImpl(), APILocator.getIndiciesAPI());
    }

    /**
     * List of all sitesearch indicies
     * @return
     */
    @Override
    public  List<String> listIndices() {
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return Collections.EMPTY_LIST;

        final List<String> indices = new ArrayList<>();

        indices.addAll(
            indexApi.listIndices().stream()
                    .filter(IndexType.SITE_SEARCH::is)
                    .collect(Collectors.toList())
        );

        Collections.sort(indices);
        Collections.reverse(indices);
        setDefaultToSpecificPosition(indices, 0);
        return indices;
    }

    /**
     * Set the default site search index to the specified position of the arraylist
     */
    private void setDefaultToSpecificPosition(final List<String> list, final int indexPosition) {

        if (list != null && list.size() > 1){

            try {
                //search the default site search index
                final String defaultIndice = indiciesAPI.loadIndicies().getSiteSearch();
                if (defaultIndice != null && !defaultIndice.isEmpty() && !list.isEmpty() ){
                    final int index = list.indexOf(defaultIndice);
                    //change the element defaultIndex to the first position of the arraylist if it is not yet
                    if(index < 0){
                        Logger.warn(this.getClass(), String.format("The default site search '%s' index was not found in the list of indices.", defaultIndice));
                    } else {
                        list.remove(index);
                        list.add(indexPosition, defaultIndice);
                    }
                }
            } catch (DotDataException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public  List<String> listClosedIndices() {

        List<String> indices=new ArrayList<>();

        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return indices;
        for(String indexName: indexApi.getClosedIndexes()){
            if(IndexType.SITE_SEARCH.is(indexName)){
                indices.add(indexName);
            }

        }
        Collections.sort(indices);
        Collections.reverse(indices);
        return indices;
    }

    @Override
    public SiteSearchResults search(String query, int start, int rows) throws ElasticsearchException{
        SiteSearchResults results = new SiteSearchResults();
        if(query ==null){
            results.setError("null query");
            return results;
        }
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return results;

        try{


            results =  search(indiciesAPI.loadIndicies().getSiteSearch(), query, start, rows);


        }
        catch(Exception e){
            results.setError(e.getMessage());


        }

        return results;



    }

    @Override
    public SiteSearchResults search(String indexName, String query, int offset, int limit) {
        if(!UtilMethods.isSet(query)){
            query = "*";
        }
        SiteSearchResults results = new SiteSearchResults();

        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return results;

        boolean isJson = StringUtils.isJson(query);

        //https://github.com/elasticsearch/elasticsearch/issues/2980
        if (query.contains( "/" )) {
            query = query.replaceAll( "/", "\\\\/" );
        }

        results.setQuery(query);
        results.setLimit(limit);
        results.setOffset(offset);

        SearchResponse resp = null;
        try {
            if(indexName ==null){
                indexName = indiciesAPI.loadIndicies().getSiteSearch();
            }
            if(!IndexType.SITE_SEARCH.is(indexName)){
                throw new ElasticsearchException(indexName + " is not a sitesearch index");

            }
            results.setIndex(indexName);
            SearchRequest request = new SearchRequest(indexApi.getNameWithClusterIDPrefix(indexName));
            SearchSourceBuilder searchSourceBuilder;

            if(!isJson){
                searchSourceBuilder = new SearchSourceBuilder();

                searchSourceBuilder = searchSourceBuilder.query( QueryBuilders
                            .queryStringQuery( query )
                            .defaultField( "*" ));
                if ( limit > 0 )
                    searchSourceBuilder.size(limit);
                if ( offset > 0 )
                    searchSourceBuilder.from(offset);

                searchSourceBuilder.highlighter(new HighlightBuilder().field("content", 255));
            }else{
                searchSourceBuilder = SearchSourceBuilderUtil
                        .getSearchSourceBuilder(query);

                if ( !query.contains( "highlight" ) ) {
                    searchSourceBuilder.highlighter(new HighlightBuilder().field("content"));
                }


            }
            searchSourceBuilder.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
            request.source(searchSourceBuilder);

            try{
                resp = RestHighLevelClientProvider.getInstance().getClient()
                        .search(request, RequestOptions.DEFAULT);
                results.setTook(resp.getTook().toString());
            }catch (SearchPhaseExecutionException e) {
                if(e.getMessage().contains("-order_dotraw] in order to sort on")){

                }else{
                    throw e;
                }
            }


            SearchHits hits =  resp.getHits();
            results.setTotalResults(hits.getTotalHits().value);
            results.setMaxScore(hits.getMaxScore());
            if(!isJson){
            	results.setQuery(searchSourceBuilder.toString());
            }


            for(SearchHit hit : hits.getHits()){


                    SiteSearchResult ssr = new SiteSearchResult(hit.getSourceAsMap());
                    ssr.setScore(hit.getScore());
                    try{
                    	ssr.setHighLight(new String[0]);
	                    Map<String, HighlightField> hl = hit.getHighlightFields();

	                    for(String key : hl.keySet()){

	                    	List<String> highlights = new ArrayList<>();
	                    	if(hl.get(key)!=null && hl.get(key).fragments() != null){
		                    	for(Text t : hl.get(key).fragments()){
		                    		highlights.add(t.toString());
		                    	}
	                    	}

	                    	ssr.setHighLight(highlights.toArray(new String[highlights.size()]));
	                    	break;
	                    }




                    }
                    catch(Exception e){
                    	Logger.error(this.getClass(), "Unable to get Site Search Highlights:" + e.getMessage());
                    }

                    results.getResults().add(ssr);

            }

        } catch (Exception e) {
            Logger.error(ESContentFactoryImpl.class, e.getMessage(), e);
            results.setError(e.getMessage());
        }


        return results;
    }

    /**
     * {@inheritDoc}
     * @param indexName
     * @return
     * @throws DotDataException
     */
    @Override
    public boolean isDefaultIndex(final String indexName) throws DotDataException {
       return  indexName.equals(indiciesAPI.loadIndicies().getSiteSearch());
    }

    @Override
    public void activateIndex(String indexName) throws DotDataException {
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return;

        final IndiciesInfo info = indiciesAPI.loadIndicies();
        final IndiciesInfo.Builder builder = IndiciesInfo.Builder.copy(info);

        if(IndexType.SITE_SEARCH.is(indexName)) {
            builder.setSiteSearch(indexName);
        }

        indiciesAPI.point(builder.build());
    }

    @Override
    public void deactivateIndex(String indexName) throws DotDataException, IOException {
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return;

        final IndiciesInfo info = indiciesAPI.loadIndicies();
        final IndiciesInfo.Builder builder = IndiciesInfo.Builder.copy(info);
        if(IndexType.SITE_SEARCH.is(indexName)) {
            builder.setSiteSearch(null);
        }

        indiciesAPI.point(builder.build());
    }

    @Override
    public synchronized boolean createSiteSearchIndex(String indexName, String alias, int shards) throws ElasticsearchException, IOException {
        if(indexName==null){
            return false;
        }
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return false;

        indexName=indexName.toLowerCase();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource("es-sitesearch-settings.json");
        // read settings and mappings
        String settings = new String(com.liferay.util.FileUtil.getBytes(new File(url.getPath())));
        url = classLoader.getResource("es-sitesearch-mapping.json");
        String mapping = new String(com.liferay.util.FileUtil.getBytes(new File(url.getPath())));


        //create index
        CreateIndexResponse cir = indexApi.createIndex(indexName, settings, shards);
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

        if(UtilMethods.isSet(alias)){
            indexApi.createAlias(indexName, alias);
        }

        //put mappings
        mappingAPI.putMapping(indexName, mapping);

        return true;
    }

    public synchronized boolean setAlias(String indexName, final String alias){
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level){
            return false;
        }
        if(UtilMethods.isNotSet(indexName) || UtilMethods.isNotSet(alias)){
            throw new IllegalArgumentException(String.format(" either one or both params aren't set. index: `%s`, alias: `%s` ",indexName, alias));
        }
        indexName = indexName.toLowerCase();
        indexApi.createAlias(indexName, alias);
        return false;
    }

    @Override
    public List<ScheduledTask> getTasks() throws SchedulerException{
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return null;

        List<ScheduledTask> tasks = QuartzUtils.getScheduledTasks(ES_SITE_SEARCH_NAME);
        return tasks;


    }

    @Override
    public ScheduledTask getTask(String taskName) throws SchedulerException{
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return null;

        List<ScheduledTask> tasks = getTasks();

        for(ScheduledTask x:tasks){

            if(x.getJobName()!=null && x.getJobName().equals(taskName)){
                return x;
            }
        }
        return null;
    }

    @Override
    public void scheduleTask(SiteSearchConfig config) throws SchedulerException, ParseException, ClassNotFoundException{
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return;

        String name = config.getJobName();
        name.replace('"', '\'');
        name.replace("'", "");
        String cronString = config.getCronExpression();

        if(config.getJobId()==null)
            config.setJobId(UUIDGenerator.generateUuid());


        ScheduledTask   task = new CronScheduledTask(name, ES_SITE_SEARCH_NAME,"Site Search ", SiteSearchJobProxy.class.getCanonicalName(),new Date(),null, 1,config,cronString);
        task.setSequentialScheduled(true);

        QuartzUtils.scheduleTask(task);

    }

    @Override
    public void deleteTask(String taskName) throws SchedulerException{
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return;

        ScheduledTask t = getTask(taskName);

        //Pause and remove any current jobs in the group
        QuartzUtils.pauseJob(t.getJobName(), ES_SITE_SEARCH_NAME);
        QuartzUtils.removeTaskRuntimeValues(t.getJobName(), ES_SITE_SEARCH_NAME);
        QuartzUtils.removeJob(t.getJobName(), ES_SITE_SEARCH_NAME);

    }


    @Override
    public void putToIndex(String idx, SiteSearchResult res, String resultType){
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return;
       try{



           if(res.getContentLength() ==0){
        	  // deleteFromIndex(idx, docId)
        	   return;
           }
           if(res.getTitle() ==null && res.getFileName() != null){
               res.setTitle(res.getFileName() );
           }

           /**
            *  Strip HTML out of text content
            */
           if(res.getContent()!= null && UtilMethods.isSet( res.getMimeType() ) && res.getMimeType().contains("text/")){
        	   res.getMap().put("content_raw", res.getContent());
        	   res.setContent(res.getContent().replaceAll("\\<.*?\\>", ""));

           }
           String desc= res.getDescription() ;

           if(!UtilMethods.isSet( res.getDescription() ) && UtilMethods.isSet( res.getContent())){
               desc = UtilMethods.prettyShortenString(res.getContent(), 500);
           }
           res.setDescription(desc);

           if(res.getMap().containsKey("keywords") && res.getMap().containsKey("seokeywords")){
               res.setKeywords((String)res.getMap().get("seokeywords"));
           }else{
               res.setKeywords((String)res.getMap().get("keywords"));
           }

           Logger.info(this.getClass(), "writing from : " + idx + " type: " + resultType  + " url:" + res.getUrl());
           String json = new ESMappingAPIImpl().toJsonString(res.getMap());

           IndexRequest request = new IndexRequest(indexApi.getNameWithClusterIDPrefix(idx));
           request.id(res.getId());
           request.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
           request.source(json, XContentType.JSON);
           request.setRefreshPolicy(RefreshPolicy.IMMEDIATE);
           RestHighLevelClientProvider.getInstance().getClient()
                   .index(request, RequestOptions.DEFAULT);

        } catch (Exception e) {
            Logger.error(ESSiteSearchAPI.class, e.getMessage(), e);
        }

    }


    @Override
    public SiteSearchResult getFromIndex(final String index, final String id){
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return null;

        try {
            GetResponse response = RestHighLevelClientProvider.getInstance()
                    .getClient().get(new GetRequest(indexApi.getNameWithClusterIDPrefix(index), id), RequestOptions.DEFAULT);
            if (response.isExists()) {
                SiteSearchResult ssr = new SiteSearchResult(response.getSource());
                ssr.setScore(1);
                return ssr;
            }
        }catch (IOException e){
            Logger.error(ESSiteSearchAPI.class, e.getMessage(), e);
        }

        return null;
    }


    @Override
    public void putToIndex(String idx, List<SiteSearchResult> res, String resultType){
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return;
         for(SiteSearchResult r : res){
             putToIndex(idx, r, resultType);
         }

    }

    @Override
    public void deleteFromIndex(final String idx, final String docId){
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return;
       try{

           Logger.info(this.getClass(), "deleting from : " + idx  + " url:" + docId);
           final RestHighLevelClient client = RestHighLevelClientProvider.getInstance().getClient();
           final DeleteRequest request = new DeleteRequest(indexApi.getNameWithClusterIDPrefix(idx), docId);
           request.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
           request.setRefreshPolicy(RefreshPolicy.IMMEDIATE);
           client.delete(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            Logger.error(ESIndexAPI.class, e.getMessage(), e);
        }
    }
    @Override
    public void pauseTask(String taskName) throws SchedulerException {
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return;

        ScheduledTask t = getTask(taskName);

        QuartzUtils.pauseJob(t.getJobName(), ES_SITE_SEARCH_NAME);


    }

    @Override
    public SiteSearchPublishStatus getTaskProgress(String taskName) throws SchedulerException{
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return null;

        TaskRuntimeValues trv = QuartzUtils.getTaskRuntimeValues(taskName, ES_SITE_SEARCH_NAME);
        if(trv==null || !(trv instanceof SiteSearchPublishStatus) ){
            QuartzUtils.setTaskRuntimeValues(taskName, ES_SITE_SEARCH_NAME, new SiteSearchPublishStatus());
        }
        return (SiteSearchPublishStatus) QuartzUtils.getTaskRuntimeValues(taskName, ES_SITE_SEARCH_NAME);

    }
    @Override
    public boolean isTaskRunning(String jobName) throws SchedulerException{

        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return false;

        return QuartzUtils.isJobRunning(jobName, ES_SITE_SEARCH_NAME);

    }

    @Override
    public void executeTaskNow(SiteSearchConfig config) throws SchedulerException, ParseException, ClassNotFoundException{
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return;


        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 10);

        String cron = new SimpleDateFormat("ss mm H d M ? yyyy").format(cal.getTime());

        config.setCronExpression(cron);


        scheduleTask(config);
    }

    @Override
    public Map<String, Aggregation> getAggregations ( String indexName, String query ) throws DotDataException {

        RestHighLevelClient client = RestHighLevelClientProvider.getInstance().getClient();
        if ( indexName == null ) {
            indexName = indiciesAPI.loadIndicies().getSiteSearch();
        }
        if ( !indexApi.indexExists( indexName ) ) {
            // try using it as an alias
            indexName = indexApi.getAliasToIndexMap( listIndices() ).get( indexName );
        }

        if ( indexName == null || !IndexType.SITE_SEARCH.is(indexName) ) {
            throw new ElasticsearchException( indexName + " is not a sitesearch index or alias" );
        }

        //https://github.com/elasticsearch/elasticsearch/issues/2980
        if ( query.contains( "/" ) ) {
            query = query.replaceAll( "/", "\\\\\\\\/" );
        }

        try {
            final SearchRequest request = new SearchRequest(indexApi.getNameWithClusterIDPrefix(indexName));
            request.source(SearchSourceBuilderUtil.getSearchSourceBuilder(query)
                    .timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS)));

            final SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            return response.getAggregations().asMap();
        } catch ( ElasticsearchException | IOException e ) {
            Logger.error( this.getClass(), "Error getting aggregations for query.\n" + e.getMessage(), e );
            throw new ElasticsearchException( "Error getting aggregations for query.\n" + e.getMessage(), e );
        }
    }


    @Override
    public Map<String, Aggregation> getFacets ( String indexName, String query ) throws DotDataException {

        final RestHighLevelClient client = RestHighLevelClientProvider.getInstance().getClient();
        if ( indexName == null ) {
            indexName = indiciesAPI.loadIndicies().getSiteSearch();
        }
        if ( !indexApi.indexExists( indexName ) ) {
            // try using it as an alias
            indexName = indexApi.getAliasToIndexMap( listIndices() ).get( indexName );
        }

        if ( indexName == null || !IndexType.SITE_SEARCH.is(indexName ) ) {
            throw new ElasticsearchException( indexName + " is not a sitesearch index or alias" );
        }

        //https://github.com/elasticsearch/elasticsearch/issues/2980
        if ( query.contains( "/" ) ) {
            query = query.replaceAll( "/", "\\\\\\\\/" );
        }

        try {
            final SearchRequest request = new SearchRequest(indexApi.getNameWithClusterIDPrefix(indexName));
            request.source(SearchSourceBuilderUtil.getSearchSourceBuilder(query)
                    .timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS)));

            final SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            return response.getAggregations().asMap();
        } catch ( ElasticsearchException | IOException e ) {
            Logger.error( this.getClass(), "Error getting Facets for query.\n"  + e.getMessage(), e );
            throw new ElasticsearchException( "Error getting Facets for query.\n"  + e.getMessage(), e );
        }
    }

    /**
     * This method is responsible for deleting old Site Search indices.
     * It performs the following steps:
     * 1. Retrieves all Site Search indices.
     * 2. Removes the default Site Search index from the list of indices to be removed.
     * 3. Retrieves all indices with an alias and removes them from the list of indices to be removed.
     * 4. Removes indices which were created in the last day from the list of indices to be removed.
     * 5. If there are any indices left to be removed, it logs their names and deletes them.
     */
    public void deleteOldSiteSearchIndices(){
        //Get All SiteSearch Indices
        final List<String> indicesToRemove = new ArrayList<>();

        indicesToRemove.addAll(listIndices());

        //Remove Default SiteSearch Index
        final IndiciesInfo info = Try.of(()->APILocator.getIndiciesAPI().loadIndicies())
                .getOrNull();

        if(info!=null) {
            indicesToRemove.remove(info.getSiteSearch());
        }

        //Get All Indices with an Alias
        final List<String> listOfIndicesWithAlias = new ArrayList<>(indexApi.getIndexAlias(indicesToRemove).keySet());

        //Remove Indices with an Alias from the list of indicesToRemove
        indicesToRemove.removeAll(listOfIndicesWithAlias);

        //Remove Indices which were created in the last day from the list of indicesToRemove
        final Date yesterdayDate = Date.from(Instant.now().minus(Duration.ofDays(1)));
        final String yesterdayDateTimestamp = ContentletIndexAPIImpl.timestampFormatter.format(yesterdayDate);
        final long yesterdayDateLong = Long.parseLong(yesterdayDateTimestamp);

        final List<String> listOfIndicesCreatedInTheLast24Hours = new ArrayList<>();
        for(final String index : indicesToRemove){
            final String indexTimestamp = index.split("_")[1];
            final long indexTimestampLong = Long.parseLong(indexTimestamp);
            if(indexTimestampLong >= yesterdayDateLong){
                listOfIndicesCreatedInTheLast24Hours.add(index);
            }
        }

        indicesToRemove.removeAll(listOfIndicesCreatedInTheLast24Hours);

        if(!indicesToRemove.isEmpty()) {
            Logger.info(this.getClass(), "The following indices will be deleted: " + String.join(",", indicesToRemove));
            indexApi.deleteMultiple(indicesToRemove.toArray(new String[0]));
        }
    }




}
