package com.dotcms.publisher.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotcms.publisher.myTest.PushPublisher;
import com.dotcms.publisher.myTest.PushPublisherBundler;
import com.dotcms.publisher.myTest.PushPublisherConfig;
import com.dotcms.publishing.IBundler;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

/**
 * This class read the publishing_queue table and send bundles to some endpoints
 * @author Alberto
 *
 */
public class PublisherQueueJob implements StatefulJob {

	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		try {
			Logger.info(PublisherQueueJob.class, "Started PublishQueue Job");
			PublisherAPI pubAPI = PublisherAPI.getInstance();  
			PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance(); 


			PushPublisherConfig pconf = new PushPublisherConfig();
			List<Class> clazz = new ArrayList<Class>();
			List<IBundler> bundler = new ArrayList<IBundler>();
			bundler.add(new PushPublisherBundler());
			clazz.add(PushPublisher.class);

			List<Map<String,Object>> bundleIds = pubAPI.getQueueBundleIds();
			List<Map<String,Object>> tempBundleContents = null;
			PublishAuditStatus status = null;
			PublishAuditHistory historyPojo = null;
			String tempBundleId = null;

			for(Map<String,Object> bundleId: bundleIds) {
				tempBundleId = (String)bundleId.get("bundle_id");
				tempBundleContents = pubAPI.getQueueElementsByBundleId(tempBundleId);
				
				//Setting Audit objects
				//History
				historyPojo = new PublishAuditHistory();
				//Retriving assets
				List<String> assets = new ArrayList<String>();
				
				
				StringBuilder luceneQuery = new StringBuilder();
				for(Map<String,Object> c : tempBundleContents) {
					assets.add((String) c.get("asset"));
					
					luceneQuery.append("identifier:"+(String) c.get("asset"));
					luceneQuery.append(" ");
					
				}
				
				historyPojo.setAssets(assets);
				
				
				//Status
				status =  new PublishAuditStatus(tempBundleId);
				status.setStatusPojo(historyPojo);
				
				//Insert in Audit table
				pubAuditAPI.insertPublishAuditStatus(status);
				
				if(tempBundleContents.size() > 1)
					pconf.setLuceneQuery("+("+luceneQuery.toString()+")");
				else
					pconf.setLuceneQuery("+"+luceneQuery.toString());
				
				if(luceneQuery.toString().length() > 3) {
					pconf.setId(tempBundleId);
					pconf.setUser(APILocator.getUserAPI().getSystemUser());
					pconf.runNow();
	
					pconf.setPublishers(clazz);
					pconf.setIncremental(false);
					pconf.setLiveOnly(false);
					pconf.setBundlers(bundler);
					
					APILocator.getPublisherAPI().publish(pconf);
				}
				
			}
			
			Logger.info(PublisherQueueJob.class, "Finished PublishQueue Job");
		} catch (NumberFormatException e) {
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
		} catch (DotDataException e) {
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
		} catch (DotPublisherException e) {
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
		} catch (Exception e) {
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
		}

	}
}
