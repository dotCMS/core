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
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.DotInvalidPasswordException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.ejb.UserManager;
import com.liferay.portal.ejb.UserManagerFactory;
import com.liferay.util.LocaleUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.Consumes;

/**
 * This resource change the user password.
 * If there is a known error returns 500 and the error messages related.
 * Otherwise returns 500 and the exception as Json.
 */
@Path("/v1/changePassword")
@SwaggerCompliant(value = "Core authentication and user management APIs", batch = 1)
@Tag(name = "Authentication")
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

    @Operation(
        summary = "Reset user password",
        description = "Resets a user's password using a valid token received via email or other secure channel"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Password reset successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityPasswordResetView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid login, token, or password",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - token expired or invalid",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @JSONP
    @InitRequestRequired
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public final Response resetPassword(@Context final HttpServletRequest request,
                                        @RequestBody(description = "Reset password form containing token and new password", 
                                                   required = true,
                                                   content = @Content(schema = @Schema(implementation = ResetPasswordForm.class)))
                                        final ResetPasswordForm resetPasswordForm) {

        Response res;
        final String password = resetPasswordForm.getPassword();
        final String token = resetPasswordForm.getToken();
        final Locale locale   = LocaleUtil.getLocale(request);

        try {

            final Optional<String> userIdOpt = APILocator.getUserAPI().getUserIdByToken(token);
            if(userIdOpt.isEmpty()){
                throw new DotInvalidTokenException(token);
            }

            this.userManager.resetPassword(userIdOpt.get(), token, password);

            SecurityLogger.logInfo(ResetPasswordResource.class,
                    String.format("User %s successful changed his password from IP: %s", userIdOpt.get(), request.getRemoteAddr()));
            res = Response.ok(new ResponseEntityView<>(userIdOpt.get())).build();
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