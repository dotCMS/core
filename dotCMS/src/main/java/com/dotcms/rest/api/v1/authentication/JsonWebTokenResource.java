package com.dotcms.rest.api.v1.authentication;

import static com.dotcms.util.CollectionsUtils.map;
import static java.util.Collections.EMPTY_MAP;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
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
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.RequiredLayoutException;
import com.liferay.portal.UserActiveException;
import com.liferay.portal.UserEmailAddressException;
import com.liferay.portal.UserPasswordException;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.language.LanguageWrapper;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.LocaleUtil;

import io.vavr.control.Try;


@Path("/v1/authentication")
public class JsonWebTokenResource implements Serializable {


    private final ApiTokenAPI tokenApi;
    private final WebResource webResource;

    /**
     * Default constructor.
     */
    public JsonWebTokenResource() {
        this(APILocator.getApiTokenAPI(), new WebResource());
    }

    @VisibleForTesting
    protected JsonWebTokenResource(final ApiTokenAPI tokenApi, final WebResource webResource) {

        this.tokenApi = tokenApi;
        this.webResource = webResource;
    }


    @GET
    @Path("/api-tokens/{userId}")
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
    @Path("/api-token/{tokenId}/revoke")
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
                return ExceptionMapperUtil.createResponse(new DotStateException("Token Expired"), Response.Status.NOT_FOUND);
            }
            
            
            SecurityLogger.logInfo(this.getClass(), "Revoking token " + token + " from " + request.getRemoteAddr() + " ");
            tokenApi.revokeToken(token, user);
            token = tokenApi.findApiToken(tokenId).get();
            return Response.ok(new ResponseEntityView(map("revoked", token), EMPTY_MAP)).build(); // 200
        }
        return ExceptionMapperUtil.createResponse(new DotStateException("No token"), Response.Status.NOT_FOUND);
    }

    @DELETE
    @Path("/api-token/{tokenId}/delete")
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
    @Path("/api-token/issue")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response issueApiToken(@Context final HttpServletRequest request,
                                   @Context final HttpServletResponse response,
                                   final Map<String,String> formData) {
        
        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
        final User requestingUser = initDataObject.getUser();
        final User forUser = Try.of(()->APILocator.getUserAPI().loadUserById(formData.get("userId"),requestingUser, false )).getOrNull();
        
        if(forUser ==null) {
            return ExceptionMapperUtil.createResponse(new DotStateException("No user found"), Response.Status.NOT_FOUND);
        }

        String netmaskStr = (formData.get("netmask")!=null && !"0.0.0.0/0".equals(formData.get("netmask"))) ? formData.get("netmask"):null;
        final String netmask = Try.of(()->new SubnetUtils(netmaskStr).getInfo().getCidrSignature()).getOrNull();
        
        final int expirationSeconds = Try.of(()->Integer.parseInt(formData.get("expirationSeconds"))).getOrElse(-1);
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
    @Path("/api-token/{tokenId}/jwt")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getApiToken(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @PathParam("tokenId") final String tokenId) {


        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, "users");
        final User user = initDataObject.getUser();

        Optional<ApiToken> optToken = tokenApi.findApiToken(tokenId);
        if (!optToken.isPresent()) {
            return Response.status(404).build();
        }
        ApiToken token = optToken.get();
        if(!token.isValid()) {
            return Response.status(500).build(); // 500
        }


        SecurityLogger.logInfo(this.getClass(), "Revealing token " + token + " to " + request.getRemoteAddr() + " ");
        final String jwt = tokenApi.getJWT(token, user);
        return Response.ok(new ResponseEntityView(map("jwt", jwt), EMPTY_MAP)).build(); // 200


        

    }
    



}
