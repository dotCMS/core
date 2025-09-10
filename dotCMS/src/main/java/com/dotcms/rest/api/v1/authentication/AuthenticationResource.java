package com.dotcms.rest.api.v1.authentication;

import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityMapMapView;
import com.dotcms.rest.ResponseEntityMapView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.LoginMode;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.RequiredLayoutException;
import com.liferay.portal.UserActiveException;
import com.liferay.portal.UserEmailAddressException;
import com.liferay.portal.UserPasswordException;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.ejb.UserLocalManagerFactory;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.language.LanguageWrapper;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.LocaleUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;

/**
 * This resource does the authentication, if the authentication is successfully
 * returns the User Object as a Json, If there is a known error returns 500 and
 * the error messages related. Otherwise returns 500 and the exception as Json.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jul 7, 2016
 */


@SwaggerCompliant(value = "Core authentication and authorization APIs", batch = 1)
@SuppressWarnings("serial")
@Path("/v1/authentication")
@Tag(name = "Authentication",
        externalDocs = @ExternalDocumentation(description = "Additional Authentication API information",
                                                url = "https://www.dotcms.com/docs/latest/rest-api-authentication"))

public class AuthenticationResource implements Serializable {

    static final String USER = "user";
    static final String LOGIN_AS_USER = "loginAsUser";

    private final UserLocalManager userLocalManager;
    private final LoginServiceAPI loginService;
    private final ResponseUtil responseUtil;

    private final AuthenticationHelper authenticationHelper;

    /**
     * Default constructor.
     */
    public AuthenticationResource() {
        this(APILocator.getLoginServiceAPI(),
                UserLocalManagerFactory.getManager(),
                ResponseUtil.INSTANCE,
                AuthenticationHelper.getInstance());
    }

    @VisibleForTesting
    protected AuthenticationResource(final LoginServiceAPI loginService,
                                     final UserLocalManager userLocalManager,
                                     final ResponseUtil responseUtil,
                                     AuthenticationHelper authenticationHelper) {
        this.loginService = loginService;
        this.userLocalManager = userLocalManager;
        this.responseUtil = responseUtil;
        this.authenticationHelper = authenticationHelper;
    }

    @POST
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "postAuthenticationV1",
                summary = "Verifies user or application authentication",
                description = "Takes a user's login ID and password and checks them against the user rolls.\n\n" +
                                "If the user is found and authenticated, a session is created.\n\n" +
                                "Otherwise the system will return an 'authentication failed' message.\n\n",
                tags = {"Authentication"},
                responses = {
                    @ApiResponse(responseCode = "200", description = "User authentication successful",
                        content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityMapView.class))),
                    @ApiResponse(responseCode = "401", description = "User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden request"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type"),
                    @ApiResponse(responseCode = "500", description = "Unexpected error")
                }
            )
    public final Response authentication(
                                   @Context final HttpServletRequest request,
                                   @Context final HttpServletResponse response,
                                   @RequestBody(description = "This method takes a user's credentials and language preferences to authenticate them.\n\n" +
                                                                "Requires a POST body consisting of a JSON object containing the following properties:\n\n" + 
                                                                "| **Property** | **Value** | **Description**                               |\n" +
                                                                "|--------------|-----------|-----------------------------------------------|\n" +
                                                                "| `userId`     | String    | **Required.** ID of user attempting to log in |\n" +
                                                                "| `password`   | String    | User password                                 |\n" +
                                                                "| `language`   | String    | Preferred language for user                   |\n" +
                                                                "| `country`    | String    | Country where user is located                 |\n",
                                                required = true,
                                                content = @Content(
                                                    schema = @Schema(implementation = AuthenticationForm.class)
                                                ))
                                   final AuthenticationForm authenticationForm) {

        Response res = null;
        boolean authenticated = false;
        String userId = authenticationForm.getUserId();
        final Locale locale = LocaleUtil.getLocale(request,
                authenticationForm.getCountry(), authenticationForm.getLanguage());

        try {

            authenticated = authenticationForm.isBackEndLogin() 
                ? this.loginService.doBackEndLogin(userId,
                    authenticationForm.getPassword(),
                    authenticationForm.isRememberMe(), request, response)
                : this.loginService.doActionLogin(userId,
                            authenticationForm.getPassword(),
                            authenticationForm.isRememberMe(), request, response);

            if (authenticated) {

                final HttpSession ses = request.getSession();
                final User user = this.userLocalManager.getUserById((String) ses.getAttribute(WebKeys.USER_ID));
                final Map<String, Object> userMap = user.toMap();

                userMap.put("loggedInDate", new Date());

                String pageUrl = (String) request.getSession().getAttribute(WebKeys.LOGIN_TO_EDIT_MODE);

                if (pageUrl != null) {
                    userMap.put("editModeUrl", pageUrl);
                    request.getSession().removeAttribute(WebKeys.LOGIN_TO_EDIT_MODE);
                }

                LoginMode.set(request,
                        authenticationForm.isBackEndLogin()? LoginMode.BE:LoginMode.FE);

                res = Response.ok(new ResponseEntityMapView(userMap)).build(); // 200
                request.getSession().setAttribute(Globals.LOCALE_KEY, locale);
            } else {

                res = this.responseUtil.getErrorResponse(request, Response.Status.UNAUTHORIZED,
                        locale, userId, "authentication-failed");
            }
        } catch (NoSuchUserException | UserEmailAddressException | UserPasswordException | AuthException e) {
            res = this.responseUtil.getErrorResponse(request, Response.Status.UNAUTHORIZED, locale, userId, "authentication-failed");
        }  catch (RequiredLayoutException e) {
            res = this.responseUtil.getErrorResponse(request, Response.Status.INTERNAL_SERVER_ERROR, locale, userId, "user-without-portlet");
        } catch (UserActiveException e) {

            try {

                res = Response.status(Response.Status.UNAUTHORIZED).entity(new ResponseEntityView<>
                        (List.of(new ErrorEntity("your-account-is-not-active",
                                LanguageUtil.format(locale,
                                        "your-account-is-not-active", new LanguageWrapper[]{
                                                new LanguageWrapper("<b><i>", userId, "</i></b>")},
                                        false)
                        )))).build();
            } catch (LanguageException e1) {
                // Quiet
            }
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            SecurityLogger.logInfo(this.getClass(),"An invalid attempt to login as " + userId.toLowerCase() + " has been made from IP: " + request.getRemoteAddr());
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return res;
    } // authentication

    @GET
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getLogInUserV1",
                summary = "Retrieves user data",
                description = "Provides information about any users that are currently in a session.\n\n" +
                                "This retrieved data will be formatted into a JSON response body.\n\n",
                tags = {"Authentication"},
                responses = {
                    @ApiResponse(responseCode = "200", description = "User data successfully collected",
                                content = @Content(
                                    schema = @Schema(implementation = ResponseEntityMapMapView.class)
                                )),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized request"),
                    @ApiResponse(responseCode = "404", description = "User not found")
                })
    @Path("logInUser")
    public final Response getLoginUser(@Context final HttpServletRequest request){
        Response res = null;

        try {
            Map<String, Map<String,Object>> users = authenticationHelper.getUsers(request);
            // todo: add here the loggedInDate???
            res = Response.ok(new ResponseEntityMapMapView(users)).build();
        } catch (Exception e) {
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return res;
    }

} // E:O:F:AuthenticationResource.
