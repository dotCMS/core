package com.dotmarketing.sitesearch.job;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.elasticsearch.ElasticSearchException;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.sitesearch.SiteSearchConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
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
		DateFormat df = new java.text.SimpleDateFormat("yyyy-M-d HH:mm:ss");
		Date start = null;
		Date end =null;
		
		try{
			String startDateStr = dataMap.getString("startDateDate") + " " +dataMap.getString("startDateTime").replaceAll("T",""); 
			start = df.parse(startDateStr);
		}
		catch(Exception e){
			
		}
		try{
			String endDateStr = dataMap.getString("endDateDate") + " " +dataMap.getString("endDateTime").replaceAll("T",""); 
			end = df.parse(endDateStr);
		}
		catch(Exception e){
			
		}
		
		
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
		
		

		
		SiteSearchConfig config = new SiteSearchConfig();
		boolean squentiallyScheduled = true;
		boolean runJobAfterSeq = true;

		List<Host> hosts=new ArrayList<Host>();

		if(indexAll){
				hosts = APILocator.getHostAPI().findAll(userToRun, true);				
		}else{
			
			for(String h : indexHosts){
				hosts.add(APILocator.getHostAPI().find(h, userToRun, true));
			}
			
		}

		config.setHosts( hosts);		
		

		String indexName    = dataMap.getString("indexName");
		if("DEFAULT".equals(indexName)){
			indexName = APILocator.getIndiciesAPI().loadIndicies().site_search;
		}
		if("NEWINDEX".equals(indexName)){
			indexName = "sitesearchindex_" + ESMappingAPIImpl.datetimeFormat.format(new Date());
			APILocator.getSiteSearchAPI().createSiteSearchIndex(indexName , 1);
			config.setSwitchIndexWhenDone(true);
		}
		
		config.setIndexName(indexName);

		

		

		config.setStartDate(start);
		config.setEndDate(end);
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
