package com.dotcms.rest;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.EndpointDetail;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.receiver.BundlePublisher;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
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
    	//Configure and Invoke the Publisher
    	PushPublishLogger.log(PublishThread.class, "Started bundle publish process", bundleName);

		PublisherConfig pconf = new PublisherConfig();
		BundlePublisher bundlePublisher = new BundlePublisher();
		pconf.setId(bundleName);
		pconf.setEndpoint(endpointId);
		pconf.setGroupId(groupId);
		try {
			bundlePublisher.init(pconf);
			bundlePublisher.process(null);
		} catch (DotPublishingException e) {

			EndpointDetail detail = new EndpointDetail();
			detail.setStatus(PublishAuditStatus.Status.FAILED_TO_PUBLISH.getCode());
			detail.setInfo("Failed to publish because an error occurred: "+e.getMessage());
			status.getStatusPojo().addOrUpdateEndpoint(groupId,endpointId, detail);

			try {
				PublishAuditAPI.getInstance().updatePublishAuditStatus(bundleName.substring(0, bundleName.indexOf(".tar.gz")),
						PublishAuditStatus.Status.FAILED_TO_PUBLISH,
						status.getStatusPojo());
			} catch (DotPublisherException e1) {
				PushPublishLogger.log(PublishThread.class, "Unable to update audit status ", bundleName);
			}
		}

		PushPublishLogger.log(PublishThread.class, "Finished bundle publish process", bundleName);
    }
}