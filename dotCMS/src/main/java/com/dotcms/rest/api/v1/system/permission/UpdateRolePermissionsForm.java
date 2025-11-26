package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
public class UpdateRolePermissionsForm {

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
}
