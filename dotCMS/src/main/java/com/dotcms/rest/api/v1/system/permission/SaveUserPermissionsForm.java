package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.api.Validated;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.business.PermissionAPI;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;

/**
 * Form for updating user permissions on a specific asset.
 * Maps modern REST API structure to legacy RoleAjax.saveRolePermission() logic.
 *
 * @author hassandotcms
 */
public class SaveUserPermissionsForm extends Validated {

    @JsonProperty("permissions")
    @Schema(
        description = "Permission assignments by scope (INDIVIDUAL, HOST, FOLDER, etc.) with permission levels (READ, WRITE, PUBLISH, EDIT_PERMISSIONS, CAN_ADD_CHILDREN)",
        example = "{\"INDIVIDUAL\": [\"READ\", \"WRITE\"], \"HOST\": [\"READ\"]}",
        required = true
    )
    @NotNull(message = "permissions is required")
    private final Map<String, Set<PermissionAPI.Type>> permissions;

    @JsonProperty("cascade")
    @Schema(
        description = "Whether to cascade permissions to all children assets",
        example = "false",
        defaultValue = "false"
    )
    private final boolean cascade;

    /**
     * Constructs form for saving user permissions.
     *
     * @param permissions Map of permission scopes to permission types
     * @param cascade Whether to cascade permissions to children
     */
    public SaveUserPermissionsForm(
        @JsonProperty("permissions") final Map<String, Set<PermissionAPI.Type>> permissions,
        @JsonProperty("cascade") final boolean cascade
    ) {
        this.permissions = permissions;
        this.cascade = cascade;
    }

    /**
     * Gets the permission assignments map.
     *
     * @return Map of scopes to permission types
     */
    public Map<String, Set<PermissionAPI.Type>> getPermissions() {
        return permissions;
    }

    /**
     * Checks if cascade is enabled.
     *
     * @return true if permissions should cascade to children
     */
    public boolean isCascade() {
        return cascade;
    }

    @Override
    public void checkValid() {
        super.checkValid(); // JSR-303 validation

        if (permissions == null || permissions.isEmpty()) {
            throw new BadRequestException("permissions cannot be empty");
        }

        for (final Map.Entry<String, Set<PermissionAPI.Type>> entry : permissions.entrySet()) {
            final String scope = entry.getKey();

            // Validate scope using PermissionAPI.Scope enum
            if (!PermissionConversionUtils.isValidScope(scope)) {
                throw new BadRequestException("Invalid permission scope: " + scope +
                    ". Valid scopes: " + PermissionAPI.Scope.getAllScopeNames());
            }

            final Set<PermissionAPI.Type> levels = entry.getValue();

            // Validate permission levels are not null or empty
            if (levels == null) {
                throw new BadRequestException("Permission levels for scope '" + scope + "' cannot be null");
            }
            if (levels.isEmpty()) {
                throw new BadRequestException("Permission levels for scope '" + scope + "' cannot be empty");
            }

            // Validate no null elements in the set
            if (levels.contains(null)) {
                throw new BadRequestException("Permission level cannot be null for scope '" + scope + "'");
            }

            // Permission type validation is handled by Jackson enum deserialization
            // If invalid enum values are passed, Jackson will fail to deserialize
        }
    }
}
