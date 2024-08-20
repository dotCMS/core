package com.dotcms.rest.api.v1.system.role;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

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
	public Response checkRoles(final @Context HttpServletRequest request, final @PathParam("userId") String userId,
			final @PathParam("roleIds") String roleIds) {
		webResource.init(true, request, true);
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
	 * Load role based on the role id.
	 *
	 * @param roleId id of the role to search for.
	 * @param loadChildrenRoles true - will add the data of all children roles of the requested role.
	 * 							false - will only show the data of the requested role.
	 * @return {@link RoleView} role requested.
	 */
	@GET
	@Path("/{roleid}")
	@Produces("application/json")
	public Response loadRoleByRoleId(@Context final HttpServletRequest request,
									 @Context final HttpServletResponse response,
									 @PathParam   ("roleid") final String roleId,
									 @DefaultValue("true") @QueryParam("loadChildrenRoles") final boolean loadChildrenRoles)
			throws DotDataException, DotSecurityException {

		webResource.init(true, request, true);

		try {
			final Role role = this.roleAPI.loadRoleById(roleId);

			if (null == role || !UtilMethods.isSet(role.getId())) {
				throw new DoesNotExistException("The role: " + roleId + " does not exists");
			}

			final List<RoleView> childrenRoles = new ArrayList<>();
			if (loadChildrenRoles) {
				final List<String> roleChildrenIdList = null != role.getRoleChildren() ? role.getRoleChildren() : new ArrayList<>();
				for (final String childRoleId : roleChildrenIdList) {
					childrenRoles.add(new RoleView(this.roleAPI.loadRoleById(childRoleId), new ArrayList<>()));
				}
			}

			return Response.ok(new ResponseEntityView(new RoleView(role, childrenRoles))).build();
		} catch (Exception e) {
			Logger.error(this, "An error occurred when processing the request.", e);
			return ResponseUtil.mapExceptionResponse(e);
		}

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

		webResource.init(true, request, true);

		try {
			final List<RoleView> rootRolesView = new ArrayList<>();
			final List<Role> rootRoles = this.roleAPI.findRootRoles();

			if (loadChildrenRoles) {
				for (final Role role : rootRoles) {
					final List<RoleView> childrenRoles = new ArrayList<>();
					final List<String> roleChildrenIdList =
							null != role.getRoleChildren() ? role.getRoleChildren() : new ArrayList<>();
					for (final String childRoleId : roleChildrenIdList) {
						childrenRoles.add(new RoleView(this.roleAPI.loadRoleById(childRoleId),
								new ArrayList<>()));
					}
					rootRolesView.add(new RoleView(role, childrenRoles));
				}
			} else {
				rootRoles.stream()
						.forEach(role -> rootRolesView.add(new RoleView(role, new ArrayList<>())));
			}

			return Response.ok(new ResponseEntityView(rootRolesView)).build();
		} catch (Exception e) {
			Logger.error(this, "An error occurred when processing the request.", e);
			return ResponseUtil.mapExceptionResponse(e);
		}
	}
}
