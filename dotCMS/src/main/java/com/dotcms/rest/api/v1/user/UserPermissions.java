package com.dotcms.rest.api.v1.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * User permissions response containing permission assignments organized by asset.
 * This bean represents a user's individual role permissions across different hosts and folders.
 * Each asset in the response includes detailed permission information such as the asset type,
 * path, assigned permissions, and whether the user can edit those permissions.
 */
@Schema(description = "User permissions organized by asset type and scope")
public class UserPermissions {

    @JsonProperty("user")
    @Schema(
        description = "User information",
        required = true
    )
    private final UserInfo user;

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
     * @param user The user information (id, name, email)
     * @param roleId The user's individual role identifier
     * @param assets List of assets with their permission assignments
     */
    public UserPermissions(final UserInfo user, final String roleId,
                          final List<UserPermissionAsset> assets) {
        this.user = user;
        this.roleId = roleId;
        this.assets = assets;
    }

    /**
     * Gets the user information.
     *
     * @return User information
     */
    public UserInfo getUser() {
        return user;
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
