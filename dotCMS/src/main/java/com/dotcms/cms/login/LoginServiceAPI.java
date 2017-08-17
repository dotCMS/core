package com.dotcms.cms.login;


import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;

import static com.dotcms.util.FunctionUtils.ifTrue;

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

    public static boolean useCASLoginFilter = new Boolean (Config.getBooleanProperty("FRONTEND_CAS_FILTER_ON",false));

    // Default max days for the JWT
    public static final int JSON_WEB_TOKEN_DAYS_MAX_AGE_DEFAULT = 14;

    // max expiration day allowed for a json web token
    public static final String JSON_WEB_TOKEN_MAX_ALLOWED_EXPIRATION_DAYS = "json.web.token.max.allowed.expiration.days";


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
     boolean doCookieLogin(final String encryptedId, final HttpServletRequest request, final HttpServletResponse response) ;



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
    boolean doLogin(final String userName, final String password,
                            final boolean rememberMe, final HttpServletRequest request,
                                          final HttpServletResponse response) throws NoSuchUserException ;

    
    /**
     * 
     * @param userName
     * @param password
     * @return
     * @throws NoSuchUserException
     */
    boolean doLogin(final String userName, final String password) throws NoSuchUserException ;
    /**
     * 
     * @param request
     * @param response
     */
    void doLogout(final HttpServletRequest request, final HttpServletResponse response) ;


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

    boolean passwordMatch(String password, User user) throws DotSecurityException;

    

} // E:O:F:LoginServiceAPI.
