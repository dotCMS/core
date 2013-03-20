package com.dotcms.rest;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
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

	protected User authenticateUser(String username, String password, HttpServletRequest req) throws DotDataException, DotSecurityException {
		User user = null;

			if(UtilMethods.isSet(username) && UtilMethods.isSet(password)) { // providing login and password so let's try to authenticate

				try {
					if(LoginFactory.doLogin(username, password)) {
						Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();

						if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
							user = APILocator.getUserAPI().loadByUserByEmail(username, APILocator.getUserAPI().getSystemUser(), false);
						} else {
							user = APILocator.getUserAPI().loadUserById(username, APILocator.getUserAPI().getSystemUser(), false);
						}
					} else {
						Logger.debug(this.getClass(), "No Such User Found. Username: " + username + ", Password: " + password);
					}

				}  catch (com.liferay.portal.NoSuchUserException e1) {
					Logger.debug(this.getClass(), "No Such User Found. Username: " + username + ", Password: " + password);
					SecurityLogger.logDebug(this.getClass(), "No Such User Found. Username: " + username + ", Password: " + password);
				}

			} else {  // neither providing login nor password, so let's check if we have a user logged in
				try {
					user = WebAPILocator.getUserWebAPI().getLoggedInUser(req);
				}  catch (Exception e) {
					Logger.debug(this.getClass(), "Can't retrieve user from session");
				}
			}

		return user;
	}

	protected Map<String, String> parsePath(String path) {

		/* Getting the values of the URL params passed in the /key/value/ syntax*/

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
