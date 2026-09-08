package com.dotcms.rest.api.v1.system.role;

import com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityPaginatedDataView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.PaginationUtilParams;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.UserPaginator;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.RoleNameException;
import com.dotmarketing.portlets.user.ajax.UserAjax;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Try;
import org.apache.commons.beanutils.BeanUtils;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * This end-point provides access to information associated to dotCMS roles that
 * can be associated to one or more users in the system.
 *
 * @author Jose Castro
 * @version 3.7
 * @since Aug 9, 2016
 *
 */
@Path("/v1/roles")
@SwaggerCompliant(value = "Core authentication and user management APIs", batch = 1)
@Tag(name = "Roles")
@SuppressWarnings("serial")
public class RoleResource implements Serializable {

	private static final String ROLE_ID_SEPARATOR = ",";

	private final WebResource webResource;
	private final RoleAPI roleAPI;
	private final RoleHelper roleHelper = new RoleHelper();
	private final UserAPI userAPI     = APILocator.getUserAPI();
	private final PaginationUtil userPaginationUtil = new PaginationUtil(new UserPaginator());

	/**
	 * Default class constructor.
	 */
	public RoleResource() {
		this(new WebResource(new ApiProvider()), APILocator.getRoleAPI());
	}

	@VisibleForTesting
	public RoleResource(WebResource webResource, RoleAPI roleAPI) {
		this.webResource = webResource;
		this.roleAPI = roleAPI;
	}

	/**
	 * Verifies that a user is assigned to one of the specified role IDs. It is
	 * not guaranteed that this method will traverse the full list of roles.
	 * Once it finds a role that is associated to the user, it will return.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 * http://localhost:8080/api/v1/roles/checkuserroles/userid/dotcms.org.2789/roleids/8b21a705-5deb-4572-8752-fa0c25c34332,892ab105-f212-407f-8fb4-58ec59310a5e
	 * </pre>
	 *
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @param userId
	 *            - The ID of the user going through role verification.
	 * @param roleIds
	 *            - A comma-separated list of role IDs to check the user
	 *            against.
	 * @return If the user is associated to at least one role ID, returns a
	 *         {@link Response} with {@code true}. Otherwise, returns a
	 *         {@link Response} with {@code false}.
	 */
	@Operation(
		operationId = "checkUserRoles",
		summary = "Check user roles",
		description = "Verifies that a user is assigned to one of the specified role IDs"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Role check completed successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityRoleOperationView.class))),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - insufficient permissions",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@Path("/checkuserroles/userid/{userId}/roleids/{roleIds}")
	@Produces("application/json")
	public Response checkRoles(final @Context HttpServletRequest request,
							   final @Context HttpServletResponse response,
							   @Parameter(description = "User ID to check", required = true)
							   final @PathParam("userId") String userId,
							   @Parameter(description = "Comma-separated list of role IDs", required = true)
							   final @PathParam("roleIds") String roleIds) {

		final InitDataObject init = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true).init();

		boolean hasUserRole = false;
		try {
			String[] roles = roleIds.split(ROLE_ID_SEPARATOR);
			hasUserRole = this.roleAPI.doesUserHaveRoles(userId, list(roles));
		} catch (Exception e) {
			// In case of unknown error, so we report it as a 500
			Logger.error(this, "An error occurred when processing the request.", e);
			return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return Response.ok(new ResponseEntityRoleOperationView(Map.of("checkRoles", hasUserRole))).build();
	}

	/**
	 * Deletes a set of layouts into a role
	 * The user must have to be a BE and has to have access to roles portlet
	 */
	@Operation(
		operationId = "deleteRoleLayouts",
		summary = "Delete role layouts",
		description = "Deletes a set of layouts from a role"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Layouts deleted successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityRoleOperationView.class))),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - invalid role or layout data",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - admin permissions required",
					content = @Content(mediaType = "application/json"))
	})
	@DELETE
	@Path("/layouts")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteRoleLayouts(
			final @Context HttpServletRequest request,
			final @Context HttpServletResponse response,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "Role and layout information", 
				required = true,
				content = @Content(schema = @Schema(implementation = RoleLayoutForm.class))
			)
			final RoleLayoutForm roleLayoutForm) throws DotDataException, DotSecurityException {

		final InitDataObject initDataObject = new WebResource.InitBuilder()
				.requiredFrontendUser(false).rejectWhenNoUser(true)
				.requiredBackendUser(true).requiredPortlet("roles")
				.requestAndResponse(request, response).init();

		if (this.roleAPI.doesUserHaveRole(initDataObject.getUser(), this.roleAPI.loadCMSAdminRole())) {

			final String roleId         = roleLayoutForm.getRoleId();
			final Set<String> layoutIds = roleLayoutForm.getLayoutIds();
			final Role role = roleAPI.loadRoleById(roleId);
			final LayoutAPI layoutAPI   = APILocator.getLayoutAPI();

			Logger.debug(this, ()-> "Deleting the layouts : " + layoutIds + " to the role: " + roleId);

			return Response.ok(new ResponseEntityRoleOperationView(Map.of("deletedLayouts",
					this.roleHelper.deleteRoleLayouts(role, layoutIds, layoutAPI,
							this.roleAPI, APILocator.getSystemEventsAPI())))).build();
		} else {

			final String remoteIp = request.getRemoteHost();
			SecurityLogger.logInfo(UserAjax.class, "unauthorized attempt to call delete role layouts by user "+
					initDataObject.getUser().getUserId() + " from " + remoteIp);
			throw new DotSecurityException("User: '" +  initDataObject.getUser().getUserId() + "' not authorized");
		}
	}

	/**
	 * Add a new role
	 * Only admins can add roles.
	 */
	@Operation(
		operationId = "createRole",
		summary = "Create new role",
		description = "Creates a new role in the system. Only admins can add roles."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Role created successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = RoleResponseEntityView.class))),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - invalid role data or role name failed",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - admin permissions required",
					content = @Content(mediaType = "application/json"))
	})
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response addNewRole(
			final @Context HttpServletRequest request,
			final @Context HttpServletResponse response,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "Role information", 
				required = true,
				content = @Content(schema = @Schema(implementation = RoleForm.class))
			)
			final RoleForm roleForm) throws DotDataException, DotSecurityException {

		final User user = this.initRequireRolesPortletAndCmsAdmin(request, response);

		Role role = new Role();
		role.setName(roleForm.getRoleName());
		role.setRoleKey(roleForm.getRoleKey());
		role.setEditUsers(roleForm.isCanEditUsers());
		role.setEditPermissions(roleForm.isCanEditPermissions());
		role.setEditLayouts(roleForm.isCanEditLayouts());
		role.setDescription(roleForm.getDescription());

		if(Objects.nonNull(roleForm.getParentRoleId())) {

			final Role parentRole = roleAPI.loadRoleById(roleForm.getParentRoleId());
			role.setParent(parentRole.getId());
		}

		final String date = DateUtil.getCurrentDate();

		ActivityLogger.logInfo(getClass(), "Adding Role", "Date: " + date + "; "+ "User:" + user.getUserId());
		AdminLogger.log(getClass(), "Adding Role", "Date: " + date + "; "+ "User:" + user.getUserId());

		try {

			role = roleAPI.save(role);
		}  catch(RoleNameException e) {

			ActivityLogger.logInfo(getClass(), "Error Adding Role. Invalid Name", "Date: " + date + ";  "+ "User:" + user.getUserId());
			AdminLogger.log(getClass(), "Error Adding Role. Invalid Name", "Date: " + date + ";  "+ "User:" + user.getUserId());
			throw new DotDataException(
					Try.of(()->LanguageUtil.get(user,"Role-Save-Name-Failed")).getOrElse("Role Name not valid"),
					"Role-Save-Name-Failed", e);

		} catch(DotDataException | DotStateException e) {
			ActivityLogger.logInfo(getClass(), "Error Adding Role", "Date: " + date + ";  "+ "User:" + user.getUserId());
			AdminLogger.log(getClass(), "Error Adding Role", "Date: " + date + ";  "+ "User:" + user.getUserId());
			throw e;
		}

		ActivityLogger.logInfo(getClass(), "Role Created", "Date: " + date + "; "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );
		AdminLogger.log(getClass(), "Role Created", "Date: " + date + "; "+ "User:" + user.getUserId() + "; RoleID: " + role.getId() );

		return Response.ok(new RoleResponseEntityView(role.toMap())).build();
	}

	/**
	 * Shared authorization gate for the role-mutation endpoints (#36936–#36939): requires an
	 * authenticated backend user with access to the Roles portlet AND the CMS Administrator
	 * role. Rejections are security-logged.
	 *
	 * @return the authenticated, authorized user
	 */
	private User initRequireRolesPortletAndCmsAdmin(final HttpServletRequest request,
			final HttpServletResponse response) throws DotDataException, DotSecurityException {

		final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource)
				.requiredFrontendUser(false).rejectWhenNoUser(true)
				.requiredBackendUser(true).requiredPortlet("roles")
				.requestAndResponse(request, response).init();

		final User user = initDataObject.getUser();
		if (!this.roleAPI.doesUserHaveRole(user, this.roleAPI.loadCMSAdminRole())) {

			SecurityLogger.logInfo(this.getClass(), "unauthorized attempt to modify roles by user "
					+ user.getUserId() + " from " + request.getRemoteHost());
			throw new DotSecurityException("User: '" + user.getUserId() + "' not authorized");
		}

		return user;
	}

	/**
	 * Updates an existing role — name, key, description, can-grant flags and parent
	 * (reparent). A null {@code parentRoleId} turns the role into a root role, mirroring the
	 * legacy DWR {@code RoleAjax#updateRole} behavior. System and locked roles are rejected.
	 * The caller must be a backend user with access to the Roles portlet and the CMS
	 * Administrator role.
	 */
	@Operation(
		operationId = "updateRole",
		summary = "Update a role",
		description = "Updates an existing role's name, key, description, can-grant flags and parent. " +
				"PUT is a full replace: every field of the role is overwritten from the request body, so " +
				"clients must send the complete role representation — omitted fields are reset (booleans " +
				"default to false, omitted roleKey/description are cleared, omitted parentRoleId reparents " +
				"to root). A null parentRoleId turns the role into a root role. Reparenting under the role's " +
				"own descendant is rejected. System and locked roles cannot be updated. Note: the role is " +
				"updated in place — grants and permissions attached to the role are preserved."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200",
					description = "Role updated successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityRoleDetailView.class))),
		@ApiResponse(responseCode = "400",
					description = "Bad request - invalid role name, or the reparent would create a hierarchy cycle",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401",
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403",
					description = "Forbidden - admin permissions required, or the role is a system or locked role",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404",
					description = "Role or parent role not found",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "409",
					description = "Conflict - duplicate role key, or duplicate role name under the same parent",
					content = @Content(mediaType = "application/json"))
	})
	@PUT
	@Path("/{roleid}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ResponseEntityRoleDetailView updateRole(
			final @Context HttpServletRequest request,
			final @Context HttpServletResponse response,
			@Parameter(description = "Id of the role to update", required = true)
			final @PathParam("roleid") String roleId,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "Role information — same shape as POST /v1/roles",
				required = true,
				content = @Content(schema = @Schema(implementation = RoleForm.class))
			)
			final RoleForm roleForm) throws DotDataException, DotSecurityException {

		final User user = this.initRequireRolesPortletAndCmsAdmin(request, response);

		final Role updatedRole = this.roleHelper.updateRole(roleId, roleForm, user);

		// same response shape as GET /v1/roles/{roleid}, counts included
		return new ResponseEntityRoleDetailView(
				this.roleHelper.toRoleViews(List.of(updatedRole), true, this.roleAPI).get(0));
	}

	/**
	 * Deletes an existing role. The deletion CASCADES, matching the legacy behavior the old
	 * Roles &amp; Tools portlet has always had: the role is removed from every user that has it,
	 * all its permissions are deleted, and its layout (tool-group) assignments are detached.
	 * Deletion is blocked only where legacy blocks it: roles with children (409), roles
	 * referenced by a workflow action's Assign To (409), and system or locked roles (403).
	 * The caller must be a backend user with access to the Roles portlet and the CMS
	 * Administrator role.
	 */
	@Operation(
		operationId = "deleteRole",
		summary = "Delete a role",
		description = "Deletes a role. The deletion CASCADES and is not reversible: the role is " +
				"removed from all users that have it, all permissions granted to the role are " +
				"deleted, and its layout (tool-group) assignments are detached. The response " +
				"reports how many users were affected. Deletion is rejected when the role has " +
				"child roles or is referenced by a workflow action's Assign To (409), and for " +
				"system or locked roles (403)."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200",
					description = "Role deleted successfully; usersAffected reports the cascade blast radius",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityRoleDeletionView.class))),
		@ApiResponse(responseCode = "401",
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403",
					description = "Forbidden - admin permissions required, or the role is a system or locked role",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404",
					description = "Role not found",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "409",
					description = "Conflict - the role has child roles, or a workflow action references it",
					content = @Content(mediaType = "application/json"))
	})
	@DELETE
	@Path("/{roleid}")
	@Produces(MediaType.APPLICATION_JSON)
	public ResponseEntityRoleDeletionView deleteRole(
			final @Context HttpServletRequest request,
			final @Context HttpServletResponse response,
			@Parameter(description = "Id of the role to delete", required = true)
			final @PathParam("roleid") String roleId) throws DotDataException, DotSecurityException {

		final User user = this.initRequireRolesPortletAndCmsAdmin(request, response);

		final int usersAffected = this.roleHelper.deleteRole(roleId, user);

		return new ResponseEntityRoleDeletionView(RoleDeletionView.builder()
				.deleted(true)
				.roleId(roleId)
				.usersAffected(usersAffected)
				.build());
	}

	/**
	 * Grants a role to a user. Idempotent, mirroring the legacy DWR
	 * {@code RoleAjax#addUserToRole} path: granting a role the user already holds — directly
	 * or inherited through the role hierarchy — returns 200 and changes nothing. The only
	 * grant gate is the role's {@code editUsers} flag (403). The caller must be a backend
	 * user with access to the Roles portlet and the CMS Administrator role.
	 */
	@Operation(
		operationId = "addUserToRole",
		summary = "Grant a role to a user",
		description = "Grants the role to the user as a DIRECT membership. The operation is " +
				"IDEMPOTENT: granting a role the user already holds returns 200 and changes " +
				"nothing — no duplicate membership is created and retries are safe, even when the " +
				"role's editUsers flag has since been turned off. Note the " +
				"inherited-membership behavior (legacy parity): role membership is inherited DOWN " +
				"the role tree, so a user holding a parent role implicitly holds every child role. " +
				"Granting a role the user already INHERITS this way also returns 200 but does NOT " +
				"create a direct membership — the user will not appear in the role's direct-users " +
				"list afterwards. Roles whose editUsers flag is false cannot be granted (403); " +
				"workflow and system roles are non-grantable because that flag is false on them."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200",
					description = "User holds the role after the call; the response carries the granted "
							+ "roleId and a minimal user payload",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityRoleUserGrantView.class))),
		@ApiResponse(responseCode = "401",
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403",
					description = "Forbidden - admin permissions required, or the role's editUsers flag is false",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404",
					description = "Role or user not found",
					content = @Content(mediaType = "application/json"))
	})
	@POST
	@Path("/{roleid}/users/{userId}")
	@Produces(MediaType.APPLICATION_JSON)
	public ResponseEntityRoleUserGrantView addUserToRole(
			final @Context HttpServletRequest request,
			final @Context HttpServletResponse response,
			@Parameter(description = "Id of the role to grant", required = true)
			final @PathParam("roleid") String roleId,
			@Parameter(description = "Id of the user to grant the role to", required = true)
			final @PathParam("userId") String userId) throws DotDataException, DotSecurityException {

		final User user = this.initRequireRolesPortletAndCmsAdmin(request, response);

		final User targetUser = this.roleHelper.addUserToRole(roleId, userId, user);

		return new ResponseEntityRoleUserGrantView(RoleUserGrantView.builder()
				.granted(true)
				.roleId(roleId)
				.user(RoleMemberUserView.builder()
						.userId(targetUser.getUserId())
						.email(targetUser.getEmailAddress())
						.fullName(targetUser.getFullName())
						.build())
				.build());
	}

	/**
	 * Bulk-removes users from a role with partial-success semantics: removable direct
	 * memberships are removed, everything else is reported in {@code skipped} with a reason
	 * ({@code not_found}, {@code inherited}, {@code error}) — the batch never fails as a whole
	 * once the role resolves and passes the {@code editUsers} gate (non-user-assignable roles
	 * are rejected with 403, mirroring the grant endpoint). The caller must be a backend user
	 * with access to the Roles portlet and the CMS Administrator role.
	 */
	@Operation(
		operationId = "removeUsersFromRole",
		summary = "Remove users from a role",
		description = "Bulk-removes the DIRECT membership of the given users from the role. The " +
				"batch has PARTIAL-SUCCESS semantics: it never fails as a whole once the role " +
				"resolves — every removable membership is removed and every other entry is " +
				"reported in the skipped list with a reason: not_found (no user matches the id), " +
				"inherited (the user is not a direct member — the membership is inherited through " +
				"the role hierarchy, or the user is not a member at all; inherited membership can " +
				"only be revoked by removing the user from the ancestor role that grants it), or " +
				"error (unexpected per-user failure, logged server-side). Removals are committed " +
				"per user, so entries already processed stay removed regardless of later entries. " +
				"Only user-assignable roles (editUsers=true) accept membership changes: a role " +
				"whose editUsers flag is false — e.g. a user's individual role or a system role — " +
				"is rejected with 403 for the whole request, mirroring the grant endpoint."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200",
					description = "Batch processed; removedUserIds and skipped report the per-user outcomes",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityRoleUsersRemovalView.class))),
		@ApiResponse(responseCode = "400",
					description = "Bad request - missing body, empty userIds, or null/blank entries",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401",
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403",
					description = "Forbidden - admin permissions required, or the role's editUsers flag is false",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404",
					description = "Role not found",
					content = @Content(mediaType = "application/json"))
	})
	@DELETE
	@Path("/{roleid}/users")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ResponseEntityRoleUsersRemovalView removeUsersFromRole(
			final @Context HttpServletRequest request,
			final @Context HttpServletResponse response,
			@Parameter(description = "Id of the role to remove users from", required = true)
			final @PathParam("roleid") String roleId,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "Ids of the users to remove from the role",
				required = true,
				content = @Content(schema = @Schema(implementation = RoleUsersForm.class))
			)
			final RoleUsersForm roleUsersForm) throws DotDataException, DotSecurityException {

		final User user = this.initRequireRolesPortletAndCmsAdmin(request, response);

		if (null == roleUsersForm) {
			throw new BadRequestException("Request body with userIds is required");
		}
		roleUsersForm.checkValid();

		return new ResponseEntityRoleUsersRemovalView(
				this.roleHelper.removeUsersFromRole(roleId, roleUsersForm.getUserIds(), user));
	}

	/**
	 * Saves set of layout into a role
	 * The user must have to be a BE and has to have access to roles portlet
	 */
	@Operation(
		operationId = "saveRoleLayouts",
		summary = "Save role layouts",
		description = "Saves a set of layouts to a role"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Layouts saved successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityRoleOperationView.class))),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - invalid role or layout data",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - admin permissions required",
					content = @Content(mediaType = "application/json"))
	})
	@POST
	@Path("/layouts")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response saveRoleLayouts(
			final @Context HttpServletRequest request,
			final @Context HttpServletResponse response,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "Role and layout information", 
				required = true,
				content = @Content(schema = @Schema(implementation = RoleLayoutForm.class))
			)
			final RoleLayoutForm roleLayoutForm) throws DotDataException, DotSecurityException {

		final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource)
				.requiredFrontendUser(false).rejectWhenNoUser(true)
				.requiredBackendUser(true).requiredPortlet("roles")
				.requestAndResponse(request, response).init();

		if (this.roleAPI.doesUserHaveRole(initDataObject.getUser(), this.roleAPI.loadCMSAdminRole())) {

			final String roleId         = roleLayoutForm.getRoleId();
			final Set<String> layoutIds = roleLayoutForm.getLayoutIds();
			final Role role = roleAPI.loadRoleById(roleId);
			final LayoutAPI layoutAPI   = APILocator.getLayoutAPI();

			Logger.debug(this, ()-> "Saving the layouts : " + layoutIds + " to the role: " + roleId);

			return Response.ok(new ResponseEntityRoleOperationView(Map.of("savedLayouts",
					this.roleHelper.saveRoleLayouts(role, layoutIds, layoutAPI,
							this.roleAPI, APILocator.getSystemEventsAPI())))).build();
		}

		final String remoteIp = request.getRemoteHost();
		SecurityLogger.logInfo(UserAjax.class, "unauthorized attempt to call save role layouts by user "+
				initDataObject.getUser().getUserId() + " from " + remoteIp);
		throw new DotSecurityException("User: '" +  initDataObject.getUser().getUserId() + "' not authorized");
	}

	/**
	 * Returns a collection of layouts associated to a role
	 * The user must have to be a BE and has to have access to roles portlet
	 */
	@Operation(
		operationId = "findRoleLayouts",
		summary = "Find role layouts",
		description = "Returns a collection of layouts associated to a role"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Role layouts retrieved successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityLayoutList.class))),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - invalid role ID",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - roles portlet access required",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@Path("/{roleId}/layouts")
	@Produces(MediaType.APPLICATION_JSON)
	public Response findRoleLayouts(
			final @Context HttpServletRequest request,
			final @Context HttpServletResponse response,
			@Parameter(description = "Role ID", required = true)
			final @PathParam("roleId") String roleId) throws DotDataException {

		new WebResource.InitBuilder(this.webResource)
				.requiredFrontendUser(false).rejectWhenNoUser(true)
				.requiredBackendUser(true).requiredPortlet("roles")
				.requestAndResponse(request, response).init();

		Logger.debug(this, ()-> "Finding the role layouts for the roleid: " + roleId);
		final Role role              = roleAPI.loadRoleById(roleId);
		final LayoutAPI layoutAPI    = APILocator.getLayoutAPI();

        return Response.ok(new ResponseEntityLayoutList(layoutAPI.loadLayoutsForRole(role)))
                .build();
	}

	/**
	 * Load the user and roles by role id.
	 * @param request   {@link HttpServletRequest}
	 * @param response  {@link HttpServletResponse}
	 * @param roleId    {@link String} role
	 * @param roleHierarchyForAssign {@link Boolean} true if want to include the hierarchy, false by default
	 * @param roleNameToFilter {@link String} prefix role name, if you want to filter the results
	 * @return Response
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@Operation(
		operationId = "loadUsersAndRolesByRoleId",
		summary = "Load users and roles by role ID",
		description = "Load the user and roles by role id with optional hierarchy and filtering"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Users and roles retrieved successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityRoleListView.class))),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - invalid role ID",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - backend user required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404", 
					description = "Role not found",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@Path("/{roleid}/rolehierarchyanduserroles")
	@Produces(MediaType.APPLICATION_JSON)
	@SuppressWarnings("unchecked")
	public Response loadUsersAndRolesByRoleId(@Context final HttpServletRequest request,
											  @Context final HttpServletResponse response,
											  @Parameter(description = "Role ID", required = true)
											  @PathParam   ("roleid") final String roleId,
											  @Parameter(description = "Include role hierarchy", required = false)
											  @DefaultValue("false") @QueryParam("roleHierarchyForAssign") final boolean roleHierarchyForAssign,
											  @Parameter(description = "Role name filter prefix", required = false)
											  @QueryParam  ("name") final String roleNameToFilter) throws DotDataException, DotSecurityException {

		new WebResource.InitBuilder(this.webResource).requiredBackendUser(true)
				.requiredFrontendUser(false).requestAndResponse(request, response)
				.rejectWhenNoUser(true).init();

		final Role role = this.roleAPI.loadRoleById(roleId);

		if (null == role || !UtilMethods.isSet(role.getId())) {

			throw new DoesNotExistException("The role: " + roleId + " does not exists");
		}

		final List<Role> roleList = new ArrayList<>();
		final List<User> userList = new ArrayList<>();

		Logger.debug(this, ()->"loading users and roles by role: " + roleId);

		if (!role.isUser()) {

			userList.addAll(this.roleAPI.findUsersForRole(role, roleHierarchyForAssign));
			roleList.addAll(roleHierarchyForAssign? this.roleAPI.findRoleHierarchy(role): Arrays.asList(role));
		} else {

			userList.add(this.userAPI.loadUserById(role.getRoleKey(), APILocator.systemUser(), false));
		}

		for (final User user : userList) {

			final Role roleToTest = this.roleAPI.getUserRole(user);
			if (roleToTest != null && UtilMethods.isSet(roleToTest.getId())) {

				roleList.add(roleToTest);
			}
		}

		return Response.ok(new ResponseEntityRoleListView(
				null != roleNameToFilter? this.filterRoleList(roleNameToFilter, roleList):roleList)).build();
	}

	private final List<Role> filterRoleList(final String roleNameToFilter, final List<Role> roleList) {

		final String roleNameToFilterClean = roleNameToFilter.toLowerCase().replaceAll( "\\*", StringPool.BLANK);
		return UtilMethods.isSet(roleNameToFilterClean)?
				roleList.stream().filter(myRole -> myRole.getName().toLowerCase()
						.startsWith(roleNameToFilterClean)).collect(Collectors.toList()):
				roleList;
	}

	/**
	 * Returns the paginated list of users directly granted the given role, using the standard
	 * user serialization (email address included). Grants inherited through the role hierarchy
	 * are not part of the response: clients that need the effective member list walk the
	 * ancestor chain through the {@code parent} attribute of {@link RoleView} and call this
	 * endpoint per role.
	 *
	 * @param request   {@link HttpServletRequest}
	 * @param response  {@link HttpServletResponse}
	 * @param roleId    id of the role to list users for
	 * @param filter    optional search matching user id, first name, last name, email or full name
	 * @param page      page number, 1-based
	 * @param perPage   page size
	 * @param orderBy   column to sort by
	 * @param direction sorting direction, ASC or DESC
	 * @return the paginated user list
	 * @throws DotDataException if loading the role fails
	 */
	@Operation(
		operationId = "loadUsersByRoleId",
		summary = "Get the users directly granted a role",
		description = "Returns the paginated list of users directly granted the given role, "
				+ "using the standard user serialization (email address included). Grants "
				+ "inherited through the role hierarchy are not included."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200",
					description = "Users retrieved successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityPaginatedDataView.class))),
		@ApiResponse(responseCode = "400",
					description = "Bad request - invalid pagination or sorting parameters",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401",
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403",
					description = "Forbidden - roles portlet access required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404",
					description = "Role not found",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@Path("/{roleid}/users")
	@JSONP
	@NoCache
	@Produces(MediaType.APPLICATION_JSON)
	public ResponseEntityPaginatedDataView loadUsersByRoleId(
			@Parameter(hidden = true) @Context final HttpServletRequest request,
			@Parameter(hidden = true) @Context final HttpServletResponse response,
			@Parameter(description = "Id of the role to list users for", required = true)
			@PathParam("roleid") final String roleId,
			@Parameter(description = "Filter matching user id, first name, last name, email or full name")
			@QueryParam("filter") final String filter,
			@Parameter(description = "Page number for pagination")
			@DefaultValue("1") @QueryParam(PaginationUtil.PAGE) final int page,
			@Parameter(description = "Number of items per page")
			@DefaultValue("40") @QueryParam(PaginationUtil.PER_PAGE) final int perPage,
			@Parameter(description = "Column name for sorting results")
			@QueryParam(PaginationUtil.ORDER_BY) final String orderBy,
			@Parameter(description = "Sorting direction: ASC or DESC")
			@DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION) final String direction)
			throws DotDataException {

		final InitDataObject initData = new WebResource.InitBuilder(this.webResource)
				.requiredBackendUser(true).requiredFrontendUser(false)
				.requiredPortlet("roles")
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true).init();

		Logger.debug(this, () -> "Loading the users directly granted the role: " + roleId);

		final Role role = this.roleAPI.loadRoleById(roleId);
		if (null == role || !UtilMethods.isSet(role.getId())) {

			throw new DoesNotExistException("The role: " + roleId + " does not exist");
		}

		final OrderDirection orderDirection = OrderDirection.valueOf(direction);

		// UserPaginator reads ordering from FilteringParams keys, not from the
		// PaginationUtil orderBy/direction arguments, so pass them explicitly (the
		// same wiring /v1/users/filter uses). The direction value is enum-gated and
		// mapped to the SQLUtil constants FilteringParams expects (leading space).
		final Map<String, Object> extraParams = new HashMap<>(
				Map.of(UserPaginator.ROLES_PARAM, List.of(role),
						UserAPI.FilteringParams.ORDER_DIRECTION_PARAM,
						OrderDirection.DESC == orderDirection ? SQLUtil._DESC : SQLUtil._ASC));
		if (UtilMethods.isSet(orderBy)) {
			extraParams.put(UserAPI.FilteringParams.ORDER_BY_PARAM, orderBy);
		}

		final PaginationUtilParams<Map<String, Object>, List<Map<String, Object>>> params =
				new PaginationUtilParams.Builder<Map<String, Object>, List<Map<String, Object>>>()
						.withRequest(request).withResponse(response)
						.withUser(initData.getUser()).withFilter(filter)
						.withPage(page).withPerPage(perPage)
						.withOrderBy(orderBy).withDirection(orderDirection)
						.withExtraParams(extraParams).build();

		return this.userPaginationUtil.getPageView(params);
	}


	/**
	 * Load role based on the role id.
	 *
	 * @param roleId id of the role to search for.
	 * @param loadChildrenRoles true - will add the data of all children roles of the requested role.
	 * 							false - will only show the data of the requested role.
	 * @return {@link RoleView} role requested.
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@Operation(
		operationId = "loadRoleByRoleId",
		summary = "Load role by role ID",
		description = "Load role based on the role id with optional children roles"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Role retrieved successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityRoleDetailView.class))),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - invalid role ID",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - backend user required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404", 
					description = "Role not found",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@Path("/{roleid}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response loadRoleByRoleId(@Context final HttpServletRequest request,
									 @Context final HttpServletResponse response,
									 @Parameter(description = "Role ID", required = true)
									 @PathParam   ("roleid") final String roleId,
									 @Parameter(description = "Load children roles", required = false)
									 @DefaultValue("true") @QueryParam("loadChildrenRoles") final boolean loadChildrenRoles)
			throws DotDataException, DotSecurityException {

		new WebResource.InitBuilder(this.webResource).requiredBackendUser(true)
				.requiredFrontendUser(false).requestAndResponse(request, response)
				.rejectWhenNoUser(true).init();

		final Role role = this.roleAPI.loadRoleById(roleId);

		if (null == role || !UtilMethods.isSet(role.getId())) {

			throw new DoesNotExistException("The role: " + roleId + " does not exists");
		}

		return Response.ok(new ResponseEntityRoleDetailView(
				this.roleHelper.toRoleViews(List.of(role), loadChildrenRoles, this.roleAPI)
						.get(0))).build();

	}

	/**
	 * Loads the root roles.
	 *
	 * @param loadChildrenRoles true - will add the data of all children roles of the requested role.
	 * 							false - will only show the data of the requested role.
	 * @return list of {@link RoleView}
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@Operation(
		operationId = "loadRootRoles",
		summary = "Load root roles",
		description = "Loads the root roles with optional children roles"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Root roles retrieved successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityRoleViewListView.class))),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - backend user required",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response loadRootRoles(@Context final HttpServletRequest request,
								  @Context final HttpServletResponse response,
								  @Parameter(description = "Load children roles", required = false)
								  @DefaultValue("true") @QueryParam("loadChildrenRoles") final boolean loadChildrenRoles)
			throws DotDataException, DotSecurityException {

		new WebResource.InitBuilder(this.webResource).requiredBackendUser(true)
				.requiredFrontendUser(false).requestAndResponse(request, response)
				.rejectWhenNoUser(true).init();

		final List<Role> rootRoles = this.roleAPI.findRootRoles();

		return Response.ok(new ResponseEntityRoleViewListView(
				this.roleHelper.toRoleViews(rootRoles, loadChildrenRoles, this.roleAPI))).build();
	}

	/**
	 * Search roles
	 * If you want to filter by name:
	 * /api/v1/roles/_search?searchName=CMS
	 * Will include the roles starting by CMS
	 *
	 * if you want to filter by role key
	 * /api/v1/roles/_search?searchKey=dotcms
	 * Will include the roles starting by dotcmds
	 *
	 * Want specific role
	 * /api/v1/roles/_search?roleId=654b0931-1027-41f7-ad4d-173115ed8ec1
	 *
	 * Want pagination
	 * /api/v1/roles/_search?start=5&count=10
	 * From the 5 to the 15
	 *
	 * Do not want to include user roles (by default is true)
	 * /api/v1/roles/_search?includeUserRoles=false
	 *
	 * Want to include workflow roles (by default is false)
	 * /api/v1/roles/_search?includeWorkflowRoles=true
	 *
	 * @return list of {@link Role}
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@Path("_search")
	@GET
	@Produces("application/json")
	@Operation(
		operationId = "searchRoles",
		summary = "Search Roles",
		description = "Search and filter roles by name, key, or ID with pagination support. Includes options to filter by workflow roles.",
		responses = {
					@ApiResponse(
							responseCode = "200",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation = ResponseEntitySmallRoleView.class)))})
	public Response searchRoles(@Context final HttpServletRequest request,
								@Context final HttpServletResponse response,
								@Parameter(name = "searchName", description = "Value to filter by role name")
								@DefaultValue("")   @QueryParam("searchName") final String searchName,
								@Parameter(name = "searchKey", description = "Value to filter by role key")
								@DefaultValue("")   @QueryParam("searchKey") final String searchKey,
								@Parameter(name = "roleId", description = "Value for specific role id")
								@DefaultValue("")   @QueryParam("roleId")     final String roleId,
								@Parameter(name = "start", description = "Offset on pagination")
								@DefaultValue("0")  @QueryParam("start")      final int startParam,
								@Parameter(name = "count", description = "Size on pagination")
								@DefaultValue("20") @QueryParam("count")      final int count,
								@Parameter(name = "includeUserRoles", description = "Set false if do not want to include user rules")
								@DefaultValue("true") @QueryParam("includeUserRoles")      final boolean includeUserRoles,
								@Parameter(name = "includeWorkflowRoles", description = "Set to true if want to include the workflow roles")
								@DefaultValue("false") @QueryParam("includeWorkflowRoles")  final boolean includeWorkflowRoles)
            throws DotDataException, DotSecurityException, LanguageException, IOException, InvocationTargetException, IllegalAccessException {

		final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource).requiredBackendUser(true)
				.requiredFrontendUser(false).requestAndResponse(request, response)
				.rejectWhenNoUser(true).init();

		Logger.debug(this, ()-> "Searching role, searchName: " + searchName + ", searchKey: " + searchKey + ", roleId: " + roleId
				+ ", start: " + startParam + ", count: " + count + ", includeUserRoles: " + includeUserRoles + ", includeWorkflowRoles: " + includeWorkflowRoles);

        int start = startParam;
        final Role cmsAnonOrig    = this.roleAPI.loadCMSAnonymousRole();
        final Role cmsAnon        = new Role();
        BeanUtils.copyProperties(cmsAnon, cmsAnonOrig);
        final String cmsAnonName  = LanguageUtil.get(initDataObject.getUser(), "current-user");
        cmsAnon.setName(cmsAnonName);
        final List<Role> roleList = new ArrayList<>();
        if (UtilMethods.isSet(roleId)) {

            final Role role = this.roleAPI.loadRoleById(roleId);
            if (role != null) {

                return Response.ok(new ResponseEntitySmallRoleView(rolesToView(
						List.of(role.getId().equals(cmsAnon.getId())? cmsAnon:role)))).build();
            }
        }

		if (this.fillRoles(searchName, count, start, cmsAnon, cmsAnonName, roleList, includeUserRoles, searchKey)) { // include system user?

            roleList.add(0, cmsAnon);
        }

        if(includeWorkflowRoles) {

            roleList.addAll(APILocator.getRoleAPI().findWorkflowSpecialRoles());
        }

		return Response.ok(new ResponseEntitySmallRoleView(rolesToView(roleList))).build();
	}


	/**
	 * Get all layouts (tool groups) in the system, each enriched with the localized titles of
	 * the portlets it contains.
	 * <p>
	 * Requires an authenticated back-end user with access to the {@code roles} portlet, the same
	 * gate every other endpoint on this resource enforces.
	 *
	 * @return {@link LayoutMapResponseEntityView} List of Layouts
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@Operation(
		operationId = "getAllLayouts",
		summary = "Get all layouts",
		description = "Get all layouts (tool groups) in the system. Requires an authenticated "
				+ "back-end user with access to the Roles portlet."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200",
					description = "Layouts retrieved successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = LayoutMapResponseEntityView.class))),
		@ApiResponse(responseCode = "401",
					description = "Unauthorized - authentication required, or the caller is not a "
							+ "backend user with access to the Roles portlet",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500",
					description = "Internal server error",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@Path("/layouts")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllLayouts(@Context final HttpServletRequest request,
								  @Context final HttpServletResponse response)
			throws DotDataException, LanguageException, DotRuntimeException, PortalException, SystemException {

		final InitDataObject initData = new WebResource.InitBuilder(this.webResource)
				.requiredFrontendUser(false).rejectWhenNoUser(true)
				.requiredBackendUser(true).requiredPortlet("roles")
				.requestAndResponse(request, response).init();
		final User user = initData.getUser();

		final List<Map<String, Object>> layoutsMap = new ArrayList<>();
		final List<Layout> layouts = APILocator.getLayoutAPI().findAllLayouts();

		for(final Layout layout: layouts) {

			final Map<String, Object> layoutMap = layout.toMap();
			layoutMap.put("portletTitles", getPorletTitlesFromLayout(layout, user));
			layoutsMap.add(layoutMap);
		}

		return Response.ok(new LayoutMapResponseEntityView(layoutsMap)).build();
	}

	/**
	 * Given an id (user id or email), if the user exist will retrieve the user roles assigned
	 * This endpoint is only available for Admin Clients or CLients with User|Roles Layout
	 *
	 * @param id String could be the user id or email
	 * @return list of {@link RoleView}
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@Operation(
			operationId = "loadUserRoles",
			summary = "Load user roles",
			description = "Loads the user roles"
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200",
					description = "User roles retrieved successfully",
					content = @Content(mediaType = "application/json",
							schema = @Schema(implementation = ResponseEntityRoleViewListView.class))),
			@ApiResponse(responseCode = "401",
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
			@ApiResponse(responseCode = "403",
					description = "Forbidden - backend user required",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@Path("/users/{userIdOrEmail}")
	@Produces(MediaType.APPLICATION_JSON)
	public ResponseEntityRoleViewListView loadUserRoles(@Context final HttpServletRequest request,
								  @Context final HttpServletResponse response,
								  @Parameter(description = "User id or email", required = true)
								  @DefaultValue("true") @PathParam("userIdOrEmail") final String userIdOrEmail)
			throws DotDataException {

		final User modUser = new WebResource.InitBuilder(this.webResource).requiredBackendUser(true)
				.requiredFrontendUser(false).requestAndResponse(request, response)
				.rejectWhenNoUser(true).init().getUser();

		final boolean isRoleAdministrator = modUser.isAdmin() ||
				(
						APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(PortletID.ROLES.toString(), modUser) &&
						APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(PortletID.USERS.toString(), modUser)
				);

		Logger.debug(this, ()-> "Loading the user roles for: " + modUser);

		if (isRoleAdministrator) {

			User userRecover = Try.of(()->this.userAPI.loadUserById(userIdOrEmail)).getOrNull();
			if (userRecover == null) {
				userRecover = Try.of(()->this.userAPI.loadUserById(userIdOrEmail)).getOrNull();
			}

			if (userRecover == null) {
				userRecover = Try.of(()->this.userAPI.loadByUserByEmail(userIdOrEmail, modUser, false)).getOrNull();
			}

			if (userRecover == null) {
				throw new com.dotmarketing.business.NoSuchUserException("No user found with id: " + userIdOrEmail);
			}

			final List<Role> userRoles = this.roleAPI.loadRolesForUser(userRecover.getUserId());

			return new ResponseEntityRoleViewListView(
					this.roleHelper.toRoleViews(userRoles, false, this.roleAPI));
		}

		final String forbiddenMessage = "The User: " + modUser.getUserId() + " does not have permissions to retrieve users roles";
		Logger.error(this, forbiddenMessage);
		throw new ForbiddenException(forbiddenMessage);
	}

	/**
	 * Resolves the localized title of every portlet in the layout, in the given user's locale.
	 *
	 * @param layout the layout whose portlet titles are wanted
	 * @param user   the authenticated caller; drives the locale of the titles
	 * @return one title per entry in {@link Layout#getPortletIds()}, in the same order
	 */
	private List<String> getPorletTitlesFromLayout (final Layout layout, final User user)
			throws LanguageException, DotRuntimeException, PortalException, SystemException {

		final List<String> portletIds    = layout.getPortletIds();
		final List<String> portletTitles = new ArrayList<>();
		if(portletIds != null) {
			for(final String portletId: portletIds) {

				final String portletTitle = LanguageUtil.get(user,
						"com.dotcms.repackage.javax.portlet.title." + portletId);
				portletTitles.add(portletTitle);
			}
		}

		return portletTitles;
	}

	private boolean fillRoles(final String searchName, final int count, final int startParam,
							  final Role cmsAnon, final String cmsAnonName, final List<Role> roleList,
							  final boolean includeUserRoles, final String searchKey) throws DotDataException {

		boolean addSystemUser = searchName.length() > 0 && cmsAnonName.startsWith(searchName);
		int start = startParam;

		while (roleList.size() < count) {

			final List<Role> roles = StringUtils.isSet(searchKey)?
					this.roleAPI.findRolesByKeyFilterLeftWildcard(searchKey, start, count):
					this.roleAPI.findRolesByFilterLeftWildcard(searchName, start, count);
			if (roles.isEmpty()) {

				break;
			}
			for (Role role : roles) {

				if (role.isUser()) {

					if (!includeUserRoles) {
						continue;
					}

					try {

						APILocator.getUserAPI().loadUserById(role.getRoleKey(), APILocator.systemUser(), false );
					} catch ( Exception e ) {
						continue;
					}
				}

				if (role.getId().equals(cmsAnon.getId())) {

					role = cmsAnon;
					addSystemUser = false;
				}

				if (role.isSystem() &&
						!role.isUser() &&
						!role.getId().equals(cmsAnon.getId()) &&
						!role.getId().equals(APILocator.getRoleAPI().loadCMSAdminRole().getId())) {

					continue;
				}

				if (role.getName().equals(searchName)) {

					roleList.add( 0, role );
				} else {

					roleList.add( role );
				}
			}

			start = start + count;
		}

		return addSystemUser;
	}

	private List<SmallRoleView> rolesToView (final List <Role> roles)
        throws DotDataException, LanguageException {

        final List<SmallRoleView> list = new ArrayList<>();
        final User defaultUser = APILocator.getUserAPI().getDefaultUser();
        Role defaultUserRole   = null;
        if (defaultUser != null) {

            defaultUserRole = APILocator.getRoleAPI().getUserRole(defaultUser);
        }

        for (final Role role : roles) {

			final Map<String, Object> map = new HashMap<>();

            if ((defaultUserRole != null && role.getId().equals(defaultUserRole.getId())) || //Exclude default user
                    (!role.isEditPermissions()) || //We just want to add roles that can have permissions assigned to them
                    (role.getName().contains("anonymous user")) //We need to exclude also the system anonymous user
            ) {
                continue;
            }

            list.add(new SmallRoleView(role.getName() + ((role.isUser()) ? " (" + LanguageUtil.get(APILocator.getCompanyAPI().getDefaultCompany(), "User") + ")" : StringPool.BLANK),
					role.getId(), role.getRoleKey(), role.isUser()));
        }

        return list;
    }
}
