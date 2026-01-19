package com.dotcms.rest.api.v1.publishing;

import com.dotcms.annotations.Nullable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.List;

/**
 * Represents a publishing job combining audit status and bundle metadata.
 * This view provides a unified representation of publishing operations including
 * status, bundle information, asset preview, and timing details.
 *
 * @author dotCMS
 * @since Jan 2026
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = PublishingJobView.class)
@JsonDeserialize(as = PublishingJobView.class)
@Schema(description = "Publishing job combining audit status and bundle metadata")
public interface AbstractPublishingJobView {

    /**
     * Unique bundle identifier.
     *
     * @return Bundle ID
     */
    @Schema(
            description = "Unique bundle identifier",
            example = "f3d9a4b7-staging-bundle-2026-01-15",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String bundleId();

    /**
     * Human-readable bundle name.
     *
     * @return Bundle name or null if not set
     */
    @Schema(
            description = "Human-readable bundle name",
            example = "bundle-testName"
    )
    @Nullable
    String bundleName();

    /**
     * Current publishing status.
     *
     * @return Status name
     */
    @Schema(
            description = "Current publishing status",
            example = "SUCCESS",
            allowableValues = {
                    // Initial/Queue states
                    "BUNDLE_REQUESTED", "WAITING_FOR_PUBLISHING",
                    // In-progress states
                    "BUNDLING", "SENDING_TO_ENDPOINTS", "PUBLISHING_BUNDLE",
                    // Intermediate states
                    "BUNDLE_SENT_SUCCESSFULLY", "RECEIVED_BUNDLE", "BUNDLE_SAVED_SUCCESSFULLY",
                    // Success states
                    "SUCCESS", "SUCCESS_WITH_WARNINGS",
                    // Failure states
                    "FAILED_TO_BUNDLE", "FAILED_TO_SENT", "FAILED_TO_SEND_TO_ALL_GROUPS",
                    "FAILED_TO_SEND_TO_SOME_GROUPS", "FAILED_TO_PUBLISH",
                    "FAILED_INTEGRITY_CHECK", "INVALID_TOKEN", "LICENSE_REQUIRED"
            },
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String status();

    /**
     * Publishing filter name used for this bundle.
     *
     * @return Filter name or null if not set
     */
    @Schema(
            description = "Publishing filter name used",
            example = "Live to Staging"
    )
    @Nullable
    String filterName();

    /**
     * Total number of assets in the bundle.
     *
     * @return Asset count
     */
    @Schema(
            description = "Total number of assets in the bundle",
            example = "47",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    int assetCount();

    /**
     * Preview of first 3 assets in the bundle.
     *
     * @return List of asset previews (max 3)
     */
    @Schema(
            description = "Preview of first 3 assets in the bundle",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<AssetPreviewView> assetPreview();

    /**
     * Number of target environments for this bundle.
     *
     * @return Environment count
     */
    @Schema(
            description = "Number of target environments",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    int environmentCount();

    /**
     * Bundle creation timestamp.
     *
     * @return Creation date/time
     */
    @Schema(
            description = "Bundle creation timestamp in ISO 8601 format",
            example = "2026-01-15T10:29:55Z",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    Instant createDate();

    /**
     * Last status update timestamp.
     *
     * @return Status update date/time or null
     */
    @Schema(
            description = "Last status update timestamp in ISO 8601 format",
            example = "2026-01-15T10:31:22Z"
    )
    @Nullable
    Instant statusUpdated();

    /**
     * Number of publish attempts.
     *
     * @return Number of tries
     */
    @Schema(
            description = "Number of publish attempts",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    int numTries();

}
