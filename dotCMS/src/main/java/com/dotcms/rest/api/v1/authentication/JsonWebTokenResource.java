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
import com.dotmarketing.util.SecurityLogger;

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
        this(APILocator.getApiTokenAPI(),new WebResource());
    }

    @VisibleForTesting
    protected JsonWebTokenResource(final ApiTokenAPI tokenApi,final WebResource webResource) {

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
            @PathParam("userId") final String userId) {

        final boolean showRevoked = Try.of(()-> Boolean.valueOf(request.getParameter("showRevoked"))).getOrElse(Boolean.FALSE);
        
        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, "users");

        
        List<ApiToken> tokens = tokenApi.findApiTokensByUserId(userId, showRevoked, initDataObject.getUser());
        
        
        return Response.ok(new ResponseEntityView(map("tokens",
                tokens), EMPTY_MAP)).build(); // 200

    } 

    @PUT
    @Path("/api-token/{tokenId}/revoke")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response revokeApiTokens(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @PathParam("tokenId") final String tokenId) {


        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, "users");

        
        
        
        Optional<ApiToken> token = tokenApi.findApiToken(tokenId);
        if(token.isPresent()) {
            SecurityLogger.logInfo(this.getClass(), "Revoking token "+token+" from " + request.getRemoteAddr() + " ");
            tokenApi.revokeToken(token.get());
        }
        token = tokenApi.findApiToken(tokenId);
        
        
        return Response.ok(new ResponseEntityView(map("revoked",
                token), EMPTY_MAP)).build(); // 200
        
        

    } 

}
