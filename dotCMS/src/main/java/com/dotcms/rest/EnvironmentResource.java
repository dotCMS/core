package com.dotcms.rest;

import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.IncorrectPasswordException;
import com.dotcms.rest.api.v1.site.ResponseSiteVariablesEntityView;
import com.dotcms.rest.api.v1.user.UserForm;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.apache.commons.lang.StringEscapeUtils;
import org.glassfish.jersey.server.JSONP;


@Path("/environment")
public class EnvironmentResource {

    private final WebResource webResource = new WebResource();

    /**
	 * <p>Returns a JSON representation of the environments (with servers) that the Role with the given roleid can push to
	 * <br>Each Environment node contains: id, name.
	 *
	 * Usage: /loadenvironments/{roleid}
	 * @throws JSONException
	 *
	 */

	@GET
	@Path("/loadenvironments/{params:.*}")
	@Produces("application/json")
	public Response loadEnvironments(@Context HttpServletRequest request, @Context final HttpServletResponse response, @PathParam("params") String params)
			throws DotDataException, JSONException {

		final InitDataObject initData = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.params(params)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true)
				.init();

        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

		String roleId = initData.getParamsMap().get("roleid");

		//Using JsonArray instead of manually creating the json object
		JSONArray jsonEnvironments = new JSONArray();

		//First objects is expected to be blank
		JSONObject jsonEnvironmentFirst = new JSONObject();
		jsonEnvironmentFirst.put( "id", "0" );
		jsonEnvironmentFirst.put( "name", "");

		jsonEnvironments.add(jsonEnvironmentFirst);

		try {
			Role role = APILocator.getRoleAPI().loadRoleById(roleId);
			User user = APILocator.getUserAPI().loadUserById(role.getRoleKey());
			boolean isAdmin = APILocator.getUserAPI().isCMSAdmin(user);

			List<Role> roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId(), true);
			Set<Environment> environments = new HashSet<>();
			if (isAdmin) {
				List<Environment> app = APILocator.getEnvironmentAPI()
						.findEnvironmentsWithServers();
				for (Environment e : app){
					environments.add(e);
				}
			} else {
				for (Role r : roles) {
					environments.addAll(APILocator.getEnvironmentAPI()
							.findEnvironmentsByRole(r.getId()));
				}
			}

			//For each env, create one json and add it to the array
			for (Environment e : environments) {

				JSONObject environmentBundle = new JSONObject();
				environmentBundle.put("id", e.getId());
				//Escape name for cases like: dotcms's
				environmentBundle.put("name", StringEscapeUtils.unescapeJava(e.getName()));

				jsonEnvironments.add(environmentBundle);
			}

			CacheControl cc = new CacheControl();
			cc.setNoCache(true);
			return Response.ok(jsonEnvironments.toString(), MediaType.APPLICATION_JSON_TYPE)
					.cacheControl(cc).build();
		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);
		}
	}

	/**
	 * Creates an env and his permissions
	 *
	 * @param httpServletRequest
	 * @throws Exception
	 */
	@Operation(summary = "Update an existing user.",
			responses = {
					@ApiResponse(
							responseCode = "200",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation =
											ResponseSiteVariablesEntityView.class)),
							description = "If success returns a map with the user + user id."),
					@ApiResponse(
							responseCode = "403",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation =
											ResponseSiteVariablesEntityView.class)),
							description = "If the user is not an admin or access to the role + user layouts or does have permission, it will return a 403."),
					@ApiResponse(
							responseCode = "404",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation =
											ResponseSiteVariablesEntityView.class)),
							description = "If the user to update does not exist"),
					@ApiResponse(
							responseCode = "400",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation =
											ResponseSiteVariablesEntityView.class)),
							description = "If the user information is not valid"),
			})
	@POST
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response udpate(@Context final HttpServletRequest httpServletRequest,
								 @Context final HttpServletResponse httpServletResponse,
								 final UserForm createUserForm) throws DotDataException, DotSecurityException {

		final User modUser = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.requestAndResponse(httpServletRequest, httpServletResponse)
				.rejectWhenNoUser(true)
				.init().getUser();

		final boolean isRoleAdministrator = modUser.isAdmin() ||
						APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(PortletID.CONFIGURATION.toString(), modUser);

		if (isRoleAdministrator) {
			final String environmentName = null;


			final Environment existingEnvironment = APILocator.getEnvironmentAPI().findEnvironmentByName(environmentName);

			if (Objects.isNull(existingEnvironment)) {
				Logger.info(getClass(), "Can't save Environment. An Environment with the given name already exists. ");
				// 404
			}

			final String whoCanUseTmp = null;//request.getParameter("whoCanUse");
			final String pushType = null; // request.getParameter("pushType")
			final Environment environment = new Environment();
			environment.setName(environmentName);
			environment.setPushToAll("pushToAll".equals(pushType));

			List<String> whoCanUse = Arrays.asList(whoCanUseTmp.split(","));
			List<Permission> permissions = new ArrayList<>();

			for (String perm : whoCanUse) {
				if (!UtilMethods.isSet(perm)) {
					continue;
				}

				Role test = null;//resolveRole(perm);
				Permission p = new Permission(environment.getId(), test.getId(), PermissionAPI.PERMISSION_USE);

				boolean exists = false;
				for (Permission curr : permissions)
					exists = exists || curr.getRoleId().equals(p.getRoleId());

				if (!exists)
					permissions.add(p);
			}


			EnvironmentAPI eAPI = APILocator.getEnvironmentAPI();
			eAPI.saveEnvironment(environment, permissions);

		}

		throw new ForbiddenException("The user: " + modUser.getUserId() + " does not have permissions to update users");
	} // create.
}
