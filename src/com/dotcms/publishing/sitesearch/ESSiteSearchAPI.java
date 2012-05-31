package com.dotcms.publishing.sitesearch;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.quartz.SchedulerException;

import com.dotcms.content.elasticsearch.business.ESContentFactoryImpl;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.publishing.sitesearch.job.SiteSearchJobProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.quartz.TaskRuntimeValues;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.ibm.icu.text.SimpleDateFormat;

public class ESSiteSearchAPI implements SiteSearchAPI{

	private static final ESIndexAPI iapi  = new ESIndexAPI();
    private static final ESMappingAPIImpl mappingAPI = new ESMappingAPIImpl();
	/**
	 * List of all sitesearch indicies
	 * @return
	 */
    @Override
	public  List<String> listIndices() {

		List<String> indices=new ArrayList<String>();
		for(String x:iapi.listIndices()){
			if(x.startsWith(ES_SITE_SEARCH_NAME)){
				indices.add(x);
			}
			
		}
		Collections.sort(indices);
		Collections.reverse(indices);
		return indices;
	}
	@Override
	public SiteSearchResults search(String query, String sort, int start, int rows) throws ElasticSearchException{
		SiteSearchResults results = new SiteSearchResults();
		if(query ==null){
			results.setError("null query");
			return results;
		}
		
		try{

		
			results =  search(APILocator.getIndiciesAPI().loadIndicies().site_search, query, null, start, rows);

		
		}
		catch(Exception e){
			results.setError(e.getMessage());

			
		}
		
		return results;
		
		
		
	}
	
	@Override
	public SiteSearchResults search(String indexName, String query, String sort, int offset, int limit) {
		SiteSearchResults results = new SiteSearchResults();
		if(indexName ==null){
			return results;
		}
		boolean isJson = StringUtils.isJson(query);


		results.setQuery(query);
		
	    Client client=new ESClient().getClient();
	    SearchResponse resp = null;
        try {
    		if(indexName ==null){
    			indexName = APILocator.getIndiciesAPI().loadIndicies().site_search;
    		}
        	if(!indexName.startsWith(ES_SITE_SEARCH_NAME)){
        		throw new ElasticSearchException(indexName + " is not a sitesearch index");
        		
        	}
        	
        	
        	
            SearchRequestBuilder srb = null;

            
            if(!isJson){
                srb = client.prepareSearch()
                        .setQuery(QueryBuilders.queryString(query))
                        .setIndices(indexName);
	            if(limit>0)
	                srb.setSize(limit);
	            if(offset>0)
	                srb.setFrom(offset);
            }else{
                srb = client.prepareSearch()
                        .setIndices(indexName);
            	srb.setSource(query);
            	
            }

            try{
            	resp = srb.execute().actionGet();
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
    	    results.setQuery(srb.toString());
    	    ObjectMapper mapper = new ObjectMapper();
    	    for(SearchHit hit : hits.hits()){

    	    		
    				SiteSearchResult ssr = new SiteSearchResult(hit.getSource());
    				ssr.setScore(hit.getScore());
    				
    				
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
        IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();

        if(indexName.startsWith(ES_SITE_SEARCH_NAME)) {
        	info.site_search=indexName;
        }

        APILocator.getIndiciesAPI().point(info);
    }
	
	@Override
    public void deactivateIndex(String indexName) throws DotDataException, IOException {
        IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();

        if(indexName.startsWith(ES_SITE_SEARCH_NAME)) {
        	info.site_search=null;
        }

        APILocator.getIndiciesAPI().point(info);
    }
	
	@Override
	public synchronized boolean createSiteSearchIndex(String indexName, String alias, int shards) throws ElasticSearchException, IOException {
		if(indexName==null){
			return false;
		}
		indexName=indexName.toLowerCase();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		URL url = classLoader.getResource("es-sitesearch-settings.json");
        // read settings and mappings
		String settings = new String(com.liferay.util.FileUtil.getBytes(new File(url.getPath())));
		url = classLoader.getResource("es-sitesearch-mapping.json");
		String mapping = new String(com.liferay.util.FileUtil.getBytes(new File(url.getPath())));
		
		
		//create index
		CreateIndexResponse cir = iapi.createIndex(indexName, settings, shards);
		int i = 0;
		while(!cir.acknowledged()){
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if(i++ > 300){
				throw new ElasticSearchException("index timed out creating");
			}
		}
		
		iapi.createAlias(indexName, alias);

		//put mappings
		mappingAPI.putMapping(indexName, ES_SITE_SEARCH_MAPPING, mapping);
		
		return true;
	}
	
	@Override
	public List<ScheduledTask> getTasks() throws SchedulerException{

		List<ScheduledTask> tasks = QuartzUtils.getStandardScheduledTasks(ES_SITE_SEARCH_NAME);
		return tasks;

		
	}
	
	@Override
	public ScheduledTask getTask(String taskName) throws SchedulerException{

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
		String name = config.getJobName();
		name.replace('"', '\'');
		name.replace("'", "");
		String cronString = config.getCronExpression();

		


		ScheduledTask	task = new CronScheduledTask(name, ES_SITE_SEARCH_NAME,"Site Search ", SiteSearchJobProxy.class.getCanonicalName(),new Date(),null, 1,config,cronString);
		task.setSequentialScheduled(true);
		
		QuartzUtils.scheduleTask(task);
		
	}
	
	@Override
	public void deleteTask(String taskName) throws SchedulerException{
		
		ScheduledTask t = getTask(taskName);

		//Pause and remove any current jobs in the group
		QuartzUtils.pauseJob(t.getJobName(), ES_SITE_SEARCH_NAME);
		QuartzUtils.removeTaskRuntimeValues(t.getJobName(), ES_SITE_SEARCH_NAME);
		QuartzUtils.removeJob(t.getJobName(), ES_SITE_SEARCH_NAME);	
			
	}
	
    
    @Override
    public void putToIndex(String idx, SiteSearchResult res){
	   try{
		   
		   
		   
		   
		   if(res.getTitle() ==null && res.getFileName() != null){
			   res.setTitle(res.getFileName() );
		   }
		   
		   if(!UtilMethods.isSet( res.getDescription() ) && UtilMethods.isSet( res.getContent())){
			   
			   String x = res.getContent();
			   String noHTMLString = res.getContent().replaceAll("\\<.*?\\>", "");
			   
			   
			   
			   
			   res.setDescription(UtilMethods.prettyShortenString(noHTMLString, 500));
		   }
		   
		   Logger.info(this.getClass(), "writing from : " + idx  + " url:" + res.getUrl());
		   Client client=new ESClient().getClient();
		   String json = new ESMappingAPIImpl().toJsonString(res.getMap());
		   IndexResponse response = client.prepareIndex(idx, ES_SITE_SEARCH_MAPPING, res.getId())
			        .setSource(json)
			        .execute()
			        .actionGet();
		   
		} catch (Exception e) {
		    Logger.error(ESIndexAPI.class, e.getMessage(), e);
		}

    }
    
    
    @Override
    public SiteSearchResult getFromIndex(String index, String id){



	   GetResponse response = new ESClient().getClient().prepareGet(index, ES_SITE_SEARCH_MAPPING, id)
		        .execute()
		        .actionGet();
	   	if(response.exists()){
			SiteSearchResult ssr = new SiteSearchResult(response.getSource());
			ssr.setScore(1);
			return ssr;
	   	}
	   	else {
	   		return null;
	   	}

    }
    
    
    @Override
    public void putToIndex(String idx, List<SiteSearchResult> res){
	 for(SiteSearchResult r : res){
		 putToIndex(idx, r);
	 }
    	
    }
    
    @Override
    public void deleteFromIndex(String idx,String docId){
	   try{
		   
		   Logger.info(this.getClass(), "deleting from : " + idx  + " url:" + docId);
		   Client client=new ESClient().getClient();
		   DeleteResponse response = client.prepareDelete(idx, ES_SITE_SEARCH_MAPPING, docId)
			        .execute()
			        .actionGet();
		} catch (Exception e) {
		    Logger.error(ESIndexAPI.class, e.getMessage(), e);
		}
    }
	@Override
	public void pauseTask(String taskName) throws SchedulerException {
		
		ScheduledTask t = getTask(taskName);

		QuartzUtils.pauseJob(t.getJobName(), ES_SITE_SEARCH_NAME);

		
	}
    
	@Override
	public SiteSearchPublishStatus getTaskProgress(String taskName) throws SchedulerException{
		

		TaskRuntimeValues trv = QuartzUtils.getTaskRuntimeValues(taskName, ES_SITE_SEARCH_NAME);
		if(trv==null || !(trv instanceof SiteSearchPublishStatus) ){
			QuartzUtils.setTaskRuntimeValues(taskName, ES_SITE_SEARCH_NAME, new SiteSearchPublishStatus());
		}
		return (SiteSearchPublishStatus) QuartzUtils.getTaskRuntimeValues(taskName, ES_SITE_SEARCH_NAME);
			
			
		
		
		
		
	}
	@Override
	public boolean isTaskRunning(String jobName) throws SchedulerException{
		
		return QuartzUtils.isJobRunning(jobName, ES_SITE_SEARCH_NAME);
		
	}
	
	@Override
	public void executeTaskNow(SiteSearchConfig config) throws SchedulerException, ParseException, ClassNotFoundException{


		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, 10);
		
		String cron = new SimpleDateFormat("ss mm H d M ? yyyy").format(cal.getTime());
		
		config.setCronExpression(cron);


		scheduleTask(config);
	}
	
	
	
}
