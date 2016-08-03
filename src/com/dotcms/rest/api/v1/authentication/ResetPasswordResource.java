package com.dotcms.rest.api.v1.authentication;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.SecurityLoggerServiceAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotInvalidPasswordException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.ejb.UserLocalManagerFactory;
import com.liferay.portal.ejb.UserManager;
import com.liferay.portal.ejb.UserManagerFactory;
import com.liferay.util.LocaleUtil;
import com.liferay.util.servlet.SessionMessages;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * This resource change the user password.
 * If there is a known error returns 500 and the error messages related.
 * Otherwise returns 500 and the exception as Json.
 */
@Path("/v1/changePassword")
public class ResetPasswordResource {

    private final UserManager userManager;
    private final SecurityLoggerServiceAPI securityLogger;
    private final AuthenticationHelper  authenticationHelper;

    public ResetPasswordResource(){
        this ( UserManagerFactory.getManager(),
                APILocator.getSecurityLogger(),
                AuthenticationHelper.INSTANCE);
    }

    @VisibleForTesting
    public ResetPasswordResource(UserManager userManager, SecurityLoggerServiceAPI securityLogger,
                                 AuthenticationHelper authenticationHelper) {
        this.userManager = userManager;
        this.securityLogger = securityLogger;
        this.authenticationHelper = authenticationHelper;
    }

    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response resetPassword(@Context final HttpServletRequest request,
                                         final ResetPasswordForm resetPasswordForm) {

        Response res = null;

        String userId = resetPasswordForm.getUserId();
        String password = resetPasswordForm.getPassword();
        String token = resetPasswordForm.getToken();

        final Locale locale = LocaleUtil.getLocale(request);

        try {
            userManager.resetPassword( userId, token, password);

            this.securityLogger.logInfo(this.getClass(),
                    String.format("User %s successful changed his password from IP: %s", userId, request.getRemoteAddr()));
            res = Response.ok(new ResponseEntityView( userId )).build();
        } catch (NoSuchUserException e) {
            res = this.authenticationHelper.getErrorResponse(request, Response.Status.BAD_REQUEST, locale, null,
                    "please-enter-a-valid-login");
        } catch (DotSecurityException e) {
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        } catch (TokenUnvalidException e) {
            if (e.isExpired()){
                res = this.authenticationHelper.getErrorResponse(request, Response.Status.UNAUTHORIZED, locale, null,
                        "reset_token_expired");
            }else{
                res = this.authenticationHelper.getErrorResponse(request, Response.Status.BAD_REQUEST, locale, null,
                        "reset-password-token-invalid");
            }
        } catch (DotInvalidPasswordException e){
            res = this.authenticationHelper.getErrorResponse(request, Response.Status.BAD_REQUEST, locale, null,
                    "reset-password-invalid-password");
        }catch (Exception  e) {
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return res;
    }
}
