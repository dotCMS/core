package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Form representing permissions for a single role in an asset permission update request.
 * Used as part of {@link UpdateAssetPermissionsForm}.
 *
 * <p>Example JSON:
 * <pre>{@code
 * {
 *   "roleId": "role-123",
 *   "individual": ["READ", "WRITE", "PUBLISH"],
 *   "inheritable": {
 *     "FOLDER": ["READ", "CAN_ADD_CHILDREN"],
 *     "CONTENT": ["READ", "WRITE", "PUBLISH"]
 *   }
 * }
 * }</pre>
 *
 * @author dotCMS
 * @since 24.01
 */
public class RolePermissionForm {

    private final String roleId;
    private final List<String> individual;
    private final Map<String, List<String>> inheritable;

    /**
     * Creates a new RolePermissionForm.
     *
     * @param roleId      Role identifier (required)
     * @param individual  Permission levels for this asset (e.g., ["READ", "WRITE"])
     * @param inheritable Permission scopes to permission levels for child assets
     */
    @JsonCreator
    public RolePermissionForm(
            @JsonProperty("roleId") final String roleId,
            @JsonProperty("individual") final List<String> individual,
            @JsonProperty("inheritable") final Map<String, List<String>> inheritable) {
        this.roleId = roleId;
        this.individual = individual;
        this.inheritable = inheritable;
    }

    /**
     * Gets the role identifier.
     *
     * @return Role ID string
     */
    public String getRoleId() {
        return roleId;
    }

    /**
     * Gets the individual (direct) permission levels for this asset.
     * Valid values: READ, WRITE, PUBLISH, EDIT_PERMISSIONS, CAN_ADD_CHILDREN
     *
     * @return List of permission level strings, or null if not specified
     */
    public List<String> getIndividual() {
        return individual;
    }

    /**
     * Gets the inheritable permissions map for child assets.
     * Keys are permission scopes (FOLDER, CONTENT, PAGE, etc.)
     * Values are lists of permission level strings.
     *
     * @return Map of scope to permission levels, or null if not specified
     */
    public Map<String, List<String>> getInheritable() {
        return inheritable;
    }
}
