package com.dotcms.rest.api.v1.authentication;

import static com.dotmarketing.util.Constants.CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD;

import com.dotcms.api.system.user.UserService;
import com.dotcms.api.system.user.UserServiceFactory;
import com.dotcms.company.CompanyAPI;
import com.dotcms.concurrent.DotConcurrentException;
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
import com.dotmarketing.db.DbConnectionFactory;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
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

    /** Shortest time between two warnings about reset requests dropped because the pool is full. */
    private static final int DROPPED_WARNING_INTERVAL_MILLIS = 60_000;

    /**
     * Submitted values (normalized) with a reset queued or running. A second request for a value
     * that is already pending is not queued again. Static because Jersey may create a resource
     * instance per request.
     */
    private static final Set<String> PENDING_RESETS = ConcurrentHashMap.newKeySet();

    /** Reset requests dropped since startup because the pool could not take them. */
    private static final AtomicLong DROPPED_RESETS = new AtomicLong();

    private final UserLocalManager userLocalManager;
    private final CompanyAPI  companyAPI;
    private final ResponseUtil responseUtil;
    private final UserService userService;
    private final Executor resetExecutor;

    /**
     * Creates the resource with the server's managers and the shared reset pool.
     */
    public ForgotPasswordResource() {

        this (UserLocalManagerFactory.getManager(),
                UserServiceFactory.getInstance().getUserService(),
                APILocator.getCompanyAPI(),
                ResponseUtil.INSTANCE
                );
    }

    /**
     * Creates the resource with the given collaborators and the shared reset pool.
     *
     * @param userLocalManager finds the account when the company logs in by user id
     * @param userService      creates the reset token and sends the reset email
     * @param companyAPI       resolves the company the request is for
     * @param responseUtil     builds the error responses
     */
    @VisibleForTesting
    public ForgotPasswordResource(final UserLocalManager userLocalManager,
                                  final UserService userService,
                                  final CompanyAPI  companyAPI,
                                  final ResponseUtil responseUtil) {

        this(userLocalManager, userService, companyAPI, responseUtil, resetPool());
    }

    /**
     * Creates the resource with the given collaborators and the executor that runs the
     * account-dependent part of each reset.
     *
     * @param userLocalManager finds the account when the company logs in by user id
     * @param userService      creates the reset token and sends the reset email
     * @param companyAPI       resolves the company the request is for
     * @param responseUtil     builds the error responses
     * @param resetExecutor    runs the account lookup, token and email after the response is built
     */
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
     * its queue is full the request is dropped (and a warning logged) instead of doing the work on
     * the request thread; the response is the same 200 either way.
     * Size and queue are set with {@code forgot-password.pool.size} (default 4) and
     * {@code forgot-password.queue.size} (default 100); the environment variables are
     * {@code DOT_FORGOT_PASSWORD_POOL_SIZE} and {@code DOT_FORGOT_PASSWORD_QUEUE_SIZE}.
     *
     * @return the pool used to process reset requests
     */
    private static Executor resetPool() {

        final int poolSize = Config.getIntProperty("forgot-password.pool.size", 4);

        return DotConcurrentFactory.getInstance().getSubmitter(FORGOT_PASSWORD_POOL,
                new DotConcurrentFactory.SubmitterConfigBuilder()
                        .poolSize(poolSize)
                        .maxPoolSize(poolSize)
                        .queueCapacity(Config.getIntProperty("forgot-password.queue.size", 100))
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
     * is not an email address is rejected with 400, as is a request with no body. A request for a
     * value that already has a reset pending, or that arrives while the pool's queue is full, gets
     * the same 200 and is not queued.
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

                this.queueReset(companyId, submittedValue, loginByUserId, remoteAddr, locale);
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
     * Hands the reset to the reset pool, unless one is already pending for the same value. When the
     * pool cannot take it the reset is dropped and a rate-limited warning is logged; the caller
     * answers the same way in every case.
     *
     * @param companyId      the id of the company the request is for
     * @param submittedValue the user id or email address submitted with the request
     * @param loginByUserId  whether the company logs in by user id instead of email address
     * @param remoteAddr     the address the request came from, for the security log
     * @param locale         the locale of the reset email
     */
    private void queueReset(final String companyId, final String submittedValue,
                            final boolean loginByUserId, final String remoteAddr,
                            final Locale locale) {

        final String pendingKey = pendingKey(companyId, submittedValue, loginByUserId);
        if (!PENDING_RESETS.add(pendingKey)) {

            return; // a reset for this value is already queued or running
        }

        try {

            this.resetExecutor.execute(() -> this.sendResetInBackground(
                    pendingKey, companyId, submittedValue, loginByUserId, remoteAddr, locale));
        } catch (DotConcurrentException | RejectedExecutionException e) {

            PENDING_RESETS.remove(pendingKey);
            Logger.warnEvery(this.getClass(), "forgot-password-reset-dropped",
                    String.format("The password reset pool is full; %d reset requests dropped since startup",
                            DROPPED_RESETS.incrementAndGet()),
                    DROPPED_WARNING_INTERVAL_MILLIS);
        }
    }

    /**
     * Returns the key under which a pending reset for the submitted value is tracked. Email
     * addresses are compared without case; user ids as submitted, apart from surrounding blanks.
     *
     * @param companyId      the id of the company the request is for
     * @param submittedValue the user id or email address submitted with the request
     * @param loginByUserId  whether the company logs in by user id instead of email address
     * @return the key for {@link #PENDING_RESETS}
     */
    @VisibleForTesting
    static String pendingKey(final String companyId, final String submittedValue,
                             final boolean loginByUserId) {

        final String value = loginByUserId ? submittedValue.trim() : submittedValue.trim().toLowerCase();
        return companyId + ':' + value;
    }

    /**
     * Returns whether a reset is queued or running for the given key.
     *
     * @param pendingKey a key from {@link #pendingKey}
     * @return true while the reset for that key has not finished
     */
    @VisibleForTesting
    static boolean isResetPending(final String pendingKey) {

        return PENDING_RESETS.contains(pendingKey);
    }

    /**
     * Runs {@link #sendReset} on the reset pool, where nobody is waiting for an answer: whatever
     * happens is written to the log, never thrown or returned. Afterwards the thread's database
     * connection and Hibernate session are closed, so the next task on the same pool thread starts
     * with a fresh session instead of one holding this task's cached entities, and the value's
     * pending entry is released.
     *
     * @param pendingKey     the key of this reset in {@link #PENDING_RESETS}
     * @param companyId      the id of the company the request is for
     * @param submittedValue the user id or email address submitted with the request
     * @param loginByUserId  whether the company logs in by user id instead of email address
     * @param remoteAddr     the address the request came from, for the security log
     * @param locale         the locale of the reset email
     */
    private void sendResetInBackground(final String pendingKey,
                                       final String companyId, final String submittedValue,
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
        } finally {

            DbConnectionFactory.closeSilently();
            PENDING_RESETS.remove(pendingKey);
        }
    }

} // E:O:F:ForgotPasswordResource.
