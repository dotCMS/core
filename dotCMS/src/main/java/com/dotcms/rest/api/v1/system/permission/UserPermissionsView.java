package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response wrapper containing a user's permission assignments organized by assets.
 * Includes user information, their individual role ID, and a list of permission assets
 * (hosts and folders) they have access to.
 *
 * @author dotCMS
 * @since 24.01
 */
@Schema(description = "User permissions organized by assets")
public class UserPermissionsView {

    @JsonProperty("user")
    @Schema(
        description = "User information",
        required = true
    )
    private final UserInfoView user;

    @JsonProperty("roleId")
    @Schema(
        description = "User's individual role identifier",
        example = "abc-123-def-456",
        required = true
    )
    private final String roleId;

    @JsonProperty("assets")
    @Schema(
        description = "List of permission assets (hosts and folders) with their permission assignments",
        required = true
    )
    private final List<UserPermissionAssetView> assets;

    /**
     * Constructs the user permissions view.
     *
     * @param user User information
     * @param roleId User's individual role ID
     * @param assets List of permission assets
     */
    public UserPermissionsView(final UserInfoView user,
                               final String roleId,
                               final List<UserPermissionAssetView> assets) {
        this.user = user;
        this.roleId = roleId;
        this.assets = assets;
    }

    public UserInfoView getUser() {
        return user;
    }

    public String getRoleId() {
        return roleId;
    }

    public List<UserPermissionAssetView> getAssets() {
        return assets;
    }
}
