package com.dotcms.rest.api.v1.system.permission;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper for building asset-centric permission responses for REST endpoints.
 * Provides permission data transformation for the View Asset Permissions API.
 *
 * @author hassandotcms
 * @since 24.01
 */
@ApplicationScoped
public class AssetPermissionHelper {

    private final PermissionAPI permissionAPI;
    private final RoleAPI roleAPI;
    private final HostAPI hostAPI;
    private final FolderAPI folderAPI;

    /**
     * Default constructor for CDI.
     */
    public AssetPermissionHelper() {
        this(APILocator.getPermissionAPI(),
             APILocator.getRoleAPI(),
             APILocator.getHostAPI(),
             APILocator.getFolderAPI());
    }

    /**
     * Constructor with dependency injection for testing and CDI.
     *
     * @param permissionAPI Permission API for permission operations
     * @param roleAPI Role API for role lookups
     * @param hostAPI Host API for host lookups
     * @param folderAPI Folder API for folder lookups
     */
    @Inject
    public AssetPermissionHelper(final PermissionAPI permissionAPI,
                                 final RoleAPI roleAPI,
                                 @Named("HostAPI") final HostAPI hostAPI,
                                 final FolderAPI folderAPI) {
        this.permissionAPI = permissionAPI;
        this.roleAPI = roleAPI;
        this.hostAPI = hostAPI;
        this.folderAPI = folderAPI;
    }

    /**
     * Resolves an asset by ID, trying multiple asset types.
     *
     * @param assetId Asset identifier (inode or identifier)
     * @return Permissionable asset or null if not found
     * @throws DotDataException If there's an error accessing data
     * @throws DotSecurityException If security validation fails
     */
    public Permissionable resolveAsset(final String assetId)
            throws DotDataException, DotSecurityException {

        if (!UtilMethods.isSet(assetId)) {
            return null;
        }

        final User systemUser = APILocator.getUserAPI().getSystemUser();
        final boolean respectFrontendRoles = false;

        Logger.debug(this, () -> String.format("Resolving asset by ID: %s", assetId));

        // Try Folder (most common for permissions)
        try {
            final Folder folder = folderAPI.find(assetId, systemUser, respectFrontendRoles);
            if (UtilMethods.isSet(() -> folder.getIdentifier())) {
                Logger.debug(this, () -> String.format("Resolved as Folder: %s", assetId));
                return folder;
            }
        } catch (Exception e) {
            Logger.debug(this, () -> String.format("Not a folder: %s", assetId));
        }

        // Try Host
        try {
            final Host host = hostAPI.find(assetId, systemUser, respectFrontendRoles);
            if (host != null && UtilMethods.isSet(host.getIdentifier())) {
                Logger.debug(this, () -> String.format("Resolved as Host: %s", assetId));
                return host;
            }
        } catch (Exception e) {
            Logger.debug(this, () -> String.format("Not a host: %s", assetId));
        }

        // Try Contentlet
        try {
            final Contentlet contentlet = APILocator.getContentletAPI()
                .findContentletByIdentifierAnyLanguage(assetId);
            if (contentlet != null && UtilMethods.isSet(contentlet.getIdentifier())) {
                Logger.debug(this, () -> String.format("Resolved as Contentlet: %s", assetId));
                return contentlet;
            }
        } catch (Exception e) {
            Logger.debug(this, () -> String.format("Not a contentlet: %s", assetId));
        }

        // Try Template
        try {
            final Template template = APILocator.getTemplateAPI()
                .findWorkingTemplate(assetId, systemUser, respectFrontendRoles);
            if (template != null && UtilMethods.isSet(template.getIdentifier())) {
                Logger.debug(this, () -> String.format("Resolved as Template: %s", assetId));
                return template;
            }
        } catch (Exception e) {
            Logger.debug(this, () -> String.format("Not a template: %s", assetId));
        }

        // Try Container
        try {
            final Container container = APILocator.getContainerAPI()
                .getWorkingContainerById(assetId, systemUser, respectFrontendRoles);
            if (container != null && UtilMethods.isSet(container.getIdentifier())) {
                Logger.debug(this, () -> String.format("Resolved as Container: %s", assetId));
                return container;
            }
        } catch (Exception e) {
            Logger.debug(this, () -> String.format("Not a container: %s", assetId));
        }

        Logger.warn(this, String.format("Unable to resolve asset: %s", assetId));
        return null;
    }

    /**
     * Builds asset metadata for constructing the response view.
     *
     * @param asset The permissionable asset
     * @param requestingUser User making the request
     * @return AssetMetadata containing all metadata fields
     * @throws DotDataException If there's an error accessing data
     * @throws DotSecurityException If security validation fails
     */
    public AssetMetadata getAssetMetadata(final Permissionable asset,
                                          final User requestingUser)
            throws DotDataException, DotSecurityException {

        final boolean canEditPermissions = permissionAPI.doesUserHavePermission(
            asset, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, requestingUser, false);

        final boolean canEdit = permissionAPI.doesUserHavePermission(
            asset, PermissionAPI.PERMISSION_WRITE, requestingUser, false);

        final boolean isInheriting = permissionAPI.isInheritingPermissions(asset);

        String parentAssetId = null;
        try {
            final Permissionable parent = permissionAPI.findParentPermissionable(asset);
            if (parent != null) {
                parentAssetId = parent.getPermissionId();
            }
        } catch (Exception e) {
            Logger.debug(this, () -> "No parent permissionable found for asset");
        }

        return AssetMetadata.builder()
            .assetId(asset.getPermissionId())
            .assetType(getAssetTypeAsScope(asset))
            .inheritanceMode(isInheriting ? InheritanceMode.INHERITED : InheritanceMode.INDIVIDUAL)
            .isParentPermissionable(asset.isParentPermissionable())
            .canEditPermissions(canEditPermissions)
            .canEdit(canEdit)
            .parentAssetId(parentAssetId)
            .build();
    }

    /**
     * Builds permission data grouped by role, returning typed immutable views.
     *
     * @param asset The permissionable asset
     * @param requestingUser User making the request
     * @return List of RolePermissionView objects
     * @throws DotDataException If there's an error accessing data
     */
    public List<RolePermissionView> buildRolePermissions(final Permissionable asset,
                                                         final User requestingUser)
            throws DotDataException {

        final List<Permission> permissions = permissionAPI.getPermissions(asset, true);

        if (permissions == null || permissions.isEmpty()) {
            Logger.debug(this, () -> "No permissions found for asset");
            return new ArrayList<>();
        }

        final boolean isInheriting = permissionAPI.isInheritingPermissions(asset);
        final boolean isParentPermissionable = asset.isParentPermissionable();

        // Group permissions by role ID
        final Map<String, List<Permission>> permissionsByRole = permissions.stream()
            .collect(Collectors.groupingBy(Permission::getRoleId, LinkedHashMap::new, Collectors.toList()));

        final List<RolePermissionView> rolePermissions = new ArrayList<>();

        for (final Map.Entry<String, List<Permission>> entry : permissionsByRole.entrySet()) {
            final String roleId = entry.getKey();
            final List<Permission> rolePermissionList = entry.getValue();

            try {
                final Role role = roleAPI.loadRoleById(roleId);
                if (role == null) {
                    Logger.warn(this, String.format("Role not found: %s", roleId));
                    continue;
                }

                // Separate individual and inheritable permissions
                final List<Permission> individualPermissions = rolePermissionList.stream()
                    .filter(Permission::isIndividualPermission)
                    .collect(Collectors.toList());

                final List<Permission> inheritablePermissions = rolePermissionList.stream()
                    .filter(p -> !p.isIndividualPermission())
                    .collect(Collectors.toList());

                // Build individual permissions set
                final Set<PermissionAPI.Type> individual = convertPermissionsToTypeSet(individualPermissions);

                // Build inheritable permissions map (only for parent permissionables)
                final Map<PermissionAPI.Scope, Set<PermissionAPI.Type>> inheritable;
                if (isParentPermissionable && !inheritablePermissions.isEmpty()) {
                    inheritable = buildInheritablePermissionMap(inheritablePermissions);
                } else {
                    inheritable = null;
                }

                final RolePermissionView rolePermissionView = RolePermissionView.builder()
                    .roleId(roleId)
                    .roleName(role.getName())
                    .inherited(isInheriting)
                    .individual(individual)
                    .inheritable(inheritable)
                    .build();

                rolePermissions.add(rolePermissionView);

            } catch (DotDataException e) {
                Logger.warn(this, String.format("Error loading role: %s - %s",
                    roleId, e.getMessage()));
            }
        }

        return rolePermissions;
    }

    /**
     * Converts a list of permissions to a set of permission types.
     * Handles bit-packed permissions correctly using EnumSet for efficiency.
     *
     * @param permissions List of permissions with same scope
     * @return Set of permission types
     */
    private Set<PermissionAPI.Type> convertPermissionsToTypeSet(final List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return EnumSet.noneOf(PermissionAPI.Type.class);
        }

        // Combine all permission bits
        int combinedBits = 0;
        for (final Permission permission : permissions) {
            combinedBits |= permission.getPermission();
        }

        return convertBitsToPermissionTypes(combinedBits);
    }

    /**
     * Builds inheritable permission map grouped by scope.
     *
     * @param inheritablePermissions List of inheritable permissions
     * @return Map of scope to permission types
     */
    private Map<PermissionAPI.Scope, Set<PermissionAPI.Type>> buildInheritablePermissionMap(
            final List<Permission> inheritablePermissions) {

        if (inheritablePermissions == null || inheritablePermissions.isEmpty()) {
            return new LinkedHashMap<>();
        }

        return inheritablePermissions.stream()
            .collect(Collectors.groupingBy(
                p -> getScopeFromPermissionType(p.getType()),
                LinkedHashMap::new,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    this::convertPermissionsToTypeSet
                )
            ));
    }

    /**
     * Converts permission bits to permission types.
     * Uses canonical types only (excludes aliases USE and EDIT).
     *
     * @param permissionBits Bit-packed permission value
     * @return Set of permission types
     */
    private Set<PermissionAPI.Type> convertBitsToPermissionTypes(final int permissionBits) {
        final EnumSet<PermissionAPI.Type> permissions = EnumSet.noneOf(PermissionAPI.Type.class);

        if ((permissionBits & PermissionAPI.PERMISSION_READ) > 0) {
            permissions.add(PermissionAPI.Type.READ);
        }
        if ((permissionBits & PermissionAPI.PERMISSION_WRITE) > 0) {
            permissions.add(PermissionAPI.Type.WRITE);
        }
        if ((permissionBits & PermissionAPI.PERMISSION_PUBLISH) > 0) {
            permissions.add(PermissionAPI.Type.PUBLISH);
        }
        if ((permissionBits & PermissionAPI.PERMISSION_EDIT_PERMISSIONS) > 0) {
            permissions.add(PermissionAPI.Type.EDIT_PERMISSIONS);
        }
        if ((permissionBits & PermissionAPI.PERMISSION_CAN_ADD_CHILDREN) > 0) {
            permissions.add(PermissionAPI.Type.CAN_ADD_CHILDREN);
        }

        return permissions;
    }

    /**
     * Gets the scope enum from a permission type string (class canonical name).
     *
     * @param permissionType Internal permission type (class name or "individual")
     * @return PermissionAPI.Scope enum value
     */
    private PermissionAPI.Scope getScopeFromPermissionType(final String permissionType) {
        if (!UtilMethods.isSet(permissionType)) {
            return PermissionAPI.Scope.INDIVIDUAL;
        }

        final PermissionAPI.Scope scope = PermissionAPI.Scope.fromPermissionType(permissionType);
        if (scope != null) {
            return scope;
        }

        Logger.debug(this, () -> String.format("Unknown permission type: %s", permissionType));
        return PermissionAPI.Scope.INDIVIDUAL;
    }

    /**
     * Gets the asset type as a Scope enum for the response.
     *
     * @param asset The permissionable asset
     * @return PermissionAPI.Scope representing the asset type
     */
    private PermissionAPI.Scope getAssetTypeAsScope(final Permissionable asset) {
        if (asset == null) {
            return PermissionAPI.Scope.INDIVIDUAL;
        }

        return getScopeFromPermissionType(asset.getPermissionType());
    }
}
