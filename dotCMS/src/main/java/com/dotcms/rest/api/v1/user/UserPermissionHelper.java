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
 * Helper for building user permission responses for REST endpoints.
 * Provides modern REST-compliant permission data transformation.
 */
@ApplicationScoped
public class UserPermissionHelper {

    private final PermissionAPI permissionAPI;
    private final HostAPI hostAPI;
    private final FolderAPI folderAPI;
    private final UserAPI userAPI;

    /**
     * Default constructor for CDI.
     */
    public UserPermissionHelper() {
        this(APILocator.getPermissionAPI(),
             APILocator.getHostAPI(),
             APILocator.getFolderAPI(),
             APILocator.getUserAPI());
    }

    /**
     * Constructor with dependency injection.
     */
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
     * Builds modern REST response for user permissions
     */
    public List<Map<String, Object>> buildUserPermissionResponse(Role role, User requestingUser) 
            throws DotDataException, DotSecurityException {

        final User systemUser = userAPI.getSystemUser();
        final Host systemHost = APILocator.systemHost();
        final boolean respectFrontendRoles = false;

        final Set<Permissionable> permAssets = new HashSet<>();
        final Map<String, List<Permission>> permByInode = new HashMap<>();

        final List<Permission> perms = permissionAPI.getPermissionsByRole(role, true, true);

        // Group permissions and collect assets
        for (Permission p : perms) {
            permByInode.computeIfAbsent(p.getInode(), k -> new ArrayList<>()).add(p);

            final Folder folder = folderAPI.find(p.getInode(), systemUser, respectFrontendRoles);

            if (folder != null && UtilMethods.isSet(folder.getIdentifier())) {
                permAssets.add(folder);
            } else {
                final Host host = hostAPI.find(p.getInode(), systemUser, respectFrontendRoles);
                if (host != null) {
                    permAssets.add(host);
                }
            }
        }

        final List<Map<String, Object>> result = new ArrayList<>();
        boolean systemHostInList = false;

        // Process assets and build REST response
        for (Permissionable asset : permAssets) {
            if (asset instanceof Host && ((Host)asset).isSystemHost()) {
                systemHostInList = true;
            }

            final String assetId = getAssetId(asset);
            final List<Permission> permissions = permByInode.get(assetId);
            result.add(buildAssetResponse(asset, permissions, requestingUser));
        }

        // Always include system host
        if (!systemHostInList) {
            result.add(buildAssetResponse(systemHost, new ArrayList<>(), requestingUser));
        }

        return result;
    }

    /**
     * Gets the asset ID for permission lookup
     */
    private String getAssetId(Permissionable asset) {
        if (asset instanceof Host) {
            return ((Host) asset).getIdentifier();
        } else if (asset instanceof Folder) {
            return ((Folder) asset).getInode();
        }
        return "";
    }

    /**
     * Builds modern REST response for a single asset
     */
    private Map<String, Object> buildAssetResponse(Permissionable asset, 
                                                   List<Permission> permissions,
                                                   User requestingUser) 
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
            final Host host = hostAPI.find(folder.getHostId(), userAPI.getSystemUser(), false);
            
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
     * Builds modern permission map with REST-standard format
     */
    public Map<String, List<String>> buildPermissionMap(List<Permission> permissions) {
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
                            Collectors.toSet(),  // Collect as Set first (no duplicates)
                            ArrayList::new        // Convert to List for JSON
                        ))
                )
            ));
    }

    /**
     * Maps permission type to modern REST enum-style names
     */
    private String getModernPermissionType(String permissionType) {
        if (PermissionAPI.INDIVIDUAL_PERMISSION_TYPE.equals(permissionType)) {
            return "INDIVIDUAL";
        }
        if (IHTMLPage.class.getCanonicalName().equals(permissionType)) {
            return "PAGE";
        }
        if (Container.class.getCanonicalName().equals(permissionType)) {
            return "CONTAINER";
        }
        if (Folder.class.getCanonicalName().equals(permissionType)) {
            return "FOLDER";
        }
        if (Link.class.getCanonicalName().equals(permissionType)) {
            return "LINK";
        }
        if (Template.class.getCanonicalName().equals(permissionType)) {
            return "TEMPLATE";
        }
        if (TemplateLayout.class.getCanonicalName().equals(permissionType)) {
            return "TEMPLATE_LAYOUT";
        }
        if (Structure.class.getCanonicalName().equals(permissionType)) {
            return "STRUCTURE";
        }
        if (Contentlet.class.getCanonicalName().equals(permissionType)) {
            return "CONTENT";
        }
        if (Category.class.getCanonicalName().equals(permissionType)) {
            return "CATEGORY";
        }
        if (Rule.class.getCanonicalName().equals(permissionType)) {
            return "RULE";
        }
        if (Host.class.getCanonicalName().equals(permissionType)) {
            return "HOST";
        }

        Logger.debug(this, "Unknown permission type: " + permissionType);
        return permissionType.toUpperCase();
    }

    /**
     * Converts permission bits to canonical permission names (READ, WRITE, PUBLISH, EDIT_PERMISSIONS)
     * Avoids duplicate aliases like USE/READ or EDIT/WRITE
     */
    private List<String> convertBitsToPermissionNames(int permissionBits) {
        final List<String> permissions = new ArrayList<>();

        // Check each permission bit using canonical names only
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