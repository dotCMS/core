package com.dotcms.rest.api.v1.user;

import static com.dotcms.util.CollectionsUtils.getMapValue;
import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ErrorResponseHelper;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.IncorrectPasswordException;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.UserFirstNameException;
import com.dotmarketing.exception.UserLastNameException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.LocaleUtil;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
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
import org.glassfish.jersey.server.JSONP;

/**
 * This end-point provides access to information associated to dotCMS users.
 *
 * @author Geoff Granum
 * @version 3.5, 3.7
 * @since Mar 29, 2016
 *
 */
@Path("/v1/users")
@SuppressWarnings("serial")
public class UserResource implements Serializable {

	private final WebResource webResource;
	private final UserAPI userAPI;
	private final HostAPI siteAPI;
	private final UserResourceHelper helper;
	private final ErrorResponseHelper errorHelper;

	/**
	 * Default class constructor.
	 */
	public UserResource() {
		this(new WebResource(new ApiProvider()), APILocator.getUserAPI(), APILocator.getHostAPI(), UserResourceHelper.getInstance(),
				ErrorResponseHelper.INSTANCE);
	}

	@VisibleForTesting
	protected UserResource(final WebResource webResource,  final UserAPI userAPI, final HostAPI siteAPI, final UserResourceHelper userHelper,
						   final ErrorResponseHelper errorHelper) {
		this.webResource = webResource;
		this.userAPI = userAPI;
		this.siteAPI = siteAPI;
		this.helper = userHelper;
		this.errorHelper = errorHelper;
	}

	/**
	 *
	 * @param request
	 * @return
	 */
	@GET
	@JSONP
	@Path("/current")
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public RestUser self(@Context final  HttpServletRequest request, final @Context HttpServletResponse response) {

		final User user = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true)
				.init().getUser();

		final RestUser.Builder currentUser = new RestUser.Builder();

		if(user != null) {
			try {
				final Role role = APILocator.getRoleAPI().getUserRole(user);
				currentUser.userId(user.getUserId())
						.givenName(user.getFirstName())
						.email(user.getEmailAddress())
						.surname(user.getLastName())
						.roleId(role.getId());
			} catch (final DotDataException e) {
				Logger.error(this, "Could not provide current user: " + e.getMessage(), e);
				throw new BadRequestException("Could not provide current user.");
			}
		}

		return currentUser.build();
	}

	/**
	 *
	 * @param httpServletRequest
	 * @param updateUserForm
	 * @return
	 * @throws Exception
	 */
	@PUT
	@JSONP
	@Path("/current")
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response update(@Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse,
								 final UpdateUserForm updateUserForm) throws Exception {

		final User modUser = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.requestAndResponse(httpServletRequest, httpServletResponse)
				.rejectWhenNoUser(true)
				.init().getUser();

		Response response;
		final String date = DateUtil.getCurrentDate();
		final User userToUpdated;
		Locale locale = LocaleUtil.getLocale(httpServletRequest);
		final Locale systemLocale = this.userAPI.getSystemUser().getLocale();
		Map<String, Object> userMap = Collections.EMPTY_MAP;

		this.helper.log("Updating User", "Date: " + date + "; "
				+ "User:" + modUser.getUserId());

		try {

			if (null == locale) {
				locale = modUser.getLocale();
			}

			userToUpdated = this.helper.updateUser(updateUserForm, modUser, httpServletRequest, locale);
			this.helper.log("User Updated", "Date: " + date + "; "+ "User:" + modUser.getUserId());

			boolean reAuthenticationRequired =  null != updateUserForm.getNewPassword();

			if (!reAuthenticationRequired) { // if re authentication is not required, sent the current changed user
				userMap = userToUpdated.toMap();
			}

			response = Response.ok(new ResponseEntityView(map("userID", userToUpdated.getUserId(),
					"reauthenticate", reAuthenticationRequired, "user", userMap))).build(); // 200
		} catch (final UserFirstNameException e) {

			this.helper.log("Error Updating User. Invalid First Name", "Date: " + date + ";  "+ "User:" + modUser.getUserId());
			response = this.errorHelper.getErrorResponse(Response.Status.BAD_REQUEST, locale, "User-Info-Save-First-Name-Failed");
		} catch (final UserLastNameException e) {

			this.helper.log("Error Updating User. Invalid Last Name", "Date: " + date + ";  "+ "User:" + modUser.getUserId());
			response = this.errorHelper.getErrorResponse(Response.Status.BAD_REQUEST, locale, "User-Info-Save-Last-Name-Failed");
		} catch (final DotSecurityException  e) {

			this.helper.log("Error Updating User. "+e.getMessage(), "Date: " + date + ";  "+ "User:" + modUser.getUserId());
			response = this.errorHelper.getErrorResponse(Response.Status.UNAUTHORIZED, locale, "User-Doesnot-Have-Permission");
		} catch (final NoSuchUserException  e) {

			this.helper.log("Error Updating User. "+e.getMessage(), "Date: " + date + ";  "+ "User:" + modUser.getUserId());
			response = this.errorHelper.getErrorResponse(Response.Status.NOT_FOUND, locale, "User-Not-Found");
		} catch (final DotDataException e) {
			if(null != e.getMessageKey()){
				this.helper.log("Error Updating User. "+e.getFormattedMessage(systemLocale), "Date: " + date + ";  "+ "User:" + modUser.getUserId());
				response = this.errorHelper.getErrorResponse(Response.Status.BAD_REQUEST, locale, e.getMessageKey());
			} else{
				this.helper.log("Error Updating User. "+e.getMessage(), "Date: " + date + ";  "+ "User:" + modUser.getUserId());
				response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
			}
		} catch (final IncorrectPasswordException e) {
			this.helper.log("Error Updating User. " + e.getMessage(), "Date: " + date + ";  "+ "User:" + modUser.getUserId());
			response = ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
		} catch (final Exception  e) {
			this.helper.log("Error Updating User. "+e.getMessage(), "Date: " + date + ";  "+ "User:" + modUser.getUserId());
			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	} // update.

	/**
	 * Returns a list of dotCMS users based on the specified search criteria.
	 * Depending on the filtering values, 2 types of result can be returned:
	 * <ol>
	 * <li>If the {@code assetInode} and {@code permission} parameters <b>are
	 * specified</b>, this method will return a list of users that have the
	 * specified permission type on the specified asset Inode.</li>
	 * <li>If the {@code assetInode} and {@code permission} parameters <b>are
	 * NOT specified</b>, this method will return a list of users based on
	 * specific filtering parameters (see
	 * {@link UserResourceHelper#getUserList(String, String, Map)}).</li>
	 * </ol>
	 * <p>
	 * The parameters for this REST call are the following:
	 * <ul>
	 * <li>{@code assetInode}</li>
	 * <li>{@code permission}</li>
	 * <li>{@code query}</li>
	 * <li>{@code start}</li>
	 * <li>{@code limit}</li>
	 * <li>{@code includeAnonymous}</li>
	 * <li>{@code includeDefault}</li>
	 * </ul>
	 * <p>
	 * Example #1
	 *
	 * <pre>
	 * http://localhost:8080/api/v1/users/filter/permission/1/start/0/limit/30/includeAnonymous/false/includeDefault/false
	 * </pre>
	 *
	 * Example #2
	 *
	 * <pre>
	 * http://localhost:8080/api/v1/users/filter/assetInode/6e13c345-4599-49d0-aa47-6a7e59245247/permission/1/query/John/start/0/limit/10/includeAnonymous/false/includeDefault/false
	 * </pre>
	 *
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @param params
	 *            - The parameters that can be specified in the REST call.
	 * @return A {@link Response} containing the list of dotCMS users that match
	 *         the filtering criteria.
	 */
	@GET
	@JSONP
	@Path("/filter/{params:.*}")
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public Response filter(@Context final HttpServletRequest request, @Context final HttpServletResponse response, @PathParam("params") final String params) {

		final InitDataObject initData = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.params(params)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true)
				.init();

		final Map<String, String> urlParams = initData.getParamsMap();
		Map<String, Object> userList;
		try {
			final Map<String, String> filterParams = map(
					"query", urlParams.get("query"),
					"start", getMapValue(urlParams, "start", "0"),
					"limit", getMapValue(urlParams, "limit", "-1"),
					"includeAnonymous", getMapValue(urlParams, "includeAnonymous", "false"),
					"includeDefault", getMapValue(urlParams, "includeDefault", "false"));
			userList = this.helper.getUserList(urlParams.get("assetInode"), urlParams.get("permission"), filterParams);
		} catch (final Exception e) {
			// In case of unknown error, a Status 500 is returned
			Logger.error(this, "An error occurred when filtering the list of Login As users: " + e.getMessage(), e);
			return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
		return Response.ok(new ResponseEntityView(userList)).build();
	}

	/**
	 * Performs all the changes in the {@link HttpSession} that are required to
	 * simulate another user's login via the 'Login As' feature in dotCMS.
	 * <p>
	 * The parameters for this REST call are the following:
	 * <ul>
	 * <li>{@code userid}</li>
	 * <li>{@code pwd}</li>
	 * </ul>
	 * <p>
	 * Example #1: Login as non-admin user.
	 *
	 * <pre>
	 * http://localhost:8080/api/v1/users/loginas/userid/dotcms.org.2789
	 * </pre>
	 *
	 * Example #2: Login as admin user.
	 *
	 * <pre>
	 * http://localhost:8080/api/v1/users/loginas/userid/dotcms.org.2/pwd/admin
	 * </pre>
	 *
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 *            - The parameters that can be specified in the REST call.
	 * @return A {@link Response} containing the status of the operation. This
	 *         will probably require a page refresh.
	 * @throws Exception An error occurred when authenticating the request.
	 */
	@POST
	@Path("/loginas")
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public final Response loginAs(@Context final HttpServletRequest request, @Context final HttpServletResponse httpResponse, final LoginAsForm loginAsForm) throws Exception {
		final String loginAsUserId = loginAsForm.getUserId();
		final String loginAsUserPwd = loginAsForm.getPassword();

		final InitDataObject initData = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.credentials(loginAsUserId,loginAsUserPwd)
				.requestAndResponse(request, httpResponse)
				.rejectWhenNoUser(true)
				.init();

		final String serverName = request.getServerName();
		final User currentUser = initData.getUser();
		final Host currentSite = Host.class.cast(request.getSession().getAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST));
		Response response;
		try {
			Logger.info(this,"currentUser: " + currentUser + " ,serverName: " + serverName + " ,currentSite: " + currentSite);
			final Map<String, Object> sessionData = this.helper.doLoginAs(currentUser, loginAsUserId, loginAsUserPwd, serverName);
			Logger.info(this,"sessionData");
			updateLoginAsSessionInfo(request, Host.class.cast(sessionData.get(com.dotmarketing.util.WebKeys
					.CURRENT_HOST)), currentUser.getUserId(), loginAsUserId);
			Logger.info(this,"updateLoginAsSessionInfo");
			this.setImpersonatedUserSite(request, sessionData.get(WebKeys.USER_ID).toString());
			Logger.info(this,"sessionData");
			response = Response.ok(new ResponseEntityView(map("loginAs", true))).build();
		} catch (final NoSuchUserException | DotSecurityException e) {
			SecurityLogger.logInfo(UserResource.class, String.format("ERROR: An attempt to login as a different user " +
							"was made by UserID '%s' / Remote IP '%s': %s", currentUser.getUserId(), request.getRemoteAddr(),
					e.getMessage()));
			revertLoginAsSessionInfo(request, currentSite, currentUser.getUserId());
			return ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED);
		} catch (final DotDataException e) {
			SecurityLogger.logInfo(UserResource.class, String.format("ERROR: An attempt to login as a different user " +
							"was made by UserID '%s' / Remote IP '%s': %s", currentUser.getUserId(), request.getRemoteAddr(),
					e.getMessage()));
			revertLoginAsSessionInfo(request, currentSite, currentUser.getUserId());
			if (UtilMethods.isSet(e.getMessageKey())) {
				final User user = initData.getUser();
				response = Response.ok(new ResponseEntityView(
						list(new ErrorEntity(e.getMessageKey(), LanguageUtil.get(user.getLocale(), e.getMessageKey()))),
						map("loginAs", false))).build();
			} else {
				return ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
			}
		} catch (final Exception e) {
			SecurityLogger.logInfo(UserResource.class, String.format("ERROR: An error occurred when processing the " +
					"Login As request from UserID '%s': %s", currentUser.getUserId(), e.getMessage()));
			revertLoginAsSessionInfo(request, currentSite, currentUser.getUserId());
			if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
				return ErrorResponseHelper.INSTANCE.getErrorResponse(Response.Status.UNAUTHORIZED, initData.getUser()
						.getLocale(), e.getMessage());
			}
			// In case of unknown error, a Status 500 is returned
			Logger.error(this,"ERROR MESSAGE: " + e.getMessage());
			Logger.error(this,"ERROR: " + e);
			Logger.error(this,"ERROR ST: " + e.getStackTrace());
			return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
		SecurityLogger.logInfo(UserResource.class, String.format("UserID '%s' has successfully logged in as '%s' / " +
				"Remote IP: '%s'", currentUser.getUserId(), loginAsUserId, request.getRemoteAddr()));
		return response;
	}

	/**
	 * Updates the current HTTP Session data in order to simulate that the impersonated user - the Login As User - is
	 * logged into the back-end of the system. This involves refreshing the UI of the back-end to reflect the Portlets
	 * that the impersonated user has access to.
	 *
	 * @param request         The {@link HttpServletRequest} object.
	 * @param site            The {@link Host} object representing the Site that the Login As user is directed to.
	 * @param principalUserId The ID of the Administrator User that is temporarily impersonating user.
	 * @param loginAsUserId   The ID of the user that is being impersonated.
	 */
	private void updateLoginAsSessionInfo(final HttpServletRequest request, final Host site, final String
			principalUserId, final String loginAsUserId) {
		final HttpSession session = request.getSession();
		if (UtilMethods.isSet(principalUserId)) {
			session.setAttribute(WebKeys.PRINCIPAL_USER_ID, principalUserId);
		} else {
			session.setAttribute(WebKeys.PRINCIPAL_USER_ID, null);
		}
		final String userToImpersonate = (UtilMethods.isSet(loginAsUserId) ? loginAsUserId : principalUserId);
        session.removeAttribute(WebKeys.USER);
		session.setAttribute(WebKeys.USER_ID, userToImpersonate);
		PrincipalThreadLocal.setName(userToImpersonate);
		session.setAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST, site);
	}

	/**
	 * Reverts all the data changes made to the current HTTP Session data in order to unlink the impersonated user and
	 * bring the original Administrator User back. This process refreshes the back-end UI and brings back all the
	 * expected Portlets.
	 *
	 * @param request         The {@link HttpServletRequest} object.
	 * @param currentSite     The {@link Host} object representing the Site from where the Administrator User
	 *                        impersonated another user.
	 * @param principalUserId The ID of the Administrator User that is temporarily impersonating user.
	 */
	private void revertLoginAsSessionInfo(final HttpServletRequest request, final Host currentSite, final String
			principalUserId) {
		updateLoginAsSessionInfo(request, currentSite, principalUserId, null);
		request.getSession().removeAttribute(WebKeys.PRINCIPAL_USER_ID);
	}

	/**
	 * Sets the appropriate Site for the user that is being impersonated. The approach is the following:
	 * <ul>
	 *     <li>If the impersonated user has access to the Site previously selected by the impersonating user, then
	 *     return the same Site.</li>
	 *     <li>If the impersonated user DOES NOT have access to it, then traverse the list of Sites such a user has
	 *     access to and return the first one.</li>
	 * </ul>
	 *
	 * @param req    The {@link HttpServletRequest} object.
	 * @param userID The ID of the user that is being impersonated.
	 *
	 * @throws DotDataException     An error ocurred when accessing the data source.
	 * @throws DotSecurityException The specified user does not have permissions to perform this action.
	 */
	private void setImpersonatedUserSite(final HttpServletRequest req, final String userID) throws DotDataException, DotSecurityException {
		final HttpSession session = req.getSession();
		final User user = this.userAPI.loadUserById(userID);
		final String currentSiteID = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
		Host currentSite;
		try {
			currentSite = this.siteAPI.find(currentSiteID, user, false);
		} catch (final DotSecurityException e) {
			final List<Host> sites = this.siteAPI.findAll(user, 1,0,null, false);
			if (sites.isEmpty()) {
				// Review the permissions assigned to this user and assign VIEW permissions to AT LEAST one Site
				throw new DotRuntimeException(String.format("Impersonated user '%s' does not have VIEW permissions on " +
						"ANY Site in the system.", userID), e);
			}
			currentSite =  sites.get(0);
		}
		session.setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, currentSite.getIdentifier());
	}

	/**
	 * Performs all the changes in the {@link HttpSession} that are required to
	 * logout from the simulated user's login via the 'Login As' feature in
	 * dotCMS.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 * http://localhost:8080/api/v1/users/logoutas
	 * </pre>
	 *
	 * @param httpServletRequest
	 *            - The {@link HttpServletRequest} object.
	 * @return A {@link Response} containing the status of the operation. This
	 *         will probably require a page refresh.
	 */
	@PUT
	@Path("/logoutas")
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public final Response logoutAs(@Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse) {

		new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.requestAndResponse(httpServletRequest, httpServletResponse)
				.rejectWhenNoUser(true)
				.init();

		final String serverName = httpServletRequest.getServerName();
		String principalUserId = null;
		if (httpServletRequest.getSession().getAttribute(WebKeys.PRINCIPAL_USER_ID) != null) {
			principalUserId = httpServletRequest.getSession().getAttribute(WebKeys.PRINCIPAL_USER_ID).toString();
		}
		User currentLoginAsUser = new User();
		Response response;
		try {
			currentLoginAsUser = PortalUtil.getUser(httpServletRequest);
			final Map<String, Object> sessionData = this.helper.doLogoutAs(principalUserId, currentLoginAsUser, serverName);
			revertLoginAsSessionInfo(httpServletRequest, Host.class.cast(sessionData.get(com.dotmarketing.util.WebKeys
					.CURRENT_HOST)), principalUserId);
			response = Response.ok(new ResponseEntityView(map("logoutAs", true))).build();
		} catch (final DotSecurityException | DotDataException e) {
			SecurityLogger.logInfo(UserResource.class, String.format("ERROR: An error occurred when attempting to log " +
							"out as user '%s' by UserID '%s' / Remote IP '%s': %s", currentLoginAsUser.getUserId(),
					principalUserId, httpServletRequest.getRemoteAddr(), e.getMessage()));
			return ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
		} catch (final Exception e) {
			// In case of unknown error, so we report it as a 500
			SecurityLogger.logInfo(UserResource.class, String.format("ERROR: An error occurred when attempting to log " +
							"out as user '%s' by UserID '%s' / Remote IP '%s': %s", currentLoginAsUser.getUserId(),
					principalUserId, httpServletRequest.getRemoteAddr(), e.getMessage()));
			return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
		SecurityLogger.logInfo(UserResource.class, String.format("UserID '%s' has successfully logged out as '%s' / " +
				"Remote IP: '%s'", principalUserId, currentLoginAsUser.getUserId(), httpServletRequest.getRemoteAddr
				()));
		return response;
	}

	/**
	 * Returns all the users (without the anonymous and default users) that can
	 * be impersonated. The format of the JSON result is:<br>
	 *
	 * <pre>
	 * {
	 *   "name":"Admin User",
	 *   "emailaddress":"admin@dotcms.com",
	 *   "id":"dotcms.org.1",
	 *   "type":"user",
	 *   "requestPassword": true
	 * }
	 * </pre>
	 *
	 * This service returns a 500 HTTP code if anything goes wrong. The
	 * currently logged-in user is automatically removed from the result list.
	 *
	 * @return The list of users that can be impersonated.
	 *
	 * @deprecated use {@link com.dotcms.rest.api.v2.user.UserResource#loginAsData(HttpServletRequest, HttpServletResponse, String, int, int)}
	 */
	@GET
	@Path("/loginAsData")
	@JSONP
	@NoCache
	@Deprecated
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public final Response loginAsData(@Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @QueryParam("filter") final String filter,
									  @QueryParam("includeUsersCount") final boolean includeUsersCount) {
		Response response;
		User currentUser = new User();
		try {
			final InitDataObject initData = new WebResource.InitBuilder(webResource)
					.requiredBackendUser(true)
					.requiredFrontendUser(false)
					.requestAndResponse(httpServletRequest, httpServletResponse)
					.rejectWhenNoUser(true)
					.init();

			currentUser = initData.getUser();
			response = Response.ok(this.helper.getLoginAsUsers(currentUser, filter, includeUsersCount)).build();
		} catch (final Exception e) {
			SecurityLogger.logInfo(UserResource.class, "ERROR: An error occurred when retrieving the Login As data: "
					+ e.getMessage());
			return ErrorResponseHelper.INSTANCE.getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, currentUser.getLocale(), e.getMessage());
		}
		return response;
	}

}
