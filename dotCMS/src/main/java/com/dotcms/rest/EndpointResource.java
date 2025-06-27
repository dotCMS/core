package com.dotcms.rest;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.PublishingEndPointValidationException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Endpoint for managing PP endpoints
 @author jsanca
 */
@Tag(name = "Environment")
@Path("/v1/environments/endpoints")
public class EndpointResource {

	public static final String THE_USER_KEY = "The user: ";
	private final WebResource webResource = new WebResource();
	private final PublishingEndPointAPI publisherEndPointAPI = APILocator.getPublisherEndPointAPI();

	/**
	 * Returns the endpoints for the current user
	 * if it is admin returns all of them, otherwise returns the ones that the user has access to
	 *
	 * @throws JSONException
	 *
	 */
	@Operation(summary = "Returns the endpoints",
			responses = {
					@ApiResponse(
							responseCode = "200",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation =
											ResponseEntityEndpointsView.class)),
							description = "Collection of environments.")
			})
	@GET
	@Produces("application/json")
	@NoCache
	public ResponseEntityEndpointsView getEndpoints(@Context HttpServletRequest request, @Context final HttpServletResponse response)
			throws DotDataException, JSONException, DotSecurityException {

		final InitDataObject initData = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true)
				.init();

		final User user = initData.getUser();
		final boolean isAdmin = user.isAdmin();

		Logger.debug(this, ()-> "Retrieving PublishingEndPoint for user: " + user.getUserId() + " isAdmin: " + isAdmin);

		return new ResponseEntityEndpointsView(this.publisherEndPointAPI.getAllEndPoints());
	}

	/**
	 * Returns the endpoints for the current user
	 * if it is admin returns all of them, otherwise returns the ones that the user has access to
	 *
	 * @throws JSONException
	 *
	 */
	@Operation(summary = "Returns the endpoints",
			responses = {
					@ApiResponse(
							responseCode = "200",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation =
											ResponseEntityEndpointsView.class)),
							description = "Collection of environments.")
			})
	@GET
	@Path("/environment/{environmentId}")
	@Produces("application/json")
	@NoCache
	public ResponseEntityEndpointsView getEndpointsByEnvironmentId(@Context HttpServletRequest request,
																   @Context final HttpServletResponse response,
																   @PathParam("environmentId") String environmentId)
			throws DotDataException, JSONException, DotSecurityException {

		final InitDataObject initData = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true)
				.init();

		final User user = initData.getUser();
		final boolean isAdmin = user.isAdmin();

		Logger.debug(this, ()-> "Retrieving PublishingEndPoint for user: " + user.getUserId()
				+ " isAdmin: " + isAdmin + " environmentId: " + environmentId);

		return new ResponseEntityEndpointsView(this.publisherEndPointAPI.findSendingEndPointsByEnvironment(environmentId));
	}

    /**
	 * Get an endpoint by id
	 */
	@Operation(summary = "Returns the endpoint by id",
			responses = {
					@ApiResponse(
							responseCode = "200",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation =
											ResponseEntityEndpointView.class)),
							description = "Collection of environments.")
			})
	@GET
	@Path("/{endpointId}")
	@Produces("application/json")
	public ResponseEntityEndpointView getEndpoint(@Context HttpServletRequest request,
												  @Context final HttpServletResponse response,
									 			@PathParam("endpointId") String endpointId)
			throws DotDataException, JSONException {

		final InitDataObject initData = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true)
				.init();

		Logger.debug(this, ()-> "Retrieving PublishingEndPoint for endpointId: " + endpointId);
		final PublishingEndPoint publishingEndPoint = this.publisherEndPointAPI.findEndPointById(endpointId);
		if (Objects.isNull(publishingEndPoint)) {

			throw new DoesNotExistException("Can't find endpoint with id: " + endpointId);
		}
        return new ResponseEntityEndpointView(publishingEndPoint);
	}

	/**
	 * Creates an endpoint and its permissions
	 * If the permission can not be resolved will be just skipped and logged
	 *
	 * @param httpServletRequest
	 * @throws Exception
	 */
	@Operation(summary = "Creates an endpoint",
			responses = {
					@ApiResponse(
							responseCode = "200",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation =
											ResponseEntityEndpointView.class)),
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
							description = "If the endpoint already exits"),
			})
	@POST
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final ResponseEntityEndpointView create(@Context final HttpServletRequest httpServletRequest,
								 @Context final HttpServletResponse httpServletResponse,
								 final EndpointForm endpointForm) throws DotDataException, DotSecurityException, PublishingEndPointValidationException {

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

			final PublishingEndPointAPI publisherEndPointAPI = APILocator.getPublisherEndPointAPI();
			final String endpointName = endpointForm.getName();
			final PublishingEndPoint existingServer = publisherEndPointAPI.findEndPointByName(endpointName);

			if (Objects.nonNull(existingServer)) {

				Logger.info(getClass(), "Can't save endpoint. An endpoint with the given name " + endpointName + " already exists.");
				throw new IllegalArgumentException("An endpoint with the given name " + endpointName + " already exists.");
			}

			Logger.debug(this, ()-> "Creating endpoint: " + endpointName);

			final String protocol = endpointForm.getProtocol();
			final PublishingEndPoint endpoint = publisherEndPointAPI.createEndPoint(protocol);
			endpoint.setServerName(new StringBuilder(endpointName));
			endpoint.setAddress(endpointForm.getAddress());
			endpoint.setPort(endpointForm.getPort());
			endpoint.setProtocol(protocol);
			endpoint.setAuthKey(new StringBuilder(
					PublicEncryptionFactory.encryptString(
							endpointForm.getAuthorizationToken())));
			endpoint.setEnabled(endpointForm.isEnabled());
			endpoint.setSending(endpointForm.isSending());
			endpoint.setGroupId(endpointForm.getEnvironmentId());

			endpoint.validatePublishingEndPoint();

			//Save the endpoint.
			publisherEndPointAPI.saveEndPoint(endpoint);

			return new ResponseEntityEndpointView(endpoint);
		}

		throw new ForbiddenException(THE_USER_KEY + modUser.getUserId() +
				" does not have permissions to create an environment/endpoint");
	} // create.

	/**
	 * Updates an endpoint and its permissions
	 * If the permission can not be resolved will be just skipped and logged
	 *
	 * @param httpServletRequest
	 * @throws Exception
	 */
	@Operation(summary = "Updates an endpoint",
			responses = {
					@ApiResponse(
							responseCode = "200",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation =
											ResponseEntityEndpointView.class)),
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
	public final ResponseEntityEndpointView update(@Context final HttpServletRequest httpServletRequest,
													  @Context final HttpServletResponse httpServletResponse,
													  @PathParam("id") final String id,
													  final EndpointForm endpointForm) throws DotDataException, DotSecurityException, PublishingEndPointValidationException {

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

			return updateEndpoint(httpServletRequest, id, endpointForm, modUser);
		}

		throw new ForbiddenException(THE_USER_KEY + modUser.getUserId() +
				" does not have permissions to update an endpoint");
	} // update.

	private ResponseEntityEndpointView updateEndpoint(final HttpServletRequest httpServletRequest,
														 final String id,
														 final EndpointForm endpointForm,
														 final User modUser) throws DotDataException, DotSecurityException, PublishingEndPointValidationException {

		final PublishingEndPointAPI publishingEndPointAPI = APILocator.getPublisherEndPointAPI();
		final String endpointName = endpointForm.getName();
		final PublishingEndPoint existingEndpoint = publishingEndPointAPI.findEndPointByName(endpointName);

		if (Objects.nonNull(existingEndpoint)
				&& !existingEndpoint.getId().equals(id)) {

			Logger.info(getClass(), "Can't save EndPoint. An Endpoint with the given name: " + endpointName + " already exists. ");
			throw new IllegalArgumentException("Can't save EndPoint. An Endpoint with the given name: " + endpointName + " already exists. ");
		}

		Logger.debug(this, ()-> "Updating endpoint: " + endpointName);

		final String protocol = endpointForm.getProtocol();
		final PublishingEndPoint endpoint = publishingEndPointAPI.createEndPoint(protocol);
		endpoint.setId(id);
		endpoint.setServerName(new StringBuilder(endpointName));
		endpoint.setAddress(endpointForm.getAddress());
		endpoint.setPort(endpointForm.getPort());
		endpoint.setProtocol(protocol);
		endpoint.setAuthKey(new StringBuilder(
				PublicEncryptionFactory.encryptString(
						endpointForm.getAuthorizationToken())));
		endpoint.setEnabled(endpointForm.isEnabled());
		endpoint.setSending(endpointForm.isSending());
		endpoint.setGroupId(endpointForm.getEnvironmentId());

		endpoint.validatePublishingEndPoint();

		//Update the endpoint.
		publishingEndPointAPI.updateEndPoint(endpoint);

		return new ResponseEntityEndpointView(endpoint);
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
	 * Deletes an endpoint and its permissions
	 * If the permission can not be resolved will be just skipped and logged
	 *
	 * @param httpServletRequest
	 * @throws Exception
	 */
	@Operation(summary = "Deletes an endpoint",
			responses = {
					@ApiResponse(
							responseCode = "200",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation =
											ResponseEntityBooleanView.class)),
							description = "If deletion is successfully endpoint."),
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
							description = "If the endpoint does not exits"),
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

			deleteEndpoint(httpServletRequest, id);

			return new ResponseEntityBooleanView(Boolean.TRUE);
		}

		throw new ForbiddenException(THE_USER_KEY + modUser.getUserId() +
				" does not have permissions to delete an endpoint");
	} // delete	.

	private void deleteEndpoint(final HttpServletRequest request, final String id) throws DotDataException {

		final PublishingEndPointAPI publisherEndPointAPI = APILocator.getPublisherEndPointAPI();
		final PublishingEndPoint publishingEndPoint = publisherEndPointAPI.findEndPointById(id);

		if (Objects.isNull(publishingEndPoint)) {

			Logger.info(getClass(), "Can't delete endpoint: " + id + ". An endpoint should exists. ");
			throw new DoesNotExistException("Can't delete endpoint: " + id + ". An endpoint should exists. ");
		}

		Logger.debug(this, ()-> "Deleting endpoint: " + publishingEndPoint.getServerName());

		final String environmentId = publishingEndPoint.getGroupId();

		//Delete the end point
		publisherEndPointAPI.deleteEndPointById(id);

		// if the environment is now empty, lets remove it from session
		if(publisherEndPointAPI.findSendingEndPointsByEnvironment(environmentId).isEmpty()) {
			//If it was deleted successfully lets remove it from session
			if (UtilMethods.isSet(request.getSession().getAttribute(WebKeys.SELECTED_ENVIRONMENTS))) {

				//Get the selected environments from the session
				final List<Environment> lastSelectedEnvironments = (List<Environment>) request.getSession().getAttribute(WebKeys.SELECTED_ENVIRONMENTS);
				final Iterator<Environment> environmentsIterator = lastSelectedEnvironments.iterator();

				while (environmentsIterator.hasNext()) {

					final Environment currentEnv = environmentsIterator.next();
					//Verify if the current env is on the ones stored in session
					if (currentEnv.getId().equals(environmentId)) {
						//If we found it lets remove it
						environmentsIterator.remove();
					}
				}
			}
		}
	}

}
