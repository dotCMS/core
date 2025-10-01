package com.dotcms.rest.api.v1.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * User permissions response containing permission assignments organized by asset.
 * This bean represents a user's individual role permissions across different hosts and folders.
 * Each asset in the response includes detailed permission information such as the asset type,
 * path, assigned permissions, and whether the user can edit those permissions.
 */
@Schema(description = "User permissions organized by asset type and scope")
public class UserPermissions {

    @JsonProperty("userId")
    @Schema(
        description = "User identifier (email address or user ID)",
        example = "admin@dotcms.com",
        required = true
    )
    private final String userId;

    @JsonProperty("roleId")
    @Schema(
        description = "The user's individual role identifier",
        example = "abc-123-def-456",
        required = true
    )
    private final String roleId;

    @JsonProperty("assets")
    @Schema(
        description = "List of assets (hosts and folders) with their permission assignments",
        required = true
    )
    private final List<UserPermissionAsset> assets;

    /**
     * Constructs user permissions response.
     *
     * @param userId The user identifier (email or user ID)
     * @param roleId The user's individual role identifier
     * @param assets List of assets with their permission assignments
     */
    public UserPermissions(final String userId, final String roleId,
                          final List<UserPermissionAsset> assets) {
        this.userId = userId;
        this.roleId = roleId;
        this.assets = assets;
    }

    /**
     * Gets the user identifier.
     *
     * @return User ID or email address
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
     * Gets the list of assets with their permission assignments.
     *
     * @return List of permission assets
     */
    public List<UserPermissionAsset> getAssets() {
        return assets;
    }
}
