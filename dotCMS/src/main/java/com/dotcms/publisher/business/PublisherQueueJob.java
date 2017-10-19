package com.dotcms.publisher.business;

import static com.dotcms.util.CollectionsUtils.map;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotcms.enterprise.publishing.PublishDateUpdater;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.DeliveryStrategy;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.com.google.common.collect.Sets;
import com.dotcms.repackage.javax.ws.rs.client.Client;
import com.dotcms.repackage.javax.ws.rs.client.WebTarget;
import com.dotcms.repackage.org.apache.log4j.MDC;
import com.dotcms.rest.RestClientBuilder;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.UtilMethods;

/**
 * This job is in charge of auditing and triggering the push publishing process
 * in dotCMS. This job is executed right after a user marks contents or a bundle
 * for Push Publishing, and at second zero of every minute. Basically, what this
 * job does is:
 * <ol>
 * <li><b>Bundle status update:</b> Each bundle is associated to an environment,
 * which contains one or more end-points. This job will connect to one or all of
 * them to verify the deployment status of the bundle in order to update its
 * status in the sender server. Examples of status can be:
 * <ul>
 * <li><code>Success</code></li>
 * <li><code>Bundle sent</code></li>
 * <li><code>Failed to Publish</code></li>
 * <li><code>Failed to send to all environments</code></li>
 * <li>etc. (Please see the {@link Status} class to see all the available status
 * options.)</li>
 * </ul>
 * </li>
 * <li><b>Pending bundle push:</b> Besides auditing the different bundles that
 * are being sent, this job will take the bundles that are in the publishing
 * queue, identifies its assets (i.e., pages, contentlets, folders, etc.) and
 * sends them to publishing mechanism.</li>
 * </ol>
 * 
 * @author Alberto
 * @version N/A
 * @since Oct 5, 2012
 *
 */
public class PublisherQueueJob implements StatefulJob {

	private static final String BUNDLE_ID     = "BundleId";
	private static final String ENDPOINT_NAME = "EndpointName";

	public static final Integer MAX_NUM_TRIES = Config.getIntProperty("PUBLISHER_QUEUE_MAX_TRIES", 3);

	private Client restClient;
	private PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();
	private PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();
	private PublisherAPI pubAPI = PublisherAPI.getInstance();
    private EnvironmentAPI environmentAPI = APILocator.getEnvironmentAPI();
    private PublishingEndPointAPI publisherEndPointAPI = APILocator.getPublisherEndPointAPI();

    /**
	 * Reads from the publishing queue table and depending of the publish date
	 * will send a bundle to publish (see
	 * {@link com.dotcms.publishing.PublisherAPI#publish(PublisherConfig)}).
	 *
	 * @param jobExecutionContext
	 *            - Context Containing the current job context information (the
	 *            data).
	 * @throws JobExecutionException
	 *             An exception occurred while executing the job.
	 */
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		try {
			Logger.debug(PublisherQueueJob.class, "Started PublishQueue Job - check for publish dates");
			PublishDateUpdater.updatePublishExpireDates(jobExecutionContext.getFireTime());
			Logger.debug(PublisherQueueJob.class, "Finished PublishQueue Job - check for publish/expire dates");

			Logger.debug(PublisherQueueJob.class, "Started PublishQueue Job - Audit update");
			updateAuditStatus();
			Logger.debug(PublisherQueueJob.class, "Finished PublishQueue Job - Audit update");

			//Verify if we have endpoints where to send the bundles
			List<PublishingEndPoint> endpoints = endpointAPI.getEnabledReceivingEndPoints();
			if ( endpoints != null && endpoints.size() > 0 ) {
				Logger.debug(PublisherQueueJob.class, "Started PublishQueue Job");

				List<Map<String, Object>> bundles = pubAPI.getQueueBundleIdsToProcess();
				List<PublishQueueElement> tempBundleContents;
				PublishAuditStatus status;
				PublishAuditHistory historyPojo;
				String tempBundleId;
				if (bundles.size() > 0) {
					Logger.info(this, "");
					Logger.info(this, "Found " + bundles.size() + " bundle(s) to process.");
					Logger.info(this, "");
				}
				for ( Map<String, Object> bundle : bundles ) {

					Date publishDate = (Date) bundle.get("publish_date");

					Logger.info(this, "Processing bundle: ID: " + bundle.get("bundle_id") + ". Status: "
							+ (UtilMethods.isSet(bundle.get("status")) ? bundle.get("status") : "Starting")
							+ ". Publish Date: " + publishDate);
					if ( publishDate.before(new Date()) ) {
						tempBundleId = (String) bundle.get("bundle_id");
						MDC.put(BUNDLE_ID, BUNDLE_ID + "=" + tempBundleId);

						try {
							PushPublishLogger.log(this.getClass(), "Pre-publish work started.");
							tempBundleContents = pubAPI.getQueueElementsByBundleId(tempBundleId);

							//Setting Audit objects History
							historyPojo = new PublishAuditHistory();
							//Retrieving assets
							Map<String, String> assets = new HashMap<String, String>();
							List<PublishQueueElement> assetsToPublish = new ArrayList<PublishQueueElement>();

							for ( PublishQueueElement c : tempBundleContents ) {
								assets.put(c.getAsset(), c.getType());
								assetsToPublish.add(c);
							}
							historyPojo.setAssets(assets);

							final Map<String, Object> jobDataMap = jobExecutionContext.getMergedJobDataMap();
							DeliveryStrategy deliveryStrategy = DeliveryStrategy.class
									.cast(jobDataMap.get("deliveryStrategy"));
							
							PublisherConfig pconf = new PushPublisherConfig();
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
							pconf.setPublishers(new ArrayList<>(getPublishersForBundle(tempBundleId)));
							pconf.setDeliveryStrategy(deliveryStrategy);
							if ( Integer.parseInt(bundle.get("operation").toString()) == PublisherAPI.ADD_OR_UPDATE_ELEMENT ) {
								pconf.setOperation(PushPublisherConfig.Operation.PUBLISH);
							} else {
								pconf.setOperation(PushPublisherConfig.Operation.UNPUBLISH);
							}

							pconf = setUpConfigForPublisher(pconf);
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
	 * Updates the status of a Bundle in the job queue. This method also
	 * verifies and limits the number of times a Bundle is allowed to attempt to
	 * be published in case of errors.
	 *
	 * @throws DotPublisherException
	 *             An error occurred when modifying the Publishing status,
	 *             retrieving status information or removing the current bundle
	 *             from the Publish queue table.
	 * @throws DotDataException
	 *             An error occurred when retrieving the end-points from the
	 *             database.
	 */
	private void updateAuditStatus() throws DotPublisherException, DotDataException {
		List<PublishAuditStatus> pendingBundleAudits = pubAuditAPI.getPendingPublishAuditStatus();
		// For each bundle audit
 		for ( PublishAuditStatus bundleAudit : pendingBundleAudits ) {
			MDC.put(BUNDLE_ID, BUNDLE_ID + "=" + bundleAudit.getBundleId());

			try {
				PublishAuditHistory localHistory = bundleAudit.getStatusPojo();
				//There is no need to keep checking after MAX_NUM_TRIES.
				if ( localHistory.getNumTries() <= (MAX_NUM_TRIES + 1) ) {

					final Map<String, Map<String, EndpointDetail>> endpointTrackingMap =
							collectEndpointInfoFromRemote(bundleAudit);

					final GroupPushStats groupPushStats = getGroupStats(endpointTrackingMap);

					updateBundleStatus(bundleAudit, endpointTrackingMap, groupPushStats);
				} else {
					//We delete the Publish Queue.
					pubAPI.deleteElementsFromPublishQueueTable(bundleAudit.getBundleId());
				}
			} finally {
				MDC.remove(BUNDLE_ID);
				MDC.remove(ENDPOINT_NAME);
			}
		}
	}

	private Map<String, Map<String, EndpointDetail>> collectEndpointInfoFromRemote(final PublishAuditStatus bundleAudit)
			throws DotDataException, DotPublisherException {
		Map<String, Map<String, EndpointDetail>> endpointTrackingMap = new HashMap<>();
		PublishAuditHistory localHistory = bundleAudit.getStatusPojo();
		Map<String, Map<String, EndpointDetail>> endpointsMap = localHistory.getEndpointsMap();
		// For each group (environment)
		for ( String groupID : endpointsMap.keySet() ) {
            Map<String, EndpointDetail> endpointsGroup = endpointsMap.get(groupID);
            // For each end-point (server) in the group
            for ( String endpointID : endpointsGroup.keySet() ) {
                PublishingEndPoint targetEndpoint = endpointAPI.findEndPointById(endpointID);

                if ( targetEndpoint != null && !targetEndpoint.isSending() ) {
                    MDC.put(ENDPOINT_NAME, ENDPOINT_NAME + "=" + targetEndpoint.getServerName());
                    // Don't poll status for static publishing
                    if (!AWSS3Publisher.PROTOCOL_AWS_S3.equalsIgnoreCase(targetEndpoint.getProtocol())) {

                        try {
                            // Try to get the status of the remote end-points to
                            // update the local history
                            final PublishAuditHistory remoteHistory = getRemoteHistoryFromEndpoint(bundleAudit, targetEndpoint);

                            if (remoteHistory != null) {
                                updateLocalPublishDatesFromRemote(localHistory, remoteHistory);
                                endpointTrackingMap.putAll(remoteHistory.getEndpointsMap());
                                updateLocalEndpointDetailFromRemote(localHistory, groupID, endpointID, remoteHistory);
                            }
                        } catch (Exception e) {
                            // An error occurred when retrieving the end-point's audit info.
                            // Usually caused by a network problem.
                            Logger.error(PublisherQueueJob.class,
                                    String.format(
                                            "An error occurred when accessing end-point '%s' with IP %s: %s",
                                            targetEndpoint.getServerName(), targetEndpoint.getAddress(),
                                            e.getMessage()),
                                    e);
                            String failedAuditUpdate = "failed-remote-group-" + System.currentTimeMillis();
                            EndpointDetail detail = new EndpointDetail();
                            detail.setStatus(Status.FAILED_TO_PUBLISH.getCode());
                            endpointTrackingMap.put(failedAuditUpdate, map(failedAuditUpdate, detail));

                            PushPublishLogger.log(this.getClass(), "Status update: Failed to publish bundle");
                        }
                    } else {
                        PublishAuditStatus pas = pubAuditAPI.getPublishAuditStatus(bundleAudit.getBundleId());
                        endpointTrackingMap.putAll(pas.getStatusPojo().getEndpointsMap());
                    }
                }
            }
        }

        return endpointTrackingMap;
	}

	@NotNull
	private void updateBundleStatus(final PublishAuditStatus bundleAudit,
									final Map<String, Map<String, EndpointDetail>> endpointTrackingMap,
									final GroupPushStats groupPushStats) throws DotDataException, DotPublisherException {
		Status bundleStatus;
		PublishAuditHistory localHistory = bundleAudit.getStatusPojo();

		if ( localHistory.getNumTries() >= MAX_NUM_TRIES && (groupPushStats.getCountGroupFailed() > 0
                || groupPushStats.getCountGroupPublishing() > 0) ) {
            // If bundle cannot be installed after [MAX_NUM_TRIES] tries
            // and some groups could not be published
            List<Environment> environments = APILocator.getEnvironmentAPI().findEnvironmentsByBundleId(bundleAudit.getBundleId());
            for ( Environment environment : environments ) {
                APILocator.getPushedAssetsAPI().deletePushedAssets(bundleAudit.getBundleId(), environment.getId());
            }
            PushPublishLogger.log(this.getClass(), "Status Update: Failed to publish");
            bundleStatus = Status.FAILED_TO_PUBLISH;
            pubAuditAPI.updatePublishAuditStatus(bundleAudit.getBundleId(), bundleStatus, localHistory);
            pubAPI.deleteElementsFromPublishQueueTable(bundleAudit.getBundleId());
        } else if (groupPushStats.getCountGroupFailed() > 0
				&& (groupPushStats.getCountGroupOk() + groupPushStats.getCountGroupFailed()) == endpointTrackingMap.size()) {
            // If bundle was installed in some groups only
            bundleStatus = Status.FAILED_TO_SEND_TO_SOME_GROUPS;
            pubAuditAPI.updatePublishAuditStatus(bundleAudit.getBundleId(), bundleStatus, localHistory);
        } else if (groupPushStats.getCountGroupFailed() > 0 && groupPushStats.getCountGroupFailed() == endpointTrackingMap.size()) {
            // If bundle cannot be installed in all groups
            bundleStatus = Status.FAILED_TO_SEND_TO_ALL_GROUPS;
            pubAuditAPI.updatePublishAuditStatus(bundleAudit.getBundleId(), bundleStatus, localHistory);
        } else if (groupPushStats.getCountGroupOk() > 0 && groupPushStats.getCountGroupOk() == endpointTrackingMap.size()) {
            // If bundle was installed in all groups
            PushPublishLogger.log(this.getClass(), "Status Update: Success");
            bundleStatus = Status.SUCCESS;
            pubAuditAPI.updatePublishAuditStatus(bundleAudit.getBundleId(), bundleStatus, localHistory);
            pubAPI.deleteElementsFromPublishQueueTable(bundleAudit.getBundleId());
        } else if ( groupPushStats.getCountGroupPublishing() == endpointTrackingMap.size() ) {
            // If bundle is still publishing in all groups
            bundleStatus = Status.PUBLISHING_BUNDLE;
            pubAuditAPI.updatePublishAuditStatus(bundleAudit.getBundleId(), bundleStatus, localHistory);
        } else {
            // Otherwise, just keep trying to publish the bundle
            bundleStatus = Status.WAITING_FOR_PUBLISHING;
            pubAuditAPI.updatePublishAuditStatus(bundleAudit.getBundleId(), bundleStatus, localHistory);
        }

		Logger.info(this, "===========================================================");
		Logger.info(this, String.format("For bundle '%s':", bundleAudit.getBundleId()));
		if (!bundleStatus.equals(PublishAuditStatus.Status.PUBLISHING_BUNDLE)
				&& !bundleStatus.equals(PublishAuditStatus.Status.WAITING_FOR_PUBLISHING)) {
			Logger.info(this, String.format("-> Re-try attempts: %d", localHistory.getNumTries()));
		}
		Logger.info(this, String.format("-> Status:          %s", bundleStatus.toString()));
		Logger.info(this, "===========================================================");
	}

	private GroupPushStats getGroupStats(final Map<String, Map<String, EndpointDetail>> endpointTrackingMap) {
		final GroupPushStats groupPushStats = new GroupPushStats();

		for (final Map<String, EndpointDetail> group : endpointTrackingMap.values()) {
            boolean isGroupOk = false;
            boolean isGroupPublishing = false;
            boolean isGroupFailed = false;
            for (final EndpointDetail detail : group.values() ) {
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
                groupPushStats.increaseCountGroupOk();
            }
            if ( isGroupPublishing ) {
                groupPushStats.increaseCountGroupPublishing();
            }
            if ( isGroupFailed ) {
                groupPushStats.increaseCountGroupFailed();
            }
        }

        return groupPushStats;
	}

	private void updateLocalEndpointDetailFromRemote(final PublishAuditHistory localHistory, final String groupID,
													 final String endpointID, final PublishAuditHistory remoteHistory) {
		for (final Map<String, EndpointDetail> remoteGroup : remoteHistory.getEndpointsMap().values()) {
            for (final EndpointDetail remoteDetail : remoteGroup.values()) {
                localHistory.addOrUpdateEndpoint(groupID, endpointID, remoteDetail);
            }
        }
	}

	private void updateLocalPublishDatesFromRemote(final PublishAuditHistory localHistory,
												   final PublishAuditHistory remoteHistory) {
		Date publishStart;
		Date publishEnd;
		publishStart = remoteHistory.getPublishStart();
		publishEnd = remoteHistory.getPublishEnd();
		if(localHistory.getPublishStart()==null || publishStart.before(localHistory.getPublishStart())) {
            localHistory.setPublishStart(publishStart);
        }
		if(localHistory.getPublishEnd()==null || publishEnd.after(localHistory.getPublishEnd())) {
            localHistory.setPublishEnd(publishEnd);
        }
	}

    /**
     * Obtains the bundle history that has been created in the specified end-point.
     * 
     * @param bundleAudit - The {@link PublishAuditStatus} object containing bundle data.
     * @param targetEndpoint - The {@link PublishingEndPoint} whose bundle history will be
     *        retrieved.
     * @return The {@link PublishAuditHistory} of the bundle in the specified end-point.
     */
	private PublishAuditHistory getRemoteHistoryFromEndpoint(final  PublishAuditStatus bundleAudit,
															 final PublishingEndPoint targetEndpoint) {
	    final WebTarget webTarget = getRestClient().target(targetEndpoint.toURL() + "/api/auditPublishing");
		return PublishAuditHistory.getObjectFromString(
                webTarget
                        .path("get")
                        .path(bundleAudit.getBundleId()).request().get(String.class));
	}

	/**
	 * Get the Publisher needed depending on the protocol of the end-points of
	 * the bundle.
	 *
	 * @param bundleId
	 *            - The Id of the generated bundle.
	 * @return The {@link Publisher} classes for the specified bundle.
	 */
    private Set<Class<?>> getPublishersForBundle(String bundleId){

        Set<Class<?>> publishersClasses = new HashSet<>();

	    try{
            Map<String, Class<? extends IPublisher>> protocolPublisherMap = Maps.newConcurrentMap();
            //TODO: for OSGI we need to get this list from implementations of IPublisher or something else.
            Set<Class<?>> publishers = Sets.newHashSet(PushPublisher.class, AWSS3Publisher.class);

            //Fill protocolPublisherMap with protocol -> publisher.
            for (Class publisher : publishers) {
                Publisher p = (Publisher)publisher.newInstance();
                for (String protocol : p.getProtocols()) {
                    protocolPublisherMap.put(protocol, publisher);
                }
            }

            //For each environment in the bundle we need to get the end-points.
            List<Environment> environments = this.environmentAPI.findEnvironmentsByBundleId(bundleId);

            for (Environment environment : environments) {
                //For each end-point we choose if run static or dynamic process (Static = AWSS3Publisher, Dynamic = PushPublisher)
                List<PublishingEndPoint> endpoints = this.publisherEndPointAPI.findSendingEndPointsByEnvironment(environment.getId());

                //For each end-point we need include the Publisher depending on the type.
                for (PublishingEndPoint endpoint : endpoints) {
                    //Only if the end-point is enabled.
                    if (endpoint.isEnabled() && protocolPublisherMap.containsKey(endpoint.getProtocol())){
                        publishersClasses.add(protocolPublisherMap.get(endpoint.getProtocol()));

                    }
                }
            }
        } catch (Exception e){
	        Logger.error(this, "Error trying to get publishers from bundle id: " + bundleId, e);
        }

	    return publishersClasses;
    }

	/**
	 * Sends the parameter {@link PublisherConfig} to each Publisher in use to
	 * be filled with the necessary information, such as extra languages, hosts,
	 * etc.
	 *
	 * @param pconf
	 *            - The {@link PublisherConfig} object.
	 * @return
	 * @throws IllegalAccessException
	 *             The {@link Publisher} class could not be instantiated.
	 * @throws InstantiationException
	 *             The {@link Publisher} class could not be instantiated.
	 */
    private PublisherConfig setUpConfigForPublisher(PublisherConfig pconf)
        throws IllegalAccessException, InstantiationException {

        final List<Class> publishers = pconf.getPublishers();
        for (Class<?> publisher : publishers) {
            pconf = ((Publisher)publisher.newInstance()).setUpConfig(pconf);
        }

        return pconf;
    }

    private class GroupPushStats {
		private int countGroupOk = 0;
		private int countGroupPublishing = 0;
		private int countGroupFailed = 0;

		public void increaseCountGroupOk() {
			countGroupOk++;
		}

		public void increaseCountGroupPublishing() {
			countGroupPublishing++;
		}

		public void increaseCountGroupFailed() {
			countGroupFailed++;
		}

		public int getCountGroupOk() {
			return countGroupOk;
		}

		public int getCountGroupPublishing() {
			return countGroupPublishing;
		}

		public int getCountGroupFailed() {
			return countGroupFailed;
		}
	}

    /**
     * Returns an instance of the REST {@link Client} used to access Push Publishing end-points and
     * retrieve their information.
     * 
     * @return The REST {@link Client}.
     */
    private Client getRestClient() {
        if (null == this.restClient) {
            this.restClient = RestClientBuilder.newClient();
        }
        return this.restClient;
    }

}
