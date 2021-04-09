package com.dotcms.rest.api.v1.authentication;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestClientBuilder;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.*;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.net.ConnectException;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.glassfish.jersey.internal.util.Base64;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dotcms.util.CollectionsUtils.map;
import static java.util.Collections.EMPTY_MAP;

/**
 * Endpoint to handle Api Tokens
 */
@Path("/v1/apitoken")
public class ApiTokenResource implements Serializable {

    private final ApiTokenAPI tokenApi;
    private final WebResource webResource;
    private Client restClient;

    /**
     * Default constructor.
     */
    public ApiTokenResource() {
        this(APILocator.getApiTokenAPI(), new WebResource());
    }

    @VisibleForTesting
    protected ApiTokenResource(final ApiTokenAPI tokenApi, final WebResource webResource) {

        this.tokenApi    = tokenApi;
        this.webResource = webResource;
    }

    @GET
    @Path("/{userId}/tokens")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getApiTokens(
            @Context final HttpServletRequest request, 
            @Context final HttpServletResponse response,
            @PathParam("userId") final String userId, 
            @QueryParam("showRevoked") final boolean showRevoked) {


        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, "users");
        final List<ApiToken> tokens = tokenApi.findApiTokensByUserId(userId, showRevoked, initDataObject.getUser());

        return Response.ok(new ResponseEntityView(map("tokens", tokens), EMPTY_MAP)).build(); // 200
    }

    @PUT
    @Path("/{tokenId}/revoke")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response revokeApiToken(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @PathParam("tokenId") final String tokenId) {

        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, "users");
        final User user                     = initDataObject.getUser();
        final Optional<ApiToken> optToken   = this.tokenApi.findApiToken(tokenId);

        if (optToken.isPresent()) {

            ApiToken token = optToken.get();
            
            if(token.isExpired()) {

                return ExceptionMapperUtil.createResponse(new DotStateException("Token Expired"), Response.Status.NOT_ACCEPTABLE);
            }

            SecurityLogger.logInfo(this.getClass(), "Revoking token " + token + " from " + request.getRemoteAddr() + " ");
            this.tokenApi.revokeToken(token, user);
            token = this.tokenApi.findApiToken(tokenId).get();
            return Response.ok(new ResponseEntityView(map("revoked", token), EMPTY_MAP)).build(); // 200
        }

        return ExceptionMapperUtil.createResponse(new DotStateException("No token"), Response.Status.NOT_FOUND);
    }

    @DELETE
    @Path("/{tokenId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteApiToken(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @PathParam("tokenId") final String tokenId) {

        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, "users");
        final User user                     = initDataObject.getUser();
        final Optional<ApiToken> optToken   = tokenApi.findApiToken(tokenId);

        if (optToken.isPresent()) {

            final ApiToken token = optToken.get();
            SecurityLogger.logInfo(this.getClass(), "Deleting token " + token + " from " + request.getRemoteAddr() + " ");

            if(tokenApi.deleteToken(token, user)) {

                return Response.ok(new ResponseEntityView(map("deleted", token), EMPTY_MAP)).build(); // 200
            }

            return ExceptionMapperUtil.createResponse(new DotStateException("No permissions to token"), Response.Status.FORBIDDEN);
        }

        return ExceptionMapperUtil.createResponse(new DotStateException("No token"), Response.Status.NOT_FOUND);
    }

    /**
     * Issue a new APIToken.
     * If the shouldBeAdmin is true then response with a 403 if the user for whom the token is not admin
     * If the userId parameter is null then the token is generated to the requesting user
     *
     * @param request
     * @param response
     * @param formData - json data, expecting {netmask:'192.168.1.0/24', expirationDays:1000, userId:'dotcms.org.1', shouldBeAdmin: 'true'}
     * @return Response
     */
    @POST
    @Path("/")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response issueApiToken(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final ApiTokenForm formData) throws DotDataException, DotSecurityException {
        
        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
        final User requestingUser = initDataObject.getUser();
        final User forUser = formData.userId != null ? getUserById(formData, requestingUser) : requestingUser;

        if(forUser == null) {
            return ExceptionMapperUtil.createResponse(new DotStateException("No user found"), Response.Status.NOT_FOUND);
        }
        
        if (requestingUser != forUser && !requestingUser.isAdmin()) {
            throw new DotDataException("Just Admin user can request a Token for another user");
        }

        if (!forUser.isAdmin() && formData.shouldBeAdmin) {
            throw new DotSecurityException("User should be Admin");
        }

        final String netmaskStr = formData.network!=null && !"0.0.0.0/0".equals(formData.network)? formData.network:null;
        final String netmask    = Try.of(()->new SubnetUtils(netmaskStr).getInfo().getCidrSignature()).getOrNull();
        final Map<String, Object> claims = formData.claims;
        final int expirationSeconds      = formData.expirationSeconds;

        if(expirationSeconds < 0) {

            return ExceptionMapperUtil.createResponse(new DotStateException("invalid expirationDays"), Response.Status.BAD_REQUEST);
        }

        ApiToken token = ApiToken.builder()
                .withAllowNetwork(netmask)
                .withIssueDate(new Date())
                .withClaims(claims)
                .withUser(forUser)
                .withRequestingIp(request.getRemoteAddr())
                .withRequestingUserId(requestingUser.getUserId())
                .withExpires( Date.from(Instant.now().plus(expirationSeconds, ChronoUnit.SECONDS)))
                .build();

        token = this.tokenApi.persistApiToken(token, requestingUser);
        final String jwt = this.tokenApi.getJWT(token, requestingUser);
        return Response.ok(new ResponseEntityView(map("token", token,"jwt", jwt), EMPTY_MAP)).build(); // 200
    }

    private User getUserById(ApiTokenForm formData, User requestingUser) {
        final User forUser = Try.of(()->
                    APILocator.getUserAPI().loadUserById(formData.userId, requestingUser, false ))
                .getOrNull();
        return forUser;
    }

    /**
     * Request a Token to a remote server
     * @param request
     * @param response
     * @param formData - json data, expecting
     * <pre>
     * {
     *    token: {
     *      netmask:'192.168.1.0/24',
     *      expirationDays:1000,
     *      userId:'dotcms.org.1'
     *      claims: {
     *          label: 'Push Publish'
     *      }
     *    },
     * 	  remote: {
     * 	    host: localhost,
     * 		port: 8090
     * 	  },
     * 	  auth: {
     * 	    login: admin@dotcms.com,
     * 		pasword: [password in Base64]
     * 	  }
     * }
     * </pre>
     * @return Response
     */
    @PUT
    @Path("/remote")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getRemoteToken(@Context final HttpServletRequest httpRequest,
                                        final RemoteAPITokenForm formData) {

        if (!Config.getBooleanProperty("ENABLE_PROXY_TOKEN_REQUESTS", true)) {
            final String message = "ENABLE_PROXY_TOKEN_REQUESTS is disabled, remote token is not allow";
            SecurityLogger.logInfo(ApiTokenResource.class, message);
            throw new ForbiddenException("ENABLE_PROXY_TOKEN_REQUESTS should be true");
        }

        final InitDataObject initDataObject = this.webResource.init(null, true, httpRequest, true, null);

        if (!initDataObject.getUser().isAdmin()) {
            SecurityLogger.logInfo(ApiTokenResource.class, "Should be Admin user to request a remote token");
            throw new ForbiddenException("Should be Admin user to request a remote token");
        }

        final String protocol = formData.protocol();
        final Client client = getRestClient();

        final String remoteURL = String.format("%s://%s:%d/api/v1/apitoken", protocol, formData.host(), formData.port());
        final WebTarget webTarget = client.target(remoteURL);

        String password = "";

        if (UtilMethods.isSet(formData.password())) {
            password = Base64.decodeAsString(formData.password());
        }

        try {
            final Response response = webTarget.request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Basic " + Base64.encodeAsString(formData.login() + ":" + password))
                    .post(Entity.entity(formData.getTokenInfo(), MediaType.APPLICATION_JSON));

            if (response.getStatus() != HttpStatus.SC_OK) {
                final String message = String.format("Status code : %s", response.getStatus());

                if (response.getStatus() == HttpStatus.SC_UNAUTHORIZED || response.getStatus() == HttpStatus.SC_FORBIDDEN ) {
                    SecurityLogger.logInfo(ApiTokenResource.class, message);
                } else {
                    Logger.error(ApiTokenResource.class, message);
                }
            }

            return Response
                    .status(response.getStatus())
                    .entity(response.readEntity(String.class))
                    .build();
        } catch (ProcessingException e){
            if (e.getCause().getClass() == UnknownHostException.class ||
                    e.getCause().getClass() == NoRouteToHostException.class ||
                    e.getCause().getClass() == ConnectException.class) {
                Logger.error(ApiTokenResource.class, String.format("Invalid server URL: %s", remoteURL));
                return Response.status(Response.Status.NOT_FOUND).build();
            } else {
                throw e;
            }
        }
    }

    private Client getRestClient() {
        if (null == this.restClient) {
            this.restClient = RestClientBuilder.newClient();
        }
        return this.restClient;
    }

    /**
     * Get a JWT issued from a token
     * @param request
     * @param response
     * @param tokenId
     * @return
     */
    @GET
    @Path("/{tokenId}/jwt")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getJwtFromApiToken(@Context final HttpServletRequest request,
                                             @Context final HttpServletResponse response,
                                             @PathParam("tokenId") final String tokenId) {

        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, "users");
        final User user = initDataObject.getUser();
        final Optional<ApiToken> optToken = tokenApi.findApiToken(tokenId);

        if (!optToken.isPresent()) {

            return ExceptionMapperUtil.createResponse(new DotStateException("token id not found"), Response.Status.NOT_FOUND);
        }

        final ApiToken token = optToken.get();
        if(token.isExpired() || token.isRevoked()) {

            return ExceptionMapperUtil.createResponse(new DotStateException("token not valid"), Response.Status.BAD_REQUEST);
        }

        SecurityLogger.logInfo(this.getClass(), "Revealing token to user: " + user.getUserId() + " from: " + request.getRemoteAddr() + " token:"  + token );
        final String jwt = tokenApi.getJWT(token, user);
        return Response.ok(new ResponseEntityView(map("jwt", jwt), EMPTY_MAP)).build(); // 200
    }



    @PUT
    @Path("/users/{userid}/revoke")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response revokeUserToken(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                         @PathParam("userid") final String userid) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource).rejectWhenNoUser(true)
                                                .requestAndResponse(request, response).requiredPortlet("users")
                                                .requiredFrontendUser(false)
                                                .requiredBackendUser(true).init();

        if (APILocator.getRoleAPI().doesUserHaveRole(initDataObject.getUser(), APILocator.getRoleAPI().loadCMSAdminRole())) {

            final User user      = initDataObject.getUser();
            final User userToken = APILocator.getUserAPI().loadUserById(userid);

            if (null != userToken) {

                SecurityLogger.logInfo(this.getClass(), "Revoking token " + userid + " from " + request.getRemoteAddr() + " ");
                userToken.setSkinId(UUIDGenerator.generateUuid()); // setting a new id will invalidate the token
                APILocator.getUserAPI().save(userToken, user, PageMode.get(request).respectAnonPerms); // this will invalidate
                return Response.ok(new ResponseEntityView(map("revoked", userid), EMPTY_MAP)).build(); // 200
            }
        } else {

            return ResponseUtil.INSTANCE.getErrorResponse(request, Response.Status.UNAUTHORIZED, initDataObject.getUser().getLocale(),
                    initDataObject.getUser().getUserId(),
                    "unauthorized to remove the token");
        }

        return ResponseUtil.INSTANCE.getErrorResponse(request, Response.Status.NOT_FOUND, initDataObject.getUser().getLocale(),
                initDataObject.getUser().getUserId(),
                "No token");
    }

    @PUT
    @Path("/users/revoke")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response revokeUsersToken(@Context final HttpServletRequest request,
                                           @Context final HttpServletResponse response) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource).rejectWhenNoUser(true)
                .requestAndResponse(request, response).requiredPortlet("users")
                .requiredFrontendUser(false)
                .requiredBackendUser(true).init();

        if (APILocator.getRoleAPI().doesUserHaveRole(initDataObject.getUser(), APILocator.getRoleAPI().loadCMSAdminRole())) {

            final User user                 = initDataObject.getUser();
            final List<User>   usersToken   = APILocator.getUserAPI().findAllUsers();
            final List<String> userTokenIds = new ArrayList<>();
            if (null != usersToken && !usersToken.isEmpty()) {

                for (final User userToken: usersToken) {

                    SecurityLogger.logInfo(this.getClass(), "Revoking token " + userToken.getUserId() + " from " + request.getRemoteAddr() + " ");
                    userToken.setSkinId(UUIDGenerator.generateUuid()); // setting a new id will invalidate the token
                    APILocator.getUserAPI().save(userToken, user, PageMode.get(request).respectAnonPerms); // this will invalidate
                    userTokenIds.add( userToken.getUserId());
                }

                return Response.ok(new ResponseEntityView(map("revoked", userTokenIds), EMPTY_MAP)).build(); // 200
            }
        } else {

            return ExceptionMapperUtil.createResponse(new DotStateException("unauthorized to remove the token"), Response.Status.UNAUTHORIZED);
        }

        return ExceptionMapperUtil.createResponse(new DotStateException("No token"), Response.Status.NOT_FOUND);
    }
}
