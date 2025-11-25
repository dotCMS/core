package com.dotcms.api;

import com.dotcms.api.provider.BuildVersionHeaders;
import com.dotcms.api.provider.DefaultResponseExceptionMapper;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.authentication.APITokenRequest;
import com.dotcms.model.authentication.TokenEntity;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;

/**
 *
 */
@Path("/v1/authentication")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tags(
  value = { @Tag(name = "Authentication")}
)
@RegisterClientHeaders(BuildVersionHeaders.class)
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
