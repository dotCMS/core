package com.dotmarketing.sitesearch.job;

import java.util.ArrayList;
import java.util.List;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.sitesearch.CrawlerUtil;
import com.dotmarketing.util.UtilMethods;

public class SiteSearchJobImpl {

	@SuppressWarnings("unchecked")
	public void run(JobExecutionContext jobContext) throws JobExecutionException {

		JobDataMap dataMap = jobContext.getJobDetail().getJobDataMap();
		String pathsToIgnore  = dataMap.getString("pathsToIgnore");
		String pathsToInclude  = dataMap.getString("pathsToInclude");
		String extToignore    = dataMap.getString("extToIgnore");
		String writeToIndex    = dataMap.getString("writeToIndex");
		boolean squentiallyScheduled = (Boolean)dataMap.get("squentiallyScheduled");
		boolean runJobAfterSeq = (Boolean)dataMap.get("runJobAfterSeq");
		List<String> indexHosts = (List<String>)dataMap.get("hosts");

		
		
		
		
		

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
