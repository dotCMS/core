package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.Pagination;
import com.dotcms.rest.ResponseEntityPaginatedDataView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.api.v1.user.UserResourceHelper;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.beans.Permission;
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
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.PaginationUtilParams;
import com.dotcms.util.pagination.AssetPermissionsPaginator;
import com.dotcms.util.pagination.UserPermissionsPaginator;
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

import javax.inject.Inject;
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

    private final WebResource webResource;
    private final PermissionHelper permissionHelper;
    private final UserAPI userAPI;
    private final RoleAPI roleAPI;
    private final PermissionSaveHelper permissionSaveHelper;
    private final UserPermissionsPaginator userPermissionsPaginator;
    private final AssetPermissionHelper assetPermissionHelper;
    private final AssetPermissionsPaginator assetPermissionsPaginator;

    public PermissionResource() {
        this(new WebResource(),
             PermissionHelper.getInstance(),
             APILocator.getUserAPI(),
             APILocator.getRoleAPI(),
             new PermissionSaveHelper(),
             new UserPermissionsPaginator(),
             new AssetPermissionHelper(),
             new AssetPermissionsPaginator());
    }

    @VisibleForTesting
    public PermissionResource(final PermissionSaveHelper permissionSaveHelper) {
        this(new WebResource(),
             PermissionHelper.getInstance(),
             APILocator.getUserAPI(),
             APILocator.getRoleAPI(),
             permissionSaveHelper,
             new UserPermissionsPaginator(permissionSaveHelper),
             new AssetPermissionHelper(),
             new AssetPermissionsPaginator());
    }

    @VisibleForTesting
    public PermissionResource(final WebResource webResource,
                              final PermissionHelper permissionHelper,
                              final UserAPI userAPI,
                              final RoleAPI roleAPI,
                              final PermissionSaveHelper permissionSaveHelper,
                              final UserPermissionsPaginator userPermissionsPaginator,
                              final AssetPermissionHelper assetPermissionHelper,
                              final AssetPermissionsPaginator assetPermissionsPaginator) {

        this.webResource = webResource;
        this.permissionHelper = permissionHelper;
        this.userAPI = userAPI;
        this.roleAPI = roleAPI;
        this.permissionSaveHelper = permissionSaveHelper;
        this.userPermissionsPaginator = userPermissionsPaginator;
        this.assetPermissionHelper = assetPermissionHelper;
        this.assetPermissionsPaginator = assetPermissionsPaginator;
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

    @Operation(
        summary = "Get permission metadata",
        description = "Returns available permission levels and scopes that can be assigned to users and roles"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "Permission metadata retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = ResponseEntityPermissionMetadataView.class))),
        @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403",
                    description = "Forbidden - frontend user attempted access (backend user required)",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityPermissionMetadataView getPermissionMetadata(
            @Parameter(hidden = true) @Context HttpServletRequest request,
            @Parameter(hidden = true) @Context HttpServletResponse response) {

        Logger.debug(this, () -> "Retrieving permission metadata");

        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final PermissionMetadataView permissionMetadata = PermissionMetadataView.builder()
            .levels(PermissionUtils.getAvailablePermissionLevels())
            .scopes(PermissionUtils.getAvailablePermissionScopes())
            .build();

        Logger.debug(this, () -> "Permission metadata retrieved successfully");

        return new ResponseEntityPermissionMetadataView(permissionMetadata);
    }

    /**
     * Gets permissions for a user's individual role, organized by assets (hosts and folders).
     * Returns user information, their individual role ID, and a paginated list of permission assets.
     *
     * @param request HTTP servlet request
     * @param response HTTP servlet response
     * @param userId User ID or email address
     * @param page Page number (1-based, consistent with other paginated endpoints)
     * @param perPage Items per page
     * @return ResponseEntityUserPermissionsView containing the user permissions
     * @throws DotDataException if data access fails
     * @throws DotSecurityException if security check fails
     */
    @Operation(
        summary = "Get user permissions",
        description = "Retrieves permissions for a user's individual role, organized by assets (hosts and folders). " +
                      "Admin users can view any user's permissions. Non-admin users can only view their own permissions."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "User permissions retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityPaginatedDataView.class))),
        @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403",
                    description = "Forbidden - non-admin user attempted to view another user's permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/user/{userId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityPaginatedDataView getUserPermissions(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response,
            @Parameter(description = "User ID or email address", required = true, example = "dotcms.org.1")
            @PathParam("userId") final String userId,
            @Parameter(description = "Page number (1-based)")
            @QueryParam("page") @DefaultValue("1") final int page,
            @Parameter(description = "Items per page")
            @QueryParam("per_page") @DefaultValue("40") final int perPage
    ) throws DotDataException, DotSecurityException {

        Logger.debug(this, () -> "Retrieving permissions for user: " + userId);

        final User requestingUser = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init().getUser();

        // Security check: only admin can view other users' permissions
        if (!requestingUser.getUserId().equals(userId) && !requestingUser.isAdmin()) {
            Logger.warn(this, () -> "Non-admin user " + requestingUser.getUserId() +
                                  " attempted to view permissions for user: " + userId);
            throw new DotSecurityException("Only admin user can retrieve other users permissions");
        }

        final User systemUser = userAPI.getSystemUser();
        final User targetUser = loadUserByIdOrEmail(userId, systemUser);

        // Get user's individual role
        final Role userRole = roleAPI.loadRoleById(targetUser.getUserId());

        // Build user info (needed in function closure)
        final UserInfoView userInfo = UserInfoView.builder()
                .id(targetUser.getUserId())
                .name(targetUser.getFullName())
                .email(targetUser.getEmailAddress())
                .build();

        final String roleId = userRole.getId();

        // Use PaginationUtil with function to build complex response
        final PaginationUtil paginationUtil = new PaginationUtil(userPermissionsPaginator);

        final Map<String, Object> extraParams = Map.of(
                UserPermissionsPaginator.ROLE_PARAM, userRole,
                UserPermissionsPaginator.USER_ID_PARAM, targetUser.getUserId()
        );

        final PaginationUtilParams<UserPermissionAssetView, UserPermissionsView> params =
                new PaginationUtilParams.Builder<UserPermissionAssetView, UserPermissionsView>()
                        .withRequest(request)
                        .withResponse(response)
                        .withUser(requestingUser)
                        .withPage(page)
                        .withPerPage(perPage)
                        .withExtraParams(extraParams)
                        .withFunction(paginatedAssets -> UserPermissionsView.builder()
                                .user(userInfo)
                                .roleId(roleId)
                                .assets(paginatedAssets)
                                .build())
                        .build();

        Logger.debug(this, () -> String.format("Retrieving permission assets for user %s (page %d, perPage %d)",
                userId, page, perPage));

        return paginationUtil.getPageView(params);
    }

    /**
     * Updates permissions for a user's individual role on a specific asset.
     * This endpoint assigns permissions directly to the user's individual role.
     * If the asset inherits permissions, inheritance will be broken automatically before saving.
     * Optionally cascade permissions to descendants (removes their individual permissions).
     *
     * @param request HTTP servlet request
     * @param response HTTP servlet response
     * @param userId User ID or email address
     * @param assetId Asset ID (host identifier or folder inode)
     * @param form Permission assignments to save
     * @return ResponseEntitySaveUserPermissionsView containing the result
     * @throws DotDataException if data access fails
     * @throws DotSecurityException if security check fails
     */
    @Operation(
        summary = "Update user permissions on asset",
        description = "Saves permissions for a user's individual role on a specific asset (host or folder). " +
                      "This endpoint assigns permissions directly to the user's individual role. " +
                      "If the asset inherits permissions, inheritance will be broken automatically before saving. " +
                      "Optionally cascade permissions to descendants (removes their individual permissions). " +
                      "Only admin users can update permissions."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "User permissions updated successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntitySaveUserPermissionsView.class))),
        @ApiResponse(responseCode = "400",
                    description = "Bad request - invalid input (see error message for details)",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403",
                    description = "Forbidden - admin access required or user lacks EDIT_PERMISSIONS on asset",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404",
                    description = "User or asset not found",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/user/{userId}/asset/{assetId}")
    @JSONP
    @NoCache
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntitySaveUserPermissionsView updateUserPermissions(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response,
            @Parameter(description = "User ID or email address", required = true, example = "dotcms.org.1")
            @PathParam("userId") final String userId,
            @Parameter(description = "Asset identifier (host ID or folder inode)", required = true, example = "48190c8c-42c4-46af-8d1a-0cd5db894797")
            @PathParam("assetId") final String assetId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Permission updates to apply. Use empty arrays to remove all permissions for a scope.",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SaveUserPermissionsForm.class)
                )
            )
            final SaveUserPermissionsForm form
    ) throws DotDataException, DotSecurityException {

        Logger.debug(this, () -> "Updating permissions for user: " + userId + " on asset: " + assetId);

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final User requestingUser = initData.getUser();

        if (!requestingUser.isAdmin()) {
            Logger.warn(this, () -> "Non-admin user " + requestingUser.getUserId() +
                                  " attempted to update permissions");
            throw new DotSecurityException("Only admin users can update permissions");
        }

        if (!UtilMethods.isSet(userId)) {
            throw new BadRequestException("User ID is required");
        }
        if (!UtilMethods.isSet(assetId)) {
            throw new BadRequestException("Asset ID is required");
        }

        form.checkValid();

        Logger.debug(this, () -> String.format("PUT /permissions/user/%s/asset/%s requested by %s",
            userId, assetId, requestingUser.getUserId()));

        final User systemUser = APILocator.getUserAPI().getSystemUser();
        final User targetUser = loadUserByIdOrEmail(userId, systemUser);
        final Permissionable asset = permissionSaveHelper.resolveAsset(assetId, systemUser);

        // Security check: User must have EDIT_PERMISSIONS on the asset
        if (!APILocator.getPermissionAPI().doesUserHavePermission(asset, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, requestingUser)) {
            Logger.warn(this, () -> String.format("User %s does not have EDIT_PERMISSIONS on asset %s",
                requestingUser.getUserId(), assetId));
            throw new DotSecurityException("User does not have permission to edit permissions on this asset");
        }

        final SaveUserPermissionsView saveResult = permissionSaveHelper.saveUserPermissions(
            targetUser.getUserId(),
            assetId,
            form,
            requestingUser
        );

        Logger.info(this, () -> String.format("Successfully updated permissions for user %s on asset %s (requested by %s)",
            targetUser.getUserId(), assetId, requestingUser.getUserId()));

        return new ResponseEntitySaveUserPermissionsView(saveResult);
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
                                      schema = @Schema(implementation = ResponseEntityPaginatedDataView.class))),
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
    public ResponseEntityPaginatedDataView getAssetPermissions(
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

        // Validate input parameters
        if (!UtilMethods.isSet(assetId)) {
            Logger.warn(this, "Asset ID is required but was not provided");
            throw new IllegalArgumentException("Asset ID is required");
        }

        // Resolve the asset to verify it exists
        final Permissionable asset = assetPermissionHelper.resolveAsset(assetId);
        if (asset == null) {
            Logger.warn(this, String.format("Asset not found: %s", assetId));
            throw new NotFoundInDbException(String.format("Asset not found: %s", assetId));
        }

        // Check if user has READ permission on the asset
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final boolean hasReadPermission = permissionAPI.doesUserHavePermission(
            asset, PermissionAPI.PERMISSION_READ, user, false);

        if (!hasReadPermission) {
            Logger.warn(this, String.format("User %s does not have READ access to asset: %s",
                user.getUserId(), assetId));
            throw new DotSecurityException("User does not have permission to view this asset's permissions");
        }

        // Get asset metadata for the response
        final AssetPermissionHelper.AssetMetadata metadata = assetPermissionHelper.getAssetMetadata(asset, user);

        // Use PaginationUtil with paginator (same pattern as getUserPermissions)
        final PaginationUtil paginationUtil = new PaginationUtil(assetPermissionsPaginator);

        final Map<String, Object> extraParams = Map.of(
            AssetPermissionsPaginator.ASSET_PARAM, asset,
            AssetPermissionsPaginator.REQUESTING_USER_PARAM, user
        );

        final PaginationUtilParams<RolePermissionView, AssetPermissionsView> params =
            new PaginationUtilParams.Builder<RolePermissionView, AssetPermissionsView>()
                .withRequest(request)
                .withResponse(response)
                .withUser(user)
                .withPage(page)
                .withPerPage(perPage)
                .withExtraParams(extraParams)
                .withFunction(paginatedRoles -> AssetPermissionsView.builder()
                    .assetId(metadata.assetId())
                    .assetType(metadata.assetType())
                    .inheritanceMode(metadata.inheritanceMode())
                    .isParentPermissionable(metadata.isParentPermissionable())
                    .canEditPermissions(metadata.canEditPermissions())
                    .canEdit(metadata.canEdit())
                    .parentAssetId(metadata.parentAssetId())
                    .permissions(paginatedRoles)
                    .build())
                .build();

        Logger.debug(this, () -> String.format(
            "Retrieving permission roles for asset %s (page %d, perPage %d)",
            assetId, page, perPage));

        return paginationUtil.getPageView(params);
    }

    /**
     * Loads a user by ID or email.
     * First tries to load by user ID, then falls back to email lookup.
     *
     * @param userIdOrEmail User ID or email address
     * @param systemUser System user for API calls
     * @return The found User
     * @throws DotDataException if user lookup fails
     * @throws DotSecurityException if security check fails
     */
    private User loadUserByIdOrEmail(final String userIdOrEmail, final User systemUser)
            throws DotDataException, DotSecurityException {

        try {
            return this.userAPI.loadUserById(userIdOrEmail);
        } catch (com.dotmarketing.business.NoSuchUserException e) {
            try {
                return this.userAPI.loadByUserByEmail(userIdOrEmail, systemUser, false);
            } catch (com.dotmarketing.business.NoSuchUserException ex) {
                Logger.warn(this, String.format("User not found: %s", userIdOrEmail));
                throw new com.dotcms.rest.exception.NotFoundException("User not found: " + userIdOrEmail);
            }
        }
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

        // Validate form before processing (follows SaveUserPermissionsForm pattern)
        form.checkValid();

        // Delegate to helper for business logic
        final UpdateAssetPermissionsView result = assetPermissionHelper.updateAssetPermissions(
            assetId, form, cascade, user);

        Logger.info(this, () -> String.format(
            "Successfully updated permissions for asset: %s", assetId));

        return new ResponseEntityUpdatePermissionsView(result);
    }
}
