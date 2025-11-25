package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

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
public class UpdateAssetPermissionsForm {

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
}
