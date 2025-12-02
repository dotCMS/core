package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.Set;

/**
 * Represents a single permission asset (host or folder) with its associated permissions.
 * This bean encapsulates the permission details for a specific asset that a user's
 * individual role has access to, including the asset metadata and the specific
 * permission levels granted.
 *
 * @author hassandotcms
 * @since 24.01
 */
@Schema(description = "Permission asset with associated permission assignments")
public class UserPermissionAssetView {

    @JsonProperty("id")
    @Schema(
        description = "Asset identifier (host identifier for HOST type, folder inode for FOLDER type)",
        example = "abc-123-def-456",
        required = true
    )
    private final String id;

    @JsonProperty("type")
    @Schema(
        description = "Asset type",
        allowableValues = {"HOST", "FOLDER"},
        example = "HOST",
        required = true
    )
    private final String type;

    @JsonProperty("name")
    @Schema(
        description = "Asset name (hostname for HOST, folder name for FOLDER)",
        example = "demo.dotcms.com",
        required = true
    )
    private final String name;

    @JsonProperty("path")
    @Schema(
        description = "Full path to the asset",
        example = "/demo.dotcms.com/application",
        required = true
    )
    private final String path;

    @JsonProperty("hostId")
    @Schema(
        description = "Host identifier (same as id for HOST type, parent host for FOLDER type)",
        example = "abc-123-def-456",
        required = true
    )
    private final String hostId;

    @JsonProperty("canEditPermissions")
    @Schema(
        description = "Whether the requesting user can edit permissions on this asset",
        example = "true",
        required = true
    )
    private final boolean canEditPermissions;

    @JsonProperty("inheritsPermissions")
    @Schema(
        description = "Whether this asset inherits permissions from its parent",
        example = "false",
        required = true
    )
    private final boolean inheritsPermissions;

    @JsonProperty("permissions")
    @Schema(
        description = "Map of permission types (INDIVIDUAL, HOST, FOLDER, etc.) to sets of permission level names (READ, WRITE, PUBLISH, EDIT_PERMISSIONS, CAN_ADD_CHILDREN)",
        example = "{\"INDIVIDUAL\": [\"READ\", \"WRITE\", \"PUBLISH\"], \"HOST\": [\"READ\"]}",
        required = true
    )
    private final Map<String, Set<String>> permissions;

    /**
     * Constructs a user permission asset view with all required fields.
     *
     * @param id Asset identifier
     * @param type Asset type (HOST or FOLDER)
     * @param name Asset name
     * @param path Full path to the asset
     * @param hostId Host identifier
     * @param canEditPermissions Whether the user can edit permissions
     * @param inheritsPermissions Whether permissions are inherited
     * @param permissions Map of permission types to permission names
     */
    public UserPermissionAssetView(final String id,
                                   final String type,
                                   final String name,
                                   final String path,
                                   final String hostId,
                                   final boolean canEditPermissions,
                                   final boolean inheritsPermissions,
                                   final Map<String, Set<String>> permissions) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.path = path;
        this.hostId = hostId;
        this.canEditPermissions = canEditPermissions;
        this.inheritsPermissions = inheritsPermissions;
        this.permissions = permissions;
    }

    /**
     * Gets the asset identifier.
     *
     * @return Asset ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the asset type.
     *
     * @return Asset type (HOST or FOLDER)
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the asset name.
     *
     * @return Asset name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the full path to the asset.
     *
     * @return Asset path
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the host identifier.
     *
     * @return Host ID
     */
    public String getHostId() {
        return hostId;
    }

    /**
     * Checks if the user can edit permissions on this asset.
     *
     * @return true if user can edit permissions, false otherwise
     */
    public boolean isCanEditPermissions() {
        return canEditPermissions;
    }

    /**
     * Checks if this asset inherits permissions from its parent.
     *
     * @return true if permissions are inherited, false otherwise
     */
    public boolean isInheritsPermissions() {
        return inheritsPermissions;
    }

    /**
     * Gets the permission assignments for this asset.
     * Map keys are permission types (INDIVIDUAL, HOST, FOLDER, etc.)
     * and values are sets of permission level names (READ, WRITE, etc.).
     *
     * @return Permission map
     */
    public Map<String, Set<String>> getPermissions() {
        return permissions;
    }
}
