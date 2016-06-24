package com.dotcms.publisher.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.enterprise.publishing.PublishDateUpdater;
import com.dotcms.repackage.javax.ws.rs.client.Client;
import com.dotcms.repackage.javax.ws.rs.client.WebTarget;
import com.dotcms.repackage.org.apache.log4j.MDC;
import com.dotcms.rest.RestClientBuilder;
import com.dotmarketing.util.PushPublishLogger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

/**
 * This class read the publishing_queue table and send bundles to some environments
 * @author Alberto
 *
 */
public class PublisherQueueJob implements StatefulJob {

	private static final String BUNDLE_ID = "BundleId";
	private PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();
	private PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();
	private PublisherAPI pubAPI = PublisherAPI.getInstance();

	public static final Integer MAX_NUM_TRIES = Config.getIntProperty("PUBLISHER_QUEUE_MAX_TRIES", 3);

	/**
	 * Reads from the publishing queue table and depending of the publish date will send a bundle<br/>
	 * to publish ({@link com.dotcms.publishing.PublisherAPI#publish(com.dotcms.publishing.PublisherConfig)}).
	 *
	 * @param arg0 Containing the current job context information
	 * @throws JobExecutionException if there is an exception while executing the job.
	 * @see PublisherAPI
	 * @see PublisherAPIImpl
	 */
	@SuppressWarnings("rawtypes")
	public void execute(JobExecutionContext arg0) throws JobExecutionException {

		try {

			Logger.debug(PublisherQueueJob.class, "Started PublishQueue Job - check for publish dates");
			PublishDateUpdater.updatePublishExpireDates(arg0.getFireTime());
			Logger.debug(PublisherQueueJob.class, "Finished PublishQueue Job - check for publish/expire dates");

			Logger.debug(PublisherQueueJob.class, "Started PublishQueue Job - Audit update");
			updateAuditStatus();
			Logger.debug(PublisherQueueJob.class, "Finished PublishQueue Job - Audit update");

			//Verify if we have endpoints where to send the bundles
			List<PublishingEndPoint> endpoints = endpointAPI.getEnabledReceivingEndPoints();
			if ( endpoints != null && endpoints.size() > 0 ) {

				Logger.debug(PublisherQueueJob.class, "Started PublishQueue Job");
				PublisherAPI pubAPI = PublisherAPI.getInstance();

				List<Class> clazz = new ArrayList<Class>();
				clazz.add(PushPublisher.class);

				List<Map<String, Object>> bundles = pubAPI.getQueueBundleIdsToProcess();
				List<PublishQueueElement> tempBundleContents;
				PublishAuditStatus status;
				PublishAuditHistory historyPojo;
				String tempBundleId;

				for ( Map<String, Object> bundle : bundles ) {
					Date publishDate = (Date) bundle.get("publish_date");

					if ( publishDate.before(new Date()) ) {
						tempBundleId = (String) bundle.get("bundle_id");
						MDC.put(BUNDLE_ID, BUNDLE_ID + "=" + tempBundleId);

						try {
							PushPublishLogger.log(this.getClass(), "Pre-publish work started.");
							tempBundleContents = pubAPI.getQueueElementsByBundleId(tempBundleId);

							//Setting Audit objects
							//History
							historyPojo = new PublishAuditHistory();
							//Retriving assets
							Map<String, String> assets = new HashMap<String, String>();
							List<PublishQueueElement> assetsToPublish = new ArrayList<PublishQueueElement>();

							for ( PublishQueueElement c : tempBundleContents ) {
								assets.put(c.getAsset(), c.getType());
								assetsToPublish.add(c);
							}
							historyPojo.setAssets(assets);

							PushPublisherConfig pconf = new PushPublisherConfig();
							pconf.setAssets(assetsToPublish);

							//Status
							status = new PublishAuditStatus(tempBundleId);
							status.setStatusPojo(historyPojo);

							//Insert in Audit table
							pubAuditAPI.insertPublishAuditStatus(status);

							//Queries creation
							pconf.setLuceneQueries(PublisherUtil.prepareQueries(tempBundleContents));
							pconf.setId(tempBundleId);
							pconf.setUser(APILocator.getUserAPI().getSystemUser());
							pconf.setStartDate(new Date());
							pconf.runNow();

							pconf.setPublishers(clazz);
							//						pconf.setEndpoints(endpoints);

							if ( Integer.parseInt(bundle.get("operation").toString()) == PublisherAPI.ADD_OR_UPDATE_ELEMENT ) {
								pconf.setOperation(PushPublisherConfig.Operation.PUBLISH);
							} else {
								pconf.setOperation(PushPublisherConfig.Operation.UNPUBLISH);
							}

							PushPublishLogger.log(this.getClass(), "Pre-publish work complete.");

							try {
								APILocator.getPublisherAPI().publish(pconf);
							} catch (DotPublishingException e) {
								/*
								If we are getting errors creating the bundle we should stop trying to publish it, this is not just a connection error,
								there is something wrong with a bundler or creating the bundle.
								 */
								Logger.error(PublisherQueueJob.class, "Unable to publish Bundle: " + e.getMessage(), e);
								PushPublishLogger.log(this.getClass(), "Status Update: Failed to bundle");
								pubAuditAPI.updatePublishAuditStatus(pconf.getId(), PublishAuditStatus.Status.FAILED_TO_BUNDLE, historyPojo);
								pubAPI.deleteElementsFromPublishQueueTable(pconf.getId());
							}
						} finally {
							MDC.remove(BUNDLE_ID);
						}
					}

				}

				Logger.debug(PublisherQueueJob.class, "Finished PublishQueue Job");
			}

		} catch (Exception e) {
			Logger.error(PublisherQueueJob.class, e.getMessage(), e);
		} finally {
			try {
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.warn(this, "exception while calling HibernateUtil.closeSession()", e);
			} finally {
				DbConnectionFactory.closeConnection();
			}
		}

	}

	/**
	 * Method that updates the status of a Bundle in the job queue. This method also verifies and limit the number<br/>
	 * of times a Bundle is allowed to try to be published in case of errors.
	 *
	 * @throws DotPublisherException If fails modifying the Publishing status, retrieving status information or<br/>
	 *                               removing the current bundle from the Publish queue table
	 * @throws DotDataException      If fails retrieving end points
	 */
	private void updateAuditStatus() throws DotPublisherException, DotDataException {

		Client client = RestClientBuilder.newClient();
		List<PublishAuditStatus> pendingBundleAudits = pubAuditAPI.getPendingPublishAuditStatus();
		// For each bundle
		for ( PublishAuditStatus bundleAudit : pendingBundleAudits ) {

			MDC.put(BUNDLE_ID, BUNDLE_ID + "=" + bundleAudit.getBundleId());

			try {

				PublishAuditHistory localHistory = bundleAudit.getStatusPojo();
				Date publishStart = null;
				Date publishEnd = null;
				//There is no need to keep checking after MAX_NUM_TRIES.
				if ( localHistory.getNumTries() <= (MAX_NUM_TRIES + 1) ) {
					Map<String, Map<String, EndpointDetail>> endpointsMap = localHistory.getEndpointsMap();
					Map<String, Map<String, EndpointDetail>> endpointTrackingMap = new HashMap<String, Map<String, EndpointDetail>>();
					// For each group (environment)
					for ( String groupID : endpointsMap.keySet() ) {
						Map<String, EndpointDetail> endpointsGroup = endpointsMap.get(groupID);
						// For each endpoint (server) in the group
						for ( String endpointID : endpointsGroup.keySet() ) {
							PublishingEndPoint targetEndpoint = endpointAPI.findEndPointById(endpointID);
							if ( targetEndpoint != null && !targetEndpoint.isSending() ) {
								WebTarget webTarget = client.target(targetEndpoint.toURL() + "/api/auditPublishing");
								try {
									// Try to get the status of the remote endpoints to
									// update the local history
									PublishAuditHistory remoteHistory =
											PublishAuditHistory.getObjectFromString(
													webTarget
															.path("get")
															.path(bundleAudit.getBundleId()).request().get(String.class));
									if ( remoteHistory != null ) {
										publishStart = remoteHistory.getPublishStart();
										publishEnd = remoteHistory.getPublishEnd();
										endpointTrackingMap.putAll(remoteHistory
												.getEndpointsMap());
										for ( String remoteGroupId : remoteHistory
												.getEndpointsMap().keySet() ) {
											Map<String, EndpointDetail> remoteGroup = endpointTrackingMap
													.get(remoteGroupId);
											for ( String remoteEndpointId : remoteGroup
													.keySet() ) {
												EndpointDetail remoteDetail = remoteGroup
														.get(remoteEndpointId);
												localHistory.addOrUpdateEndpoint(
														groupID, endpointID,
														remoteDetail);
											}
										}
									}
								} catch (Exception e) {
									Logger.error(PublisherQueueJob.class, e.getMessage(), e);
								}
							}
						}
					}
					int countGroupOk = 0;
					int countGroupPublishing = 0;
					int countGroupFailed = 0;
					// Check the push status in all groups (environments) to update the
					// publish audit table with the latest info
					for ( String groupId : endpointTrackingMap.keySet() ) {
						Map<String, EndpointDetail> group = endpointTrackingMap.get(groupId);
						boolean isGroupOk = false;
						boolean isGroupPublishing = false;
						boolean isGroupFailed = false;
						for ( String endpoint : group.keySet() ) {
							EndpointDetail detail = group.get(endpoint);
							if ( detail.getStatus() == Status.SUCCESS.getCode() ) {
								isGroupOk = true;
							} else if ( detail.getStatus() == Status.PUBLISHING_BUNDLE
									.getCode() ) {
								isGroupPublishing = true;
							} else if ( detail.getStatus() == Status.FAILED_TO_PUBLISH
									.getCode() ) {
								isGroupFailed = true;
							}
						}
						if ( isGroupOk ) {
							countGroupOk++;
						}
						if ( isGroupPublishing ) {
							countGroupPublishing++;
						}
						if ( isGroupFailed ) {
							countGroupFailed++;
						}
					}
					localHistory.setPublishStart(publishStart);
					localHistory.setPublishEnd(publishEnd);
					if ( localHistory.getNumTries() >= MAX_NUM_TRIES && (countGroupFailed > 0 || countGroupPublishing > 0) ) {
						// If bundle cannot be installed after [MAX_NUM_TRIES] tries
						// and some groups could not be published
						List<Environment> environments = APILocator.getEnvironmentAPI().findEnvironmentsByBundleId(bundleAudit.getBundleId());
						for ( Environment environment : environments ) {
							APILocator.getPushedAssetsAPI().deletePushedAssets(bundleAudit.getBundleId(), environment.getId());
						}
						PushPublishLogger.log(this.getClass(), "Status Update: Failed to publish");
						pubAuditAPI.updatePublishAuditStatus(bundleAudit.getBundleId(),
								PublishAuditStatus.Status.FAILED_TO_PUBLISH,
								localHistory);
						pubAPI.deleteElementsFromPublishQueueTable(bundleAudit.getBundleId());
					} else if ( countGroupFailed > 0 && countGroupOk > 0 ) {
						// If bundle was installed in some groups only
						pubAuditAPI.updatePublishAuditStatus(bundleAudit.getBundleId(),
								PublishAuditStatus.Status.FAILED_TO_SEND_TO_SOME_GROUPS,
								localHistory);
					} else if ( countGroupFailed == endpointTrackingMap.size() ) {
						// If bundle cannot be installed in all groups
						pubAuditAPI.updatePublishAuditStatus(bundleAudit.getBundleId(),
								PublishAuditStatus.Status.FAILED_TO_SEND_TO_ALL_GROUPS,
								localHistory);
					} else if ( countGroupOk == endpointTrackingMap.size() ) {
						// If bundle was installed in all groups
						PushPublishLogger.log(this.getClass(), "Status Update: Success");
						pubAuditAPI.updatePublishAuditStatus(bundleAudit.getBundleId(),
								PublishAuditStatus.Status.SUCCESS,
								localHistory);
						pubAPI.deleteElementsFromPublishQueueTable(bundleAudit.getBundleId());
					} else if ( countGroupPublishing == endpointTrackingMap.size() ) {
						// If bundle is still publishing in all groups
						pubAuditAPI.updatePublishAuditStatus(bundleAudit.getBundleId(),
								PublishAuditStatus.Status.PUBLISHING_BUNDLE,
								localHistory);
					} else {
						// Otherwise, just keep trying to publish the bundle
						pubAuditAPI.updatePublishAuditStatus(bundleAudit.getBundleId(),
								PublishAuditStatus.Status.WAITING_FOR_PUBLISHING,
								localHistory);
					}
				} else {
					//We delete the Publish Queue.
					pubAPI.deleteElementsFromPublishQueueTable(bundleAudit.getBundleId());
				}
			} finally {
				MDC.remove(BUNDLE_ID);
			}
		}
	}

}