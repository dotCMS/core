package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * View for save user permissions operation result.
 * Contains the result of saving permissions for a user on an asset.
 *
 * @author hassandotcms
 */
public class SaveUserPermissionsView {

    @JsonProperty("userId")
    @Schema(description = "User identifier", example = "admin@dotcms.com", required = true)
    private final String userId;

    @JsonProperty("roleId")
    @Schema(description = "User's individual role identifier", example = "abc-123-def-456", required = true)
    private final String roleId;

    @JsonProperty("asset")
    @Schema(description = "The updated asset with new permission assignments", required = true)
    private final UserPermissionAssetView asset;

    @JsonProperty("cascadeInitiated")
    @Schema(description = "Whether permission cascade to children was initiated", required = true)
    private final boolean cascadeInitiated;

    /**
     * Constructs save response with updated permission information.
     *
     * @param userId User identifier
     * @param roleId User's individual role identifier
     * @param asset Updated asset with new permissions
     * @param cascadeInitiated Whether cascade was triggered
     */
    public SaveUserPermissionsView(
        final String userId,
        final String roleId,
        final UserPermissionAssetView asset,
        final boolean cascadeInitiated
    ) {
        this.userId = userId;
        this.roleId = roleId;
        this.asset = asset;
        this.cascadeInitiated = cascadeInitiated;
    }

    /**
     * Gets the user identifier.
     *
     * @return User ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Gets the user's individual role ID.
     *
     * @return Role identifier
     */
    public String getRoleId() {
        return roleId;
    }

    /**
     * Gets the updated asset with permissions.
     *
     * @return Permission asset view
     */
    public UserPermissionAssetView getAsset() {
        return asset;
    }

    /**
     * Checks if cascade was initiated.
     *
     * @return true if cascade job was triggered
     */
    public boolean isCascadeInitiated() {
        return cascadeInitiated;
    }
}
