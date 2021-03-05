package com.dotcms.rest.api.v1.authentication;

import com.dotmarketing.business.APILocator;
import java.util.Locale;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.DotInvalidPasswordException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.ejb.UserManager;
import com.liferay.portal.ejb.UserManagerFactory;
import com.liferay.util.LocaleUtil;

/**
 * This resource change the user password.
 * If there is a known error returns 500 and the error messages related.
 * Otherwise returns 500 and the exception as Json.
 */
@Path("/v1/changePassword")
public class ResetPasswordResource {

    private final UserManager userManager;
    private final ResponseUtil responseUtil;

    public ResetPasswordResource(){
        this ( UserManagerFactory.getManager(),
                ResponseUtil.INSTANCE);
    }

    @VisibleForTesting
    public ResetPasswordResource(final UserManager userManager,
                                 final ResponseUtil responseUtil) {

        this.userManager = userManager;
        this.responseUtil = responseUtil;
    }

    @POST
    @JSONP
    @InitRequestRequired
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response resetPassword(@Context final HttpServletRequest request,
                                        final ResetPasswordForm resetPasswordForm) {

        Response res;
        final String password = resetPasswordForm.getPassword();
        final String token = resetPasswordForm.getToken();
        final Locale locale   = LocaleUtil.getLocale(request);

        try {

            final Optional<String> userIdOpt = APILocator.getUserAPI().getUserIdByIcqId(token);
            if(!userIdOpt.isPresent()){
                throw new DotInvalidTokenException(token);
            }

            this.userManager.resetPassword(userIdOpt.get(), token, password);

            SecurityLogger.logInfo(ResetPasswordResource.class,
                    String.format("User %s successful changed his password from IP: %s", userIdOpt.get(), request.getRemoteAddr()));
            res = Response.ok(new ResponseEntityView(userIdOpt.get())).build();
        } catch (NoSuchUserException e) {
        	SecurityLogger.logInfo(ResetPasswordResource.class,
        			"Error resetting password. "
        	        + this.responseUtil.getFormattedMessage(null,"please-enter-a-valid-login"));
            res = this.responseUtil.getErrorResponse(request, Response.Status.BAD_REQUEST, locale, null,
                    "please-enter-a-valid-login");
        } catch (DotSecurityException e) {
        	SecurityLogger.logInfo(ResetPasswordResource.class,"Error resetting password. "+e.getMessage());
            throw new ForbiddenException(e);
        } catch (DotInvalidTokenException e) {
            if (e.isExpired()){
            	SecurityLogger.logInfo(ResetPasswordResource.class,
            			"Error resetting password. "
            	        + this.responseUtil.getFormattedMessage(null,"reset-password-token-expired"));
                res = this.responseUtil.getErrorResponse(request, Status.FORBIDDEN, locale, null,
                        "reset-password-token-expired");
            }else{
            	SecurityLogger.logInfo(ResetPasswordResource.class,
            			"Error resetting password. "
            	        + this.responseUtil.getFormattedMessage(null,"reset-password-token-invalid"));
                res = this.responseUtil.getErrorResponse(request, Response.Status.BAD_REQUEST, locale, null,
                        "reset-password-token-invalid");
            }
        } catch (DotInvalidPasswordException e){
        	SecurityLogger.logInfo(ResetPasswordResource.class,
        			"Error resetting password. "
        	        + this.responseUtil.getFormattedMessage(null,"reset-password-invalid-password"));
            res = this.responseUtil.getErrorResponse(request, Response.Status.BAD_REQUEST, locale, null,
                    "reset-password-invalid-password");
        }catch (Exception  e) {
        	SecurityLogger.logInfo(ResetPasswordResource.class,"Error resetting password. "+e.getMessage());
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return res;
    }

}