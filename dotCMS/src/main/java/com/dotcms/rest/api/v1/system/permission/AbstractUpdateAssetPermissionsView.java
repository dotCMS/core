package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

/**
 * Immutable view for the update asset permissions operation result.
 * Contains the result of saving permissions for multiple roles on an asset.
 *
 * <p>This view is returned by the PUT /api/v1/permissions/{assetId} endpoint
 * and includes information about the operation (message, counts) plus the
 * updated asset with its new permission assignments.
 *
 * @author dotCMS
 * @since 24.01
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = UpdateAssetPermissionsView.class)
@JsonDeserialize(as = UpdateAssetPermissionsView.class)
@Schema(description = "Result of updating asset permissions")
public interface AbstractUpdateAssetPermissionsView {

    /**
     * Gets the success message.
     *
     * @return Success message describing the operation result
     */
    @JsonProperty("message")
    @Schema(
        description = "Success message describing the operation result",
        example = "Permissions saved successfully",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String message();

    /**
     * Gets the number of permissions saved.
     *
     * @return Count of permission entries saved
     */
    @JsonProperty("permissionCount")
    @Schema(
        description = "Number of permission entries saved during this operation",
        example = "5",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    int permissionCount();

    /**
     * Indicates if inheritance was broken during this operation.
     * When saving permissions on an asset that was inheriting from its parent,
     * inheritance is automatically broken before saving.
     *
     * @return true if inheritance was broken, false if asset already had individual permissions
     */
    @JsonProperty("inheritanceBroken")
    @Schema(
        description = "Whether permission inheritance was broken during this operation. " +
                     "True if the asset was previously inheriting permissions from its parent.",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean inheritanceBroken();

    /**
     * Gets the updated asset with its new permission assignments.
     *
     * @return Asset permissions view with metadata and role permissions
     */
    @JsonProperty("asset")
    @Schema(
        description = "The updated asset with its new permission assignments",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    AssetPermissionsView asset();

    /**
     * Gets any warnings from cascade operations that partially failed.
     * Present only when cascade was requested and some jobs failed to trigger.
     *
     * @return Optional list of cascade warning messages
     */
    @JsonProperty("cascadeWarnings")
    @Schema(
        description = "Warnings from cascade operations that partially failed. " +
                     "Present only when cascade was requested and some role cascades failed.",
        example = "[\"Failed to trigger cascade for role xyz123: Connection timeout\"]",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    Optional<List<String>> cascadeWarnings();
}
