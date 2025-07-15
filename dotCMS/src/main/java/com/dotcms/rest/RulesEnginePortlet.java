package com.dotcms.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dotcms.rest.annotation.SwaggerCompliant;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import javax.servlet.ServletException;
import java.io.IOException;

@SwaggerCompliant(value = "Legacy and utility APIs", batch = 8)
@Tag(name = "Rules Engine")
@Path("/rulesengine")
public class RulesEnginePortlet extends BaseRestPortlet {

    @Operation(
        summary = "Load Rules Engine layout component",
        description = "Renders the Rules Engine portlet layout component for the dotCMS backend interface. Used for displaying the rules management UI within the admin interface."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Rules Engine layout loaded successfully",
                    content = @Content(mediaType = "text/html")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access Rules Engine",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error loading layout",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/layout/{params:.*}")
    @Produces("text/html")
    public Response getLayout(@Context HttpServletRequest request, 
                             @Context HttpServletResponse response, 
                             @Parameter(description = "Layout parameters (portletId/jspName)", required = true)
                             @PathParam("params") String params)
            throws DotDataException, ServletException, IOException, PortalException, SystemException {
        return super.getLayout(request, response, params);
    }
}