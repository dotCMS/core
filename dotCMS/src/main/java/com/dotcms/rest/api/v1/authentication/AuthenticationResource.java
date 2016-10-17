package com.dotcms.rest.api.v1.authentication;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.cms.login.LoginService;
import com.dotcms.cms.login.LoginServiceFactory;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.*;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.ejb.UserLocalManagerFactory;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.language.LanguageWrapper;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.LocaleUtil;

/**
 * This resource does the authentication, if the authentication is successfully
 * returns the User Object as a Json, If there is a known error returns 500 and
 * the error messages related. Otherwise returns 500 and the exception as Json.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jul 7, 2016
 */
@SuppressWarnings("serial")
@Path("/v1/authentication")
public class AuthenticationResource implements Serializable {

    static final String USER = "user";
    static final String LOGIN_AS_USER = "loginAsUser";

    private final UserLocalManager userLocalManager;
    private final LoginService loginService;
    private final ResponseUtil responseUtil;

    private final AuthenticationHelper authenticationHelper;

    /**
     * Default constructor.
     */
    public AuthenticationResource() {
        this(LoginServiceFactory.getInstance().getLoginService(),
                UserLocalManagerFactory.getManager(),
                ResponseUtil.INSTANCE,
                AuthenticationHelper.getInstance());
    }

    @VisibleForTesting
    protected AuthenticationResource(final LoginService loginService,
                                     final UserLocalManager userLocalManager,
                                     final ResponseUtil responseUtil,
                                     AuthenticationHelper authenticationHelper) {
        this.loginService = loginService;
        this.userLocalManager = userLocalManager;
        this.responseUtil = responseUtil;
        this.authenticationHelper = authenticationHelper;
    }

    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response authentication(@Context final HttpServletRequest request,
                                   @Context final HttpServletResponse response,
                                   final AuthenticationForm authenticationForm) {

        Response res = null;
        boolean authenticated = false;
        String userId = authenticationForm.getUserId();
        final Locale locale = LocaleUtil.getLocale(request,
                authenticationForm.getCountry(), authenticationForm.getLanguage());

        try {

            authenticated =
                    this.loginService.doActionLogin(userId,
                            authenticationForm.getPassword(),
                            authenticationForm.isRememberMe(), request, response);

            if (authenticated) {

                final HttpSession ses = request.getSession();
                final User user = this.userLocalManager.getUserById((String) ses.getAttribute(WebKeys.USER_ID));
                res = Response.ok(new ResponseEntityView(user.toMap())).build(); // 200
                request.getSession().setAttribute(Globals.LOCALE_KEY, locale);
            } else {

                res = this.responseUtil.getErrorResponse(request, Response.Status.UNAUTHORIZED,
                        locale, userId, "authentication-failed");
            }
        } catch (NoSuchUserException | UserEmailAddressException | UserPasswordException e) {
            res = this.responseUtil.getErrorResponse(request, Response.Status.UNAUTHORIZED, locale, userId, "authentication-failed");
        } catch (AuthException e) {
            res = this.responseUtil.getErrorResponse(request, Response.Status.UNAUTHORIZED, locale, userId, "authentication-failed");
        } catch (RequiredLayoutException e) {
            res = this.responseUtil.getErrorResponse(request, Response.Status.INTERNAL_SERVER_ERROR, locale, userId, "user-without-portlet");
        } catch (UserActiveException e) {

            try {

                res = Response.status(Response.Status.UNAUTHORIZED).entity(new ResponseEntityView
                        (Arrays.asList(new ErrorEntity("your-account-is-not-active",
                                LanguageUtil.format(locale,
                                        "your-account-is-not-active", new LanguageWrapper[] {new LanguageWrapper("<b><i>", userId, "</i></b>")}, false)
                        )))).build();
            } catch (LanguageException e1) {
                // Quiet
            }
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            SecurityLogger.logInfo(this.getClass(),"An invalid attempt to login as " + userId.toLowerCase() + " has been made from IP: " + request.getRemoteAddr());
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return res;
    } // authentication

    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("logInUser")
    public final Response getLoginUser(@Context final HttpServletRequest request){
        Response res = null;

        try {
            Map<String, Map> users = authenticationHelper.getUsers(request);
            res = Response.ok(new ResponseEntityView(users)).build();
        } catch (Exception e) {
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return res;
    }

} // E:O:F:AuthenticationResource.
