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
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = PermissionMetadataView.class)
@JsonDeserialize(as = PermissionMetadataView.class)
@Schema(description = "Permission metadata containing available levels and scopes in the system")
<<<<<<<< HEAD:dotCMS/src/main/java/com/dotcms/rest/api/v1/system/permission/PermissionMetadataView.java
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
        example = "[\"INDIVIDUAL\", \"HOST\", \"FOLDER\", \"CONTENT\", \"TEMPLATE\", \"PAGE\", \"CONTAINER\", \"STRUCTURE\", \"CATEGORY\"]",
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
========
public interface AbstractPermissionMetadataView {
>>>>>>>> 33394-implement-rest-api-get-permission-metadata:dotCMS/src/main/java/com/dotcms/rest/api/v1/system/permission/AbstractPermissionMetadataView.java

    /**
     * Gets the available permission levels.
     *
     * @return Set of permission level names
     */
<<<<<<<< HEAD:dotCMS/src/main/java/com/dotcms/rest/api/v1/system/permission/PermissionMetadataView.java
    public Set<String> getLevels() {
        return levels;
    }
========
    @JsonProperty("levels")
    @Schema(
        description = "Available permission levels that can be assigned to users and roles",
        example = "[\"READ\", \"WRITE\", \"PUBLISH\", \"EDIT_PERMISSIONS\", \"CAN_ADD_CHILDREN\"]",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    Set<String> levels();
>>>>>>>> 33394-implement-rest-api-get-permission-metadata:dotCMS/src/main/java/com/dotcms/rest/api/v1/system/permission/AbstractPermissionMetadataView.java

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
