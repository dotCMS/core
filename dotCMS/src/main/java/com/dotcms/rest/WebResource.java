package com.dotcms.rest;

import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenAuthCredentialProcessorImpl;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.base.Optional;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.dotcms.repackage.org.glassfish.jersey.internal.util.Base64;
import com.dotcms.repackage.org.glassfish.jersey.server.ContainerRequest;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.rest.validation.ServletPreconditions;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.*;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public  class WebResource {

    public static final String BASIC  = "Basic ";
    public static final String BEARER = "Bearer ";

    private final UserWebAPI        userWebAPI;
    private final UserAPI           userAPI;
    private final LayoutAPI         layoutAPI;
    private final JsonWebTokenAuthCredentialProcessor jsonWebTokenAuthCredentialProcessor;

    public WebResource() {

        this(new ApiProvider());
    }

    public WebResource(final ApiProvider apiProvider) {

        this(apiProvider, JsonWebTokenAuthCredentialProcessorImpl.getInstance());
    }

    public WebResource(final ApiProvider apiProvider,
                       final JsonWebTokenAuthCredentialProcessor jsonWebTokenAuthCredentialProcessor) {

        this.userAPI           = apiProvider.userAPI();
        this.userWebAPI        = apiProvider.userWebAPI();
        this.layoutAPI         = apiProvider.layoutAPI();
        this.jsonWebTokenAuthCredentialProcessor = jsonWebTokenAuthCredentialProcessor;
    }

    /**
     * <p>Checks if SSL is required. If it is required and no secure request is provided, throws a ForbiddenException.
     *
     * @param request
     */

    public void init(HttpServletRequest request) {
        checkForceSSL(request);
    }

    /**
     * <p>1) Checks if SSL is required. If it is required and no secure request is provided, throws a ForbiddenException.
     * <p>2) If 1) does not throw an exception, returns an {@link InitDataObject} with a <code>Map</code> containing
     * the keys and values extracted from <code>params</code>
     *
     *
     * @param params a string containing parameters in the /key/value form
     * @param request
     * @return an initDataObject with the resulting <code>Map</code>
     */

    public InitDataObject init(String params, HttpServletRequest request) {

        checkForceSSL(request);

        InitDataObject initData = new InitDataObject();

        if(!UtilMethods.isSet(params))
            return initData;

        initData.setParamsMap(buildParamsMap(params));
        return initData;
    }

    public InitDataObject init(boolean authenticate, HttpServletRequest request, boolean rejectWhenNoUser) throws SecurityException {
        return init(null, authenticate, request, rejectWhenNoUser, null);
    }


    /**
     *
     * <p>1) Checks if SSL is required. If it is required and no secure request is provided, throws a ForbiddenException.
     * <p>2) If 1) does not throw an exception, returns an {@link InitDataObject} with:
     *
     * <br>a) a <code>Map</code> with the keys and values extracted from <code>params</code>.
     *
     *<br><br>if <code>authenticate</code> is set to <code>true</code>:
     * <br>b) , an authenticated {@link User}, if found.
     * If no User can be retrieved, and <code>rejectWhenNoUser</code> is <code>true</code>, it will throw an exception,
     * otherwise returns <code>null</code>.
     *
     * <br><br>There are five ways to get the User. They are executed in the specified order. When found, the remaining ways won't be executed.
     * <br>1) Using username and password contained in <code>params</code>.
     * <br>2) Using username and password in Base64 contained in the <code>request</code> HEADER parameter DOTAUTH.
     * <br>3) Using username and password in Base64 contained in the <code>request</code> HEADER parameter AUTHORIZATION (BASIC Auth).
     * <br>4) From the session. It first tries to get the Backend logged in user. If no user found, tries to get the Frontend logged in user.
     *
     *
     * @param params a string containing the URL parameters in the /key/value form
     * @param authenticate
     * @param request
     * @param rejectWhenNoUser determines whether a SecurityException is thrown or not when authentication fails.
     * @param requiredPortlet portlet name which the user needs to have access to
     * @return an initDataObject with the resulting <code>Map</code>
     */

    public InitDataObject init(String params, boolean authenticate, HttpServletRequest request, boolean rejectWhenNoUser, String requiredPortlet) throws SecurityException {

        checkForceSSL(request);

        InitDataObject initData = new InitDataObject();

        if(!UtilMethods.isSet(params))
            params = "";

        Map<String, String> paramsMap = buildParamsMap(params);
        User user = authenticate(request, paramsMap, rejectWhenNoUser);

        if(UtilMethods.isSet(requiredPortlet)) {

            try {
                if(!layoutAPI.doesUserHaveAccessToPortlet(requiredPortlet, user)){
                    throw new SecurityException("User does not have access to required Portlet", Response.Status.UNAUTHORIZED);
                }
            } catch (DotDataException e) {
                throw new SecurityException("User does not have access to required Portlet", Response.Status.UNAUTHORIZED);
            }

        }

        initData.setParamsMap(paramsMap);
        initData.setUser(user);

        return initData;
    }


    /**
     * Returns an authenticated {@link User}. There are five ways to get the User's credentials.
     * They are executed in the specified order. When found, the remaining ways won't be executed.
     * <br>1) Using username and password contained in <code>params</code>.
     * <br>2) Using username and password in Base64 contained in the <code>request</code> HEADER parameter DOTAUTH.
     * <br>3) Using username and password in Base64 contained in the <code>request</code> HEADER parameter AUTHORIZATION (BASIC Auth).
     * <br>4) From the session. It first tries to get the Backend logged in user.
     * <br>5) If no user found, tries to get the Frontend logged in user.
     */
    public User authenticate(HttpServletRequest request, Map<String, String> params, boolean rejectWhenNoUser) throws SecurityException {
        request = ServletPreconditions.checkSslIsEnabledIfRequired(request);
        boolean forceFrontendAuth = Config.getBooleanProperty("REST_API_FORCE_FRONT_END_SESSION_AUTH", false);
        User user = null;

        Optional<UsernamePassword> userPass = getAuthCredentialsFromMap(params);

        if(!userPass.isPresent()) {
            userPass = getAuthCredentialsFromHeaderAuth(request);
        }

        if(!userPass.isPresent()) {
            userPass = getAuthCredentialsFromBasicAuth(request);
        }

        if(userPass.isPresent()) {
            user = authenticateUser(userPass.get().username, userPass.get().password, request, userAPI);
        }

        if(null == user) {
            user = processAuthCredentialsFromJWT(request, this.jsonWebTokenAuthCredentialProcessor);
        }

        if(user == null && !forceFrontendAuth) {
            user = getBackUserFromRequest(request, userWebAPI);
        }

        if(user == null) {
            user = getFrontEndUserFromRequest(request, userWebAPI);
        }

        if(user == null && (Config.getBooleanProperty("REST_API_REJECT_WITH_NO_USER", false) || rejectWhenNoUser) ) {

            throw new SecurityException("Invalid User", Response.Status.UNAUTHORIZED);
        } else if(user == null) {

            user = this.getAnonymousUser();
        }

        return user;
    }

    /**
     * Get the anonymous user if it is possible, otherwise will return null.
     * @return User
     */
    public User getAnonymousUser() {

        User user = null;

        try {

            user = APILocator.getUserAPI().getAnonymousUser();
        } catch (DotDataException e) {
            user = null;
            Logger.debug(getClass(), "Could not get Anonymous User. ");
        }
        return user;
    } // getAnonymousUser.

    private static User processAuthCredentialsFromJWT(final HttpServletRequest request, final JsonWebTokenAuthCredentialProcessor authCredentialProcessor) {

        return authCredentialProcessor.processAuthCredentialsFromJWT(request);
    } // getAuthCredentialsFromJWT.


    private static Optional<UsernamePassword> getAuthCredentialsFromMap(Map<String, String> map) {

        Optional<UsernamePassword> result = Optional.absent();

        String username = map.get(RESTParams.USER.getValue());
        String password = map.get(RESTParams.PASSWORD.getValue());

        if(StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
            result = Optional.of(new UsernamePassword(username, password));
        }

        return result;
    }

    @VisibleForTesting
    static Optional<UsernamePassword> getAuthCredentialsFromBasicAuth(HttpServletRequest request) throws SecurityException {

        Optional<UsernamePassword> result = Optional.absent();
        // Extract authentication credentials
        String authentication = request.getHeader(ContainerRequest.AUTHORIZATION);

        if(StringUtils.isNotEmpty(authentication) && authentication.startsWith(BASIC)) {
            authentication = authentication.substring(BASIC.length());
            // @todo ggranum: this should be a split limit 1.
            // "username:SomePass:word".split(":") ==> ["username", "SomePass", "word"]
            // "username:SomePass:word".split(":", 1) ==> ["username", "SomePass:word"]
            String[] values = Base64.decodeAsString(authentication).split(":");
            if(values.length < 2) {
                // "Invalid syntax for username and password"
                throw new SecurityException("Invalid syntax for username and password", Response.Status.BAD_REQUEST);
            }
            result = Optional.of(new UsernamePassword(values[0], values[1]));
        }
        return result;
    }

    @VisibleForTesting
    static Optional<UsernamePassword> getAuthCredentialsFromHeaderAuth(HttpServletRequest request) throws SecurityException {
        Optional<UsernamePassword> result = Optional.absent();

        String authentication = request.getHeader("DOTAUTH");
        if(StringUtils.isNotEmpty(authentication)) {
            // @todo ggranum: this should be a split limit 1.
            // "username:SomePass:word".split(":") ==> ["username", "SomePass", "word"]
            // "username:SomePass:word".split(":", 1) ==> ["username", "SomePass:word"]
            String[] values = Base64.decodeAsString(authentication).split(":");
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
    static User authenticateUser(String username, String password, HttpServletRequest req, UserAPI userAPI) throws SecurityException {
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

                    Logger.warn(WebResource.class, "Request IP: " + ip + ". Can't authenticate user. Username: " + username);
                    SecurityLogger.logDebug(WebResource.class, "Request IP: " + ip + ". Can't authenticate user. Username: " + username);
                    throw new SecurityException("Invalid credentials", Response.Status.UNAUTHORIZED);
                }
            } catch (SecurityException e) {
                throw e;
            } catch (Exception e) {  // doLogin throwing Exception
                Logger.warn(WebResource.class, "Request IP: " + ip + ". Can't authenticate user. Username: " + username);
                SecurityLogger.logDebug(WebResource.class, "Request IP: " + ip + ". Can't authenticate user. Username: " + username);
                throw new SecurityException("Authentication credentials are required", Response.Status.UNAUTHORIZED);
            }
        } else if(StringUtils.isNotEmpty(username) || StringUtils.isNotEmpty(password)) { // providing login or password
            Logger.warn(WebResource.class, "Request IP: " + ip + ". Can't authenticate user.");
            SecurityLogger.logDebug(WebResource.class, "Request IP: " + ip + ". Can't authenticate user.");
            throw new SecurityException("Authentication credentials are required", Response.Status.UNAUTHORIZED);
        }

        return user;
    }

    /**
     * This method returns the Backend logged in user from request.
     */

    private static User getBackUserFromRequest(HttpServletRequest req, UserWebAPI userWebAPI) {
        User user = null;

        if(req != null) { // let's check if we have a request and try to get the user logged in from it
            try {
                user = userWebAPI.getLoggedInUser(req);
            } catch (Exception e) {
                Logger.warn(WebResource.class, "Can't retrieve Backend User from session");
            }
        }
        return user;
    }

    /**
     * This method returns the Frontend logged in user from request.
     */

    private static User getFrontEndUserFromRequest(HttpServletRequest req, UserWebAPI userWebAPI) {
        User user = null;

        if(req != null) { // let's check if we have a request and try to get the user logged in from it
            try {
                user = userWebAPI.getLoggedInFrontendUser(req);
            } catch (Exception e) {
                Logger.warn(WebResource.class, "Can't retrieve user from session");
            }
        }

        return user;
    }

    /**
     * This method returns a <code>Map</code> with the keys and values extracted from <code>params</code>
     *
     *
     * @param params a string in the form of "/key/value/.../key/value"
     * @return a <code>Map</code> with the keys and values extracted from <code>params</code>
     */

    private static Map<String, String> buildParamsMap(String params) {

        if (params.startsWith("/")) {
            params = params.substring(1);
        }
        String[] pathParts = params.split("/");
        Map<String, String> pathMap = new HashMap<String, String>();
        for (int i=0; i < pathParts.length/2; i++) {
            String key = pathParts[2*i].toLowerCase();
            String value = pathParts[2*i+1];

            if (UtilMethods.isSet(value)) {
                pathMap.put(key, value);
            }
        }
        return pathMap;
    }


    private static void checkForceSSL(HttpServletRequest request) {
        if(Config.getBooleanProperty("FORCE_SSL_ON_RESP_API", false) && UtilMethods.isSet(request) && !request.isSecure())
            throw new SecurityException("SSL Required.", Response.Status.FORBIDDEN);

    }

    protected static Map processJSON(InputStream input) throws JSONException, IOException {
        HashMap<String,Object> map=new HashMap<String,Object>();
        JSONObject obj=new JSONObject(IOUtils.toString(input));
        Iterator<String> keys = obj.keys();
        while(keys.hasNext()) {
            String key=keys.next();
            Object value=obj.get(key);
            map.put(key, value.toString());
        }

        return map;
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
