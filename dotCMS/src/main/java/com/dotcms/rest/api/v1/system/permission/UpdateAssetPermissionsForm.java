package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.api.Validated;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Form for updating asset permissions via PUT /api/v1/permissions/{assetId}.
 *
 * <p>This form represents the request body for the asset permissions update endpoint.
 * Note: The {@code cascade} parameter is passed as a query parameter, not in this form.
 *
 * <p>Example JSON:
 * <pre>{@code
 * {
 *   "permissions": [
 *     {
 *       "roleId": "role-123",
 *       "individual": ["READ", "WRITE", "PUBLISH"],
 *       "inheritable": {
 *         "FOLDER": ["READ", "CAN_ADD_CHILDREN"],
 *         "CONTENT": ["READ", "WRITE", "PUBLISH"]
 *       }
 *     },
 *     {
 *       "roleId": "role-456",
 *       "individual": ["READ"]
 *     }
 *   ]
 * }
 * }</pre>
 *
 * @author dotCMS
 * @since 24.01
 */
public class UpdateAssetPermissionsForm extends Validated {

    @JsonProperty("permissions")
    @Schema(
        description = "List of role permission entries. Each entry defines permissions for a specific role on the asset.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "permissions is required")
    private final List<RolePermissionForm> permissions;

    /**
     * Creates a new UpdateAssetPermissionsForm.
     *
     * @param permissions List of role permission entries (required)
     */
    @JsonCreator
    public UpdateAssetPermissionsForm(
            @JsonProperty("permissions") final List<RolePermissionForm> permissions) {
        this.permissions = permissions;
    }

    /**
     * Gets the list of role permission entries.
     * Each entry defines permissions for a specific role on the asset.
     *
     * @return List of RolePermissionForm entries
     */
    public List<RolePermissionForm> getPermissions() {
        return permissions;
    }

    @Override
    public void checkValid() {
        super.checkValid(); // JSR-303 validation

        if (permissions == null || permissions.isEmpty()) {
            throw new BadRequestException("permissions cannot be empty");
        }

        for (int i = 0; i < permissions.size(); i++) {
            final RolePermissionForm roleForm = permissions.get(i);
            validateRolePermissionForm(roleForm, i);
        }
    }

    /**
     * Validates a single RolePermissionForm entry.
     * Note: Permission level validation is handled by Jackson's enum deserialization.
     * This method validates structure and scope names.
     */
    private void validateRolePermissionForm(final RolePermissionForm roleForm, final int index) {
        if (roleForm == null) {
            throw new BadRequestException(
                    String.format("permissions[%d] cannot be null", index));
        }

        if (!UtilMethods.isSet(roleForm.getRoleId())) {
            throw new BadRequestException(
                    String.format("permissions[%d].roleId is required", index));
        }

        // Must have at least individual or inheritable permissions
        final boolean hasIndividual = roleForm.getIndividual() != null && !roleForm.getIndividual().isEmpty();
        final boolean hasInheritable = roleForm.getInheritable() != null && !roleForm.getInheritable().isEmpty();

        if (!hasIndividual && !hasInheritable) {
            throw new BadRequestException(
                    String.format("permissions[%d] must have at least 'individual' or 'inheritable' permissions", index));
        }

        // Individual permission type validation is handled by Jackson enum deserialization
        // If invalid enum values are passed, Jackson will fail to deserialize

        // Validate inheritable permissions (scope names and structure)
        if (hasInheritable) {
            for (final Map.Entry<String, Set<PermissionAPI.Type>> entry : roleForm.getInheritable().entrySet()) {
                final String scope = entry.getKey();
                final Set<PermissionAPI.Type> levels = entry.getValue();

                // Validate scope name using PermissionAPI.Scope enum
                if (scope == null || !PermissionConversionUtils.isValidScope(scope)) {
                    throw new BadRequestException(
                            String.format("permissions[%d].inheritable contains invalid scope '%s'. Valid scopes: %s",
                                    index, scope, PermissionAPI.Scope.getAllScopeNames()));
                }

                if (levels == null || levels.isEmpty()) {
                    throw new BadRequestException(
                            String.format("permissions[%d].inheritable['%s'] cannot be null or empty", index, scope));
                }

                // Permission type validation is handled by Jackson enum deserialization
            }
        }
    }
}
