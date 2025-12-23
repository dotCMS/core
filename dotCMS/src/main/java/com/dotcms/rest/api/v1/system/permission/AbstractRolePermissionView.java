package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Immutable view representing a role's permission assignments on an asset.
 * Each role can have individual permissions (directly on the asset) and
 * inheritable permissions (propagated to child assets).
 *
 * @author hassandotcms
 * @since 24.01
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = RolePermissionView.class)
@JsonDeserialize(as = RolePermissionView.class)
@Schema(description = "Role permission assignment for an asset")
public interface AbstractRolePermissionView {

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
     * Indicates if these permissions are inherited from a parent asset.
     *
     * @return true if inherited, false if set directly on this asset
     */
    @JsonProperty("inherited")
    @Schema(
        description = "Whether permissions are inherited from a parent asset",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean inherited();

    /**
     * Gets the individual permission levels assigned to this role on the asset.
     * These are permissions that apply directly to this asset.
     *
     * @return List of permission level names (READ, WRITE, PUBLISH, EDIT_PERMISSIONS, CAN_ADD_CHILDREN)
     */
    @JsonProperty("individual")
    @Schema(
        description = "Individual permission levels assigned directly to this role on the asset",
        example = "[\"READ\", \"WRITE\", \"PUBLISH\"]",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<String> individual();

    /**
     * Gets the inheritable permissions grouped by scope.
     * Only present for parent permissionables (hosts, folders).
     * Map keys are permission scopes (HOST, FOLDER, CONTENT, TEMPLATE, etc.)
     * and values are lists of permission level names.
     *
     * @return Map of scope to permission levels, or null if not a parent permissionable
     */
    @JsonProperty("inheritable")
    @Schema(
        description = "Inheritable permissions by scope (only for parent permissionables). " +
                     "Keys are permission scopes (HOST, FOLDER, CONTENT, etc.), " +
                     "values are permission level names",
        example = "{\"FOLDER\": [\"READ\", \"WRITE\"], \"CONTENT\": [\"READ\"]}"
    )
    @Nullable
    Map<String, List<String>> inheritable();
}
