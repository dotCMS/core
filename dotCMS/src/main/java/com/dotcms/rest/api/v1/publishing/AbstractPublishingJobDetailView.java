package com.dotcms.rest.api.v1.publishing;

import com.dotcms.annotations.Nullable;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.util.List;

/**
 * Represents detailed publishing job information with full environment/endpoint breakdown.
 * This view provides complete status information including per-endpoint success/failure status,
 * error messages, stack traces, and all timestamps.
 *
 * @since Jan 2026
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = PublishingJobDetailView.class)
@JsonDeserialize(as = PublishingJobDetailView.class)
@Schema(description = "Detailed publishing job with environment/endpoint breakdown")
public interface AbstractPublishingJobDetailView {

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
     * @return Status enum value
     */
    @Schema(
            description = "Current publishing status",
            example = "SUCCESS",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    PublishAuditStatus.Status status();

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
     * List of environments with their endpoints and individual status.
     *
     * @return List of environments with endpoint details
     */
    @Schema(
            description = "Environments with their endpoints and status details",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<EnvironmentDetailView> environments();

    /**
     * All timestamps related to this publishing job.
     *
     * @return Timestamps view
     */
    @Schema(
            description = "Publishing job timestamps",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    TimestampsView timestamps();

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
