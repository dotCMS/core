package com.dotmarketing.sitesearch.job;

import java.util.ArrayList;
import java.util.List;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.sitesearch.CrawlerUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class SiteSearchJobImpl {

	@SuppressWarnings("unchecked")
	public void run(JobExecutionContext jobContext) throws JobExecutionException {

		JobDataMap dataMap = jobContext.getJobDetail().getJobDataMap();
		String pathsToIgnore  = dataMap.getString("pathsToIgnore");
		String extToignore    = dataMap.getString("extToIgnore");
		boolean followQueryString = (Boolean)dataMap.get("followQueryString");
		boolean indexAll = (Boolean)dataMap.get("indexAll");
		boolean squentiallyScheduled = (Boolean)dataMap.get("squentiallyScheduled");
		boolean runJobAfterSeq = (Boolean)dataMap.get("runJobAfterSeq");
		List<String> indexHosts = (List<String>)dataMap.get("hosts");
		String port    = dataMap.getString("port");
		

		HostAPI hostAPI = APILocator.getHostAPI();
		UserAPI userAPI = APILocator.getUserAPI();

		CrawlerUtil crawlerUtil = new CrawlerUtil();

		if(UtilMethods.isSet(pathsToIgnore)){
			List<String> paths = new ArrayList<String>();
			for(String path :pathsToIgnore.split(",")){
				path = path.trim().toLowerCase();
				if(UtilMethods.isSet(path) && !path.contains("*")){
					paths.add(path);
				}
			}
			crawlerUtil.setPathsToIgnore(paths);
		}

		if(UtilMethods.isSet(extToignore)){
			List<String> exts = new ArrayList<String>();
			for(String ext :extToignore.split(",")){
				ext = ext.trim().toLowerCase();
				if(UtilMethods.isSet(ext) && !ext.contains("*")){
					exts.add(ext);
				}
			}
			crawlerUtil.setExtensionsToIgnore(exts);
		}

		if(indexAll || UtilMethods.isSet(indexHosts)){
			List<Host> hostsToIndex = new ArrayList<Host>();
			if(!indexAll){
			for(String hostId : indexHosts){
				try {
					Host host = hostAPI.find(hostId, userAPI.getSystemUser(), false);
					if(host!=null && !host.isSystemHost() && host.isLive()){
						hostsToIndex.add(host);
					}
				} catch (Exception e) {
					Logger.error(this, "Unable to get Host with id = " + hostId);
				} 
			}
			}else{
				try {
					List<Host> allHosts = hostAPI.findAll(userAPI.getSystemUser(), false);
					for(Host host : allHosts){
						if(!host.isSystemHost() && !host.isArchived()){
							hostsToIndex.add(host);
						}
					}
				} catch (Exception e) {
					Logger.error(this, "Exception trying to index All hosts caused by " + e);
				} 
			}
			crawlerUtil.setHosts(hostsToIndex);
		}
		
		if(UtilMethods.isSet(port)){
			try{
			    Integer.valueOf(port);
			    crawlerUtil.setPortNumber(port);
			}catch(NumberFormatException e){
				Logger.error(this, "Invalid port number " + e);
			}
		}
			

		crawlerUtil.setQueryStringEnabled(followQueryString);
		crawlerUtil.index();
		if (squentiallyScheduled && runJobAfterSeq) {
			try {
				QuartzUtils.resumeJob("site-search", "site-search-group");
			} catch (SchedulerException e) {
				throw new JobExecutionException(e);
			}
		}
	}

}
