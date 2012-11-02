package com.dotcms.publisher.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublisherEndpointAPI;
import com.dotcms.publisher.myTest.PushPublisher;
import com.dotcms.publisher.myTest.PushPublisherBundler;
import com.dotcms.publisher.myTest.PushPublisherConfig;
import com.dotcms.publishing.IBundler;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

/**
 * This class read the publishing_queue table and send bundles to some endpoints
 * @author Alberto
 *
 */
public class PublisherQueueJob implements StatefulJob {

	private static final String IDENTIFIER = "identifier:";
	private static final int _ASSET_LENGTH_LIMIT = 20;
	
	private PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance(); 
	PublisherEndpointAPI endpointAPI = APILocator.getPublisherEndpointAPI();

	@SuppressWarnings("rawtypes")
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		try {
			Logger.info(PublisherQueueJob.class, "Started PublishQueue Job");
			PublisherAPI pubAPI = PublisherAPI.getInstance();  
			
			PushPublisherConfig pconf = new PushPublisherConfig();
			List<Class> clazz = new ArrayList<Class>();
			List<IBundler> bundler = new ArrayList<IBundler>();
			bundler.add(new PushPublisherBundler());
			clazz.add(PushPublisher.class);

			List<Map<String,Object>> bundles = pubAPI.getQueueBundleIds(100,0);
			List<Map<String,Object>> tempBundleContents = null;
			PublishAuditStatus status = null;
			PublishAuditHistory historyPojo = null;
			String tempBundleId = null;

			for(Map<String,Object> bundle: bundles) {
				Date publishDate = null;
				if (bundle.get("publish_date") instanceof java.util.Date) {
					publishDate = (Date) bundle.get("publish_date");
				} else if (bundle.get("publish_date") instanceof oracle.sql.TIMESTAMP){
					publishDate = new Date(((oracle.sql.TIMESTAMP) bundle.get("publish_date")).timeValue().getTime());
				}
				
				if(publishDate.before(new Date())) {
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
					pconf.setBundlers(bundler);
					pconf.setEndpoints(endpointAPI.findReceiverEndpoints());
					
					if(((Long) bundle.get("operation")).intValue() == PublisherAPI.ADD_OR_UPDATE_ELEMENT)
						pconf.setOperation(PushPublisherConfig.Operation.PUBLISH);
					else
						pconf.setOperation(PushPublisherConfig.Operation.UNPUBLISH);
					
					APILocator.getPublisherAPI().publish(pconf);
				}
				
			}
			
			Logger.info(PublisherQueueJob.class, "Finished PublishQueue Job");
			
			
			Logger.info(PublisherQueueJob.class, "Started PublishQueue Job - Audit update");
			updateAuditStatus();
			Logger.info(PublisherQueueJob.class, "Started PublishQueue Job - Audit update");
			
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
	
	private void updateAuditStatus() throws DotPublisherException, DotDataException {
		ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client client = Client.create(clientConfig);
        WebResource webResource = null;
        
        List<Map<String,Object>> pendingAudits = pubAuditAPI.getPendingPublishAuditStatus();
        
        for(Map<String,Object> pendingAudit: pendingAudits) {
        	//Gets endpoints list
        	PublishAuditHistory localHistory = PublishAuditHistory.getObjectFromString(
					(String) pendingAudit.get("status_pojo"));
        	Map<String, EndpointDetail> endpointsTarget = localHistory.getEndpointsMap();
        	boolean hasError = false;
        	int counter = 0;
        	Map<String, EndpointDetail> bufferMap = new HashMap<String, EndpointDetail>();
        	for(String endpointId: endpointsTarget.keySet()) {
        		EndpointDetail localDetail = endpointsTarget.get(endpointId);
        		
        		if(localDetail.getStatus() == PublishAuditStatus.Status.BUNDLE_SENT_SUCCESSFULLY.getCode()) {
	        		PublishingEndPoint target = endpointAPI.findEndpointById(endpointId);
	        		
	        		if(target != null) {
		        		webResource = client.resource(target.toURL()+"/api/auditPublishing");
		        	
			        	PublishAuditHistory remoteHistory = 
			        			PublishAuditHistory.getObjectFromString(
			        			webResource
						        .path("get")
						        .path((String) pendingAudit.get("bundle_id")).get(String.class));
			        	
			        	if(remoteHistory != null) {
				        	int statusCode = -1;
				        	Map<String, EndpointDetail> endpointsMap = remoteHistory.getEndpointsMap();
				        	for(String endpointRemote: endpointsMap.keySet()) {
				        		EndpointDetail detail = endpointsMap.get(endpointRemote);
				        		bufferMap.put(endpointId, detail);
				        		
				        		statusCode = detail.getStatus();
				        		
				        		if(statusCode != PublishAuditStatus.Status.SUCCESS.getCode())
				        			hasError = true;
				        		else
				        			counter++;
				        	}
			        	}
	        		}
        		}
	        }
        	
        	for(String bufferKey: bufferMap.keySet()) {
        		localHistory.addOrUpdateEndpoint(bufferKey, bufferMap.get(bufferKey));
        	}
        	
        	
        	if(!hasError && counter == endpointsTarget.size()) {
	        	pubAuditAPI.updatePublishAuditStatus((String) pendingAudit.get("bundle_id"), 
	        			PublishAuditStatus.Status.SUCCESS, 
	        			localHistory);
        	} else if(hasError && counter == 0) {
        		pubAuditAPI.updatePublishAuditStatus((String) pendingAudit.get("bundle_id"), 
	        			PublishAuditStatus.Status.FAILED_TO_PUBLISH, 
	        			localHistory);
        	} else {
        		pubAuditAPI.updatePublishAuditStatus((String) pendingAudit.get("bundle_id"), 
        				PublishAuditStatus.getStatusObjectByCode(
        						new Integer((Integer)pendingAudit.get("status")).intValue()), 
	        			localHistory);
        	}
        }
        
	}
	
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
