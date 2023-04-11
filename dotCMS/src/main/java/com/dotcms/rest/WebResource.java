package com.dotcms.rest;

import static com.liferay.util.StringPool.BLANK;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.exception.InvalidLicenseException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.internal.util.Base64;
import org.glassfish.jersey.server.ContainerRequest;

import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenAuthCredentialProcessorImpl;
import org.apache.commons.io.IOUtils;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.rest.validation.ServletPreconditions;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.CookieUtil;
import com.liferay.util.StringPool;

import io.vavr.control.Try;

/**
 * The Web Resource is a helper for all authentication and get the current user logged in
 * It supports several authentication method such as BASIC, Bearer (JWT), etc.
 */
public  class WebResource {

    public static final String BASIC  = "Basic ";

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
     * @deprecated
     * @see {@link InitBuilder}
     * @param request  {@link HttpServletRequest}
     */
    @Deprecated
    public void init(final HttpServletRequest request) {
        checkForceSSL(request);
    }

    /**
     * <p>1) Checks if SSL is required. If it is required and no secure request is provided, throws a ForbiddenException.
     * <p>2) If 1) does not throw an exception, returns an {@link InitDataObject} with a <code>Map</code> containing
     * the keys and values extracted from <code>params</code>
     *
     * @deprecated
     * @see {@link InitBuilder}
     * @param params   {@link String} a string containing parameters in the /key/value form
     * @param request  {@link HttpServletRequest}
     * @return an initDataObject with the resulting <code>Map</code>
     */
    @Deprecated
    public InitDataObject init(final String params, final HttpServletRequest request) {

        checkForceSSL(request);

        final InitDataObject initData = new InitDataObject();

        if(!UtilMethods.isSet(params)) {
            return initData;
        }

        initData.setParamsMap(buildParamsMap(params));
        return initData;
    }

    /**
     *
     * <p>
     *     1) Checks if SSL is required. If it is required and no secure request is provided, throws a ForbiddenException.
     *
     *      If no User can be retrieved, and <code>rejectWhenNoUser</code> is <code>true</code>, it will throw an exception,
     *      otherwise returns <code>null</code>.
     * </p>
     * <br>
     * <br>There are five ways to get the User. They are executed in the specified order. When found, the remaining ways won't be executed.
     * <br>1) Using username and password contained in <code>params</code>.
     * <br>2) Using username and password in Base64 contained in the <code>request</code> HEADER parameter DOTAUTH.
     * <br>3) Using username and password in Base64 contained in the <code>request</code> HEADER parameter AUTHORIZATION (BASIC Auth).
     * <br>4) From the session. It first tries to get the Backend logged in user. If no user found, tries to get the Frontend logged in user.
     *
     * @deprecated
     * @see {@link InitBuilder}
     * @param request  {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @param rejectWhenNoUser determines whether a SecurityException is thrown or not when authentication fails.
     * @return an initDataObject with the resulting <code>Map</code>
     * @throws SecurityException
     */
    @Deprecated
    public InitDataObject init(final HttpServletRequest request, final HttpServletResponse response,
                               final boolean rejectWhenNoUser) throws SecurityException {

        return init(null, request, response, rejectWhenNoUser, null);
    }

    /**
     * @deprecated
     * @see #init(HttpServletRequest, HttpServletResponse, boolean)
     * @param authenticate
     * @param request
     * @param rejectWhenNoUser
     * @return InitDataObject
     * @throws SecurityException
     */
    @Deprecated
    public InitDataObject init(final boolean authenticate, final HttpServletRequest request,
                               final boolean rejectWhenNoUser) throws SecurityException {

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
     * @param params   {@link String} a string containing the URL parameters in the /key/value form
     * @param request  {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @param rejectWhenNoUser determines whether a SecurityException is thrown or not when authentication fails.
     * @param requiredPortlet portlet name which the user needs to have access to
     * @return an initDataObject with the resulting <code>Map</code>
     */
    public InitDataObject init(final String params, final HttpServletRequest request, final HttpServletResponse response,
                               final boolean rejectWhenNoUser, final String requiredPortlet) throws SecurityException {

        checkForceSSL(request);
        AnonymousAccess access = (rejectWhenNoUser) ? AnonymousAccess.NONE : AnonymousAccess.systemSetting();
        final Map<String, String> paramsMap = buildParamsMap(!UtilMethods.isSet(params)?BLANK:params);
        return initWithMap(paramsMap, request, response, access, requiredPortlet);
    }

    /**
     * @deprecated
     * @see #init(String, HttpServletRequest, HttpServletResponse, boolean, String)
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
    @Deprecated
    public InitDataObject init(String params, final boolean authenticate, final HttpServletRequest request, final boolean rejectWhenNoUser, final String requiredPortlet) throws SecurityException {

        checkForceSSL(request);

        if(!UtilMethods.isSet(params)) {
            params = BLANK;
        }
        AnonymousAccess access = (rejectWhenNoUser) ? AnonymousAccess.NONE : AnonymousAccess.systemSetting();
        final Map<String, String> paramsMap = buildParamsMap(params);
        return initWithMap(paramsMap, request, new EmptyHttpResponse(), access, requiredPortlet);
    }

    /**
     * @deprecated
     * @see #init(String, String, HttpServletRequest, HttpServletResponse, boolean, String)
     * @param userId
     * @param password
     * @param authenticate
     * @param request
     * @param rejectWhenNoUser
     * @param requiredPortlet
     * @return
     * @throws SecurityException
     */
    @Deprecated
    public InitDataObject init(String userId, String password, boolean authenticate, HttpServletRequest request, boolean rejectWhenNoUser, String requiredPortlet) throws SecurityException {
        AnonymousAccess access = (rejectWhenNoUser) ? AnonymousAccess.NONE : AnonymousAccess.systemSetting();
        return initWithMap(CollectionsUtils.map("userid", userId, "pwd", password), request, new EmptyHttpResponse(), access, requiredPortlet);
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
     * @param userId   {@link String} a string with the userId/email
     * @param password {@link String} a string with password.
     * @param request  {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @param rejectWhenNoUser determines whether a SecurityException is thrown or not when authentication fails.
     * @param requiredPortlet portlet name which the user needs to have access to
     * @return an initDataObject with the resulting <code>Map</code>
     */
    public InitDataObject init(final String userId, final String password,
                               final HttpServletRequest request, final HttpServletResponse response,
                               final boolean rejectWhenNoUser, final String requiredPortlet) throws SecurityException {
      
      
        AnonymousAccess access = (rejectWhenNoUser) ? AnonymousAccess.NONE : AnonymousAccess.systemSetting();
      
        return initWithMap(CollectionsUtils.map("userid", userId, "pwd", password), request, response, access, requiredPortlet);
    }

    private InitDataObject initWithMap(final Map<String, String> paramsMap,
                                       final HttpServletRequest request,
                                       final HttpServletResponse response,
                                       final AnonymousAccess access,
                                       final String requiredPortlet) throws SecurityException {

        final InitDataObject initData = new InitDataObject();
        final User user = getCurrentUser(request, response, paramsMap, access);

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
     *
     * @param builder
     * @return
     * @throws SecurityException
     */
    @VisibleForTesting
    public InitDataObject init(final InitBuilder builder) throws SecurityException {

        checkForceSSL(builder.request);

        final InitDataObject initData = new InitDataObject();
        final Map<String, String> paramsMap = new HashMap<>(
                buildParamsMap(
                        UtilMethods.isSet(builder.params) ? builder.params : BLANK
                )
        );

        if (UtilMethods.isSet(builder.userId) && UtilMethods.isSet(builder.password)) {
            paramsMap.putAll(CollectionsUtils
                    .map("userid", builder.userId, "pwd", builder.password));
        }
        
        final User user = getCurrentUser(builder.request, builder.response, paramsMap,
                builder.anonAccess);


        // make sure we have the right anon permissions
        if(user.equals(userAPI.getAnonymousUserNoThrow())){
          if(builder.anonAccess.ordinal()>AnonymousAccess.systemSetting().ordinal()) {
            throw new SecurityException(
                String.format(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS +  " permission exceeded - system set to %s but %s was required", AnonymousAccess.systemSetting().name(), builder.anonAccess.name()),
                Response.Status.UNAUTHORIZED);
          }
        } 
        
        if (!builder.requiredRolesSet.isEmpty()) {
            final RoleAPI roleAPI = APILocator.getRoleAPI();
            
            boolean hasARequiredRole=builder.requiredRolesSet.stream()
                .anyMatch(roleKey -> Try.of(()->roleAPI
                    .doesUserHaveRole(user, roleAPI.loadRoleByKey(roleKey)))
                    .getOrElse(false));

            if(!hasARequiredRole) {
              throw new SecurityException(
                  String.format("User " + (user!=null ? user.getFullName() + ":" + user.getEmailAddress() : user) +" lacks one of the required role %s", builder.requiredRolesSet.toString()),
                  Response.Status.UNAUTHORIZED);
            }
            
            
            if (builder.requiredRolesSet.contains(Role.DOTCMS_BACK_END_USER) &&
                            builder.request != null &&
                            builder.request.getRequestURL() != null) {
                String host = builder.request.getHeader("host");
                String portalUrl = APILocator.getCompanyAPI().getDefaultCompany().getPortalURL();
                String scheme =  builder.request.getScheme();
                
                if(!StringUtils.containsIgnoreCase(scheme + "://" + host, portalUrl)) {
                    SecurityLogger.logInfo(getClass(),builder.request.getRequestURL().toString() + " must use the portalUrl " + portalUrl.toLowerCase());          
                    throw new SecurityException("Not Found",  Response.Status.NOT_FOUND);
                }

            }
            
        }
        

        if (UtilMethods.isSet(builder.requiredPortlet)) {
            for (final String requiredPortlet : builder.requiredPortlet) {
                try {
                    if (!layoutAPI.doesUserHaveAccessToPortlet(requiredPortlet, user)) {
                        throw new SecurityException(
                                String.format(
                                        "User " + (user!=null ? user.getFullName() + ":" + user.getEmailAddress() : user) +" does not have access to required Portlet %s",
                                        requiredPortlet),
                                Response.Status.UNAUTHORIZED);
                    }
                } catch (DotDataException e) {
                    throw new SecurityException("User " + (user!=null ? user.getFullName() + ":" + user.getEmailAddress() : user) +" does not have access to required Portlet",
                            Response.Status.UNAUTHORIZED);
                }
            }
        }
        

        
        
        


        initData.setUser(user);
        initData.setParamsMap(paramsMap);

        return initData;
    }

    /**
     * Return the current login user.<br>
     * if exist a user login by login as then return this user not the principal user
     *
     * @param request  {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @param paramsMap {@link Map}
     * @param rejectWhenNoUser {@link Boolean}
     *
     * @return the login user or the login as user if exist any
     */
    public User getCurrentUser(final HttpServletRequest  request,
                               final HttpServletResponse response,
                               final Map<String, String> paramsMap, final AnonymousAccess access) {

        User user = PortalUtil.getUser(request);

        if(user==null) {
            user = authenticate(request, response, paramsMap, access);
        }
        return user;
    }

    /**
     * @deprecated
     * @see #getCurrentUser(HttpServletRequest, HttpServletResponse, Map, boolean)
     *
     * Return the current login user.<br>
     * if exist a user login by login as then return this user not the principal user
     *
     * @param request
     * @param paramsMap
     * @param rejectWhenNoUser
     *
     * @return the login user or the login as user if exist any
     */
    @Deprecated
    public User getCurrentUser(final HttpServletRequest request, final Map<String, String> paramsMap, final boolean rejectWhenNoUser) {
        AnonymousAccess access = (rejectWhenNoUser) ? AnonymousAccess.NONE : AnonymousAccess.systemSetting();
        return this.getCurrentUser(request, new EmptyHttpResponse(), paramsMap, access);
    }



    /**
     * @deprecated
     * @see #authenticate(HttpServletRequest, HttpServletResponse, Map, boolean)
     * Returns an authenticated {@link User}. There are five ways to get the User's credentials.
     * They are executed in the specified order. When found, the remaining ways won't be executed.
     * <br>1) Using username and password contained in <code>params</code>.
     * <br>2) Using username and password in Base64 contained in the <code>request</code> HEADER parameter DOTAUTH.
     * <br>3) Using username and password in Base64 contained in the <code>request</code> HEADER parameter AUTHORIZATION (BASIC Auth).
     * <br>4) From the session. It first tries to get the Backend logged in user.
     * <br>5) If no user found, tries to get the Frontend logged in user.
     */
    @Deprecated
    public User authenticate(final HttpServletRequest request,
                             final Map<String, String> params, final boolean rejectWhenNoUser) throws SecurityException {
        AnonymousAccess access = (rejectWhenNoUser) ? AnonymousAccess.NONE : AnonymousAccess.systemSetting();
        return this.authenticate(request, new EmptyHttpResponse(), params, access);
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
    public User authenticate(HttpServletRequest request, final HttpServletResponse response,
                             final Map<String, String> params, final AnonymousAccess access) throws SecurityException {

        request = ServletPreconditions.checkSslIsEnabledIfRequired(request);

        User user = null;

        Optional<UsernamePassword> userPass = getAuthCredentialsFromMap(params);

        if(!userPass.isPresent()) {
            userPass = getAuthCredentialsFromHeaderAuth(request);
        }

        if(!userPass.isPresent()) {
            userPass = getAuthCredentialsFromBasicAuth(request);
        }

        if(userPass.isPresent()) {
            user = authenticateUser(userPass.get().username, userPass.get().password, request, response, userAPI);
        }

        if(null == user) {
            user = this.jsonWebTokenAuthCredentialProcessor.processAuthHeaderFromJWT(request);
        }

        if(null == user) {
           // user = this.processCookieJWT(request);
        }


        if(user == null ) {
            user = getBackUserFromRequest(request, userWebAPI);
        }

        if(user == null) {
            user = getFrontEndUserFromRequest(request, userWebAPI);
        }
        
        if(user==null) {
          user = this.getAnonymousUser();
        }


        if( UserAPI.CMS_ANON_USER_ID.equals(user.getUserId()) && access == AnonymousAccess.NONE) {

            throw new SecurityException("Invalid User", Response.Status.UNAUTHORIZED);
        } 

        request.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
        request.setAttribute(com.liferay.portal.util.WebKeys.USER, user);
        PrincipalThreadLocal.setName(user.getUserId());

        return user;
    }

    /**
     * Get the anonymous user if it is possible, otherwise will return null.
     * @return User
     */
    public User getAnonymousUser() {

        return APILocator.getUserAPI().getAnonymousUserNoThrow();

    } // getAnonymousUser.


    private static Optional<UsernamePassword> getAuthCredentialsFromMap(final Map<String, String> map) {

        Optional<UsernamePassword> result = Optional.empty();

        String username = map.get(RESTParams.USER.getValue());
        String password = map.get(RESTParams.PASSWORD.getValue());

        if(StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
            result = Optional.of(new UsernamePassword(username, password));
        }

        return result;
    }

    @VisibleForTesting
    static Optional<UsernamePassword> getAuthCredentialsFromBasicAuth(final HttpServletRequest request) throws SecurityException {

        Optional<UsernamePassword> result =  Optional.empty();
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
        Optional<UsernamePassword> result =  Optional.empty();

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
    static User authenticateUser(final String username, final String password,
                                 final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 final UserAPI userAPI) throws SecurityException {
        User user       = null;
        final String ip = request != null ? request.getRemoteAddr() : BLANK;

        if(StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) { // providing login and password so let's try to authenticate

            try {

                if(APILocator.getLoginServiceAPI().doActionLogin(username, password, false, request, response)) {

                    final Company company = CompanyUtils.getDefaultCompany();

                    user = company.getAuthType().equals(Company.AUTH_TYPE_EA)?
                        userAPI.loadByUserByEmail(username, userAPI.getSystemUser(), false):
                        userAPI.loadUserById(username, userAPI.getSystemUser(), false);
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
                throw new SecurityException("Authentication credentials are required", e, Response.Status.UNAUTHORIZED);
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
    private static User getBackUserFromRequest(final HttpServletRequest req, final UserWebAPI userWebAPI) {
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

    private User processCookieJWT(final HttpServletRequest request) {
        User user = null;

        if(request != null) {
            final String jwt=Try.of(()->CookieUtil.get(request.getCookies(), CookieKeys.JWT_ACCESS_TOKEN)).getOrNull();
            user = APILocator.getApiTokenAPI().userFromJwt(jwt, request.getRemoteAddr()).orElse(null);
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

    public static Map<String, String> buildParamsMap(String params) {

        if (params.startsWith(StringPool.FORWARD_SLASH)) {
            params = params.substring(1);
        }

        final String[] pathParts = params.split(StringPool.FORWARD_SLASH);
        final Map<String, String> pathMap = new HashMap<>();
        for (int i=0; i < pathParts.length/2; i++) {

            final String key = pathParts[2*i].toLowerCase();
            final String value = pathParts[2*i+1];

            if (UtilMethods.isSet(value)) {
                pathMap.put(key, value);
            }
        }

        return pathMap;
    }


    private static void checkForceSSL(final HttpServletRequest request) {

        if(Config.getBooleanProperty("FORCE_SSL_ON_RESP_API", false)
                && UtilMethods.isSet(request) && !request.isSecure()) {
            throw new SecurityException("SSL Required.", Response.Status.FORBIDDEN);
        }
    }

    public static Map processJSON(final InputStream input) throws JSONException, IOException {

        final HashMap<String,Object> map = new HashMap<>();
        final JSONObject obj        = new JSONObject(IOUtils.toString(input));
        final Iterator<String> keys = obj.keys();
        while(keys.hasNext()) {

            final String key   = keys.next();
            final Object value = obj.get(key);
            map.put(key, value);
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

   public static class InitBuilder {

        private WebResource webResource;

        private String userId = null;
        private String password = null;
        private String params = BLANK;
        private HttpServletRequest request = null;
        private HttpServletResponse response = null;
        private String[] requiredPortlet = null;
        private final Set<String> requiredRolesSet = new HashSet<>();
        private AnonymousAccess anonAccess=AnonymousAccess.NONE;
        private boolean requireLicense = false;
        
        public InitBuilder() {
          this(new WebResource());

        }

        public InitBuilder(HttpServletRequest request, HttpServletResponse response) {
            this();
            this.request=request;
            this.response=response;
        }

        public InitBuilder(final WebResource webResource) {
            this.webResource = webResource;
        }

        public InitBuilder credentials(final String userId, final String password) {
            this.userId = userId;
            this.password = password;
            return this;
        }

        public InitBuilder params(final String params) {
            this.params = params;
            return this;
        }

        public InitBuilder requestAndResponse(final HttpServletRequest request,
                final HttpServletResponse response) {
            this.request = request;
            this.response = response;
            return this;
        }

        public InitBuilder requiredBackendUser(final boolean requiredBackendUser) {
            if (requiredBackendUser) {
                requiredRolesSet.add(Role.DOTCMS_BACK_END_USER);
            } else {
                requiredRolesSet.remove(Role.DOTCMS_BACK_END_USER);
            }
            return this;
        }

        public InitBuilder requiredFrontendUser(final boolean requiredFrontendUser) {
            if (requiredFrontendUser) {
                requiredRolesSet.add(Role.DOTCMS_FRONT_END_USER);
            } else {
                requiredRolesSet.remove(Role.DOTCMS_FRONT_END_USER);
            }
            return this;
        }

        public InitBuilder requiredRoles(final String... requiredRoles) {
            requiredRolesSet.addAll(Arrays.asList(requiredRoles));
            return this;
        }

        public InitBuilder requiredAnonAccess(final AnonymousAccess access) {
          this.anonAccess = access;
          return this;
        }


        public InitBuilder rejectWhenNoUser(final boolean rejectWhenNoUser) {
            this.anonAccess = (rejectWhenNoUser)  ? AnonymousAccess.NONE : this.anonAccess;
            return this;
        }

        public InitBuilder requiredPortlet(final String... requiredPortlet) {
            this.requiredPortlet = requiredPortlet;
            return this;
        }

        public InitBuilder requireLicense(final boolean requireLicense){
            this.requireLicense = requireLicense;
            return this;
        }

        public InitDataObject init() {

            response = (response == null ? new EmptyHttpResponse() : response);

            if (UtilMethods.isSet(userId) || UtilMethods.isSet(params)) {
                if (!UtilMethods.isSet(userId) && !UtilMethods.isSet(params)) {
                    throw new IllegalArgumentException(
                            "Missing required value building credentials you need to specify both `user` and `password` or none of them.");
                }
            }

            if (!UtilMethods.isSet(request)) {
                throw new IllegalArgumentException(
                        "A request is always required and it hasn't been set.");
            }


            if (anonAccess != AnonymousAccess.NONE) {

                if(UtilMethods.isSet(requiredPortlet) || UtilMethods.isSet(requiredRolesSet)){
                    Logger.debug(InitBuilder.class,
                            "Setting AnonymousAccess  to `READ` or `WRITE` implies that neither a portlet is required nor a set of roles.");
                }

                Logger.debug(InitBuilder.class,
                        () -> request.getRequestURI()+" calling webResource.init(..) with AnonymousAccess set to `" + anonAccess + "`");
            }

            if(requireLicense){
                if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
                    throw new InvalidLicenseException("Need an enterprise license to run this");
                }
            }

            try {
                return webResource.init(this);
            } catch (final Exception e) {
                 Logger.warnAndDebug(WebResource.class,"InitDataObject Error: uri:" + request.getRequestURI() + " err:" + e.getMessage() ,e);
                 throw e;
            }
        }

    }

}
