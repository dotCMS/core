package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Immutable view representing an asset's permission data including metadata
 * and a paginated list of role permissions. This is the entity returned by
 * the GET /v1/permissions/{assetId} endpoint.
 *
 * @author hassandotcms
 * @since 24.01
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = AssetPermissionsView.class)
@JsonDeserialize(as = AssetPermissionsView.class)
@Schema(description = "Asset permissions organized by roles with metadata")
public interface AbstractAssetPermissionsView {

    /**
     * Gets the asset identifier.
     *
     * @return Asset ID (inode or identifier depending on asset type)
     */
    @JsonProperty("assetId")
    @Schema(
        description = "Asset identifier",
        example = "48190c8c-42c4-46af-8d1a-0cd5db894797",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String assetId();

    /**
     * Gets the asset type.
     *
     * @return Asset type constant (HOST, FOLDER, CONTENT, TEMPLATE, CONTAINER, etc.)
     */
    @JsonProperty("assetType")
    @Schema(
        description = "Asset type",
        example = "FOLDER",
        allowableValues = {"HOST", "FOLDER", "CONTENT", "TEMPLATE", "CONTAINER", "PAGE", "LINK", "CATEGORY", "RULE", "CONTENT_TYPE"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String assetType();

    /**
     * Gets the permission inheritance mode.
     *
     * @return INHERITED if inheriting from parent, INDIVIDUAL if has own permissions
     */
    @JsonProperty("inheritanceMode")
    @Schema(
        description = "Permission inheritance mode",
        example = "INDIVIDUAL",
        allowableValues = {"INHERITED", "INDIVIDUAL"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String inheritanceMode();

    /**
     * Indicates if this asset can have child permissionables.
     * Hosts and folders are typically parent permissionables.
     *
     * @return true if asset can have children with inheritable permissions
     */
    @JsonProperty("isParentPermissionable")
    @Schema(
        description = "Whether this asset can have child permissionables (e.g., hosts and folders)",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean isParentPermissionable();

    /**
     * Indicates if the requesting user can edit permissions on this asset.
     *
     * @return true if user has EDIT_PERMISSIONS permission
     */
    @JsonProperty("canEditPermissions")
    @Schema(
        description = "Whether the requesting user can edit permissions on this asset",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean canEditPermissions();

    /**
     * Indicates if the requesting user can edit this asset.
     *
     * @return true if user has WRITE permission
     */
    @JsonProperty("canEdit")
    @Schema(
        description = "Whether the requesting user can edit this asset",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean canEdit();

    /**
     * Gets the parent asset identifier if one exists.
     *
     * @return Parent asset ID, or null if no parent
     */
    @JsonProperty("parentAssetId")
    @Schema(
        description = "Parent asset identifier (null if no parent or at root level)",
        example = "abc-123-def-456"
    )
    @Nullable
    String parentAssetId();

    /**
     * Gets the paginated list of role permissions.
     * Each entry represents a role and its permissions on this asset.
     *
     * @return List of role permission views
     */
    @JsonProperty("permissions")
    @Schema(
        description = "Paginated list of role permissions assigned to this asset",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<RolePermissionView> permissions();
}
