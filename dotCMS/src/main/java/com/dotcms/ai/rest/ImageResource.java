package com.dotcms.ai.rest;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.Marshaller;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.model.AIImageRequestDTO;
import com.dotcms.ai.api.ImageAPI;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import org.apache.commons.lang.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * The ImageResource class provides REST endpoints for interacting with the AI image generation service.
 * It includes methods for generating images based on a given prompt.
 */
@SwaggerCompliant(value = "Modern APIs and specialized services", batch = 7)
@Path("/v1/ai/image")
@Tag(name = "AI")
public class ImageResource {

    /**
     * Handles GET requests to test the response of the image service.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @return a Response object containing the test response
     */
    @Operation(
        summary = "Test AI image service",
        description = "Returns a test response to verify the AI image service is operational"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Test response returned successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(type = "object", description = "Simple key-value map indicating image service type")))
    })
    @GET
    @JSONP
    @Path("/test")
    @Produces({MediaType.APPLICATION_JSON})
    public final Response indexByInode(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response) {

        return Response.ok(Map.of(AiKeys.TYPE, AiKeys.IMAGE), MediaType.APPLICATION_JSON).build();
    }

    /**
     * Handles GET requests to generate images based on a given prompt.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param prompt the prompt to generate images from
     * @return a Response object containing the generated images
     * @throws IOException if an I/O error occurs
     */
    @Operation(
        summary = "Generate images using AI",
        description = "Generates AI-powered images based on the provided prompt using GET request with query parameter"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Images generated successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(type = "object", description = "AI image generation response containing image URLs and metadata"))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - prompt is required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error - app config missing or AI service error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @Path("/generate")
    @Produces({MediaType.APPLICATION_JSON})
    public final Response indexByInode(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @Parameter(description = "The prompt to generate images from", required = true)
                                       @QueryParam("prompt") final String prompt) throws IOException {
        final AIImageRequestDTO.Builder dto = new AIImageRequestDTO.Builder();
        dto.prompt(prompt);
        return handleImageRequest(request, response, dto.build());
    }

    /**
     * Handles POST requests to generate images based on a given AIImageRequestDTO.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param aiImageRequestDTO the AIImageRequestDTO containing the prompt and other parameters
     * @return a Response object containing the generated images
     * @throws IOException if an I/O error occurs
     */
    @Operation(
        summary = "Generate images using AI (POST)",
        description = "Generates AI-powered images based on the provided AIImageRequestDTO with detailed configuration options"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Images generated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "object", description = "AI image generation response containing image URLs and metadata"))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - prompt is required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error - app config missing or AI service error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @JSONP
    @Path("/generate")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response handleImageRequest(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @RequestBody(description = "Image generation request containing prompt and configuration options", 
                                                  required = true,
                                                    content = @Content(mediaType = "application/json",
                                                       schema = @Schema(type = "object", description = "JSON object with image generation form data including prompt, size, and style parameters")))

                                       final AIImageRequestDTO aiImageRequestDTO) throws IOException {
        final User user = new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(true)
                .init().getUser();

        Logger.debug(this.getClass(), String.format(
                "[DotAI API request] : IP address = %s, URL = %s, method = %s, parameters = %s, body = %s",
                request.getRemoteAddr(), request.getRequestURL().toString(), request.getMethod(),
                readParameters(request.getParameterMap()), Marshaller.marshal(aiImageRequestDTO)));

        final AppConfig config = ConfigService.INSTANCE.config(WebAPILocator.getHostWebAPI().getHost(request));
        if (UtilMethods.isEmpty(config.getApiKey())) {
            return Response
                    .status(Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(AiKeys.ERROR, "App Config missing"))
                    .build();
        }

        if (StringUtils.isBlank(aiImageRequestDTO.getPrompt())) {
            return Response
                    .status(Status.BAD_REQUEST)
                    .entity(Map.of(AiKeys.ERROR, "`prompt` is required"))
                    .build();
        }

        final ImageAPI service = APILocator.getDotAIAPI().getImageAPI(
                config,
                user,
                APILocator.getHostAPI(),
                APILocator.getTempFileAPI());
        final JSONObject resp = service.sendRequest(aiImageRequestDTO);

        return Response.ok(Marshaller.marshal(resp)).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    private String readParameters(final Map<String, String[]> parameterMap) {
        final StringBuilder sb = new StringBuilder("[");
        parameterMap.forEach((key, paramValues) -> {
            sb.append(key).append(":");
            Arrays.stream(paramValues).forEach(sb::append);
        });
        sb.append("]");

        return sb.toString();
    }

}
