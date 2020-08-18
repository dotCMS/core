package com.dotcms.rest.api.v1.system.role;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.user.ajax.UserAjax;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;

import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.Set;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

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
	@GET
	@Path("/checkuserroles/userid/{userId}/roleids/{roleIds}")
	@Produces("application/json")
	public Response checkRoles(final @Context HttpServletRequest request,
							   final @Context HttpServletResponse response,
							   final @PathParam("userId") String userId,
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

		return Response.ok(new ResponseEntityView(map("checkRoles", hasUserRole))).build();
	}

	/**
	 * Deletes a set of layouts into a role
	 * The user must have to be a BE and has to have access to roles portlet
	 */
	@DELETE
	@Path("/layouts")
	@Produces("application/json")
	public Response deleteRoleLayouts(
			final @Context HttpServletRequest request,
			final @Context HttpServletResponse response,
			final RoleLayoutForm roleLayoutForm) throws DotDataException, DotSecurityException {

		final InitDataObject initDataObject = new WebResource.InitBuilder()
				.requiredFrontendUser(false).rejectWhenNoUser(true)
				.requiredBackendUser(true).requiredPortlet("roles")
				.requestAndResponse(request, response).init();

		if (this.roleAPI.doesUserHaveRole(initDataObject.getUser(), this.roleAPI.loadCMSAdminRole())) {

			final String roleId         = roleLayoutForm.getRoleId();
			final Set<String> layoutIds = roleLayoutForm.getLayoutIds();
			final Role role 			= roleAPI.loadRoleById(roleId);
			final LayoutAPI layoutAPI   = APILocator.getLayoutAPI();

			Logger.debug(this, ()-> "Deleting the layouts : " + layoutIds + " to the role: " + roleId);

			return Response.ok(new ResponseEntityView(map("deletedLayouts",
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
	 * Saves set of layout into a role
	 * The user must have to be a BE and has to have access to roles portlet
	 */
	@POST
	@Path("/layouts")
	@Produces("application/json")
	public Response saveRoleLayouts(
			final @Context HttpServletRequest request,
			final @Context HttpServletResponse response,
			final RoleLayoutForm roleLayoutForm) throws DotDataException, DotSecurityException {

		final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource)
				.requiredFrontendUser(false).rejectWhenNoUser(true)
				.requiredBackendUser(true).requiredPortlet("roles")
				.requestAndResponse(request, response).init();

		if (this.roleAPI.doesUserHaveRole(initDataObject.getUser(), this.roleAPI.loadCMSAdminRole())) {

			final String roleId         = roleLayoutForm.getRoleId();
			final Set<String> layoutIds = roleLayoutForm.getLayoutIds();
			final Role role 			= roleAPI.loadRoleById(roleId);
			final LayoutAPI layoutAPI   = APILocator.getLayoutAPI();

			Logger.debug(this, ()-> "Saving the layouts : " + layoutIds + " to the role: " + roleId);

			return Response.ok(new ResponseEntityView(map("savedLayouts",
					this.roleHelper.saveRoleLayouts(role, layoutIds, layoutAPI,
							this.roleAPI, APILocator.getSystemEventsAPI())))).build();
		} else {

			final String remoteIp = request.getRemoteHost();
			SecurityLogger.logInfo(UserAjax.class, "unauthorized attempt to call save role layouts by user "+
					initDataObject.getUser().getUserId() + " from " + remoteIp);
			throw new DotSecurityException("User: '" +  initDataObject.getUser().getUserId() + "' not authorized");
		}
	}

	/**
	 * Returns a collection of layouts associated to a role
	 * The user must have to be a BE and has to have access to roles portlet
	 */
	@GET
	@Path("/{roleId}/layouts")
	@Produces("application/json")
	public Response findRoleLayouts(
			final @Context HttpServletRequest request,
			final @Context HttpServletResponse response,
			final @PathParam("roleId") String roleId) throws DotDataException {

		new WebResource.InitBuilder(this.webResource)
				.requiredFrontendUser(false).rejectWhenNoUser(true)
				.requiredBackendUser(true).requiredPortlet("roles")
				.requestAndResponse(request, response).init();

		Logger.debug(this, ()-> "Finding the role layouts for the roleid: " + roleId);
		final Role role              = roleAPI.loadRoleById(roleId);
		final LayoutAPI layoutAPI    = APILocator.getLayoutAPI();

		return Response.ok(new ResponseEntityView(
				layoutAPI.loadLayoutsForRole(role)
		)).build();
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
	@GET
	@Path("/{roleid}/rolehierarchyanduserroles")
	@Produces("application/json")
	@SuppressWarnings("unchecked")
	public Response loadUsersAndRolesByRoleId(@Context final HttpServletRequest request,
			@Context final HttpServletResponse response,
			@PathParam   ("roleid") final String roleId,
			@DefaultValue("false") @QueryParam("roleHierarchyForAssign") final boolean roleHierarchyForAssign,
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

		return Response.ok(new ResponseEntityView(
				null != roleNameToFilter? this.filterRoleList(roleNameToFilter, roleList):roleList)).build();
	}

	private final List<Role> filterRoleList(final String roleNameToFilter, final List<Role> roleList) {

		final String roleNameToFilterClean = roleNameToFilter.toLowerCase().replaceAll( "\\*", StringPool.BLANK);
		return UtilMethods.isSet(roleNameToFilterClean)?
				roleList.stream().filter(myRole -> myRole.getName().toLowerCase()
						.startsWith(roleNameToFilterClean)).collect(Collectors.toList()):
				roleList;
	}
}
