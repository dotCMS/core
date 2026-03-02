package com.dotcms.rest.api.v1.publishing;

import com.dotcms.publishing.PublisherConfig.DeliveryStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.util.List;

/**
 * Form for retrying failed or successful bundles.
 * Supports bulk operations where multiple bundles can be retried in a single request.
 *
 * @author hassandotcms
 * @since Feb 2026
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = RetryBundlesForm.class)
@JsonDeserialize(as = RetryBundlesForm.class)
@Schema(description = "Request body for retrying publishing bundles")
public interface AbstractRetryBundlesForm {

    /**
     * List of bundle identifiers to retry.
     *
     * @return List of bundle IDs
     */
    @Schema(
            description = "List of bundle identifiers to retry",
            example = "[\"bundle-123\", \"bundle-456\"]",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<String> bundleIds();

    /**
     * Force push flag to override push history.
     * When true, the bundle will be re-published even if it was previously successful.
     * Note: For bundles with SUCCESS or SUCCESS_WITH_WARNINGS status, this is automatically set to true.
     *
     * @return Force push flag (default: false)
     */
    @Schema(
            description = "Force push to override existing content at endpoints. " +
                    "Automatically true for SUCCESS/SUCCESS_WITH_WARNINGS bundles.",
            example = "false"
    )
    @Value.Default
    default boolean forcePush() {
        return false;
    }

    /**
     * Delivery strategy determining which endpoints receive the retry.
     *
     * @return Delivery strategy (default: ALL_ENDPOINTS)
     */
    @Schema(
            description = "Which endpoints to retry: ALL_ENDPOINTS sends to all, " +
                    "FAILED_ENDPOINTS sends only to previously failed endpoints",
            example = "FAILED_ENDPOINTS"
    )
    @Value.Default
    default DeliveryStrategy deliveryStrategy() {
        return DeliveryStrategy.ALL_ENDPOINTS;
    }
}
