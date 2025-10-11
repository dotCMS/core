package com.dotcms.rest.api.v1.user;

import com.dotcms.rest.api.Validated;
import com.dotcms.rest.exception.BadRequestException;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Form for updating user permissions on a specific asset.
 * Maps modern REST API structure to legacy RoleAjax.saveRolePermission() logic.
 *
 * @author Hassan
 * @since 24.01
 */
public class SaveUserPermissionsForm extends Validated {

    @JsonProperty("permissions")
    @Schema(
        description = "Permission assignments by scope (INDIVIDUAL, HOST, FOLDER, etc.) with permission levels (READ, WRITE, PUBLISH, EDIT_PERMISSIONS, CAN_ADD_CHILDREN)",
        example = "{\"INDIVIDUAL\": [\"READ\", \"WRITE\"], \"HOST\": [\"READ\"]}",
        required = true
    )
    @NotNull(message = "permissions is required")
    private final Map<String, List<String>> permissions;

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
     * @param permissions Map of permission scopes to permission levels
     * @param cascade Whether to cascade permissions to children
     */
    public SaveUserPermissionsForm(
        @JsonProperty("permissions") final Map<String, List<String>> permissions,
        @JsonProperty("cascade") final boolean cascade
    ) {
        this.permissions = permissions;
        this.cascade = cascade;
    }

    /**
     * Gets the permission assignments map.
     *
     * @return Map of scopes to permission levels
     */
    public Map<String, List<String>> getPermissions() {
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

        // Validate against metadata API
        final UserPermissionHelper helper = new UserPermissionHelper();
        final Set<String> validScopes = helper.getAvailablePermissionScopes();
        final List<String> validLevels = helper.getAvailablePermissionLevels();

        for (final Map.Entry<String, List<String>> entry : permissions.entrySet()) {
            final String scope = entry.getKey();
            if (!validScopes.contains(scope)) {
                throw new BadRequestException("Invalid permission scope: " + scope);
            }

            final List<String> levels = entry.getValue();

            // Validate permission levels are not null or empty
            if (levels == null) {
                throw new BadRequestException("Permission levels for scope '" + scope + "' cannot be null");
            }
            if (levels.isEmpty()) {
                throw new BadRequestException("Permission levels for scope '" + scope + "' cannot be empty");
            }

            // Validate each permission level
            for (final String level : levels) {
                if (level == null) {
                    throw new BadRequestException("Permission level cannot be null in scope '" + scope + "'");
                }
                if (!validLevels.contains(level)) {
                    throw new BadRequestException("Invalid permission level '" + level + "' in scope '" + scope + "'");
                }
            }
        }
    }
}
