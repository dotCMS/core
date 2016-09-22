package com.dotcms.rest.api.v1.authentication;

import com.dotcms.auth.providers.jwt.beans.JWTBean;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
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
import com.dotmarketing.business.DotInvalidPasswordException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.ejb.UserManager;
import com.liferay.portal.ejb.UserManagerFactory;
import com.liferay.util.LocaleUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * This resource change the user password.
 * If there is a known error returns 500 and the error messages related.
 * Otherwise returns 500 and the exception as Json.
 */
@Path("/v1/changePassword")
public class ResetPasswordResource {

    private final UserManager userManager;
    private final ResponseUtil responseUtil;
    private final JsonWebTokenService   jsonWebTokenService;

    public ResetPasswordResource(){
        this ( UserManagerFactory.getManager(),
                ResponseUtil.INSTANCE,
                JsonWebTokenFactory.getInstance().getJsonWebTokenService());
    }

    @VisibleForTesting
    public ResetPasswordResource(final UserManager userManager,
                                 final ResponseUtil responseUtil,
                                 final JsonWebTokenService   jsonWebTokenService) {

        this.userManager = userManager;
        this.responseUtil = responseUtil;
        this.jsonWebTokenService  = jsonWebTokenService;
    }

    @POST
    @JSONP
    @InitRequestRequired
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response resetPassword(@Context final HttpServletRequest request,
                                        final ResetPasswordForm resetPasswordForm) {

        Response res = null;
        final String password = resetPasswordForm.getPassword();
        final String jwtToken = resetPasswordForm.getToken();
        final Locale locale   = LocaleUtil.getLocale(request);
        final String token;
        final String userId;
        final JWTBean jwtBean;

        try {

            jwtBean = this.jsonWebTokenService.parseToken(jwtToken);
            if (null == jwtBean) {
            	SecurityLogger.logInfo(ResetPasswordResource.class,
            			"Error reseting password. "
            	        + this.responseUtil.getFormattedMessage(null,"reset-password-token-expired"));
                res = this.responseUtil.getErrorResponse(request, Response.Status.UNAUTHORIZED, locale, null,
                        "reset-password-token-expired");
            } else {
                userId = jwtBean.getId();
                token = jwtBean.getSubject();

                this.userManager.resetPassword(userId, token, password);

                SecurityLogger.logInfo(ResetPasswordResource.class,
                		String.format("User %s successful changed his password from IP: %s", userId, request.getRemoteAddr()));
                res = Response.ok(new ResponseEntityView(userId)).build();
            }
        } catch (NoSuchUserException e) {
        	SecurityLogger.logInfo(ResetPasswordResource.class,
        			"Error resetting password. "
        	        + this.responseUtil.getFormattedMessage(null,"please-enter-a-valid-login"));
            res = this.responseUtil.getErrorResponse(request, Response.Status.BAD_REQUEST, locale, null,
                    "please-enter-a-valid-login");
        } catch (DotSecurityException e) {
        	SecurityLogger.logInfo(ResetPasswordResource.class,"Error resetting password. "+e.getMessage());
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        } catch (DotInvalidTokenException e) {
            if (e.isExpired()){
            	SecurityLogger.logInfo(ResetPasswordResource.class,
            			"Error resetting password. "
            	        + this.responseUtil.getFormattedMessage(null,"reset-password-token-expired"));
                res = this.responseUtil.getErrorResponse(request, Response.Status.UNAUTHORIZED, locale, null,
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
