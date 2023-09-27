package com.dotcms.api;

import com.dotcms.api.provider.DefaultResponseExceptionMapper;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.authentication.APITokenRequest;
import com.dotcms.model.authentication.TokenEntity;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 *
 */
@Path("/v1/authentication")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tags(
  value = { @Tag(name = "Authentication")}
)
@RegisterProvider(DefaultResponseExceptionMapper.class)
public interface AuthenticationAPI {

    @POST
    @Operation(
            summary = "Get the API Token",
            description = "Returns a valid authorization token that serves as a pass for users."
    )
    @Produces({"application/json"})
    @Path("/api-token")
    ResponseEntityView<TokenEntity> getToken(APITokenRequest request);

}
