package com.dotcms.rest.api.v1.system.role;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.system.permission.ResponseEntityPermissionView;
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

import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.apache.commons.beanutils.BeanUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

		return Response.ok(new ResponseEntityView<>(
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

		return Response.ok(new ResponseEntityView<List<Role>>(
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
	@GET
	@Path("/{roleid}")
	@Produces("application/json")
	public Response loadRoleByRoleId(@Context final HttpServletRequest request,
			@Context final HttpServletResponse response,
			@PathParam   ("roleid") final String roleId,
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

		return Response.ok(new ResponseEntityView(new RoleView(role,childrenRoles))).build();

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
	@GET
	@Produces("application/json")
	public Response loadRootRoles(@Context final HttpServletRequest request,
			@Context final HttpServletResponse response,
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

		return Response.ok(new ResponseEntityView<>(rootRolesView)).build();
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
	@Operation(summary = "Search Roles",
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
