package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * View for save user permissions operation result.
 * Contains the result of saving permissions for a user on an asset.
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = SaveUserPermissionsView.class)
@JsonDeserialize(as = SaveUserPermissionsView.class)
@Schema(description = "Result of saving user permissions on an asset")
public interface AbstractSaveUserPermissionsView {

    /**
     * Gets the user identifier.
     *
     * @return User ID
     */
    @JsonProperty("userId")
    @Schema(
        description = "User identifier",
        example = "admin@dotcms.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String userId();

    /**
     * Gets the user's individual role ID.
     *
     * @return Role identifier
     */
    @JsonProperty("roleId")
    @Schema(
        description = "User's individual role identifier",
        example = "abc-123-def-456",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String roleId();

    /**
     * Gets the updated asset with permissions.
     *
     * @return Permission asset view
     */
    @JsonProperty("asset")
    @Schema(
        description = "The updated asset with new permission assignments",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    UserPermissionAssetView asset();

    /**
     * Checks if cascade was initiated.
     *
     * @return true if cascade job was triggered
     */
    @JsonProperty("cascadeInitiated")
    @Schema(
        description = "Whether permission cascade to children was initiated",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean cascadeInitiated();
}
