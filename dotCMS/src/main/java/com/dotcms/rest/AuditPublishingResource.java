package com.dotcms.rest;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.rest.annotation.SwaggerCompliant;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.dotmarketing.util.Logger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerCompliant(value = "Publishing and content distribution APIs", batch = 5)
@Path("/auditPublishing")
@Tag(name = "Publishing")
public class AuditPublishingResource {
    private PublishAuditAPI auditAPI = PublishAuditAPI.getInstance();

    @Operation(
        summary = "Get publish audit status",
        description = "Retrieves the publishing audit status for a specific bundle by its ID. Returns the serialized status information in XML format."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Publish audit status retrieved successfully",
                    content = @Content(mediaType = "text/xml")),
        @ApiResponse(responseCode = "404", 
                    description = "Bundle not found or no audit status available",
                    content = @Content(mediaType = "text/xml")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error retrieving audit status",
                    content = @Content(mediaType = "text/xml"))
    })
    @GET
    @Path("/get/{bundleId:.*}")
    @Produces(MediaType.TEXT_XML)
    public Response get(@Parameter(description = "Bundle ID to get audit status for", required = true) @PathParam("bundleId") String bundleId) {
        PublishAuditStatus status = null;

        try {
            status = auditAPI.getPublishAuditStatus(bundleId);

            if(status != null)
                return Response.ok( status.getStatusPojo().getSerialized()).build();
        } catch (DotPublisherException e) {
            Logger.warn(this, "error trying to get status for bundle "+bundleId,e);
        }

        return Response.status(404).build();
    }


}