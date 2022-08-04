package com.dotcms.api.contenttype;

import com.dotcms.api.provider.DefaultResponseExceptionMapper;
import com.dotcms.api.provider.DotCMSClientHeaders;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.contenttype.ContentType;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Entry point to dotCMS ContentType Rest API
 */
@Path("/v1/contenttype")
@RegisterRestClient(configKey = "legacy-api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tags(
        value = { @Tag(name = "Content-Type")}
)
@RegisterClientHeaders(DotCMSClientHeaders.class)
@RegisterProvider(DefaultResponseExceptionMapper.class)
public interface ContentTypeAPI {

    @GET
    @Operation(
            summary = "Get a list of Content-types for a given set of param"
    )
    ResponseEntityView<List<ContentType>> getContentTypes(@QueryParam("filter") String filter,
            @QueryParam("page") Integer page,
            @QueryParam("perPage") Integer perPage,
            @QueryParam("orderBy") String orderBy,
            @QueryParam("direction") String direction,
            @QueryParam("type") String type,
            @QueryParam("host") String host);

}
