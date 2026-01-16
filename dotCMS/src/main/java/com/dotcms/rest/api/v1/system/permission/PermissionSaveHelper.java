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
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.quartz.job.CascadePermissionsJob;
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
 * Helper for saving user permissions on assets.
 * Handles permission conversion, asset resolution, and save operations.
 *
 * @author hassandotcms
 */
@ApplicationScoped
public class PermissionSaveHelper {

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
        return PermissionAPI.Scope.getAllScopeNames();
    }

    /**
     * Returns all available permission levels that can be assigned.
     * These represent the different types of access (READ, WRITE, etc.).
     *
     * @return Set of permission level names
     */
    public Set<String> getAvailablePermissionLevels() {
        return new HashSet<>(PermissionConversionUtils.convertBitsToPermissionNames(
            PermissionAPI.PERMISSION_READ |
            PermissionAPI.PERMISSION_WRITE |
            PermissionAPI.PERMISSION_PUBLISH |
            PermissionAPI.PERMISSION_EDIT_PERMISSIONS |
            PermissionAPI.PERMISSION_CAN_ADD_CHILDREN
        ));
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
        if (host != null && UtilMethods.isSet(() -> host.getIdentifier())) {
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
        final Map<String, Set<PermissionAPI.Type>> permissionMap = form.getPermissions();

        for (final Map.Entry<String, Set<PermissionAPI.Type>> entry : permissionMap.entrySet()) {
            final String scope = entry.getKey();
            final Set<PermissionAPI.Type> levels = entry.getValue();

            if (levels == null || levels.isEmpty()) {
                continue;
            }

            final int permissionBits = PermissionConversionUtils.convertTypesToBits(levels);
            if (permissionBits == 0) {
                continue;
            }

            final String permissionType = PermissionConversionUtils.convertScopeToPermissionType(scope);
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

        return SaveUserPermissionsView.builder()
            .userId(userId)
            .roleId(userRole.getId())
            .asset(updatedAsset)
            .cascadeInitiated(cascadeInitiated)
            .build();
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

        return UserPermissionAssetView.builder()
            .id(id)
            .type(type)
            .name(name)
            .path(path)
            .hostId(hostId)
            .canEditPermissions(canEditPermissions)
            .inheritsPermissions(inheritsPermissions)
            .permissions(permissionMap)
            .build();
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
                p -> PermissionConversionUtils.getModernPermissionType(p.getType()),
                Collectors.mapping(
                    p -> PermissionConversionUtils.convertBitsToPermissionNames(p.getPermission()),
                    Collectors.flatMapping(List::stream,
                        Collectors.toSet()
                    )
                )
            ));
    }

    // ========== GET User Permissions Methods ==========

    /**
     * Gets all permission assets for a user's role.
     * Returns hosts and folders that the role has direct permissions on.
     *
     * @param role The user's individual role
     * @param requestingUser The user making the request (for permission checks)
     * @return List of permission assets with their permissions
     * @throws DotDataException if data access fails
     * @throws DotSecurityException if security check fails
     */
    public List<UserPermissionAssetView> getUserPermissionAssets(final Role role, final User requestingUser)
            throws DotDataException, DotSecurityException {

        final User systemUser = userAPI.getSystemUser();
        final Host systemHost = APILocator.systemHost();
        final boolean respectFrontendRoles = false;

        final Set<Permissionable> permissionAssets = new HashSet<>();
        final Map<String, List<Permission>> permissionsByInode = new HashMap<>();

        final List<Permission> permissions = permissionAPI.getPermissionsByRole(role, true, true);

        // Collect assets from permissions
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

        final List<UserPermissionAssetView> result = new ArrayList<>();
        boolean systemHostInList = false;

        for (final Permissionable asset : permissionAssets) {
            if (asset instanceof Host && ((Host) asset).isSystemHost()) {
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
     * Gets paginated permission assets for a user's role.
     *
     * @param role The user's individual role
     * @param requestingUser The user making the request
     * @param page Page number (0-based)
     * @param perPage Number of items per page
     * @return Paginated list of permission assets
     * @throws DotDataException if data access fails
     * @throws DotSecurityException if security check fails
     */
    public List<UserPermissionAssetView> getUserPermissionAssetsPaginated(
            final Role role,
            final User requestingUser,
            final int page,
            final int perPage) throws DotDataException, DotSecurityException {

        final List<UserPermissionAssetView> allAssets = getUserPermissionAssets(role, requestingUser);

        final int totalEntries = allAssets.size();
        final int offset = page * perPage;

        if (offset >= totalEntries) {
            return new ArrayList<>();
        }

        final int endIndex = Math.min(offset + perPage, totalEntries);
        return allAssets.subList(offset, endIndex);
    }

    /**
     * Gets total count of permission assets for a role.
     * Used for pagination metadata.
     *
     * @param role The role to count assets for
     * @param requestingUser The user making the request
     * @return Total number of assets
     * @throws DotDataException if data access fails
     * @throws DotSecurityException if security check fails
     */
    public int getTotalPermissionAssetCount(final Role role, final User requestingUser)
            throws DotDataException, DotSecurityException {
        return getUserPermissionAssets(role, requestingUser).size();
    }

    /**
     * Gets the asset ID for a permissionable (host identifier or folder inode).
     */
    private String getAssetId(final Permissionable asset) {
        if (asset instanceof Host) {
            return ((Host) asset).getIdentifier();
        } else if (asset instanceof Folder) {
            return ((Folder) asset).getInode();
        }
        return StringPool.BLANK;
    }
}
