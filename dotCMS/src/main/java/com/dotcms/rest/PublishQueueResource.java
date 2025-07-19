package com.dotcms.rest;

import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.json.JSONException;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.annotation.SwaggerCompliant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@SwaggerCompliant(value = "Publishing and content distribution APIs", batch = 5)
@Tag(name = "Publishing")
@Path("/v1/publishqueue")
public class PublishQueueResource {

    private final WebResource            webResource            = new WebResource();
    private final BundleAPI              bundleAPI              = APILocator.getBundleAPI();
    private final SystemMessageEventUtil systemMessageEventUtil = SystemMessageEventUtil.getInstance();


    /**
     * Deletes elements {@link com.dotcms.publisher.business.PublishQueueElement} from the Push Publish Queue
     *
     * @param request
     * @return
     * @throws DotStateException
     * @throws DotDataException
     * @throws JSONException
     */
    @Operation(
        summary = "Delete assets from publish queue",
        description = "Removes specified elements from the Push Publish Queue by their identifiers. Requires backend user authentication and publishing-queue portlet access."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Elements removed from publish queue successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid identifiers or form data",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access publishing-queue portlet",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error or publisher exception",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteAssetsByIdentifiers(@Context   final HttpServletRequest request,
            @Context   final HttpServletResponse response,
            @RequestBody(
                description = "Form containing identifiers of publish queue elements to delete", 
                required = true,
                content = @Content(schema = @Schema(implementation = DeletePPQueueElementsByIdentifierForm.class))
            ) final DeletePPQueueElementsByIdentifierForm  deletePPQueueElementsByIdentifierForm)
            throws DotPublisherException {

        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredPortlet("publishing-queue")
                .init();

        PublisherAPI.getInstance().deleteElementsFromPublishQueueTable(
                deletePPQueueElementsByIdentifierForm.getIdentifiers(),
                0);

        return Response.ok(new ResponseEntityStringView(
                "Requested elements were removed from the Push-Publish Queue")).build();
    }
}
