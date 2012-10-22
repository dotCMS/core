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

			List<Map<String,Object>> iresults =  null;

			
			iresults =  pubAPI.getQueueElements();

			String luceneQuery = null;
			PushPublisherConfig pconf = new PushPublisherConfig();
			@SuppressWarnings("rawtypes")
			List<Class> clazz = new ArrayList<Class>();
			List<IBundler> bundler = new ArrayList<IBundler>();
			bundler.add(new PushPublisherBundler());
			clazz.add(PushPublisher.class);
			
			for(Map<String,Object> c : iresults) {
				luceneQuery = (String)c.get("asset");
				pconf.setLuceneQuery(luceneQuery);
				pconf.setId((String) c.get("bundle_id"));
				pconf.setUser(APILocator.getUserAPI().getSystemUser());
				pconf.runNow();

				pconf.setPublishers(clazz);
				pconf.setIncremental(false);
				pconf.setLiveOnly(false);
				pconf.setBundlers(bundler);
				
				pubAuditAPI.insertPublishAuditStatus(new PublishAuditStatus(pconf.getId()));
				
				APILocator.getPublisherAPI().publish(pconf);
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
