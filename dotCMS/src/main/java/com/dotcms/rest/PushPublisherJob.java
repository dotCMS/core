package com.dotcms.rest;

import com.dotcms.publisher.business.PublishAuditStatus;
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
	private static final String DEPLOY_ALLOWED_KEY = "deployAllowed";
	private String bundleName;
	private String endpointId;
	private String groupId;
	private PublishAuditStatus status;

    /**
     * Processes a received bundle with the {@link BundlePublisher}.
     *
     * @param bundleName   File name of the received bundle.
     * @param status       Audit status of the bundle.
     * @param deployAllowed Whether the bundle may deploy or remove plugin jars. Callers set it from
     *                     how the bundle arrived; when it is {@code false} the plugin part of the
     *                     bundle is ignored.
     * @return The configuration used to process the bundle.
     */
    public PublisherConfig processBundle(final String bundleName, final PublishAuditStatus status,
            final boolean deployAllowed) {
		//Configure and Invoke the Publisher
		Logger.info(PushPublisherJob.class, "Started bundle publish process");
		PushPublishLogger.log(PushPublisherJob.class, "Started bundle publish process", bundleName);

		PublisherConfig config = new PublisherConfig();
		BundlePublisher bundlePublisher = new BundlePublisher();
		config.setId(bundleName);
		config.setEndpoint(endpointId);
		config.setGroupId(groupId);
		config.setPublishAuditStatus(status);
		config.put(PublisherConfig.Config.OSGI_DEPLOY_ALLOWED.name(), deployAllowed);
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
	 * Sends for processing a given bundle to the {@link BundlePublisher}. Reads the bundle name, its status and the plugin deployment flag from the
	 * trigger data and processes the bundle. A trigger without the flag never deploys plugins.
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

		// a job without the flag (for example queued by an older version) never deploys plugins
		final boolean deployAllowed = Boolean.TRUE.equals(executionData.get(DEPLOY_ALLOWED_KEY));
		processBundle(bundleName, status, deployAllowed);
	}

	/**
	 * Creates {@link JobDataMap} and {@link JobDetail} instances to trigger the Push Publisher Job.
	 *
	 * @param bundleName Bundle file name
	 * @param status PublishAuditStatus
	 * @param deployAllowed Whether the bundle may deploy or remove plugin jars; see
	 *                      {@link #processBundle(String, PublishAuditStatus, boolean)}
	 */
	public static void triggerPushPublisherJob(final String bundleName,
			final PublishAuditStatus status, final boolean deployAllowed) {
		Logger.debug(PushPublisherJob.class, "Triggering Push Publisher Job for bundle: " + bundleName);
		Logger.debug(PushPublisherJob.class, "Status: " + status.getStatus().name());
		final ImmutableMap<String, Serializable> nextExecutionData = ImmutableMap
				.of("bundleName", bundleName, "status", status, DEPLOY_ALLOWED_KEY, deployAllowed);

		try {
			DotStatefulJob.enqueueTrigger(nextExecutionData, PushPublisherJob.class);
		} catch (Exception e) {
			Logger.error(PushPublisherJob.class, "Error scheduling the Publishing of bundle. Bundle name: " + bundleName, e);
			throw new DotRuntimeException("Error scheduling the reset of permissions", e);
		}
	}
}