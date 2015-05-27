package com.dotcms.rest.config;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.base.Optional;
import com.dotcms.repackage.com.sun.jersey.core.util.Base64;
import com.dotcms.repackage.com.sun.jersey.spi.container.ContainerRequest;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.rest.validation.ServletPreconditions;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;

/**
 * Proxy the user authentication behind non-static method calls.
 * Exists to enable unit testing of ReST Resources.
 *
 * Note that this class shares the same issues as discussed in ApiProvider.
 *
 * Borrows from WebResource, with much modification.
 * @author Geoff M. Grnaum
 * @version 1.0.0
 * @since 3.2.0
 */
public class AuthenticationProvider {

    private final UserAPI userAPI;
    private final UserWebAPI userWebAPI;

    public AuthenticationProvider(ApiProvider apiProvider) {
        this.userAPI = apiProvider.userAPI();
        this.userWebAPI = apiProvider.userWebAPI();
    }

    /**
     * Returns an authenticated {@link User}. There are four ways to get the User's credentials.
     * They are executed in the specified order. When found, the remaining ways won't be executed.
     * <br>1) Using username and password in Base64 contained in the <code>request</code> HEADER parameter DOTAUTH.
     * <br>2) Using username and password in Base64 contained in the <code>request</code> HEADER parameter AUTHORIZATION (BASIC Auth).
     * <br>3) From the session. It first tries to get the Backend logged in user.
     * <br>4) If no user found, tries to get the Frontend logged in user.
     */
    public User authenticate(HttpServletRequest request) throws SecurityException {
        request = ServletPreconditions.checkSslIsEnabledIfRequired(request);
        boolean forceFrontendAuth = Config.getBooleanProperty("REST_API_FORCE_FRONT_END_SESSION_AUTH", false);
        User user = null;
        Optional<UsernamePassword> userPass = getAuthCredentialsFromHeaderAuth(request);

        if(!userPass.isPresent()) {
            userPass = getAuthCredentialsFromBasicAuth(request);
        }

        if(userPass.isPresent()) {
            user = authenticateUser(userPass.get().username, userPass.get().password, request, userAPI);
        }

        if(user == null && !forceFrontendAuth) {
            user = getBackUserFromRequest(request);
        }

        if(user == null) {
            user = getFrontEndUserFromRequest(request);
        }

        if(user == null) {
            throw new SecurityException("Invalid User", Response.Status.UNAUTHORIZED);
        }
        return user;
    }

    @VisibleForTesting
    Optional<UsernamePassword> getAuthCredentialsFromBasicAuth(HttpServletRequest request) throws SecurityException {

        Optional<UsernamePassword> result = Optional.absent();
        // Extract authentication credentials
        String authentication = request.getHeader(ContainerRequest.AUTHORIZATION);

        if(StringUtils.isNotEmpty(authentication) && authentication.startsWith("Basic ")) {
            authentication = authentication.substring("Basic ".length());
            // @todo ggranum: this should be a split limit 1.
            // "username:SomePass:word".split(":") ==> ["username", "SomePass", "word"]
            // "username:SomePass:word".split(":", 1) ==> ["username", "SomePass:word"]
            String[] values = Base64.base64Decode(authentication).split(":");
            if(values.length < 2) {
                // "Invalid syntax for username and password"
                throw new SecurityException("Invalid syntax for username and password", Response.Status.BAD_REQUEST);
            }
            result = Optional.of(new UsernamePassword(values[0], values[1]));
        }
        return result;
    }

    @VisibleForTesting
    Optional<UsernamePassword> getAuthCredentialsFromHeaderAuth(HttpServletRequest request) throws SecurityException {
        Optional<UsernamePassword> result = Optional.absent();

        String authentication = request.getHeader("DOTAUTH");
        if(StringUtils.isNotEmpty(authentication)) {
            // @todo ggranum: this should be a split limit 1.
            // "username:SomePass:word".split(":") ==> ["username", "SomePass", "word"]
            // "username:SomePass:word".split(":", 1) ==> ["username", "SomePass:word"]
            String[] values = Base64.base64Decode(authentication).split(":");
            if(values.length < 2) {
                throw new SecurityException("Invalid syntax for username and password", Response.Status.BAD_REQUEST);
            }
            result = Optional.of(new UsernamePassword(values[0], values[1]));
        }
        return result;
    }

    /**
     * Authenticates and returns a {@link User} using <code>username</code> and <code>password</code>.
     * If a wrong <code>username</code> or <code>password</code> are provided, a SecurityException is thrown
     */
    @VisibleForTesting
    User authenticateUser(String username, String password, HttpServletRequest req, UserAPI userAPI) throws SecurityException {
        User user = null;
        String ip = req != null ? req.getRemoteAddr() : "";

        if(StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) { // providing login and password so let's try to authenticate

            try {

                if(LoginFactory.doLogin(username, password)) {
                    Company comp = PublicCompanyFactory.getDefaultCompany();

                    if(comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
                        user = userAPI.loadByUserByEmail(username, userAPI.getSystemUser(), false);
                    } else {
                        user = userAPI.loadUserById(username, userAPI.getSystemUser(), false);
                    }
                } else { // doLogin returning false

                    Logger.warn(AuthenticationProvider.class, "Request IP: " + ip + ". Can't authenticate user. Username: " + username);
                    SecurityLogger.logDebug(AuthenticationProvider.class, "Request IP: " + ip + ". Can't authenticate user. Username: " + username);
                    throw new SecurityException("Invalid credentials", Response.Status.UNAUTHORIZED);
                }
            } catch (SecurityException e) {
                throw e;
            } catch (Exception e) {  // doLogin throwing Exception
                Logger.warn(AuthenticationProvider.class, "Request IP: " + ip + ". Can't authenticate user. Username: " + username);
                SecurityLogger.logDebug(AuthenticationProvider.class, "Request IP: " + ip + ". Can't authenticate user. Username: " + username);
                throw new SecurityException("Authentication credentials are required", Response.Status.UNAUTHORIZED);
            }
        } else if(StringUtils.isNotEmpty(username) || StringUtils.isNotEmpty(password)) { // providing login or password
            Logger.warn(AuthenticationProvider.class, "Request IP: " + ip + ". Can't authenticate user.");
            SecurityLogger.logDebug(AuthenticationProvider.class, "Request IP: " + ip + ". Can't authenticate user.");
            throw new SecurityException("Authentication credentials are required", Response.Status.UNAUTHORIZED);
        }

        return user;
    }

    /**
     * This method returns the Backend logged in user from request.
     */

    private User getBackUserFromRequest(HttpServletRequest req) {
        User user = null;

        if(req != null) { // let's check if we have a request and try to get the user logged in from it
            try {
                user = userWebAPI.getLoggedInUser(req);
            } catch (Exception e) {
                Logger.warn(AuthenticationProvider.class, "Can't retrieve Backend User from session");
            }
        }
        return user;
    }

    /**
     * This method returns the Frontend logged in user from request.
     */

    private User getFrontEndUserFromRequest(HttpServletRequest req) {
        User user = null;

        if(req != null) { // let's check if we have a request and try to get the user logged in from it
            try {
                user = userWebAPI.getLoggedInFrontendUser(req);
            } catch (Exception e) {
                Logger.warn(AuthenticationProvider.class, "Can't retrieve user from session");
            }
        }

        return user;
    }

    @VisibleForTesting
    static final class UsernamePassword {

        final String username;
        final String password;

        private UsernamePassword(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}

 
