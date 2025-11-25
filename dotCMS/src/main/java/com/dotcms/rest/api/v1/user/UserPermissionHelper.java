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
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
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
    public List<Map<String, Object>> buildUserPermissionResponse(final Role role, final User requestingUser) 
            throws DotDataException, DotSecurityException {

        final User systemUser = userAPI.getSystemUser();
        final Host systemHost = APILocator.systemHost();
        final boolean respectFrontendRoles = false;

        final Set<Permissionable> permissionAssets = new HashSet<>();
        final Map<String, List<Permission>> permissionsByInode = new HashMap<>();

        final List<Permission> permissions = permissionAPI.getPermissionsByRole(role, true, true);

        collectPermissionAssets(permissions, systemUser, respectFrontendRoles, 
                               permissionAssets, permissionsByInode);

        final List<Map<String, Object>> result = new ArrayList<>();
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
    private Map<String, Object> buildAssetResponse(final Permissionable asset, 
                                                   final List<Permission> permissions,
                                                   final User requestingUser,
                                                   final User systemUser) 
            throws DotDataException, DotSecurityException {

        final Map<String, Object> response = new HashMap<>();
        
        if (asset instanceof Host) {
            final Host host = (Host) asset;
            response.put("id", host.getIdentifier());
            response.put("type", "HOST");
            response.put("name", host.getHostname());
            response.put("path", "/" + host.getHostname());
            response.put("hostId", host.getIdentifier());
        } else if (asset instanceof Folder) {
            final Folder folder = (Folder) asset;
            final Identifier id = APILocator.getIdentifierAPI().find(folder.getIdentifier());
            final Host host = hostAPI.find(folder.getHostId(), systemUser, false);
            
            response.put("id", folder.getInode());
            response.put("type", "FOLDER");
            response.put("name", folder.getName());
            response.put("path", "/" + host.getHostname() + id.getParentPath() + folder.getName());
            response.put("hostId", folder.getHostId());
        }

        response.put("canEditPermissions", 
            permissionAPI.doesUserHavePermission(
                asset, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, requestingUser, false));
                
        response.put("inheritsPermissions",
            permissionAPI.isInheritingPermissions(asset));

        response.put("permissions", buildPermissionMap(permissions));

        return response;
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
     * Gets the modern API type name for a permission type.
     * Delegates to {@link com.dotcms.rest.api.v1.system.permission.PermissionConversionUtils}.
     *
     * @param permissionType Internal permission type (class name or scope)
     * @return Modern API type constant
     */
    private String getModernPermissionType(final String permissionType) {
        return com.dotcms.rest.api.v1.system.permission.PermissionConversionUtils
            .getModernPermissionType(permissionType);
    }

    /**
     * Converts permission bits to permission level names.
     * Delegates to {@link com.dotcms.rest.api.v1.system.permission.PermissionConversionUtils}.
     *
     * @param permissionBits Bit-packed permission value
     * @return List of permission level strings
     */
    private List<String> convertBitsToPermissionNames(final int permissionBits) {
        return com.dotcms.rest.api.v1.system.permission.PermissionConversionUtils
            .convertBitsToPermissionNames(permissionBits);
    }
}