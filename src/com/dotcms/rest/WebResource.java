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


	/**
	 * Tries to authenticate with given user/password. If succeed, returns the authenticated user.
	 * If do not succeed, throws a ForbiddenException when rejectWhenNoUser is TRUE,
	 * or null if rejectWhenNoUser is FALSE
	 *
	 * @param username
	 * @param password
	 * @param req
	 * @param rejectWhenNoUser if TRUE, throws a ForbiddenException when authentication fails.
	 * @return authenticated user
	 */

	protected User authenticateUser(String username, String password, HttpServletRequest req, boolean rejectWhenNoUser) throws ForbiddenException {
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
					Logger.warn(this.getClass(), "Request IP: " + ip + ". No Such User Found. Username: " + username + ", Password: " + password);
					SecurityLogger.logDebug(this.getClass(), "Request IP: " + ip + ".No Such User Found. Username: " + username + ", Password: " + password);
					throw new ForbiddenException("Forbidden Resource");
				}

			}  catch (Exception e) {  // doLogin throwing Exception
				//
				if(Config.getBooleanProperty("REJECT_REST_API_WITH_NO_USER", false) || rejectWhenNoUser) {
					Logger.warn(this.getClass(), "Request IP: " + ip + ".No Such User Found. Username: " + username + ", Password: " + password);
					SecurityLogger.logDebug(this.getClass(), "Request IP: " + ip + ".No Such User Found. Username: " + username + ", Password: " + password);
					throw new ForbiddenException("Forbidden Resource");
				}
			}

		} else {  // neither providing login nor password
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

		}

		return user;
	}

	/**
	 * This method gets the key and the value of any given path in the "/key/value/.../key/value" form
	 * and returns a Map containing those key/value entries
	 *
	 * @param path a String in the form of "/key/value/.../key/value"
	 * @return map with the key/value entries taken out from the path parameter
	 */

	protected Map<String, String> parsePath(String path) {

		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		String[] pathParts = path.split("/");
		Map<String, String> pathMap = new HashMap<String, String>();
		for (int i=0; i < pathParts.length/2; i++) {
			String key = pathParts[2*i].toLowerCase();
			String value = pathParts[2*i+1];
			pathMap.put(key, value);
		}
		return pathMap;
	}

}
