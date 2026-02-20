package com.dotcms.rest.api.v1.publishing;

import com.dotcms.annotations.Nullable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * Result view for a single bundle retry operation.
 * Contains success/failure status and details about the retry attempt.
 *
 * @author hassandotcms
 * @since Feb 2026
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = RetryBundleResultView.class)
@JsonDeserialize(as = RetryBundleResultView.class)
@Schema(description = "Result of a single bundle retry operation")
public interface AbstractRetryBundleResultView {

    /**
     * The bundle identifier that was processed.
     *
     * @return Bundle ID
     */
    @Schema(
            description = "Bundle identifier that was processed",
            example = "01HQXYZ123456789ABCDEFGHIJ",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String bundleId();

    /**
     * Whether the retry operation was successful.
     *
     * @return true if bundle was successfully re-queued
     */
    @Schema(
            description = "Whether the retry operation was successful",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean success();

    /**
     * Human-readable message describing the result.
     *
     * @return Result message
     */
    @Schema(
            description = "Human-readable message describing the result",
            example = "Bundle successfully re-queued for publishing",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String message();

    /**
     * Whether force push was applied (null if retry failed).
     *
     * @return Force push flag or null
     */
    @Schema(
            description = "Whether force push was applied to this bundle",
            example = "true"
    )
    @Nullable
    Boolean forcePush();

    /**
     * The publishing operation type: PUBLISH or UNPUBLISH (null if retry failed).
     *
     * @return Operation type or null
     */
    @Schema(
            description = "Publishing operation type",
            example = "PUBLISH"
    )
    @Nullable
    String operation();

    /**
     * The delivery strategy used for this retry.
     *
     * @return Delivery strategy name
     */
    @Schema(
            description = "Delivery strategy used: ALL_ENDPOINTS or FAILED_ENDPOINTS",
            example = "FAILED_ENDPOINTS",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String deliveryStrategy();

    /**
     * Number of assets in the bundle (null if retry failed).
     *
     * @return Asset count or null
     */
    @Schema(
            description = "Number of assets in the bundle",
            example = "47"
    )
    @Nullable
    Integer assetCount();
}
