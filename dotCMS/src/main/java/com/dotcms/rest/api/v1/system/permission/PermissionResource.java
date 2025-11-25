package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotmarketing.beans.Permission;
import com.dotcms.rest.api.v1.user.UserPermissionHelper;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Encapsulate the expose functionality for permissions
 * @author jsanca
 */
@Path("/v1/permissions")
@SwaggerCompliant(value = "Core authentication and user management APIs", batch = 1)
@Tag(name = "Permissions")
public class PermissionResource {

    private final WebResource      webResource;
    private final PermissionHelper permissionHelper;
    private final AssetPermissionHelper assetPermissionHelper;
    private final UserPermissionHelper userPermissionHelper;
    private final UserAPI          userAPI;
    private final RoleAPI          roleAPI;

    public PermissionResource() {

        this(new WebResource(), PermissionHelper.getInstance(),
             new AssetPermissionHelper(), new UserPermissionHelper(),
             APILocator.getUserAPI(), APILocator.getRoleAPI());
    }

    @VisibleForTesting
    public PermissionResource(final WebResource      webResource,
                              final PermissionHelper permissionHelper,
                              final AssetPermissionHelper assetPermissionHelper,
                              final UserPermissionHelper userPermissionHelper,
                              final UserAPI          userAPI,
                              final RoleAPI          roleAPI) {

        this.webResource      = webResource;
        this.permissionHelper = permissionHelper;
        this.assetPermissionHelper = assetPermissionHelper;
        this.userPermissionHelper = userPermissionHelper;
        this.userAPI          = userAPI;
        this.roleAPI          = roleAPI;
    }

    /**
     * Load a map of permission type indexed by permissionable types (optional all if not passed any) and permissions (READ, WRITE)
     * @param request     {@link HttpServletRequest}
     * @param response    {@link HttpServletResponse}
     * @param userid      {@link String}
     * @param permissions {@link String}
     * @param permissionableTypes {@link String}
     * @return Response
     * @throws DotDataException
     */
    @Operation(
        summary = "Get permissions by permission type",
        description = "Load a map of permission type indexed by permissionable types and permissions"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Permissions retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityPermissionsByTypeView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/_bypermissiontype")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public Response getPermissionsByPermissionType(final @Context HttpServletRequest request,
                                                   final @Context HttpServletResponse response,
                                                   @Parameter(description = "User ID", required = false)
                                                   final @QueryParam("userid")         String userid,
                                                   @Parameter(description = "Permission type (READ, WRITE)", required = false)
                                                   final @QueryParam("permission")     String permissions,
                                                   @Parameter(description = "Permissionable types", required = false)
                                                   final @QueryParam("permissiontype") String permissionableTypes)
            throws DotDataException, DotSecurityException {

        final User userInvoker = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init().getUser();

        Logger.debug(this, ()-> "GetPermissionsByPermissionType, permission: " +
                permissions + "permissiontype: " + permissionableTypes);

        //
        if (!userInvoker.getUserId().equals(userid) && !userInvoker.isAdmin()) {

            throw new DotSecurityException("Only admin user can retrieve other users permissions");
        }

        final User user = this.userAPI.loadUserById(userid);

        final Map<String, Map<String, Boolean>> permissionsMap = this.permissionHelper.getPermissionsByPermissionType(user,
                null != permissions? Stream.of(permissions.split(StringPool.COMMA)).
                        map(this.permissionHelper::fromStringToPermissionInt).collect(Collectors.toList()): null,
                null != permissionableTypes? Arrays.asList(permissionableTypes.split(StringPool.COMMA)): null
                );

        return Response.ok(new ResponseEntityView<>(permissionsMap)).build();
    }

    /**
     * Load a map of permission type indexed by permissionable types (optional all if not passed any) and permissions (READ, WRITE)
     * @param request     {@link HttpServletRequest}
     * @param response    {@link HttpServletResponse}
     * @param contentletId {@link String}
     * @param type      {@link String}
     * @return Response
     * @throws DotDataException
     */
    @GET
    @Path("/_bycontent")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Operation(summary = "Get permission for a Contentlet",
            description = "Retrieves permissions for a specific contentlet by its identifier. Only admin users can access this endpoint. Optionally filter by permission type (READ, WRITE, PUBLISH).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityPermissionView.class))),
                    @ApiResponse(responseCode = "403", description = "If not admin user"),})
    public Response getByContentlet(final @Context HttpServletRequest request,
                                    final @Context HttpServletResponse response,
                                    @Parameter(description = "Contentlet identifier", required = true)
                                    final @QueryParam("contentletId")   String contentletId,
                                    @Parameter(description = "Permission type (READ, WRITE, PUBLISH)", required = false)
                                    final @DefaultValue("READ") @QueryParam("type")   String type)
            throws DotDataException, DotSecurityException {

        final User userInvoker = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init().getUser();

        Logger.debug(this, ()-> "getByContentlet, contentlet: " +
                contentletId + "type: " + type);

        if (!userInvoker.isAdmin()) {

            throw new DotSecurityException("Only admin user can retrieve other users permissions");
        }

        PermissionAPI.Type permissionType = "ALL".equalsIgnoreCase(type)?
                null:PermissionAPI.Type.valueOf(type);

        final Contentlet contentlet = APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(contentletId);
        if(null == contentlet){
             return Response.status(Response.Status.NOT_FOUND).build();
        }
        final List<Permission> permissions = APILocator.getPermissionAPI().getPermissions(contentlet);

        return Response.ok(new ResponseEntityPermissionView(permissions.stream()
                .filter(permission -> this.filter(permissionType, permission))
                .map(PermissionResource::from)
                .collect(Collectors.toList()))).build();
    }

    /**
     * Return a map of roles group by permission type
     * @param request     {@link HttpServletRequest}
     * @param response    {@link HttpServletResponse}
     * @param contentletId {@link String}
     * @param type      {@link String}
     * @return Response
     * @throws DotDataException
     */
    @GET
    @Path("/_bycontent/_groupbytype")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Operation(summary = "Get permissions roles group by type for a Contentlet",
            description = "Retrieves permissions for a specific contentlet grouped by permission type (READ, WRITE, PUBLISH). Only admin users or content owners can access this endpoint.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityPermissionView.class))),
                    @ApiResponse(responseCode = "403", description = "If not admin user"),})
    public Response getByContentletGroupByType(final @Context HttpServletRequest request,
                                    final @Context HttpServletResponse response,
                                    @Parameter(description = "Contentlet identifier", required = true)
                                    final @QueryParam("contentletId")   String contentletId)
            throws DotDataException, DotSecurityException {

        if (!UtilMethods.isSet(contentletId)) {

            Logger.error(this, "The parameter contentletId is required");
            throw new IllegalArgumentException("The parameter contentletId is required");
        }

        final User userInvoker = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init().getUser();

        Logger.debug(this, ()-> "getByContentletGroupByType, contentlet: " + contentletId);

        final Contentlet contentlet = APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(contentletId);
        if (null == contentlet) {

            Logger.error(this, "The contentletId:" + contentletId + ", does not exist");
            throw new NotFoundInDbException("The contentletId:" + contentletId + ", does not exist");
        }

        if (!userInvoker.isAdmin() || !contentlet.getOwner().equals(userInvoker.getUserId())) {

            Logger.error(this, "Only admin user can retrieve other users permissions");
            throw new DotSecurityException("Only admin user can retrieve other users permissions");
        }

        final List<Permission> permissions = APILocator.getPermissionAPI().getPermissions(contentlet);

        final Map<String, List<String>> permissionsRoleGroupByTypeMap = new HashMap<>();
        for (final Permission permission : permissions) {

            permissionsRoleGroupByTypeMap.computeIfAbsent(
                    PermissionAPI.Type.findById(permission.getPermission()).name(),
                    k -> new ArrayList<>()).add(permission.getRoleId());
        }

        return Response.ok(new ResponseEntityPermissionGroupByTypeView(permissionsRoleGroupByTypeMap)).build();
    }

    private boolean filter(final PermissionAPI.Type permissionType, final Permission permission) {

        return null != permissionType?
                permission.getPermission() == permissionType.getType(): true;
    }

    public static PermissionView from(Permission permission) {

        final PermissionView view = new PermissionView(permission.getId(), permission.getInode(), permission.getRoleId(),
                PermissionAPI.Type.findById(permission.getPermission()), permission.isBitPermission(), permission.getType());
        return view;
    }

    /**
     * Retrieves permissions for a specific asset, including both individual and inherited permissions.
     * Results are paginated by role. Supports all permissionable asset types (Host, Folder, Contentlet,
     * Template, Container, Category, ContentType, Link, Rule, etc.).
     *
     * @param request HTTP servlet request
     * @param response HTTP servlet response
     * @param assetId Asset identifier (inode or identifier)
     * @param page Page number for pagination (1-indexed, default: 1)
     * @param perPage Number of roles to return per page (default: 40, max: 100)
     * @return ResponseEntityAssetPermissionsView containing asset metadata, paginated permissions, and pagination metadata
     * @throws DotDataException If there's an error accessing permission data
     * @throws DotSecurityException If security validation fails
     */
    @Operation(
        summary = "Get asset permissions",
        description = "Retrieves permissions for a specific asset by its identifier (inode or identifier). " +
                     "Returns asset metadata, a paginated list of roles with their permission levels, " +
                     "and pagination information. Supports all permissionable asset types including hosts, " +
                     "folders, contentlets, templates, containers, categories, links, and rules."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "Permissions retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityAssetPermissionsView.class))),
        @ApiResponse(responseCode = "400",
                    description = "Bad request - invalid query parameters (page < 1 or per_page not in 1-100 range)",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403",
                    description = "Forbidden - user lacks permission to view asset",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404",
                    description = "Asset not found",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/{assetId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityAssetPermissionsView getAssetPermissions(
            final @Context HttpServletRequest request,
            final @Context HttpServletResponse response,
            @Parameter(description = "Asset identifier (inode or identifier)", required = true)
            final @PathParam("assetId") String assetId,
            @Parameter(description = "Page number for pagination (1-indexed)", required = false, example = "1")
            final @QueryParam("page") @DefaultValue("1") Integer page,
            @Parameter(description = "Number of roles to return per page (max: 100)", required = false, example = "40")
            final @QueryParam("per_page") @DefaultValue("40") Integer perPage)
            throws DotDataException, DotSecurityException {

        Logger.debug(this, () -> String.format(
            "getAssetPermissions called - assetId: %s, page: %d, per_page: %d",
            assetId, page, perPage));

        // Initialize request context with authentication
        final User user = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init()
                .getUser();

        // Validate pagination parameters
        if (page < 1) {
            Logger.warn(this, String.format("Invalid page number: %d (must be >= 1)", page));
            throw new IllegalArgumentException("Invalid page number: must be >= 1");
        }

        if (perPage < 1 || perPage > 100) {
            Logger.warn(this, String.format("Invalid per_page: %d (must be between 1 and 100)", perPage));
            throw new IllegalArgumentException("Invalid per_page: must be between 1 and 100");
        }

        if (!UtilMethods.isSet(assetId)) {
            Logger.warn(this, "Asset ID is required but was not provided");
            throw new IllegalArgumentException("Asset ID is required");
        }

        // Verify user has READ permission on the asset
        // This is a viewing operation, not a management operation, so any user with READ access can view permissions
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();

        // First, resolve the asset to verify it exists and user has access
        final Permissionable asset = assetPermissionHelper.resolveAsset(assetId);
        if (asset == null) {
            Logger.warn(this, String.format("Asset not found: %s", assetId));
            throw new NotFoundInDbException(String.format("Asset not found: %s", assetId));
        }

        // Check if user has READ permission on the asset
        final boolean hasReadPermission = permissionAPI.doesUserHavePermission(
            asset, PermissionAPI.PERMISSION_READ, user, false);

        if (!hasReadPermission) {
            Logger.warn(this, String.format("User %s does not have READ access to asset: %s",
                user.getUserId(), assetId));
            throw new DotSecurityException("User does not have permission to view this asset's permissions");
        }

        // User has READ permission, proceed with building the response
        final AssetPermissionHelper.AssetPermissionResponse permissionResponse = assetPermissionHelper
            .buildAssetPermissionResponse(assetId, page, perPage, user);

        Logger.info(this, () -> String.format(
            "Successfully retrieved permissions for asset: %s", assetId));

        return new ResponseEntityAssetPermissionsView(permissionResponse.entity, permissionResponse.pagination);
    }

    /**
     * Updates permissions for a specific asset. This operation replaces all permissions for
     * the asset. If the asset is currently inheriting permissions, inheritance will be
     * automatically broken before applying the new permissions.
     *
     * @param request  HTTP servlet request
     * @param response HTTP servlet response
     * @param assetId  Asset identifier (inode or identifier)
     * @param cascade  If true, triggers async job to cascade permissions to descendant assets
     * @param form     Request body containing permissions to save
     * @return ResponseEntityUpdatePermissionsView containing operation result and updated permissions
     * @throws DotDataException     If there's an error accessing permission data
     * @throws DotSecurityException If security validation fails
     */
    @Operation(
        summary = "Update asset permissions",
        description = "Replaces all permissions for a specific asset. If the asset is currently " +
                     "inheriting permissions, inheritance will be automatically broken. " +
                     "Only admin users can access this endpoint. Use cascade=true to trigger " +
                     "an async job that removes individual permissions from descendant assets."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "Permissions updated successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityUpdatePermissionsView.class))),
        @ApiResponse(responseCode = "400",
                    description = "Bad request - invalid request body or role IDs",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403",
                    description = "Forbidden - user is not admin or lacks EDIT_PERMISSIONS on asset",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404",
                    description = "Asset not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500",
                    description = "Failed to update permissions",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/{assetId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public ResponseEntityUpdatePermissionsView updateAssetPermissions(
            final @Context HttpServletRequest request,
            final @Context HttpServletResponse response,
            @Parameter(description = "Asset identifier (inode or identifier)", required = true)
            final @PathParam("assetId") String assetId,
            @Parameter(description = "If true, triggers async job to cascade permissions to descendants", required = false)
            final @QueryParam("cascade") @DefaultValue("false") boolean cascade,
            @RequestBody(description = "Permission update data", required = true,
                        content = @Content(schema = @Schema(implementation = UpdateAssetPermissionsForm.class)))
            final UpdateAssetPermissionsForm form)
            throws DotDataException, DotSecurityException {

        Logger.debug(this, () -> String.format(
            "updateAssetPermissions called - assetId: %s, cascade: %s",
            assetId, cascade));

        // Initialize request context with authentication
        final User user = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init()
                .getUser();

        // Verify user is admin
        if (!user.isAdmin()) {
            Logger.warn(this, String.format(
                "Non-admin user %s attempted to update permissions for asset: %s",
                user.getUserId(), assetId));
            throw new DotSecurityException("Only admin users can update asset permissions");
        }

        // Delegate to helper for business logic
        final java.util.Map<String, Object> result = assetPermissionHelper.updateAssetPermissions(
            assetId, form, cascade, user);

        Logger.info(this, () -> String.format(
            "Successfully updated permissions for asset: %s", assetId));

        return new ResponseEntityUpdatePermissionsView(result);
    }

    /**
     * Resets permissions for a specific asset to inherit from its parent.
     * This operation removes all individual permissions from the asset, making it
     * inherit permissions from its parent in the hierarchy.
     *
     * @param request  HTTP servlet request
     * @param response HTTP servlet response
     * @param assetId  Asset identifier (inode or identifier)
     * @return ResponseEntityResetPermissionsView containing operation result
     * @throws DotDataException     If there's an error accessing permission data
     * @throws DotSecurityException If security validation fails
     */
    @Operation(
        summary = "Reset asset permissions to inherited",
        description = "Removes all individual permissions from an asset, making it inherit " +
                     "permissions from its parent in the hierarchy. Only admin users can " +
                     "access this endpoint. Returns 409 Conflict if the asset already inherits."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "Permissions reset successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityResetPermissionsView.class))),
        @ApiResponse(responseCode = "400",
                    description = "Bad request - invalid asset ID",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403",
                    description = "Forbidden - user is not admin",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404",
                    description = "Asset not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "409",
                    description = "Conflict - asset already inherits permissions from parent",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/{assetId}/_reset")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityResetPermissionsView resetAssetPermissions(
            final @Context HttpServletRequest request,
            final @Context HttpServletResponse response,
            @Parameter(description = "Asset identifier (inode or identifier)", required = true)
            final @PathParam("assetId") String assetId)
            throws DotDataException, DotSecurityException {

        Logger.debug(this, () -> String.format(
            "resetAssetPermissions called - assetId: %s", assetId));

        // Initialize request context with authentication
        final User user = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init()
                .getUser();

        // Verify user is admin
        if (!user.isAdmin()) {
            Logger.warn(this, String.format(
                "Non-admin user %s attempted to reset permissions for asset: %s",
                user.getUserId(), assetId));
            throw new DotSecurityException("Only admin users can reset asset permissions");
        }

        // Delegate to helper for business logic
        final java.util.Map<String, Object> result = assetPermissionHelper.resetAssetPermissions(
            assetId, user);

        Logger.info(this, () -> String.format(
            "Successfully reset permissions for asset: %s", assetId));

        return new ResponseEntityResetPermissionsView(result);
    }

    /**
     * Retrieves all hosts and folders where a role has permissions defined,
     * organized by asset with full permission matrices.
     *
     * @param request HTTP servlet request
     * @param response HTTP servlet response
     * @param roleId Role identifier
     * @return ResponseEntityRolePermissionsView containing role info and permission assets
     * @throws DotDataException If there's an error accessing permission data
     * @throws DotSecurityException If security validation fails
     */
    @Operation(
        summary = "Get role permissions",
        description = "Retrieves all hosts and folders where a role has permissions defined, " +
                     "organized by asset with full permission matrices. " +
                     "Admin users can view any role. Non-admin users can only view " +
                     "permissions for roles they belong to."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "Role permissions retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityRolePermissionsView.class))),
        @ApiResponse(responseCode = "400",
                    description = "Bad request - invalid role id",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403",
                    description = "Forbidden - user does not have access to view this role's permissions",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/role/{roleId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityRolePermissionsView getRolePermissions(
            final @Context HttpServletRequest request,
            final @Context HttpServletResponse response,
            @Parameter(description = "Role identifier", required = true)
            final @PathParam("roleId") String roleId)
            throws DotDataException, DotSecurityException {

        Logger.debug(this, () -> String.format("getRolePermissions called - roleId: %s", roleId));

        // Initialize request context with authentication
        final User requestingUser = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init()
                .getUser();

        // Validate roleId
        if (!UtilMethods.isSet(roleId)) {
            Logger.warn(this, "Role ID is required but was not provided");
            throw new BadRequestException("Role ID is required");
        }

        // Load the role
        final Role role = roleAPI.loadRoleById(roleId);
        if (role == null) {
            Logger.warn(this, String.format("Invalid role id: %s", roleId));
            throw new BadRequestException("Invalid role id: " + roleId);
        }

        // Authorization check - admin OR user has this role
        if (!requestingUser.isAdmin()) {
            final boolean userHasRole = roleAPI.doesUserHaveRole(requestingUser, role);
            if (!userHasRole) {
                Logger.warn(this, String.format(
                    "User %s attempted to view permissions for role %s without having that role",
                    requestingUser.getUserId(), roleId));
                throw new ForbiddenException(
                    "User does not have access to view permissions for role: " + roleId);
            }
        }

        // Build permission response (reuse existing helper)
        final List<Map<String, Object>> assets = userPermissionHelper
                .buildUserPermissionResponse(role, requestingUser);

        // Build response with roleId and roleName
        final Map<String, Object> responseData = Map.of(
            "roleId", role.getId(),
            "roleName", role.getName(),
            "assets", assets
        );

        Logger.info(this, () -> String.format(
            "Successfully retrieved permissions for role %s (requested by %s)",
            roleId, requestingUser.getUserId()));

        return new ResponseEntityRolePermissionsView(responseData);
    }
}
