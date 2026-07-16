package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * Immutable view for the reset asset permissions operation result.
 * Contains the result of resetting permissions for an asset to inherit from its parent.
 *
 * <p>This view is returned by the PUT /api/v1/permissions/{assetId}/_reset endpoint
 * and includes information about the operation (message, asset ID, and count of
 * removed permissions).
 *
 * @author dotCMS
 * @since 24.01
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = ResetAssetPermissionsView.class)
@JsonDeserialize(as = ResetAssetPermissionsView.class)
@Schema(description = "Result of resetting asset permissions to inherited")
public interface AbstractResetAssetPermissionsView {

    /**
     * Gets the success message.
     *
     * @return Success message describing the operation result
     */
    @JsonProperty("message")
    @Schema(
        description = "Success message describing the operation result",
        example = "Individual permissions removed. Asset now inherits from parent.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String message();

    /**
     * Gets the asset identifier.
     *
     * @return The identifier of the asset whose permissions were reset
     */
    @JsonProperty("assetId")
    @Schema(
        description = "The identifier of the asset whose permissions were reset",
        example = "abc123-def456",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String assetId();

    /**
     * Gets the count of individual permissions that were removed.
     *
     * @return Number of individual permissions removed during the reset operation
     */
    @JsonProperty("previousPermissionCount")
    @Schema(
        description = "Number of individual permissions that were removed during the reset operation",
        example = "5",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    int previousPermissionCount();
}
