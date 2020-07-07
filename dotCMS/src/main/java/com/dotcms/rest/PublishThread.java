package com.dotcms.rest;

import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.receiver.BundlePublisher;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;

public class PublishThread implements Runnable {
	private String bundleName;
	private String endpointId;
	private String groupId;
	private PublishAuditStatus status;

	public PublishThread(String bundleName, String groupId, String endpointId, PublishAuditStatus status) {
		this.bundleName = bundleName;
		this.endpointId = endpointId;
		this.status = status;
		this.groupId = groupId;
	}

    /**
     * Sends for processing a given bundle to the {@link BundlePublisher}
     *
     * @see PublisherConfig
     * @see BundlePublisher
     */
    public void run() {
		processBundle();
    }

    public PublisherConfig processBundle() {
		//Configure and Invoke the Publisher
		Logger.info(PublishThread.class, "Started bundle publish process");
		PushPublishLogger.log(PublishThread.class, "Started bundle publish process", bundleName);

		PublisherConfig config = new PublisherConfig();
		BundlePublisher bundlePublisher = new BundlePublisher();
		config.setId(bundleName);
		config.setEndpoint(endpointId);
		config.setGroupId(groupId);
		config.setPublishAuditStatus(status);
		try {
			bundlePublisher.init(config);
			config = bundlePublisher.process(null);
		} catch (DotPublishingException e) {
			Logger.error("Failed to publish because an error occurred: ", e.getMessage());
		}

		PushPublishLogger.log(PublishThread.class, "Finished bundle publish process", bundleName);
		Logger.info(PublishThread.class, "Finished bundle publish process");

		return config;
	}
}