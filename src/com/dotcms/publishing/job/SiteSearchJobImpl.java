package com.dotcms.publishing.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.DateTimeZone;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.statistical.StatisticalFacet;
import org.elasticsearch.search.facet.statistical.StatisticalFacetBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchConfig;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class SiteSearchJobImpl {
    private PublishStatus status = new PublishStatus();
    public PublishStatus getStatus() {
        return status;
    }
    public void setStatus(PublishStatus status) {
        this.status = status;
    }
    @SuppressWarnings("unchecked")
    public void run(JobExecutionContext jobContext) throws JobExecutionException, DotPublishingException, DotDataException, DotSecurityException, ElasticSearchException, IOException {
        if(LicenseUtil.getLevel()<200)
            return;
                    
        JobDataMap dataMap = jobContext.getJobDetail().getJobDataMap();
        List<Host> selectedHosts = new ArrayList<Host>();
        boolean indexAll = UtilMethods.isSet((String) dataMap.get("indexAll")) ? true : false;
        String[] indexHosts = null;
        Object obj = (dataMap.get("indexhost") != null) ?dataMap.get("indexhost") : new String[0];
        if(obj instanceof String){
            indexHosts = new String[] {(String) obj};
        }
        else{
            indexHosts = (String[]) obj;
        }

        
        
        boolean incremental = dataMap.getString("incremental") != null;
        
        User userToRun = APILocator.getUserAPI().getSystemUser();

        boolean include = ("all".equals(dataMap.getString("includeExclude")) || "include".equals(dataMap.getString("includeExclude")));
        
        String path = dataMap.getString("paths");
        List<String> paths = new ArrayList<String>();
        if(path != null){
            path = path.replace(',', '\r');
            for(String x : path.split("\r")){
                if(UtilMethods.isSet(x)){
                    paths.add(x);
                }
            }   
        }
        
        String[] languageToIndex=(String[])dataMap.get("langToIndex");
        int counter = 0;  
        String indexName = null;
        boolean createNew=false;
        for(String lang : languageToIndex) {
        	counter = counter + 1;
            SiteSearchConfig config = new SiteSearchConfig();
            
            config.setLanguage(Long.parseLong(lang));       
            
            config.setJobId(dataMap.getString("QUARTZ_JOB_NAME"));
    
            List<Host> hosts=new ArrayList<Host>();
    
            if(indexAll){
                    hosts = APILocator.getHostAPI().findAll(userToRun, true);               
            }else{
                
                for(String h : indexHosts){
                    hosts.add(APILocator.getHostAPI().find(h, userToRun, true));
                }
                
            }
    
            config.setHosts( hosts);        
            
            // reuse or create new indexes as needed
            String indexAlias = dataMap.getString("indexAlias");
            ESIndexAPI iapi=new ESIndexAPI();
            Map<String,String> aliasMap=iapi.getAliasToIndexMap(APILocator.getSiteSearchAPI().listIndices());
            
            if(counter==1){
	            if(indexAlias.equals(APILocator.getIndiciesAPI().loadIndicies().site_search)){
	            	Logger.info(this, "Index Alias is DEFAULT");
	                indexName = APILocator.getIndiciesAPI().loadIndicies().site_search;
	            }
	            else if(indexAlias.equals("create-new")){
	            	Logger.info(this, "Index Alias is default");
	            	createNew=true;
	                indexName = SiteSearchAPI.ES_SITE_SEARCH_NAME  + "_" + ESMappingAPIImpl.datetimeFormat.format(new Date());
	                APILocator.getSiteSearchAPI().createSiteSearchIndex(indexName, null, 1);
	            }
	            else {
	                indexName=aliasMap.get(indexAlias);
	                Logger.info(this, "Index Alias is " + indexName);
	            }
            }
            if(createNew && languageToIndex.length == counter){
            	config.setSwitchIndexWhenDone(true);
            }
            
            config.setIndexName(indexName);
    
            
            
            // if it is going to be an incremental job, write the bundle to the same folder
            // every time.  Otherwise, create a new folder using a date stamp.
            if(dataMap.get("incremental")!=null){
                config.setId(StringUtils.sanitizeCamelCase(config.getJobName()));
            }
            else{
                String x = UtilMethods.dateToJDBC(new Date()).replace(':', '-').replace(' ', '_');
                config.setId(x);
            }
            
            if(incremental) {
                config.setEndDate(jobContext.getFireTime());
                
                config.setStartDate(null);
                try {
                    /* this one is not so bad. it will take the date of the last time this job ran.
                     * the issue here is that every time we restart dotCMS it will come as null so
                     * we will do a full site search index build.
                     * 
                    SearchResponse response = new ESClient().getClient().prepareSearch().setIndices(config.getIndexName())
                        .addSort("modified", SortOrder.DESC).addField("modified").setSize(1)
                        .setQuery(QueryBuilders.matchAllQuery()).execute().actionGet();
                    if(response.hits().hits().length>0) {
                        Date maxMod = response.hits().hits()[0].field("modified").getValue();
                        config.setStartDate(maxMod);
                    }*/
                    
                    /* this approach is taking the max modified date from documents in the index (if any).
                     * that way we can avoid doing a full build every time we restart dotCMS
                     */
                    SearchResponse response = new ESClient().getClient().prepareSearch().setIndices(config.getIndexName())
                            .setQuery(QueryBuilders.matchAllQuery())
                            .addFacet(FacetBuilders.statisticalFacet("maxmod").field("modified")).execute().actionGet();
                    StatisticalFacet maxmod = (StatisticalFacet)response.facets().facetsAsMap().get("maxmod");
                    if(maxmod.getCount()>0)
                        config.setStartDate(new DateTime((long)maxmod.getMax(),DateTimeZone.UTC).toDate());
                    
                }
                catch(Exception ex) {
                    Logger.warn(this, "can't determine max modified date on sitesearch index "+config.getIndexName(),ex);
                }
            }
            else {
                // set null explicitly just in case
                config.setEndDate(null);
                config.setStartDate(null);
            }
            config.setIncremental(incremental);
            config.setUser(userToRun);
            if(include){
                config.setIncludePatterns(paths);
            }
            else{
                
                config.setExcludePatterns(paths);
            }
            
            APILocator.getPublisherAPI().publish(config,status);
        }
    }

}