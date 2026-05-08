package com.dotcms.ai.rest;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.app.ProviderConfigMerger;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.ai.util.LineReadingOutputStream;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import io.vavr.Tuple;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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

    private static final ObjectMapper REDACTION_MAPPER = DotObjectMapperProvider.createDefaultMapper();

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
        description = "Retrieves the current AI service configuration. " +
            "Accepts an optional siteId query parameter (site identifier / UUID, or the literal SYSTEM_HOST). " +
            "Hostname values are not supported — use the site identifier. " +
            "When siteId is omitted or cannot be resolved, falls back to the site derived from the HTTP Host header.",
        tags = {"AI"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Configuration retrieved successfully",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks permission for the requested site"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public final Response getConfig(@Context final HttpServletRequest request,
                                    @Context final HttpServletResponse response,
                                    @QueryParam("siteId") final String siteId) {
        final User user = new WebResource
                .InitBuilder(request, response)
                .requiredBackendUser(true)
                .init()
                .getUser();
        final Host host;
        try {
            host = resolveHost(siteId, request, user);
        } catch (final DotSecurityException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of(AiKeys.ERROR, "Access denied to site: " + sanitize(siteId)))
                    .build();
        }
        final AppConfig appConfig = ConfigService.INSTANCE.config(host);

        final Map<String, Object> map = new HashMap<>();
        map.put(AiKeys.CONFIG_HOST, host.getHostname() + " (falls back to system host)");

        final String providerConfig = appConfig.getProviderConfig();
        if (StringUtils.isNotBlank(providerConfig)) {
            map.put(AppKeys.PROVIDER_CONFIG.key, redactCredentials(providerConfig));
        }

        final Map<String, String> settings = new LinkedHashMap<>();
        Arrays.stream(AppKeys.values())
                .filter(k -> k.settingsKey != null)
                .forEach(k -> settings.put(k.settingsKey, appConfig.getConfig(k)));
        map.put("settings", settings);

        return Response.ok(map).build();
    }

    @PUT
    @JSONP
    @Path("/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
        operationId = "saveAiConfig",
        summary = "Save AI provider configuration",
        description = "Saves the providerConfig JSON for the target site. " +
            "Accepts an optional siteId query parameter (site identifier / UUID, or the literal SYSTEM_HOST). " +
            "Hostname values are not supported — use the site identifier. " +
            "When siteId is omitted, saves to the site derived from the HTTP Host header. " +
            "An unresolvable siteId returns 400. " +
            "Credential fields set to \"*****\" are preserved from the existing stored configuration. Requires CMS admin.",
        tags = {"AI"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Configuration saved successfully"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid request body, or site not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CMS admin or access denied to site"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public Response saveConfig(@Context final HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               @QueryParam("siteId") final String siteId,
                               final String body) {
        final User user = new WebResource
                .InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .init()
                .getUser();

        if (!user.isAdmin()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of(AiKeys.ERROR, "Only CMS admins can update the AI configuration"))
                    .build();
        }

        if (StringUtils.isBlank(body)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(AiKeys.ERROR, "Request body is required"))
                    .build();
        }

        try {
            final Host host = resolveHostStrict(siteId, request, user);
            if (host == null) {
                final String msg = StringUtils.isNotBlank(siteId)
                        ? "Site not found: " + sanitize(siteId)
                        : "Could not resolve current site from request";
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(AiKeys.ERROR, msg))
                        .build();
            }
            final AppConfig current = ConfigService.INSTANCE.config(host);

            final String merged = ProviderConfigMerger.containsMasked(body)
                    ? ProviderConfigMerger.merge(body, current.getProviderConfig())
                    : body;

            if (ProviderConfigMerger.containsMaskedCredential(merged)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(AiKeys.ERROR,
                                "Credential fields still contain placeholder values — provide real credentials or load the existing configuration first"))
                        .build();
            }

            try {
                final JsonNode mergedRoot = REDACTION_MAPPER.readTree(merged);
                if (!mergedRoot.isObject()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of(AiKeys.ERROR, "Request body must be a JSON object"))
                            .build();
                }
            } catch (final Exception parseEx) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(AiKeys.ERROR, "Invalid JSON in request body"))
                        .build();
            }

            final Secret secret = Secret.builder()
                    .withValue(merged)
                    .withType(Type.STRING)
                    .withHidden(true)
                    .build();

            APILocator.getAppsAPI().saveSecret(
                    AppKeys.APP_KEY,
                    Tuple.of(AppKeys.PROVIDER_CONFIG.key, secret),
                    host,
                    user);

            return Response.ok(Map.of(
                    AppKeys.PROVIDER_CONFIG.key, redactCredentials(merged),
                    AiKeys.CONFIG_HOST, host.getHostname()
            )).build();

        } catch (final DotSecurityException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of(AiKeys.ERROR, "Access denied to site: " + sanitize(siteId)))
                    .build();
        } catch (final Exception e) {
            Logger.error(CompletionsResource.class, "Failed to save AI config: " + e.getMessage(), e);
            return Response.serverError()
                    .entity(Map.of(AiKeys.ERROR, "Failed to save configuration"))
                    .build();
        }
    }

    private static String redactCredentials(final String json) {
        try {
            final JsonNode root = REDACTION_MAPPER.readTree(json);
            redactNode(root);
            return REDACTION_MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            Logger.warn(CompletionsResource.class, "Failed to parse providerConfig for redaction: " + e.getMessage(), e);
            return "[CONFIG PRESENT — REDACTION FAILED]";
        }
    }

    private static void redactNode(final JsonNode node) {
        if (node.isObject()) {
            final ObjectNode obj = (ObjectNode) node;
            final Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> field = fields.next();
                if (ProviderConfigMerger.CREDENTIAL_FIELDS.contains(field.getKey())) {
                    obj.put(field.getKey(), "*****");
                } else {
                    redactNode(field.getValue());
                }
            }
        } else if (node.isArray()) {
            node.forEach(CompletionsResource::redactNode);
        }
    }

    /**
     * Resolves a host from {@code siteId} and falls back to the HTTP host on failure.
     * Throws {@link DotSecurityException} when the user lacks permission for the requested site.
     * Falls back to the HTTP-derived host when {@code siteId} is blank or not found.
     */
    private static Host resolveHost(final String siteId,
                                    final HttpServletRequest request,
                                    final User user) throws DotSecurityException {
        if (StringUtils.isNotBlank(siteId)) {
            try {
                final Host found = findHost(siteId, user);
                if (found != null) {
                    return found;
                }
            } catch (final DotSecurityException e) {
                throw e;
            } catch (final Exception e) {
                Logger.warn(CompletionsResource.class,
                        "Could not resolve siteId '" + sanitize(siteId) + "', falling back to current host: " + e.getMessage());
            }
        }
        return WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
    }

    /**
     * Resolves a host from {@code siteId} strictly — no fallback.
     * Falls back to the HTTP-derived host when siteId is blank.
     * Returns {@code null} when the site is not found.
     * Throws {@link DotSecurityException} when the user lacks permission.
     * Use for write operations where silently targeting the wrong site is unacceptable.
     */
    private static Host resolveHostStrict(final String siteId,
                                          final HttpServletRequest request,
                                          final User user) throws DotSecurityException {
        if (StringUtils.isBlank(siteId)) {
            return WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        }
        try {
            return findHost(siteId, user);
        } catch (final DotSecurityException e) {
            throw e;
        } catch (final Exception e) {
            Logger.warn(CompletionsResource.class, "Could not resolve siteId '" + sanitize(siteId) + "': " + e.getMessage());
            return null;
        }
    }

    private static String sanitize(final String value) {
        return value == null ? "null" : value.replaceAll("[\r\n\t]", "_");
    }

    private static Host findHost(final String siteId, final User user) throws Exception {
        if ("SYSTEM_HOST".equalsIgnoreCase(siteId)) {
            return APILocator.systemHost();
        }
        final Host found = APILocator.getHostAPI().find(siteId, user, false);
        return (found != null && StringUtils.isNotBlank(found.getIdentifier()) && !found.isArchived()) ? found : null;
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

        if (formIn.stream) {
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
