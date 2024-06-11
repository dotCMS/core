package com.dotcms.api;

import com.dotcms.api.provider.DefaultResponseExceptionMapper;
import com.dotcms.api.provider.DotCMSClientHeaders;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.content.CreateContentRequest;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;

@Path("/v1/workflow")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tags(
        value = {@Tag(name = "Workflow")}
)
@RegisterClientHeaders(DotCMSClientHeaders.class)
@RegisterProvider(DefaultResponseExceptionMapper.class)
public interface WorkflowAPI {

    @PUT
    @Path("/actions/default/fire/{systemAction}")
    @Operation(
            summary = " Fires default action"
    )
    ResponseEntityView<Map<String, Object>> create(
            @PathParam("systemAction") final String systemAction,
            @QueryParam("inode") final String inode,
            @QueryParam("identifier") final String identifier,
            @QueryParam("indexPolicy") final String indexPolicy,
            final CreateContentRequest request
    );

}
