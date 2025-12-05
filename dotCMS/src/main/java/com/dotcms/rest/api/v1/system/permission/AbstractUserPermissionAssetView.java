package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Set;

/**
 * Represents a single permission asset (host or folder) with its associated permissions.
 * This interface encapsulates the permission details for a specific asset that a user's
 * individual role has access to, including the asset metadata and the specific
 * permission levels granted.
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = UserPermissionAssetView.class)
@JsonDeserialize(as = UserPermissionAssetView.class)
@Schema(description = "Permission asset with associated permission assignments")
public interface AbstractUserPermissionAssetView {

    /**
     * Gets the asset identifier.
     *
     * @return Asset ID (host identifier for HOST type, folder inode for FOLDER type)
     */
    @JsonProperty("id")
    @Schema(
        description = "Asset identifier (host identifier for HOST type, folder inode for FOLDER type)",
        example = "abc-123-def-456",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String id();

    /**
     * Gets the asset type.
     *
     * @return Asset type (HOST or FOLDER)
     */
    @JsonProperty("type")
    @Schema(
        description = "Asset type",
        allowableValues = {"HOST", "FOLDER"},
        example = "HOST",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String type();

    /**
     * Gets the asset name.
     *
     * @return Asset name (hostname for HOST, folder name for FOLDER)
     */
    @JsonProperty("name")
    @Schema(
        description = "Asset name (hostname for HOST, folder name for FOLDER)",
        example = "demo.dotcms.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String name();

    /**
     * Gets the full path to the asset.
     *
     * @return Asset path
     */
    @JsonProperty("path")
    @Schema(
        description = "Full path to the asset",
        example = "/demo.dotcms.com/application",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String path();

    /**
     * Gets the host identifier.
     *
     * @return Host ID (same as id for HOST type, parent host for FOLDER type)
     */
    @JsonProperty("hostId")
    @Schema(
        description = "Host identifier (same as id for HOST type, parent host for FOLDER type)",
        example = "abc-123-def-456",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String hostId();

    /**
     * Checks if the user can edit permissions on this asset.
     *
     * @return true if user can edit permissions, false otherwise
     */
    @JsonProperty("canEditPermissions")
    @Schema(
        description = "Whether the requesting user can edit permissions on this asset",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean canEditPermissions();

    /**
     * Checks if this asset inherits permissions from its parent.
     *
     * @return true if permissions are inherited, false otherwise
     */
    @JsonProperty("inheritsPermissions")
    @Schema(
        description = "Whether this asset inherits permissions from its parent",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean inheritsPermissions();

    /**
     * Gets the permission assignments for this asset.
     * Map keys are permission types (INDIVIDUAL, HOST, FOLDER, etc.)
     * and values are sets of permission level names (READ, WRITE, etc.).
     *
     * @return Permission map
     */
    @JsonProperty("permissions")
    @Schema(
        description = "Map of permission types (INDIVIDUAL, HOST, FOLDER, etc.) to sets of permission level names (READ, WRITE, PUBLISH, EDIT_PERMISSIONS, CAN_ADD_CHILDREN)",
        example = "{\"INDIVIDUAL\": [\"READ\", \"WRITE\", \"PUBLISH\"], \"HOST\": [\"READ\"]}",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    Map<String, Set<String>> permissions();
}
