package com.dotcms.publisher.business;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publisher.pusher.PushUtils;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.system.event.local.type.pushpublish.AllPushPublishEndpointsFailureEvent;
import com.dotcms.system.event.local.type.pushpublish.AllPushPublishEndpointsSuccessEvent;
import com.dotcms.system.event.local.type.pushpublish.SinglePushPublishEndpointFailureEvent;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

public class TestPushPublisher extends PushPublisher {

    public TestPushPublisher() {
    }

    private final PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();
    private final PublishingEndPointAPI publishingEndPointAPI = APILocator.getPublisherEndPointAPI();
    private final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();


    @Override
    public PublisherConfig process (final PublishStatus status ) throws DotPublishingException {

        PublishAuditHistory currentStatusHistory = null;
        try {
            //Compressing bundle
            File bundleRoot = BundlerUtil.getBundleRoot(this.config);
            ArrayList<File> list = new ArrayList<File>(1);
            list.add(bundleRoot);
            File bundle = new File(bundleRoot+File.separator+".."+File.separator+this.config.getId()+".tar.gz");

            // If the tar.gz doesn't exist or if it the first try to push bundle
            // we need to compress the bundle folder into the tar.gz file.
            if (!bundle.exists() || !pubAuditAPI.isPublishRetry(config.getId())) {
                PushUtils.compressFiles(list, bundle, bundleRoot.getAbsolutePath());
            } else {
                Logger.info(this, "Retrying bundle: " + config.getId()
                        + ", we don't need to compress bundle again");
            }

            List<Environment> environments = APILocator.getEnvironmentAPI().findEnvironmentsByBundleId(this.config.getId());

            currentStatusHistory = pubAuditAPI.getPublishAuditStatus(this.config.getId()).getStatusPojo();
            Map<String, Map<String, EndpointDetail>> endpointsMap = currentStatusHistory.getEndpointsMap();
            // If not empty, don't overwrite publish history already set via the PublisherQueueJob
            boolean isHistoryEmpty = endpointsMap.size() == 0;
            currentStatusHistory.setPublishStart(new Date());
            PushPublishLogger.log(this.getClass(), "Status Update: Sending to all environments");
            pubAuditAPI.updatePublishAuditStatus(this.config.getId(), PublishAuditStatus.Status.SENDING_TO_ENDPOINTS, currentStatusHistory);
            //Increment numTries
            currentStatusHistory.addNumTries();
            // Counters for determining the publishing status
            int errorCounter = 0;
            int totalEndpoints = 0;
            for (Environment environment : environments) {
                List<PublishingEndPoint> allEndpoints = this.publishingEndPointAPI.findSendingEndPointsByEnvironment(environment.getId());
                List<PublishingEndPoint> endpoints = new ArrayList<PublishingEndPoint>();
                totalEndpoints += (null != allEndpoints) ? allEndpoints.size() : 0;

                Map<String, EndpointDetail> endpointsDetail = endpointsMap.get(environment.getId());
                //Filter Endpoints list and push only to those that are enabled and are Dynamic (not S3 at the moment)
                for(PublishingEndPoint ep : allEndpoints) {
                    if(ep.isEnabled() && getProtocols().contains(ep.getProtocol())) {
                        // If pushing a bundle for the first time, always add
                        // all end-points
                        if (null == endpointsDetail || endpointsDetail.size() == 0) {
                            endpoints.add(ep);
                        } else {
                            EndpointDetail epDetail = endpointsDetail.get(ep.getId());
                            // If re-trying a bundle or just re-attempting to
                            // install a bundle, send it only to those
                            // end-points whose status IS NOT success
                            if (PublisherConfig.DeliveryStrategy.ALL_ENDPOINTS.equals(this.config.getDeliveryStrategy())
                                    || (PublisherConfig.DeliveryStrategy.FAILED_ENDPOINTS.equals(this.config.getDeliveryStrategy())
                                    && PublishAuditStatus.Status.SUCCESS.getCode() != epDetail.getStatus()
                                    && Status.SUCCESS_WITH_WARNINGS.getCode() != epDetail.getStatus()
                                    && PublishAuditStatus.Status.BUNDLE_SENT_SUCCESSFULLY.getCode() != epDetail.getStatus())) {
                                endpoints.add(ep);
                            }
                        }
                    }
                }

                boolean failedEnvironment = false;
                if(!environment.getPushToAll()) {
                    Collections.shuffle(endpoints);
                    if(!endpoints.isEmpty())
                        endpoints = endpoints.subList(0, 1);
                }

                for (PublishingEndPoint endpoint : endpoints) {
                    EndpointDetail detail = new EndpointDetail();

                    try (InputStream bundleStream = new BufferedInputStream(Files.newInputStream(bundle.toPath()));) {

                        Bundle b=APILocator.getBundleAPI().getBundleById(this.config.getId());

                        System.out.println("******** PUBLISHER TEST UTIL *******");
                        System.out.println("******** PRINTING BUNDLE *******");
                        System.out.println(IOUtils.toString(bundleStream));
                        //For logging purpose
                        PushPublishLogger.log(this.getClass(), "Status Update: Bundle sent");
                        detail.setStatus(PublishAuditStatus.Status.BUNDLE_SENT_SUCCESSFULLY.getCode());
                        detail.setInfo("Everything ok");

                    } catch(Exception e) {
                        // if the bundle can't be sent after the total num of tries, delete the pushed assets for this bundle
                        if(currentStatusHistory.getNumTries() >= PublisherQueueJob.MAX_NUM_TRIES) {
                            APILocator.getPushedAssetsAPI().deletePushedAssets(this.config.getId(), environment.getId());
                        }
                        detail.setStatus(PublishAuditStatus.Status.FAILED_TO_SENT.getCode());
                        String
                                error =
                                "An error occurred for the endpoint " + endpoint.getServerName() + " with address "
                                        + endpoint.getAddress() +
                                        endpoint.getPort() + ". Error: " + e.getMessage();
                        detail.setInfo(error);
                        failedEnvironment |= true;
                        errorCounter++;
                        Logger.error(this.getClass(), error, e);

                        PushPublishLogger.log(this.getClass(), "Status Update: Failed to send bundle. Exception: " + e.getMessage());
                    }

                    if (isHistoryEmpty || failedEnvironment) {
                        currentStatusHistory.addOrUpdateEndpoint(environment.getId(), endpoint.getId(), detail);
                    }
                }
            }

            if(errorCounter==0) {
                //Updating audit table
                PushPublishLogger.log(this.getClass(), "Status Update: Bundle sent");
                pubAuditAPI.updatePublishAuditStatus(this.config.getId(),
                        PublishAuditStatus.Status.BUNDLE_SENT_SUCCESSFULLY, currentStatusHistory);

                //Triggering event listener when all endpoints are successfully sent
                localSystemEventsAPI.asyncNotify(new AllPushPublishEndpointsSuccessEvent(config));
            } else {

                    /*
                    If we have failed bundles we need to update the delivery strategy in order to only
                    retry the failing endpoints and avoid to resent to successfully endpoints
                     */

                if (errorCounter == totalEndpoints) {
                    pubAuditAPI.updatePublishAuditStatus(this.config.getId(),
                            PublishAuditStatus.Status.FAILED_TO_SEND_TO_ALL_GROUPS, currentStatusHistory);

                    //Triggering event listener when all endpoints failed during the process
                    localSystemEventsAPI.asyncNotify(new AllPushPublishEndpointsFailureEvent(config.getAssets()));
                } else {
                    pubAuditAPI.updatePublishAuditStatus(this.config.getId(),
                            PublishAuditStatus.Status.FAILED_TO_SEND_TO_SOME_GROUPS, currentStatusHistory);

                    //Triggering event listener when at least one endpoint is successfully sent but others failed
                    localSystemEventsAPI.asyncNotify(new SinglePushPublishEndpointFailureEvent(config.getAssets()));
                }
            }

            return this.config;
        } catch (Exception e) {
            //Updating audit table
            try {
                PushPublishLogger.log(this.getClass(), "Status Update: Failed to publish");
                pubAuditAPI.updatePublishAuditStatus(this.config.getId(), PublishAuditStatus.Status.FAILED_TO_PUBLISH, currentStatusHistory);
            } catch (DotPublisherException e1) {
                throw new DotPublishingException(e.getMessage(),e);
            }
            Logger.error(this.getClass(), e.getMessage(), e);
            throw new DotPublishingException(e.getMessage(),e);
        }
    }
}