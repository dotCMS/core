package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rest.exception.BadRequestException;
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
import com.liferay.portal.model.User;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        // Host.getPermissionType() returns Contentlet's type (inherited),
        // but API should show HOST for user clarity
        if (asset instanceof Host) {
            return PermissionAPI.Scope.HOST;
        }

        return getScopeFromPermissionType(asset.getPermissionType());
    }

    // ========================================================================
    // UPDATE ASSET PERMISSIONS METHODS
    // ========================================================================

    /**
     * Validates the update request form and asset ID.
     *
     * @param assetId Asset identifier
     * @param form    Permission update form
     * @throws BadRequestException  If validation fails
     * @throws DotDataException     If role lookup fails
     */
    private void validateUpdateRequest(final String assetId,
                                       final UpdateAssetPermissionsForm form)
            throws DotDataException {

        if (!UtilMethods.isSet(assetId)) {
            throw new BadRequestException("Asset ID is required");
        }

        if (form == null || form.getPermissions() == null || form.getPermissions().isEmpty()) {
            throw new BadRequestException("permissions list is required");
        }

        for (final RolePermissionForm roleForm : form.getPermissions()) {
            // Validate role ID is provided
            if (!UtilMethods.isSet(roleForm.getRoleId())) {
                throw new BadRequestException("roleId is required for each permission entry");
            }

            // Validate role exists
            final Role role = roleAPI.loadRoleById(roleForm.getRoleId());
            if (role == null) {
                throw new BadRequestException(String.format(
                    "Invalid role id: %s", roleForm.getRoleId()));
            }

            // Individual permission validation is handled by Jackson enum deserialization
            // No manual validation needed for Set<PermissionAPI.Type>

            // Validate inheritable scope names (permission types validated by Jackson)
            if (roleForm.getInheritable() != null) {
                for (final String scope : roleForm.getInheritable().keySet()) {
                    if (!PermissionConversionUtils.isValidScope(scope)) {
                        throw new BadRequestException(String.format(
                            "Invalid permission scope: %s", scope));
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
                final int permissionBits = PermissionConversionUtils.convertTypesToBits(
                    roleForm.getIndividual());
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
                for (final Map.Entry<String, Set<PermissionAPI.Type>> entry : roleForm.getInheritable().entrySet()) {
                    final String scopeName = entry.getKey();
                    final Set<PermissionAPI.Type> scopePermissions = entry.getValue();

                    if (scopePermissions == null || scopePermissions.isEmpty()) {
                        continue;
                    }

                    final String permissionType = PermissionConversionUtils.convertScopeToPermissionType(scopeName);
                    final int permissionBits = PermissionConversionUtils.convertTypesToBits(scopePermissions);

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
     * Updates permissions for an asset based on the provided form.
     * Automatically breaks inheritance if the asset is currently inheriting.
     *
     * @param assetId  Asset identifier (inode or identifier)
     * @param form     Permission update form with role permissions
     * @param cascade  If true, triggers async cascade job (query parameter)
     * @param user     Requesting user (must be admin)
     * @return UpdateAssetPermissionsView containing message, permissionCount, inheritanceBroken, and asset
     * @throws DotDataException     If there's an error accessing data
     * @throws DotSecurityException If security validation fails
     */
    public UpdateAssetPermissionsView updateAssetPermissions(final String assetId,
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
            throw new NotFoundInDbException("Asset not found: " + assetId);
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
        final List<String> cascadeWarnings = new ArrayList<>();
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
                    final String warning = String.format(
                        "Failed to trigger cascade for role %s: %s",
                        roleForm.getRoleId(), e.getMessage());
                    Logger.warn(this, warning);
                    cascadeWarnings.add(warning);
                }
            }
        }

        // 9. Build and return response
        if (cascadeWarnings.isEmpty()) {
            Logger.info(this, () -> String.format(
                "Successfully updated permissions for asset: %s", assetId));
        } else {
            Logger.warn(this, () -> String.format(
                "Permissions saved for asset: %s, but %d cascade job(s) failed to trigger",
                assetId, cascadeWarnings.size()));
        }

        return buildUpdateResponse(asset, user, wasInheriting, permissionsToSave.size(), cascadeWarnings);
    }

    /**
     * Builds the typed response view for the update operation.
     *
     * @param asset             Updated asset
     * @param user              Requesting user
     * @param inheritanceBroken Whether inheritance was broken during this operation
     * @param permissionCount   Number of permissions saved
     * @param cascadeWarnings   Warnings from cascade operations that partially failed
     * @return UpdateAssetPermissionsView with message, permissionCount, inheritanceBroken, cascadeWarnings, and asset
     * @throws DotDataException     If there's an error accessing data
     * @throws DotSecurityException If security validation fails
     */
    private UpdateAssetPermissionsView buildUpdateResponse(final Permissionable asset,
                                                            final User user,
                                                            final boolean inheritanceBroken,
                                                            final int permissionCount,
                                                            final List<String> cascadeWarnings)
            throws DotDataException, DotSecurityException {

        // Use existing method to get asset metadata
        final AssetMetadata metadata = getAssetMetadata(asset, user);

        // Use existing method to get role permissions
        final List<RolePermissionView> rolePermissions = buildRolePermissions(asset, user);

        // Build typed AssetPermissionsView from metadata and permissions
        final AssetPermissionsView assetView = AssetPermissionsView.builder()
            .assetId(metadata.assetId())
            .assetType(metadata.assetType())
            .inheritanceMode(metadata.inheritanceMode())
            .isParentPermissionable(metadata.isParentPermissionable())
            .canEditPermissions(metadata.canEditPermissions())
            .canEdit(metadata.canEdit())
            .parentAssetId(metadata.parentAssetId())
            .permissions(rolePermissions)
            .build();

        // Return typed response view
        return UpdateAssetPermissionsView.builder()
            .message("Permissions saved successfully")
            .permissionCount(permissionCount)
            .inheritanceBroken(inheritanceBroken)
            .cascadeWarnings(cascadeWarnings.isEmpty() ? Optional.empty() : Optional.of(cascadeWarnings))
            .asset(assetView)
            .build();
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
     * @return ResetAssetPermissionsView containing message, assetId, and previousPermissionCount
     * @throws DotDataException     If there's an error accessing data
     * @throws DotSecurityException If security validation fails
     * @throws NotFoundInDbException If asset is not found
     * @throws ConflictException    If asset already inherits permissions (409)
     */
    public ResetAssetPermissionsView resetAssetPermissions(final String assetId, final User user)
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

        return ResetAssetPermissionsView.builder()
                .message("Individual permissions removed. Asset now inherits from parent.")
                .assetId(assetId)
                .previousPermissionCount(previousPermissionCount)
                .build();
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
     * @return UpdateRolePermissionsView containing roleId, roleName, and asset with updated permissions
     * @throws DotDataException     If there's an error accessing data
     * @throws DotSecurityException If security validation fails
     */
    public UpdateRolePermissionsView updateRolePermissions(final String roleId,
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
     * Builds the response view for the role permission update operation.
     *
     * @param asset Updated asset
     * @param role  Updated role
     * @param user  Requesting user
     * @return UpdateRolePermissionsView with roleId, roleName, and asset data
     * @throws DotDataException     If there's an error accessing data
     * @throws DotSecurityException If security validation fails
     */
    private UpdateRolePermissionsView buildRolePermissionUpdateResponse(final Permissionable asset,
                                                                         final Role role,
                                                                         final User user)
            throws DotDataException, DotSecurityException {

        // Build asset object with permissions for this role only
        final RolePermissionAssetView assetView = buildAssetWithRolePermissions(asset, role, user);

        return UpdateRolePermissionsView.builder()
            .roleId(role.getId())
            .roleName(role.getName())
            .asset(assetView)
            .build();
    }

    /**
     * Builds asset view with permissions filtered to a specific role.
     *
     * @param asset Target asset
     * @param role  Target role
     * @param user  Requesting user
     * @return RolePermissionAssetView with id, type, name, path, hostId, and permissions
     * @throws DotDataException     If there's an error accessing data
     * @throws DotSecurityException If security validation fails
     */
    private RolePermissionAssetView buildAssetWithRolePermissions(final Permissionable asset,
                                                                   final Role role,
                                                                   final User user)
            throws DotDataException, DotSecurityException {

        // Extract name, path, hostId based on asset type
        final String name;
        final String path;
        final String hostId;

        if (asset instanceof Host) {
            final Host host = (Host) asset;
            name = host.getHostname();
            path = "/" + host.getHostname();
            hostId = host.getIdentifier();
        } else if (asset instanceof Folder) {
            final Folder folder = (Folder) asset;
            name = folder.getName();
            path = APILocator.getIdentifierAPI().find(folder.getIdentifier()).getPath();
            hostId = folder.getHostId();
        } else {
            name = "";
            path = "";
            hostId = "";
        }

        // Permission metadata
        final boolean canEditPermissions = permissionAPI.doesUserHavePermission(
            asset, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user, false);
        final boolean inheritsPermissions = permissionAPI.isInheritingPermissions(asset);

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

        return RolePermissionAssetView.builder()
            .id(asset.getPermissionId())
            .type(getAssetTypeAsScope(asset).name())
            .name(name)
            .path(path)
            .hostId(hostId)
            .canEditPermissions(canEditPermissions)
            .inheritsPermissions(inheritsPermissions)
            .permissions(permissionMap)
            .build();
    }
}
