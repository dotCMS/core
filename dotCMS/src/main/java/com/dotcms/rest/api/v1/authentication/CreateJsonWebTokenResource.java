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
import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.util.SecurityLoggerServiceAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
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
import java.util.Locale;

import static com.dotcms.util.CollectionsUtils.map;
import static java.util.Collections.EMPTY_MAP;

/**
 * Create a new Json Web Token
 * @author jsanca
 */
@Path("/v1/authentication")
public class CreateJsonWebTokenResource implements Serializable {

    private final static int JSON_WEB_TOKEN_MAX_ALLOWED_EXPIRATION_DAYS_DEFAULT_VALUE = 30;
    private final UserLocalManager         userLocalManager;
    private final LoginService             loginService;
    private final ResponseUtil             responseUtil;
    private final JsonWebTokenUtils        jsonWebTokenUtils;
    private final SecurityLoggerServiceAPI securityLoggerServiceAPI;

    /**
     * Default constructor.
     */
    public CreateJsonWebTokenResource() {
        this(LoginServiceFactory.getInstance().getLoginService(),
                UserLocalManagerFactory.getManager(),
                ResponseUtil.INSTANCE,
                JsonWebTokenUtils.getInstance(),
                APILocator.getSecurityLogger()
                );
    }

    @VisibleForTesting
    protected CreateJsonWebTokenResource(final LoginService loginService,
                                     final UserLocalManager userLocalManager,
                                     final ResponseUtil responseUtil,
                                     final JsonWebTokenUtils     jsonWebTokenUtils,
                                     final SecurityLoggerServiceAPI securityLoggerServiceAPI
                                     ) {

        this.loginService               = loginService;
        this.userLocalManager           = userLocalManager;
        this.responseUtil       = responseUtil;
        this.jsonWebTokenUtils          = jsonWebTokenUtils;
        this.securityLoggerServiceAPI   = securityLoggerServiceAPI;
    }

    @POST
    @Path("/api-token")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getApiToken(@Context final HttpServletRequest request,
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
                final int jwtMaxAge = createTokenForm.getExpirationDays() > 0 ?
                        this.getExpirationDays (createTokenForm.getExpirationDays()):
                        Config.getIntProperty(
                            LoginService.JSON_WEB_TOKEN_DAYS_MAX_AGE,
                            LoginService.JSON_WEB_TOKEN_DAYS_MAX_AGE_DEFAULT);

                this.securityLoggerServiceAPI.logInfo(this.getClass(),
                        "A Json Web Token " + userId.toLowerCase() + " has been created from IP: " +
                                HttpRequestDataUtil.getRemoteAddress(request));
                res = Response.ok(new ResponseEntityView(map("token",
                        createJsonWebToken(user, jwtMaxAge)), EMPTY_MAP)).build(); // 200
            } else {

                res = this.responseUtil.getErrorResponse(request, Response.Status.UNAUTHORIZED,
                        locale, userId, "authentication-failed");
            }
        } catch (NoSuchUserException | UserEmailAddressException | UserPasswordException e) {

            res = this.responseUtil.getErrorResponse(request, Response.Status.UNAUTHORIZED,
                    locale, userId, "authentication-failed");
        } catch (AuthException e) {

            res = this.responseUtil.getErrorResponse(request, Response.Status.UNAUTHORIZED,
                    locale, userId, "authentication-failed");
        } catch (RequiredLayoutException e) {

            res = this.responseUtil.getErrorResponse(request, Response.Status.INTERNAL_SERVER_ERROR,
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

    protected int getExpirationDays(final int expirationDays) {


        final int jsonWebTokenMaxAllowedExpirationDay =
                Config.getIntProperty(LoginService.JSON_WEB_TOKEN_MAX_ALLOWED_EXPIRATION_DAYS,
                        JSON_WEB_TOKEN_MAX_ALLOWED_EXPIRATION_DAYS_DEFAULT_VALUE);

        final int maxAllowedExpirationDays =
                (jsonWebTokenMaxAllowedExpirationDay > 0 && (expirationDays > jsonWebTokenMaxAllowedExpirationDay))?
                         this.getJsonWebTokenMaxAllowedExpirationDay(jsonWebTokenMaxAllowedExpirationDay, expirationDays):
                         expirationDays;

        Logger.debug(this, "Json Web Token Expiration days value: " + expirationDays + " days");

        return maxAllowedExpirationDays;
    }

    private int getJsonWebTokenMaxAllowedExpirationDay(final int jsonWebTokenMaxAllowedExpirationDay,
                                                       final int expirationDays) {

        Logger.debug(this, "Json Web Token Expiration days pass by the user is: " + expirationDays
                + " days, it exceeds the max allowed expiration day set in the configuration: " + jsonWebTokenMaxAllowedExpirationDay +
                ", so the expiration days for this particular token will be overriden to :" + jsonWebTokenMaxAllowedExpirationDay);
        return jsonWebTokenMaxAllowedExpirationDay;
    }

    /**
     * Creates Json Web Token
     * @param user {@link User}
     * @param jwtMaxAge {@link Integer}
     * @return String json web token
     * @throws PortalException
     * @throws SystemException
     */
    protected String createJsonWebToken (final User user, final int jwtMaxAge) throws PortalException, SystemException {

        return this.jsonWebTokenUtils.createToken(user, jwtMaxAge);
    }
} // E:O:F:CreateJsonWebTokenResource.
