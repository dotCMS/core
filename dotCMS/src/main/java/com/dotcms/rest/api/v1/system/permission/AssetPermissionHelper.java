package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.contenttype.exception.NotFoundInDbException;
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
import com.dotmarketing.quartz.job.CascadePermissionsJob;
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
 * @author Hassan
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
     * Builds complete permission response for an asset with pagination.
     *
     * @param assetId Asset identifier (inode or identifier)
     * @param page Page number (1-indexed)
     * @param perPage Number of roles per page
     * @param requestingUser User making the request (for permission checks)
     * @return AssetPermissionResponse containing entity and pagination at root level
     * @throws DotDataException If there's an error accessing permission data
     * @throws DotSecurityException If security validation fails
     * @throws NotFoundInDbException If asset is not found
     */
    public AssetPermissionResponse buildAssetPermissionResponse(final String assetId,
                                                                 final int page,
                                                                 final int perPage,
                                                                 final User requestingUser)
            throws DotDataException, DotSecurityException {

        Logger.debug(this, () -> String.format(
            "Building asset permission response for assetId: %s, page: %d, perPage: %d",
            assetId, page, perPage));

        final Permissionable asset = resolveAsset(assetId);
        if (asset == null) {
            Logger.warn(this, String.format("Asset not found: %s", assetId));
            throw new NotFoundInDbException(String.format("Asset not found: %s", assetId));
        }

        final User systemUser = APILocator.getUserAPI().getSystemUser();

        // Build entity data (asset metadata + permissions)
        final Map<String, Object> entity = new HashMap<>();
        entity.putAll(getAssetMetadata(asset, requestingUser, systemUser));

        // Get all permissions for this asset
        final List<Permission> permissions = permissionAPI.getPermissions(asset, true);
        Logger.debug(this, () -> String.format(
            "Retrieved %d permissions for asset: %s", permissions.size(), assetId));

        // Build and paginate role permissions
        final List<Map<String, Object>> rolePermissions = buildRolePermissions(
            permissions, asset, requestingUser);

        final PaginatedResult paginatedData = paginateRoles(rolePermissions, page, perPage);

        entity.put("permissions", paginatedData.permissions);

        Logger.info(this, () -> String.format(
            "Successfully built permission response for asset: %s", assetId));

        return new AssetPermissionResponse(entity, paginatedData.pagination);
    }

    /**
     * Resolves an asset by ID, trying multiple asset types.
     *
     * @param assetId Asset identifier (inode or identifier)
     * @return Permissionable asset or null if not found
     * @throws DotDataException If there's an error accessing data
     * @throws DotSecurityException If security validation fails
     */
    @VisibleForTesting
    protected Permissionable resolveAsset(final String assetId)
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
     * Builds asset-level metadata fields for the response.
     *
     * @param asset The permissionable asset
     * @param requestingUser User making the request
     * @param systemUser System user for permission checks
     * @return Map with asset metadata fields
     * @throws DotDataException If there's an error accessing data
     * @throws DotSecurityException If security validation fails
     */
    @VisibleForTesting
    protected Map<String, Object> getAssetMetadata(final Permissionable asset,
                                                   final User requestingUser,
                                                   final User systemUser)
            throws DotDataException, DotSecurityException {

        final Map<String, Object> metadata = new HashMap<>();

        metadata.put("assetId", asset.getPermissionId());
        metadata.put("assetType", getAssetType(asset));
        metadata.put("inheritanceMode",
            permissionAPI.isInheritingPermissions(asset) ? "INHERITED" : "INDIVIDUAL");
        metadata.put("isParentPermissionable", asset.isParentPermissionable());

        metadata.put("canEditPermissions",
            permissionAPI.doesUserHavePermission(
                asset, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, requestingUser, false));

        metadata.put("canEdit",
            permissionAPI.doesUserHavePermission(
                asset, PermissionAPI.PERMISSION_WRITE, requestingUser, false));

        // Get parent asset ID if exists
        try {
            final Permissionable parent = permissionAPI.findParentPermissionable(asset);
            if (parent != null) {
                metadata.put("parentAssetId", parent.getPermissionId());
            } else {
                metadata.put("parentAssetId", null);
            }
        } catch (Exception e) {
            Logger.debug(this, () -> "No parent permissionable found for asset");
            metadata.put("parentAssetId", null);
        }

        return metadata;
    }

    /**
     * Builds permission data grouped by role.
     *
     * @param permissions List of permissions for the asset
     * @param asset The permissionable asset
     * @param requestingUser User making the request
     * @return List of role permission maps
     * @throws DotDataException If there's an error accessing data
     */
    @VisibleForTesting
    protected List<Map<String, Object>> buildRolePermissions(final List<Permission> permissions,
                                                            final Permissionable asset,
                                                            final User requestingUser)
            throws DotDataException {

        if (permissions == null || permissions.isEmpty()) {
            Logger.debug(this, () -> "No permissions found for asset");
            return new ArrayList<>();
        }

        final boolean isInheriting = permissionAPI.isInheritingPermissions(asset);
        final boolean isParentPermissionable = asset.isParentPermissionable();

        // Group permissions by role ID
        final Map<String, List<Permission>> permissionsByRole = permissions.stream()
            .collect(Collectors.groupingBy(Permission::getRoleId, LinkedHashMap::new, Collectors.toList()));

        final List<Map<String, Object>> rolePermissions = new ArrayList<>();

        for (final Map.Entry<String, List<Permission>> entry : permissionsByRole.entrySet()) {
            final String roleId = entry.getKey();
            final List<Permission> rolePermissionList = entry.getValue();

            try {
                final Role role = roleAPI.loadRoleById(roleId);
                if (role == null) {
                    Logger.warn(this, String.format("Role not found: %s", roleId));
                    continue;
                }

                final Map<String, Object> rolePermission = new HashMap<>();
                rolePermission.put("roleId", roleId);
                rolePermission.put("roleName", role.getName());
                rolePermission.put("inherited", isInheriting);

                // Separate individual and inheritable permissions
                final List<Permission> individualPermissions = rolePermissionList.stream()
                    .filter(Permission::isIndividualPermission)
                    .collect(Collectors.toList());

                final List<Permission> inheritablePermissions = rolePermissionList.stream()
                    .filter(p -> !p.isIndividualPermission())
                    .collect(Collectors.toList());

                // Build individual permissions array
                rolePermission.put("individual",
                    convertPermissionsToStringArray(individualPermissions));

                // Build inheritable permissions map (only for parent permissionables)
                if (isParentPermissionable && !inheritablePermissions.isEmpty()) {
                    rolePermission.put("inheritable",
                        buildInheritablePermissionMap(inheritablePermissions));
                } else {
                    rolePermission.put("inheritable", null);
                }

                rolePermissions.add(rolePermission);

            } catch (DotDataException e) {
                Logger.warn(this, String.format("Error loading role: %s - %s",
                    roleId, e.getMessage()));
            }
        }

        return rolePermissions;
    }

    /**
     * Holds paginated permissions and pagination metadata.
     */
    @VisibleForTesting
    protected static class PaginatedResult {
        final List<Map<String, Object>> permissions;
        final com.dotcms.rest.Pagination pagination;

        PaginatedResult(final List<Map<String, Object>> permissions,
                       final com.dotcms.rest.Pagination pagination) {
            this.permissions = permissions;
            this.pagination = pagination;
        }
    }

    /**
     * Holds the complete response with entity and pagination at root level.
     */
    public static class AssetPermissionResponse {
        public final Map<String, Object> entity;
        public final com.dotcms.rest.Pagination pagination;

        public AssetPermissionResponse(final Map<String, Object> entity,
                                      final com.dotcms.rest.Pagination pagination) {
            this.entity = entity;
            this.pagination = pagination;
        }
    }

    /**
     * Paginates the role permissions list and builds pagination metadata.
     *
     * @param rolePermissions Complete list of role permissions
     * @param page Page number (1-indexed)
     * @param perPage Number of roles per page
     * @return PaginatedResult with permissions list and Pagination object
     */
    @VisibleForTesting
    protected PaginatedResult paginateRoles(final List<Map<String, Object>> rolePermissions,
                                           final int page,
                                           final int perPage) {

        final int totalEntries = rolePermissions.size();
        final int startIndex = (page - 1) * perPage;
        final int endIndex = Math.min(startIndex + perPage, totalEntries);

        final List<Map<String, Object>> paginatedPermissions;
        if (startIndex >= totalEntries) {
            paginatedPermissions = new ArrayList<>();
        } else {
            paginatedPermissions = rolePermissions.subList(startIndex, endIndex);
        }

        final com.dotcms.rest.Pagination pagination = new com.dotcms.rest.Pagination.Builder()
            .currentPage(page)
            .perPage(perPage)
            .totalEntries(totalEntries)
            .build();

        return new PaginatedResult(paginatedPermissions, pagination);
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
     * Delegates to {@link PermissionConversionUtils#convertBitsToPermissionNames(int)}.
     *
     * @param permissionBits Bit-packed permission value
     * @return List of permission level strings
     */
    private List<String> convertBitsToPermissionNames(final int permissionBits) {
        return PermissionConversionUtils.convertBitsToPermissionNames(permissionBits);
    }

    /**
     * Gets the modern API type name for a permission type.
     * Delegates to {@link PermissionConversionUtils#getModernPermissionType(String)}.
     *
     * @param permissionType Internal permission type (class name or scope)
     * @return Modern API type constant
     */
    private String getModernPermissionType(final String permissionType) {
        return PermissionConversionUtils.getModernPermissionType(permissionType);
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

    // ========================================================================
    // UPDATE ASSET PERMISSIONS METHODS
    // ========================================================================

    /**
     * Updates permissions for an asset based on the provided form.
     * Automatically breaks inheritance if the asset is currently inheriting.
     *
     * @param assetId  Asset identifier (inode or identifier)
     * @param form     Permission update form with role permissions
     * @param cascade  If true, triggers async cascade job (query parameter)
     * @param user     Requesting user (must be admin)
     * @return Response map containing message, permissionCount, inheritanceBroken, and asset
     * @throws DotDataException     If there's an error accessing data
     * @throws DotSecurityException If security validation fails
     */
    public Map<String, Object> updateAssetPermissions(final String assetId,
                                                       final UpdateAssetPermissionsForm form,
                                                       final boolean cascade,
                                                       final User user)
            throws DotDataException, DotSecurityException {

        Logger.debug(this, () -> String.format(
            "updateAssetPermissions - assetId: %s, cascade: %s, user: %s",
            assetId, cascade, user.getUserId()));

        // 1. Validate request
        validateUpdateRequest(assetId, form);

        // 2. Resolve asset
        final Permissionable asset = resolveAsset(assetId);
        if (asset == null) {
            throw new NotFoundInDbException("asset does not exist");
        }

        // 3. Check user has EDIT_PERMISSIONS on asset
        final boolean canEditPermissions = permissionAPI.doesUserHavePermission(
            asset, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user, false);
        if (!canEditPermissions) {
            throw new DotSecurityException(String.format(
                "User does not have EDIT_PERMISSIONS permission on asset: %s", assetId));
        }

        // 4. Check if asset is currently inheriting (for response flag)
        final boolean wasInheriting = permissionAPI.isInheritingPermissions(asset);

        // 5. Break inheritance if currently inheriting
        if (wasInheriting) {
            Logger.debug(this, () -> String.format(
                "Breaking permission inheritance for asset: %s", assetId));
            final Permissionable parent = permissionAPI.findParentPermissionable(asset);
            if (parent != null) {
                permissionAPI.permissionIndividually(parent, asset, user);
            }
        }

        // 6. Build Permission objects from form
        final List<Permission> permissionsToSave = buildPermissionsFromForm(form, asset);

        // 7. Save permissions
        if (!permissionsToSave.isEmpty()) {
            permissionAPI.save(permissionsToSave, asset, user, false);
        }

        // 8. Handle cascade if requested and asset is a parent permissionable
        if (cascade && asset.isParentPermissionable()) {
            Logger.info(this, () -> String.format(
                "Triggering cascade permissions job for asset: %s", assetId));
            // Trigger cascade for each role in the form
            for (final RolePermissionForm roleForm : form.getPermissions()) {
                try {
                    final Role role = roleAPI.loadRoleById(roleForm.getRoleId());
                    if (role != null) {
                        CascadePermissionsJob.triggerJobImmediately(asset, role);
                    }
                } catch (Exception e) {
                    Logger.warn(this, String.format(
                        "Failed to trigger cascade for role %s: %s",
                        roleForm.getRoleId(), e.getMessage()));
                }
            }
        }

        // 9. Build and return response
        Logger.info(this, () -> String.format(
            "Successfully updated permissions for asset: %s", assetId));

        return buildUpdateResponse(asset, user, wasInheriting, permissionsToSave.size());
    }

    /**
     * Validates the update request form and asset ID.
     *
     * @param assetId Asset identifier
     * @param form    Permission update form
     * @throws IllegalArgumentException If validation fails
     * @throws DotDataException         If role lookup fails
     */
    private void validateUpdateRequest(final String assetId,
                                       final UpdateAssetPermissionsForm form)
            throws DotDataException {

        if (!UtilMethods.isSet(assetId)) {
            throw new IllegalArgumentException("Asset ID is required");
        }

        if (form == null || form.getPermissions() == null || form.getPermissions().isEmpty()) {
            throw new IllegalArgumentException("permissions list is required");
        }

        for (final RolePermissionForm roleForm : form.getPermissions()) {
            // Validate role ID is provided
            if (!UtilMethods.isSet(roleForm.getRoleId())) {
                throw new IllegalArgumentException("roleId is required for each permission entry");
            }

            // Validate role exists
            final Role role = roleAPI.loadRoleById(roleForm.getRoleId());
            if (role == null) {
                throw new IllegalArgumentException(String.format(
                    "Invalid role id: %s", roleForm.getRoleId()));
            }

            // Validate individual permission names
            if (roleForm.getIndividual() != null) {
                for (final String perm : roleForm.getIndividual()) {
                    if (!PermissionConversionUtils.isValidPermissionLevel(perm)) {
                        throw new IllegalArgumentException(String.format(
                            "Invalid permission level: %s", perm));
                    }
                }
            }

            // Validate inheritable permission names and scopes
            if (roleForm.getInheritable() != null) {
                for (final Map.Entry<String, List<String>> entry : roleForm.getInheritable().entrySet()) {
                    final String scope = entry.getKey();
                    if (!PermissionConversionUtils.isValidScope(scope)) {
                        throw new IllegalArgumentException(String.format(
                            "Invalid permission scope: %s", scope));
                    }

                    if (entry.getValue() != null) {
                        for (final String perm : entry.getValue()) {
                            if (!PermissionConversionUtils.isValidPermissionLevel(perm)) {
                                throw new IllegalArgumentException(String.format(
                                    "Invalid permission level '%s' in scope '%s'", perm, scope));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Builds Permission objects from the update form.
     *
     * @param form  Permission update form
     * @param asset Target asset
     * @return List of Permission objects to save
     */
    private List<Permission> buildPermissionsFromForm(final UpdateAssetPermissionsForm form,
                                                       final Permissionable asset) {

        final List<Permission> permissions = new ArrayList<>();
        final String assetPermissionId = asset.getPermissionId();

        for (final RolePermissionForm roleForm : form.getPermissions()) {
            final String roleId = roleForm.getRoleId();

            // Build individual permissions
            if (roleForm.getIndividual() != null && !roleForm.getIndividual().isEmpty()) {
                final int permissionBits = convertPermissionNamesToBits(roleForm.getIndividual());
                permissions.add(new Permission(
                    PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                    assetPermissionId,
                    roleId,
                    permissionBits,
                    true
                ));
            }

            // Build inheritable permissions (only for parent permissionables)
            if (asset.isParentPermissionable() && roleForm.getInheritable() != null) {
                for (final Map.Entry<String, List<String>> entry : roleForm.getInheritable().entrySet()) {
                    final String scopeName = entry.getKey();
                    final List<String> scopePermissions = entry.getValue();

                    if (scopePermissions == null || scopePermissions.isEmpty()) {
                        continue;
                    }

                    final String permissionType = convertScopeToPermissionType(scopeName);
                    final int permissionBits = convertPermissionNamesToBits(scopePermissions);

                    permissions.add(new Permission(
                        permissionType,
                        assetPermissionId,
                        roleId,
                        permissionBits,
                        true
                    ));
                }
            }
        }

        return permissions;
    }

    /**
     * Converts permission level names to a bitwise permission value.
     * Delegates to {@link PermissionConversionUtils#convertPermissionNamesToBits(List)}.
     *
     * @param permissionNames List of permission level names (READ, WRITE, etc.)
     * @return Combined bit value
     */
    private int convertPermissionNamesToBits(final List<String> permissionNames) {
        return PermissionConversionUtils.convertPermissionNamesToBits(permissionNames);
    }

    /**
     * Converts an API scope name to internal permission type.
     * Delegates to {@link PermissionConversionUtils#convertScopeToPermissionType(String)}.
     *
     * @param scopeName API scope name (FOLDER, CONTENT, etc.)
     * @return Internal permission type (class canonical name)
     * @throws IllegalArgumentException If scope is unknown
     */
    private String convertScopeToPermissionType(final String scopeName) {
        return PermissionConversionUtils.convertScopeToPermissionType(scopeName);
    }

    /**
     * Builds the response map for the update operation.
     *
     * @param asset             Updated asset
     * @param user              Requesting user
     * @param inheritanceBroken Whether inheritance was broken during this operation
     * @param permissionCount   Number of permissions saved
     * @return Response map with message, permissionCount, inheritanceBroken, and asset
     * @throws DotDataException     If there's an error accessing data
     * @throws DotSecurityException If security validation fails
     */
    private Map<String, Object> buildUpdateResponse(final Permissionable asset,
                                                     final User user,
                                                     final boolean inheritanceBroken,
                                                     final int permissionCount)
            throws DotDataException, DotSecurityException {

        final Map<String, Object> response = new HashMap<>();
        response.put("message", "Permissions saved successfully");
        response.put("permissionCount", permissionCount);
        response.put("inheritanceBroken", inheritanceBroken);

        // Build asset object
        final User systemUser = APILocator.getUserAPI().getSystemUser();
        final Map<String, Object> assetData = getAssetMetadata(asset, user, systemUser);

        // Get updated permissions for the asset
        final List<Permission> permissions = permissionAPI.getPermissions(asset, true);
        final List<Map<String, Object>> rolePermissions = buildRolePermissions(permissions, asset, user);
        assetData.put("permissions", rolePermissions);

        response.put("asset", assetData);

        return response;
    }
}
