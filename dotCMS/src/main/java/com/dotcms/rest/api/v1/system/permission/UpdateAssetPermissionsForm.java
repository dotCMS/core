package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.api.Validated;
import com.dotcms.rest.exception.BadRequestException;
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

        final Set<String> validScopes = PermissionConversionUtils.SCOPE_TO_TYPE_MAPPINGS.keySet();
        final Set<String> validLevels = PermissionConversionUtils.VALID_PERMISSION_LEVELS;

        for (int i = 0; i < permissions.size(); i++) {
            final RolePermissionForm roleForm = permissions.get(i);
            validateRolePermissionForm(roleForm, i, validScopes, validLevels);
        }
    }

    /**
     * Validates a single RolePermissionForm entry.
     */
    private void validateRolePermissionForm(final RolePermissionForm roleForm,
                                            final int index,
                                            final Set<String> validScopes,
                                            final Set<String> validLevels) {
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

        // Validate individual permission levels
        if (hasIndividual) {
            for (final String level : roleForm.getIndividual()) {
                if (level == null) {
                    throw new BadRequestException(
                            String.format("permissions[%d].individual contains null value", index));
                }
                if (!validLevels.contains(level.toUpperCase())) {
                    throw new BadRequestException(
                            String.format("permissions[%d].individual contains invalid level '%s'. Valid levels: %s",
                                    index, level, validLevels));
                }
            }
        }

        // Validate inheritable permissions (scope -> levels)
        if (hasInheritable) {
            for (final Map.Entry<String, List<String>> entry : roleForm.getInheritable().entrySet()) {
                final String scope = entry.getKey();
                final List<String> levels = entry.getValue();

                if (scope == null || !validScopes.contains(scope.toUpperCase())) {
                    throw new BadRequestException(
                            String.format("permissions[%d].inheritable contains invalid scope '%s'. Valid scopes: %s",
                                    index, scope, validScopes));
                }

                if (levels == null || levels.isEmpty()) {
                    throw new BadRequestException(
                            String.format("permissions[%d].inheritable['%s'] cannot be null or empty", index, scope));
                }

                for (final String level : levels) {
                    if (level == null) {
                        throw new BadRequestException(
                                String.format("permissions[%d].inheritable['%s'] contains null value", index, scope));
                    }
                    if (!validLevels.contains(level.toUpperCase())) {
                        throw new BadRequestException(
                                String.format("permissions[%d].inheritable['%s'] contains invalid level '%s'. Valid levels: %s",
                                        index, scope, level, validLevels));
                    }
                }
            }
        }
    }
}
