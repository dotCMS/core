/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.bundlers.BinaryExporterBundler;
import com.dotcms.enterprise.publishing.bundlers.CSSExporterBundler;
import com.dotcms.enterprise.publishing.bundlers.FileAssetBundler;
import com.dotcms.enterprise.publishing.bundlers.HTMLPageAsContentBundler;
import com.dotcms.enterprise.publishing.bundlers.ShortyBundler;
import com.dotcms.enterprise.publishing.bundlers.URLMapBundler;
import com.dotcms.publisher.assets.business.PushedAssetsAPI;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.EndpointDetail;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.business.PublisherQueueJob;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.ThreadContext;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.system.event.local.type.staticpublish.AllStaticPublishEndpointsFailureEvent;
import com.dotcms.system.event.local.type.staticpublish.AllStaticPublishEndpointsSuccessEvent;
import com.dotcms.system.event.local.type.staticpublish.SingleStaticPublishEndpointFailureEvent;
import com.dotcms.system.event.local.type.staticpublish.SingleStaticPublishEndpointSuccessEvent;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.StringUtils;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * This will use several bundlers to create a static copy of the site. The bundle generated will be
 * copied to a PUBLISH_TO path in the file system locally. Listeners can be implemented in order to
 * transport the files into a desired endpoint, for example FTP, AWS S3, etc.
 *
 * @author Oscar Arrieta.
 * created on October, 2017.
 */
@PublisherConfiguration(isStatic = true)
public class StaticPublisher extends Publisher {

    private static final int REQUIRED_LICENSE_LEVEL = LicenseLevel.PLATFORM.level;

    public static final String DOTCMS_STATIC_PUBLISH_TO = "static_publish_to";
    public static final String DEFAULT_PUBLISH_TO = "dotcms-static";
    public static final String REGEX_DOT_DOT = "\\.{2}";
    public static final String PROTOCOL_STATIC = "static";

    private final PublishAuditAPI publishAuditAPI;
    private final EnvironmentAPI environmentAPI;
    private final PublishingEndPointAPI publisherEndPointAPI;
    private final PushedAssetsAPI pushedAssetsAPI;
    private final PublisherAPI publisherAPI;

    private LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();

    /**
     * Class constructor.
     */
    public StaticPublisher() {
        this.publishAuditAPI = PublishAuditAPI.getInstance();
        this.environmentAPI = APILocator.getEnvironmentAPI();
        this.publisherEndPointAPI = APILocator.getPublisherEndPointAPI();
        this.pushedAssetsAPI = APILocator.getPushedAssetsAPI();
        this.publisherAPI    = PublisherAPI.getInstance();
    }

    /**
     * Safety check to review that the current dotCMS instance is assigned the correct license level
     * to perform this functionality.
     */
    private void checkLicense() {
        if (LicenseUtil.getLevel() < REQUIRED_LICENSE_LEVEL) {
            throw new RuntimeException("Need a Platform license to run this functionality");
        }
    } //checkLicense.

    @Override
    public PublisherConfig init(PublisherConfig config) throws DotPublishingException {

        this.checkLicense();

        try {
            config = (PublisherConfig) config.clone();
            config.setStatic(true);
            config.put(DOT_STATIC_DATE, new Date());

            this.config = super.init(config);
        } catch (CloneNotSupportedException e) {
        }

        return this.config;
    } // init.

    @Override
    public PublisherConfig process(PublishStatus status) throws DotPublishingException {
        this.checkLicense();

        Logger.info(this, "Starting Static Publish process");

        PublishAuditHistory currentStatusHistory = null;
        final String configId = config.getId();

        try {
            this.compressBundleIfNeeded();

            //We need to update audit table.
            currentStatusHistory = startStatusHistory();

            int failedEnvironmentCounter = 0;

            List<Environment> environments = environmentAPI.findEnvironmentsByBundleId(configId);
            for (Environment environment : environments) {

                if (handleEnvironment(environment, currentStatusHistory)) {
                    failedEnvironmentCounter++;
                }
            }

            if (failedEnvironmentCounter == 0) {
                //Updating Audit table
                PushPublishLogger.log(this.getClass(), "Status Update: Bundle sent");
                this.publishAuditAPI.updatePublishAuditStatus(config.getId(),
                    PublishAuditStatus.Status.BUNDLE_SAVED_SUCCESSFULLY, currentStatusHistory);

                //Triggering static event listener when all endpoints are successfully sent.
                localSystemEventsAPI.asyncNotify(new AllStaticPublishEndpointsSuccessEvent(config));
                publisherAPI.deleteElementsFromPublishQueueTable(config.getId());
            } else {
                if (failedEnvironmentCounter == environments.size()) {
                    this.publishAuditAPI.updatePublishAuditStatus(config.getId(),
                        PublishAuditStatus.Status.FAILED_TO_SEND_TO_ALL_GROUPS,
                        currentStatusHistory);

                    //Triggering static event listener when all endpoints failed during the process.
                    localSystemEventsAPI.asyncNotify(new AllStaticPublishEndpointsFailureEvent(config.getAssets()));
                } else {
                    this.publishAuditAPI.updatePublishAuditStatus(config.getId(),
                        PublishAuditStatus.Status.FAILED_TO_SEND_TO_SOME_GROUPS,
                        currentStatusHistory);

                    //Triggering static event listener when at least one endpoint is successfully sent but others failed.
                    localSystemEventsAPI.asyncNotify(new SingleStaticPublishEndpointFailureEvent(config.getAssets()));
                }
            }

        } catch (IOException | DotPublisherException | DotDataException e) {
            try {
                PushPublishLogger.log(this.getClass(), "Status Update: Failed to publish");
                this.publishAuditAPI.updatePublishAuditStatus(
                    configId,
                    Status.FAILED_TO_PUBLISH,
                    currentStatusHistory);

            } catch (DotPublisherException dpE) {
                throw new DotPublishingException(dpE.getMessage());
            }
            Logger.error(this.getClass(), e.getMessage(), e);
            throw new DotPublishingException(e.getMessage(),e);
        }

        Logger.info(this, "End of Static Publish process");
        return config;
    } //process.

    /**
     * Handle each static endpoint under the environment passed. If error, saves the error in the
     * detail of the audit. If success, updates the audit with Success code.
     */
    private boolean handleEnvironment(Environment environment,
                                      PublishAuditHistory currentStatusHistory)
        throws DotDataException{

        EndpointDetail detail = new EndpointDetail();
        boolean hasEnvironmentFailed = false;
        boolean isHistoryEmpty = currentStatusHistory.getEndpointsMap().isEmpty();

        for (PublishingEndPoint endpoint : getStaticEndpointsByEnviroment(environment)) {
            try {
                handleEndpoint(endpoint);
            } catch (DotDataException | DotPublishingException e) {
                // if the bundle can't be sent after the total num of tries, delete the pushed assets for this bundle
                if (currentStatusHistory.getNumTries() >= PublisherQueueJob.MAX_NUM_TRIES) {
                    this.pushedAssetsAPI.deletePushedAssets(config.getId(), environment.getId());
                }
                detail.setStatus(PublishAuditStatus.Status.FAILED_TO_PUBLISH.getCode());
                String error =
                    "An error occurred for the endpoint " + endpoint.getId() + " with address "
                        + endpoint.getAddress() + ".  Error: " + e.getMessage();
                detail.setInfo(error);
                hasEnvironmentFailed = true;

                Logger.error(this.getClass(), error, e);
                PushPublishLogger.log(this.getClass(), "Status Update: Failed to publish bundle");
            } finally {
                ThreadContext.remove(BUNDLE_ID);
                ThreadContext.remove(ENDPOINT_NAME);
            }

            if (detail.getStatus() == 0) {
                detail.setStatus(Status.BUNDLE_SAVED_SUCCESSFULLY.getCode());
                detail.setInfo("Bundle saved successfully for Endpoint " + endpoint.getId());
                currentStatusHistory.setPublishEnd(new Date());
            }

            // If not empty, don't overwrite publish history already set via the PublisherQueueJob.
            if (isHistoryEmpty || hasEnvironmentFailed) {
                currentStatusHistory
                    .addOrUpdateEndpoint(environment.getId(), endpoint.getId(), detail);
            }

            if (detail.getStatus() == Status.BUNDLE_SAVED_SUCCESSFULLY.getCode()) {
                localSystemEventsAPI
                        .asyncNotify(new SingleStaticPublishEndpointSuccessEvent(config, endpoint));
            }else{
                localSystemEventsAPI
                        .asyncNotify(new SingleStaticPublishEndpointFailureEvent(config.getAssets()));
            }
        }

        return hasEnvironmentFailed;
    } //handleEnvironment.

    /**
     * Retrieves endpoint properties and build the publish-to folder path with that information.
     * Then it will pass /live folder to handleLiveFolder method.
     */
    private void handleEndpoint(PublishingEndPoint endpoint)
        throws DotDataException, DotPublishingException {

        //For logging purpose
        ThreadContext.put(ENDPOINT_NAME, ENDPOINT_NAME + "=" + endpoint.getServerName());

        final Properties props = getEndPointProperties(endpoint);
        final String publishTo = props.getProperty(DOTCMS_STATIC_PUBLISH_TO);
        config.put(DOTCMS_STATIC_PUBLISH_TO, publishTo);

        //Getting the host name, then the languages under the bundle.
        final File bundleRoot = BundlerUtil.getBundleRoot(this.config);

        //For logging purpose.
        ThreadContext.put(BUNDLE_ID, BUNDLE_ID + "=" + bundleRoot.getName());

        final File liveFolder = new File(bundleRoot.getAbsolutePath() + LIVE_FOLDER);

        if (liveFolder.exists()) {
            PushPublishLogger.log(this.getClass(), "Status Update: Moving bundle");
            handleLiveFolder(liveFolder);

        } else {
            Logger.warn(this.getClass(), "Bundle is EMPTY");
            PushPublishLogger.log(this.getClass(), "Bundle is EMPTY");
        }
    } //handleEndpoint.

    /**
     * Finds all endpoints under the environment and filter just the ones that are static. If the
     * environment is set up to push only to endpoint we will grab just one randomly.
     *
     * @return List of all static endpoints under the environment.
     */
    private List<PublishingEndPoint> getStaticEndpointsByEnviroment(Environment environment)
        throws DotDataException {
        List<PublishingEndPoint> endpoints = Lists.newArrayList();

        //Filter Endpoints list and push only to those that are enabled and ARE static.
        final List<PublishingEndPoint> sendingEndPointsByEnvironment =
            this.publisherEndPointAPI.findSendingEndPointsByEnvironment(environment.getId());

        for (PublishingEndPoint ep : sendingEndPointsByEnvironment) {
            if (ep.isEnabled() && getProtocols().contains(ep.getProtocol())) {
                endpoints.add(ep);
            }
        }

        //We just one random server if the environment is set up to push to just one.
        if (!endpoints.isEmpty() && !environment.getPushToAll()) {
            Collections.shuffle(endpoints);
            endpoints = endpoints.subList(0, 1);
        }

        return endpoints;
    } //getStaticEndpointsByEnviroment.

    /**
     * 1. Gets {@link PublishAuditHistory} status based on the config id.
     * 2. Sets the current date as Publish start.
     * 3. Updates the status in DB with status SENDING_TO_ENDPOINTS.
     * 4. Adds 1 to the number of tries to star the count.
     */
    private PublishAuditHistory startStatusHistory() throws DotPublisherException {
        final String configId = config.getId();
        PublishAuditHistory currentStatusHistory =
            publishAuditAPI.getPublishAuditStatus(configId).getStatusPojo();

        currentStatusHistory.setPublishStart(new Date());
        currentStatusHistory.addNumTries();

        publishAuditAPI.updatePublishAuditStatus(configId, Status.SENDING_TO_ENDPOINTS,
            currentStatusHistory);

        return currentStatusHistory;
    } //startStatusHistory.

    /**
     * Handle Hosts and Languages in bundle path in order to construct "Push To" value and its
     * variables.
     */
    private void handleLiveFolder(final File liveFolder)
        throws DotPublishingException {

        //For each host.
        for (File hostFolder : liveFolder.listFiles(FileUtil.getOnlyFolderFileFilter())) {
            Host host = getHostFromFilePath(hostFolder);
            config.put(CURRENT_HOST, host);

            final Collection<LanguageFolder> languagesFolders = getLanguageFolders(hostFolder);

            //For each language.
            for (LanguageFolder languageFolder : languagesFolders) {
                //For each file under i.e. /live/demo.dotcms.com/1/
                Language language = languageFolder.getLanguage();
                config.put(CURRENT_LANGUAGE, Long.toString(language.getId()));

                final File publishToFolder = getPublishToFolder();

                Collection<File> listFiles = Arrays.asList(languageFolder.getLanguageFolder().listFiles());

                for (File file : listFiles) {
                    try {
                        if (file.isDirectory()) {
                            FileUtils.copyDirectoryToDirectory(file, publishToFolder);
                        } else {
                            FileUtils.copyFileToDirectory(file, publishToFolder);
                        }
                    } catch (IOException e) {
                        Logger.error(this, "Error copying file: " + file.getAbsolutePath(), e);
                    }

                }
            }
        }
    } //handleLiveFolder.

    /**
     * Retrieves publish-to endpoint value, if the value has variables this mehod will interpolate
     * with already known data like Sites, Dates, Languages, etc.
     */
    private String getPublishTo() {
        final String publishTo = (String) config.get(DOTCMS_STATIC_PUBLISH_TO);
        final Map<String, Object> contextMap = this.getContextMap(publishTo, config);
        final String folderName = StringUtils.interpolate(publishTo, contextMap);

        return (null != folderName && folderName.trim().length() > 0) ?
            this.normalizePublishTo(folderName) : DEFAULT_PUBLISH_TO;
    } //getPublishTo.

    /**
     * Clean publish-to property value from endpoint properties.
     */
    private String normalizePublishTo(final String publishTo) {
        String normalizedBucketName = publishTo.replaceAll(REGEX_DOT_DOT, "");
        return normalizedBucketName.toLowerCase();
    } // normalizePublishTo.

    /**
     * Finds the path for static publish from configuration and adds the publish-to path from the
     * endpoint properties in order to create a folder where Static Publish is going to store its
     * data.
     */
    private File getPublishToFolder() {
        final File folder = new File(BundlerUtil.getStaticBundleRoot(config), getPublishTo());
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    } //getPublishToFolder.

    @Override
    public List<Class> getBundlers() {
        final List<Class> list = new ArrayList<>();

        list.add(StaticDependencyBundler.class);
        list.add(FileAssetBundler.class);
        list.add(HTMLPageAsContentBundler.class);
        list.add(URLMapBundler.class);
        list.add(BinaryExporterBundler.class);
        list.add(CSSExporterBundler.class);
        list.add(ShortyBundler.class);
        list.add(StaticFolderBundler.class);

        return list;
    } //getBundlers.

    @Override
    public Set<String> getProtocols() {
        Set<String> protocols = new HashSet<>();
        protocols.add(PROTOCOL_STATIC);
        return protocols;
    }
}
