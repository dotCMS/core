package com.dotcms.rest.api.v1.authentication;

import com.dotcms.cms.login.LoginService;
import com.dotcms.cms.login.LoginServiceFactory;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.*;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.ejb.UserLocalManagerFactory;
import com.liferay.portal.ejb.UserLocalManagerUtil;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.language.LanguageWrapper;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.LocaleUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;

/**
 * This resource does the authentication, if the authentication is successfully returns the User Object as a Json,
 * If there is a known error returns 500 and the error messages related.
 * Otherwise returns 500 and the exception as Json.
 * @author jsanca
 */
@Path("/v1/authentication")
public class AuthenticationResource implements Serializable {

    private final UserLocalManager userLocalManager;
    private final LoginService loginService;
    private final WebResource webResource;
    private final AuthenticationHelper  authenticationHelper;

    @SuppressWarnings("unused")
    public AuthenticationResource() {
        this(LoginServiceFactory.getInstance().getLoginService(),
                UserLocalManagerFactory.getManager(),
                AuthenticationHelper.INSTANCE,
                new WebResource(new ApiProvider()));
    }

    @VisibleForTesting
    protected AuthenticationResource(final LoginService loginService,
                                     final UserLocalManager userLocalManager,
                                     final AuthenticationHelper  authenticationHelper,
                                     final WebResource webResource) {
        this.loginService = loginService;
        this.userLocalManager = userLocalManager;
        this.authenticationHelper = authenticationHelper;
        this.webResource = webResource;
    }

    // todo: add the https annotation
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response authentication(@Context final HttpServletRequest request,
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
                SecurityLogger.logInfo(this.getClass(), "An invalid attempt to login as " + userId.toLowerCase() + " has been made from IP: " + request.getRemoteAddr());
            }
        } catch (NoSuchUserException | UserEmailAddressException e) {

            res = this.authenticationHelper.getErrorResponse(request, Response.Status.UNAUTHORIZED, locale, userId, "please-enter-a-valid-login");
        } catch (AuthException e) {

            res = this.authenticationHelper.getErrorResponse(request, Response.Status.UNAUTHORIZED, locale, userId, "authentication-failed");
        }  catch (UserPasswordException e) {

            res = this.authenticationHelper.getErrorResponse(request, Response.Status.UNAUTHORIZED, locale, userId, "please-enter-a-valid-password");
        } catch (RequiredLayoutException e) {

            res = this.authenticationHelper.getErrorResponse(request, Response.Status.INTERNAL_SERVER_ERROR, locale, userId, "user-without-portlet");
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
            res = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }

        return res;
    } // authentication



} // E:O:F:AuthenticationResource.
