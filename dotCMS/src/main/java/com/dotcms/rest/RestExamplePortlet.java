package com.dotcms.rest;

import com.dotcms.rest.annotation.SwaggerCompliant;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerCompliant(value = "Legacy & Utility Resources - Example REST endpoint for testing and demonstration purposes", batch = 8)
@Tag(name = "Administration")
@Path("/restexample")
public class RestExamplePortlet extends BaseRestPortlet {

        @Operation(
                summary = "Load test JSON",
                description = "Returns a simple test JSON response. This is an example endpoint for testing REST functionality."
        )
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", 
                            description = "Test JSON returned successfully",
                            content = @Content(mediaType = "application/json",
                                              schema = @Schema(type = "object", description = "Test JSON object with a simple test property"))),
                @ApiResponse(responseCode = "500", 
                            description = "Internal server error",
                            content = @Content(mediaType = "application/json"))
        })
        @GET
        @Path("/test/{params:.*}")
        @Produces("application/json")
        public Response loadJson(@Context HttpServletRequest request,
                        @Parameter(description = "URL parameters for the test request", required = true) @PathParam("params") String params) throws DotStateException,
                        DotDataException, DotSecurityException {

                CacheControl cc = new CacheControl();
                cc.setNoCache(true);

                ResponseBuilder builder = Response
                                .ok("{\"test\":\"test\"}", "application/json");
                return builder.cacheControl(cc).build();

        }

        @Operation(
                summary = "Load RestExample layout component",
                description = "Renders the RestExample portlet layout component for the dotCMS backend interface. This is the standard layout endpoint for the example REST portlet."
        )
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", 
                            description = "RestExample layout loaded successfully",
                            content = @Content(mediaType = "text/html")),
                @ApiResponse(responseCode = "401", 
                            description = "Unauthorized - user authentication required",
                            content = @Content(mediaType = "application/json")),
                @ApiResponse(responseCode = "403", 
                            description = "Forbidden - insufficient permissions to access RestExample",
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