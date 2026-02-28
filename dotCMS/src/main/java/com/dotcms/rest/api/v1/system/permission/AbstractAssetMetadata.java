package com.dotcms.rest.api.v1.system.permission;

import com.dotmarketing.business.PermissionAPI;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Immutable metadata holder for asset permission responses.
 * Contains asset identification and user capability information.
 *
 * @author hassandotcms
 * @since 24.01
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
public interface AbstractAssetMetadata {

    /**
     * Gets the asset identifier.
     *
     * @return Asset ID (inode or identifier depending on asset type)
     */
    String assetId();

    /**
     * Gets the asset type scope.
     *
     * @return Permission scope representing the asset type
     */
    PermissionAPI.Scope assetType();

    /**
     * Gets the permission inheritance mode.
     *
     * @return INHERITED if inheriting from parent, INDIVIDUAL if has own permissions
     */
    InheritanceMode inheritanceMode();

    /**
     * Indicates if this asset can have child permissionables.
     *
     * @return true if asset can have children with inheritable permissions
     */
    boolean isParentPermissionable();

    /**
     * Indicates if the requesting user can edit permissions on this asset.
     *
     * @return true if user has EDIT_PERMISSIONS permission
     */
    boolean canEditPermissions();

    /**
     * Indicates if the requesting user can edit this asset.
     *
     * @return true if user has WRITE permission
     */
    boolean canEdit();

    /**
     * Gets the parent asset identifier if one exists.
     *
     * @return Parent asset ID, or null if no parent
     */
    @Nullable
    String parentAssetId();
}
