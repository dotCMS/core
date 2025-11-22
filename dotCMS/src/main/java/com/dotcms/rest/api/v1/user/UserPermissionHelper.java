package com.dotcms.rest.api.v1.user;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
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
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Helper for transforming user permissions to REST responses.
 */
@ApplicationScoped
public class UserPermissionHelper {

    private final PermissionAPI permissionAPI;
    private final HostAPI hostAPI;
    private final FolderAPI folderAPI;
    private final UserAPI userAPI;

    public UserPermissionHelper() {
        this(APILocator.getPermissionAPI(),
             APILocator.getHostAPI(),
             APILocator.getFolderAPI(),
             APILocator.getUserAPI());
    }

    @Inject
    public UserPermissionHelper(final PermissionAPI permissionAPI,
                               @Named("HostAPI") final HostAPI hostAPI,
                               final FolderAPI folderAPI,
                               final UserAPI userAPI) {
        this.permissionAPI = permissionAPI;
        this.hostAPI = hostAPI;
        this.folderAPI = folderAPI;
        this.userAPI = userAPI;
    }

    /**
     * Builds permission response data for the given role, grouped by asset.
     */
    public List<UserPermissionAsset> buildUserPermissionResponse(final Role role, final User requestingUser)
            throws DotDataException, DotSecurityException {

        final User systemUser = userAPI.getSystemUser();
        final Host systemHost = APILocator.systemHost();
        final boolean respectFrontendRoles = false;

        final Set<Permissionable> permissionAssets = new HashSet<>();
        final Map<String, List<Permission>> permissionsByInode = new HashMap<>();

        final List<Permission> permissions = permissionAPI.getPermissionsByRole(role, true, true);

        collectPermissionAssets(permissions, systemUser, respectFrontendRoles,
                               permissionAssets, permissionsByInode);

        final List<UserPermissionAsset> result = new ArrayList<>();
        boolean systemHostInList = false;

        for (Permissionable asset : permissionAssets) {
            if (asset instanceof Host && ((Host)asset).isSystemHost()) {
                systemHostInList = true;
            }

            final String assetId = getAssetId(asset);
            final List<Permission> assetPermissions = permissionsByInode.get(assetId);
            result.add(buildAssetResponse(asset, assetPermissions, requestingUser, systemUser));
        }

        // Always include system host
        if (!systemHostInList) {
            result.add(buildAssetResponse(systemHost, new ArrayList<>(), requestingUser, systemUser));
        }

        return result;
    }

    /**
     * Collects assets from permissions and groups by asset ID.
     * Mutates the provided collections.
     */
    private void collectPermissionAssets(
            final List<Permission> permissions,
            final User systemUser,
            final boolean respectFrontendRoles,
            final Set<Permissionable> permissionAssets,
            final Map<String, List<Permission>> permissionsByInode) 
            throws DotDataException, DotSecurityException {
        
        for (final Permission permission : permissions) {
            permissionsByInode.computeIfAbsent(permission.getInode(), k -> new ArrayList<>()).add(permission);

            final Folder folder = folderAPI.find(permission.getInode(), systemUser, respectFrontendRoles);

            if (UtilMethods.isSet(() -> folder.getIdentifier())) {
                permissionAssets.add(folder);
            } else {
                final Host host = hostAPI.find(permission.getInode(), systemUser, respectFrontendRoles);
                if (host != null) {
                    permissionAssets.add(host);
                }
            }
        }
    }

    private String getAssetId(final Permissionable asset) {
        if (asset instanceof Host) {
            return ((Host) asset).getIdentifier();
        } else if (asset instanceof Folder) {
            return ((Folder) asset).getInode();
        }
        return StringPool.BLANK;
    }

    /**
     * Builds response data for a single permissionable asset.
     */
    private UserPermissionAsset buildAssetResponse(final Permissionable asset,
                                                   final List<Permission> permissions,
                                                   final User requestingUser,
                                                   final User systemUser)
            throws DotDataException, DotSecurityException {

        final String id;
        final String type;
        final String name;
        final String path;
        final String hostId;

        if (asset instanceof Host) {
            final Host host = (Host) asset;
            id = host.getIdentifier();
            type = "HOST";
            name = host.getHostname();
            path = "/" + host.getHostname();
            hostId = host.getIdentifier();
        } else if (asset instanceof Folder) {
            final Folder folder = (Folder) asset;
            final Identifier identifier = APILocator.getIdentifierAPI().find(folder.getIdentifier());
            final Host host = hostAPI.find(folder.getHostId(), systemUser, false);

            id = folder.getInode();
            type = "FOLDER";
            name = folder.getName();
            path = "/" + host.getHostname() + identifier.getParentPath() + folder.getName();
            hostId = folder.getHostId();
        } else {
            throw new DotDataException("Unsupported asset type: " + asset.getClass().getName());
        }

        final boolean canEditPermissions = permissionAPI.doesUserHavePermission(
                asset, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, requestingUser, false);

        final boolean inheritsPermissions = permissionAPI.isInheritingPermissions(asset);

        final Map<String, List<String>> permissionMap = buildPermissionMap(permissions);

        return new UserPermissionAsset(id, type, name, path, hostId,
                                      canEditPermissions, inheritsPermissions, permissionMap);
    }

    /**
     * Groups permissions by type with permission names.
     */
    public Map<String, List<String>> buildPermissionMap(final List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new HashMap<>();
        }

        return permissions.stream()
            .collect(Collectors.groupingBy(
                p -> getModernPermissionType(p.getType()),
                Collectors.mapping(
                    p -> convertBitsToPermissionNames(p.getPermission()),
                    Collectors.flatMapping(List::stream, 
                        Collectors.collectingAndThen(
                            Collectors.toSet(),
                            ArrayList::new
                        ))
                )
            ));
    }

    /**
     * Maps permission type class names to API type constants.
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

    private String getModernPermissionType(final String permissionType) {
        final String mappedType = PERMISSION_TYPE_MAPPINGS.get(permissionType.toUpperCase());
        if (mappedType != null) {
            return mappedType;
        }
        Logger.debug(this, "Unknown permission type: " + permissionType);
        return permissionType.toUpperCase();
    }

    /**
     * Returns all available permission scopes that can be assigned.
     * These represent the different asset types that support permissions.
     * 
     * @return Set of permission scope names (e.g., "INDIVIDUAL", "HOST", "FOLDER")
     */
    public Set<String> getAvailablePermissionScopes() {
        return new HashSet<>(PERMISSION_TYPE_MAPPINGS.values());
    }

    /**
     * Returns all available permission levels that can be assigned.
     * These represent the different types of access (READ, WRITE, etc.).
     * 
     * @return List of permission level names
     */
    public List<String> getAvailablePermissionLevels() {
        return convertBitsToPermissionNames(
            PermissionAPI.PERMISSION_READ | 
            PermissionAPI.PERMISSION_WRITE |
            PermissionAPI.PERMISSION_PUBLISH |
            PermissionAPI.PERMISSION_EDIT_PERMISSIONS |
            PermissionAPI.PERMISSION_CAN_ADD_CHILDREN
        );
    }

    public List<String> convertBitsToPermissionNames(final int permissionBits) {
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
}