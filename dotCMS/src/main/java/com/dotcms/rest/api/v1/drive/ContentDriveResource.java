package com.dotcms.rest.api.v1.drive;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;

/**
 * Content Drive Resource
 * <p>This resource provides drive-like functionality for browsing and searching content assets.</p>
 * <p>Enables navigation through content structures with filtering and search capabilities.</p>
 */
@Path("/v1/drive")
public class ContentDriveResource {

    private final ContentDriveHelper helper = ContentDriveHelper.newInstance();

    /**
     * Search content assets using drive functionality
     */
    @Operation(
        summary = "Search content assets with drive functionality (Internal API)",
        description = "⚠️ **INTERNAL API - NOT FOR EXTERNAL USE** ⚠️\n\n" +
                     "This endpoint is designed exclusively for dotCMS internal operations and UI components. " +
                     "It is not intended for external integrations or third-party applications.\n\n" +
                     "**Important Notice:**\n" +
                     "• This API may change without notice in future versions\n" +
                     "• No backward compatibility guarantees\n" +
                     "• dotCMS does not provide support tickets for this endpoint\n" +
                     "• Use at your own risk for custom implementations\n\n" +
                     "**Functionality:** Search and browse content assets using drive-like functionality " +
                     "with filtering, navigation, and content type filtering capabilities.",
        tags = {"Internal APIs"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "Drive search results retrieved successfully",
                    content = @Content(mediaType = "application/json",
                        schema = @Schema(type = "object",
                        description = "Drive search response containing filtered assets, folders, and navigation metadata with content type filtering")
                    )
        ),
        @ApiResponse(responseCode = "401",
                    description = "Unauthorized access",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404",
                    description = "Not found. Site or folder not found at the specified path",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400",
                    description = "Invalid request parameters or malformed drive request",
                    content = @Content(mediaType = "application/json"))
    })
    @Hidden
    @Path("/search")
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response search(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            DriveRequestForm form
    ) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        final User user = initDataObject.getUser();
        Logger.debug(this,
                String.format("User [%s] is requesting content drive search for path [%s]",
                        user.getUserId(), form.assetPath()));
        return Response.ok(new ResponseEntityView<>(helper.driveSearch(form, user))).build();
    }

}
