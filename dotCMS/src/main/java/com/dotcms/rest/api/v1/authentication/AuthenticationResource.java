package com.dotcms.rest.api.v1.authentication;

import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.RequiredLayoutException;
import com.liferay.portal.UserActiveException;
import com.liferay.portal.UserEmailAddressException;
import com.liferay.portal.UserPasswordException;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.ejb.UserLocalManagerFactory;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.language.LanguageWrapper;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.LocaleUtil;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;

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
    private final LoginServiceAPI loginService;
    private final ResponseUtil responseUtil;

    private final AuthenticationHelper authenticationHelper;

    /**
     * Default constructor.
     */
    public AuthenticationResource() {
        this(APILocator.getLoginServiceAPI(),
                UserLocalManagerFactory.getManager(),
                ResponseUtil.INSTANCE,
                AuthenticationHelper.getInstance());
    }

    @VisibleForTesting
    protected AuthenticationResource(final LoginServiceAPI loginService,
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

            authenticated = authenticationForm.isBackEndLogin() 
                ? this.loginService.doBackEndLogin(userId,
                    authenticationForm.getPassword(),
                    authenticationForm.isRememberMe(), request, response)
                : this.loginService.doActionLogin(userId,
                            authenticationForm.getPassword(),
                            authenticationForm.isRememberMe(), request, response);

            if (authenticated) {

                final HttpSession ses = request.getSession();
                final User user = this.userLocalManager.getUserById((String) ses.getAttribute(WebKeys.USER_ID));
                final Map<String, Object> userMap = user.toMap();

                userMap.put("loggedInDate", new Date());

                String pageUrl = (String) request.getSession().getAttribute(WebKeys.LOGIN_TO_EDIT_MODE);

                if (pageUrl != null) {
                    userMap.put("editModeUrl", pageUrl);
                    request.getSession().removeAttribute(WebKeys.LOGIN_TO_EDIT_MODE);
                }

                res = Response.ok(new ResponseEntityView(userMap)).build(); // 200
                request.getSession().setAttribute(Globals.LOCALE_KEY, locale);
            } else {

                res = this.responseUtil.getErrorResponse(request, Response.Status.UNAUTHORIZED,
                        locale, userId, "authentication-failed");
            }
        } catch (NoSuchUserException | UserEmailAddressException | UserPasswordException | AuthException e) {
            res = this.responseUtil.getErrorResponse(request, Response.Status.UNAUTHORIZED, locale, userId, "authentication-failed");
        }  catch (RequiredLayoutException e) {
            res = this.responseUtil.getErrorResponse(request, Response.Status.INTERNAL_SERVER_ERROR, locale, userId, "user-without-portlet");
        } catch (UserActiveException e) {

            try {

                res = Response.status(Response.Status.UNAUTHORIZED).entity(new ResponseEntityView
                        (Collections.singletonList(new ErrorEntity("your-account-is-not-active",
                                LanguageUtil.format(locale,
                                        "your-account-is-not-active", new LanguageWrapper[]{
                                                new LanguageWrapper("<b><i>", userId, "</i></b>")},
                                        false)
                        )))).build();
            } catch (LanguageException e1) {
                // Quiet
            }
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
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
            // todo: add here the loggedInDate???
            res = Response.ok(new ResponseEntityView(users)).build();
        } catch (Exception e) {
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return res;
    }

} // E:O:F:AuthenticationResource.
