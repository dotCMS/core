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
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        return new AssetMetadata(
            asset.getPermissionId(),
            getAssetType(asset),
            isInheriting ? "INHERITED" : "INDIVIDUAL",
            asset.isParentPermissionable(),
            canEditPermissions,
            canEdit,
            parentAssetId
        );
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

                // Build individual permissions array
                final List<String> individual = convertPermissionsToStringArray(individualPermissions);

                // Build inheritable permissions map (only for parent permissionables)
                final Map<String, List<String>> inheritable;
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
     * Asset metadata holder for constructing the response view.
     */
    public static class AssetMetadata {
        private final String assetId;
        private final String assetType;
        private final String inheritanceMode;
        private final boolean isParentPermissionable;
        private final boolean canEditPermissions;
        private final boolean canEdit;
        private final String parentAssetId;

        public AssetMetadata(final String assetId,
                            final String assetType,
                            final String inheritanceMode,
                            final boolean isParentPermissionable,
                            final boolean canEditPermissions,
                            final boolean canEdit,
                            final String parentAssetId) {
            this.assetId = assetId;
            this.assetType = assetType;
            this.inheritanceMode = inheritanceMode;
            this.isParentPermissionable = isParentPermissionable;
            this.canEditPermissions = canEditPermissions;
            this.canEdit = canEdit;
            this.parentAssetId = parentAssetId;
        }

        public String assetId() { return assetId; }
        public String assetType() { return assetType; }
        public String inheritanceMode() { return inheritanceMode; }
        public boolean isParentPermissionable() { return isParentPermissionable; }
        public boolean canEditPermissions() { return canEditPermissions; }
        public boolean canEdit() { return canEdit; }
        public String parentAssetId() { return parentAssetId; }
    }

    /**
     * Converts a list of permissions to an array of permission level strings.
     * Handles bit-packed permissions correctly.
     *
     * @param permissions List of permissions with same scope
     * @return List of permission level strings (e.g., ["READ", "WRITE"])
     */
    private List<String> convertPermissionsToStringArray(final List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new ArrayList<>();
        }

        // Combine all permission bits
        int combinedBits = 0;
        for (final Permission permission : permissions) {
            combinedBits |= permission.getPermission();
        }

        return convertBitsToPermissionNames(combinedBits);
    }

    /**
     * Builds inheritable permission map grouped by scope.
     *
     * @param inheritablePermissions List of inheritable permissions
     * @return Map of scope to permission level arrays
     */
    private Map<String, List<String>> buildInheritablePermissionMap(
            final List<Permission> inheritablePermissions) {

        if (inheritablePermissions == null || inheritablePermissions.isEmpty()) {
            return new HashMap<>();
        }

        return inheritablePermissions.stream()
            .collect(Collectors.groupingBy(
                p -> getModernPermissionType(p.getType()),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    this::convertPermissionsToStringArray
                )
            ));
    }

    /**
     * Converts permission bits to permission level names.
     * Avoids duplicate aliases (e.g., USE=READ, EDIT=WRITE).
     *
     * @param permissionBits Bit-packed permission value
     * @return List of permission level strings
     */
    private List<String> convertBitsToPermissionNames(final int permissionBits) {
        final List<String> permissions = new ArrayList<>();

        if ((permissionBits & PermissionAPI.PERMISSION_READ) > 0) {
            permissions.add("READ");
        }
        if ((permissionBits & PermissionAPI.PERMISSION_WRITE) > 0) {
            permissions.add("WRITE");
        }
        if ((permissionBits & PermissionAPI.PERMISSION_PUBLISH) > 0) {
            permissions.add("PUBLISH");
        }
        if ((permissionBits & PermissionAPI.PERMISSION_EDIT_PERMISSIONS) > 0) {
            permissions.add("EDIT_PERMISSIONS");
        }
        if ((permissionBits & PermissionAPI.PERMISSION_CAN_ADD_CHILDREN) > 0) {
            permissions.add("CAN_ADD_CHILDREN");
        }

        return permissions;
    }

    /**
     * Maps internal permission type class names to modern API type constants.
     */
    private static final Map<String, String> PERMISSION_TYPE_MAPPINGS = Map.ofEntries(
        Map.entry(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE.toUpperCase(), "INDIVIDUAL"),
        Map.entry(IHTMLPage.class.getCanonicalName().toUpperCase(), "PAGE"),
        Map.entry(Container.class.getCanonicalName().toUpperCase(), "CONTAINER"),
        Map.entry(Folder.class.getCanonicalName().toUpperCase(), "FOLDER"),
        Map.entry(Link.class.getCanonicalName().toUpperCase(), "LINK"),
        Map.entry(Template.class.getCanonicalName().toUpperCase(), "TEMPLATE"),
        Map.entry(TemplateLayout.class.getCanonicalName().toUpperCase(), "TEMPLATE_LAYOUT"),
        Map.entry(Structure.class.getCanonicalName().toUpperCase(), "CONTENT_TYPE"),
        Map.entry(Contentlet.class.getCanonicalName().toUpperCase(), "CONTENT"),
        Map.entry(Category.class.getCanonicalName().toUpperCase(), "CATEGORY"),
        Map.entry(Rule.class.getCanonicalName().toUpperCase(), "RULE"),
        Map.entry(Host.class.getCanonicalName().toUpperCase(), "HOST")
    );

    /**
     * Gets the modern API type name for a permission type.
     *
     * @param permissionType Internal permission type (class name or scope)
     * @return Modern API type constant
     */
    private String getModernPermissionType(final String permissionType) {
        if (!UtilMethods.isSet(permissionType)) {
            return StringPool.BLANK;
        }

        final String mappedType = PERMISSION_TYPE_MAPPINGS.get(permissionType.toUpperCase());
        if (mappedType != null) {
            return mappedType;
        }

        Logger.debug(this, () -> String.format("Unknown permission type: %s", permissionType));
        return permissionType.toUpperCase();
    }

    /**
     * Gets the asset type string for the response.
     * Maps Permissionable types to API type constants (uppercase enum).
     *
     * @param asset The permissionable asset
     * @return Asset type enum constant (e.g., "FOLDER", "HOST", "CONTENT")
     */
    private String getAssetType(final Permissionable asset) {
        if (asset == null) {
            return StringPool.BLANK;
        }

        final String permissionType = asset.getPermissionType();
        final String modernType = getModernPermissionType(permissionType);

        // Return uppercase for asset type enum
        return modernType;
    }
}
