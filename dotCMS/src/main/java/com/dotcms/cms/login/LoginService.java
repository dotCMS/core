package com.dotcms.cms.login;

import java.io.Serializable;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.cms.login.struts.LoginForm;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PreviewFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.auth.PrincipalFinder;
import com.liferay.portal.ejb.UserLocalManagerUtil;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.events.EventsProcessor;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.CookieUtil;
import com.liferay.util.InstancePool;

/**
 * Encapsulates the login services This class is just a wrapper to encapsulate
 * the {@link com.dotmarketing.cms.login.factories.LoginFactory} This approach
 * provides the ability to inject, proxy, mock, use diff implementation based on
 * a contract etc.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 20, 2016
 */
public interface LoginService extends Serializable {

    public static final String JSON_WEB_TOKEN_DAYS_MAX_AGE = "json.web.token.days.max.age";

    // Default max days for the JWT
    public static final int JSON_WEB_TOKEN_DAYS_MAX_AGE_DEFAULT = 14;

    // max expiration day allowed for a json web token
    public static final String JSON_WEB_TOKEN_MAX_ALLOWED_EXPIRATION_DAYS = "json.web.token.max.allowed.expiration.days";

    /**
     * Calls the event processor, kill cookies, portlets session and the http session.
     * @param req
     * @param res
     * @throws Exception
     */
    void doActionLogout(final HttpServletRequest req, final HttpServletResponse res) throws Exception;

    /**
     * Checks whether the current request belongs to a user that has already
     * been authenticated.
     *
     * @param req
     *            - The {@link HttpServletRequest} object.
     * @return If the request belongs to an authenticated user, returns
     *         {@code true}. Otherwise, returns {@code false}.
     */
    default boolean isLoggedIn (final HttpServletRequest req) {

        return null != PortalUtil.getUserId(req);
    }

    /**
     * Do the action login based on an userId, pass and rememberMe
     * This is basically a refactor from {@link com.liferay.portal.action.LoginAction} to encapsulate the login action into a
     * method that can be use by {@link com.liferay.portal.action.LoginAction} and the {@link com.dotcms.rest.api.v1.authentication.AuthenticationResource}
     * (web and rest json services)
     * If the user can be authenticate, will return true and create all the var's in session
     * In addition a JWT cookie will be created.
     *
     * @param userId
     * @param password
     * @param rememberMe
     * @param req
     * @param res
     * @return boolean
     */
    boolean doActionLogin(final String userId, final String password, final boolean rememberMe,
                             final HttpServletRequest req, final HttpServletResponse res) throws Exception ;

	/**
	 * 
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws NoSuchUserException
	 */
    default boolean doLogin(final LoginForm form,
                            final HttpServletRequest request,
                            final HttpServletResponse response) throws NoSuchUserException {

        return LoginFactory.doLogin(form, request, response);
    }

    /**
     * 
     * @param encryptedId
     * @param request
     * @param response
     * @return
     */
    default boolean doCookieLogin(final String encryptedId, final HttpServletRequest request, final HttpServletResponse response) {

        return LoginFactory.doCookieLogin(encryptedId, request, response);
    }

    /**
     * 
     * @param userName
     * @param password
     * @param rememberMe
     * @param request
     * @param response
     * @return
     * @throws NoSuchUserException
     */
    default boolean doLogin(final String userName, final String password,
                            final boolean rememberMe, final HttpServletRequest request,
                                          final HttpServletResponse response) throws NoSuchUserException {

        return LoginFactory.doLogin(userName, password, rememberMe, request, response);
    }

    /**
     * 
     * @param userName
     * @param password
     * @param rememberMe
     * @param request
     * @param response
     * @param skipPasswordCheck
     * @return
     * @throws NoSuchUserException
     */
    default boolean doLogin(final String userName, final String password,
                            final boolean rememberMe, final HttpServletRequest request,
                            final HttpServletResponse response,
                            final boolean skipPasswordCheck) throws NoSuchUserException {

        return LoginFactory.doLogin(userName, password, rememberMe,
                request, response, skipPasswordCheck);
    }

    /**
     * 
     * @param userName
     * @param password
     * @return
     * @throws NoSuchUserException
     */
    default boolean doLogin(final String userName, final String password) throws NoSuchUserException {

        return LoginFactory.doLogin(userName, password);
    }

    /**
     * 
     * @param request
     * @param response
     */
    default void doLogout(final HttpServletRequest request, final HttpServletResponse response) {

        LoginFactory.doLogout(request, response);
    }

	/**
	 * This method validates legacy, clear and new passwords. When identifies a
	 * clear or legacy password it will validate and then change it to a
	 * stronger hash then save it into the system.
	 * 
	 * @param password
	 *            string entered by the user
	 * @param user
	 *            MUST be loaded from db with all the information because we are
	 *            going to save this object if rehash process required
	 * @return If passwords match, returns {@code true}. Otherwise, returns
	 *         {@code false}.
	 */
    default boolean passwordMatch(final String password, final User user) {

        return LoginFactory.passwordMatch(password, user);
    }

    /**
     * Return the current login user.
     *
     * @param req
     * @return login user
     */
    User getLogInUser( HttpServletRequest req );
} // E:O:F:LoginService.
