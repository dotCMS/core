package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

/**
 * Permission metadata response containing available permission levels and scopes.
 * This bean represents the permission configuration options available in the dotCMS system.
 * It provides information about what permission levels can be assigned (READ, WRITE, PUBLISH, etc.)
 * and what asset types support permissions (HOST, FOLDER, CONTENT, etc.).
 */
@Schema(description = "Permission metadata containing available levels and scopes in the system")
public class PermissionMetadataView {

    @JsonProperty("levels")
    @Schema(
        description = "Available permission levels that can be assigned to users and roles",
        example = "[\"READ\", \"WRITE\", \"PUBLISH\", \"EDIT_PERMISSIONS\", \"CAN_ADD_CHILDREN\"]",
        required = true
    )
    private final Set<String> levels;

    @JsonProperty("scopes")
    @Schema(
        description = "Available permission scopes (asset types) that support permissions",
        example = "[\"INDIVIDUAL\", \"HOST\", \"FOLDER\", \"CONTENT\", \"TEMPLATE\", \"PAGE\", \"CONTAINER\", \"CONTENT_TYPE\", \"CATEGORY\"]",
        required = true
    )
    private final Set<String> scopes;

    /**
     * Constructs permission metadata with available levels and scopes.
     *
     * @param levels The set of available permission levels (READ, WRITE, PUBLISH, EDIT_PERMISSIONS, CAN_ADD_CHILDREN)
     * @param scopes The set of available permission scopes (asset types that support permissions)
     */
    public PermissionMetadataView(final Set<String> levels, final Set<String> scopes) {
        this.levels = levels;
        this.scopes = scopes;
    }

    /**
     * Gets the available permission levels.
     *
     * @return Set of permission level names
     */
    public Set<String> getLevels() {
        return levels;
    }

    /**
     * Gets the available permission scopes.
     *
     * @return Set of permission scope names (asset types)
     */
    public Set<String> getScopes() {
        return scopes;
    }
}
