package com.dotcms.api;

import com.dotcms.api.provider.DefaultResponseExceptionMapper;
import com.dotcms.api.provider.DotCMSClientHeaders;
import com.dotcms.model.user.User;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Entry point to dotCMS User Rest API
 */
@Path("/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tags(
   value = { @Tag(name = "User")}
)
@RegisterClientHeaders(DotCMSClientHeaders.class)
@RegisterProvider(DefaultResponseExceptionMapper.class)
public interface UserAPI {

    /**
     * Get current user Operation
     * @return Current user
     */
    @GET
    @Operation(
            summary = "Returns Current logged in user.",
            description = "Once a user has logged a token is given to it. "
                        + "Based on the authorization token this method will tell you who is currently logged in. "
    )
    @Produces(MediaType.APPLICATION_JSON)
    @Path("current")
    User getCurrent();

}
