package com.dotcms.cms.login;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.util.ReflectionUtils;
import com.dotcms.util.security.EncryptorFactory;
import com.dotmarketing.cms.login.struts.LoginForm;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.model.User;

/**
 * Login Service Factory that allows developers to inject custom login services.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 20, 2016
 */
public class LoginServiceFactory implements Serializable {

	/**
	 * Used to keep the instance of the {@link LoginService}. Should be volatile
	 * to avoid thread-caching
	 */
    private volatile LoginService loginService = null;

	/**
	 * Get the login service implementation from the
	 * dotmarketing-config.properties
	 */
    public static final String LOGIN_SERVICE_IMPLEMENTATION_KEY = "login.service.implementation";

    private LoginServiceFactory() {
        // singleton
    }

    private static class SingletonHolder {
        private static final LoginServiceFactory INSTANCE = new LoginServiceFactory();
    }

	/**
	 * Get the instance.
	 * 
	 * @return EncryptorFactory
	 */
    public static LoginServiceFactory getInstance() {

        return LoginServiceFactory.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Returns the custom Login Service, or the default implementation.
     * 
     * @return The {@link LoginService}.
     */
    public LoginService getLoginService () {

        String loginServiceFactoryClass = null;

        if (null == this.loginService) {

            synchronized (EncryptorFactory.class) {

                if (null == this.loginService) {

                    loginServiceFactoryClass =
                            Config.getStringProperty
                                    (LOGIN_SERVICE_IMPLEMENTATION_KEY, null);

                    if (UtilMethods.isSet(loginServiceFactoryClass)) {

                        if (Logger.isDebugEnabled(LoginServiceFactory.class)) {

                            Logger.debug(LoginServiceFactory.class,
                                    "Using the login service class: " + loginServiceFactoryClass);
                        }

                        this.loginService =
                                (LoginService) ReflectionUtils.newInstance(loginServiceFactoryClass);

                        if (null == this.loginService) {

                            if (Logger.isDebugEnabled(LoginServiceFactory.class)) {

                                Logger.debug(LoginServiceFactory.class,
                                        "Could not used this class: " + loginServiceFactoryClass +
                                                ", using the default implementations");
                            }

                            this.loginService =
                                    new LoginServiceFactory.LoginServiceImpl();
                        }
                    } else {

                        this.loginService =
                                new LoginServiceFactory.LoginServiceImpl();
                    }
                }
            }
        }

        return this.loginService;
    }

    /**
     * Default implementation
     */
    private final class LoginServiceImpl implements LoginService {

        @Override
        public boolean doLogin(final LoginForm form,
                               final HttpServletRequest request,
                               final HttpServletResponse response) throws NoSuchUserException {

            return LoginService.super.doLogin(form, request, response);
        }

        @Override
        public boolean doCookieLogin(final String encryptedId, final HttpServletRequest request, final HttpServletResponse response) {

            return LoginService.super.doCookieLogin(encryptedId, request, response);
        }

        @Override
        public boolean doLogin(final String userName, final String password, final boolean rememberMe,
                               final HttpServletRequest request, final HttpServletResponse response) throws NoSuchUserException {

            return LoginService.super.doLogin(userName, password, rememberMe, request, response);
        }

        @Override
        public boolean doLogin(final String userName, final String password, final boolean rememberMe,
                               final HttpServletRequest request,
                               final HttpServletResponse response,
                               final boolean skipPasswordCheck) throws NoSuchUserException {

            return LoginService.super.doLogin(userName, password, rememberMe, request, response, skipPasswordCheck);
        }

        @Override
        public boolean doLogin(final String userName, final String password) throws NoSuchUserException {

            return LoginService.super.doLogin(userName, password);
        }

        @Override
        public void doLogout(final HttpServletRequest request, final HttpServletResponse response) {

            LoginService.super.doLogout(request, response);
        }

        @Override
        public boolean passwordMatch(String password, User user) {
            return false;
        }
    }

} // E:O:F:LoginServiceFactory.
