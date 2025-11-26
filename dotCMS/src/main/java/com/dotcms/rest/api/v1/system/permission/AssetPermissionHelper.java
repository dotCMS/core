package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rest.exception.ConflictException;
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
import java.util.Collections;
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
     * <p>Note: Host extends Contentlet but doesn't override getPermissionType(),
     * so we must check instanceof Host explicitly to return "HOST" instead of "CONTENT".
     *
     * @param asset The permissionable asset
     * @return Asset type enum constant (e.g., "FOLDER", "HOST", "CONTENT")
     */
    private String getAssetType(final Permissionable asset) {
        if (asset == null) {
            return StringPool.BLANK;
        }

        // Special case: Host extends Contentlet but should return "HOST"
        // Host.getPermissionType() returns Contentlet.class which maps to "CONTENT"
        if (asset instanceof Host) {
            return "HOST";
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
     * Builds Permission objects from the asset permission update form.
     *
     * <p>Follows the RoleAjax pattern with hybrid semantics:
     * <ul>
     *   <li>Scope/individual present with values → set those permissions</li>
     *   <li>Scope/individual present with empty array → save bits=0 → triggers delete</li>
     *   <li>Scope/individual absent (null) → not in save list → preserved (untouched)</li>
     * </ul>
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
            // null = omit (preserve), empty = remove, values = set
            if (roleForm.getIndividual() != null) {
                final int permissionBits = roleForm.getIndividual().isEmpty()
                    ? 0  // Empty array = remove (bits=0 triggers delete)
                    : convertPermissionNamesToBits(roleForm.getIndividual());
                permissions.add(new Permission(
                    PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                    assetPermissionId,
                    roleId,
                    permissionBits,
                    true
                ));
            }

            // Build inheritable permissions (only for parent permissionables)
            // null map = omit all (preserve), present map with scopes = process each
            if (asset.isParentPermissionable() && roleForm.getInheritable() != null) {
                for (final Map.Entry<String, List<String>> entry : roleForm.getInheritable().entrySet()) {
                    final String scopeName = entry.getKey();
                    final List<String> scopePermissions = entry.getValue();

                    // null value for a scope key = skip (preserve)
                    // empty array = remove (bits=0)
                    // values = set
                    if (scopePermissions == null) {
                        continue;
                    }

                    final String permissionType = convertScopeToPermissionType(scopeName);
                    final int permissionBits = scopePermissions.isEmpty()
                        ? 0  // Empty array = remove (bits=0 triggers delete)
                        : convertPermissionNamesToBits(scopePermissions);

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

    // ========================================================================
    // RESET ASSET PERMISSIONS METHODS
    // ========================================================================

    /**
     * Resets permissions for an asset to inherit from its parent.
     * Removes all individual permissions from the asset, making it inherit permissions.
     *
     * @param assetId Asset identifier (inode or identifier)
     * @param user    Requesting user (must be admin)
     * @return Response map containing message, assetId, and previousPermissionCount
     * @throws DotDataException     If there's an error accessing data
     * @throws DotSecurityException If security validation fails
     * @throws NotFoundInDbException If asset is not found
     * @throws ConflictException    If asset already inherits permissions (409)
     */
    public Map<String, Object> resetAssetPermissions(final String assetId, final User user)
            throws DotDataException, DotSecurityException {

        Logger.debug(this, () -> String.format(
            "resetAssetPermissions - assetId: %s, user: %s", assetId, user.getUserId()));

        // 1. Validate asset ID
        if (!UtilMethods.isSet(assetId)) {
            throw new IllegalArgumentException("Asset ID is required");
        }

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

        // 4. Check if asset is already inheriting - return 409 Conflict
        if (permissionAPI.isInheritingPermissions(asset)) {
            Logger.debug(this, () -> String.format(
                "Asset already inherits permissions: %s", assetId));
            throw new ConflictException("Asset already inherits permissions from parent");
        }

        // 5. Get count of individual permissions before removal
        final List<Permission> individualPermissions = permissionAPI.getPermissions(asset, true, true);
        final int previousPermissionCount = individualPermissions.size();

        Logger.debug(this, () -> String.format(
            "Removing %d individual permissions from asset: %s", previousPermissionCount, assetId));

        // 6. Remove all individual permissions - asset will now inherit
        permissionAPI.removePermissions(asset);

        // 7. Build and return response
        Logger.info(this, () -> String.format(
            "Successfully reset permissions for asset: %s (removed %d permissions)",
            assetId, previousPermissionCount));

        final Map<String, Object> response = new HashMap<>();
        response.put("message", "Individual permissions removed. Asset now inherits from parent.");
        response.put("assetId", assetId);
        response.put("previousPermissionCount", previousPermissionCount);

        return response;
    }

    // ========================================================================
    // UPDATE ROLE PERMISSIONS METHODS
    // ========================================================================

    /**
     * Updates permissions for a specific role on an asset.
     * Automatically breaks inheritance if the asset is currently inheriting.
     *
     * <p>Request semantics:
     * <ul>
     *   <li>PUT replaces all permissions for this role on the asset</li>
     *   <li>Omitting a scope preserves existing permissions for that scope (hybrid model)</li>
     *   <li>Empty array [] removes permissions for that scope</li>
     *   <li>Implicit inheritance break if asset currently inherits</li>
     * </ul>
     *
     * @param roleId   Role identifier
     * @param assetId  Asset identifier (Host ID, Host Name, or Folder ID)
     * @param form     Permission update form with scope-to-permissions map
     * @param cascade  If true, triggers async cascade job (query parameter)
     * @param user     Requesting user (must be admin)
     * @return Response map containing roleId, roleName, and asset with updated permissions
     * @throws DotDataException     If there's an error accessing data
     * @throws DotSecurityException If security validation fails
     */
    public Map<String, Object> updateRolePermissions(final String roleId,
                                                      final String assetId,
                                                      final UpdateRolePermissionsForm form,
                                                      final boolean cascade,
                                                      final User user)
            throws DotDataException, DotSecurityException {

        Logger.debug(this, () -> String.format(
            "updateRolePermissions - roleId: %s, assetId: %s, cascade: %s, user: %s",
            roleId, assetId, cascade, user.getUserId()));

        // 1. Validate inputs
        validateRolePermissionRequest(roleId, assetId, form);

        // 2. Load and validate role
        final Role role = roleAPI.loadRoleById(roleId);
        if (role == null) {
            throw new NotFoundInDbException(String.format("Role not found: %s", roleId));
        }

        // 3. Resolve asset (Host ID, Host Name, or Folder ID)
        final Permissionable asset = resolveHostOrFolder(assetId);
        if (asset == null) {
            throw new NotFoundInDbException(String.format("Asset not found: %s", assetId));
        }

        // 4. Check user has EDIT_PERMISSIONS on asset
        final boolean canEditPermissions = permissionAPI.doesUserHavePermission(
            asset, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user, false);
        if (!canEditPermissions) {
            throw new DotSecurityException(String.format(
                "User does not have EDIT_PERMISSIONS permission on asset: %s", assetId));
        }

        final User systemUser = APILocator.getUserAPI().getSystemUser();

        // 5. Break inheritance if currently inheriting (for this role only)
        if (permissionAPI.isInheritingPermissions(asset)) {
            Logger.debug(this, () -> String.format(
                "Breaking permission inheritance for asset: %s, role: %s", assetId, roleId));
            final Permissionable parent = permissionAPI.findParentPermissionable(asset);
            if (parent != null) {
                permissionAPI.permissionIndividuallyByRole(parent, asset, systemUser, role);
            }
        }

        // 6. Build permissions from form (hybrid semantics: omit=preserve, empty=remove)
        // Following RoleAjax pattern - no reading existing, just build from form
        final List<Permission> permissionsToSave = buildRolePermissionsFromForm(form, asset, role);

        // 7. Save just THIS role's permissions using the modern save() method
        // save() upserts each permission by (inode, roleId, type) key, preserving:
        // - Other roles' permissions (different roleId)
        // - This role's scopes not in the form (different type)
        if (!permissionsToSave.isEmpty()) {
            permissionAPI.save(permissionsToSave, asset, systemUser, false);
        }

        // 8. Handle cascade if requested and asset is a parent permissionable
        if (cascade && asset.isParentPermissionable()) {
            Logger.info(this, () -> String.format(
                "Triggering cascade permissions job for asset: %s, role: %s", assetId, roleId));
            CascadePermissionsJob.triggerJobImmediately(asset, role);
        }

        // 9. Build and return response
        Logger.info(this, () -> String.format(
            "Successfully updated permissions for role: %s on asset: %s", roleId, assetId));

        return buildRolePermissionUpdateResponse(asset, role, user);
    }

    /**
     * Resolves an asset by Host ID, Host Name, or Folder ID.
     * Tries Host first (by ID then by name), then Folder.
     *
     * @param assetId Asset identifier (Host ID, Host Name, or Folder ID)
     * @return Permissionable asset (Host or Folder) or null if not found
     * @throws DotDataException If there's an error accessing data
     * @throws DotSecurityException If security validation fails
     */
    private Permissionable resolveHostOrFolder(final String assetId)
            throws DotDataException, DotSecurityException {

        if (!UtilMethods.isSet(assetId)) {
            return null;
        }

        final User systemUser = APILocator.getUserAPI().getSystemUser();
        final boolean respectFrontendRoles = false;

        Logger.debug(this, () -> String.format("Resolving host or folder by ID: %s", assetId));

        // Try Host by ID first
        try {
            final Host host = hostAPI.find(assetId, systemUser, respectFrontendRoles);
            if (host != null && UtilMethods.isSet(host.getIdentifier())) {
                Logger.debug(this, () -> String.format("Resolved as Host by ID: %s", assetId));
                return host;
            }
        } catch (Exception e) {
            Logger.debug(this, () -> String.format("Not a host by ID: %s", assetId));
        }

        // Try Host by name
        try {
            final Host host = hostAPI.findByName(assetId, systemUser, respectFrontendRoles);
            if (host != null && UtilMethods.isSet(host.getIdentifier())) {
                Logger.debug(this, () -> String.format("Resolved as Host by name: %s", assetId));
                return host;
            }
        } catch (Exception e) {
            Logger.debug(this, () -> String.format("Not a host by name: %s", assetId));
        }

        // Try Folder
        try {
            final Folder folder = folderAPI.find(assetId, systemUser, respectFrontendRoles);
            if (UtilMethods.isSet(() -> folder.getIdentifier())) {
                Logger.debug(this, () -> String.format("Resolved as Folder: %s", assetId));
                return folder;
            }
        } catch (Exception e) {
            Logger.debug(this, () -> String.format("Not a folder: %s", assetId));
        }

        Logger.warn(this, String.format("Unable to resolve host or folder: %s", assetId));
        return null;
    }

    /**
     * Validates the role permission update request.
     *
     * @param roleId  Role identifier
     * @param assetId Asset identifier
     * @param form    Permission update form
     * @throws IllegalArgumentException If validation fails
     */
    private void validateRolePermissionRequest(final String roleId,
                                                final String assetId,
                                                final UpdateRolePermissionsForm form) {

        if (!UtilMethods.isSet(roleId)) {
            throw new IllegalArgumentException("Role ID is required");
        }

        if (!UtilMethods.isSet(assetId)) {
            throw new IllegalArgumentException("Asset ID is required");
        }

        if (form == null || form.getPermissions() == null || form.getPermissions().isEmpty()) {
            throw new IllegalArgumentException("permissions cannot be empty");
        }

        // Validate scopes and permission levels
        for (final Map.Entry<String, List<String>> entry : form.getPermissions().entrySet()) {
            final String scope = entry.getKey();
            if (!PermissionConversionUtils.isValidScope(scope)) {
                throw new IllegalArgumentException(String.format(
                    "Invalid permission scope: %s", scope));
            }

            final List<String> levels = entry.getValue();
            if (levels != null) {
                for (final String level : levels) {
                    if (!PermissionConversionUtils.isValidPermissionLevel(level)) {
                        throw new IllegalArgumentException(String.format(
                            "Invalid permission level '%s' in scope '%s'", level, scope));
                    }
                }
            }
        }
    }

    /**
     * Builds Permission objects from the role permission update form.
     *
     * <p>Follows the RoleAjax pattern - does NOT read existing permissions.
     * Hybrid semantics work implicitly via assignPermissions() behavior:
     * <ul>
     *   <li>Scopes in form with values → set those permissions</li>
     *   <li>Scopes in form with empty array → save bits=0 → triggers delete</li>
     *   <li>Scopes NOT in form → not in save list → preserved (untouched)</li>
     * </ul>
     *
     * @param form  Permission update form
     * @param asset Target asset
     * @param role  Target role
     * @return List of Permission objects to save
     */
    private List<Permission> buildRolePermissionsFromForm(final UpdateRolePermissionsForm form,
                                                           final Permissionable asset,
                                                           final Role role) {

        final List<Permission> permissions = new ArrayList<>();
        final String assetPermissionId = asset.getPermissionId();
        final String roleId = role.getId();

        // Process ONLY what's in the form - no reading existing!
        // Scopes not in the form are preserved implicitly (assignPermissions doesn't delete them)
        for (final Map.Entry<String, List<String>> entry : form.getPermissions().entrySet()) {
            final String scopeName = entry.getKey();
            final List<String> scopePermissions = entry.getValue();
            final String permissionType = PermissionConversionUtils.convertScopeToPermissionType(scopeName);

            // Empty array = remove (bits=0 triggers delete in persistPermission)
            // Non-empty = set those permissions
            final int permissionBits = (scopePermissions == null || scopePermissions.isEmpty())
                ? 0
                : PermissionConversionUtils.convertPermissionNamesToBits(scopePermissions);

            permissions.add(new Permission(
                permissionType,
                assetPermissionId,
                roleId,
                permissionBits,
                true  // isBitPermission
            ));
        }

        return permissions;
    }

    /**
     * Builds the response map for the role permission update operation.
     *
     * @param asset Updated asset
     * @param role  Updated role
     * @param user  Requesting user
     * @return Response map with roleId, roleName, and asset data
     * @throws DotDataException     If there's an error accessing data
     * @throws DotSecurityException If security validation fails
     */
    private Map<String, Object> buildRolePermissionUpdateResponse(final Permissionable asset,
                                                                    final Role role,
                                                                    final User user)
            throws DotDataException, DotSecurityException {

        final Map<String, Object> response = new HashMap<>();
        response.put("roleId", role.getId());
        response.put("roleName", role.getName());

        // Build asset object with permissions for this role only
        final Map<String, Object> assetData = buildAssetWithRolePermissions(asset, role, user);
        response.put("asset", assetData);

        return response;
    }

    /**
     * Builds asset data map with permissions filtered to a specific role.
     *
     * @param asset Target asset
     * @param role  Target role
     * @param user  Requesting user
     * @return Asset data map with id, type, name, path, hostId, and permissions
     * @throws DotDataException     If there's an error accessing data
     * @throws DotSecurityException If security validation fails
     */
    private Map<String, Object> buildAssetWithRolePermissions(final Permissionable asset,
                                                               final Role role,
                                                               final User user)
            throws DotDataException, DotSecurityException {

        final Map<String, Object> assetData = new HashMap<>();

        // Basic asset info
        assetData.put("id", asset.getPermissionId());
        assetData.put("type", getAssetType(asset));

        // Add name and path based on asset type
        if (asset instanceof Host) {
            final Host host = (Host) asset;
            assetData.put("name", host.getHostname());
            assetData.put("path", "/" + host.getHostname());
            assetData.put("hostId", host.getIdentifier());
        } else if (asset instanceof Folder) {
            final Folder folder = (Folder) asset;
            assetData.put("name", folder.getName());
            assetData.put("path", APILocator.getIdentifierAPI().find(folder.getIdentifier()).getPath());
            assetData.put("hostId", folder.getHostId());
        }

        // Permission metadata
        assetData.put("canEditPermissions", permissionAPI.doesUserHavePermission(
            asset, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user, false));
        assetData.put("inheritsPermissions", permissionAPI.isInheritingPermissions(asset));

        // Get permissions for this role and build permission map
        // We need BOTH individual permissions (INDIVIDUAL scope) AND inheritable permissions
        // (CONTENT, FOLDER, etc.) stored on this asset. The getPermissions() method only returns
        // individual permissions for this asset, so we also need getInheritablePermissions().
        final List<Permission> individualPermissions = permissionAPI.getPermissions(asset, true);
        final List<Permission> inheritablePermissions = asset.isParentPermissionable()
            ? permissionAPI.getInheritablePermissions(asset, true)
            : Collections.emptyList();

        // Combine both lists and filter to this role only
        final List<Permission> allPermissions = new ArrayList<>(individualPermissions);
        allPermissions.addAll(inheritablePermissions);
        final List<Permission> rolePermissions = allPermissions.stream()
            .filter(p -> p.getRoleId().equals(role.getId()))
            .collect(Collectors.toList());

        final Map<String, List<String>> permissionMap = new LinkedHashMap<>();
        for (final Permission permission : rolePermissions) {
            final String modernType = PermissionConversionUtils.getModernPermissionType(permission.getType());
            final List<String> permissionNames = PermissionConversionUtils.convertBitsToPermissionNames(
                permission.getPermission());
            if (!permissionNames.isEmpty()) {
                permissionMap.put(modernType, permissionNames);
            }
        }

        assetData.put("permissions", permissionMap);

        return assetData;
    }
}
