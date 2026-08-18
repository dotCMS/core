package com.dotcms.ai.rest;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.app.ProviderConfigMerger;
import com.dotcms.ai.client.langchain4j.Capability;
import com.dotcms.ai.client.langchain4j.LangChain4jModelFactory;
import com.dotcms.ai.client.langchain4j.ProviderConfig;
import com.dotcms.ai.client.langchain4j.ProviderConnectionTester;
import com.dotcms.ai.client.langchain4j.TestConnectionResult;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotSecurityException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Objects;

/**
 * Exposes dotAI provider configuration metadata: which providers are available, which
 * capabilities (chat, embeddings, image) each supports, and which {@code providerConfig} fields
 * each capability needs. Backed entirely by {@link LangChain4jModelFactory#listProviderMetadata()}
 * — adding a new provider there makes it appear here automatically, with no REST-layer change.
 */
@Path("/v1/ai/providers")
@Tag(name = "AI", description = "AI-powered content generation and analysis endpoints")
public class AiProviderResource {

    private static final ObjectMapper MAPPER = DotObjectMapperProvider.createDefaultMapper();

    /**
     * Lists capability and field metadata for every registered dotAI provider.
     *
     * @param request  the HttpServletRequest object.
     * @param response the HttpServletResponse object.
     * @return a Response wrapping the list of provider metadata.
     */
    @Operation(
            operationId = "listAiProviders",
            summary = "List dotAI provider configuration metadata",
            description = "Returns, for every registered dotAI provider, the capabilities it "
                    + "supports (chat/embeddings/image) and the providerConfig fields each "
                    + "supported capability requires or accepts."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Provider metadata retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityAiProviderListView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @NoCache
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response listProviders(@Context final HttpServletRequest request,
                                        @Context final HttpServletResponse response) {

        new WebResource.InitBuilder(request, response).requiredBackendUser(true).init();
        return Response.ok(new ResponseEntityAiProviderListView(
                LangChain4jModelFactory.listProviderMetadata())).build();
    }

    /**
     * Tests whether a provider configuration actually works: builds the model for the requested
     * capability and issues one minimal, real request against the provider (a short chat reply,
     * a one-line embedding, or — for image — the generation of a single test image).
     *
     * <p>The posted config section may carry masked credential fields (e.g. {@code "apiKey":
     * "*****"}), left untouched by a client that only redisplays the previously-saved config. Any
     * such masked field is resolved against the real value already stored for {@code siteId}
     * before testing — mirroring how {@code PUT /v1/ai/completions/config} preserves unmasked
     * credentials on save — so the real secret never has to round-trip through the browser.
     * Resolution only happens when the posted {@code provider} and {@code endpoint} match what's
     * actually stored, so a caller can't pair a masked credential with a different provider or an
     * attacker-controlled {@code endpoint} to exfiltrate the real secret to it. Requires CMS admin,
     * matching {@code PUT /v1/ai/completions/config}.
     *
     * @param request     the HttpServletRequest object.
     * @param response    the HttpServletResponse object.
     * @param capability  which capability section to test — {@code chat}, {@code embeddings}, or {@code image}.
     * @param siteId      optional site identifier (or {@code SYSTEM_HOST}) whose stored config resolves masked
     *                    credentials; falls back to the site derived from the HTTP Host header.
     * @param body        the provider config section to test, e.g. {@code {"provider":"openai","apiKey":"...","model":"gpt-4o"}}.
     * @return a Response wrapping the test result: {@code success} plus a human-readable {@code message}.
     */
    @Operation(
            operationId = "testAiProviderConnection",
            summary = "Test a dotAI provider connection",
            description = "Builds the provider client for the given capability from the posted "
                    + "configuration and issues one minimal real request against the provider "
                    + "(a short chat reply, a one-line embedding, or a single test image). "
                    + "Masked credential fields (\"*****\") in the posted config are resolved "
                    + "against the real value already stored for siteId before testing. "
                    + "Returns success=false with a message on any validation or provider error "
                    + "rather than an HTTP error status, so the caller can always render the result."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Test executed — check the success field for the outcome",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityAiTestConnectionView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Unknown capability or malformed request body",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - requires CMS admin, or access denied to site",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @JSONP
    @NoCache
    @Path("/test/{capability}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public final Response testConnection(@Context final HttpServletRequest request,
                                         @Context final HttpServletResponse response,
                                         @PathParam("capability") final String capability,
                                         @QueryParam("siteId") final String siteId,
                                         @RequestBody(description = "Provider config section to test",
                                                 content = @Content(schema = @Schema(implementation = Map.class)))
                                         final String body) {

        final User user = new WebResource.InitBuilder(request, response).requiredBackendUser(true).init().getUser();

        if (!user.isAdmin()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of(AiKeys.ERROR, "Only CMS admins can test the AI provider connection"))
                    .build();
        }

        final Capability parsedCapability;
        try {
            parsedCapability = Capability.valueOf(capability.toUpperCase());
        } catch (final IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(AiKeys.ERROR, "Unknown capability: " + AiHostResolver.sanitize(capability)))
                    .build();
        }

        if (StringUtils.isBlank(body)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(AiKeys.ERROR, "Request body is required"))
                    .build();
        }

        final String resolvedBody;
        try {
            final Host host = AiHostResolver.resolveHost(siteId, request, user);
            final AppConfig storedConfig = ConfigService.INSTANCE.config(host);
            resolvedBody = resolveMaskedCredentials(body, storedConfig.getProviderConfig(), capability.toLowerCase());
        } catch (final DotSecurityException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of(AiKeys.ERROR, "Access denied to site: " + AiHostResolver.sanitize(siteId)))
                    .build();
        }

        if (ProviderConfigMerger.containsMaskedCredential(resolvedBody)) {
            return Response.ok(new ResponseEntityAiTestConnectionView(new TestConnectionResult(false,
                    "One or more credential fields still hold a placeholder value — "
                            + "re-enter them, or save the configuration first, then test again.")))
                    .build();
        }

        final ProviderConfig config;
        try {
            config = MAPPER.readValue(resolvedBody, ProviderConfig.class);
        } catch (final Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(AiKeys.ERROR, "Invalid provider configuration: " + e.getMessage()))
                    .build();
        }

        final TestConnectionResult result = ProviderConnectionTester.test(parsedCapability, config);
        return Response.ok(new ResponseEntityAiTestConnectionView(result)).build();
    }

    /**
     * Resolves any {@code "*****"} masked credential field in {@code body} against the real value
     * from the currently-stored {@code providerConfig}'s {@code sectionKey} section (e.g. {@code
     * chat}, {@code embeddings}, {@code image}). Returns {@code body} unchanged — masked fields
     * and all — when there's nothing masked, nothing stored yet, the stored section can't be
     * parsed, or {@link #targetsStoredDestination} rejects the posted {@code provider}/{@code
     * endpoint} as not matching what's stored; the caller then surfaces the still-masked
     * credential as a "re-enter it" failure rather than silently using it.
     */
    static String resolveMaskedCredentials(final String body,
                                           final String storedProviderConfigJson,
                                           final String sectionKey) {
        if (StringUtils.isBlank(storedProviderConfigJson) || !ProviderConfigMerger.containsMasked(body)) {
            return body;
        }
        try {
            final JsonNode incoming = MAPPER.readTree(body);
            final JsonNode storedSection = MAPPER.readTree(storedProviderConfigJson).get(sectionKey);
            if (storedSection == null || !storedSection.isObject()
                    || !targetsStoredDestination(incoming, storedSection)) {
                return body;
            }
            return ProviderConfigMerger.merge(body, storedSection.toString());
        } catch (final Exception e) {
            return body;
        }
    }

    /**
     * Guards against pairing a masked credential (obtainable by anyone who can {@code GET} the
     * redacted config) with a different {@code provider} or a caller-controlled {@code endpoint}
     * — which would otherwise resolve to the real stored secret and send it to a destination the
     * caller chose rather than the one it was actually saved for. Only {@code provider} and {@code
     * endpoint} are checked: every other field (model, temperature, timeout, region, etc.) doesn't
     * change where the request — and the secret riding along with it — is sent, so those can
     * differ freely between the posted body and the stored config.
     */
    static boolean targetsStoredDestination(final JsonNode incoming, final JsonNode stored) {
        return textEquals(incoming.get("provider"), stored.get("provider"))
                && textEquals(incoming.get("endpoint"), stored.get("endpoint"));
    }

    static boolean textEquals(final JsonNode a, final JsonNode b) {
        final String left = a != null && !a.isNull() ? a.asText() : null;
        final String right = b != null && !b.isNull() ? b.asText() : null;

        return Objects.equals(left, right);
    }

}
