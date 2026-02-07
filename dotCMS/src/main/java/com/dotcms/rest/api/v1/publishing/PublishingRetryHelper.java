package com.dotcms.rest.api.v1.publishing;

import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher;
import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.DeliveryStrategy;
import com.dotcms.publishing.manifest.CSVManifestBuilder;
import com.dotcms.publishing.manifest.CSVManifestReader;
import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
import com.dotcms.publishing.manifest.ManifestReaderFactory;
import com.dotcms.publishing.manifest.ManifestReason;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Helper class for retrying failed or successful publishing bundles.
 * Extracts and modernizes retry logic from RemotePublishAjaxAction.retry().
 *
 * <p>This helper handles both Push Publishing (sends to remote dotCMS servers)
 * and Static Publishing (publishes to AWS S3 or file systems).</p>
 *
 * <p>This helper throws exceptions on failure - the caller (Resource) is responsible
 * for catching exceptions and building appropriate response views.</p>
 *
 * @author hassandotcms
 * @since Feb 2026
 */
public class PublishingRetryHelper {

    private final PublisherAPI publisherAPI;
    private final PublishAuditAPI publishAuditAPI;
    private final BundleAPI bundleAPI;
    private final EnvironmentAPI environmentAPI;
    private final PublishingEndPointAPI publishingEndPointAPI;

    /**
     * Default constructor using APILocator for dependencies.
     */
    public PublishingRetryHelper() {
        this(PublisherAPI.getInstance(),
             PublishAuditAPI.getInstance(),
             APILocator.getBundleAPI(),
             APILocator.getEnvironmentAPI(),
             APILocator.getPublisherEndPointAPI());
    }

    /**
     * Constructor for testing with dependency injection.
     */
    @VisibleForTesting
    public PublishingRetryHelper(final PublisherAPI publisherAPI,
                                  final PublishAuditAPI publishAuditAPI,
                                  final BundleAPI bundleAPI,
                                  final EnvironmentAPI environmentAPI,
                                  final PublishingEndPointAPI publishingEndPointAPI) {
        this.publisherAPI = publisherAPI;
        this.publishAuditAPI = publishAuditAPI;
        this.bundleAPI = bundleAPI;
        this.environmentAPI = environmentAPI;
        this.publishingEndPointAPI = publishingEndPointAPI;
    }

    /**
     * Retry a single bundle. Throws exceptions on failure.
     *
     * @param bundleId         The bundle identifier to retry
     * @param forcePush        Whether to force push (override existing content)
     * @param deliveryStrategy Which endpoints to retry (ALL or FAILED only)
     * @param user             The user performing the retry
     * @param request          The HTTP request (used for sendingBundle check)
     * @return RetryResultDTO with success details
     * @throws IllegalArgumentException if bundleId is empty
     * @throws DotPublisherException if bundle not found, not retryable, or already in queue
     * @throws DotDataException if database error occurs
     * @throws DotPublishingException if publishing error occurs
     */
    public RetryResultDTO retryBundle(
            final String bundleId,
            final boolean forcePush,
            final DeliveryStrategy deliveryStrategy,
            final User user,
            final HttpServletRequest request)
            throws DotPublisherException, DotDataException, DotPublishingException {

        // Validate bundle ID is provided
        if (!UtilMethods.isSet(bundleId) || bundleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Bundle ID is required");
        }

        final String trimmedBundleId = bundleId.trim();

        // Get audit status
        final PublishAuditStatus status = publishAuditAPI.getPublishAuditStatus(trimmedBundleId);
        if (status == null) {
            throw new DotPublisherException("Bundle not found in audit history: " + trimmedBundleId);
        }

        // Validate bundle is retryable
        if (!BundlerUtil.isRetryable(status)) {
            throw new DotPublisherException(
                    String.format("Cannot retry bundles with status %s - only failed or successful bundles can be retried",
                            status.getStatus().name()));
        }

        // Check if bundle is already in queue
        final List<PublishQueueElement> foundBundles =
                publisherAPI.getQueueElementsByBundleId(trimmedBundleId);
        if (foundBundles != null && !foundBundles.isEmpty()) {
            throw new DotPublisherException("Bundle already in queue - cannot retry while publishing");
        }

        // Get audit history for updating numTries
        final String pojoString = status.getStatusPojo().getSerialized();
        final PublishAuditHistory auditHistory = PublishAuditHistory.getObjectFromString(pojoString);

        // Check if this is a static bundle
        final PublisherConfig basicConfig = new PublisherConfig();
        basicConfig.setId(trimmedBundleId);
        final File bundleRoot = BundlerUtil.getBundleRoot(basicConfig.getName(), false);
        final File bundleStaticFile = new File(bundleRoot.getAbsolutePath() + PublisherConfig.STATIC_SUFFIX);

        if (bundleStaticFile.exists()) {
            // Handle static publishing (AWS S3 or static file system)
            return retryStaticBundle(trimmedBundleId, auditHistory, status, deliveryStrategy);
        } else {
            // Handle push publishing
            return retryPushBundle(trimmedBundleId, forcePush, deliveryStrategy,
                    auditHistory, status, user, request, basicConfig);
        }
    }

    /**
     * Retry a static publishing bundle (AWS S3 or static file system).
     */
    private RetryResultDTO retryStaticBundle(
            final String bundleId,
            final PublishAuditHistory auditHistory,
            final PublishAuditStatus status,
            final DeliveryStrategy deliveryStrategy)
            throws DotPublisherException, DotDataException, DotPublishingException {

        final PublisherConfig basicConfig = new PublisherConfig();
        basicConfig.setId(bundleId);
        final File bundleRoot = BundlerUtil.getBundleRoot(basicConfig.getName(), false);
        final File bundleStaticFile = new File(bundleRoot.getAbsolutePath() + PublisherConfig.STATIC_SUFFIX);

        // Read the bundle configuration
        final File readBundleFile = new File(bundleStaticFile.getAbsolutePath() + File.separator + "bundle.xml");
        final PublisherConfig readConfig = (PublisherConfig) BundlerUtil.xmlToObject(readBundleFile);

        final PublisherConfig configStatic = new PublisherConfig();
        configStatic.setId(bundleId);
        configStatic.setOperation(readConfig.getOperation());

        // Reset number of tries
        auditHistory.setNumTries(0);
        publishAuditAPI.updatePublishAuditStatus(configStatic.getId(), status.getStatus(), auditHistory, true);

        // Get environments
        final List<Environment> environments = environmentAPI.findEnvironmentsByBundleId(bundleId);

        // Process each environment
        for (final Environment environment : environments) {
            final List<PublishingEndPoint> endPoints =
                    publishingEndPointAPI.findSendingEndPointsByEnvironment(environment.getId());

            if (endPoints.isEmpty()) {
                continue;
            }

            final PublishingEndPoint targetEndpoint = endPoints.get(0);
            final Publisher staticPublisher;

            // Choose appropriate publisher based on protocol
            if (AWSS3Publisher.PROTOCOL_AWS_S3.equalsIgnoreCase(targetEndpoint.getProtocol())) {
                staticPublisher = new AWSS3Publisher();
            } else {
                staticPublisher = new StaticPublisher();
            }

            // Initialize and process
            staticPublisher.init(configStatic);
            staticPublisher.process(null);
        }

        Logger.info(this, "Successfully retried static bundle: " + bundleId);

        return new RetryResultDTO(
                bundleId + PublisherConfig.STATIC_SUFFIX,
                false, // forcePush not applicable for static
                readConfig.getOperation() != null ? readConfig.getOperation().name() : null,
                deliveryStrategy.name(),
                0 // assetCount not easily retrievable for static
        );
    }

    /**
     * Retry a push publishing bundle.
     */
    private RetryResultDTO retryPushBundle(
            final String bundleId,
            final boolean forcePush,
            final DeliveryStrategy deliveryStrategy,
            final PublishAuditHistory auditHistory,
            final PublishAuditStatus status,
            final User user,
            final HttpServletRequest request,
            final PublisherConfig basicConfig)
            throws DotPublisherException, DotDataException {

        // Verify bundle tar.gz file exists
        final File bundleFile = new File(ConfigUtils.getBundlePath() + File.separator + basicConfig.getId() + ".tar.gz");
        if (!bundleFile.exists()) {
            Logger.warn(this, "No Push Publish Bundle with id: " + bundleId + " found.");
            throw new DotPublisherException("Bundle file not found: " + bundleId);
        }

        if (!BundlerUtil.bundleExists(basicConfig)) {
            Logger.error(this, String.format("Bundle's tar.gzip file for %s not exists", bundleId));
            throw new DotPublisherException("Bundle descriptor not found: " + bundleId);
        }

        // Read the manifest to get operation and assets
        final CSVManifestReader csvManifestReader = ManifestReaderFactory.INSTANCE
                .createCSVManifestReader(basicConfig.getId());

        final String operationStr = csvManifestReader
                .getMetadata(CSVManifestBuilder.OPERATION_METADATA_NAME);

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setOperation(PushPublisherConfig.Operation.valueOf(operationStr));

        // Check if this is a sending bundle (not received)
        if (request != null && !isSendingBundle(request, bundleId)) {
            throw new DotPublisherException(
                    "Cannot retry received bundles - only bundles sent from this server can be retried");
        }

        // Get the bundle
        final Bundle bundle = bundleAPI.getBundleById(bundleId);
        if (bundle == null) {
            Logger.error(this, "No Bundle with id: " + bundleId + " found.");
            throw new DotPublisherException("Bundle not found: " + bundleId);
        }

        // Determine force push value
        final boolean effectiveForcePush;
        if (status.getStatus().equals(Status.SUCCESS) ||
            status.getStatus().equals(Status.SUCCESS_WITH_WARNINGS)) {
            // Always force push for successful bundles
            effectiveForcePush = true;
        } else {
            effectiveForcePush = forcePush;
        }

        // Update the bundle
        bundle.setForcePush(effectiveForcePush);
        bundleAPI.updateBundle(bundle);

        // Reset number of tries
        auditHistory.setNumTries(0);
        publishAuditAPI.updatePublishAuditStatus(bundle.getId(), status.getStatus(), auditHistory, true);

        // Get identifiers from manifest
        final HashSet<String> identifiers = new HashSet<>();
        final Collection<ManifestInfo> assets = csvManifestReader.getAssets(ManifestReason.INCLUDE_BY_USER);

        if (assets != null && !assets.isEmpty()) {
            for (final ManifestInfo asset : assets) {
                identifiers.add(asset.id());
            }
        }

        // Add to appropriate queue based on operation
        if (config.getOperation().equals(PushPublisherConfig.Operation.PUBLISH)) {
            publisherAPI.addContentsToPublish(
                    new ArrayList<>(identifiers), bundleId, new Date(), user, deliveryStrategy);
        } else {
            publisherAPI.addContentsToUnpublish(
                    new ArrayList<>(identifiers), bundleId, new Date(), user, deliveryStrategy);
        }

        Logger.info(this, String.format("Successfully re-queued bundle %s with %d assets for %s",
                bundleId, identifiers.size(), config.getOperation().name()));

        return new RetryResultDTO(
                bundleId,
                effectiveForcePush,
                config.getOperation().name(),
                deliveryStrategy.name(),
                identifiers.size()
        );
    }

    /**
     * Checks if the bundle is being sent from this server (not received).
     */
    private boolean isSendingBundle(final HttpServletRequest request, final String bundleId) {
        try {
            String remoteIP = request.getRemoteHost();
            final int port = request.getLocalPort();
            if (!UtilMethods.isSet(remoteIP)) {
                remoteIP = request.getRemoteAddr();
            }

            final List<Environment> environments = environmentAPI.findEnvironmentsByBundleId(bundleId);

            for (final Environment environment : environments) {
                final List<PublishingEndPoint> endPoints =
                        publishingEndPointAPI.findSendingEndPointsByEnvironment(environment.getId());

                for (final PublishingEndPoint endPoint : endPoints) {
                    final String endPointAddress = endPoint.getAddress();
                    final String endPointPort = endPoint.getPort();

                    if (endPointAddress.equals(remoteIP) &&
                        endPointPort.equals(String.valueOf(port))) {
                        return false;
                    }
                }
            }

            return true;
        } catch (DotDataException e) {
            Logger.error(this, "Error checking if bundle is sending: " + e.getMessage(), e);
            return true;
        }
    }
}
