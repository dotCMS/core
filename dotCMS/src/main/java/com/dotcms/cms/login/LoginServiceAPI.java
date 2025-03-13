package com.dotcms.cms.login;

import static com.dotcms.util.FunctionUtils.ifTrue;

import com.dotmarketing.util.Config;
import io.vavr.Lazy;
import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;

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
public interface LoginServiceAPI extends Serializable {

    public static final String JSON_WEB_TOKEN_DAYS_MAX_AGE = "json.web.token.days.max.age";

    // Default max days for the JWT
    public static final int JSON_WEB_TOKEN_DAYS_MAX_AGE_DEFAULT = 14;

    // max expiration day allowed for a json web token
    public static final String JSON_WEB_TOKEN_MAX_ALLOWED_EXPIRATION_DAYS = "json.web.token.max.allowed.expiration.days";

    public final static Lazy<Integer> JWT_TOKEN_MAX_AGE_DAYS = Lazy.of(() -> Config.getIntProperty(
            JSON_WEB_TOKEN_DAYS_MAX_AGE,
            JSON_WEB_TOKEN_DAYS_MAX_AGE_DEFAULT));
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
     * If the user can be authenticate, will returns true and create all the var's in session (including the prevention of the session fixation)
     * In addition a JWT cookie will be created.
     *
     * @param userId
     * @param password
     * @param rememberMe boolean if true will create the JWT access token cookie in order to remember the user
     * @param request
     * @param response
     * @return boolean
     */
    boolean doActionLogin(final String userId, final String password, final boolean rememberMe,
                             final HttpServletRequest request, final HttpServletResponse response) throws Exception ;

    /**
     * When an user is being logged in, the previous session must be invalidated and created a new one.
     * The default behavior does this, however it is able to turn off by using PREVENT_SESSION_FIXATION_ON_LOGIN on the dotmarketing config.
     * Note: if does not exists a current session the preventSessionFixation won't do anything and will return a null session, so that you can make the decision
     * to create a new one or no. If exists will invalidate it and create a new one.
     * @param request HttpServletRequest
     * @return HttpSession return the new session in case it is created, otherwise returns the same one
     */
    HttpSession preventSessionFixation(final HttpServletRequest request);



    /**
     * Do the remember me, usually a cookie approach with a specific strategy such as JWT
     * If the param rememberMe is true, the remember will create a cookie for a configurable time, otherwise will be removed after the session.
     * @param req {@link HttpServletRequest}
     * @param res {@link HttpServletResponse}
     * @param user {@link User}
     * @param rememberMe {@link Boolean}
     */
    void doRememberMe(final HttpServletRequest req,
                      final HttpServletResponse res,
                      final User user,
                      final boolean rememberMe);

    /**
     * Do the remember me, usually a cookie approach with a specific strategy such as JWT
     *
     * @param req {@link HttpServletRequest}
     * @param res {@link HttpServletResponse}
     * @param user {@link User}
     * @param maxAge {@link Integer} if maxAge is negative, means the cookie will be create just for the session.
     */
    void doRememberMe(final HttpServletRequest req,
                      final HttpServletResponse res,
                      final User user,
                      final int maxAge);

    /**
     * Do the login based one on a encryptedId, usually a strategy to follow on a cookie.
     * @param encryptedId {@link String}
     * @param request {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @return Boolean
     */
    default boolean doCookieLogin(final String encryptedId, final HttpServletRequest request, final HttpServletResponse response) {

        return LoginFactory.doCookieLogin(encryptedId, request, response);
    }

    /**
     * Do the login based one on a encryptedId, usually a strategy to follow on a cookie.
     * @param encryptedId {@link String}
     * @param request {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @param rememberMe {@link Boolean} if it is true the cookie for remember me will be created.
     * @return Boolean
     */
    default boolean doCookieLogin(final String encryptedId,
                                  final HttpServletRequest request,
                                  final HttpServletResponse response,
                                  final boolean rememberMe) {

        return
                ifTrue(this.doCookieLogin(encryptedId, request, response),
                        () -> this.doRememberMe(request, response, this.getLoggedInUser(request), rememberMe));
    } // doCookieLogin.

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
    User getLoggedInUser(HttpServletRequest req );

    /**
     * Return the current login user.
     *
     * @return login user, if a user is login otherwise return System User
     */
    User getLoggedInUser( );

    /**
     * Performs a backendlogin that checks to insure that 
     * 1. the user has the Role: Back End User and
     * 2. that the user has layouts
     * @param userId
     * @param password
     * @param rememberMe
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    boolean doBackEndLogin(String userId, String password, boolean rememberMe, HttpServletRequest request, HttpServletResponse response)
        throws Exception;
} // E:O:F:LoginServiceAPI.
