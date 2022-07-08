package com.dotcms.api;

import com.dotcms.api.provider.DotCMSClientHeaders;
import com.dotcms.model.contenttype.GetContentTypesAbstractResponseEntityView;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/v1/contenttype")
@RegisterRestClient(configKey = "legacy-api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterClientHeaders(DotCMSClientHeaders.class)
public interface ContentTypeAPI {

    @GET
    GetContentTypesAbstractResponseEntityView getContentTypes(@QueryParam("filter") String filter,
            @QueryParam("page") Integer page,
            @QueryParam("perPage") Integer perPage,
            @QueryParam("orderBy") String orderBy,
            @QueryParam("direction") String direction,
            @QueryParam("type") String type,
            @QueryParam("host") String host);

}
