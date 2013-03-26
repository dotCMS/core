package com.dotcms.rest;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

public class WebResource {

	protected static final String RENDER = "render";
	protected static final String TYPE = "type";
	protected static final String QUERY = "query";
	protected static final String ORDERBY = "orderby";
	protected static final String LIMIT = "limit";
	protected static final String OFFSET = "offset";
	protected static final String USER = "user";
	protected static final String PASSWORD = "password";
	protected static final String ID = "id";
	protected static final String INODE = "inode";
	protected static final String LIVE = "live";
	protected static final String LANGUAGE = "language";

	public enum AuthType {
	    NO_AUTH,
	    PARAMS,
	    SESSION,
	    PARAMS_OR_SESSION,
	}

	/**
	 * <p>Checks if SSL is required. If so, throws a ForbiddenException when no secure request is provided.
	 *
	 * @param request
	 */

	protected void init(HttpServletRequest request) {
		checkForceSSL(request);
	}

	/**
	 * <p>1) Checks if SSL is required. If so, throws a ForbiddenException when no secure request is provided.
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
	 * <p>1) Checks if SSL is required. If so, throws a <code>ForbiddenException</code> when no secure <code>request</code> is provided.
	 * <p>2) If 1) does not throw an exception, returns an {@link InitDataObject} with:
	 *
	 * <br>a) a <code>Map</code> with the keys and values extracted from <code>params</code>
	 *
	 * <br><br>b) a {@link User}, according to <code>authType</code>.
	 * <br>	If authType is NO_AUTH, performs no authentication and returns null.
	 * <br>	If authType is PARAMS, gets the username and password from <code>params</code> and tries to authenticate.
	 * <br>	If authType is SESSION, tries to get the logged in user from <code>request</code>.
	 * <br>	If authType is PARAMS_OR_SESSION, acts like PARAMS if both username and password are contained in <code>params</code>. If not, acts like SESSION.
	 *
	 *
	 * @param params a string containing parameters in the /key/value form
	 * @param authType
	 * @param request
	 * @param rejectWhenNoUser determines whether a ForbiddenException is thrown or not when authentication fails.
	 * @return an initDataObject with the resulting <code>Map</code>
	 */

	protected InitDataObject init(String params, AuthType authType, HttpServletRequest request, boolean rejectWhenNoUser) {

		checkForceSSL(request);

		InitDataObject initData = new InitDataObject();

		if(!UtilMethods.isSet(params))
			return initData;

		Map<String, String> paramsMap = buildParamsMap(params);
		User user = null;

		user = authenticateUser(paramsMap, request, rejectWhenNoUser, authType);

		initData.setParamsMap(paramsMap);
		initData.setUser(user);

		return initData;
	}

	/**
	 * <p>Tries to authenticate depending on <code>authType</code> param.
	 * <br>	If authType is NO_AUTH, performs no authentication and returns null.
	 * <br>	If authType is PARAMS, gets the username and password from <code>params</code> and tries to authenticate.
	 * <br>	If authType is SESSION, tries to get the logged in user from <code>request</code>.
	 * <br>	If authType is PARAMS_OR_SESSION, acts like PARAMS if both username and password are contained in <code>params</code>. If not, acts like SESSION.
	 *
	 * @param paramsMap
	 * @param request
	 * @param rejectWhenNoUser determines whether a ForbiddenException is thrown or not when authentication fails.
	 * @param authType
	 * @return authenticated user
	 */

	private User authenticateUser(Map<String, String> paramsMap, HttpServletRequest request, boolean rejectWhenNoUser, AuthType authType) throws ForbiddenException {

		switch (authType) {
		case NO_AUTH:
			return null;

		case PARAMS:
			return authenticateUserFromParams(paramsMap, request, rejectWhenNoUser);

		case SESSION:
			return getUserFromRequest(request, rejectWhenNoUser);

		case PARAMS_OR_SESSION:
			User user = authenticateUserFromParams(paramsMap, request, rejectWhenNoUser);

			if(user==null)
				user = getUserFromRequest(request, rejectWhenNoUser);

			return user;

		default:
			return null;
		}
	}

	/**
	 * Tries to authenticate with the username and password contained in the paramsMap, if set.
	 *
	 * @param paramsMap
	 * @param req
	 * @param rejectWhenNoUser determines whether a ForbiddenException is thrown or not when authentication fails.
	 * @return
	 */

	private User authenticateUserFromParams(Map<String, String> paramsMap, HttpServletRequest req, boolean rejectWhenNoUser) {
		User user = null;
		String ip = req!=null?req.getRemoteAddr():"";

		String username = paramsMap.get(USER);
		String password = paramsMap.get(PASSWORD);

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

					Logger.warn(this.getClass(), "Request IP: " + ip + ".Can't authenticate user. Username: " + username);
					SecurityLogger.logDebug(this.getClass(), "Request IP: " + ip + ".Can't authenticate user. Username: " + username);
					throw new ForbiddenException("Forbidden Resource");
				}

			}  catch (Exception e) {  // doLogin throwing Exception

				Logger.warn(this.getClass(), "Request IP: " + ip + ".Can't authenticate user. Username: " + username);
				SecurityLogger.logDebug(this.getClass(), "Request IP: " + ip + ".Can't authenticate user. Username: " + username);
				throw new ForbiddenException("Forbidden Resource");
			}

		} else if(UtilMethods.isSet(username) || UtilMethods.isSet(password)){ // providing login or password

			Logger.warn(this.getClass(), "Request IP: " + ip + ".Can't authenticate user.");
			SecurityLogger.logDebug(this.getClass(), "Request IP: " + ip + ".Can't authenticate user.");
			throw new ForbiddenException("Forbidden Resource");
		}

		return user;
	}

	/**
	 * This method returns the logged in user from request. If no user can be retrieved and rejectWhenNoUser is true, throws a ForbiddenException,
	 * otherwise returns null.
	 *
	 * @param request
	 * @param rejectWhenNoUser Determines whether an exception will be thrown or not when no user can be retrieved
	 * @return
	 */

	private User getUserFromRequest(HttpServletRequest req, boolean rejectWhenNoUser) {
		User user = null;

		if(UtilMethods.isSet(req)) { // let's check if we have a request and try to get the user logged in from it
			try {
				user = WebAPILocator.getUserWebAPI().getLoggedInUser(req);
			}  catch (Exception e) {

				if(Config.getBooleanProperty("REJECT_REST_API_WITH_NO_USER", false) || rejectWhenNoUser) {
					Logger.warn(this.getClass(), "Can't retrieve user from session");
					throw new ForbiddenException("Forbidden Resource");
				} else {
					Logger.warn(this.getClass(), "Can't retrieve user from session");
				}
			}
		} else if(Config.getBooleanProperty("REJECT_REST_API_WITH_NO_USER", false) || rejectWhenNoUser) { // we don't have a request. If rejectWhenNoUser is true, throw exception
			Logger.warn(this.getClass(), "No Valid Request");
			throw new ForbiddenException("Forbidden Resource");
		} else {
			Logger.warn(this.getClass(), "No Valid Request");
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
			throw new ForbiddenException("SSL Required.");

	}

}
