package com.dotcms.rest.api.v1.system.permission;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    private final PermissionSaveHelper permissionSaveHelper;

    public PermissionResource() {
        this(new WebResource(),
             PermissionHelper.getInstance(),
             APILocator.getUserAPI());
    }

    @VisibleForTesting
    public PermissionResource(final WebResource      webResource,
                              final PermissionHelper permissionHelper,
                              final UserAPI          userAPI) {

        this.webResource      = webResource;
        this.permissionHelper = permissionHelper;
        this.userAPI          = userAPI;
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

        final User systemUser = APILocator.systemUser();
        final User targetUser = userResourceHelper.loadUserByIdOrEmail(userId, systemUser, requestingUser);
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
}
