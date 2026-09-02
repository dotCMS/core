package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * View for update role permissions operation result.
 * Contains the result of updating permissions for a role on an asset.
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = UpdateRolePermissionsView.class)
@JsonDeserialize(as = UpdateRolePermissionsView.class)
@Schema(description = "Result of updating role permissions on an asset")
public interface AbstractUpdateRolePermissionsView {

    /**
     * Gets the role identifier.
     *
     * @return Role ID
     */
    @JsonProperty("roleId")
    @Schema(
        description = "Role identifier",
        example = "abc-123-def-456",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String roleId();

    /**
     * Gets the role name.
     *
     * @return Role name
     */
    @JsonProperty("roleName")
    @Schema(
        description = "Role name",
        example = "Content Editor",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String roleName();

    /**
     * Gets the updated asset with permissions.
     *
     * @return Role permission asset view
     */
    @JsonProperty("asset")
    @Schema(
        description = "The updated asset with new permission assignments for this role",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    RolePermissionAssetView asset();
}
