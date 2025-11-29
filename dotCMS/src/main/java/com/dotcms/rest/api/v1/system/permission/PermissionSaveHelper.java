package com.dotcms.rest.api.v1.system.permission;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
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
import com.dotmarketing.quartz.job.CascadePermissionsJob;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Helper for saving user permissions on assets.
 * Handles permission conversion, asset resolution, and save operations.
 *
 * @author dotCMS
 * @since 24.01
 */
@ApplicationScoped
public class PermissionSaveHelper {

    /**
     * Maps permission names to their corresponding permission bits.
     * Uses functional approach for clean mapping of permission names to bit operations.
     */
    private static final Map<String, Function<Integer, Integer>> PERMISSION_MAPPERS = Map.of(
        "READ", bits -> bits | PermissionAPI.PERMISSION_READ,
        "WRITE", bits -> bits | PermissionAPI.PERMISSION_WRITE,
        "PUBLISH", bits -> bits | PermissionAPI.PERMISSION_PUBLISH,
        "EDIT_PERMISSIONS", bits -> bits | PermissionAPI.PERMISSION_EDIT_PERMISSIONS,
        "CAN_ADD_CHILDREN", bits -> bits | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN
    );

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

    /**
     * Maps from API scope names (UPPERCASE) to Permission type strings.
     * Reverse mapping of PERMISSION_TYPE_MAPPINGS.
     * Based on RoleAjax.saveRolePermission() logic.
     */
    private static final Map<String, String> SCOPE_TO_PERMISSION_TYPE = Map.ofEntries(
        Map.entry("INDIVIDUAL", PermissionAPI.INDIVIDUAL_PERMISSION_TYPE),
        Map.entry("HOST", Host.class.getCanonicalName()),
        Map.entry("FOLDER", Folder.class.getCanonicalName()),
        Map.entry("CONTAINER", Container.class.getCanonicalName()),
        Map.entry("TEMPLATE", Template.class.getCanonicalName()),
        Map.entry("TEMPLATE_LAYOUT", TemplateLayout.class.getCanonicalName()),
        Map.entry("LINK", Link.class.getCanonicalName()),
        Map.entry("CONTENT", Contentlet.class.getCanonicalName()),
        Map.entry("PAGE", IHTMLPage.class.getCanonicalName()),
        Map.entry("STRUCTURE", Structure.class.getCanonicalName()),
        Map.entry("CATEGORY", Category.class.getCanonicalName()),
        Map.entry("RULE", Rule.class.getCanonicalName())
    );

    private final PermissionAPI permissionAPI;
    private final HostAPI hostAPI;
    private final FolderAPI folderAPI;
    private final UserAPI userAPI;

    /**
     * Default constructor using APILocator.
     */
    public PermissionSaveHelper() {
        this(APILocator.getPermissionAPI(),
             APILocator.getHostAPI(),
             APILocator.getFolderAPI(),
             APILocator.getUserAPI());
    }

    /**
     * CDI constructor with injected dependencies.
     *
     * @param permissionAPI Permission API instance
     * @param hostAPI Host API instance
     * @param folderAPI Folder API instance
     * @param userAPI User API instance
     */
    @Inject
    public PermissionSaveHelper(final PermissionAPI permissionAPI,
                                @Named("HostAPI") final HostAPI hostAPI,
                                final FolderAPI folderAPI,
                                final UserAPI userAPI) {
        this.permissionAPI = permissionAPI;
        this.hostAPI = hostAPI;
        this.folderAPI = folderAPI;
        this.userAPI = userAPI;
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
     * @return Set of permission level names
     */
    public Set<String> getAvailablePermissionLevels() {
        return new HashSet<>(convertBitsToPermissionNames(
            PermissionAPI.PERMISSION_READ |
            PermissionAPI.PERMISSION_WRITE |
            PermissionAPI.PERMISSION_PUBLISH |
            PermissionAPI.PERMISSION_EDIT_PERMISSIONS |
            PermissionAPI.PERMISSION_CAN_ADD_CHILDREN
        ));
    }

    /**
     * Converts permission bit mask to list of permission names.
     *
     * @param permissionBits The permission bit mask
     * @return List of permission names (READ, WRITE, PUBLISH, EDIT_PERMISSIONS, CAN_ADD_CHILDREN)
     */
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

    /**
     * Converts permission level names to permission bit mask.
     * Inverse operation of convertBitsToPermissionNames().
     *
     * @param permissionNames Collection of permission names (READ, WRITE, PUBLISH, EDIT_PERMISSIONS, CAN_ADD_CHILDREN)
     * @return Combined permission bit mask
     */
    public int convertPermissionNamesToBits(final Collection<String> permissionNames) {
        if (permissionNames == null || permissionNames.isEmpty()) {
            return 0;
        }

        int permissionBits = 0;

        for (final String permissionName : permissionNames) {
            final String upperName = permissionName.toUpperCase();
            final Function<Integer, Integer> mapper = PERMISSION_MAPPERS.get(upperName);

            if (mapper != null) {
                permissionBits = mapper.apply(permissionBits);
            } else {
                Logger.warn(this, "Unknown permission name: " + permissionName);
            }
        }

        return permissionBits;
    }

    /**
     * Gets the Permission type string for a given scope name.
     * Maps from REST API scope names (UPPERCASE) to Permission type class names.
     *
     * @param scopeName The scope name from API (UPPERCASE like "HOST", "FOLDER", "INDIVIDUAL")
     * @return Permission type class name or INDIVIDUAL constant
     */
    public String getPermissionTypeForScope(final String scopeName) {
        final String permissionType = SCOPE_TO_PERMISSION_TYPE.get(scopeName.toUpperCase());
        if (permissionType == null) {
            Logger.warn(this, "Unknown permission scope: " + scopeName);
            return scopeName;
        }
        return permissionType;
    }

    /**
     * Resolves asset (Host or Folder) from asset ID.
     * Replicates RoleAjax.saveRolePermission() logic (lines 815-820).
     *
     * @param assetId The asset identifier (host identifier or folder inode)
     * @param user User for lookups
     * @return The resolved Permissionable asset
     * @throws DotDataException if asset resolution fails
     * @throws DotSecurityException if security check fails
     */
    public Permissionable resolveAsset(final String assetId, final User user)
            throws DotDataException, DotSecurityException {

        Logger.debug(this, () -> "Resolving asset: " + assetId);

        // Try host first (line 815)
        final Host host = hostAPI.find(assetId, user, false);
        if (host != null) {
            Logger.debug(this, () -> "Asset resolved as Host: " + host.getHostname());
            return host;
        }

        // Try folder (lines 817-819)
        final Folder folder = folderAPI.find(assetId, user, false);
        if (folder != null && UtilMethods.isSet(() -> folder.getIdentifier())) {
            Logger.debug(this, () -> "Asset resolved as Folder: " + folder.getName());
            return folder;
        }

        Logger.warn(this, "Asset not found: " + assetId);
        throw new NotFoundException("Asset not found: " + assetId);
    }

    /**
     * Saves permissions for a user's individual role on a specific asset.
     * Replicates RoleAjax.saveRolePermission() functionality (lines 801-899).
     *
     * CRITICAL: This method does NOT use @WrapInTransaction because:
     * - assignPermissions() is already @WrapInTransaction (PermissionBitAPIImpl:765)
     * - permissionIndividuallyByRole() is already @WrapInTransaction (PermissionBitAPIImpl:1691)
     *
     * @param userId User identifier (email or ID)
     * @param assetId Asset identifier (host or folder)
     * @param form Permission save form
     * @param requestingUser User making the request
     * @return Save response with updated asset
     * @throws DotDataException if data access fails
     * @throws DotSecurityException if security check fails
     */
    public SaveUserPermissionsView saveUserPermissions(
            final String userId,
            final String assetId,
            final SaveUserPermissionsForm form,
            final User requestingUser) throws DotDataException, DotSecurityException {

        Logger.info(this, () -> String.format("Applying permissions for user %s on asset %s", userId, assetId));

        final User systemUser = userAPI.getSystemUser();
        final boolean respectFrontendRoles = false;
        final RoleAPI roleAPI = APILocator.getRoleAPI();

        // Load user and get individual role
        final User targetUser = userAPI.loadUserById(userId);
        if (targetUser == null) {
            throw new NotFoundException("User not found: " + userId);
        }

        final Role userRole = roleAPI.getUserRole(targetUser);
        if (userRole == null) {
            throw new DotDataException("User role not found for: " + userId);
        }

        final Permissionable asset = resolveAsset(assetId, systemUser);

        if (permissionAPI.isInheritingPermissions(asset)) {
            Logger.debug(this, () -> "Breaking permission inheritance for asset: " + assetId);
            final Permissionable parentPermissionable = permissionAPI.findParentPermissionable(asset);
            permissionAPI.permissionIndividuallyByRole(parentPermissionable, asset, systemUser, userRole);
        }

        final List<Permission> permissionsToSave = new ArrayList<>();
        final Map<String, Set<String>> permissionMap = form.getPermissions();

        for (final Map.Entry<String, Set<String>> entry : permissionMap.entrySet()) {
            final String scope = entry.getKey();
            final Set<String> levels = entry.getValue();

            if (levels == null || levels.isEmpty()) {
                continue;
            }

            final int permissionBits = convertPermissionNamesToBits(levels);
            if (permissionBits == 0) {
                continue;
            }

            final String permissionType = getPermissionTypeForScope(scope);
            final Permission permission = new Permission(
                permissionType,
                asset.getPermissionId(),
                userRole.getId(),
                permissionBits,
                true
            );
            permissionsToSave.add(permission);

            Logger.debug(this, () -> String.format("Added permission: scope=%s, bits=%d, type=%s",
                scope, permissionBits, permissionType));
        }

        // NOTE: assignPermissions() requires non-empty list
        if (!permissionsToSave.isEmpty()) {
            Logger.debug(this, () -> String.format("Assigning %d permissions to asset %s for role %s",
                permissionsToSave.size(), assetId, userRole.getId()));
            permissionAPI.assignPermissions(permissionsToSave, asset, systemUser, respectFrontendRoles);
        } else {
            Logger.warn(this, "No permissions to save - assignPermissions throws on empty list");
            throw new BadRequestException("At least one permission must be specified");
        }

        boolean cascadeInitiated = false;
        if (form.isCascade() && asset.isParentPermissionable()) {
            Logger.info(this, () -> String.format("Cascading permissions for asset %s", assetId));
            // NOTE: Currently using legacy Quartz job pattern.
            // TODO: Migrate to new JobProcessor pattern in future (separate ticket)
            //       - Create CascadePermissionsProcessor implements JobProcessor
            //       - Use JobQueueManagerAPI.createJob("cascadePermissions", params)
            //       - Reference: ImportContentletsProcessor for implementation pattern
            //       - Coordinate with migration of RoleAjax.java:888 and PermissionAjax.java:308
            CascadePermissionsJob.triggerJobImmediately(asset, userRole);
            cascadeInitiated = true;
        }

        final List<Permission> updatedPermissions = permissionAPI.getPermissionsByRole(userRole, true, true)
            .stream()
            .filter(p -> p.getInode().equals(assetId))
            .collect(Collectors.toList());

        final UserPermissionAssetView updatedAsset = buildAssetResponse(
            asset,
            updatedPermissions,
            requestingUser,
            systemUser
        );

        Logger.info(this, () -> String.format("Successfully saved permissions for user %s on asset %s", userId, assetId));

        return new SaveUserPermissionsView(userId, userRole.getId(), updatedAsset, cascadeInitiated);
    }

    /**
     * Builds response data for a single permissionable asset.
     */
    private UserPermissionAssetView buildAssetResponse(final Permissionable asset,
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
            path = StringPool.FORWARD_SLASH + host.getHostname();
            hostId = host.getIdentifier();
        } else if (asset instanceof Folder) {
            final Folder folder = (Folder) asset;
            final Identifier identifier = APILocator.getIdentifierAPI().find(folder.getIdentifier());
            final Host host = hostAPI.find(folder.getHostId(), systemUser, false);

            id = folder.getInode();
            type = "FOLDER";
            name = folder.getName();
            path = StringPool.FORWARD_SLASH + host.getHostname() + identifier.getParentPath() + folder.getName();
            hostId = folder.getHostId();
        } else {
            throw new DotDataException("Unsupported asset type: " + asset.getClass().getName());
        }

        final boolean canEditPermissions = permissionAPI.doesUserHavePermission(
                asset, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, requestingUser, false);

        final boolean inheritsPermissions = permissionAPI.isInheritingPermissions(asset);

        final Map<String, Set<String>> permissionMap = buildPermissionMap(permissions);

        return new UserPermissionAssetView(id, type, name, path, hostId,
                                           canEditPermissions, inheritsPermissions, permissionMap);
    }

    /**
     * Groups permissions by type with permission names.
     */
    private Map<String, Set<String>> buildPermissionMap(final List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new HashMap<>();
        }

        return permissions.stream()
            .collect(Collectors.groupingBy(
                p -> getModernPermissionType(p.getType()),
                Collectors.mapping(
                    p -> convertBitsToPermissionNames(p.getPermission()),
                    Collectors.flatMapping(List::stream,
                        Collectors.toSet()
                    )
                )
            ));
    }

    /**
     * Maps permission type class names to API type constants.
     */
    private String getModernPermissionType(final String permissionType) {
        final String mappedType = PERMISSION_TYPE_MAPPINGS.get(permissionType.toUpperCase());
        if (mappedType != null) {
            return mappedType;
        }
        Logger.debug(this, "Unknown permission type: " + permissionType);
        return permissionType.toUpperCase();
    }
}
