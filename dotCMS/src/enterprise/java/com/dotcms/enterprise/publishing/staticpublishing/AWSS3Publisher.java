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
import com.dotcms.enterprise.publishing.bundlers.*;
import com.dotcms.publisher.assets.business.PushedAssetsAPI;
import com.dotcms.publisher.business.*;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotcms.publisher.pusher.PushUtils;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.*;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.*;
import com.google.common.annotations.VisibleForTesting;
import org.apache.logging.log4j.ThreadContext;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Similar to the TimeMachine this will be use several bundlers to create a static copy of the site.
 * Usually the site generated will be push to a bucket in a remote server, for instance AWS S3.
 *
 * @author jsanca
 */
@PublisherConfiguration (isStatic = true)
public class AWSS3Publisher extends Publisher {

    private static final int REQUIRED_LICENSE_LEVEL                      = LicenseLevel.PLATFORM.level; // Super Prime license.
    public static final String DOTCMS_PUSH_AWS_S3_BUCKET_ID              = "aws_bucket_name";
    public static final String DOTCMS_PUSH_AWS_S3_BUCKET_ROOT_PREFIX     = "aws_bucket_folder_prefix";
    public static final String DOTCMS_PUSH_AWS_S3_BUCKET_REGION          = "aws_bucket_region";
    public static final String DOTCMS_PUSH_AWS_S3_BUCKET_VALIDATION_NAME = "aws_validation_bucket";
    public static final String DOTCMS_PUSH_AWS_S3_ENDPOINT               = "aws_endpoint";
    public static final String DOTCMS_PUSH_AWS_S3_BUCKET_REGION_DEFAULT  = "us-east-1";
    public static final String DOTCMS_PUSH_AWS_S3_TOKEN                  = "aws_access_key";
    public static final String DOTCMS_PUSH_AWS_S3_SECRET                 = "aws_secret_access_key";
    public static final String PROTOCOL_AWS_S3                           = "awss3";
    public static final String DEFAULT_BUCKET_NAME                       = "dot-bucket-default";

    private static final String CREATED_BUCKETS                          = "createdBuckets";

    private final HostAPI hostAPI;
    private final PublishAuditAPI publishAuditAPI;
    private final EnvironmentAPI environmentAPI;
    private final PublishingEndPointAPI publisherEndPointAPI;
    private final PushedAssetsAPI pushedAssetsAPI;
    private final S3VanityAliasService vanityAliasService;
    private final ContentletAPI contentletAPI;
    private final LanguageAPI languageAPI;

    /**
     * Class constructor.
     */
    public AWSS3Publisher() {
    	this.hostAPI = APILocator.getHostAPI();
        this.publishAuditAPI = PublishAuditAPI.getInstance();
        this.environmentAPI = APILocator.getEnvironmentAPI();
        this.publisherEndPointAPI = APILocator.getPublisherEndPointAPI();
        this.pushedAssetsAPI = APILocator.getPushedAssetsAPI();
        this.vanityAliasService = new S3VanityAliasService();
        this.contentletAPI = APILocator.getContentletAPI();
        this.languageAPI = APILocator.getLanguageAPI();
    }

    /**
     * Test driven constructor
     * @param hostAPI
     * @param publishAuditAPI
     * @param environmentAPI
     * @param publisherEndPointAPI
     * @param pushedAssetsAPI
     */
    @VisibleForTesting
    public AWSS3Publisher(final HostAPI hostAPI,
            final PublishAuditAPI publishAuditAPI,
            final EnvironmentAPI environmentAPI,
            final PublishingEndPointAPI publisherEndPointAPI,
            final PushedAssetsAPI pushedAssetsAPI) {
        this(hostAPI, publishAuditAPI, environmentAPI, publisherEndPointAPI, pushedAssetsAPI,
                new S3VanityAliasService());
    }

    /**
     * Test driven constructor
     * @param hostAPI
     * @param publishAuditAPI
     * @param environmentAPI
     * @param publisherEndPointAPI
     * @param pushedAssetsAPI
     * @param vanityAliasService
     */
    @VisibleForTesting
    public AWSS3Publisher(final HostAPI hostAPI,
            final PublishAuditAPI publishAuditAPI,
            final EnvironmentAPI environmentAPI,
            final PublishingEndPointAPI publisherEndPointAPI,
            final PushedAssetsAPI pushedAssetsAPI,
            final S3VanityAliasService vanityAliasService) {
        this.hostAPI = hostAPI;
        this.publishAuditAPI = publishAuditAPI;
        this.environmentAPI = environmentAPI;
        this.publisherEndPointAPI = publisherEndPointAPI;
        this.pushedAssetsAPI = pushedAssetsAPI;
        this.vanityAliasService = vanityAliasService;
        this.contentletAPI = APILocator.getContentletAPI();
        this.languageAPI = APILocator.getLanguageAPI();
    }

    /**
	 * Safety check to review that the current dotCMS instance is assigned the
	 * correct license level to perform this functionality.
	 */
    private void checkLicense() {
        if(LicenseUtil.getLevel() < REQUIRED_LICENSE_LEVEL) {
            throw new RuntimeException("Need an enterprise licence to run this functionality");
        }
    } //checkLicense

    @Override
    public PublisherConfig init(PublisherConfig config) throws DotPublishingException {

        this.checkLicense();

        try {
	        config = (PublisherConfig) config.clone();
	        config.setStatic(true);
	        config.put(DOT_STATIC_DATE, new Date());

	        this.config = super.init(config);
        } catch (CloneNotSupportedException e){}

        return this.config;
    } // init.

    private Map<String, Boolean> shouldForcePushCache = new HashMap<>();

    @Override
    public boolean shouldForcePush(String hostId, long languageId) {
    	String cacheKey = hostId +"/"+ languageId;
    	Boolean cachedValue = shouldForcePushCache.get(cacheKey);
    	if (cachedValue != null) {
    		return cachedValue;
    	}

    	boolean result = false;
    	try {
        	Host host = hostAPI.find(hostId, APILocator.getUserAPI().getSystemUser(), false);
            if(host != null) {
	            List<Environment> environments = this.environmentAPI.findEnvironmentsByBundleId(this.config.getId());

	            outerLoop:
	            for (Environment environment : environments) {
	                List<PublishingEndPoint> endpoints = new ArrayList<>();

	                //Filter Endpoints list and push only to those that are enabled and ARE static (S3 at the moment)
	                for(PublishingEndPoint ep : this.publisherEndPointAPI.findSendingEndPointsByEnvironment(environment.getId())) {
	                    if(ep.isEnabled() && getProtocols().contains(ep.getProtocol())) {
	                        endpoints.add(ep);
	                    }
	                }

	                for (PublishingEndPoint endpoint : endpoints) {
	                    Properties props = getEndPointProperties(endpoint);

	                    String tokenProp = props.getProperty(DOTCMS_PUSH_AWS_S3_TOKEN);
	                    String secretProp = props.getProperty(DOTCMS_PUSH_AWS_S3_SECRET);
	                    String bucketIDProp = props.getProperty(DOTCMS_PUSH_AWS_S3_BUCKET_ID);
                        String endPointProp = props.getProperty(DOTCMS_PUSH_AWS_S3_ENDPOINT);
                        String bucketRegion = props.getProperty(DOTCMS_PUSH_AWS_S3_BUCKET_REGION);

						AWSS3EndPointPublisher endPointPublisher = getAWSS3EndPointPublisher(tokenProp,
                                secretProp, endPointProp, bucketRegion);

	                    Object oldHost = config.get(CURRENT_HOST);
	                    Object oldLanguage = config.get(CURRENT_LANGUAGE);
	                    Object oldBucketId = config.get(DOTCMS_PUSH_AWS_S3_BUCKET_ID);
	                    try {
	                        config.put(CURRENT_HOST, host);
	                        config.put(CURRENT_LANGUAGE, Long.toString(languageId));
	                        config.put(DOTCMS_PUSH_AWS_S3_BUCKET_ID, bucketIDProp);

	                        final String bucketName = getBucketName(this.config);

	                        if (endPointPublisher.exists(bucketName)) {
	                        	result = true;

	                        	break outerLoop;
	                        }
	                    } finally {
	                        config.put(CURRENT_HOST, oldHost);
	                        config.put(CURRENT_LANGUAGE, oldLanguage);                    	
	                        config.put(DOTCMS_PUSH_AWS_S3_BUCKET_ID, oldBucketId);

	                        endPointPublisher.shutdownTransferManager();
	                    }
	                }
	            }
            }
    	} catch (Exception e){
            Logger.error(this.getClass(), e.getMessage(), e);
    	}

    	shouldForcePushCache.put(cacheKey, result);
    	return result;
    }

    @Override
    public PublisherConfig process(final PublishStatus status) throws DotPublishingException {
        this.checkLicense();
        PublishAuditHistory currentStatusHistory = null;
        try {
        	/*
        	 * Inhibited bundle compression for static-publishing scenario
        	 * Performance enhancement due to https://github.com/dotCMS/core/issues/12291
        	 */
        	if (Config.getBooleanProperty("STATIC_PUBLISHING_GENERATE_TAR_GZ", false)) {
				// Compressing the bundle
				File bundleToCompress = BundlerUtil.getBundleRoot(config);
				ArrayList<File> list = new ArrayList<>(1);
				list.add(bundleToCompress);
				File bundle = new File(bundleToCompress + File.separator + ".." + File.separator + config.getId() + ".tar.gz");
				PushUtils.compressFiles(list, bundle, bundleToCompress.getAbsolutePath());
        	}

        	List<Environment> environments = this.environmentAPI.findEnvironmentsByBundleId(this.config.getId());
            //Updating audit table
            currentStatusHistory = this.publishAuditAPI.getPublishAuditStatus(config.getId()).getStatusPojo();
            Map<String, Map<String, EndpointDetail>> endpointsMap = currentStatusHistory.getEndpointsMap();
            // If not empty, don't overwrite publish history already set via the PublisherQueueJob
            boolean isHistoryEmpty = endpointsMap.size() == 0;
            currentStatusHistory.setPublishStart(new Date());
            this.publishAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.SENDING_TO_ENDPOINTS, currentStatusHistory);
            //Increment numTries
            currentStatusHistory.addNumTries();
            int failedEnvironmentCounter = 0;

            for (Environment environment : environments) {
                List<PublishingEndPoint> allEndpoints = this.publisherEndPointAPI.findSendingEndPointsByEnvironment(environment.getId());
                List<PublishingEndPoint> endpoints = new ArrayList<>();

                //Filter Endpoints list and push only to those that are enabled and ARE static (S3 at the moment)
                for(PublishingEndPoint ep : allEndpoints) {
                    if(ep.isEnabled() && getProtocols().contains(ep.getProtocol())) {
                        endpoints.add(ep);
                    }
                }

                boolean failedEnvironment = false;

                if(!environment.getPushToAll()) {
                    Collections.shuffle(endpoints);
                    if(!endpoints.isEmpty())
                        endpoints = endpoints.subList(0, 1);
                }

                for (PublishingEndPoint endpoint : endpoints) {

                    //For logging purpose
                    ThreadContext.put(ENDPOINT_NAME, ENDPOINT_NAME + "=" + endpoint.getServerName());

                    Properties props = getEndPointProperties(endpoint);

                    String tokenProp = props.getProperty(DOTCMS_PUSH_AWS_S3_TOKEN);
                    String secretProp = props.getProperty(DOTCMS_PUSH_AWS_S3_SECRET);
                    String bucketIDProp = props.getProperty(DOTCMS_PUSH_AWS_S3_BUCKET_ID);
                    String bucketPrefixProp = props.getProperty(DOTCMS_PUSH_AWS_S3_BUCKET_ROOT_PREFIX);
                    String bucketRegion = props.getProperty(DOTCMS_PUSH_AWS_S3_BUCKET_REGION);
                    String bucketValidationName = props.getProperty(DOTCMS_PUSH_AWS_S3_BUCKET_VALIDATION_NAME);
                    String wsEndpoint = props.getProperty(DOTCMS_PUSH_AWS_S3_ENDPOINT);

                    //For each endpoint, we reset the bucket list name
                    config.put(CREATED_BUCKETS, new HashSet<String>());

                    config.put(DOTCMS_PUSH_AWS_S3_BUCKET_ID, bucketIDProp);
                    if (UtilMethods.isSet(bucketPrefixProp)){
                        config.put(DOTCMS_PUSH_AWS_S3_BUCKET_ROOT_PREFIX, bucketPrefixProp);
                    } else {
                        config.remove(DOTCMS_PUSH_AWS_S3_BUCKET_ROOT_PREFIX);
                    }

                    if (!UtilMethods.isSet(bucketRegion)){
                        bucketRegion = DOTCMS_PUSH_AWS_S3_BUCKET_REGION_DEFAULT;
                    }

					AWSS3EndPointPublisher endPointPublisher = getAWSS3EndPointPublisher(tokenProp,
                            secretProp, wsEndpoint, bucketRegion);
                    EndpointDetail detail = new EndpointDetail();

                    try {
                        endPointPublisher.checkConnectSuccessfully(bucketValidationName);
                    } catch (final EndPointPublisherConnectionException e) {
                        String error = updateStatusFailedToSend(currentStatusHistory, environment, endpoint, detail);
                        failedEnvironment |= true;
                        Logger.error(this.getClass(), error);
                    }

                    try {
                        PushPublishLogger.log(this.getClass(), "Status Update: Bundle push starting");

                        boolean amIPublishing = PublisherConfig.Operation.PUBLISH.equals(config.getOperation());

                        //Getting the host name, then the languages under the bundle.
                        File bundleRoot = BundlerUtil.getBundleRoot(this.config);

                        //For logging purpose
                        ThreadContext.put(BUNDLE_ID, BUNDLE_ID + "=" + bundleRoot.getName());

                        PushPublishLogger.log(this.getClass(), "Status Update: Pushing bundle");
                        currentStatusHistory.setPublishStart(new Date());
                        File liveFolder = new File(bundleRoot.getAbsolutePath() + LIVE_FOLDER);

                        if ( !liveFolder.exists() ) {
                            Logger.warn(this.getClass(), "Bundle is EMPTY");
                            PushPublishLogger.log(this.getClass(), "Bundle is EMPTY");
                        } else {
                            //For each host.
                            for (File hostFolder : liveFolder.listFiles(FileUtil.getOnlyFolderFileFilter())) {
                                Host host = getHostFromFilePath(hostFolder);
                                config.put(CURRENT_HOST, host);

                                final TreeSet<LanguageFolder> languagesFolders = getLanguageFolders(
                                        hostFolder);
                                Logger.info(this.getClass(), String.format("Pushed languages: %s", languagesFolders.stream().map(lang -> lang.getLanguage()).collect(Collectors.toList())));
                                //For each language.
                                for (LanguageFolder languageFolder : languagesFolders) {
                                    //For each file under i.e. /live/demo.dotcms.com/1/
                                    Language language = languageFolder.getLanguage();
                                    config.put(CURRENT_LANGUAGE, Long.toString(language.getId()));

                                    final String bucketName = getBucketName(this.config);
                                    final String bucketPrefix = getBucketPrefix(bucketPrefixProp, this.config);

                                    /* Creates a bucket only if it does not exist.
                                       In order to avoid aws stale reads, we verify against out set of buckets names
                                       if it has not been created yet. In case the bucket name does not exist in the set,
                                       a new bucket is created and its name is saved in the set*/
                                    if (!((Set<String>)config.get(CREATED_BUCKETS)).contains(bucketName)){
                                        endPointPublisher.createBucket(bucketName, bucketRegion);
                                        ((Set<String>)config.get(CREATED_BUCKETS)).add(bucketName);
                                    }

                                    Collection<File> listFiles = amIPublishing ? Arrays.asList(languageFolder.getLanguageFolder().listFiles())
                                        : FileUtils.listFiles(languageFolder.getLanguageFolder(), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);

                                    for (File file : listFiles) {
                                        final String filePath = getStaticFilePath(bundleRoot, file);

                                        try {
                                            if (amIPublishing) {
                                                endPointPublisher.pushBundleToEndpoint(bucketName, bucketRegion, bucketPrefix, filePath, file);
                                            } else {
                                                endPointPublisher.deleteFilesFromEndpoint(bucketName, bucketPrefix, filePath);
                                            }
                                        } catch(DotPublishingException e) {
                                            String error = updateStatusFailedToSend(currentStatusHistory, environment, endpoint, detail);
                                            failedEnvironment |= true;
                                            Logger.error(this.getClass(), error, e);
                                        }
                                    }
                                    publishVanityAliasesForCanonicalFilesIfEnabled(endPointPublisher, endpoint,
                                            host, language, bucketName, bucketRegion, bucketPrefix, bundleRoot,
                                            languageFolder.getLanguageFolder());
                                }
                            }
                        }
                        publishVanityAliasesForBundleAssetsIfEnabled(endPointPublisher, bucketRegion,
                                bucketPrefixProp, endpoint);
                        unpublishVanityAliasesForBundleAssetsIfEnabled(new S3VanityAliasCleanupContext(
                                endpoint.getId(), endPointPublisher));


                    } catch(Exception e) {
                        // if the bundle can't be sent after the total num of tries, delete the pushed assets for this bundle
                        if(currentStatusHistory.getNumTries() >= PublisherQueueJob.MAX_NUM_TRIES) {
                            this.pushedAssetsAPI.deletePushedAssets(config.getId(), environment.getId());
                        }
                        detail.setStatus(PublishAuditStatus.Status.FAILED_TO_PUBLISH.getCode());
                        String error = 	"An error occurred for the endpoint "+ endpoint.getId() + " with address "+ endpoint.getAddress() + ".  Error: " + e.getMessage();
                        detail.setInfo(error);
                        failedEnvironment |= true;

                        Logger.error(this.getClass(), error, e);
                        PushPublishLogger.log(this.getClass(), "Status Update: Failed to publish bundle");
                    } finally {
                        endPointPublisher.shutdownTransferManager();
                        ThreadContext.remove(BUNDLE_ID);
                        ThreadContext.remove(ENDPOINT_NAME);
                    }

                    if (detail.getStatus()==0){
                        detail.setStatus(PublishAuditStatus.Status.SUCCESS.getCode());
                        detail.setInfo("Endpoint " + endpoint.getId() + " published successfully");
                        currentStatusHistory.setPublishEnd(new Date());
                    }

                    if (isHistoryEmpty || failedEnvironment) {
                        currentStatusHistory.addOrUpdateEndpoint(environment.getId(), endpoint.getId(), detail);
                    }
                }
                if(failedEnvironment) {
                    failedEnvironmentCounter++;
                }
            }

            if(failedEnvironmentCounter==0) {
                //Updating Audit table
                PushPublishLogger.log(this.getClass(), "Status Update: Bundle sent");
                this.publishAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.BUNDLE_SENT_SUCCESSFULLY, currentStatusHistory);
            } else {
                if(failedEnvironmentCounter == environments.size()) {
                    this.publishAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.FAILED_TO_SEND_TO_ALL_GROUPS, currentStatusHistory);
                } else {
                    this.publishAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.FAILED_TO_SEND_TO_SOME_GROUPS, currentStatusHistory);
                }
            }

        } catch (Exception e) {
            try {
                PushPublishLogger.log(this.getClass(), "Status Update: Failed to publish");
                this.publishAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.FAILED_TO_PUBLISH, currentStatusHistory);
            } catch (DotPublisherException e1) {
                throw new DotPublishingException(e.getMessage(),e);
            }

            Logger.error(this.getClass(), e.getMessage(), e);
            throw new DotPublishingException(e.getMessage(),e);
        }

        return config;
    } // process.

    @NotNull
    private String updateStatusFailedToSend(PublishAuditHistory currentStatusHistory, Environment environment, PublishingEndPoint endpoint, EndpointDetail detail) throws DotDataException {
        // if the bundle can't be sent after the total num of tries, delete the pushed assets for this bundle
        if(currentStatusHistory.getNumTries() >= PublisherQueueJob.MAX_NUM_TRIES) {
            this.pushedAssetsAPI.deletePushedAssets(config.getId(), environment.getId());
        }
        detail.setStatus(PublishAuditStatus.Status.FAILED_TO_SENT.getCode());
        String error = 	"An error occurred for the endpoint " + endpoint.getId() + " Error: Can't connect to End Point";
        detail.setInfo(error);
        return error;
    }

    /**
	 * Creates and returns an AWSS3EndPointPublisher.
	 * Uses the default AWS credentials provider chain if the tokenProp and/or secretProp are not set.
	 * Uses the provided tokenProp and secretProp if they are both set.
	 *
	 * @param tokenProp - the AWS credentials token key
	 * @param secretProp - the AWS credentials secret key
     * @param endPoint - S3 server to connect
     *
	 * @return - a ready-to-use endpoint publisher
	 */
	private AWSS3EndPointPublisher getAWSS3EndPointPublisher(final String tokenProp,
            final String secretProp, final String endPoint, final String region) {
		AWSS3EndPointPublisher endPointPublisher;
		if (!UtilMethods.isSet(tokenProp) || !UtilMethods.isSet(secretProp)) {
			DefaultAWSCredentialsProviderChain creds = new DefaultAWSCredentialsProviderChain();
			endPointPublisher = new AWSS3EndPointPublisher(creds);
		} else {
			AWSS3Configuration awss3Configuration =
					new AWSS3Configuration.Builder()
                            .accessKey(tokenProp)
                            .secretKey(secretProp)
                            .endPoint(endPoint)
                            .region(region)
                            .build();

			endPointPublisher = new AWSS3EndPointPublisher(awss3Configuration);
		}
		return endPointPublisher;
	}

	/**
	 * 
	 * @param prefix
	 * @param config
	 * @return
	 */
    private String getBucketPrefix(final String prefix, final PublisherConfig config) {
        final Map<String, Object> contextMap = this.getContextMap (prefix, config);
        final String bucketPrefix = StringUtils.interpolate(prefix, contextMap);
        return (null != bucketPrefix && bucketPrefix.trim().length() > 0)?
                bucketPrefix: null;
    } //getBucketPrefix

    /**
     * Figure out the bucket name.
     * @param config {@link PublisherConfig}
     * @return List of String
     */
    protected String getBucketName(final PublisherConfig config) {

        final String bucketID   = (String) config.get(DOTCMS_PUSH_AWS_S3_BUCKET_ID);
        final Map<String, Object> contextMap = this.getContextMap(bucketID, config);
        final String bucketName = StringUtils.interpolate(bucketID, contextMap);

        return (null != bucketName && bucketName.trim().length() > 0) ?
                this.normalizeBucketName(bucketName) : DEFAULT_BUCKET_NAME;

    } // getBucketName.

    /**
     * Normalize the bucket name.
     * @param bucketName String
     * @return String
     */
    protected String normalizeBucketName (final String bucketName) {
        //Check for blank spaces and other special characters.
        final String regexValidation =
            Config.getStringProperty("STATIC_PUSH_BUCKET_NAME_REGEX",
                "[,!:;&?$*\\/\\\\\\[\\]=\\|#_@\\(\\)<>\\s]+");
        String normalizedBucketName = bucketName.replaceAll(regexValidation, "-");
        return normalizedBucketName.toLowerCase();
    } //normalizeBucketName

    /**
     * Publishes vanity clones for Vanity URL contentlets included in the bundle.
     *
     * @param endPointPublisher S3 publisher
     * @param bucketRegion bucket region
     * @param bucketPrefixProp bucket prefix property before interpolation
     * @param endpoint current endpoint
     * @throws DotPublishingException when vanity clone publishing fails
     */
    private void publishVanityAliasesForBundleAssetsIfEnabled(final AWSS3EndPointPublisher endPointPublisher,
                                                              final String bucketRegion,
                                                              final String bucketPrefixProp,
                                                              final PublishingEndPoint endpoint)
            throws DotPublishingException {
        if (!isS3VanityAliasEnabled() || !PublisherConfig.Operation.PUBLISH.equals(config.getOperation())) {
            return;
        }

        final List<PublishQueueElement> assets = config.getAssets();
        if (!UtilMethods.isSet(assets)) {
            return;
        }

        for (final PublishQueueElement asset : assets) {
            publishVanityAliasForQueueElement(endPointPublisher, bucketRegion, bucketPrefixProp, endpoint, asset);
        }
    }

    /**
     * Refreshes existing vanity aliases when their canonical static files are published.
     *
     * @param endPointPublisher S3 publisher
     * @param endpoint current endpoint
     * @param host current host
     * @param language current language
     * @param bucketName bucket name
     * @param bucketRegion bucket region
     * @param bucketPrefix bucket prefix
     * @param bundleRoot bundle root
     * @param languageFolder folder containing the static files for the current language
     * @throws DotPublishingException when alias refresh fails
     */
    private void publishVanityAliasesForCanonicalFilesIfEnabled(final AWSS3EndPointPublisher endPointPublisher,
                                                                final PublishingEndPoint endpoint,
                                                                final Host host,
                                                                final Language language,
                                                                final String bucketName,
                                                                final String bucketRegion,
                                                                final String bucketPrefix,
                                                                final File bundleRoot,
                                                                final File languageFolder)
            throws DotPublishingException {
        if (!isS3VanityAliasEnabled() || !PublisherConfig.Operation.PUBLISH.equals(config.getOperation())) {
            return;
        }

        for (final File file : FileUtils.listFiles(languageFolder, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)) {
            if (!endPointPublisher.acceptsFile(file)) {
                continue;
            }

            final String canonicalPath = getStaticFilePath(bundleRoot, file);
            try {
                vanityAliasService.publishAliases(new S3VanityAliasContext(
                        new S3VanityAliasLookup(endpoint.getId(), host.getIdentifier(), language.getId(),
                                canonicalPath),
                        bucketName, bucketRegion, bucketPrefix, host, language, file, endPointPublisher));
            } catch (final DotDataException e) {
                throw new DotPublishingException(e.getMessage(), e);
            }
        }
    }

    /**
     * Handles one bundle element as a possible published Vanity URL.
     *
     * @param endPointPublisher S3 publisher
     * @param bucketRegion bucket region
     * @param bucketPrefixProp bucket prefix property before interpolation
     * @param endpoint current endpoint
     * @param asset element from the publishing queue
     * @throws DotPublishingException when vanity clone publishing fails
     */
    private void publishVanityAliasForQueueElement(final AWSS3EndPointPublisher endPointPublisher,
                                                   final String bucketRegion,
                                                   final String bucketPrefixProp,
                                                   final PublishingEndPoint endpoint,
                                                   final PublishQueueElement asset)
            throws DotPublishingException {
        if (!isPublishedContentletAsset(asset)) {
            return;
        }

        try {
            publishLiveVanityContentlet(endPointPublisher, bucketRegion, bucketPrefixProp, endpoint, asset);
        } catch (final DotDataException | DotSecurityException e) {
            throw new DotPublishingException(e.getMessage(), e);
        }
    }

    /**
     * Publishes one live Vanity URL contentlet as a static S3 clone.
     *
     * @param endPointPublisher S3 publisher
     * @param bucketRegion bucket region
     * @param bucketPrefixProp bucket prefix property before interpolation
     * @param endpoint current endpoint
     * @param asset element from the publishing queue
     * @throws DotDataException when content or mapping reads fail
     * @throws DotSecurityException when system user cannot read the contentlet
     * @throws DotPublishingException when S3 publishing fails
     */
    private void publishLiveVanityContentlet(final AWSS3EndPointPublisher endPointPublisher,
                                             final String bucketRegion,
                                             final String bucketPrefixProp,
                                             final PublishingEndPoint endpoint,
                                             final PublishQueueElement asset)
            throws DotDataException, DotSecurityException, DotPublishingException {
        final Optional<Contentlet> vanityContentlet = findLiveVanityContentlet(asset);
        if (vanityContentlet.isEmpty()) {
            return;
        }

        final Host host = hostAPI.find(vanityContentlet.get().getHost(), APILocator.getUserAPI().getSystemUser(), false);
        final Language language = languageAPI.getLanguage(vanityContentlet.get().getLanguageId());
        if (host == null || language == null) {
            Logger.warn(this, "Skipping Vanity URL because its site or language cannot be resolved: "
                    + vanityContentlet.get().getIdentifier());
            return;
        }

        config.put(CURRENT_HOST, host);
        config.put(CURRENT_LANGUAGE, Long.toString(language.getId()));
        final String bucketName = getBucketName(config);
        final String bucketPrefix = getBucketPrefix(bucketPrefixProp, config);
        ensureBucketExists(endPointPublisher, bucketName, bucketRegion);
        vanityAliasService.publishAliasForVanityUrl(new S3VanityAliasPublishContext(endpoint.getId(),
                bucketName, bucketRegion, bucketPrefix, host, language, endPointPublisher), vanityContentlet.get());
    }

    /**
     * Finds the live Vanity URL contentlet represented by a publishing queue element.
     *
     * @param asset element from the publishing queue
     * @return live Vanity URL contentlet when the asset represents one
     * @throws DotDataException when the contentlet cannot be read
     * @throws DotSecurityException when the system user cannot read the contentlet
     */
    private Optional<Contentlet> findLiveVanityContentlet(final PublishQueueElement asset)
            throws DotDataException, DotSecurityException {
        final Optional<Contentlet> languageSpecificContentlet = findLiveVanityContentletForLanguage(asset);
        if (languageSpecificContentlet.isPresent()) {
            return languageSpecificContentlet;
        }

        final List<Contentlet> contentlets = contentletAPI.search("+identifier:" + asset.getAsset() + " +live:true",
                0, 0, null, APILocator.getUserAPI().getSystemUser(), false);
        return contentlets.stream().filter(Contentlet::isVanityUrl).findFirst();
    }

    /**
     * Finds the live Vanity URL contentlet for the queue language when present.
     *
     * @param asset element from the publishing queue
     * @return language-specific live Vanity URL when the queue carries a language
     * @throws DotDataException when the contentlet cannot be read
     * @throws DotSecurityException when the system user cannot read the contentlet
     */
    private Optional<Contentlet> findLiveVanityContentletForLanguage(final PublishQueueElement asset)
            throws DotDataException, DotSecurityException {
        if (asset.getLanguageId() == null || asset.getLanguageId() <= 0) {
            return Optional.empty();
        }

        try {
            final Contentlet contentlet = contentletAPI.findContentletByIdentifier(asset.getAsset(), true,
                    asset.getLanguageId(), APILocator.getUserAPI().getSystemUser(), false);
            return contentlet != null && contentlet.isVanityUrl() ? Optional.of(contentlet) : Optional.empty();
        } catch (final DotContentletStateException e) {
            Logger.warn(this, "Unable to find live Vanity URL for language: " + asset.getLanguageId());
            return Optional.empty();
        }
    }

    /**
     * Creates the bucket once per endpoint execution when needed.
     *
     * @param endPointPublisher S3 publisher
     * @param bucketName bucket name
     * @param bucketRegion bucket region
     * @throws DotPublishingException when bucket creation fails
     */
    private void ensureBucketExists(final AWSS3EndPointPublisher endPointPublisher,
                                    final String bucketName,
                                    final String bucketRegion) throws DotPublishingException {
        if (!((Set<String>) config.get(CREATED_BUCKETS)).contains(bucketName)) {
            endPointPublisher.createBucket(bucketName, bucketRegion);
            ((Set<String>) config.get(CREATED_BUCKETS)).add(bucketName);
        }
    }

    /**
     * Removes vanity clones when the bundle directly contains a deleted or
     * unpublished Vanity URL.
     *
     * @param context minimal cleanup context for the S3 endpoint
     * @throws DotPublishingException when vanity clone removal fails
     */
    private void unpublishVanityAliasesForBundleAssetsIfEnabled(final S3VanityAliasCleanupContext context)
            throws DotPublishingException {
        if (!isS3VanityAliasEnabled() || PublisherConfig.Operation.PUBLISH.equals(config.getOperation())) {
            return;
        }

        final List<PublishQueueElement> assets = config.getAssets();
        if (!UtilMethods.isSet(assets)) {
            return;
        }

        for (final PublishQueueElement asset : assets) {
            unpublishVanityAliasForQueueElement(context, asset);
        }
    }

    /**
     * Handles one bundle element as a possible removed Vanity URL.
     *
     * @param context minimal cleanup context for the S3 endpoint
     * @param asset element from the publishing queue
     * @throws DotPublishingException when vanity clone removal fails
     */
    private void unpublishVanityAliasForQueueElement(final S3VanityAliasCleanupContext context,
                                                    final PublishQueueElement asset)
            throws DotPublishingException {
        if (!isDeletedContentletAsset(asset)) {
            return;
        }

        try {
            final long languageId = asset.getLanguageId() == null ? -1L : asset.getLanguageId().longValue();
            vanityAliasService.unpublishAliasesByVanityUrl(context, languageId, asset.getAsset());
        } catch (final DotDataException e) {
            throw new DotPublishingException(e.getMessage(), e);
        }
    }

    /**
     * Checks whether the asset represents a deleted contentlet.
     *
     * @param asset publishing queue element
     * @return true when the asset can be a removed Vanity URL
     */
    private boolean isDeletedContentletAsset(final PublishQueueElement asset) {
        return asset != null
                && asset.getOperation() != null
                && asset.getOperation().longValue() == com.dotcms.publisher.business.PublisherAPI.DELETE_ELEMENT
                && PusheableAsset.CONTENTLET.getType().equals(asset.getType());
    }

    /**
     * Checks whether the asset represents a published contentlet.
     *
     * @param asset publishing queue element
     * @return true when the asset can be a published Vanity URL
     */
    private boolean isPublishedContentletAsset(final PublishQueueElement asset) {
        return asset != null
                && asset.getOperation() != null
                && asset.getOperation().longValue() == com.dotcms.publisher.business.PublisherAPI.ADD_OR_UPDATE_ELEMENT
                && PusheableAsset.CONTENTLET.getType().equals(asset.getType());
    }

    /**
     * Converts a bundle file into the static path used as canonical key.
     *
     * @param bundleRoot bundle root
     * @param file file or directory to convert
     * @return static path relative to host and language
     */
    private String getStaticFilePath(final File bundleRoot, final File file) {
        String filePath = file.getAbsolutePath().replace(bundleRoot.getAbsolutePath() + LIVE_FOLDER, "");

        //Always remove the /hostName/ i.e. /demo.dotcms.com/
        filePath = filePath.substring(filePath.indexOf(File.separator, filePath.indexOf(File.separator) + 1));

        //Always remove the /languageId/ i.e. /1/
        return filePath.substring(filePath.indexOf(File.separator, filePath.indexOf(File.separator) + 1));
    }

    /**
     * Checks whether S3 vanity alias support is enabled.
     *
     * @return true when vanity alias support is active
     */
    private boolean isS3VanityAliasEnabled() {
        return Config.getBooleanProperty("STATIC_PUSH_S3_VANITY_ALIAS_ENABLED", false);
    }



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
        return list;
    } // getBundlers.

    @Override
    public Set<String> getProtocols(){
        Set<String> protocols = new HashSet<>();
        protocols.add(PROTOCOL_AWS_S3);
        return  protocols;
    }

} // E:O:F:AWSS3Publisher.
