package com.dotcms.rest.api.v1.publishing;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.EndpointDetail;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublishAuditUtil;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;

import java.time.Instant;
import java.util.*;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper class for building publishing job views from audit status and bundle data.
 * This class transforms raw PublishAuditStatus objects into PublishingJobView DTOs
 * with enriched metadata including bundle names, filter titles, and asset previews.
 *
 * @author hassandotcms
 * @since Jan 2026
 */
public class PublishingJobsHelper {

    /**
     * Maximum number of assets to include in the preview list.
     */
    public static final int ASSET_PREVIEW_LIMIT = 3;

    /**
     * Set of status codes that indicate a failure state.
     * Stack traces are only included for these statuses.
     */
    private static final Set<Integer> FAILURE_STATUS_CODES = Set.of(
            Status.FAILED_TO_SENT.getCode(),
            Status.FAILED_TO_PUBLISH.getCode(),
            Status.FAILED_TO_SEND_TO_ALL_GROUPS.getCode(),
            Status.FAILED_TO_SEND_TO_SOME_GROUPS.getCode(),
            Status.FAILED_TO_BUNDLE.getCode(),
            Status.FAILED_INTEGRITY_CHECK.getCode()
    );

    /**
     * Set of statuses that indicate a bundle is actively being processed.
     * Bundles with these statuses cannot be deleted to prevent data corruption.
     */
    public static final Set<Status> IN_PROGRESS_STATUSES = Set.of(
            Status.BUNDLING,
            Status.SENDING_TO_ENDPOINTS,
            Status.PUBLISHING_BUNDLE
    );

    private final BundleAPI bundleAPI;
    private final PublisherAPI publisherAPI;
    private final PublishAuditUtil publishAuditUtil;
    private final EnvironmentAPI environmentAPI;
    private final PublishingEndPointAPI publishingEndPointAPI;

    /**
     * Default constructor using APILocator for dependencies.
     */
    public PublishingJobsHelper() {
        this(APILocator.getBundleAPI(),
             APILocator.getPublisherAPI(),
             PublishAuditUtil.getInstance(),
             APILocator.getEnvironmentAPI(),
             APILocator.getPublisherEndPointAPI());
    }

    /**
     * Constructor for testing with dependency injection.
     *
     * @param bundleAPI              Bundle API for retrieving bundle metadata
     * @param publisherAPI           Publisher API for filter lookup
     * @param publishAuditUtil       Utility for asset title resolution
     * @param environmentAPI         Environment API for environment lookup
     * @param publishingEndPointAPI  Publishing endpoint API for endpoint lookup
     */
    @VisibleForTesting
    public PublishingJobsHelper(final BundleAPI bundleAPI,
                                final PublisherAPI publisherAPI,
                                final PublishAuditUtil publishAuditUtil,
                                final EnvironmentAPI environmentAPI,
                                final PublishingEndPointAPI publishingEndPointAPI) {
        this.bundleAPI = bundleAPI;
        this.publisherAPI = publisherAPI;
        this.publishAuditUtil = publishAuditUtil;
        this.environmentAPI = environmentAPI;
        this.publishingEndPointAPI = publishingEndPointAPI;
    }

    /**
     * Transforms a PublishAuditStatus into a PublishingJobView with enriched data.
     * Fetches bundle metadata, resolves filter names, and builds asset previews.
     *
     * @param auditStatus The raw audit status to transform
     * @return A fully populated PublishingJobView
     */
    public PublishingJobView toPublishingJobView(final PublishAuditStatus auditStatus) {
        final String bundleId = auditStatus.getBundleId();
        final Bundle bundle = getBundleSafely(bundleId);
        final PublishAuditHistory history = auditStatus.getStatusPojo();

        return PublishingJobView.builder()
                .bundleId(bundleId)
                .bundleName(bundle != null ? bundle.getName() : null)
                .status(auditStatus.getStatus())
                .filterName(resolveFilterName(bundle))
                .assetCount(auditStatus.getTotalNumberOfAssets())
                .assetPreview(buildAssetPreviews(history))
                .environmentCount(countEnvironments(history))
                .createDate(toInstant(auditStatus.getCreateDate()))
                .statusUpdated(toInstant(auditStatus.getStatusUpdated()))
                .numTries(history != null ? history.getNumTries() : 0)
                .build();
    }

    /**
     * Parses a comma-separated status string into a list of Status enum values.
     * Invalid status names are logged and skipped.
     *
     * @param statusParam Comma-separated status values (e.g., "SUCCESS,FAILED_TO_PUBLISH")
     * @return List of valid Status enum values, or empty list if input is null/empty
     */
    public List<Status> parseStatuses(final String statusParam) {
        if (!UtilMethods.isSet(statusParam)) {
            return List.of();
        }
        return Arrays.stream(statusParam.split(","))
                .map(String::trim)
                .filter(UtilMethods::isSet)
                .map(this::parseStatusSafely)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    /**
     * Validates that all provided status values are valid.
     *
     * @param statusParam Comma-separated status values
     * @return List of invalid status names, or empty list if all are valid
     */
    public List<String> getInvalidStatuses(final String statusParam) {
        if (!UtilMethods.isSet(statusParam)) {
            return List.of();
        }
        return Arrays.stream(statusParam.split(","))
                .map(String::trim)
                .filter(UtilMethods::isSet)
                .filter(name -> parseStatusSafely(name).isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Gets the list of all valid status names.
     *
     * @return List of valid status names
     */
    public List<String> getValidStatusNames() {
        return Arrays.stream(Status.values())
                .map(Status::name)
                .collect(Collectors.toList());
    }

    /**
     * Checks if the given status indicates the bundle is actively being processed.
     * Bundles with in-progress status cannot be deleted to prevent data corruption.
     *
     * @param status The status to check
     * @return true if the bundle is in progress and cannot be deleted
     */
    public boolean isInProgressStatus(final Status status) {
        return status != null && IN_PROGRESS_STATUSES.contains(status);
    }

    /**
     * Safely retrieves a bundle by ID, returning null if not found or on error.
     */
    private Bundle getBundleSafely(final String bundleId) {
        try {
            return bundleAPI.getBundleById(bundleId);
        } catch (Exception e) {
            Logger.debug(this, "Unable to get bundle: " + bundleId, e);
            return null;
        }
    }

    /**
     * Resolves the human-readable filter name from a bundle's filter key.
     */
    private String resolveFilterName(final Bundle bundle) {
        if (bundle == null || !UtilMethods.isSet(bundle.getFilterKey())) {
            return null;
        }
        try {
            final FilterDescriptor filter = publisherAPI.getFilterDescriptorByKey(bundle.getFilterKey());
            return filter != null ? filter.getTitle() : bundle.getFilterKey();
        } catch (Exception e) {
            Logger.debug(this, "Unable to resolve filter: " + bundle.getFilterKey(), e);
            return bundle.getFilterKey();
        }
    }

    /**
     * Builds asset preview list from the audit history.
     * Returns the first 3 assets with their resolved titles.
     */
    private List<AssetPreviewView> buildAssetPreviews(final PublishAuditHistory history) {
        if (history == null || !UtilMethods.isSet(history.getAssets())) {
            return List.of();
        }

        return history.getAssets().entrySet().stream()
                .limit(ASSET_PREVIEW_LIMIT)
                .map(entry -> AssetPreviewView.builder()
                        .id(entry.getKey())
                        .type(normalizeAssetType(entry.getValue()))
                        .title(resolveAssetTitle(entry.getValue(), entry.getKey()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Resolves the human-readable title for an asset.
     * Falls back to the asset type if title cannot be resolved.
     */
    private String resolveAssetTitle(final String assetType, final String assetId) {
        try {
            final String title = publishAuditUtil.getTitle(assetType, assetId);
            return UtilMethods.isSet(title) ? title : assetType;
        } catch (Exception e) {
            Logger.debug(this, "Unable to resolve title for " + assetType + ":" + assetId, e);
            return assetType;
        }
    }

    /**
     * Normalizes asset type to lowercase for consistency.
     */
    private String normalizeAssetType(final String assetType) {
        return UtilMethods.isSet(assetType) ? assetType.toLowerCase() : "unknown";
    }

    /**
     * Counts the number of environment groups from the audit history.
     */
    private int countEnvironments(final PublishAuditHistory history) {
        if (history == null || !UtilMethods.isSet(history.getEndpointsMap())) {
            return 0;
        }
        return history.getEndpointsMap().size();
    }

    /**
     * Safely converts a Date to Instant.
     */
    private Instant toInstant(final Date date) {
        return date != null ? date.toInstant() : null;
    }

    /**
     * Safely parses a status name to Status enum (case-insensitive).
     *
     * @param statusName The status name to parse
     * @return Optional containing the Status if valid, empty otherwise
     */
    private Optional<Status> parseStatusSafely(final String statusName) {
        try {
            return Optional.of(Status.valueOf(statusName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            Logger.warn(this, "Invalid status value: " + statusName);
            return Optional.empty();
        }
    }

    // =========================================================================
    // DETAIL VIEW METHODS - For GET /v1/publishing/{bundleId}
    // =========================================================================

    /**
     * Transforms a PublishAuditStatus into a detailed PublishingJobDetailView
     * with full environment/endpoint breakdown.
     *
     * @param auditStatus The raw audit status to transform
     * @return A fully populated PublishingJobDetailView
     */
    public PublishingJobDetailView toPublishingJobDetailView(final PublishAuditStatus auditStatus) {
        final String bundleId = auditStatus.getBundleId();
        final Bundle bundle = getBundleSafely(bundleId);
        final PublishAuditHistory history = auditStatus.getStatusPojo();

        return PublishingJobDetailView.builder()
                .bundleId(bundleId)
                .bundleName(bundle != null ? bundle.getName() : null)
                .status(auditStatus.getStatus())
                .filterName(resolveFilterName(bundle))
                .assetCount(auditStatus.getTotalNumberOfAssets())
                .environments(buildEnvironmentDetails(history))
                .timestamps(buildTimestamps(auditStatus, history))
                .numTries(history != null ? history.getNumTries() : 0)
                .build();
    }

    /**
     * Builds the list of environment details with their endpoints from the audit history.
     *
     * @param history The audit history containing endpoint map
     * @return List of environment detail views
     */
    private List<EnvironmentDetailView> buildEnvironmentDetails(final PublishAuditHistory history) {
        if (history == null || !UtilMethods.isSet(history.getEndpointsMap())) {
            return List.of();
        }

        final List<EnvironmentDetailView> environments = new ArrayList<>();

        for (final Map.Entry<String, Map<String, EndpointDetail>> groupEntry :
                history.getEndpointsMap().entrySet()) {

            final String environmentId = groupEntry.getKey();
            final Environment environment = getEnvironmentSafely(environmentId);

            final List<EndpointDetailView> endpoints = groupEntry.getValue().entrySet().stream()
                    .map(endpointEntry -> buildEndpointDetailView(
                            endpointEntry.getKey(),
                            endpointEntry.getValue()))
                    .collect(Collectors.toList());

            environments.add(EnvironmentDetailView.builder()
                    .id(environmentId)
                    .name(environment != null ? environment.getName() : environmentId)
                    .endpoints(endpoints)
                    .build());
        }

        return environments;
    }

    /**
     * Builds the endpoint detail view from an endpoint ID and its status detail.
     *
     * @param endpointId The endpoint identifier
     * @param detail     The endpoint status detail
     * @return Endpoint detail view
     */
    private EndpointDetailView buildEndpointDetailView(final String endpointId,
                                                       final EndpointDetail detail) {
        final PublishingEndPoint endpoint = getEndpointSafely(endpointId);
        final Status statusEnum = detail != null
                ? PublishAuditStatus.getStatusObjectByCode(detail.getStatus())
                : null;

        return EndpointDetailView.builder()
                .id(endpointId)
                .serverName(endpoint != null && endpoint.getServerName() != null
                        ? endpoint.getServerName().toString()
                        : endpointId)
                .address(endpoint != null ? endpoint.getAddress() : "")
                .port(endpoint != null ? endpoint.getPort() : "")
                .protocol(endpoint != null ? endpoint.getProtocol() : "")
                .status(statusEnum)
                .statusMessage(detail != null ? detail.getInfo() : null)
                .stackTrace(shouldIncludeStackTrace(detail) ? detail.getStackTrace() : null)
                .build();
    }

    /**
     * Determines whether to include the stack trace in the response.
     * Stack traces are only included for failure statuses.
     *
     * @param detail The endpoint detail
     * @return true if stack trace should be included
     */
    private boolean shouldIncludeStackTrace(final EndpointDetail detail) {
        if (detail == null || !UtilMethods.isSet(detail.getStackTrace())) {
            return false;
        }
        return FAILURE_STATUS_CODES.contains(detail.getStatus());
    }

    /**
     * Builds the timestamps view from audit status and history.
     *
     * @param auditStatus The audit status
     * @param history     The audit history
     * @return Timestamps view
     */
    private TimestampsView buildTimestamps(final PublishAuditStatus auditStatus,
                                           final PublishAuditHistory history) {
        return TimestampsView.builder()
                .bundleStart(history != null ? toInstant(history.getBundleStart()) : null)
                .bundleEnd(history != null ? toInstant(history.getBundleEnd()) : null)
                .publishStart(history != null ? toInstant(history.getPublishStart()) : null)
                .publishEnd(history != null ? toInstant(history.getPublishEnd()) : null)
                .createDate(toInstant(auditStatus.getCreateDate()))
                .statusUpdated(toInstant(auditStatus.getStatusUpdated()))
                .build();
    }

    /**
     * Safely retrieves an environment by ID, returning null if not found or on error.
     *
     * @param environmentId The environment ID
     * @return Environment or null
     */
    private Environment getEnvironmentSafely(final String environmentId) {
        try {
            return environmentAPI.findEnvironmentById(environmentId);
        } catch (Exception e) {
            Logger.debug(this, "Unable to get environment: " + environmentId, e);
            return null;
        }
    }

    /**
     * Safely retrieves a publishing endpoint by ID, returning null if not found or on error.
     *
     * @param endpointId The endpoint ID
     * @return PublishingEndPoint or null
     */
    private PublishingEndPoint getEndpointSafely(final String endpointId) {
        try {
            return publishingEndPointAPI.findEndPointById(endpointId);
        } catch (Exception e) {
            Logger.debug(this, "Unable to get endpoint: " + endpointId, e);
            return null;
        }
    }
}
