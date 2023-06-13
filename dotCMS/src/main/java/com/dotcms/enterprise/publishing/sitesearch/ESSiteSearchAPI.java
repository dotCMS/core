package com.dotcms.enterprise.publishing.sitesearch;

import com.dotcms.content.elasticsearch.business.ESContentFactoryImpl;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.priv.util.SearchSourceBuilderUtil;
import com.dotcms.publishing.job.SiteSearchJobProxy;
import com.dotcms.repackage.com.ibm.icu.text.SimpleDateFormat;
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
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.quartz.SchedulerException;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;

public class ESSiteSearchAPI implements SiteSearchAPI{

    private static final ESIndexAPI indexApi = new ESIndexAPI();
    private static final ESMappingAPIImpl mappingAPI = new ESMappingAPIImpl();
    /**
     * List of all sitesearch indicies
     * @return
     */
    @Override
    public  List<String> listIndices() {

        List<String> indices=new ArrayList<String>();

        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return indices;

        for(String x: indexApi.listIndices()){
            if(x.startsWith(ES_SITE_SEARCH_NAME)){
                indices.add(x);
            }

        }
        Collections.sort(indices);
        Collections.reverse(indices);
        return indices;
    }

    @Override
    public  List<String> listClosedIndices() {

        List<String> indices=new ArrayList<String>();

        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return indices;

        for(String x: indexApi.getClosedIndexes()){
            if(x.startsWith(ES_SITE_SEARCH_NAME)){
                indices.add(x);
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


            results =  search(APILocator.getIndiciesAPI().loadIndicies().site_search, query, start, rows);


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

        Client client=new ESClient().getClient();
        SearchResponse resp = null;
        try {
            if(indexName ==null){
                indexName = APILocator.getIndiciesAPI().loadIndicies().site_search;
            }
            if(!indexName.startsWith(ES_SITE_SEARCH_NAME)){
                throw new ElasticsearchException(indexName + " is not a sitesearch index");

            }
            results.setIndex(indexName);
            SearchRequestBuilder srb;

            if(!isJson){
                srb = client.prepareSearch()
                        .setQuery( QueryBuilders
                            .queryStringQuery( query )
                            .defaultField( "*" ))
                        .setIndices( indexName );
                if ( limit > 0 )
                    srb.setSize( limit );
                if ( offset > 0 )
                    srb.setFrom( offset );

                srb.highlighter(new HighlightBuilder().field("content", 255));
            }else{
                srb = client.prepareSearch().setIndices( indexName );

                SearchSourceBuilder searchSourceBuilder = SearchSourceBuilderUtil
                        .getSearchSourceBuilder(query);

                if ( !query.contains( "highlight" ) ) {
                    srb.highlighter(new HighlightBuilder().field("content"));
                }

                srb.setSource(searchSourceBuilder);
            }

            try{
                resp = srb.execute().actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
                results.setTook(resp.getTook().toString());
            }catch (SearchPhaseExecutionException e) {
                if(e.getMessage().contains("-order_dotraw] in order to sort on")){

                }else{
                    throw e;
                }
            }

            SearchHits hits =  resp.getHits();
            results.setTotalResults(hits.getTotalHits());
            results.setMaxScore(hits.getMaxScore());
            if(!isJson){
            	results.setQuery(srb.toString());
            }


            for(SearchHit hit : hits.getHits()){


                    SiteSearchResult ssr = new SiteSearchResult(hit.getSourceAsMap());
                    ssr.setScore(hit.getScore());
                    try{
                    	ssr.setHighLight(new String[0]);
	                    Map<String, HighlightField> hl = hit.getHighlightFields();

	                    for(String key : hl.keySet()){

	                    	List<String> highlights = new ArrayList<String>();
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


    @Override
    public void activateIndex(String indexName) throws DotDataException {
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return;

        IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();

        if(indexName.startsWith(ES_SITE_SEARCH_NAME)) {
            info.site_search=indexName;
        }

        APILocator.getIndiciesAPI().point(info);
    }

    @Override
    public void deactivateIndex(String indexName) throws DotDataException, IOException {
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return;

        IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();

        if(indexName.startsWith(ES_SITE_SEARCH_NAME)) {
            info.site_search=null;
        }

        APILocator.getIndiciesAPI().point(info);
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
        mappingAPI.putMapping(indexName, ES_SITE_SEARCH_MAPPING, mapping);

        return true;
    }

    @Override
    public List<ScheduledTask> getTasks() throws SchedulerException{
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return null;

        List<ScheduledTask> tasks = QuartzUtils.getStandardScheduledTasks(ES_SITE_SEARCH_NAME);
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
           Client client=new ESClient().getClient();
           String json = new ESMappingAPIImpl().toJsonString(res.getMap());
           IndexResponse response = client.prepareIndex(idx, ES_SITE_SEARCH_MAPPING, res.getId())
                    .setSource(json, XContentType.JSON)
                    .execute()
                    .actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);

        } catch (Exception e) {
            Logger.error(ESIndexAPI.class, e.getMessage(), e);
        }

    }


    @Override
    public SiteSearchResult getFromIndex(String index, String id){
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return null;


       GetResponse response = new ESClient().getClient().prepareGet(index, ES_SITE_SEARCH_MAPPING, id)
                .execute()
                .actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
        if(response.isExists()){
            SiteSearchResult ssr = new SiteSearchResult(response.getSource());
            ssr.setScore(1);
            return ssr;
        }
        else {
            return null;
        }

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
    public void deleteFromIndex(String idx,String docId){
        if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            return;
       try{

           Logger.info(this.getClass(), "deleting from : " + idx  + " url:" + docId);
           Client client=new ESClient().getClient();
           DeleteResponse response = client.delete(client.prepareDelete(idx, ES_SITE_SEARCH_MAPPING, docId).request())
                    .actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
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

        Client client = new ESClient().getClient();
        if ( indexName == null ) {
            indexName = APILocator.getIndiciesAPI().loadIndicies().site_search;
        }
        if ( !APILocator.getESIndexAPI().indexExists( indexName ) ) {
            // try using it as an alias
            indexName = APILocator.getESIndexAPI().getAliasToIndexMap( listIndices() ).get( indexName );
        }

        if ( indexName == null || !indexName.startsWith( ES_SITE_SEARCH_NAME ) ) {
            throw new ElasticsearchException( indexName + " is not a sitesearch index or alias" );
        }

        //https://github.com/elasticsearch/elasticsearch/issues/2980
        if ( query.contains( "/" ) ) {
            query = query.replaceAll( "/", "\\\\\\\\/" );
        }

        try {
            SearchRequestBuilder srb = client.prepareSearch().setIndices( indexName );
            srb.setSource(SearchSourceBuilderUtil.getSearchSourceBuilder(query));

            SearchResponse resp = srb.execute().actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
            return resp.getAggregations().asMap();
        } catch ( ElasticsearchException | IOException e ) {
            Logger.error( this.getClass(), "Error getting aggregations for query.\n" + e.getMessage(), e );
            throw new ElasticsearchException( "Error getting aggregations for query.\n" + e.getMessage(), e );
        }
    }


    @Override
    public Map<String, Aggregation> getFacets ( String indexName, String query ) throws DotDataException {

        Client client = new ESClient().getClient();
        if ( indexName == null ) {
            indexName = APILocator.getIndiciesAPI().loadIndicies().site_search;
        }
        if ( !APILocator.getESIndexAPI().indexExists( indexName ) ) {
            // try using it as an alias
            indexName = APILocator.getESIndexAPI().getAliasToIndexMap( listIndices() ).get( indexName );
        }

        if ( indexName == null || !indexName.startsWith( ES_SITE_SEARCH_NAME ) ) {
            throw new ElasticsearchException( indexName + " is not a sitesearch index or alias" );
        }

        //https://github.com/elasticsearch/elasticsearch/issues/2980
        if ( query.contains( "/" ) ) {
            query = query.replaceAll( "/", "\\\\\\\\/" );
        }

        try {
            SearchRequestBuilder srb = client.prepareSearch().setIndices( indexName );
            srb.setSource(SearchSourceBuilderUtil.getSearchSourceBuilder(query));

            SearchResponse resp = srb.execute().actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
            return resp.getAggregations().asMap();
        } catch ( ElasticsearchException | IOException e ) {
            Logger.error( this.getClass(), "Error getting Facets for query.\n"  + e.getMessage(), e );
            throw new ElasticsearchException( "Error getting Facets for query.\n"  + e.getMessage(), e );
        }
    }






}