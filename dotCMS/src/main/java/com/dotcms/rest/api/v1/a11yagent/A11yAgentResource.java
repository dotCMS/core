package com.dotcms.rest.api.v1.a11yagent;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Try;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST resource that acts as the a11y-fix agent proxy.
 *
 * <p>Auth half: reuses {@code PageScannerResource}'s pattern — authenticates the backend
 * user, mints a short-lived JWT, resolves the page identifier to a fully-qualified payload.
 *
 * <p>Forward half:
 * <ul>
 *   <li>{@code POST /fix}         — plain JSON relay (agent returns the run report)</li>
 *   <li>{@code POST /fix/stream}  — streaming SSE relay ({@link EventOutput}); relays
 *       agent SSE frames as they arrive via {@code BodyHandlers.ofInputStream()}</li>
 *   <li>{@code POST /stop}        — forwards to agent /stop, passes the minted JWT</li>
 * </ul>
 *
 * <p>GZIPFilter is not registered in {@code web.xml} so no buffering risk for the SSE path.
 */
@Path("/v1/agents/a11y")
@Tag(name = "Accessibility Agent", description = "Streaming a11y-fix agent proxy")
public class A11yAgentResource {

    static final String APP_KEY = "dotPageScanner-config";

    private final WebResource webResource;
    private final HttpClient httpClient;

    public A11yAgentResource() {
        this.webResource = new WebResource();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /** Package-private constructor for unit tests. */
    A11yAgentResource(final WebResource webResource, final HttpClient httpClient) {
        this.webResource = webResource;
        this.httpClient = httpClient;
    }

    // -------------------------------------------------------------------------
    // POST /fix — plain JSON relay
    // -------------------------------------------------------------------------

    /**
     * Proxies a fix request to the agent service and returns the JSON run report.
     */
    @POST
    @Path("/fix")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "runA11yAgentFix",
            summary = "Run the accessibility fix agent on a page",
            description = "Resolves the page identifier to a live URL, URI and host id, mints a "
                    + "short-lived token for the calling user, and forwards the request to the "
                    + "configured a11y agent service. Returns the agent's report once the run "
                    + "completes. This call is synchronous and a full run can take minutes - use "
                    + "/fix/stream to receive progress as it happens. Requires the "
                    + "dotPageScanner-config App to carry the agent url and auth token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "The agent's fix report, relayed verbatim from the agent service",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400",
                    description = "identifier is missing, or the page could not be resolved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500",
                    description = "The agent App is not configured, or the agent service failed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityView.class)))
    })
    public Response fix(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final A11yAgentFixForm body) {

        final AgentContext ctx = buildContext(request, response, body);
        if (ctx.errorResponse != null) {
            return ctx.errorResponse;
        }

        return forwardJson(ctx.agentUrl + "/fix", ctx.agentPayload,
                ctx.serviceAuthToken, ctx.shortLivedToken);
    }

    // -------------------------------------------------------------------------
    // POST /fix/stream — SSE streaming relay
    // -------------------------------------------------------------------------

    /**
     * Proxies a fix request to the agent service and relays SSE frames as they arrive.
     *
     * <p>Uses {@code BodyHandlers.ofInputStream()} so the body is never buffered; frames are
     * written to {@link EventOutput} line-by-line as they arrive from the upstream agent.
     */
    @POST
    @Path("/fix/stream")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    @Operation(
            operationId = "streamA11yAgentFix",
            summary = "Run the accessibility fix agent, streaming progress over SSE",
            description = "Same as /fix, but relays the agent's Server-Sent Events as they "
                    + "arrive rather than waiting for the run to finish. Frames carry the run id, "
                    + "phase steps, progress counts, heartbeats, and a terminal done, aborted or "
                    + "error event. A configuration failure is reported as an SSE error frame "
                    + "rather than an HTTP status, because the response has already begun."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "SSE stream of agent events (text/event-stream)",
                    content = @Content(mediaType = SseFeature.SERVER_SENT_EVENTS)),
            @ApiResponse(responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json"))
    })
    public EventOutput fixStream(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final A11yAgentFixForm body) {

        final AgentContext ctx = buildContext(request, response, body);
        final EventOutput output = new EventOutput();

        if (ctx.errorResponse != null) {
            writeErrorEvent(output, ctx.errorResponse.getStatus(),
                    "Proxy configuration error — check a11y-agent App secrets");
            return output;
        }

        // Relay SSE frames asynchronously so the calling thread is not blocked.
        final Thread relayThread = Thread.ofVirtual().start(
                () -> relayStream(ctx.agentUrl + "/fix/stream", ctx.agentPayload,
                        ctx.serviceAuthToken, ctx.shortLivedToken, output));
        Logger.debug(this, () -> "SSE relay thread started: " + relayThread.getName());

        return output;
    }

    // -------------------------------------------------------------------------
    // POST /stop — stop the caller's in-flight run
    // -------------------------------------------------------------------------

    /**
     * Forwards a stop request to the agent service, passing through the {@code runId}
     * the client received from /fix or /fix/stream. Stop is addressed by runId (not by
     * the caller's identity) because the proxy mints a fresh token per request, so the
     * JWT {@code sub} differs between /fix and /stop — see {@link A11yAgentStopForm}.
     */
    @POST
    @Path("/stop")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "stopA11yAgentRun",
            summary = "Stop an in-flight accessibility fix run",
            description = "Cooperatively stops the run identified by runId. The agent stops at "
                    + "its next safe checkpoint and the open /fix/stream connection receives a "
                    + "terminal aborted event carrying a partial report - fixes already applied "
                    + "are kept. Runs are addressed by runId rather than by caller identity, "
                    + "because the proxy mints a fresh token per request."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Stop signalled, or no such run was active - both are success",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400",
                    description = "runId is missing",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json"))
    })
    public Response stop(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final A11yAgentStopForm body) {

        if (body == null || !UtilMethods.isSet(body.runId())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ResponseEntityView<>(new ErrorEntity(
                            "MISSING_RUN_ID", "runId is required")))
                    .build();
        }

        final TokenContext ctx = buildTokenContext(request, response);
        if (ctx.errorResponse != null) {
            return ctx.errorResponse;
        }

        final String payload = writeJson(Map.of("runId", body.runId()));
        return forwardJson(ctx.agentUrl + "/stop", payload,
                ctx.serviceAuthToken, ctx.shortLivedToken, "POST");
    }

    // -------------------------------------------------------------------------
    // Private helpers — context building
    // -------------------------------------------------------------------------

    /**
     * Authenticates the user, resolves the page, mints a JWT, and builds the agent payload.
     * Returns an {@link AgentContext} whose {@code errorResponse} is non-null on failure.
     */
    private AgentContext buildContext(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final A11yAgentFixForm body) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final Optional<String[]> agentConfig = resolveAgentConfig(request);
        if (agentConfig.isEmpty()) {
            return AgentContext.error(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ResponseEntityView<>(new ErrorEntity(
                            "A11Y_AGENT_NOT_CONFIGURED",
                            "A11y Agent service is not available.")))
                    .build());
        }
        final String agentUrl  = agentConfig.get()[0];
        final String authToken = agentConfig.get()[1];

        final User user = initData.getUser();

        if (body == null || !UtilMethods.isSet(body.identifier())) {
            return AgentContext.error(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ResponseEntityView<>(new ErrorEntity(
                            "MISSING_IDENTIFIER", "page.identifier is required")))
                    .build());
        }

        final PageInfo pageInfo = resolvePage(body.identifier(), body.languageId(), request);
        if (pageInfo == null) {
            return AgentContext.error(Response.status(Response.Status.NOT_FOUND)
                    .entity(new ResponseEntityView<>(new ErrorEntity(
                            "PAGE_NOT_FOUND", "No page found for identifier: " + body.identifier())))
                    .build());
        }

        final String shortLivedToken = mintShortLivedToken(user, request);
        if (!UtilMethods.isSet(shortLivedToken)) {
            return AgentContext.error(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ResponseEntityView<>(new ErrorEntity(
                            "TOKEN_GENERATION_FAILED", "Unable to generate authentication token.")))
                    .build());
        }

        final String dotcmsBaseUrl = buildBaseUrl(request);
        // No runId is minted here: the agent service owns run identity, so that it can key a
        // run on the page being fixed (hostId + identifier + languageId, all sent below) and
        // return the run already in flight instead of starting a second agent on the same
        // page. The client learns the id from the stream's first `run` frame, and passes it
        // back to /stop.
        final String payload = buildAgentPayload(dotcmsBaseUrl, pageInfo, body.skipCss());

        return new AgentContext(agentUrl, authToken, shortLivedToken, payload, null);
    }

    /** Builds context for /stop (no page needed, only auth + token). */
    private TokenContext buildTokenContext(
            final HttpServletRequest request,
            final HttpServletResponse response) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final Optional<String[]> agentConfig = resolveAgentConfig(request);
        if (agentConfig.isEmpty()) {
            return TokenContext.error(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ResponseEntityView<>(new ErrorEntity(
                            "A11Y_AGENT_NOT_CONFIGURED",
                            "A11y Agent service is not available.")))
                    .build());
        }

        final String shortLivedToken = mintShortLivedToken(initData.getUser(), request);
        if (!UtilMethods.isSet(shortLivedToken)) {
            return TokenContext.error(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ResponseEntityView<>(new ErrorEntity(
                            "TOKEN_GENERATION_FAILED", "Unable to generate authentication token.")))
                    .build());
        }

        final String[] config = agentConfig.get();
        return new TokenContext(config[0], config[1], shortLivedToken, null);
    }

    // -------------------------------------------------------------------------
    // Private helpers — forwarding
    // -------------------------------------------------------------------------

    /** Forward a request and return the upstream JSON body verbatim. */
    private Response forwardJson(
            final String url,
            final String payload,
            final String serviceAuthToken,
            final String shortLivedToken) {
        return forwardJson(url, payload, serviceAuthToken, shortLivedToken,
                payload != null ? "POST" : "GET");
    }

    private Response forwardJson(
            final String url,
            final String payload,
            final String serviceAuthToken,
            final String shortLivedToken,
            final String method) {

        try {
            final HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(300))
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .header("auth-token", serviceAuthToken)
                    .header("Authorization", "Bearer " + shortLivedToken);

            if ("POST".equalsIgnoreCase(method) && payload != null) {
                builder.POST(HttpRequest.BodyPublishers.ofString(payload));
            } else if ("POST".equalsIgnoreCase(method)) {
                builder.POST(HttpRequest.BodyPublishers.noBody());
            } else {
                builder.GET();
            }

            final HttpResponse<String> upstream =
                    httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            final int status = upstream.statusCode();
            if (status == 401 || status == 403) {
                Logger.warn(A11yAgentResource.class,
                        "A11y agent returned " + status + " — check apiAuthToken in App config");
            }

            // Relay the upstream status and body verbatim — the agent owns its error
            // shape and the Studio surfaces it directly. Only failures that never
            // reached the agent (below) are synthesized here.
            return Response.status(status).entity(upstream.body())
                    .type(MediaType.APPLICATION_JSON).build();

        } catch (Exception e) {
            Logger.error(A11yAgentResource.class,
                    "Network error forwarding to a11y agent: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new ResponseEntityView<>(new ErrorEntity(
                            "A11Y_AGENT_UNREACHABLE", "Unable to reach the a11y agent service.")))
                    .build();
        }
    }

    /**
     * Opens an SSE connection to the upstream agent and relays each frame to {@code output}
     * without buffering. Runs on a virtual thread.
     *
     * <p>SSE frames from the Hono agent follow the standard format:
     * <pre>
     * event: step
     * data: {...}
     *
     * event: done
     * data: {...}
     *
     * </pre>
     * We relay the raw lines as-is into a single unnamed {@link OutboundEvent} per logical
     * frame (the data value carries the raw SSE text so the Studio's EventSource parses it
     * correctly). We detect the end of a frame by the blank-line delimiter, then flush.
     */
    private void relayStream(
            final String url,
            final String payload,
            final String serviceAuthToken,
            final String shortLivedToken,
            final EventOutput output) {

        try {
            Logger.info(A11yAgentResource.class, "SSE relay → " + url);
            final HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(300))
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .header("Accept", SseFeature.SERVER_SENT_EVENTS)
                    .header("auth-token", serviceAuthToken)
                    .header("Authorization", "Bearer " + shortLivedToken)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            final HttpResponse<InputStream> upstream =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            final int status = upstream.statusCode();
            Logger.info(A11yAgentResource.class, "SSE relay upstream status: " + status);
            if (status == 401 || status == 403) {
                Logger.warn(A11yAgentResource.class,
                        "A11y agent returned " + status + " — check apiAuthToken in App config");
            }
            if (status >= 400) {
                // Relay the agent's own error body verbatim rather than synthesizing
                // one — the agent owns its error shape.
                writeUpstreamErrorEvent(output, upstream.body());
                return;
            }
            Logger.info(A11yAgentResource.class, "SSE relay: reading frames from upstream");

            try (final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(upstream.body(), StandardCharsets.UTF_8))) {

                String eventName = null;
                final StringBuilder dataBuilder = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.isClosed()) {
                        Logger.debug(A11yAgentResource.class,
                                "Client disconnected; stopping SSE relay");
                        break;
                    }

                    if (line.startsWith("event:")) {
                        eventName = line.substring("event:".length()).trim();
                    } else if (line.startsWith("data:")) {
                        if (dataBuilder.length() > 0) {
                            dataBuilder.append('\n');
                        }
                        dataBuilder.append(line.substring("data:".length()).trim());
                    } else if (line.isEmpty()) {
                        // blank line = end of frame; flush if we have data
                        if (dataBuilder.length() > 0) {
                            final String name = eventName;
                            final String data = dataBuilder.toString();
                            final OutboundEvent.Builder evtBuilder = new OutboundEvent.Builder()
                                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                                    .data(String.class, data);
                            if (name != null) {
                                evtBuilder.name(name);
                            }
                            output.write(evtBuilder.build());
                        }
                        eventName = null;
                        dataBuilder.setLength(0);
                    }
                }

                // Flush any trailing frame (stream ended without trailing blank line)
                if (dataBuilder.length() > 0 && !output.isClosed()) {
                    final OutboundEvent.Builder evtBuilder = new OutboundEvent.Builder()
                            .mediaType(MediaType.APPLICATION_JSON_TYPE)
                            .data(String.class, dataBuilder.toString());
                    if (eventName != null) {
                        evtBuilder.name(eventName);
                    }
                    output.write(evtBuilder.build());
                }
            }

        } catch (Exception e) {
            Logger.error(A11yAgentResource.class,
                    "Error relaying SSE stream from a11y agent: " + e.getMessage(), e);
            writeErrorEvent(output, 502, "Stream relay error: " + e.getMessage());
        } finally {
            try {
                output.close();
            } catch (IOException e) {
                Logger.warn(A11yAgentResource.class,
                        "Error closing EventOutput: " + e.getMessage());
            }
        }
    }

    /**
     * Relays the agent's own error body as the terminal {@code error} SSE frame, byte for
     * byte, so the Studio sees exactly what the agent sent. Falls back to a synthesized
     * frame only when the upstream body is empty or unreadable — i.e. when there is no
     * agent error to pass through.
     */
    private static void writeUpstreamErrorEvent(final EventOutput output,
            final InputStream upstreamBody) {
        String body = null;
        try (final InputStream in = upstreamBody) {
            body = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            Logger.warn(A11yAgentResource.class,
                    "Could not read a11y agent error body: " + e.getMessage());
        }

        if (!UtilMethods.isSet(body)) {
            writeErrorEvent(output, 502, "Agent returned an error with no body.");
            return;
        }

        // A JSON body is relayed byte for byte. A non-JSON body (an HTML error page from
        // an intermediary, say) is wrapped into {"message": "..."} so the text still
        // reaches the client instead of failing JSON.parse into a generic message.
        final String data = body.startsWith("{") || body.startsWith("[")
                ? body
                : writeJson(Map.of("message", body));

        try {
            output.write(new OutboundEvent.Builder()
                    .name("error")
                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                    .data(String.class, data)
                    .build());
        } catch (IOException e) {
            Logger.warn(A11yAgentResource.class,
                    "Error writing SSE error event: " + e.getMessage());
        } finally {
            try {
                output.close();
            } catch (IOException e) {
                Logger.warn(A11yAgentResource.class, "Error closing EventOutput: " + e.getMessage());
            }
        }
    }

    private static void writeErrorEvent(final EventOutput output, final int status,
            final String message) {
        try {
            final Map<String, Object> error = new LinkedHashMap<>();
            error.put("type", "error");
            error.put("status", status);
            error.put("message", message);
            final String data = writeJson(error);
            output.write(new OutboundEvent.Builder()
                    .name("error")
                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                    .data(String.class, data)
                    .build());
        } catch (IOException e) {
            Logger.warn(A11yAgentResource.class, "Error writing SSE error event: " + e.getMessage());
        } finally {
            try {
                output.close();
            } catch (IOException e) {
                Logger.warn(A11yAgentResource.class, "Error closing EventOutput: " + e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers — page resolution
    // -------------------------------------------------------------------------

    private PageInfo resolvePage(final String identifier, final int languageId,
            final HttpServletRequest request) {
        try {
            final Contentlet contentlet = APILocator.getContentletAPI()
                    .findContentletByIdentifierAnyLanguage(identifier, false);
            if (contentlet == null) {
                return null;
            }

            final IHTMLPage page = APILocator.getHTMLPageAssetAPI()
                    .fromContentlet(contentlet);

            final Host host = APILocator.getHostAPI()
                    .find(page.getHost(), APILocator.systemUser(), false);

            final String hostname = host != null ? host.getHostname() : request.getServerName();
            final String uri = page.getURI();
            final String baseUrl = buildBaseUrl(request);

            return new PageInfo(
                    identifier,
                    uri,
                    baseUrl + uri,
                    hostname,
                    page.getHost(),
                    languageId);

        } catch (Exception e) {
            Logger.error(A11yAgentResource.class,
                    "Error resolving page for identifier " + identifier + ": " + e.getMessage(), e);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers — App config + token
    // -------------------------------------------------------------------------

    /**
     * Reads {@code apiUrl} and {@code apiAuthToken} from the Page Scanner App secrets
     * (same keys the scanner uses). The agent runs on the same host as the scanner,
     * so {@code apiUrl} is the shared base — we append {@code /agents/a11y} to reach
     * the agent routes.
     *
     * @return array {@code [agentBaseUrl, apiAuthToken]}, or empty if not configured
     */
    private Optional<String[]> resolveAgentConfig(final HttpServletRequest request) {
        final Host currentHost = Try.<Host>of(
                () -> com.dotmarketing.business.web.WebAPILocator.getHostWebAPI()
                        .getCurrentHost(request))
                .getOrElse(APILocator.systemHost());

        final Optional<AppSecrets> secretsOpt = Try.of(
                () -> APILocator.getAppsAPI().getSecrets(APP_KEY, true,
                        currentHost, APILocator.systemUser()))
                .getOrElse(Optional.empty());

        if (secretsOpt.isEmpty()) {
            Logger.warn(A11yAgentResource.class,
                    "Page Scanner App is not configured in the Apps portlet.");
            return Optional.empty();
        }

        final Map<String, Secret> secrets = secretsOpt.get().getSecrets();
        final String apiUrl = sanitizeSecret(
                Try.of(() -> secrets.get("apiUrl").getString()).getOrElse((String) null));
        final String apiAuthToken = sanitizeSecret(
                Try.of(() -> secrets.get("apiAuthToken").getString()).getOrElse((String) null));

        if (!UtilMethods.isSet(apiUrl) || !UtilMethods.isSet(apiAuthToken)) {
            Logger.warn(A11yAgentResource.class,
                    "Page Scanner App is missing required configuration: apiUrl and apiAuthToken must be set.");
            return Optional.empty();
        }

        final String base = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        return Optional.of(new String[]{ base + "/agents/a11y", apiAuthToken });
    }

    /**
     * Mints the short-lived JWT the agent service uses to call back into dotCMS as this user.
     *
     * <p>On the {@code requestingIp} argument: it is AUDIT metadata recording who asked for the
     * token, not an enforcement field, so passing the browser's address here is correct even
     * though the agent calls back from a different egress. Enforcement is
     * {@code ApiToken.allowNetwork}, checked by {@code JsonWebTokenFactory} via
     * {@link ApiToken#isInIpRange(String)}; a null {@code allowNetwork} means unrestricted, and
     * this token deliberately leaves it unset because the agent's egress address is not known
     * to dotCMS. Setting it would need the operator to supply the agent's CIDR.</p>
     *
     * <p>The token carries the user's full rights for {@code DOT_PAGE_SCANNER_TOKEN_TTL_MS}
     * (default 5 minutes) and is not revoked after use.</p>
     *
     * @param user    the authenticated backend user the token acts as
     * @param request the originating request, used only for the audit IP
     * @return the signed JWT, or null when minting failed
     */
    private String mintShortLivedToken(final User user, final HttpServletRequest request) {
        try {
            final long ttlMs = Config.getLongProperty("DOT_PAGE_SCANNER_TOKEN_TTL_MS",
                    5L * 60L * 1000L);
            final Date expiry = new Date(System.currentTimeMillis() + ttlMs);
            final String ip = request.getRemoteAddr();

            final ApiToken apiToken = APILocator.getApiTokenAPI()
                    .persistApiToken(user.getUserId(), expiry, user.getUserId(), ip,
                            "a11y-agent-short-lived");

            return APILocator.getApiTokenAPI().getJWT(apiToken, user);
        } catch (Exception e) {
            Logger.error(A11yAgentResource.class,
                    "Error generating short-lived token: " + e.getMessage(), e);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers — payload construction
    // -------------------------------------------------------------------------

    private String buildAgentPayload(
            final String dotcmsBaseUrl,
            final PageInfo p,
            final boolean skipCss) {

        // The minted token goes in Authorization: Bearer, not the body.
        // The body carries only the resolved page fields (FixRequestSchema contract).
        // hostId is required at the top level by the agent; it is also kept inside the
        // page object since the agent still reads it there.
        final Map<String, Object> page = new LinkedHashMap<>();
        page.put("identifier", p.identifier);
        page.put("uri", p.uri);
        page.put("liveUrl", p.liveUrl);
        page.put("host", p.host);
        page.put("hostId", p.hostId);
        page.put("languageId", p.languageId);

        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("dotcmsBaseUrl", dotcmsBaseUrl);
        payload.put("hostId", p.hostId);
        payload.put("page", page);
        payload.put("options", Map.of("skipCss", skipCss));

        return writeJson(payload);
    }

    /**
     * Serializes a payload with the shared Jackson mapper.
     *
     * <p>Jackson rather than string concatenation: the hand-rolled escaping this replaces
     * covered only backslash, quote, newline, carriage return and tab, leaving the rest of the
     * U+0000-U+001F control range (notably backspace and form feed) raw. A page title or URI
     * carrying one of those produced JSON the agent could not parse, for a request that was
     * otherwise perfectly valid.
     *
     * @param payload the object graph to serialize
     * @return the JSON representation
     */
    private static String writeJson(final Object payload) {
        try {
            return DotObjectMapperProvider.getInstance().getDefaultObjectMapper()
                    .writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            // Only reachable if the maps above stop being plain data, which would be a bug
            // here rather than bad input - fail loudly instead of forwarding a malformed body.
            throw new IllegalStateException("Unable to serialize the a11y agent payload", e);
        }
    }

    private static String buildBaseUrl(final HttpServletRequest request) {
        final String scheme = UtilMethods.isSet(request.getScheme())
                ? request.getScheme() : "http";
        final int port = request.getServerPort();
        final boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        return scheme + "://" + request.getServerName() + (defaultPort ? "" : ":" + port);
    }

    private String sanitizeSecret(final String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[^\\u0020-\\u007E\\u0080-\\u00FF]", "").trim();
    }


    // -------------------------------------------------------------------------
    // Private record-like holders
    // -------------------------------------------------------------------------

    private static final class AgentContext {
        final String agentUrl;
        /** Static service secret — sent as {@code auth-token} header. */
        final String serviceAuthToken;
        /** Short-lived JWT — sent as {@code Authorization: Bearer} for agent's API calls. */
        final String shortLivedToken;
        final String agentPayload;
        final Response errorResponse;

        AgentContext(final String agentUrl, final String serviceAuthToken,
                final String shortLivedToken, final String agentPayload,
                final Response errorResponse) {
            this.agentUrl = agentUrl;
            this.serviceAuthToken = serviceAuthToken;
            this.shortLivedToken = shortLivedToken;
            this.agentPayload = agentPayload;
            this.errorResponse = errorResponse;
        }

        static AgentContext error(final Response r) {
            return new AgentContext(null, null, null, null, r);
        }
    }

    private static final class TokenContext {
        final String agentUrl;
        /** Static service secret — sent as {@code auth-token} header. */
        final String serviceAuthToken;
        /** Short-lived JWT — sent as {@code Authorization: Bearer}. */
        final String shortLivedToken;
        final Response errorResponse;

        TokenContext(final String agentUrl, final String serviceAuthToken,
                final String shortLivedToken, final Response errorResponse) {
            this.agentUrl = agentUrl;
            this.serviceAuthToken = serviceAuthToken;
            this.shortLivedToken = shortLivedToken;
            this.errorResponse = errorResponse;
        }

        static TokenContext error(final Response r) {
            return new TokenContext(null, null, null, r);
        }
    }

    private static final class PageInfo {
        final String identifier;
        final String uri;
        final String liveUrl;
        final String host;
        final String hostId;
        final int languageId;

        PageInfo(final String identifier, final String uri, final String liveUrl,
                final String host, final String hostId, final int languageId) {
            this.identifier = identifier;
            this.uri = uri;
            this.liveUrl = liveUrl;
            this.host = host;
            this.hostId = hostId;
            this.languageId = languageId;
        }
    }
}
