package com.dotcms.rest.api.v1.user;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ErrorResponseHelper;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.DotRestInstanceProvider;
import com.dotcms.rest.api.v1.authentication.IncorrectPasswordException;
import com.dotcms.rest.api.v1.site.ResponseSiteVariablesEntityView;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.UserPaginator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.UserFirstNameException;
import com.dotmarketing.exception.UserLastNameException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.LocaleUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.vavr.control.Try;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotmarketing.business.UserHelper.validateMaximumLength;

/**
 * This end-point provides access to information associated to dotCMS users.
 *
 * @author Geoff Granum
 * @version 3.5, 3.7
 * @since Mar 29, 2016
 *
 */
@Path("/v1/users")
public class UserResource implements Serializable {

	public static final String USER_ID = "userID";
	public static final String USER_MSG = "User ";
	public static final String USER_WITH_USER_ID_MSG = "User with userId '";
	private final WebResource webResource;
	private final UserAPI userAPI;
	private final HostAPI siteAPI;
	private final UserResourceHelper helper;
	private final ErrorResponseHelper errorHelper;
	private final PaginationUtil paginationUtil;
	private final RoleAPI roleAPI;

	/**
	 * Default class constructor.
	 */
	public UserResource() {
		this(new WebResource(new ApiProvider()), UserResourceHelper.getInstance(),
				new PaginationUtil(new UserPaginator()), new DotRestInstanceProvider()
																 .setUserAPI(APILocator.getUserAPI())
																 .setHostAPI(APILocator.getHostAPI())
																 .setRoleAPI(APILocator.getRoleAPI())
																 .setErrorHelper(ErrorResponseHelper.INSTANCE));
	}

	@VisibleForTesting
	protected UserResource(final WebResource webResource, final UserResourceHelper userHelper,
						   PaginationUtil paginationUtil, final DotRestInstanceProvider instanceProvider) {
		this.webResource = webResource;
		this.helper = userHelper;
		this.paginationUtil = paginationUtil;
		this.userAPI = instanceProvider.getUserAPI();
		this.siteAPI = instanceProvider.getHostAPI();
		this.errorHelper = instanceProvider.getErrorHelper();
		this.roleAPI = instanceProvider.getRoleAPI();
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

				final Role loginAsRole = APILocator.getRoleAPI().loadRoleByKey(Role.LOGIN_AS);
				if (null != loginAsRole) {

					currentUser.loginAs(APILocator.getRoleAPI().doesUserHaveRole(user, loginAsRole));
				}

				currentUser.admin(user.isAdmin());
			} catch (final DotDataException e) {
				Logger.error(this, "Could not provide current user: " + e.getMessage(), e);
				throw new BadRequestException("Could not provide current user.");
			}
		}

		return currentUser.build();
	}

	/**
	 * Updates information from the specified User. This REST Endpoint allows Users to update their own information.
	 * <p>Use example:</p>
	 * <pre>
	 *     {@code PUT} - Auth Required - http://localhost:8080/api/v1/users/current
	 * </pre>
	 * <pre>
	 *     {
	 *     		"currentPassword":"oldpass2",
	 *     		"email":"myemail@dotcms.com",
	 *     		"givenName":"Test",
	 *     		"surname":"User",
	 *     		"userId":"user-7fcb5d28-a921-4a96-b7b6-8d1730795e27",
	 *     		"newPassword":"newpasswd5"
	 *     	}
	 * </pre>
	 *
	 * @param httpServletRequest  The current {@link HttpServletRequest} instance.
	 * @param httpServletResponse The current {@link HttpServletResponse} instance.
	 * @param updateUserForm      The {@link UpdateCurrentUserForm} containing the User's information.
	 *
	 * @return The JSON response with the result of the User update process.
	 *
	 * @throws Exception Different response status codes can be returned when invalid User data is submitted. For
	 * example:
	 *                   <ul>
	 *                       <li>Invalid User's first or last name.</li>
	 *                       <li>User not having the required permissions to perform this action.</li>
	 *                       <li>User not found.</li>
	 *                       <li>Invalid User's current or new password.</li>
	 *                       <li>Generic error from server.</li>
	 *                   </ul>
	 */
	@PUT
	@JSONP
	@Path("/current")
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response updateCurrent(@Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse,
								 final UpdateCurrentUserForm updateUserForm) throws DotDataException {

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

			response = Response.ok(new ResponseEntityView(Map.of(USER_ID, userToUpdated.getUserId(),
					"reauthenticate", reAuthenticationRequired, "user", userMap))).build(); // 200
		} catch (final UserFirstNameException e) {

			this.helper.log("Error Updating User. Invalid First Name", "Date: " + date + ";  "+ "User:" + modUser.getUserId());
			response = this.errorHelper.getErrorResponse(Response.Status.BAD_REQUEST, locale, "User-Info-Save-First-Name-Failed");
		} catch (final UserLastNameException e) {

			this.helper.log("Error Updating User. Invalid Last Name", "Date: " + date + ";  "+ "User:" + modUser.getUserId());
			response = this.errorHelper.getErrorResponse(Response.Status.BAD_REQUEST, locale, "User-Info-Save-Last-Name-Failed");
		} catch (final DotSecurityException e) {

			this.helper.log("Error Updating User. "+e.getMessage(), "Date: " + date + ";  "+ "User:" + modUser.getUserId());
			response = this.errorHelper.getErrorResponse(Response.Status.UNAUTHORIZED, locale, "User-Doesnot-Have-Permission");
		} catch (final NoSuchUserException e) {

			this.helper.log("Error Updating User. "+e.getMessage(), "Date: " + date + ";  "+ "User:" + modUser.getUserId());
			response = this.errorHelper.getErrorResponse(Response.Status.NOT_FOUND, locale, "User-Not-Found");
		} catch (final DotDataException | IncorrectPasswordException e) {
			if (UtilMethods.isSet(e.getMessageKey())) {
				this.helper.log("Error Updating User. " + e.getFormattedMessage(systemLocale), "Date: " + date + ";  "+ "User:" + modUser.getUserId());
				response = this.errorHelper.getErrorResponse(Response.Status.BAD_REQUEST, locale, e.getMessageKey());
			} else{
				this.helper.log("Error Updating User. " + e.getMessage(), "Date: " + date + ";  "+ "User:" + modUser.getUserId());
				response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
			}
		} catch (final Exception e) {
			this.helper.log("Error Updating User. "+e.getMessage(), "Date: " + date + ";  "+ "User:" + modUser.getUserId());
			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	} // update.

	/**
	 * Returns a list of dotCMS users based on the specified search criteria. Depending on the filtering values, 2
	 * types of results can be returned:
	 * <ol>
	 * 		<li>If both the {@code assetInode} and {@code permission} parameters <b>ARE specified</b>, this method will
	 * 		return a list of users that have the specified permission type on the specified asset Inode, and match the
	 * 		remaining filtering parameters as well.</li>
	 * 		<li>If either the {@code assetInode} or {@code permission} parameters <b>ARE NOT specified</b>, this method
	 * 		will return a list of users based on the remaining filtering parameters.</li>
	 * </ol>
	 * <p>
	 * The parameters for this REST call are optional -- they have their respective fallback values -- and are as
	 * follows:
	 * <ul>
	 * 		<li>{@code assetInode}</li>
	 * 		<li>{@code permission}</li>
	 * 		<li>{@code query}</li>
	 * 		<li>{@code page}</li>
	 * 		<li>{@code per_page}</li>
	 * 		<li>{@code includeAnonymous}</li>
	 * 		<li>{@code includeDefault}</li>
	 * </ul>
	 * <p>
	 * Example #1:
	 * <pre>
	 * http://localhost:8080/api/v1/users/filter?page=0&per_page=30&includeAnonymous=true&includeDefault=true
	 * </pre>
	 * <p>
	 * Example #2:
	 * <pre>
	 * http://localhost:8080/api/v1/users/filter?assetInode=6e13c345-4599-49d0-aa47-6a7e59245247&permission=1&query=John&page=0&per_page=10&includeAnonymous=false
	 * </pre>
	 *
	 * @param request          The current instance of the {@link HttpServletRequest}.
	 * @param response         The current instance of the {@link HttpServletResponse}.
	 * @param filter           Allows you to filter Users by their full name or parts of it.
	 * @param page             The results page or offset, for pagination purposes.
	 * @param perPage          The size of the results page, for pagination purposes.
	 * @param orderBy          The column name that will be used to sort the paginated results. For reference, please
	 *                         check {@link SQLUtil #ORDERBY_WHITELIST(private method in SQLUtil)}.
	 * @param direction        The sorting direction for the results: {@code "ASC"} or {@code "DESC"}
	 * @param includeAnonymous If the Anonymous User must be included in the results, set this to {@code true}.
	 * @param includeDefault   If the Default User must be included in the results, set this to {@code true}.
	 * @param assetInode       The Inode of a specific asset, if you're querying Users that have a specific permission
	 *                         on it.
	 * @param permission       The permission type that Users may have on the previous asset.
	 *
	 * @return A {@link Response} containing the list of dotCMS users that match the filtering criteria.
	 */
	@GET
	@JSONP
	@Path("/filter")
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public Response filter(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
						   @QueryParam(UserPaginator.QUERY_PARAM) final String filter,
						   @DefaultValue("0") @QueryParam(PaginationUtil.PAGE) final int page,
						   @DefaultValue("40") @QueryParam(PaginationUtil.PER_PAGE) final int perPage,
						   @QueryParam(PaginationUtil.ORDER_BY) String orderBy,
						   @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION) String direction,
						   @QueryParam(UserPaginator.INCLUDE_ANONYMOUS) boolean includeAnonymous,
						   @QueryParam(UserPaginator.INCLUDE_DEFAULT) boolean includeDefault,
						   @QueryParam(UserPaginator.ASSET_INODE_PARAM) String assetInode,
						   @QueryParam(UserPaginator.PERMISSION_PARAM) int permission) {
		final InitDataObject initData = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true)
				.init();

		final Map<String, Object> extraParams = new HashMap<>();
		extraParams.put(UserPaginator.ASSET_INODE_PARAM, assetInode);
		extraParams.put(UserPaginator.PERMISSION_PARAM, permission);
		extraParams.put(UserAPI.FilteringParams.INCLUDE_ANONYMOUS_PARAM, includeAnonymous);
		extraParams.put(UserAPI.FilteringParams.INCLUDE_DEFAULT_PARAM, includeDefault);
		extraParams.put(UserAPI.FilteringParams.ORDER_BY_PARAM, orderBy);

		final OrderDirection orderDirection = OrderDirection.valueOf(direction);
		final User user = initData.getUser();
		return this.paginationUtil.getPage(request, user, filter, page, perPage, orderBy, orderDirection, extraParams);
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

		checkUserLoginAsRole(initData.getUser());

		final String serverName = request.getServerName();
		final User currentUser = initData.getUser();
		final Host currentSite = Host.class.cast(request.getSession().getAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST));
		Response response;
		try {
			final Map<String, Object> sessionData = this.helper.doLoginAs(currentUser, loginAsUserId, loginAsUserPwd, serverName);
			updateLoginAsSessionInfo(request, Host.class.cast(sessionData.get(com.dotmarketing.util.WebKeys
					.CURRENT_HOST)), currentUser.getUserId(), loginAsUserId);
			this.setImpersonatedUserSite(request, sessionData.get(WebKeys.USER_ID).toString());
			response = Response.ok(new ResponseEntityView(Map.of("loginAs", true))).build();
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
				response = Response.ok(new ResponseEntityView<>(
						list(new ErrorEntity(e.getMessageKey(), LanguageUtil.get(user.getLocale(), e.getMessageKey()))),
						Map.of("loginAs", false))).build();
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
	 * @throws DotDataException     An error occurred when accessing the data source.
	 * @throws DotSecurityException The specified user does not have permissions to perform this action.
	 * @throws DotRuntimeException  There are missing session attributes, or the User doesn't have access to any Site.
	 */
	private void setImpersonatedUserSite(final HttpServletRequest req, final String userID) throws DotDataException, DotSecurityException {
		final HttpSession session = req.getSession();
		final User user = this.userAPI.loadUserById(userID);
		final String currentSiteID = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
		if (UtilMethods.isNotSet(currentSiteID)) {
			final String errorMsg = String.format("The CMS_SELECTED_HOST_ID attribute is not present in the session " +
					"for User '%s'", userID);
			Logger.error(this, errorMsg);
			throw new DotRuntimeException(errorMsg);
		}
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
			response = Response.ok(new ResponseEntityView(Map.of("logoutAs", true))).build();
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
	 * be impersonated.
	 *
	 * @return The list of users that can be impersonated.
	 */
	@GET
	@Path("/loginAsData")
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public final Response loginAsData(@Context final HttpServletRequest httpServletRequest,
									  @Context final HttpServletResponse httpServletResponse,
									  @QueryParam(PaginationUtil.FILTER)   final String filter,
									  @QueryParam(PaginationUtil.PAGE) final int page,
									  @QueryParam(PaginationUtil.PER_PAGE) final int perPage) {

		final InitDataObject initData = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.requestAndResponse(httpServletRequest, httpServletResponse)
				.rejectWhenNoUser(true).init();

		Response response;
		final User user = initData.getUser();
		try {
			checkUserLoginAsRole(initData.getUser());
			final List<Role> roles = List.of(roleAPI.loadBackEndUserRole());
			final Map<String, Object> extraParams = new HashMap<>(Map.of(
					UserPaginator.ROLES_PARAM, roles,
					UserAPI.FilteringParams.INCLUDE_ANONYMOUS_PARAM, false,
					UserAPI.FilteringParams.INCLUDE_DEFAULT_PARAM, false,
					UserPaginator.REMOVE_CURRENT_USER_PARAM, true,
					UserPaginator.REQUEST_PASSWORD_PARAM, true));
			response = this.paginationUtil.getPage( httpServletRequest, user, filter, page, perPage, extraParams);
		} catch (final Exception e) {
			if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
				throw new ForbiddenException(e);
			}
			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
			Logger.error(this, e.getMessage(), e);
		}
		return response;
	}

	/**
	 * Utility method that verifies whether the specified User has the required "Login As" Role.
	 *
	 * @param user The {@link User} being checked.
	 *
	 * @throws DotDataException     An error occurred when interacting with the data source.
	 * @throws DotSecurityException The specified User does not have the required "Login As" Role.
	 */
	private void checkUserLoginAsRole(final User user) throws DotDataException, DotSecurityException {
		final Role loginAsRole = this.roleAPI.loadRoleByKey(Role.LOGIN_AS);
		final boolean hasRole = Try.of(() -> this.roleAPI.doesUserHaveRole(user, loginAsRole)).getOrElse(Boolean.FALSE);
		if (!hasRole) {
			final String errorMsg = String.format("User '%s' must have the Login As role to execute this action",
					user.getUserId());
			Logger.debug(this, errorMsg);
			throw new DotSecurityException(errorMsg);
		}
	}

	/**
	 * Creates an user.
	 * If userId is sent will be use, if not will be created "userId-" + UUIDUtil.uuid().
	 * By default, users will be inactive unless the active = true is sent and user has permissions( is Admin or access
	 * to Users and Roles portlets).
	 * FirstName, LastName, Email and Password are required.
	 *
	 *
	 * Scenarios:
	 *  1. No Auth or User doing the request do not have access to Users and Roles Portlets
	 *  	- Always will be inactive
	 *  	- Only the	Role DOTCMS_FRONT_END_USER will be added
	 *  2. Auth, User is Admin or have access to Users and Roles Portlets
	 *  	- Can be active if JSON includes ("active": true)
	 *  	- The list of RoleKey will be use to assign the roles, if the roleKey doesn't exist will be
	 *  		created under the ROOT ROLE.
	 *
	 * @param httpServletRequest
	 * @param createUserForm
	 * @return User Created
	 * @throws Exception
	 */
	@POST
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response create(@Context final HttpServletRequest httpServletRequest,
								 @Context final HttpServletResponse httpServletResponse,
								 final UserForm createUserForm) throws Exception {

		final User modUser = new WebResource.InitBuilder(webResource)
				.requestAndResponse(httpServletRequest, httpServletResponse)
				.rejectWhenNoUser(true)
				.init().getUser();

		final boolean isRoleAdministrator = modUser.isAdmin() ||
				(
						APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(PortletID.ROLES.toString(), modUser) &&
						APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(PortletID.USERS.toString(), modUser)
				);

		if (isRoleAdministrator) {
			final User userToUpdated = this.createNewUser(
					modUser, createUserForm);

			final Role role = APILocator.getRoleAPI().getUserRole(userToUpdated);
			return Response.ok(new ResponseEntityView(Map.of(USER_ID, userToUpdated.getUserId(), "roleId", role.getId(),
					"user", userToUpdated.toMap()))).build(); // 200
		}

		throw new ForbiddenException(USER_MSG + modUser.getUserId() + " does not have permissions to create users");
	} // create.

	@WrapInTransaction
	protected User createNewUser(final User modUser,
								 final UserForm createUserForm)
			throws DotDataException, DotSecurityException, ParseException {

		final String userId = UtilMethods.isSet(createUserForm.getUserId())?
				createUserForm.getUserId(): "userId-" + UUIDUtil.uuid();
		validateMaximumLength(createUserForm.getFirstName(),createUserForm.getLastName(),createUserForm.getEmail(),
				createUserForm.getMiddleName(),createUserForm.getNickName(),createUserForm.getBirthday());
		final User user = this.userAPI.createUser(userId, createUserForm.getEmail());

		user.setFirstName(createUserForm.getFirstName());

		if (UtilMethods.isSet(createUserForm.getLastName())) {
			user.setLastName(createUserForm.getLastName());
		}

		if (UtilMethods.isSet(createUserForm.getBirthday())) {
			user.setBirthday(DateUtil.parseISO(createUserForm.getBirthday()));
		}

		if (UtilMethods.isSet(createUserForm.getMiddleName())) {
			user.setMiddleName(createUserForm.getMiddleName());
		}

		processLanguage(createUserForm, user);

		if (UtilMethods.isSet(createUserForm.getNickName())) {
			user.setNickName(createUserForm.getNickName());
		}

		if (UtilMethods.isSet(createUserForm.getTimeZoneId())) {
			user.setTimeZoneId(createUserForm.getTimeZoneId());
		}

		user.setPassword(new String(createUserForm.getPassword()));
		user.setMale(createUserForm.isMale());
		user.setCreateDate(new Date());

		if (UtilMethods.isSet(createUserForm.getAdditionalInfo())) {
			user.setAdditionalInfo(createUserForm.getAdditionalInfo());
		}

		final List<String> roleKeys = UtilMethods.isSet(createUserForm.getRoles())?
				createUserForm.getRoles():list(Role.DOTCMS_FRONT_END_USER);

		this.userAPI.save(user, modUser, false);
		Logger.debug(this,  ()-> USER_WITH_USER_ID_MSG + userId + "' and email '" +
				createUserForm.getEmail() + "' has been created.");

		for (final String roleKey : roleKeys) {

			UserHelper.getInstance().addRole(user, roleKey, false	, false);
		}

		return user;
	}

	private static void processLanguage(final LanguageSupport createUserForm, final User user) {

		String languageTag = createUserForm.getLanguageId();
		if (UtilMethods.isSet(languageTag) && languageTag.contains("_")) {
			languageTag = languageTag.replace("_", "-");
		}

		LanguageUtil.validateLanguageTag(languageTag);
		user.setLanguageId(createUserForm.getLanguageId());
	}


	/**
	 * Update an existing user.
	 *
	 * Only Admin User or have access to Users and Roles Portlets can update an existing user
	 *
	 * @param httpServletRequest
	 * @param userForm
	 * @return User Updated
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
	@PUT
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response udpate(@Context final HttpServletRequest httpServletRequest,
								 @Context final HttpServletResponse httpServletResponse,
								 final UserForm createUserForm) throws DotDataException, IncorrectPasswordException, SystemException, DotSecurityException, ParseException, PortalException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {

		final User modUser = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.requestAndResponse(httpServletRequest, httpServletResponse)
				.rejectWhenNoUser(true)
				.init().getUser();

		final boolean isRoleAdministrator = modUser.isAdmin() ||
				(
						APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(PortletID.ROLES.toString(), modUser) &&
								APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(PortletID.USERS.toString(), modUser)
				);

		if (isRoleAdministrator) {

			final User userToUpdated = this.updateUser(
					modUser, httpServletRequest, createUserForm);

			return Response.ok(new ResponseEntityView<>(Map.of(USER_ID, userToUpdated.getUserId(),
					"user", userToUpdated.toMap()))).build(); // 200
		}

		throw new ForbiddenException(USER_MSG + modUser.getUserId() + " does not have permissions to update users");
	} // create.

	@WrapInTransaction
	private User updateUser(final User modUser, final HttpServletRequest request,
							final UserForm updateUserForm) throws DotDataException, DotSecurityException,
			ParseException, SystemException, PortalException {

		final String userId = updateUserForm.getUserId();
		boolean validatePassword = false;
		final User userRecovery;
		try {

			userRecovery = this.userAPI.loadUserById
					(updateUserForm.getUserId(), modUser, false);

			if (null == userRecovery) {

				throw new NotFoundException("The user: " + userId + " not found");
			}
		} catch (NoSuchUserException e) {

			throw new NotFoundException("The user: " + userId + " not found");
		}

		final User userToSave = (User)userRecovery.clone();

		validateMaximumLength(updateUserForm.getFirstName(),updateUserForm.getLastName(),updateUserForm.getEmail(),
				updateUserForm.getMiddleName(),updateUserForm.getNickName(),updateUserForm.getBirthday());

		if (UtilMethods.isSet(updateUserForm.getFirstName())) {
			userToSave.setFirstName(updateUserForm.getFirstName());
		}

		if (UtilMethods.isSet(updateUserForm.getLastName())) {
			userToSave.setLastName(updateUserForm.getLastName());
		}

		if (UtilMethods.isSet(updateUserForm.getEmail())) {
			userToSave.setEmailAddress(updateUserForm.getEmail());
		}

		if (UtilMethods.isSet(updateUserForm.getBirthday())) {
			userToSave.setBirthday(DateUtil.parseISO(updateUserForm.getBirthday()));
		}

		if (UtilMethods.isSet(updateUserForm.getMiddleName())) {
			userToSave.setMiddleName(updateUserForm.getMiddleName());
		}

		processLanguage(updateUserForm, userToSave);

		if (UtilMethods.isSet(updateUserForm.getNickName())) {
			userToSave.setNickName(updateUserForm.getNickName());
		}

		if (UtilMethods.isSet(updateUserForm.getTimeZoneId())) {
			userToSave.setTimeZoneId(updateUserForm.getTimeZoneId());
		}

		userToSave.setMale(updateUserForm.isMale());

		if (UtilMethods.isSet(updateUserForm.getAdditionalInfo())) {
			userToSave.setAdditionalInfo(updateUserForm.getAdditionalInfo());
		}

		// Password has changed, so it has to be validated
		userToSave.setPassword(new String(updateUserForm.getPassword()));

		if (APILocator.getPermissionAPI().doesUserHavePermission
				(APILocator.getUserProxyAPI().getUserProxy(userToSave, modUser, false),
						PermissionAPI.PERMISSION_EDIT, modUser, false)) {

			this.userAPI.save(userToSave, modUser, validatePassword,
					!WebAPILocator.getUserWebAPI().isLoggedToBackend(request));
			Logger.debug(this,  ()-> USER_WITH_USER_ID_MSG + userId + "' and email '" +
					updateUserForm.getEmail() + "' has been updated.");

			final List<String> roleKeys = UtilMethods.isSet(updateUserForm.getRoles())?
					updateUserForm.getRoles():list(Role.DOTCMS_FRONT_END_USER);

			for (final String roleKey : roleKeys) {

				UserHelper.getInstance().addRole(userToSave, roleKey, false	, false);
			}

			Logger.debug(this,  ()-> USER_WITH_USER_ID_MSG + userId + "' and email '" +
					updateUserForm.getEmail() + "' , the roles have been updated.");

		} else {

			throw new ForbiddenException(USER_MSG + modUser.getUserId() + " does not have permissions to update users");
		}

		return userToSave;
	}
}
