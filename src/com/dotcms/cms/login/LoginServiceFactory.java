package com.dotcms.cms.login;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.jwt.beans.DotCMSSubjectBean;
import com.dotcms.auth.providers.jwt.beans.JWTBean;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.util.ReflectionUtils;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotcms.util.security.EncryptorFactory;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.cms.login.struts.LoginForm;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PreviewFactory;
import com.dotmarketing.util.*;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.auth.PrincipalFinder;
import com.liferay.portal.ejb.UserLocalManagerUtil;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.events.EventsProcessor;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.servlet.PortletSessionPool;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.CookieUtil;
import com.liferay.util.InstancePool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.dotmarketing.util.CookieUtil.createJsonWebTokenCookie;

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

        private final Log log = LogFactory.getLog(LoginService.class);
        private final UserWebAPI userWebAPI;
        private final JsonWebTokenUtils jsonWebTokenUtils;

        @VisibleForTesting
        public LoginServiceImpl(final ApiProvider apiProvider,
                                final JsonWebTokenUtils jsonWebTokenUtils){

            this.userWebAPI        = apiProvider.userWebAPI();
            this.jsonWebTokenUtils = jsonWebTokenUtils;
        }

        public LoginServiceImpl(){
            this(new ApiProvider(), JsonWebTokenUtils.getInstance());
        }

        @Override
        public void doActionLogout(final HttpServletRequest req,
                                      final HttpServletResponse res) throws Exception {

            final HttpSession session = req.getSession(false);

            if (null != session) {

                log.debug("Logout - Events Processor Pre Logout events.");

                EventsProcessor.process(PropsUtil.getArray(PropsUtil.LOGOUT_EVENTS_PRE), req, res);

                log.debug("Logout - Set expire cookies");
                com.dotmarketing.util.CookieUtil.setExpireCookies(req, res);

                final Map sessions = PortletSessionPool.remove(session.getId());

                if (null != sessions) {

                    log.debug("Logout - Invalidating portlet sessions...");

                    final Iterator itr = sessions.values().iterator();

                    while (itr.hasNext()) {

                        final HttpSession portletSession = (HttpSession) itr.next();

                        if (null != portletSession) {

                            portletSession.invalidate();
                        }
                    }
                }

                log.debug("Logout - Invalidating http session...");

                session.invalidate();

                log.debug("Logout - Events Processor Post Logout events.");

                EventsProcessor.process(PropsUtil.getArray(PropsUtil.LOGOUT_EVENTS_POST), req, res);
            } else {

                log.debug("Not action needed, since the session is already ended.");
            }
        } // doActionLogout.

        @Override
        public boolean isLoggedIn(final HttpServletRequest req) {

            return LoginService.super.isLoggedIn(req);
        }

        @Override
        public boolean doActionLogin(String userId,
                                     final String password,
                                     final boolean rememberMe,
                                     final HttpServletRequest req,
                                     final HttpServletResponse res) throws Exception {

            boolean authenticated = false;
            int authResult = Authenticator.FAILURE;

            final Company company = PortalUtil.getCompany(req);

            //Search for the system user
            final User systemUser = APILocator.getUserAPI().getSystemUser();

            if ( Company.AUTH_TYPE_EA.equals(company.getAuthType()) ) {

                //Verify that the System User is not been use to log in inside the system
                if ( systemUser.getEmailAddress().equalsIgnoreCase( userId ) ) {
                    SecurityLogger.logInfo(this.getClass(),"An invalid attempt to login as a System User has been made  - you cannot login as the System User");
                    throw new AuthException( "Unable to login as System User - you cannot login as the System User." );
                }

                authResult = UserManagerUtil.authenticateByEmailAddress( company.getCompanyId(), userId, password );
                userId = UserManagerUtil.getUserId( company.getCompanyId(), userId );

            } else {

                //Verify that the System User is not been use to log in inside the system
                if ( systemUser.getUserId().equalsIgnoreCase( userId ) ) {
                    SecurityLogger.logInfo(this.getClass(),"An invalid attempt to login as a System User has been made  - you cannot login as the System User");
                    throw new AuthException( "Unable to login as System User - you cannot login as the System User." );
                }

                authResult = UserManagerUtil.authenticateByUserId( company.getCompanyId(), userId, password );
            }

            try {

                PrincipalFinder principalFinder =
                        (PrincipalFinder) InstancePool.get(
                                PropsUtil.get(PropsUtil.PRINCIPAL_FINDER));

                userId = principalFinder.fromLiferay(userId);
            }
            catch (Exception e) {

                // quiet
            }

            if (authResult == Authenticator.SUCCESS) {

                this.doAuthentication(userId, rememberMe, req, res);
                authenticated = true;
            }

            if (authResult != Authenticator.SUCCESS) {

                SecurityLogger.logInfo(this.getClass(), "An invalid attempt to login as " + userId + " has been made from IP: " + req.getRemoteAddr());
                throw new AuthException();
            }

            SecurityLogger.logInfo(this.getClass(), "User " + userId + " has successfully login from IP: " + req.getRemoteAddr());

            return authenticated;
        }

        private void doAuthentication(String userId, boolean rememberMe, HttpServletRequest req, HttpServletResponse res) throws PortalException, SystemException, DotDataException, DotSecurityException {

            final HttpSession ses = req.getSession();
            final User user = UserLocalManagerUtil.getUserById(userId);

            //DOTCMS-4943
            final UserAPI userAPI = APILocator.getUserAPI();
            final boolean respectFrontend = WebAPILocator.getUserWebAPI().isLoggedToBackend(req);
            final Locale userSelectedLocale = LanguageUtil.getDefaultLocale(req);
            if (null != userSelectedLocale) {

                user.setLanguageId(userSelectedLocale.toString());
            }

            userAPI.save(user, userAPI.getSystemUser(), respectFrontend);

            ses.setAttribute(WebKeys.USER_ID, userId);

            //DOTCMS-6392
            PreviewFactory.setVelocityURLS(req);

            //set the host to the domain of the URL if possible if not use the default host
            //http://jira.dotmarketing.net/browse/DOTCMS-4475
            try{

                String domainName = req.getServerName();
                Host h = null;
                h = APILocator.getHostAPI().findByName(domainName, user, false);
                if(h == null || !UtilMethods.isSet(h.getInode())){
                    h = APILocator.getHostAPI().findByAlias(domainName, user, false);
                }
                if(h != null && UtilMethods.isSet(h.getInode())){
                    req.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, h.getIdentifier());
                }else{
                    req.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), true).getIdentifier());
                }
            } catch (DotSecurityException se) {

                req.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), true).getIdentifier());
            }

            ses.removeAttribute("_failedLoginName");

            //JWT we crT always b/c in the future we want to use it not only for the remember me, but also for restful authentication.
            int jwtMaxAge = rememberMe ? Config.getIntProperty(
                    JSON_WEB_TOKEN_DAYS_MAX_AGE,
                    JSON_WEB_TOKEN_DAYS_MAX_AGE_DEFAULT) : -1;

            this.processJsonWebToken(req, res, user, jwtMaxAge);

            EventsProcessor.process(PropsUtil.getArray(PropsUtil.LOGIN_EVENTS_PRE), req, res);
            EventsProcessor.process(PropsUtil.getArray(PropsUtil.LOGIN_EVENTS_POST), req, res);
        }

        /**
         * Generates the JWT and its respective cookie based on the following
         * criteria:
         * <ul>
         * <li>Information from the user: ID, associated company.</li>
         * <li>The "Remember Me" option being checked or not.</li>
         * </ul>
         * The JWT will allow the user to access the dotCMS back-end even though
         * their session has expired. This way, they won't need to re-authenticate.
         *
         * @param req
         *            - The {@link HttpServletRequest} object.
         * @param res
         *            - The {@link HttpServletResponse} object.
         * @param user
         *            - The {@link User} trying to log into the system.
         * @param maxAge
         *            - The maximum days (in milliseconds) that the JWT will live.
         *            If the "Remember Me" option is checked, the token will live
         *            for many days (check its default value). Otherwise, it will
         *            only live during the current session.
         * @throws PortalException
         *             The specified user could not be found.
         * @throws SystemException
         *             An error occurred during the user ID encryption.
         */
        private void processJsonWebToken(final HttpServletRequest req,
                                         final HttpServletResponse res,
                                         final User user,
                                         final int maxAge) throws PortalException, SystemException {

            final String jwtAccessToken = this.jsonWebTokenUtils.createToken(user, maxAge);
            createJsonWebTokenCookie(req, res, jwtAccessToken, Optional.of(maxAge));
        }


        @Override
        public boolean doLogin(final LoginForm form,
                               final HttpServletRequest request,
                               final HttpServletResponse response) throws NoSuchUserException {

            return LoginService.super.doLogin(form, request, response);
        }

        @Override
        public boolean doCookieLogin(final String encryptedId, final HttpServletRequest request, final HttpServletResponse response) {

            final boolean doCookieLogin = LoginService.super.doCookieLogin(encryptedId, request, response);

            if (doCookieLogin) {

                final String decryptedId = PublicEncryptionFactory.decryptString(encryptedId);
                final HttpSession session = request.getSession(false);
                if (null != session && null != decryptedId) {
                    // this is what the PortalRequestProcessor needs to check the login.
                    session.setAttribute(WebKeys.USER_ID, decryptedId);
                } //
            }

            return doCookieLogin;
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
            return LoginFactory.passwordMatch(password, user);
        }

        /**
         * Return the current login user.
         * A {@link UserLoggingException} is thrown if any error happened getting the user.
         *
         * @param req
         * @return login user
         */
        public User getLogInUser( HttpServletRequest req ){
            User user = null;

            if(req != null) {
                try {
                    user = userWebAPI.getLoggedInUser(req);
                } catch (PortalException|SystemException e) {
                    throw new UserLoggingException( e );
                }
            }
            return user;
        }
    }

} // E:O:F:LoginServiceFactory.
