package com.dotcms.rest.api.v1.authentication;

import static com.dotmarketing.util.Constants.CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD;

import com.dotcms.api.system.user.UserService;
import com.dotcms.api.system.user.UserServiceFactory;
import com.dotcms.company.CompanyAPI;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.google.common.annotations.VisibleForTesting;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.SecurityUtils;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.SendPasswordException;
import com.liferay.portal.UserEmailAddressException;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.ejb.UserLocalManagerFactory;
import com.liferay.portal.model.Company;
import com.liferay.util.LocaleUtil;
import com.liferay.util.Validator;
import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.Consumes;

/**
 * This resource sends email with a link to recovery your password. A request that is accepted returns the submitted user id or email address,
 * If there is a known error returns 500 and the error messages related.
 * Otherwise returns 500 and the exception as Json.
 * @author jsanca
 */
@Path("/v1/forgotpassword")
@SwaggerCompliant(value = "Core authentication and user management APIs", batch = 1)
@Tag(name = "Authentication")
public class ForgotPasswordResource implements Serializable {

    /** Name of the thread pool that creates the reset token and sends the reset email. */
    static final String FORGOT_PASSWORD_POOL = "forgotPasswordPool";

    private final UserLocalManager userLocalManager;
    private final CompanyAPI  companyAPI;
    private final ResponseUtil responseUtil;
    private final UserService userService;
    private final Executor resetExecutor;

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

        this(userLocalManager, userService, companyAPI, responseUtil, resetPool());
    }

    @VisibleForTesting
    public ForgotPasswordResource(final UserLocalManager userLocalManager,
                                  final UserService userService,
                                  final CompanyAPI  companyAPI,
                                  final ResponseUtil responseUtil,
                                  final Executor resetExecutor) {

        this.userLocalManager = userLocalManager;
        this.userService      = userService;
        this.companyAPI       = companyAPI;
        this.responseUtil = responseUtil;
        this.resetExecutor = resetExecutor;
    }

    /**
     * Returns the pool that runs the account-dependent part of a reset request. It is bounded: when
     * its queue is full the endpoint answers 429 instead of doing the work on the request thread.
     * Size and queue are set with {@code FORGOT_PASSWORD_POOL_SIZE} (default 4) and
     * {@code FORGOT_PASSWORD_QUEUE_SIZE} (default 100).
     *
     * @return the pool used to process reset requests
     */
    private static Executor resetPool() {

        return DotConcurrentFactory.getInstance().getSubmitter(FORGOT_PASSWORD_POOL,
                new DotConcurrentFactory.SubmitterConfigBuilder()
                        .poolSize(Config.getIntProperty("FORGOT_PASSWORD_POOL_SIZE", 4))
                        .maxPoolSize(Config.getIntProperty("FORGOT_PASSWORD_POOL_SIZE", 4))
                        .queueCapacity(Config.getIntProperty("FORGOT_PASSWORD_QUEUE_SIZE", 100))
                        .allowCoreThreadTimeOut(true)
                        .rejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy())
                        .build());
    }

    /**
     * Requests a password reset email for the account matching the submitted value: the user id
     * when the company logs in by user id, otherwise the email address.
     * <p>
     * Every request whose value is accepted gets the same 200 response carrying the submitted
     * value, whether or not an account matches and whether or not the reset could be completed.
     * The work that depends on the account (looking it up, creating the reset token, sending the
     * email) runs on a separate thread pool after the response is built, so the request takes the
     * same time either way; a failure there is logged. With login by email address, a value that
     * is not an email address is rejected with 400, as is a request with no body. When the pool's
     * queue is full the request is rejected with 429.
     * <p>
     * When {@code CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD} is enabled, the account is
     * looked up on the request thread and an unknown account is reported with 400.
     *
     * @param request            the HTTP request
     * @param response           the HTTP response
     * @param forgotPasswordForm the {@link ForgotPasswordForm} holding the user id or email address
     * @return the {@link Response}
     */
    @Operation(
        summary = "Send password reset email",
        description = "Sends a password reset email to the account matching the submitted user ID or email address. Returns the submitted value."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Request accepted. The entity is the submitted user ID or email address",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityForgotPasswordView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - with login by email address, the submitted value is not a valid email address; or user not found (if configured to show)",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "429",
                    description = "Too many reset requests are waiting to be processed",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @JSONP
    @NoCache
    @InitRequestRequired
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public final Response forgotPassword(@Context final HttpServletRequest request,
                                         @Context final HttpServletResponse response,
                                         @RequestBody(description = "Forgot password form containing user ID or email address", 
                                                    required = true,
                                                    content = @Content(schema = @Schema(implementation = ForgotPasswordForm.class)))
                                         final ForgotPasswordForm forgotPasswordForm) {

        if (null == forgotPasswordForm) {

            return ExceptionMapperUtil.createResponse("A request body with a userId is required",
                    Response.Status.BAD_REQUEST);
        }

        Response res;
        final String submittedValue = forgotPasswordForm.getUserId();
        final Locale locale = LocaleUtil.getLocale(request);

        try {

            final Company company = this.companyAPI.getCompany(request);
            final boolean loginByUserId = Company.AUTH_TYPE_ID.equals(company.getAuthType());

            if (!loginByUserId && !Validator.isEmailAddress(submittedValue.trim().toLowerCase())) {

                return this.responseUtil.getErrorResponse(request, Response.Status.BAD_REQUEST, locale, null,
                        "please-enter-a-valid-email-address");
            }

            final String companyId = this.companyAPI.getCompanyId(request);
            final String remoteAddr = request.getRemoteAddr();

            if (Config.getBooleanProperty(CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD, false)) {

                this.sendReset(companyId, submittedValue, loginByUserId, remoteAddr, locale);
            } else {

                this.resetExecutor.execute(() -> this.sendResetInBackground(
                        companyId, submittedValue, loginByUserId, remoteAddr, locale));
            }

            res = Response.ok(new ResponseEntityView<>(submittedValue)).build(); // 200

        } catch (NoSuchUserException e) {

            // only reached when CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD is enabled
            SecurityLogger.logInfo(this.getClass(),
                    String.format(
                            "User [%s] does NOT exist in the Database. IP [%s]. %s property is TRUE",
                            SecurityUtils.sanitizeForLogging(submittedValue), request.getRemoteAddr(),
                            CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD));

            res = this.responseUtil.getErrorResponse(request, Response.Status.BAD_REQUEST, locale, null,
                    "the-email-address-you-requested-is-not-registered-in-our-database");
        } catch (SendPasswordException e) {

            res = this.responseUtil.getErrorResponse(request, Response.Status.BAD_REQUEST, locale, null,
                      "a-new-password-can-only-be-sent-to-an-external-email-address");
        } catch (UserEmailAddressException e) {

            // the request itself was accepted above; the reset could not be completed for the matching account
            SecurityLogger.logInfo(this.getClass(),
                    String.format("The password reset for [%s] could not be completed. IP [%s]",
                            SecurityUtils.sanitizeForLogging(submittedValue), request.getRemoteAddr()));
            res = Response.ok(new ResponseEntityView<>(submittedValue)).build(); // 200
        } catch (RejectedExecutionException e) {

            Logger.warn(this.getClass(), "Too many password reset requests are waiting to be processed");
            res = Response.status(429)
                    .entity(Map.of("message", "Too many requests, please try again later"))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {

            Logger.error(this.getClass(), "Error processing forgot password request", e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return res;
    } // authentication.

    /**
     * Finds the account, creates its reset token and sends the reset email.
     *
     * @param companyId      the id of the company the request is for
     * @param submittedValue the user id or email address submitted with the request
     * @param loginByUserId  whether the company logs in by user id instead of email address
     * @param remoteAddr     the address the request came from, for the security log
     * @param locale         the locale of the reset email
     * @throws Exception if there is no such account or the reset could not be sent
     */
    private void sendReset(final String companyId, final String submittedValue,
                           final boolean loginByUserId, final String remoteAddr,
                           final Locale locale) throws Exception {

        final String emailAddress = loginByUserId?
                    this.userLocalManager.getUserById(submittedValue).getEmailAddress():
                    submittedValue;

        SecurityLogger.logInfo(this.getClass(),
                String.format(
                        "Email address [%s] has requested to reset his password from IP [%s].",
                        SecurityUtils.sanitizeForLogging(emailAddress), remoteAddr));

        this.userService.sendResetPassword(companyId, emailAddress, locale);
    }

    /**
     * Runs {@link #sendReset} on the reset pool, where nobody is waiting for an answer: whatever
     * happens is written to the log, never thrown or returned.
     *
     * @param companyId      the id of the company the request is for
     * @param submittedValue the user id or email address submitted with the request
     * @param loginByUserId  whether the company logs in by user id instead of email address
     * @param remoteAddr     the address the request came from, for the security log
     * @param locale         the locale of the reset email
     */
    private void sendResetInBackground(final String companyId, final String submittedValue,
                                       final boolean loginByUserId, final String remoteAddr,
                                       final Locale locale) {

        try {

            this.sendReset(companyId, submittedValue, loginByUserId, remoteAddr, locale);
        } catch (NoSuchUserException e) {

            SecurityLogger.logInfo(this.getClass(),
                    String.format(
                            "User [%s] does NOT exist in the Database, no reset email sent. IP [%s]",
                            SecurityUtils.sanitizeForLogging(submittedValue), remoteAddr));
        } catch (UserEmailAddressException e) {

            SecurityLogger.logInfo(this.getClass(),
                    String.format("The password reset for [%s] could not be completed. IP [%s]",
                            SecurityUtils.sanitizeForLogging(submittedValue), remoteAddr));
        } catch (Exception e) {

            Logger.error(this.getClass(), "Error processing forgot password request", e);
        }
    }

} // E:O:F:ForgotPasswordResource.
