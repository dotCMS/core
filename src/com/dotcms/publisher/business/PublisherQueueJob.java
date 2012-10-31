package com.dotcms.publisher.business;

import java.util.ArrayList;
import java.util.Date;
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

	private static final String IDENTIFIER = "identifier:";
	private static final int _ASSET_LENGTH_LIMIT = 20;

	@SuppressWarnings("rawtypes")
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

			List<Map<String,Object>> bundles = pubAPI.getQueueBundleIds();
			List<Map<String,Object>> tempBundleContents = null;
			PublishAuditStatus status = null;
			PublishAuditHistory historyPojo = null;
			String tempBundleId = null;
			boolean acceptAll = true;

			for(Map<String,Object> bundle: bundles) {
				Date publishDate = (Date) bundle.get("publish_date");
				
				if(publishDate.before(new Date()) || acceptAll) {
					tempBundleId = (String)bundle.get("bundle_id");
					tempBundleContents = pubAPI.getQueueElementsByBundleId(tempBundleId);
					
					//Setting Audit objects
					//History
					historyPojo = new PublishAuditHistory();
					//Retriving assets
					List<String> assets = new ArrayList<String>();
					
					for(Map<String,Object> c : tempBundleContents) {
						assets.add((String) c.get("asset"));
					}
					historyPojo.setAssets(assets);
					
					//Status
					status =  new PublishAuditStatus(tempBundleId);
					status.setStatusPojo(historyPojo);
					
					//Insert in Audit table
					pubAuditAPI.insertPublishAuditStatus(status);
					
					//Queries creation
					pconf.setLuceneQueries(prepareQueries(tempBundleContents));
					pconf.setId(tempBundleId);
					pconf.setUser(APILocator.getUserAPI().getSystemUser());
					pconf.runNow();
	
					pconf.setPublishers(clazz);
					pconf.setIncremental(false);
					pconf.setLiveOnly(false);
					pconf.setBundlers(bundler);
					
					if(((Long) bundle.get("operation")).intValue() == PublisherAPI.ADD_OR_UPDATE_ELEMENT)
						pconf.setOperation(PushPublisherConfig.Operation.PUBLISH);
					else
						pconf.setOperation(PushPublisherConfig.Operation.UNPUBLISH);
					
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
	
//	private updateAuditStatus() {
//		ClientConfig clientConfig = new DefaultClientConfig();
//        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
//        client = Client.create(clientConfig);
//        webResource = client.resource("http://localhost:8080/rest-service");
//	 
//        webResource
//        .path("cluster")
//        .path(name)
//        .accept(MediaType.APPLICATION_JSON)
//        .get(Cluster.class);
//	}
	
	private List<String> prepareQueries(List<Map<String,Object>> bundle) {
		StringBuilder assetBuffer = new StringBuilder();
		List<String> assets;
		assets = new ArrayList<String>();
		
		if(bundle.size() == 1) {
			assetBuffer.append("+"+IDENTIFIER+(String) bundle.get(0).get("asset"));
			
			assets.add(assetBuffer.toString() +" +live:true");
			assets.add(assetBuffer.toString() +" +working:true");
			
		} else {
			int counter = 1;
			Map<String,Object> c = null;
			for(int ii = 0; ii < bundle.size(); ii++) {
				c = bundle.get(ii);
				
				assetBuffer.append(IDENTIFIER+c.get("asset"));
				assetBuffer.append(" ");
				
				if(counter == _ASSET_LENGTH_LIMIT || (ii+1 == bundle.size())) {
					assets.add("+("+assetBuffer.toString()+") +live:true");
					assets.add("+("+assetBuffer.toString()+") +working:true");
					
					assetBuffer = new StringBuilder();
					counter = 0;
				} else
					counter++;
			}
		}
		return assets;
	}
}
