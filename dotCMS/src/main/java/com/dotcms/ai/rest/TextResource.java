package com.dotcms.ai.rest;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Map;

/**
 * The TextResource class provides REST endpoints for interacting with the AI text generation service.
 * It includes methods for generating text based on a given prompt.
 */
@SwaggerCompliant(value = "Modern APIs and specialized services", batch = 7)
@Path("/v1/ai/text")
@Tag(name = "AI")
public class TextResource {

    /**
     * Handles GET requests to generate text based on a given prompt.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param prompt the prompt to generate text from
     * @return a Response object containing the generated text
     * @throws IOException if an I/O error occurs
     */
    @Operation(
        summary = "Generate text using AI",
        description = "Generates AI-powered text content based on the provided prompt using GET request with query parameter"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Text generated successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(type = "object", description = "AI text generation response with generated content and metadata"))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - prompt is required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @Path("/generate")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response doGet(@Context final HttpServletRequest request,
                          @Context final HttpServletResponse response,
                          @Parameter(description = "The text prompt to generate AI content from", required = true)
                          @QueryParam("prompt") String prompt) throws IOException {

        return doPost(request, response, new CompletionsForm.Builder().prompt(prompt).build());
    }

    /**
     * Handles POST requests to generate text based on a given prompt.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param form the form data containing the prompt
     * @return a Response object containing the generated text
     * @throws IOException if an I/O error occurs
     */
    @Operation(
        summary = "Generate text using AI (POST)",
        description = "Generates AI-powered text content based on the provided prompt and configuration using POST request with form data"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Text generated successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(type = "object", description = "AI text generation response with generated content and metadata"))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - prompt is required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @Path("/generate")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doPost(@Context final HttpServletRequest request,
                           @Context final HttpServletResponse response,
                           @RequestBody(description = "Form data containing the prompt and AI configuration options", 
                                      required = true,
                                      content = @Content(schema = @Schema(implementation = CompletionsForm.class)))
                           final CompletionsForm form) throws IOException {

        final User user = new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(true)
                .init()
                .getUser();
        final CompletionsForm formIn = CompletionsForm.copy(form).user(user).build();

        if (UtilMethods.isEmpty(formIn.prompt)) {
            return Response
                    .status(Status.BAD_REQUEST)
                    .entity(Map.of(AiKeys.ERROR, "`prompt` is required"))
                    .build();
        }

        final AppConfig config = ConfigService.INSTANCE.config(WebAPILocator.getHostWebAPI().getHost(request));

        return Response.ok(
                APILocator.getDotAIAPI()
                        .getCompletionsAPI()
                        .raw(generateRequest(formIn, config), user.getUserId())
                        .toString())
                .build();
    }

    /**
     * Generates a request for the AI text generation service based on the given form data and configuration.
     *
     * @param form the form data containing the prompt
     * @param config the configuration for the AI text generation service
     * @return a JSONObject representing the request
     */
    private JSONObject generateRequest(final CompletionsForm form, final AppConfig config) {
        final String model = form.model;
        final float temperature = form.temperature;
        final JSONObject request = new JSONObject();
        final JSONArray messages = new JSONArray();

        final String systemPrompt = UtilMethods.isSet(config.getRolePrompt()) ? config.getRolePrompt() : null;
        if (UtilMethods.isSet(systemPrompt)) {
            messages.add(Map.of(AiKeys.ROLE, AiKeys.SYSTEM, AiKeys.CONTENT, systemPrompt));
        }
        messages.add(Map.of(AiKeys.ROLE, AiKeys.USER, AiKeys.CONTENT, form.prompt));

        request.put(AiKeys.MODEL, model);
        request.put(AiKeys.TEMPERATURE, temperature);
        request.put(AiKeys.MESSAGES, messages);

        return request;
    }

}
