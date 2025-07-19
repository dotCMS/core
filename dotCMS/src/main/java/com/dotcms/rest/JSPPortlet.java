package com.dotcms.rest;

import com.dotcms.rest.annotation.SwaggerCompliant;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <a href="JSPPortlet.java.html"><b><i>View Source</i></b></a> This is a simple
 * class that extends the RestPortlet that can be re-used in the portlet.xml. To
 * use and reuse this portlet, all you need to do is have a "render" jsp file
 * under a folder with the portlet id, e.g. The "render" jsp path would look
 * like: /WEB-INF/jsp/{portlet-id}/render.jsp If you extend this portlet, you
 * can use Jersey Annotations to produce or consume web services
 * 
 */
@SwaggerCompliant(value = "Legacy & Utility Resources - JSP portlet rendering mechanism for legacy portlet integration", batch = 8)
@Tag(name = "Portlets")
@Path("/portlet")
public class JSPPortlet extends BaseRestPortlet {

	@Operation(
		summary = "Render portlet layout (GET)",
		description = "Renders a JSP portlet layout using GET method. Looks for render.jsp under /WEB-INF/jsp/{portlet-id}/ directory. This is a legacy portlet rendering mechanism."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Portlet layout rendered successfully",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized access",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - insufficient permissions",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "404", 
					description = "Portlet JSP not found",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error during portlet rendering",
					content = @Content(mediaType = "text/html"))
	})
	@GET
	@Path("/{params:.*}")
	@Produces("text/html")
	public Response layoutGet(@Context HttpServletRequest request, @Context HttpServletResponse response, 
		@Parameter(description = "Portlet parameters and path information", required = true) @PathParam("params") String params) throws DotDataException,
			DotSecurityException, ServletException, IOException, DotRuntimeException, PortalException, SystemException {

		return super.getLayout(request, response, params);
	}

	@Operation(
		summary = "Render portlet layout (POST)",
		description = "Renders a JSP portlet layout using POST method. Handles form submissions and looks for render.jsp under /WEB-INF/jsp/{portlet-id}/ directory. This is a legacy portlet rendering mechanism."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Portlet layout rendered successfully",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized access",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - insufficient permissions",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "404", 
					description = "Portlet JSP not found",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error during portlet rendering",
					content = @Content(mediaType = "text/html"))
	})
	@POST
	@Path("/{params:.*}")
	@Produces("text/html")
	public Response layoutPost(@Context HttpServletRequest request, @Context HttpServletResponse response, 
		@Parameter(description = "Portlet parameters and path information", required = true) @PathParam("params") String params) throws DotDataException,
			DotSecurityException, ServletException, IOException, DotRuntimeException, PortalException, SystemException {

		return super.getLayout(request, response, params);
	}

	@Operation(
		summary = "Load JSP portlet layout component",
		description = "Renders the JSP portlet layout component for the dotCMS backend interface. This is the standard layout endpoint that follows the BaseRestPortlet pattern."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "JSP layout loaded successfully",
					content = @Content(mediaType = "text/html")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - user authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - insufficient permissions to access JSP portlet",
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