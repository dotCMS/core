package com.dotcms.cms.login;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.dotcms.util.ReflectionUtils;
import com.dotcms.util.security.EncryptorFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * Login Service Factory that allows developers to inject custom login services.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 20, 2016
 */
public class LoginServiceAPIFactory implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Used to keep the instance of the {@link LoginServiceAPI}. Should be volatile to avoid
     * thread-caching
     */
    private volatile LoginServiceAPI loginService = null;

    /**
     * Get the login service implementation from the dotmarketing-config.properties
     */
    public static final String LOGIN_SERVICE_IMPLEMENTATION_KEY = "login.service.implementation";

    private LoginServiceAPIFactory() {
        // singleton
    }

    private static class SingletonHolder {
        private static final LoginServiceAPIFactory INSTANCE = new LoginServiceAPIFactory();
    }

    /**
     * Get the instance.
     * 
     * @return EncryptorFactory
     */
    public static LoginServiceAPIFactory getInstance() {

        return LoginServiceAPIFactory.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Returns the custom Login Service, or the default implementation.
     * 
     * @return The {@link LoginServiceAPI}.
     */
    public LoginServiceAPI getLoginService() {

        String loginServiceFactoryClass = null;

        if (null == this.loginService) {

            synchronized (EncryptorFactory.class) {

                if (null == this.loginService) {

                    loginServiceFactoryClass = Config
                                    .getStringProperty(LOGIN_SERVICE_IMPLEMENTATION_KEY, null);

                    if (UtilMethods.isSet(loginServiceFactoryClass)) {

                        if (Logger.isDebugEnabled(LoginServiceAPIFactory.class)) {

                            Logger.debug(LoginServiceAPIFactory.class,
                                            "Using the login service class: "
                                                            + loginServiceFactoryClass);
                        }

                        this.loginService = (LoginServiceAPI) ReflectionUtils
                                        .newInstance(loginServiceFactoryClass);

                        if (null == this.loginService) {

                            if (Logger.isDebugEnabled(LoginServiceAPIFactory.class)) {

                                Logger.debug(LoginServiceAPIFactory.class,
                                                "Could not used this class: "
                                                                + loginServiceFactoryClass
                                                                + ", using the default implementations");
                            }

                            this.loginService = new LoginServiceAPIImpl();
                        }
                    } else {

                        this.loginService = new LoginServiceAPIImpl();
                    }
                }
            }
        }

        return this.loginService;
    }


} // E:O:F:LoginServiceAPIFactory.
