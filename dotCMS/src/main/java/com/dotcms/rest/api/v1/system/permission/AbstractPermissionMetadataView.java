package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.util.Set;

/**
 * Permission metadata response containing available permission levels and scopes.
 * This interface represents the permission configuration options available in the dotCMS system.
 * It provides information about what permission levels can be assigned (READ, WRITE, PUBLISH, etc.)
 * and what asset types support permissions (HOST, FOLDER, CONTENT, etc.).
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = PermissionMetadataView.class)
@JsonDeserialize(as = PermissionMetadataView.class)
@Schema(description = "Permission metadata containing available levels and scopes in the system")
public interface AbstractPermissionMetadataView {

    /**
     * Gets the available permission levels.
     *
     * @return Set of permission level names (READ, WRITE, PUBLISH, EDIT_PERMISSIONS, CAN_ADD_CHILDREN)
     */
    @JsonProperty("levels")
    @Schema(
        description = "Available permission levels that can be assigned to users and roles",
        example = "[\"READ\", \"WRITE\", \"PUBLISH\", \"EDIT_PERMISSIONS\", \"CAN_ADD_CHILDREN\"]",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    Set<String> levels();

    /**
     * Gets the available permission scopes.
     *
     * @return Set of permission scope names (asset types)
     */
    @JsonProperty("scopes")
    @Schema(
        description = "Available permission scopes (asset types) that support permissions",
        example = "[\"INDIVIDUAL\", \"HOST\", \"FOLDER\", \"CONTENT\", \"TEMPLATE\", \"PAGE\", \"CONTAINER\", \"CONTENT_TYPE\", \"CATEGORY\"]",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    Set<String> scopes();
}