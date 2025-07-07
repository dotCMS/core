package com.dotcms.rest;

import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.vavr.control.Try;
import org.apache.commons.lang.StringEscapeUtils;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Endpoint for managing environments
 @author jsanca
 */
@Path("/environment")
@Tag(name = "Environment")
public class EnvironmentResource {

	public static final String THE_USER_KEY = "The user: ";
	private final WebResource webResource = new WebResource();

	/**
	 * Returns the environments for the current user
	 * if it is admin returns all of them, otherwise returns the ones that the user has access to
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
			for (final Environment environment : environmentList) {
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
							description = "If creation is successfully."),
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
				throw new IllegalArgumentException("An Environment with the given name " + environmentName + " already exists.");
			}

			Logger.debug(this, ()-> "Creating environment: " + environmentName);

			final List<String> whoCanUseList = environmentForm.getWhoCanSend();
			final PushMode pushType = environmentForm.getPushMode();

			final Environment environment = new Environment();
			environment.setName(environmentName);
			environment.setPushToAll(PushMode.PUSH_TO_ALL == pushType);

			final Map<String, Permission> permissionsMap = new HashMap<>();

			if (Objects.nonNull(whoCanUseList)) {
				processRoles(whoCanUseList, modUser, permissionsMap, environment);
			}

			APILocator.getEnvironmentAPI().saveEnvironment(environment,
					new ArrayList<>(permissionsMap.values()));

			return new ResponseEntityEnvironmentView(environment);
		}

		throw new ForbiddenException(THE_USER_KEY + modUser.getUserId() +
				" does not have permissions to create an environment");
	} // create.

	/**
	 * Updates an env and its permissions
	 * If the permission can not be resolved will be just skipped and logged
	 *
	 * @param httpServletRequest
	 * @throws Exception
	 */
	@Operation(summary = "Updates an environment",
			responses = {
					@ApiResponse(
							responseCode = "200",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation =
											ResponseEntityEnvironmentView.class)),
							description = "If update is success."),
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
	@PUT
	@Path("/{id}")
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final ResponseEntityEnvironmentView update(@Context final HttpServletRequest httpServletRequest,
													  @Context final HttpServletResponse httpServletResponse,
													  @PathParam("id") final String id,
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

			return updateEnv(httpServletRequest, id, environmentForm, modUser);
		}

		throw new ForbiddenException(THE_USER_KEY + modUser.getUserId() +
				" does not have permissions to update an environment");
	} // update.

	private ResponseEntityEnvironmentView updateEnv(final HttpServletRequest httpServletRequest, 
													final String id, 
													final EnvironmentForm environmentForm, 
													final User modUser) throws DotDataException, DotSecurityException {
		
		final String environmentName = environmentForm.getName();
		final Environment existingEnvironmentById = APILocator.getEnvironmentAPI()
				.findEnvironmentById(id);

		final Environment existingEnvironmentByName = APILocator.getEnvironmentAPI()
				.findEnvironmentByName(environmentName);
		if (Objects.isNull(existingEnvironmentById)
				|| !existingEnvironmentById.getId().equals(id)) {

			Logger.info(getClass(), "Can't save Environment. An Environment id that does not exist. ");
			throw new IllegalArgumentException("An Environment with the given id " + id + " does not exist.");
		}

		if (Objects.nonNull(existingEnvironmentByName) // if trying to give an existing name
				&& (!existingEnvironmentByName.getId().equals(id) && existingEnvironmentByName.getName().equals(environmentName))) {

			Logger.info(getClass(), "Can't save Environment. An Environment with the given name already exists. ");
			throw new IllegalArgumentException("An Environment with the given name " + environmentName + " already exists.");
		}

		Logger.debug(this, ()-> "Updating environment: " + environmentName);

		final List<String> whoCanUseList = environmentForm.getWhoCanSend();
		final PushMode pushType = environmentForm.getPushMode();

		final Environment environment = new Environment();
		environment.setId(id);
		environment.setName(environmentName);
		environment.setPushToAll(PushMode.PUSH_TO_ALL == pushType);

		final Map<String, Permission> permissionsMap = new HashMap<>();

		if (Objects.nonNull(whoCanUseList)) {
			processRoles(whoCanUseList, modUser, permissionsMap, environment);
		}

		APILocator.getEnvironmentAPI().updateEnvironment(environment,
				new ArrayList<>(permissionsMap.values()));

		//If it was updated successfully lets set the session
		updateSelectEnv(httpServletRequest, modUser, environment);

		return new ResponseEntityEnvironmentView(environment);
	}

	private static void updateSelectEnv(HttpServletRequest httpServletRequest, User modUser, Environment environment) {
		if (UtilMethods.isSet(httpServletRequest.getSession().getAttribute(
				WebKeys.SELECTED_ENVIRONMENTS + modUser.getUserId()))) {

			//Get the selected environments from the session
			final List<Environment> lastSelectedEnvironments = (List<Environment>) httpServletRequest.getSession()
					.getAttribute( WebKeys.SELECTED_ENVIRONMENTS + modUser.getUserId() );

			if (Objects.nonNull(lastSelectedEnvironments)) {
				for (int i = 0; i < lastSelectedEnvironments.size(); ++i) {
					//Verify if the current env is on the ones stored in session
					final Environment currentEnv = lastSelectedEnvironments.get(i);
					if (currentEnv.getId().equals(environment.getId())) {
						lastSelectedEnvironments.set(i, environment);
					}
				}
			}
		}
	}

	/**
	 * Deletes an env and its permissions
	 * If the permission can not be resolved will be just skipped and logged
	 *
	 * @param httpServletRequest
	 * @throws Exception
	 */
	@Operation(summary = "Deletes an environment",
			responses = {
					@ApiResponse(
							responseCode = "200",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation =
											ResponseEntityBooleanView.class)),
							description = "If deletion is successfully environment."),
					@ApiResponse(
							responseCode = "403",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation =
											ForbiddenException.class)),
							description = "If the user is not an admin or access to the configuration layout or does have permission, it will return a 403."),
					@ApiResponse(
							responseCode = "404",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation =
											DoesNotExistException.class)),
							description = "If the environment does not exits"),
			})
	@DELETE
	@Path("/{id}")
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final ResponseEntityBooleanView delete(@Context final HttpServletRequest httpServletRequest,
													  @Context final HttpServletResponse httpServletResponse,
													  @PathParam("id") final String id) throws DotDataException {

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

			deleteEnv(httpServletRequest, id, modUser);

			return new ResponseEntityBooleanView(Boolean.TRUE);
		}

		throw new ForbiddenException(THE_USER_KEY + modUser.getUserId() +
				" does not have permissions to delete an environment");
	} // delete	.

	private void deleteEnv(final HttpServletRequest httpServletRequest, final String id, final User modUser) throws DotDataException {

		final Environment existingEnvironment = APILocator.getEnvironmentAPI().
				findEnvironmentById(id);

		if (Objects.isNull(existingEnvironment)) {

			Logger.info(getClass(), "Can't delete Environment: " + id + ". An Environment should exists. ");
			throw new DoesNotExistException("Can't delete Environment: " + id + ". An Environment should exists. ");
		}

		Logger.debug(this, ()-> "Deleting environment: " + existingEnvironment.getName());

		APILocator.getEnvironmentAPI().deleteEnvironment(id);

		//If it was updated successfully lets set the session
		removeSelectedEnv(httpServletRequest, id, modUser);
	}

	private void removeSelectedEnv(final HttpServletRequest httpServletRequest,
								   final String id, final User modUser) {

		if (UtilMethods.isSet(httpServletRequest.getSession().getAttribute(
				WebKeys.SELECTED_ENVIRONMENTS + modUser.getUserId()))) {

			//Get the selected environments from the session
			final List<Environment> lastSelectedEnvironments = (List<Environment>) httpServletRequest.getSession()
					.getAttribute( WebKeys.SELECTED_ENVIRONMENTS + modUser.getUserId() );

			if (Objects.nonNull(lastSelectedEnvironments)) {
				final Iterator<Environment> environmentsIterator = lastSelectedEnvironments.iterator();

				while (environmentsIterator.hasNext()) {

					final Environment currentEnv = environmentsIterator.next();
					//Verify if the current env is on the ones stored in session
					if (currentEnv.getId().equals(id) ) {
						//If we found it lets remove it
						environmentsIterator.remove();
					}
				}
			}
		}
	}

	private void processRoles(final List<String> whoCanUseList,
							  final User modUser,
							  final Map<String, Permission> permissionsMap,
							  final Environment environment) {

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
	}

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
