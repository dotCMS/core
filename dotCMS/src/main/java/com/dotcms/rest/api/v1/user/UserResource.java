package com.dotcms.rest.api.v1.user;

import static com.dotcms.util.CollectionsUtils.getMapValue;
import static com.dotcms.util.CollectionsUtils.map;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.PUT;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ErrorResponseHelper;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.IncorrectPasswordException;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.UserFirstNameException;
import com.dotmarketing.exception.UserLastNameException;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.LocaleUtil;

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
	private final UserResourceHelper helper;
	private final ErrorResponseHelper errorHelper;

	/**
	 * Default class constructor.
	 */
	public UserResource() {
		this(new WebResource(new ApiProvider()), APILocator.getUserAPI(), UserResourceHelper.getInstance(),
				ErrorResponseHelper.INSTANCE);
	}

	@VisibleForTesting
	protected UserResource(final WebResource webResource,  final UserAPI userAPI, final UserResourceHelper userHelper,
			final ErrorResponseHelper errorHelper) {
		this.webResource = webResource;
		this.userAPI = userAPI;
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
    public RestUser self(@Context HttpServletRequest request) {

        final User user = webResource.init(true, request, true).getUser();
        final RestUser.Builder currentUser = new RestUser.Builder();

        if(user != null) {
            try {

                final Role role = APILocator.getRoleAPI().getUserRole(user);
                currentUser.userId(user.getUserId())
                    .givenName(user.getFirstName())
                    .email(user.getEmailAddress())
                    .surname(user.getLastName())
                    .roleId(role.getId());
            } catch (DotDataException e) {

                Logger.error(this, e.getMessage(), e);
                throw new BadRequestException("Could not provide current user.");
            }
        }

        return currentUser.build();
    }

    /**
     * 
     * @param request
     * @param updateUserForm
     * @return
     * @throws Exception
     */
    @PUT
    @JSONP
    @Path("/current")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response update(@Context final HttpServletRequest request,
                                 final UpdateUserForm updateUserForm) throws Exception {

        final User modUser = webResource.init(true, request, true).getUser();
        Response response = null;
        final String date = DateUtil.getCurrentDate();
        final User userToUpdated;
        Locale locale = LocaleUtil.getLocale(request);
        Locale systemLocale = this.userAPI.getSystemUser().getLocale();
        Map<String, Object> userMap = Collections.EMPTY_MAP;

        this.helper.log("Updating User", "Date: " + date + "; "
                + "User:" + modUser.getUserId());

        try {

            if (null == locale) {
                locale = modUser.getLocale();
            }

			userToUpdated = this.helper.updateUser(updateUserForm, modUser, request, locale);
            this.helper.log("User Updated", "Date: " + date + "; "+ "User:" + modUser.getUserId());

			boolean reAuthenticationRequired =  null != updateUserForm.getNewPassword();

            if (!reAuthenticationRequired) { // if re authentication is not required, sent the current changed user
                userMap = userToUpdated.toMap();
            }

            response = Response.ok(new ResponseEntityView(map("userID", userToUpdated.getUserId(),
                    "reauthenticate", reAuthenticationRequired, "user", userMap))).build(); // 200
        } catch (UserFirstNameException e) {

            this.helper.log("Error Updating User. Invalid First Name", "Date: " + date + ";  "+ "User:" + modUser.getUserId());
            response = this.errorHelper.getErrorResponse(Response.Status.BAD_REQUEST, locale, "User-Info-Save-First-Name-Failed");
        } catch (UserLastNameException e) {

            this.helper.log("Error Updating User. Invalid Last Name", "Date: " + date + ";  "+ "User:" + modUser.getUserId());
            response = this.errorHelper.getErrorResponse(Response.Status.BAD_REQUEST, locale, "User-Info-Save-Last-Name-Failed");
        } catch (DotSecurityException  e) {

            this.helper.log("Error Updating User. "+e.getMessage(), "Date: " + date + ";  "+ "User:" + modUser.getUserId());
            response = this.errorHelper.getErrorResponse(Response.Status.UNAUTHORIZED, locale, "User-Doesnot-Have-Permission");
        } catch (NoSuchUserException  e) {

            this.helper.log("Error Updating User. "+e.getMessage(), "Date: " + date + ";  "+ "User:" + modUser.getUserId());
            response = this.errorHelper.getErrorResponse(Response.Status.NOT_FOUND, locale, "User-Not-Found");
        } catch (DotDataException e) {
        	if(null != e.getMessageKey()){
        		this.helper.log("Error Updating User. "+e.getFormattedMessage(systemLocale), "Date: " + date + ";  "+ "User:" + modUser.getUserId());
        		response = this.errorHelper.getErrorResponse(Response.Status.BAD_REQUEST, locale, e.getMessageKey());
        	} else{
        		this.helper.log("Error Updating User. "+e.getMessage(), "Date: " + date + ";  "+ "User:" + modUser.getUserId());
        		response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        	}
    	} catch (IncorrectPasswordException e) {
			this.helper.log("Error Updating User. " + e.getMessage(), "Date: " + date + ";  "+ "User:" + modUser.getUserId());
			response = ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
		}catch (Exception  e) {
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
	public Response filter(@Context final HttpServletRequest request, @PathParam("params") final String params) {
		final InitDataObject initData = webResource.init(params, true, request, true, null);
		final Map<String, String> urlParams = initData.getParamsMap();
		Map<String, Object> userList = null;
		try {
			final Map<String, String> filterParams = map(
					"query", urlParams.get("query"), 
					"start", getMapValue(urlParams, "start", "0"), 
					"limit", getMapValue(urlParams, "limit", "-1"),
					"includeAnonymous", getMapValue(urlParams, "includeAnonymous", "false"), 
					"includeDefault", getMapValue(urlParams, "includeDefault", "false"));
			userList = this.helper.getUserList(urlParams.get("assetInode"), urlParams.get("permission"), filterParams);
		} catch (Exception e) {
			// In case of unknown error, so we report it as a 500
			Logger.error(this, "An error occurred when processing the request.", e);
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
	 * @param params
	 *            - The parameters that can be specified in the REST call.
	 * @return A {@link Response} containing the status of the operation. This
	 *         will probably require a page refresh.
	 */
	@PUT
	@Path("/loginas/{params:.*}")
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public final Response loginAs(@Context final HttpServletRequest request, @PathParam("params") final String params) {
		InitDataObject initData = webResource.init(params, true, request, true, null);
		final Map<String, String> urlParams = initData.getParamsMap();
		final String loginAsUserId = getMapValue(urlParams, "userid", null);
		final String loginAsUserPwd = getMapValue(urlParams, "pwd", null);
		final String serverName = request.getServerName();
		final User currentUser = initData.getUser();
		Response response = null;
		try {
			Map<String, Object> sessionData = this.helper.doLoginAs(currentUser, loginAsUserId, loginAsUserPwd, serverName);
			HttpSession session = request.getSession();
			if (session.getAttribute(WebKeys.PRINCIPAL_USER_ID) == null) {
				session.setAttribute(WebKeys.PRINCIPAL_USER_ID, sessionData.get(WebKeys.PRINCIPAL_USER_ID));
			}
			session.setAttribute(WebKeys.USER_ID, sessionData.get(WebKeys.USER_ID));
			PrincipalThreadLocal.setName(loginAsUserId);
			session.setAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST,
					sessionData.get(com.dotmarketing.util.WebKeys.CURRENT_HOST));
			response = Response.ok(new ResponseEntityView(map("loginAs", true))).build();
		} catch (NoSuchUserException | DotSecurityException e) {
			SecurityLogger.logInfo(UserResource.class,
					"An attempt to login as a different user was made by user ID ("
							+ currentUser.getUserId() + "). Remote IP: " + request.getRemoteAddr());
			return ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED);
		} catch (DotDataException e) {
			SecurityLogger.logInfo(UserResource.class,
					"An attempt to login as a different user was made by user ID ("
							+ currentUser.getUserId() + "). Remote IP: " + request.getRemoteAddr());
			return ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
		} catch (Exception e) {
			// In case of unknown error, so we report it as a 500
			SecurityLogger.logInfo(UserResource.class,
					"An error occurred when processing the request." + e.getMessage());
			return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
		SecurityLogger.logInfo(UserResource.class, "User ID (" + currentUser.getUserId()
				+ "), has sucessfully login as (" + loginAsUserId + "). Remote IP: " + request.getRemoteAddr());
		return response;
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
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @return A {@link Response} containing the status of the operation. This
	 *         will probably require a page refresh.
	 */
	@PUT
	@Path("/logoutas")
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public final Response logoutAs(@Context final HttpServletRequest request) {
		webResource.init(null, true, request, true, null);
		final String serverName = request.getServerName();
		String principalUserId = null;
		HttpSession session = request.getSession();
		if (session.getAttribute(WebKeys.PRINCIPAL_USER_ID) != null) {
			principalUserId = request.getSession().getAttribute(WebKeys.PRINCIPAL_USER_ID).toString();
		}
		User currentLoginAsUser = new User();
		Response response = null;
		try {
			currentLoginAsUser = PortalUtil.getUser(request);
			Map<String, Object> sessionData = this.helper.doLogoutAs(principalUserId, currentLoginAsUser, serverName);
			session.setAttribute(WebKeys.USER_ID, principalUserId);
			session.removeAttribute(WebKeys.PRINCIPAL_USER_ID);
			session.setAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST,
					sessionData.get(com.dotmarketing.util.WebKeys.CURRENT_HOST));
			PrincipalThreadLocal.setName(principalUserId);
			response = Response.ok(new ResponseEntityView(map("logoutAs", true))).build();
		} catch (DotSecurityException e) {
			SecurityLogger.logInfo(UserResource.class,
					"An attempt to logout as a different user was made by user ID ("
							+ currentLoginAsUser.getUserId() + "). Remote IP: " + request.getRemoteAddr());
			return ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
		} catch (DotDataException e) {
			SecurityLogger.logInfo(UserResource.class,
					"An attempt to logout as a different user was made by user ID ("
							+ currentLoginAsUser.getUserId() + "). Remote IP: " + request.getRemoteAddr());
			return ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
		} catch (Exception e) {
			// In case of unknown error, so we report it as a 500
			SecurityLogger.logInfo(UserResource.class, 
					"An error occurred when processing the request."+e.getMessage());
			return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
		SecurityLogger.logInfo(UserResource.class,
				"User (" + principalUserId + ") has sucessfully logged out as (" 
		        + currentLoginAsUser.getUserId() + "). Remote IP: " + request.getRemoteAddr());
		return response;
	}

	/**
	 * Return all the user without the anonymous and default users, also add extra login as information,
	 * with the follow json format:<br>
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
	 * This service return a 500 HTTP code if anything go wrong
	 *
	 * @return
     */
	@GET
	@Path("/loginAsData")
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public final Response loginAsData() {

		Response response = null;

		try {
			List<Map<String, Object>> loginAsUser = helper.getLoginAsUser();
			response = Response.ok(new ResponseEntityView(map("users", loginAsUser))).build();
		} catch (Exception e) {
			SecurityLogger.logInfo(UserResource.class, 
					"An error occurred when processing the request." + e.getMessage());
			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

}
