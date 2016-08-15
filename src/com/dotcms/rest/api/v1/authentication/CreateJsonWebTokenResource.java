package com.dotcms.rest.api.v1.authentication;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.cms.login.LoginService;
import com.dotcms.cms.login.LoginServiceFactory;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import static com.dotcms.util.CollectionsUtils.*;
import com.dotmarketing.util.Config;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.Arrays;
import static java.util.Collections.*;
import java.util.Locale;

/**
 * Create a new Json Web Token
 * @author jsanca
 */
@Path("/v1/authentication")
public class CreateJsonWebTokenResource implements Serializable {

    private final UserLocalManager userLocalManager;
    private final LoginService loginService;
    private final AuthenticationHelper  authenticationHelper;

    /**
     * Default constructor.
     */
    public CreateJsonWebTokenResource() {
        this(LoginServiceFactory.getInstance().getLoginService(),
                UserLocalManagerFactory.getManager(),
                AuthenticationHelper.INSTANCE);
    }

    @VisibleForTesting
    protected CreateJsonWebTokenResource(final LoginService loginService,
                                     final UserLocalManager userLocalManager,
                                     final AuthenticationHelper  authenticationHelper
                                     ) {
        this.loginService = loginService;
        this.userLocalManager = userLocalManager;
        this.authenticationHelper = authenticationHelper;
    }

    @POST
    @Path("/api-token")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response authentication(@Context final HttpServletRequest request,
                                         @Context final HttpServletResponse response,
                                         final CreateTokenForm createTokenForm) {

        final String userId = createTokenForm.getUser();
        Response res = null;
        boolean authenticated = false;
        Locale locale = LocaleUtil.getLocale(request);

        try {

            authenticated =
                    this.loginService.doActionLogin(userId,
                            createTokenForm.getPassword(),
                            false, request, response);

            if (authenticated) {

                final HttpSession ses = request.getSession();
                final User user = this.userLocalManager.getUserById((String) ses.getAttribute(WebKeys.USER_ID));
                final int jwtMaxAge = createTokenForm.getExpirationDays() > 0 ? createTokenForm.getExpirationDays():
                        Config.getIntProperty(
                            LoginService.JSON_WEB_TOKEN_DAYS_MAX_AGE,
                            LoginService.JSON_WEB_TOKEN_DAYS_MAX_AGE_DEFAULT);

                if (null == locale && null != user) {

                    locale = user.getLocale();
                }

                res = Response.ok(new ResponseEntityView(map("token",
                        createJsonWebToken(user, jwtMaxAge)), EMPTY_MAP)).build(); // 200
            }
        } catch (NoSuchUserException | UserEmailAddressException e) {

            res = this.authenticationHelper.getErrorResponse(request, Response.Status.UNAUTHORIZED,
                    locale, userId, "authentication-failed");
        } catch (AuthException e) {

            res = this.authenticationHelper.getErrorResponse(request, Response.Status.UNAUTHORIZED,
                    locale, userId, "authentication-failed");
        } catch (RequiredLayoutException e) {

            res = this.authenticationHelper.getErrorResponse(request, Response.Status.INTERNAL_SERVER_ERROR,
                    locale, userId, "user-without-portlet");
        } catch (UserActiveException e) {

            try {

                res = Response.status(Response.Status.UNAUTHORIZED).entity(new ResponseEntityView
                        (Arrays.asList(new ErrorEntity("your-account-is-not-active",
                                LanguageUtil.format(locale,
                                        "your-account-is-not-active",
                                        new LanguageWrapper[] {new LanguageWrapper("<b><i>", userId, "</i></b>")},
                                        false)
                        )))).build();
            } catch (LanguageException e1) {
                // Quiet
            }
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            SecurityLogger.logInfo(this.getClass(),"An invalid attempt to login as "
                    + userId.toLowerCase() + " has been made from IP: " + request.getRemoteAddr());
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return res;
    } // authentication

    /**
     * Creates Json Web Token
     * @param user {@link User}
     * @param jwtMaxAge {@link Integer}
     * @return String json web token
     * @throws PortalException
     * @throws SystemException
     */
    protected String createJsonWebToken (final User user, final int jwtMaxAge) throws PortalException, SystemException {

        return JsonWebTokenUtils.createJsonWebToken(user, jwtMaxAge);
    }
} // E:O:F:CreateJsonWebTokenResource.
