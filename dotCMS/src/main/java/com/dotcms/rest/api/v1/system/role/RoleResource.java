package com.dotcms.rest.api.v1.system.role;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.util.Logger;
import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

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

}
