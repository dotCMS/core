package com.dotcms.publisher.business;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.enterprise.publishing.PublishDateUpdater;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher;
import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.publisher.bundle.bean.Bundle;
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
import com.dotcms.rest.RestClientBuilder;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.util.StringPool;
import org.apache.logging.log4j.ThreadContext;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This Quartz Job runs at second zero of every minute, and is in charge of two main content
 * publishing features:
 * <ol>
 *     <li>Content Publishing and Expiration.</li>
 *     <li>Push Publishing.</li>
 * </ol>
 * <h4>Content Publishing and Expiration.</h4>
 * <p>This Job generates a list of ALL Content Types with Publishing and/or Expiration Date fields,
 * and determines whether the Contentlets of such types must be published/unpublished or not. You
 * can find more about how this can be set up in our official documentation.</p>
 * <h4>Push Publishing</h4>
 * <p>This Job takes care of auditing and triggering the push publishing process in dotCMS. It is
 * executed right after a user marks contents or a bundle for Push Publishing. Basically, what this
 * job does is:</p>
 * <ol>
 * 	   <li><b>Bundle status update:</b> Each bundle is associated to an environment, which contains
 * 	   one or more end-points. This job will connect to one or all of them to verify the deployment
 * 	   status of the bundle in order to update its status in the sender server. Examples of status
 * 	   can be:
 * <ul>
 * <li><code>Success</code></li>
 * <li><code>Bundle sent</code></li>
 * <li><code>Failed to Publish</code></li>
 * <li><code>Failed to send to all environments</code></li>
 *         <li>And several others (please see the {@link Status} class to see all the available
 *         status options).
 *         </li>
 * </ul>
 * </li>
 *     <li><b>Pending bundle push:</b> Besides auditing the different bundles that are being
 *     sent, this job will take the bundles that are in the publishing queue, identifies its
 *     assets (i.e., pages, contentlets, folders, etc.) and sends them to publishing mechanism.</li>
 * </ol>
 *
 * @author Alberto
 * @version N/A
 * @since Oct 5, 2012
 */
public class PublisherQueueJob implements StatefulJob {

	private static final String BUNDLE_ID     = "BundleId";
	private static final String ENDPOINT_NAME = "EndpointName";

	public static final Integer MAX_NUM_TRIES = Config.getIntProperty("PUBLISHER_QUEUE_MAX_TRIES", 3);
	public static final int MAX_SIZE_PP_AUDIT_PAYLOAD = Config.getIntProperty("MAX_SIZE_PP_AUDIT_PAYLOAD", 10000);

	private final PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();
	private final PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();
	private final PublisherAPI pubAPI = PublisherAPI.getInstance();
	private final EnvironmentAPI environmentAPI = APILocator.getEnvironmentAPI();
	private final PublishingEndPointAPI publisherEndPointAPI = APILocator.getPublisherEndPointAPI();

	/**
	 * Reads from the publishing queue table and depending of the publish date
	 * will send a bundle to publish (see
	 * {@link com.dotcms.publishing.PublisherAPI#publish(PublisherConfig)}}).
	 *
	 * @param jobExecutionContext
	 *            - Context Containing the current job context information (the
	 *            data).
	 * @throws JobExecutionException
	 *             An exception occurred while executing the job.
	 */
	@CloseDBIfOpened
	public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
		try {
			Logger.debug(PublisherQueueJob.class, "Started PublishQueue Job - check for publish dates");
			// Use JobExecutionContext.getPreviousFireTime() when available for accurate previous run time
			PublishDateUpdater.updatePublishExpireDates(
					jobExecutionContext.getFireTime(), 
					jobExecutionContext.getPreviousFireTime());
			Logger.debug(PublisherQueueJob.class, "Finished PublishQueue Job - check for publish/expire dates");
			List<Map<String, Object>> bundles = pubAPI.getQueueBundleIdsToProcess();
			if (null == bundles) {
				bundles = new ArrayList<>();
			}
			Logger.debug(PublisherQueueJob.class, "Started PublishQueue Job - Audit update");
			updateAuditStatus(bundles);
			Logger.debug(PublisherQueueJob.class, "Finished PublishQueue Job - Audit update");
			// Verify if we have endpoints where to send the bundles
			final List<PublishingEndPoint> endpoints = endpointAPI.getEnabledReceivingEndPoints();
			if (UtilMethods.isSet(endpoints)) {
				Logger.debug(PublisherQueueJob.class, "Started PublishQueue Job");
				if (UtilMethods.isSet(bundles)) {
					Logger.info(this, "");
					Logger.info(this, "Found " + bundles.size() + " bundle(s) to process.");
					Logger.info(this, "");
				}
				for (final Map<String, Object> bundle : bundles) {
					final Date publishDate = (Date) bundle.get("publish_date");
					Logger.info(this, "Processing bundle: ID: " + bundle.get("bundle_id") + ". Status: "
							+ (UtilMethods.isSet(bundle.get("status")) ? bundle.get("status") : "Starting")
							+ ". Publish Date: " + publishDate);
					final String tempBundleId = (String) bundle.get("bundle_id");
					final PublishAuditStatus status = new PublishAuditStatus(tempBundleId);

					ThreadContext.put(BUNDLE_ID, BUNDLE_ID + "=" + tempBundleId);

					Date bundleStart;
					try {
						PushPublishLogger.log(this.getClass(), "Pre-publish work started.");
						final List<PublishQueueElement> tempBundleContents = pubAPI.getQueueElementsByBundleId(tempBundleId);

						// Retrieving assets
						final Map<String, String> assets = new HashMap<>();
						final List<PublishQueueElement> assetsToPublish = new ArrayList<>();
						for (final PublishQueueElement c : tempBundleContents) {
							assets.put(c.getAsset(), c.getType());
							assetsToPublish.add(c);
						}
						// Setting Audit objects History
						final PublishAuditHistory historyPojo = new PublishAuditHistory();
						historyPojo.setAssets(assets);
						final Map<String, Object> jobDataMap = jobExecutionContext.getMergedJobDataMap();
						final DeliveryStrategy deliveryStrategy = DeliveryStrategy.class
								.cast(jobDataMap.get("deliveryStrategy"));

						PublisherConfig pconf = new PushPublisherConfig();
						pconf.setAssets(assetsToPublish);

						// Status
						status.setStatusPojo(historyPojo);
						// Insert in Audit table
						pubAuditAPI.insertPublishAuditStatus(status);

						// Queries creation
						pconf.setLuceneQueries(PublisherUtil.prepareQueries(tempBundleContents));
						pconf.setId(tempBundleId);
						pconf.setUser(APILocator.getUserAPI().getSystemUser());
						bundleStart = new Date();
						pconf.setStartDate(bundleStart);
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
						} catch (final DotPublishingException e) {
							/*
							If we are getting errors creating the bundle we should stop trying to publish it, this is not just a connection error,
							there is something wrong with a bundler or creating the bundle.
							 */
							Logger.error(PublisherQueueJob.class, "Unable to publish Bundle '" + pconf.getId() + "': " + ExceptionUtil.getErrorMessage(e), e);
							PushPublishLogger.log(this.getClass(), "Status Update: Failed to bundle '" + pconf.getId() + "'");
							updateAuditStatusErrorMsg(historyPojo, ExceptionUtil.getErrorMessage(e));
							historyPojo.setBundleStart(bundleStart);
							historyPojo.setBundleEnd(new Date());
							pubAuditAPI.updatePublishAuditStatus(pconf.getId(), PublishAuditStatus.Status.FAILED_TO_BUNDLE, historyPojo);
							pubAPI.deleteElementsFromPublishQueueTable(pconf.getId());
						}
					} finally {
						ThreadContext.remove(BUNDLE_ID);
					}
				}
				Logger.debug(PublisherQueueJob.class, "Finished PublishQueue Job");
			}
		} catch (final Throwable e) {
			Logger.error(PublisherQueueJob.class, "An error occurred when trying to publish bundles: " +
					ExceptionUtil.getErrorMessage(e), e);
		}
	}

	/**
	 * Updates the status of a Bundle in the job queue. This method also verifies and limits the number of times a
	 * Bundle is allowed to attempt to be published in case of errors.
	 *
	 * @param bundlesInQueue The list of bundles that are currently in the publishing queue.
	 *
	 * @throws DotPublisherException An error occurred when modifying the Publishing status, retrieving status
	 *                               information or removing the current bundle from the Publish queue table.
	 * @throws DotDataException      An error occurred when retrieving the end-points from the database.
	 */
	private void updateAuditStatus(final List<Map<String, Object>> bundlesInQueue)
			throws DotPublisherException, DotDataException, DotSecurityException, LanguageException {
		final List<PublishAuditStatus> pendingBundleAudits = pubAuditAPI.getPendingPublishAuditStatus();
		// For each bundle audit
		for (final PublishAuditStatus bundleAudit : pendingBundleAudits) {
			ThreadContext.put(BUNDLE_ID, BUNDLE_ID + "=" + bundleAudit.getBundleId());
			try {

				// There is no need to keep checking after MAX_NUM_TRIES.
				if (bundleAudit.getStatusPojo().getNumTries() <= (MAX_NUM_TRIES + 1)) {

					final Map<String, Map<String, EndpointDetail>> endpointTrackingMap =
							collectEndpointInfoFromRemote(bundleAudit);
					final GroupPushStats groupPushStats = getGroupStats(endpointTrackingMap);
					updateBundleStatus(bundleAudit, endpointTrackingMap, groupPushStats, bundlesInQueue);
				} else {
					// We delete the Publish Queue.
					pubAPI.deleteElementsFromPublishQueueTable(bundleAudit.getBundleId());
				}
			} finally {
				ThreadContext.remove(BUNDLE_ID);
				ThreadContext.remove(ENDPOINT_NAME);
			}
		}
	}


	/**
	 * Obtains the list of Endpoints inside each Push Publishing Environment and verifies the publishing status of a
	 * bundle that was recently pushed. Most of the times, the system can fail to obtain the status of a bundle:
	 * <ol>
	 *     <li>A network connection error between the sending  and receiving (Endpoint) environment.</li>
	 *     <li>The physical file of the bundle in the receiving Endpoint is not accessible or has been deleted.</li>
	 * </ol>
	 *
	 * @param bundleAudit Contains status information of a bundle that was recently pushed.
	 * @return The response status of the bundle for each Endpoint according to the Push Publishing setup.
	 * @throws DotDataException The information of a specific Endpoint could not be retrieved.
	 * @throws DotPublisherException The audit status of the bundle could not be retrieved.
	 */
	private Map<String, Map<String, EndpointDetail>> collectEndpointInfoFromRemote(final PublishAuditStatus bundleAudit)
			throws DotDataException, DotPublisherException {
		final Map<String, Map<String, EndpointDetail>> endpointTrackingMap = new HashMap<>();
		final PublishAuditHistory localHistory = bundleAudit.getStatusPojo();
		final Map<String, Map<String, EndpointDetail>> endpointsMap = localHistory.getEndpointsMap();

		final Client client = getRestClient();
		final Map<String, BundlesToSent> bundlesToSent = new HashMap<>();

		// For each group (environment)
		for (final String groupID : endpointsMap.keySet() ) {
			final Map<String, EndpointDetail> endpointsGroup = endpointsMap.get(groupID);
			// For each end-point (server) in the group
			for (final String endpointID : endpointsGroup.keySet() ) {
				final PublishingEndPoint targetEndpoint = endpointAPI.findEndPointById(endpointID);

				if (targetEndpoint != null && !targetEndpoint.isSending()) {
					ThreadContext.put(ENDPOINT_NAME, ENDPOINT_NAME + "=" + targetEndpoint.getServerName());
					// Don't poll status for static publishing
					if (!AWSS3Publisher.PROTOCOL_AWS_S3.equalsIgnoreCase(targetEndpoint.getProtocol())
							&& !StaticPublisher.PROTOCOL_STATIC.equalsIgnoreCase(targetEndpoint.getProtocol())) {

						final BundlesToSent bundleToSend = bundlesToSent.computeIfAbsent(targetEndpoint.getId(),
								key -> new BundlesToSent(targetEndpoint));

						bundleToSend.add(bundleAudit.getBundleId());

						if (bundleToSend.limitReach()) {
							sendBundle(bundleToSend.bundleIds,  bundleToSend.targetEndpoint,
									client, localHistory, endpointTrackingMap);
							bundleToSend.reset();
						}
					} else {
						final PublishAuditStatus auditStatus = pubAuditAPI.getPublishAuditStatus(bundleAudit.getBundleId());
						endpointTrackingMap.putAll(auditStatus.getStatusPojo().getEndpointsMap());
					}
				}
			}
		}

		for (BundlesToSent toSent : bundlesToSent.values()) {

			if (toSent.isEmpty()) {
				continue;
			}

			List<List<String>> partitions = Lists.partition(toSent.bundleIds, MAX_SIZE_PP_AUDIT_PAYLOAD);

			for (List<String> partition : partitions) {
				sendBundle(partition, toSent.targetEndpoint, client, localHistory, endpointTrackingMap);
			}
		}

		return endpointTrackingMap;
	}

	private void sendBundle(List<String> bundleIds, PublishingEndPoint targetEndpoint, Client client,
							PublishAuditHistory localHistory, Map<String, Map<String, EndpointDetail>> endpointTrackingMap)
			throws DotDataException {

			// Try to get the status of the remote end-points to
			// update the local history
			 getRemoteHistoryFromEndpoint(bundleIds, targetEndpoint, client)
					 .stream()
					 .filter(Objects::nonNull)
					 .forEach(remoteHistory -> {
						 sendBundle(bundleIds, targetEndpoint, localHistory,
								 endpointTrackingMap, remoteHistory);
					 });

	}

	private void sendBundle(List<String> bundleIds, PublishingEndPoint targetEndpoint, PublishAuditHistory localHistory,
							Map<String, Map<String, EndpointDetail>> endpointTrackingMap, PublishAuditHistory remoteHistory) {
		try {
			updateLocalPublishDatesFromRemote(localHistory, remoteHistory);
			endpointTrackingMap.putAll(remoteHistory.getEndpointsMap());
			updateLocalEndpointDetailFromRemote(localHistory, targetEndpoint.getGroupId(), targetEndpoint.getId(), remoteHistory);
		} catch (final Exception e) {
			// An error occurred when retrieving the end-point's audit info.
			// Usually caused by a network problem.
			Logger.error(PublisherQueueJob.class, "An error occurred when updating audit status from " +
					"endpoint=[" + targetEndpoint.toURL() + "], bundle=[" + bundleIds.get(0)
					+ "] : " + ExceptionUtil.getErrorMessage(e), e);
			final String failedAuditUpdate = "failed-remote-group-" + System.currentTimeMillis();
			final EndpointDetail detail = new EndpointDetail();
			detail.setStatus(Status.FAILED_TO_PUBLISH.getCode());
			endpointTrackingMap.put(failedAuditUpdate, Map.of(failedAuditUpdate, detail));
			PushPublishLogger.log(this.getClass(), "Status update: Failed to update bundle audit status.");
		}
	}

	/**
	 * Updates the local Publish Audit History with the information retrieved from a remote
	 * end-point.
	 *
	 * @param bundleAudit         The {@link PublishAuditStatus} object that contains the audit
	 *                            status of the bundle in the local environment.
	 * @param endpointTrackingMap The map containing what endpoints are involved in the current
	 *                            Push Publishing operation.
	 * @param groupPushStats      The {@link GroupPushStats} object that contains what environments
	 *                            were successfully updated with the pushed content, failed, or
	 *                            presented warnings.
	 * @param bundlesInQueue      The list of bundles that are currently in the publishing queue.
	 *
	 * @throws DotDataException      An error occurred when interacting with the database.
	 * @throws DotPublisherException An error occurred when modifying the Publishing status.
	 * @throws LanguageException     An error occurred when internationalizing an error message.
	 */
	private void updateBundleStatus(final PublishAuditStatus bundleAudit,
									final Map<String, Map<String, EndpointDetail>> endpointTrackingMap,
									final GroupPushStats groupPushStats, final List<Map<String, Object>> bundlesInQueue)
			throws DotDataException, DotPublisherException, LanguageException {
		Status bundleStatus;
		final PublishAuditHistory localHistory = bundleAudit.getStatusPojo();
		final String auditedBundleId = bundleAudit.getBundleId();
		// Info needed to generate the Growl Notification
		final Bundle bundle = APILocator.getBundleAPI().getBundleById(auditedBundleId);
		if (null == bundle) {
			return;
		}
		final boolean isBundleNameGenerated = UtilMethods.isSet(bundle.getName()) && bundle.getName().startsWith("bundle-");
		String notificationMessage = "";
		String notificationMessageArgument;
		final SystemMessageBuilder message = new SystemMessageBuilder()
				.setMessage(notificationMessage)
				.setSeverity(MessageSeverity.SUCCESS)
				.setType(MessageType.SIMPLE_MESSAGE)
				.setLife(5000);

		if ( localHistory.getNumTries() >= MAX_NUM_TRIES && (groupPushStats.getCountGroupFailed() > 0
				|| groupPushStats.getCountGroupPublishing() > 0) ) {
			// If bundle cannot be installed after [MAX_NUM_TRIES] tries
			// and some groups could not be published
			List<Environment> environments = APILocator.getEnvironmentAPI().findEnvironmentsByBundleId(auditedBundleId);
			for ( Environment environment : environments ) {
				APILocator.getPushedAssetsAPI().deletePushedAssets(auditedBundleId, environment.getId());
			}
			PushPublishLogger.log(this.getClass(), "Status Update: Failed to publish");
			bundleStatus = Status.FAILED_TO_PUBLISH;
			pubAuditAPI.updatePublishAuditStatus(auditedBundleId, bundleStatus, localHistory);
			pubAPI.deleteElementsFromPublishQueueTable(auditedBundleId);

			//Update Notification Info
			notificationMessage = isBundleNameGenerated ? "bundle.title.fail.notification" : "bundle.named.fail.notification";
			notificationMessageArgument = isBundleNameGenerated ? generateBundleTitle(localHistory.getAssets()) : bundle.getName();
			message.setMessage(LanguageUtil.get(
					notificationMessage,
					notificationMessageArgument));
			message.setLife(86400000);
			message.setSeverity(MessageSeverity.ERROR);
		} else if (groupPushStats.getCountGroupFailed() > 0 && groupPushStats.getCountGroupFailed() == endpointTrackingMap.size()) {
			// If bundle cannot be installed in all groups
			bundleStatus = Status.FAILED_TO_SEND_TO_ALL_GROUPS;
			pubAuditAPI.updatePublishAuditStatus(auditedBundleId, bundleStatus, localHistory);
		} else if (groupPushStats.getCountGroupFailed() > 0
				&& (groupPushStats.getCountGroupOk() + groupPushStats.getCountGroupFailed()) == endpointTrackingMap.size()) {
			// If bundle was installed in some groups only
			bundleStatus = Status.FAILED_TO_SEND_TO_SOME_GROUPS;
			pubAuditAPI.updatePublishAuditStatus(auditedBundleId, bundleStatus, localHistory);
		} else if (groupPushStats.getCountGroupOk() > 0 && groupPushStats.getCountGroupOk() == endpointTrackingMap.size()) {
			// If bundle was installed in all groups
			PushPublishLogger.log(this.getClass(), "Status Update: Success");
            bundleStatus =
                    (groupPushStats.getCountGroupWithWarnings() > 0) ? Status.SUCCESS_WITH_WARNINGS
                            : Status.SUCCESS;
			pubAuditAPI.updatePublishAuditStatus(auditedBundleId, bundleStatus, localHistory);
			pubAPI.deleteElementsFromPublishQueueTable(auditedBundleId);

			//Update Notification Info
            if (groupPushStats.getCountGroupWithWarnings() > 0) {
                notificationMessage =
                        isBundleNameGenerated ? "bundle.title.success_with_warnings.notification"
                                : "bundle.named.success_with_warnings.notification";
            } else {
                notificationMessage =
                        isBundleNameGenerated ? "bundle.title.success.notification"
                                : "bundle.named.success.notification";
            }

			notificationMessageArgument = isBundleNameGenerated ? generateBundleTitle(localHistory.getAssets()) : bundle.getName();
			message.setMessage(LanguageUtil.get(
					notificationMessage,
					notificationMessageArgument));
		} else if ( groupPushStats.getCountGroupPublishing() == endpointTrackingMap.size() ) {
			// If bundle is still publishing in all groups
			bundleStatus = Status.PUBLISHING_BUNDLE;
			pubAuditAPI.updatePublishAuditStatus(auditedBundleId, bundleStatus, localHistory);
		} else if (groupPushStats.getCountGroupSaved() > 0){
			// If the static bundle was saved but has not been sent
			bundleStatus = Status.BUNDLE_SAVED_SUCCESSFULLY;
			pubAuditAPI.updatePublishAuditStatus(auditedBundleId, bundleStatus, localHistory);
		} else {
			// Otherwise, just keep trying to publish the bundle
			bundleStatus = Status.WAITING_FOR_PUBLISHING;
			pubAuditAPI.updatePublishAuditStatus(auditedBundleId, bundleStatus, localHistory);
		}

		Logger.info(this, "===========================================================");
		Logger.info(this, String.format("For bundle '%s':", auditedBundleId));
		Logger.info(this, String.format("-> Status             : %s [%d]", bundleStatus.toString(), bundleStatus.getCode()));
		if (!bundleStatus.equals(PublishAuditStatus.Status.PUBLISHING_BUNDLE) && !bundleStatus.equals
                (PublishAuditStatus.Status.WAITING_FOR_PUBLISHING) && !bundleStatus
                .equals(Status.SUCCESS) && !bundleStatus.equals(Status.SUCCESS_WITH_WARNINGS)) {
			final int totalAttemptsFromHistory = localHistory.getNumTries();
			Logger.info(this, String.format("-> Re-publish attempts: %d out of %d", totalAttemptsFromHistory,
					MAX_NUM_TRIES));

			if(!UtilMethods.isSet(bundlesInQueue)) {
				localHistory.addNumTries();
				pubAuditAPI.updatePublishAuditStatus(auditedBundleId, bundleStatus, localHistory);
			}

			if (!isBundleInQueue(auditedBundleId, bundlesInQueue) && isInvalidBundleStatus(totalAttemptsFromHistory,
					bundleStatus)) {
				// When a bundle has been marked as "failed" but its attempts of re-publishing have NOT reached
				// MAX_NUM_TRIES, it might indicate a data consistency problem as a failed bundle MUST have
				// met such a condition.
				Logger.info(this, String.format("-> NOTE               : Bundle ID '%s' is stalled and its Audit History " +
						"will NOT be updated properly. You can safely ignore this status, or you can delete its Audit " +
						"History directly from the database.", auditedBundleId));
			}
		}
		Logger.info(this, "===========================================================");

		//Growl Notification
		if(UtilMethods.isSet(notificationMessage)) {
			SystemMessageEventUtil.getInstance().pushMessage(message.create(), ImmutableList.of(bundle.getOwner()));
		}
	}

	/**
	 * Utility method to generate the bundle title for the notification, checks the amount of assets and
	 * only shows the assetType and assetTitle of the first three items, if there are more the amount of
	 * remain assets is shown.
	 *
	 * @param bundleAssets assets that were added manually to the bundle
	 * @return a string that will be the argument for the notification
	 */
	private String generateBundleTitle(final Map<String,String> bundleAssets)
			throws LanguageException {
		int count = 0;
		String bundleTitle = "";
		for(final String id : bundleAssets.keySet()){
			if(count < 3) {
				final String assetType = bundleAssets.get(id);
				final String assetTitle = PublishAuditUtil.getInstance().getTitle(assetType, id);
				bundleTitle += "<strong>" + assetType + ":</strong> " + assetTitle + "<br/>";
				count++;
			} else {
				bundleTitle += "..." + (bundleAssets.keySet().size()-3) + " " + LanguageUtil.get("publisher_audit_more_assets");
				break;
			}
		}
		return bundleTitle;
	}

	/**
	 * Utility method to check if a bundle whose audit history is being analyzed is still present in the bundle queue.
	 *
	 * @param bundleId       The ID of the bundle whose audit history is being analyzed/updated.
	 * @param bundlesInQueue The complete list of bundles that are currently in the publishing queue.
	 *
	 * @return If the bundle is still in the queue, returns {@code true}. Otherwise, returns {@code false}.
	 */
	private boolean isBundleInQueue(final String bundleId, final List<Map<String, Object>> bundlesInQueue) {
		if (!UtilMethods.isSet(bundlesInQueue) || !UtilMethods.isSet(bundleId)) {
			return Boolean.FALSE;
		}
		final Map<String, Object> bundleFound = bundlesInQueue.stream().filter(bundle -> bundleId.equalsIgnoreCase(bundle
				.get("bundle_id").toString())).findFirst().orElse(null);
		return (UtilMethods.isSet(bundleFound) ? Boolean.TRUE : Boolean.FALSE);
	}

	/**
	 * Utility method to check if the publishing status of a bundle is invalid, based on its number of re-attempts and
	 * its current push status. This is used in combination with the {@link #isBundleInQueue(String, List)} method.
	 * So, if such a method returns {@code true} and this method returns {@code true} as well, the bundle will be
	 * considered to be in an invalid state.
	 *
	 * @param totalAttemptsFromHistory The number of publishing attempts on the bundle.
	 * @param bundleStatus             The current publishing status assigned to the bundle.
	 *
	 * @return If the bundle status is valid, returns {@code true}. Otherwise, returns {@code false}.
	 */
	private boolean isInvalidBundleStatus(final int totalAttemptsFromHistory, final Status bundleStatus) {
		if (totalAttemptsFromHistory < MAX_NUM_TRIES && (bundleStatus == Status.FAILED_TO_PUBLISH || bundleStatus ==
				Status.FAILED_TO_SEND_TO_ALL_GROUPS || bundleStatus == Status.FAILED_TO_SEND_TO_SOME_GROUPS)) {
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	private GroupPushStats getGroupStats(final Map<String, Map<String, EndpointDetail>> endpointTrackingMap) {
		final GroupPushStats groupPushStats = new GroupPushStats();

		for (final Map<String, EndpointDetail> group : endpointTrackingMap.values()) {
			boolean isGroupOk = false;
			boolean isGroupWithWarnings = false;
			boolean isGroupPublishing = false;
			boolean isGroupFailed = false;
			boolean isGroupSaved = false;
			for (final EndpointDetail detail : group.values() ) {
                if (detail.getStatus() == Status.SUCCESS.getCode()
                        || detail.getStatus() == Status.SUCCESS_WITH_WARNINGS.getCode()) {
					isGroupOk = true;

					if (detail.getStatus() == Status.SUCCESS_WITH_WARNINGS.getCode()){
                        isGroupWithWarnings = true;
                    }
				} else if ( detail.getStatus() == Status.PUBLISHING_BUNDLE
						.getCode() ) {
					isGroupPublishing = true;
				} else if ( detail.getStatus() == Status.FAILED_TO_PUBLISH
						.getCode() ) {
					isGroupFailed = true;
				} else if ( detail.getStatus() == Status.BUNDLE_SAVED_SUCCESSFULLY
						.getCode() ) {
					isGroupSaved = true;
				}
			}
			if ( isGroupOk ) {
				groupPushStats.increaseCountGroupOk();
			}
            if ( isGroupWithWarnings ) {
                groupPushStats.increaseCountGroupWithWarnings();
            }
			if ( isGroupPublishing ) {
				groupPushStats.increaseCountGroupPublishing();
			}
			if ( isGroupFailed ) {
				groupPushStats.increaseCountGroupFailed();
			}
			if ( isGroupSaved ) {
				groupPushStats.increaseCountGroupSaved();
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
		if(localHistory.getPublishStart()==null || (publishStart != null && publishStart.before(localHistory.getPublishStart()))) {
			localHistory.setPublishStart(publishStart);
		}
		if(localHistory.getPublishEnd()==null || (publishEnd != null && publishEnd.after(localHistory.getPublishEnd()))) {
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
	private List<PublishAuditHistory> getRemoteHistoryFromEndpoint(final  List<String> bundleIds,
															 final PublishingEndPoint targetEndpoint,
															 final Client client) throws DotDataException {
		final WebTarget webTarget = client.target(targetEndpoint.toURL() + "/api/auditPublishing/getAll");

		final String responseBody = webTarget
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(bundleIds, MediaType.APPLICATION_JSON))
				.readEntity(String.class);

        try {
            return (List<PublishAuditHistory>) JsonUtil.getObjectFromJson(responseBody, List.class)
                    .stream()
                    .map(item ->  PublishAuditHistory.getObjectFromString(item.toString()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new DotDataException(e);
        }
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
			final Set<Class<?>> publishers = Sets.newHashSet(
					PushPublisher.class,
					AWSS3Publisher.class,
					StaticPublisher.class);

			//Fill protocolPublisherMap with protocol -> publisher.
			for (final Class publisherClass : publishers) {
				final Publisher publisher = (Publisher) publisherClass.getDeclaredConstructor().newInstance();
				for (String protocol : publisher.getProtocols()) {
					protocolPublisherMap.put(protocol, publisherClass);
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
		} catch (final Exception e){
			Logger.error(this, String.format("Error trying to get publishers from bundle ID '%s': %s", bundleId, ExceptionUtil.getErrorMessage(e)), e);
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
		private int countGroupSaved = 0;
		private int countGroupWithWarnings = 0;

		public void increaseCountGroupOk() {
			countGroupOk++;
		}

        public void increaseCountGroupWithWarnings() {
            countGroupWithWarnings++;
        }

		public void increaseCountGroupPublishing() {
			countGroupPublishing++;
		}

		public void increaseCountGroupFailed() {
			countGroupFailed++;
		}

		public void increaseCountGroupSaved() {
			countGroupSaved++;
		}

		public int getCountGroupOk() {
			return countGroupOk;
		}

        public int getCountGroupWithWarnings() {
            return countGroupWithWarnings;
        }

		public int getCountGroupPublishing() {
			return countGroupPublishing;
		}

		public int getCountGroupFailed() {
			return countGroupFailed;
		}

		public int getCountGroupSaved() {
			return countGroupSaved;
		}
	}

	/**
	 * Returns an instance of the REST {@link Client} used to access Push Publishing end-points and
	 * retrieve their information.
	 *
	 * @return The REST {@link Client}.
	 */
	private Client getRestClient() {
		return RestClientBuilder.newClient();
	}

	/**
	 * Utility method used to reflect the appropriate error message in the Bundle Status modal in case a bundle fails
	 * during its creation process.
	 *
	 * @param auditHistory The {@link PublishAuditHistory} object for the specific failing bundle.
	 * @param errorMsg     The error message that users will read when the bundle creation process fails.
	 */
	private void updateAuditStatusErrorMsg(final PublishAuditHistory auditHistory, final String errorMsg) {
		final EndpointDetail endpointDetail = new EndpointDetail();
		endpointDetail.setStatus(PublishAuditStatus.Status.FAILED_TO_BUNDLE.getCode());
		endpointDetail.setInfo(errorMsg);
		// Environment and Endpoint IDs don't matter in this case
		auditHistory.setEndpointsMap(
				new HashMap<>(Map.of(StringPool.BLANK, Map.of(StringPool.BLANK, endpointDetail))));
	}

	private static class BundlesToSent {
		final PublishingEndPoint targetEndpoint;
		List<String>  bundleIds;


		public BundlesToSent(final PublishingEndPoint targetEndpoint) {
			this.targetEndpoint = targetEndpoint;
			this.bundleIds = new ArrayList<>();
		}

		public void add(String publisherStatus) {
			bundleIds.add(publisherStatus);
		}

		public List<String> getPublishAuditStatuses() {
			return bundleIds;
		}

		public boolean limitReach() {
			return bundleIds.size() >= MAX_SIZE_PP_AUDIT_PAYLOAD;
		}

		public void reset() {
			bundleIds.clear();
		}

		public boolean isEmpty() {
			return bundleIds.isEmpty();
		}
	}


}
