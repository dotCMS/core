package com.dotcms.rest;

import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.receiver.BundleOrigin;
import com.dotcms.publisher.receiver.BundlePluginAccess;
import com.dotcms.publisher.receiver.BundlePublisher;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.quartz.job.ResetPermissionsJob;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.Map;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;

public class PushPublisherJob extends DotStatefulJob {
	private static final String ORIGIN_KEY = "origin";
	private static final String UPLOADER_KEY = "uploaderId";
	private String bundleName;
	private String endpointId;
	private String groupId;
	private PublishAuditStatus status;

    /**
     * Processes a received bundle with the {@link BundlePublisher}.
     *
     * @param bundleName   File name of the received bundle.
     * @param status       Audit status of the bundle.
     * @param origin       How the bundle arrived, or {@code null} if unknown; an unknown origin
     *                     never deploys plugins.
     * @param uploaderId   Id of the user who uploaded the bundle, for a user upload.
     * @return The configuration used to process the bundle.
     */
    public PublisherConfig processBundle(final String bundleName, final PublishAuditStatus status,
            final BundleOrigin origin, final String uploaderId) {
		//Configure and Invoke the Publisher
		Logger.info(PushPublisherJob.class, "Started bundle publish process");
		PushPublishLogger.log(PushPublisherJob.class, "Started bundle publish process", bundleName);

		PublisherConfig config = new PublisherConfig();
		BundlePublisher bundlePublisher = new BundlePublisher();
		config.setId(bundleName);
		config.setEndpoint(endpointId);
		config.setGroupId(groupId);
		config.setPublishAuditStatus(status);
		if (origin != null) {
			config.put(PublisherConfig.Config.BUNDLE_ORIGIN.name(), origin.name());
		}
		if (uploaderId != null) {
			config.put(PublisherConfig.Config.UPLOADED_BY.name(), uploaderId);
		}
		try {
			bundlePublisher.init(config);
			config = bundlePublisher.process(null);
		} catch (DotPublishingException e) {
			Logger.error("Failed to publish because an error occurred: ", e.getMessage());
		}

		PushPublishLogger.log(PushPublisherJob.class, "Finished bundle publish process", bundleName);
		Logger.info(PushPublisherJob.class, "Finished bundle publish process");

		return config;
	}

	/**
	 * Sends for processing a given bundle to the {@link BundlePublisher}. Reads the bundle name, its status and how it arrived from the
	 * trigger data and processes the bundle. A trigger without an origin never deploys plugins.
	 *
	 * @see PublisherConfig
	 * @see BundlePublisher
	 * @param jobContext The Quartz execution context.
	 * @throws JobExecutionException If the job cannot run.
	 */
	@Override
	public void run(JobExecutionContext jobContext) throws JobExecutionException {
		Logger.debug(PushPublisherJob.class, "Running Push Publisher Job");
		Logger.debug(PushPublisherJob.class, "Job context: " + jobContext);
		final Trigger trigger = jobContext.getTrigger();
		final Map<String, Serializable> executionData = getExecutionData(trigger, PushPublisherJob.class);

		final String bundleName = (String) executionData.get("bundleName");
		final PublishAuditStatus status = (PublishAuditStatus) executionData.get("status");

		// a job without an origin (for example queued by an older version) never deploys plugins
		final BundleOrigin origin = BundlePluginAccess.parseOrigin(executionData.get(ORIGIN_KEY));
		final Serializable uploaderId = executionData.get(UPLOADER_KEY);
		processBundle(bundleName, status, origin, uploaderId instanceof String ? (String) uploaderId : null);
	}

	/**
	 * Creates {@link JobDataMap} and {@link JobDetail} instances to trigger the Push Publisher Job.
	 *
	 * @param bundleName Bundle file name
	 * @param status PublishAuditStatus
	 * @param origin How the bundle arrived; see
	 *               {@link #processBundle(String, PublishAuditStatus, BundleOrigin, String)}
	 * @param uploaderId Id of the user who uploaded the bundle, for a user upload; may be {@code null}
	 */
	public static void triggerPushPublisherJob(final String bundleName,
			final PublishAuditStatus status, final BundleOrigin origin, final String uploaderId) {
		Logger.debug(PushPublisherJob.class, "Triggering Push Publisher Job for bundle: " + bundleName);
		Logger.debug(PushPublisherJob.class, "Status: " + status.getStatus().name());
		final ImmutableMap.Builder<String, Serializable> data = ImmutableMap.<String, Serializable>builder()
				.put("bundleName", bundleName)
				.put("status", status);
		if (origin != null) {
			data.put(ORIGIN_KEY, origin.name());
		}
		if (uploaderId != null) {
			data.put(UPLOADER_KEY, uploaderId);
		}
		final ImmutableMap<String, Serializable> nextExecutionData = data.build();

		try {
			DotStatefulJob.enqueueTrigger(nextExecutionData, PushPublisherJob.class);
		} catch (Exception e) {
			Logger.error(PushPublisherJob.class, "Error scheduling the Publishing of bundle. Bundle name: " + bundleName, e);
			throw new DotRuntimeException("Error scheduling the reset of permissions", e);
		}
	}
}