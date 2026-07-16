package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.rest.api.Validated;
import com.dotcms.rest.exception.BadRequestException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Form for updating role permissions on an asset via PUT /api/v1/permissions/role/{roleId}/asset/{assetId}.
 *
 * <p>This form represents the request body for the role permissions update endpoint.
 * The roleId comes from the path parameter, not from this form.
 * The cascade parameter is passed as a query parameter, not in this form.
 *
 * <p>Request semantics:
 * <ul>
 *   <li>PUT replaces all permissions for this role on the asset</li>
 *   <li>Omitting a scope preserves existing permissions for that scope (hybrid model)</li>
 *   <li>Empty array [] removes permissions for that scope</li>
 *   <li>Implicit inheritance break if asset currently inherits</li>
 * </ul>
 *
 * <p>Example JSON:
 * <pre>{@code
 * {
 *   "permissions": {
 *     "INDIVIDUAL": ["READ", "WRITE", "PUBLISH"],
 *     "HOST": ["READ", "WRITE"],
 *     "FOLDER": ["READ", "WRITE", "CAN_ADD_CHILDREN"],
 *     "CONTENT": ["READ", "WRITE", "PUBLISH"],
 *     "PAGE": ["READ", "WRITE", "PUBLISH"],
 *     "CONTAINER": ["READ", "WRITE"],
 *     "TEMPLATE": ["READ", "WRITE"],
 *     "TEMPLATE_LAYOUT": ["READ"],
 *     "LINK": ["READ", "WRITE"],
 *     "CONTENT_TYPE": ["READ"],
 *     "CATEGORY": ["READ", "CAN_ADD_CHILDREN"],
 *     "RULE": ["READ"]
 *   }
 * }
 * }</pre>
 *
 * @author dotCMS
 * @since 24.01
 */
public class UpdateRolePermissionsForm extends Validated {

    @JsonProperty("permissions")
    @Schema(
        description = "Permission assignments by scope (INDIVIDUAL, HOST, FOLDER, CONTENT, etc.) "
            + "with permission levels (READ, WRITE, PUBLISH, EDIT_PERMISSIONS, CAN_ADD_CHILDREN). "
            + "Omitting a scope preserves existing permissions. Empty array [] removes permissions for that scope.",
        example = "{\"INDIVIDUAL\": [\"READ\", \"WRITE\"], \"CONTENT\": [\"READ\", \"PUBLISH\"]}",
        required = true
    )
    @NotNull(message = "permissions is required")
    private final Map<String, List<String>> permissions;

    /**
     * Creates a new UpdateRolePermissionsForm.
     *
     * @param permissions Map of permission scopes to permission level arrays.
     *                    Keys are scope names (INDIVIDUAL, FOLDER, CONTENT, etc.).
     *                    Values are arrays of permission levels (READ, WRITE, PUBLISH, etc.).
     */
    @JsonCreator
    public UpdateRolePermissionsForm(
            @JsonProperty("permissions") final Map<String, List<String>> permissions) {
        this.permissions = permissions;
    }

    /**
     * Gets the permission map.
     *
     * @return Map of scope names to permission level arrays
     */
    public Map<String, List<String>> getPermissions() {
        return permissions;
    }

    /**
     * Validates the form data.
     *
     * <p>Validates:
     * <ul>
     *   <li>permissions is not null or empty</li>
     *   <li>Each scope is a valid permission scope</li>
     *   <li>Each permission level is valid (empty arrays are allowed for removal)</li>
     * </ul>
     *
     * @throws BadRequestException If validation fails
     */
    @Override
    public void checkValid() {
        super.checkValid(); // JSR-303 validation

        if (permissions == null || permissions.isEmpty()) {
            throw new BadRequestException("permissions cannot be empty");
        }

        // Validate scopes and permission levels
        for (final Map.Entry<String, List<String>> entry : permissions.entrySet()) {
            final String scope = entry.getKey();
            if (!PermissionConversionUtils.isValidScope(scope)) {
                throw new BadRequestException(String.format(
                    "Invalid permission scope: %s", scope));
            }

            final List<String> levels = entry.getValue();
            // Note: Empty arrays are valid (they mean "remove permissions for this scope")
            // but null values within the array are not valid
            if (levels != null) {
                for (final String level : levels) {
                    if (level == null) {
                        throw new BadRequestException(String.format(
                            "Permission level cannot be null in scope '%s'", scope));
                    }
                    if (!PermissionConversionUtils.isValidPermissionLevel(level)) {
                        throw new BadRequestException(String.format(
                            "Invalid permission level '%s' in scope '%s'", level, scope));
                    }
                }
            }
        }
    }
}
