package com.dotcms.api;

import com.dotcms.model.authentication.APITokenRequest;
import com.dotcms.model.authentication.APITokenResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/v1/authentication")
@RegisterRestClient(configKey="legacy-api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AuthenticationAPI {

    @POST
    @Path("/api-token")
    APITokenResponse getToken(APITokenRequest request);

}
