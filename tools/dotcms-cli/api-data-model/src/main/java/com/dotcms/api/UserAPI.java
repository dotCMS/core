package com.dotcms.api;

import com.dotcms.api.provider.DotCMSClientHeaders;
import com.dotcms.model.user.User;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/v1/users")
@RegisterRestClient(configKey="legacy-api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterClientHeaders(DotCMSClientHeaders.class)
public interface UserAPI {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("current")
    User getCurrent();

}
