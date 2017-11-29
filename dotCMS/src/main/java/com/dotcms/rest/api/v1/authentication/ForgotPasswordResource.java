package com.dotcms.rest.api.v1.authentication;

import static com.dotmarketing.util.Constants.CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD;

import com.dotcms.api.system.user.UserService;
import com.dotcms.api.system.user.UserServiceFactory;
import com.dotcms.company.CompanyAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.SendPasswordException;
import com.liferay.portal.UserEmailAddressException;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.ejb.UserLocalManagerFactory;
import com.liferay.portal.model.Company;
import com.liferay.util.LocaleUtil;
import java.io.Serializable;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This resource sends email with a link to recovery your password, if it is successfully returns the User email where the message is gonna be sent,
 * If there is a known error returns 500 and the error messages related.
 * Otherwise returns 500 and the exception as Json.
 * @author jsanca
 */
@Path("/v1/forgotpassword")
public class ForgotPasswordResource implements Serializable {

    private final UserLocalManager userLocalManager;
    private final CompanyAPI  companyAPI;
    private final ResponseUtil responseUtil;
    private final UserService userService;

    public ForgotPasswordResource() {

        this (UserLocalManagerFactory.getManager(),
                UserServiceFactory.getInstance().getUserService(),
                APILocator.getCompanyAPI(),
                ResponseUtil.INSTANCE
                );
    }

    @VisibleForTesting
    public ForgotPasswordResource(final UserLocalManager userLocalManager,
                                  final UserService userService,
                                  final CompanyAPI  companyAPI,
                                  final ResponseUtil responseUtil) {

        this.userLocalManager = userLocalManager;
        this.userService      = userService;
        this.companyAPI       = companyAPI;
        this.responseUtil = responseUtil;
    }

    @POST
    @JSONP
    @NoCache
    @InitRequestRequired
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response forgotPassword(@Context final HttpServletRequest request,
                                         @Context final HttpServletResponse response,
                                         final ForgotPasswordForm forgotPasswordForm) {

        Response res;
        String emailAddress = null;
        final Locale locale = LocaleUtil.getLocale(request);

        try {

            final Company company = this.companyAPI.getCompany(request);

            emailAddress = (Company.AUTH_TYPE_ID.equals(company.getAuthType()))?
                        this.userLocalManager.getUserById
                                (forgotPasswordForm.getUserId()).getEmailAddress():
                        forgotPasswordForm.getUserId();

            SecurityLogger.logInfo(this.getClass(),
                    String.format(
                            "Email address [%s] has request to reset his password from IP [%s].",
                            emailAddress, request.getRemoteAddr()));

            this.userService.sendResetPassword(
                    this.companyAPI.getCompanyId(request), emailAddress, locale);

            res = Response.ok(new ResponseEntityView(emailAddress)).build(); // 200

        } catch (NoSuchUserException e) {

            boolean displayNotSuchUserError =
                    Config.getBooleanProperty(CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD,
                            false);

            if (displayNotSuchUserError) {

                SecurityLogger.logInfo(this.getClass(),
                        String.format(
                                "User [%s] does NOT exist in the Database. IP [%s]. %s property is TRUE",
                                emailAddress, request.getRemoteAddr(),
                                CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD));

                res = this.responseUtil.getErrorResponse(request, Response.Status.BAD_REQUEST, locale, null,
                        "the-email-address-you-requested-is-not-registered-in-our-database");
            } else {

                SecurityLogger.logInfo(this.getClass(),
                        String.format(
                                "User [%s] does NOT exist in the Database, returning OK message for security reasons. IP [%s]",
                                emailAddress, request.getRemoteAddr()));

                res = Response.ok(new ResponseEntityView(emailAddress)).build(); // 200
            }
        } catch (SendPasswordException e) {

            res = this.responseUtil.getErrorResponse(request, Response.Status.BAD_REQUEST, locale, null,
                      "a-new-password-can-only-be-sent-to-an-external-email-address");
        } catch (UserEmailAddressException e) {

            res = this.responseUtil.getErrorResponse(request, Response.Status.BAD_REQUEST, locale, null,
                    "please-enter-a-valid-email-address");
        } catch (Exception e) {

            Logger.error(this.getClass(), "Error processing forgot password request", e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return res;
    } // authentication.

} // E:O:F:ForgotPasswordResource.
