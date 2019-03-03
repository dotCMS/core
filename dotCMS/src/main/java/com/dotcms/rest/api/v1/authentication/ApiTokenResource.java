package com.dotcms.rest.api.v1.authentication;

import static com.dotcms.util.CollectionsUtils.map;
import static java.util.Collections.EMPTY_MAP;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.Consumes;
import com.dotcms.repackage.javax.ws.rs.DELETE;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.PUT;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.QueryParam;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
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

        this.tokenApi = tokenApi;
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


        List<ApiToken> tokens = tokenApi.findApiTokensByUserId(userId, showRevoked, initDataObject.getUser());


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
        final User user = initDataObject.getUser();

        Optional<ApiToken> optToken = tokenApi.findApiToken(tokenId);
        if (optToken.isPresent()) {
            ApiToken token = optToken.get();
            
            if(token.isExpired()) {
                return ExceptionMapperUtil.createResponse(new DotStateException("Token Expired"), Response.Status.NOT_ACCEPTABLE);
            }
            
            
            SecurityLogger.logInfo(this.getClass(), "Revoking token " + token + " from " + request.getRemoteAddr() + " ");
            tokenApi.revokeToken(token, user);
            token = tokenApi.findApiToken(tokenId).get();
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
        final User user = initDataObject.getUser();

        Optional<ApiToken> optToken = tokenApi.findApiToken(tokenId);
        if (optToken.isPresent()) {
            ApiToken token = optToken.get();

            
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
     * @return
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
        
        if(forUser ==null) {
            return ExceptionMapperUtil.createResponse(new DotStateException("No user found"), Response.Status.NOT_FOUND);
        }

        String netmaskStr = (formData.network!=null && !"0.0.0.0/0".equals(formData.network)) ? formData.network:null;
        final String netmask = Try.of(()->new SubnetUtils(netmaskStr).getInfo().getCidrSignature()).getOrNull();
        
        final int expirationSeconds = formData.expirationSeconds;
        if(expirationSeconds<0) {
            return ExceptionMapperUtil.createResponse(new DotStateException("invalid expirationDays"), Response.Status.NOT_FOUND);
        }

        ApiToken token = ApiToken.builder()
                .withAllowNetwork(netmask)
                .withIssueDate(new Date())
                .withUser(forUser)
                .withRequestingIp(request.getRemoteAddr())
                .withRequestingUserId(requestingUser.getUserId())
                .withExpires( Date.from(Instant.now().plus(expirationSeconds, ChronoUnit.SECONDS)))
                .build();
        token = tokenApi.persistApiToken(token, requestingUser);
        final String jwt = tokenApi.getJWT(token, requestingUser);
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
    public final Response getJwtFromApiToken(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @PathParam("tokenId") final String tokenId) {


        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, "users");
        final User user = initDataObject.getUser();

        Optional<ApiToken> optToken = tokenApi.findApiToken(tokenId);
        if (!optToken.isPresent()) {
            return ExceptionMapperUtil.createResponse(new DotStateException("token id not found"), Response.Status.NOT_FOUND);
        }
        ApiToken token = optToken.get();
        if(token.isExpired() || token.isRevoked()) {
            return ExceptionMapperUtil.createResponse(new DotStateException("token not valid"), Response.Status.BAD_REQUEST);
        }


        SecurityLogger.logInfo(this.getClass(), "Revealing token to user: " + user.getUserId() + " from: " + request.getRemoteAddr() + " token:"  + token );
        final String jwt = tokenApi.getJWT(token, user);
        return Response.ok(new ResponseEntityView(map("jwt", jwt), EMPTY_MAP)).build(); // 200


        

    }
    



}
