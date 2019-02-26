package com.dotcms.rest.api.v1.authentication;

import static com.dotcms.util.CollectionsUtils.map;
import static java.util.Collections.EMPTY_MAP;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.DELETE;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.PUT;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;

import io.vavr.API;
import io.vavr.control.Try;

/**
 * Create a new Json Web Token
 * 
 * @author jsanca
 */
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
    public final Response getApiTokens(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @PathParam("userId") final String userId) {

        final boolean showRevoked = Try.of(() -> Boolean.valueOf(request.getParameter("showRevoked"))).getOrElse(Boolean.FALSE);

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
            if (checkPerms(user, token)) {
                SecurityLogger.logInfo(this.getClass(), "Revoking token " + token + " from " + request.getRemoteAddr() + " ");
                tokenApi.revokeToken(token);
                token = tokenApi.findApiToken(tokenId).get();
            }
            return Response.ok(new ResponseEntityView(map("revoked", token), EMPTY_MAP)).build(); // 200
        }
        return Response.status(404).build();
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

            if (checkPerms(user, token)) {
                SecurityLogger.logInfo(this.getClass(), "Deleting token " + token + " from " + request.getRemoteAddr() + " ");
                tokenApi.deleteToken(token);
                return Response.ok(new ResponseEntityView(map("deleted", token), EMPTY_MAP)).build(); // 200

            }
            return Response.status(403).build(); // 403

        }


        return Response.status(404).build();

    }

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

        if (checkPerms(user, token)) {
            SecurityLogger.logInfo(this.getClass(), "Revealing token " + token + " to " + request.getRemoteAddr() + " ");
            final String jwt = tokenApi.getJWT(token);
            return Response.ok(new ResponseEntityView(map("jwt", jwt), EMPTY_MAP)).build(); // 200

        }
        
        return Response.status(403).build(); // 403
        

    }
    
    private boolean checkPerms(final User user, final ApiToken token) {
        return Try.of(() -> (APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())
                || user.getUserId().equals(token.getUserId()) || user.getUserId().equals(token.requestingUserId))).getOrElse(false);


    }


}
