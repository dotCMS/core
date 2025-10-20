package com.dotcms.ai.rest;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AIModels;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.model.SimpleModel;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.ai.util.LineReadingOutputStream;
import com.dotcms.rest.WebResource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The CompletionsResource class provides REST endpoints for interacting with the AI completions service.
 * It includes methods for generating completions based on a given prompt.
 */
@Path("/v1/ai/completions")
@Tag(name = "AI", description = "AI-powered content generation and analysis endpoints")
public class CompletionsResource {

    /**
     * Handles POST requests to generate completions based on a given prompt.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param formIn the form data containing the prompt
     * @return a Response object containing the generated completions
     */
    @POST
    @JSONP
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    @Operation(
        operationId = "summarizeFromContent",
        summary = "Generate AI completions from content",
        description = "Creates AI-powered content summaries and completions based on provided prompts. Supports both streaming and non-streaming responses.",
        tags = {"AI"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Completion generated successfully",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Object.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Missing or invalid prompt"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public final Response summarizeFromContent(@Context final HttpServletRequest request,
                                               @Context final HttpServletResponse response,
                                               @RequestBody(description = "Completion form with prompt and configuration", 
                                                          content = @Content(schema = @Schema(implementation = CompletionsForm.class)))
                                               final CompletionsForm formIn) {
        final CompletionsForm resolvedForm = resolveForm(request, response, formIn);
        return getResponse(
                request,
                response,
                formIn,
                () -> APILocator.getDotAIAPI().getCompletionsAPI().summarize(resolvedForm),
                output -> APILocator.getDotAIAPI()
                        .getCompletionsAPI()
                        .summarizeStream(resolvedForm, new LineReadingOutputStream(output)));
    }

    /**
     * Handles POST requests to generate completions based on a raw prompt.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param formIn the form data containing the prompt
     * @return a Response object containing the generated completions
     */
    @Path("/rawPrompt")
    @POST
    @JSONP
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    @Operation(
        operationId = "rawPrompt",
        summary = "Generate AI completions from raw prompt",
        description = "Processes raw prompts directly through the AI service without content preprocessing. Supports both streaming and non-streaming responses.",
        tags = {"AI"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Raw completion generated successfully",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Object.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Missing or invalid prompt"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public final Response rawPrompt(@Context final HttpServletRequest request,
                                    @Context final HttpServletResponse response,
                                    @RequestBody(description = "Completion form with raw prompt and configuration", 
                                               content = @Content(schema = @Schema(implementation = CompletionsForm.class)))
                                    final CompletionsForm formIn) {
        final CompletionsForm resolvedForm = resolveForm(request, response, formIn);
        return getResponse(
                request,
                response,
                formIn,
                () -> APILocator.getDotAIAPI().getCompletionsAPI().raw(resolvedForm),
                output -> APILocator.getDotAIAPI()
                        .getCompletionsAPI()
                        .rawStream(resolvedForm, new LineReadingOutputStream(output)));
    }

    /**
     * Handles GET requests to retrieve the configuration of the completions service.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @return a Response object containing the configuration of the completions service
     */
    @GET
    @JSONP
    @Path("/config")
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    @Operation(
        operationId = "getAiConfig",
        summary = "Get AI service configuration",
        description = "Retrieves the current AI service configuration including available models, API settings, and host-specific configurations.",
        tags = {"AI"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Configuration retrieved successfully",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public final Response getConfig(@Context final HttpServletRequest request,
                                    @Context final HttpServletResponse response) {
        // get user if we have one (this allows anon)
        new WebResource
                .InitBuilder(request, response)
                .requiredBackendUser(true)
                .init()
                .getUser();
        final Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        final AppConfig appConfig = ConfigService.INSTANCE.config(host);

        final Map<String, Object> map = new HashMap<>();
        map.put(AiKeys.CONFIG_HOST, host.getHostname() + " (falls back to system host)");
        for (final AppKeys config : AppKeys.values()) {
            map.put(config.key, appConfig.getConfig(config));
        }

        final String apiKey = UtilMethods.isSet(appConfig.getApiKey()) ? "*****" : "NOT SET";
        map.put(AppKeys.API_KEY.key, apiKey);

        final List<SimpleModel> models = AIModels.get().getAvailableModels();
        map.put(AiKeys.AVAILABLE_MODELS, models);

        return Response.ok(map).build();
    }

    private static Response badRequestResponse() {
        return Response.status(Response.Status.BAD_REQUEST).entity(Map.of(AiKeys.ERROR, "query required")).build();
    }

    private static CompletionsForm resolveForm(final HttpServletRequest request,
                                               final HttpServletResponse response,
                                               final CompletionsForm formIn) {
        // get user if we have one (this allows anon)
        final User user = new WebResource
                .InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(true)
                .init()
                .getUser();
        final Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        return withUserId(
                !user.isAdmin()
                    ? CompletionsForm
                        .copy(formIn)
                        .model(ConfigService.INSTANCE.config(host).getModel().getCurrentModel())
                        .build()
                    : formIn,
                user);
    }

    private static CompletionsForm withUserId(final CompletionsForm completionsForm, final User user) {
        return CompletionsForm.copy(completionsForm).user(user).build();
    }

    private static Response getResponse(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final CompletionsForm formIn,
                                        final Supplier<JSONObject> noStream,
                                        final Consumer<OutputStream> outputStream) {
        if (StringUtils.isBlank(formIn.prompt)) {
            return badRequestResponse();
        }

        final long startTime = System.currentTimeMillis();
        final CompletionsForm resolvedForm = resolveForm(request, response, formIn);

        if (resolvedForm.stream) {
            final StreamingOutput streaming = output -> {
                outputStream.accept(output);
                output.flush();
                output.close();
            };
            return Response.ok(streaming).build();
        }

        final JSONObject jsonResponse = noStream.get();
        jsonResponse.put(AiKeys.TOTAL_TIME, System.currentTimeMillis() - startTime + "ms");

        return Response.ok(jsonResponse.toString(), MediaType.APPLICATION_JSON).build();
    }

}
