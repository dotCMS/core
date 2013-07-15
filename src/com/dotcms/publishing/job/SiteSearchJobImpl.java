package com.dotcms.publishing.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.elasticsearch.ElasticSearchException;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.publishing.bundlers.FileAssetBundler;
import com.dotcms.enterprise.publishing.bundlers.StaticHTMLPageBundler;
import com.dotcms.enterprise.publishing.bundlers.URLMapBundler;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchConfig;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.sitesearch.model.SiteSearchAudit;
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
        
        HibernateUtil.startTransaction();

        JobDataMap dataMap = jobContext.getJobDetail().getJobDataMap();
        
        String jobId=(String)dataMap.get("JOB_ID");
        if(jobId==null) {
            jobId=dataMap.getString("QUARTZ_JOB_NAME");
        }
        
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
            path = path.replace('\n', '\r');
            for(String x : path.split("\r")){
                if(UtilMethods.isSet(x)){
                    paths.add(x);
                }
            }   
        }
        
        Date startDate,endDate;
        if(incremental) {
            endDate=jobContext.getFireTime();
            
            startDate=null;
            try {
                List<SiteSearchAudit> recentAudits = APILocator.getSiteSearchAuditAPI().findRecentAudits(jobId, 0, 1);
                if(recentAudits.size()>0)
                    startDate=recentAudits.get(0).getFireDate();
            }
            catch(Exception ex) {
                Logger.warn(this, "can't determine last audit entry for this job",ex);
            }
        }
        else {
            // set null explicitly just in case
            startDate=endDate=null;
        }
        
        String[] languageToIndex=(String[])dataMap.get("langToIndex");
        int counter = 0;  
        String indexName = null;
        boolean createNew=false;
        for(String lang : languageToIndex) {
        	counter = counter + 1;
            SiteSearchConfig config = new SiteSearchConfig();
            config.setJobId(jobId);
            config.setLanguage(Long.parseLong(lang));       
            
            config.setJobName(dataMap.getString("QUARTZ_JOB_NAME"));
    
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
            
            config.setStartDate(startDate);
            config.setEndDate(endDate);
            config.setIncremental(incremental);
            config.setUser(userToRun);
            if(include) {
                config.setIncludePatterns(paths);
            }
            else {
                config.setExcludePatterns(paths);
            }
            
            APILocator.getPublisherAPI().publish(config,status);
            
        }
        
        int filesCount=0,pagesCount=0,urlmapCount=0;
        for(BundlerStatus bs : status.getBundlerStatuses()) {
            if(bs.getBundlerClass().equals(StaticHTMLPageBundler.class.getName()))
                pagesCount+=bs.getTotal();
            else if(bs.getBundlerClass().equals(FileAssetBundler.class.getName()))
                filesCount+=bs.getTotal();
            else if(bs.getBundlerClass().equals(URLMapBundler.class.getName()))
                urlmapCount+=bs.getTotal();
        }
        
        
        try {
            SiteSearchAudit audit=new SiteSearchAudit();
            audit.setPagesCount(pagesCount);
            audit.setFilesCount(filesCount);
            audit.setUrlmapsCount(urlmapCount);
            audit.setAllHosts(indexAll);
            audit.setFireDate(jobContext.getFireTime());
            audit.setHostList(UtilMethods.join(indexHosts,","));
            audit.setIncremental(incremental);
            audit.setStartDate(startDate);
            audit.setEndDate(endDate);
            audit.setIndexName(indexName);
            audit.setJobId(jobId);
            audit.setJobName(dataMap.getString("QUARTZ_JOB_NAME"));
            audit.setLangList(UtilMethods.join(languageToIndex,","));
            audit.setPath(paths.size()>0 ? UtilMethods.join(paths,",") : "/*");
            audit.setPathInclude(include);
            APILocator.getSiteSearchAuditAPI().save(audit);
        }
        catch(DotDataException ex) {
            Logger.warn(this, "can't save audit data",ex);
        }
        finally {
            HibernateUtil.closeSession();
        }
    }

}