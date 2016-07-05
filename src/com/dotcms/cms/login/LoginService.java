package com.dotcms.cms.login;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.cms.login.struts.LoginForm;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.model.User;

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

} // E:O:F:LoginService.
