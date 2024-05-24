package com.dotcms.rest;

import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.site.ResponseSiteVariablesEntityView;
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
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.vavr.control.Try;
import org.apache.commons.lang.StringEscapeUtils;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


@Path("/environment")
public class EnvironmentResource {

    private final WebResource webResource = new WebResource();

	/**
	 * Returns the environments for the current user
	 *
	 * @throws JSONException
	 *
	 */
	@Operation(summary = "Returns the environments",
			responses = {
					@ApiResponse(
							responseCode = "200",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation =
											ResponseEntityEnvironmentsView.class)),
							description = "Collection of environments.")
			})
	@GET
	@Produces("application/json")
	@NoCache
	public ResponseEntityEnvironmentsView loadAllEnvironments(@Context HttpServletRequest request, @Context final HttpServletResponse response)
			throws DotDataException, JSONException, DotSecurityException {

		final InitDataObject initData = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true)
				.init();

		final User user = initData.getUser();
		final boolean isAdmin = user.isAdmin();
		final Set<Environment> environments = new HashSet<>();

		Logger.debug(this, ()-> "Retrieving environments for user: " + user.getUserId() + " isAdmin: " + isAdmin);
		if (isAdmin) {

			final List<Environment> environmentList =
					APILocator.getEnvironmentAPI().findAllEnvironments();
			for (final Environment environment : environmentList){
				environments.add(environment);
			}
		} else {

			final List<Role> roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId(), true);
			for (Role role : roles) {
				environments.addAll(APILocator.getEnvironmentAPI()
						.findEnvironmentsByRole(role.getId()));
			}
		}

		return new ResponseEntityEnvironmentsView(environments);
	}

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
	 * Creates an env and its permissions
	 * If the permission can not be resolved will be just skipped and logged
	 *
	 * @param httpServletRequest
	 * @throws Exception
	 */
	@Operation(summary = "Creates an environment",
			responses = {
					@ApiResponse(
							responseCode = "200",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation =
											ResponseEntityEnvironmentView.class)),
							description = "If success environment information."),
					@ApiResponse(
							responseCode = "403",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation =
											ForbiddenException.class)),
							description = "If the user is not an admin or access to the configuration layout or does have permission, it will return a 403."),
					@ApiResponse(
							responseCode = "400",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation =
											IllegalArgumentException.class)),
							description = "If the environment already exits"),
			})
	@POST
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final ResponseEntityEnvironmentView create(@Context final HttpServletRequest httpServletRequest,
								 @Context final HttpServletResponse httpServletResponse,
								 final EnvironmentForm environmentForm) throws DotDataException, DotSecurityException {

		final User modUser = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.requestAndResponse(httpServletRequest, httpServletResponse)
				.rejectWhenNoUser(true)
				.init().getUser();

		final boolean isRoleAdministrator = modUser.isAdmin() ||
						APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(
								PortletID.CONFIGURATION.toString(), modUser);

		if (isRoleAdministrator) {

			final String environmentName = environmentForm.getName();
			final Environment existingEnvironment = APILocator.getEnvironmentAPI().
					findEnvironmentByName(environmentName);

			if (Objects.nonNull(existingEnvironment)) {

				Logger.info(getClass(), "Can't save Environment. An Environment with the given name already exists. ");
				throw new IllegalArgumentException("An Environment with the given name" + environmentName + " already exist.");
			}

			final List<String> whoCanUseList = environmentForm.getWhoCanUse();
			final String pushType = environmentForm.getPushType();

			final Environment environment = new Environment();
			environment.setName(environmentName);
			environment.setPushToAll("pushToAll".equals(pushType));

			final Map<String, Permission> permissionsMap = new HashMap<>();

			for (final String permissionKey : whoCanUseList) {

				if (UtilMethods.isSet(permissionKey)) {

					final Role role = resolveRole(permissionKey, modUser);
					if (Objects.nonNull(role)) {

						permissionsMap.computeIfAbsent(role.getId(), k -> new Permission(
								environment.getId(), role.getId(), PermissionAPI.PERMISSION_USE));
					} else {

						Logger.warn(getClass(), "Role not found for key: " + permissionKey);
					}
				}
			}

			APILocator.getEnvironmentAPI().saveEnvironment(environment,
					new ArrayList<>(permissionsMap.values()));

			return new ResponseEntityEnvironmentView(environment);
		}

		throw new ForbiddenException("The user: " + modUser.getUserId() +
				" does not have permissions to update users");
	} // create.

	/**
	 * Test by roleIds, roleKeys, userIds and/or user emails
	 * @param whoCanUseToken
	 * @return
	 * @throws DotDataException
	 */

	private Role resolveRole(final String whoCanUseToken, final User modUser) {

		Role role = Try.of(()->APILocator.getRoleAPI().loadRoleById(whoCanUseToken)).getOrNull();

		if (Objects.isNull(role)) {

			role = Try.of(()->APILocator.getRoleAPI().loadRoleByKey(whoCanUseToken)).getOrNull();
		}

		if (Objects.isNull(role)) {

			final User user = Try.of(()->APILocator.getUserAPI().loadUserById(whoCanUseToken)).getOrNull();
			if (Objects.nonNull(user)) {

				role = user.getUserRole();
			}
		}

		if (Objects.isNull(role)) {

			final User user = Try.of(()->APILocator.getUserAPI().loadByUserByEmail(whoCanUseToken, modUser, false)).getOrNull();
			if (Objects.nonNull(user)) {

				role = user.getUserRole();
			}
		}

		return role;
	}
}
