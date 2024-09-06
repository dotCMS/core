package com.dotcms.api;

import com.dotcms.api.provider.DefaultResponseExceptionMapper;
import com.dotcms.api.provider.DotCMSClientHeaders;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.content.CreateContentRequest;
import java.util.Map;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
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
