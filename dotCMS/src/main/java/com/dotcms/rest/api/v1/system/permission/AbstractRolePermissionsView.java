package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.util.List;

/**
 * Response wrapper containing a role's permission assignments organized by assets.
 * Includes role information and a list of permission assets (hosts and folders)
 * where the role has access.
 *
 * @author dotCMS
 * @since 24.01
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = RolePermissionsView.class)
@JsonDeserialize(as = RolePermissionsView.class)
@Schema(description = "Role permissions organized by assets")
public interface AbstractRolePermissionsView {

    /**
     * Gets the role identifier.
     *
     * @return Role identifier
     */
    @JsonProperty("roleId")
    @Schema(
        description = "Role identifier",
        example = "abc-123-def-456",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String roleId();

    /**
     * Gets the role display name.
     *
     * @return Role name
     */
    @JsonProperty("roleName")
    @Schema(
        description = "Role display name",
        example = "CMS Administrator",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String roleName();

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
