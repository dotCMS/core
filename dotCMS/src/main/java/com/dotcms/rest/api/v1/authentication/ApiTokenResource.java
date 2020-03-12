package com.dotcms.rest.api.v1.authentication;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDGenerator;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
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
     * Issue a new APIToken
     * @param request
     * @param response
     * @param formData - json data, expecting {netmask:'192.168.1.0/24', expirationDays:1000, userId:'dotcms.org.1'}
     * @return Response
     */
    @POST
    @Path("/")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response issueApiToken(@Context final HttpServletRequest request,
                                   @Context final HttpServletResponse response,
                                   final ApiTokenForm formData) {
        
        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
        final User requestingUser = initDataObject.getUser();
        final User forUser = Try.of(()->APILocator.getUserAPI().loadUserById(formData.userId,requestingUser, false )).getOrNull();
        
        if(forUser == null) {

            return ExceptionMapperUtil.createResponse(new DotStateException("No user found"), Response.Status.NOT_FOUND);
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

        final InitDataObject initDataObject = new WebResource.InitBuilder().rejectWhenNoUser(true)
                                                .requestAndResponse(request, response).requiredPortlet("users")
                                                .requiredBackendUser(true).init();

        if (APILocator.getRoleAPI().doesUserHaveRole(initDataObject.getUser(), APILocator.getRoleAPI().loadCMSAdminRole())) {

            final User user = initDataObject.getUser();
            final User userToken = APILocator.getUserAPI().loadUserById(userid);

            if (null != userToken) {

                SecurityLogger.logInfo(this.getClass(), "Revoking token " + userid + " from " + request.getRemoteAddr() + " ");
                userToken.setSkinId(UUIDGenerator.generateUuid()); // setting a new id will invalidate the token
                APILocator.getUserAPI().save(userToken, user, PageMode.get(request).respectAnonPerms); // this will invalidate
                return Response.ok(new ResponseEntityView(map("revoked", userid), EMPTY_MAP)).build(); // 200
            }
        } else {

            return ExceptionMapperUtil.createResponse(new DotStateException("unauthorized to remove the token"), Response.Status.UNAUTHORIZED);
        }

        return ExceptionMapperUtil.createResponse(new DotStateException("No token"), Response.Status.NOT_FOUND);
    }

    @PUT
    @Path("/users/revoke")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response revokeUsersToken(@Context final HttpServletRequest request,
                                           @Context final HttpServletResponse response) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder().rejectWhenNoUser(true)
                .requestAndResponse(request, response).requiredPortlet("users")
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
