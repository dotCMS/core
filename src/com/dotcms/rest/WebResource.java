package com.dotcms.rest;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.sun.jersey.core.util.Base64;
import com.sun.jersey.spi.container.ContainerRequest;

public class WebResource {

	/**
	 * <p>Checks if SSL is required. If it is required and no secure request is provided, throws a ForbiddenException.
	 *
	 * @param request
	 */

	protected void init(HttpServletRequest request) {
		checkForceSSL(request);
	}

	/**
	 * <p>1) Checks if SSL is required. If it is required and no secure request is provided, throws a ForbiddenException.
	 * <p>2) If 1) does not throw an exception, returns an {@link InitDataObject} with a <code>Map</code> containing
	 * the keys and values extracted from <code>params</code>
	 *
	 *
	 * @param params a string containing parameters in the /key/value form
	 * @param request
	 * @return an initDataObject with the resulting <code>Map</code>
	 */

	protected InitDataObject init(String params, HttpServletRequest request) {

		checkForceSSL(request);

		InitDataObject initData = new InitDataObject();

		if(!UtilMethods.isSet(params))
			return initData;

		initData.setParamsMap(buildParamsMap(params));
		return initData;
	}

	/**
	 *
	 * <p>1) Checks if SSL is required. If it is required and no secure request is provided, throws a ForbiddenException.
	 * <p>2) If 1) does not throw an exception, returns an {@link InitDataObject} with:
	 *
	 * <br>a) a <code>Map</code> with the keys and values extracted from <code>params</code>.
	 *
	 *<br><br>if <code>authenticate</code> is set to <code>true</code>:
	 * <br>b) , an authenticated {@link User}, if found.
	 * If no User can be retrieved, and <code>rejectWhenNoUser</code> is <code>true</code>, it will throw an exception,
	 * otherwise returns <code>null</code>.
	 *
	 * <br><br>There are five ways to get the User. They are executed in the specified order. When found, the remaining ways won't be executed.
	 * <br>1) Using username and password contained in <code>params</code>.
	 * <br>2) Using username and password in Base64 contained in the <code>request</code> HEADER parameter DOTAUTH.
	 * <br>3) Using username and password in Base64 contained in the <code>request</code> HEADER parameter AUTHORIZATION (BASIC Auth).
	 * <br>4) From the session. It first tries to get the Backend logged in user. If no user found, tries to get the Frontend logged in user.
	 *
	 *
	 * @param params a string containing the URL parameters in the /key/value form
	 * @param authenticate
	 * @param request
	 * @param rejectWhenNoUser determines whether a SecurityException is thrown or not when authentication fails.
	 * @return an initDataObject with the resulting <code>Map</code>
	 */

	protected InitDataObject init(String params, boolean authenticate, HttpServletRequest request, boolean rejectWhenNoUser) throws SecurityException {

		checkForceSSL(request);

		InitDataObject initData = new InitDataObject();

		if(!UtilMethods.isSet(params))
			params = "";

		Map<String, String> paramsMap = buildParamsMap(params);
		User user = null;

		user = authenticateUser(paramsMap, request, rejectWhenNoUser);

		initData.setParamsMap(paramsMap);
		initData.setUser(user);

		return initData;
	}

	/**
	 * Returns an authenticated {@link User}. There are five ways to get the User.
	 * They are executed in the specified order. When found, the remaining ways won't be executed.
	 * <br><br>1) Using username and password contained in <code>params</code>.
	 * <br>2) Using username and password in Base64 contained in the <code>request</code> HEADER parameter DOTAUTH.
	 * <br>3) Using username and password in Base64 contained in the <code>request</code> HEADER parameter AUTHORIZATION (BASIC Auth).
	 * <br>4) From the session. It first tries to get the Backend logged in user. If no user found, tries to get the Frontend logged in user.
	 *
	 * @param paramsMap a map containing the URL parameters
	 * @param request
	 * @param rejectWhenNoUser determines whether a SecurityException is thrown or not when authentication fails.
	 * @return
	 */

	private User authenticateUser(Map<String, String> paramsMap, HttpServletRequest request, boolean rejectWhenNoUser) throws SecurityException  {

		boolean forcefrontendauth = Config.getBooleanProperty("REST_API_FORCE_FRONT_END_SESSION_AUTH", false);

		User user = (user = authenticateUserFromURL(paramsMap, request)) != null ? user
						: (user = authenticateUserFromHeaderAuth(paramsMap, request)) != null ? user
								: (user = authenticateUserFromBasicAuth(paramsMap, request)) != null ? user
										: !forcefrontendauth ? (user = getBackUserFromRequest(request)) != null ? user
												: (user = getFrontEndUserFromRequest(request))
												: (user = getFrontEndUserFromRequest(request)) != null ? user : null;


		if(user==null && (Config.getBooleanProperty("REST_API_REJECT_WITH_NO_USER", false) || rejectWhenNoUser) ) {
			throw new SecurityException("Invalid User", Response.Status.UNAUTHORIZED);
		}

		return user;
	}

	private User authenticateUserFromURL(Map<String, String> paramsMap, HttpServletRequest request) {

		String username = paramsMap.get(RESTParams.USER.getValue());
		String password = paramsMap.get(RESTParams.PASSWORD.getValue());

		return authenticateUser(username, password, request);
	}

	private User authenticateUserFromBasicAuth(Map<String, String> paramsMap, HttpServletRequest request) throws SecurityException  {

		// Extract authentication credentials
		String authentication = request.getHeader(ContainerRequest.AUTHORIZATION);

		if(UtilMethods.isSet(authentication) && authentication.startsWith("Basic ")) {
			authentication = authentication.substring("Basic ".length());
			String[] values = new String(Base64.base64Decode(authentication)).split(":");
			if (values.length < 2) {
				// "Invalid syntax for username and password"
				throw new SecurityException("Invalid syntax for username and password", Response.Status.BAD_REQUEST);
			}
			String username = values[0];
			String password = values[1];

			return authenticateUser(username, password, request);
		} else {
			return null;
		}
	}

	private User authenticateUserFromHeaderAuth(Map<String, String> paramsMap, HttpServletRequest request) throws SecurityException  {
		// Extract authentication credentials
		String authentication = request.getHeader("DOTAUTH");

		if(UtilMethods.isSet(authentication)) {
			String[] values = new String(Base64.base64Decode(authentication)).split(":");
			if (values.length < 2) {
				// "Invalid syntax for username and password"
				throw new SecurityException("Invalid syntax for username and password", Response.Status.BAD_REQUEST);
			}
			String username = values[0];
			String password = values[1];

			return authenticateUser(username, password, request);
		} else {
			return null;
		}
	}

	/**
	 * Authenticates and returns a {@link User} using <code>username</code> and <code>password</code>.
	 * If a wrong <code>username</code> or <code>password</code> are provided,
	 * a SecurityException is thrown
	 *
	 * @param username
	 * @param password
	 * @param req
	 * @return
	 */


	private User authenticateUser(String username, String password, HttpServletRequest req) throws SecurityException {
		User user = null;
		String ip = req!=null?req.getRemoteAddr():"";

		if(UtilMethods.isSet(username) && UtilMethods.isSet(password)) { // providing login and password so let's try to authenticate

			try {

				if(LoginFactory.doLogin(username, password)) {
					Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();

					if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
						user = APILocator.getUserAPI().loadByUserByEmail(username, APILocator.getUserAPI().getSystemUser(), false);
					} else {
						user = APILocator.getUserAPI().loadUserById(username, APILocator.getUserAPI().getSystemUser(), false);
					}
				} else { // doLogin returning false

					Logger.warn(this.getClass(), "Request IP: " + ip + ". Can't authenticate user. Username: " + username);
					SecurityLogger.logDebug(this.getClass(), "Request IP: " + ip + ". Can't authenticate user. Username: " + username);
					throw new SecurityException("Invalid credentials", Response.Status.UNAUTHORIZED);
				}

			}  catch(SecurityException e) {
				throw e;
			} catch (Exception e) {  // doLogin throwing Exception

				Logger.warn(this.getClass(), "Request IP: " + ip + ". Can't authenticate user. Username: " + username);
				SecurityLogger.logDebug(this.getClass(), "Request IP: " + ip + ". Can't authenticate user. Username: " + username);
				throw new SecurityException("Authentication credentials are required", Response.Status.UNAUTHORIZED);
			}

		} else if(UtilMethods.isSet(username) || UtilMethods.isSet(password)){ // providing login or password

			Logger.warn(this.getClass(), "Request IP: " + ip + ". Can't authenticate user.");
			SecurityLogger.logDebug(this.getClass(), "Request IP: " + ip + ". Can't authenticate user.");
			throw new SecurityException("Authentication credentials are required", Response.Status.UNAUTHORIZED);
		}

		return user;
	}


	/**
	 * This method returns the Backend logged in user from request.
	 *
	 * @param request
	 * @return
	 */

	private User getBackUserFromRequest(HttpServletRequest req) {
		User user = null;

		if(UtilMethods.isSet(req)) { // let's check if we have a request and try to get the user logged in from it
			try {
				user = WebAPILocator.getUserWebAPI().getLoggedInUser(req);
			}  catch (Exception e) {
				Logger.warn(this.getClass(), "Can't retrieve Backend User from session");
			}
		}

		return user;
	}

	/**
	 * This method returns the Frontend logged in user from request.
	 *
	 * @param request
	 * @return
	 */

	private User getFrontEndUserFromRequest(HttpServletRequest req) {
		User user = null;

		if(UtilMethods.isSet(req)) { // let's check if we have a request and try to get the user logged in from it
			try {
				user = WebAPILocator.getUserWebAPI().getLoggedInFrontendUser(req);
			}  catch (Exception e) {
				Logger.warn(this.getClass(), "Can't retrieve user from session");
			}
		}

		return user;
	}

	/**
	 * This method returns a <code>Map</code> with the keys and values extracted from <code>params</code>
	 *
	 *
	 * @param params a string in the form of "/key/value/.../key/value"
	 * @return a <code>Map</code> with the keys and values extracted from <code>params</code>
	 */

	private Map<String, String> buildParamsMap(String params) {

		if (params.startsWith("/")) {
			params = params.substring(1);
		}
		String[] pathParts = params.split("/");
		Map<String, String> pathMap = new HashMap<String, String>();
		for (int i=0; i < pathParts.length/2; i++) {
			String key = pathParts[2*i].toLowerCase();
			String value = pathParts[2*i+1];
			pathMap.put(key, value);
		}
		return pathMap;
	}


	private void checkForceSSL(HttpServletRequest request) {
		if(Config.getBooleanProperty("FORCE_SSL_ON_RESP_API", false) && UtilMethods.isSet(request) && !request.isSecure())
			throw new SecurityException("SSL Required.", Response.Status.FORBIDDEN);

	}

}
