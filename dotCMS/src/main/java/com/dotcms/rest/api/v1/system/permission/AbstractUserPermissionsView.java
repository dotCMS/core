package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.util.List;

/**
 * Response wrapper containing a user's permission assignments organized by assets.
 * Includes user information, their individual role ID, and a list of permission assets
 * (hosts and folders) they have access to.
 *
 * @author hassandotcms
 * @since 24.01
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = UserPermissionsView.class)
@JsonDeserialize(as = UserPermissionsView.class)
@Schema(description = "User permissions organized by assets")
public interface AbstractUserPermissionsView {

    /**
     * Gets the user information.
     *
     * @return User info view
     */
    @JsonProperty("user")
    @Schema(
        description = "User information",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    UserInfoView user();

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
     * Gets the list of permission assets.
     *
     * @return List of permission assets (hosts and folders)
     */
    @JsonProperty("assets")
    @Schema(
        description = "List of permission assets (hosts and folders) with their permission assignments",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<UserPermissionAssetView> assets();
}
