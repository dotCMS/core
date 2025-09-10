package com.dotcms.rest.api.v1.system.role;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.WebAPILocator;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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

		final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource)
				.requiredFrontendUser(false).rejectWhenNoUser(true)
				.requiredBackendUser(true).requiredPortlet("roles")
				.requestAndResponse(request, response).init();

		if (this.roleAPI.doesUserHaveRole(initDataObject.getUser(), this.roleAPI.loadCMSAdminRole())) {

			final User user = initDataObject.getUser();
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
						Try.of(()->LanguageUtil.get(initDataObject.getUser(),"Role-Save-Name-Failed")).getOrElse("Role Name not valid"),
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

		final String remoteIp = request.getRemoteHost();
		SecurityLogger.logInfo(UserAjax.class, "unauthorized attempt to call create a role by user "+
				initDataObject.getUser().getUserId() + " from " + remoteIp);
		throw new DotSecurityException("User: '" +  initDataObject.getUser().getUserId() + "' not authorized");
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

		final List<RoleView> childrenRoles = new ArrayList<>();
		if(loadChildrenRoles){
			final List<String> roleChildrenIdList = null!=role.getRoleChildren() ? role.getRoleChildren() : new ArrayList<>();
			for(final String childRoleId : roleChildrenIdList){
				childrenRoles.add(new RoleView(this.roleAPI.loadRoleById(childRoleId),new ArrayList<>()));
			}
		}

		return Response.ok(new ResponseEntityRoleDetailView(new RoleView(role,childrenRoles))).build();

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

		final List<RoleView> rootRolesView = new ArrayList<>();
		final List<Role> rootRoles = this.roleAPI.findRootRoles();

		if(loadChildrenRoles){
			for(final Role role : rootRoles) {
				final List<RoleView> childrenRoles = new ArrayList<>();
				final List<String> roleChildrenIdList =
						null != role.getRoleChildren() ? role.getRoleChildren() : new ArrayList<>();
				for (final String childRoleId : roleChildrenIdList) {
					childrenRoles.add(new RoleView(this.roleAPI.loadRoleById(childRoleId),
							new ArrayList<>()));
				}
				rootRolesView.add(new RoleView(role,childrenRoles));
			}
		} else {
			rootRoles.stream()
					.forEach(role -> rootRolesView.add(new RoleView(role, new ArrayList<>())));
		}

		return Response.ok(new ResponseEntityRoleViewListView(rootRolesView)).build();
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
	 * Get all layouts
	 *
	 * @return {@link LayoutMapResponseEntityView} List of Layouts
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@Operation(
		operationId = "getAllLayouts",
		summary = "Get all layouts",
		description = "Get all layouts in the system"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Layouts retrieved successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = LayoutMapResponseEntityView.class))),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
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

		final List<Map<String, Object>> layoutsMap = new ArrayList<>();
		final List<Layout> layouts = APILocator.getLayoutAPI().findAllLayouts();

		for(final Layout layout: layouts) {

			final Map<String, Object> layoutMap = layout.toMap();
			layoutMap.put("portletTitles", getPorletTitlesFromLayout(layout, request));
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

			final List<RoleView> userRolesView = new ArrayList<>();
			final List<Role> userRoles = this.roleAPI.loadRolesForUser(userRecover.getUserId());

			userRoles.stream()
					.forEach(role -> userRolesView.add(new RoleView(role, new ArrayList<>())));

			return new ResponseEntityRoleViewListView(userRolesView);
		}

		final String forbiddenMessage = "The User: " + modUser.getUserId() + " does not have permissions to retrieve users roles";
		Logger.error(this, forbiddenMessage);
		throw new ForbiddenException(forbiddenMessage);
	}

	private List<String> getPorletTitlesFromLayout (final Layout layout,
													final HttpServletRequest request)
			throws LanguageException, DotRuntimeException, PortalException, SystemException {

		final List<String> portletIds    = layout.getPortletIds();
		final List<String> portletTitles = new ArrayList<>();
		if(portletIds != null) {
			for(final String portletId: portletIds) {

				final String portletTitle = LanguageUtil.get(
						WebAPILocator.getUserWebAPI().getLoggedInUser(request),
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
