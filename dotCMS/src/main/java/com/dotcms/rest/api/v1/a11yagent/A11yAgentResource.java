package com.dotcms.rest.api.v1.a11yagent;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Try;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST resource that acts as the a11y-fix agent proxy (plan §8).
 *
 * <p>Auth half: reuses {@code PageScannerResource}'s pattern — authenticates the backend
 * user, mints a short-lived JWT, resolves the page identifier to a fully-qualified payload.
 *
 * <p>Forward half:
 * <ul>
 *   <li>{@code POST /fix}         — plain JSON relay (agent returns §6 report)</li>
 *   <li>{@code POST /fix/stream}  — streaming SSE relay ({@link EventOutput}); relays
 *       agent SSE frames as they arrive via {@code BodyHandlers.ofInputStream()} (plan §8.6)</li>
 *   <li>{@code POST /stop}        — forwards to agent /stop, passes the minted JWT</li>
 *   <li>{@code GET  /active-run}  — forwards to agent /active-run, passes the minted JWT</li>
 * </ul>
 *
 * <p>GZIPFilter is not registered in {@code web.xml} so no buffering risk for the SSE path (§8.5).
 */
@Path("/v1/agent/a11y")
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
     * Proxies a fix request to the agent service and returns the §6 JSON report.
     */
    @POST
    @Path("/fix")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
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
     * Proxies a fix request to the agent service and relays SSE frames as they arrive (plan §8.6).
     *
     * <p>Uses {@code BodyHandlers.ofInputStream()} so the body is never buffered; frames are
     * written to {@link EventOutput} line-by-line as they arrive from the upstream agent.
     */
    @POST
    @Path("/fix/stream")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(SseFeature.SERVER_SENT_EVENTS)
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
     * Forwards a stop request to the agent service using the caller's minted JWT.
     */
    @POST
    @Path("/stop")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response stop(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {

        final TokenContext ctx = buildTokenContext(request, response);
        if (ctx.errorResponse != null) {
            return ctx.errorResponse;
        }

        return forwardJson(ctx.agentUrl + "/stop", null,
                ctx.serviceAuthToken, ctx.shortLivedToken, "POST");
    }

    // -------------------------------------------------------------------------
    // GET /active-run — retrieve the caller's active or last run
    // -------------------------------------------------------------------------

    /**
     * Forwards an active-run query to the agent service using the caller's minted JWT.
     */
    @GET
    @Path("/active-run")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response activeRun(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {

        final TokenContext ctx = buildTokenContext(request, response);
        if (ctx.errorResponse != null) {
            return ctx.errorResponse;
        }

        return forwardJson(ctx.agentUrl + "/active-run", null,
                ctx.serviceAuthToken, ctx.shortLivedToken, "GET");
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

        if (body == null || !UtilMethods.isSet(body.getIdentifier())) {
            return AgentContext.error(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ResponseEntityView<>(new ErrorEntity(
                            "MISSING_IDENTIFIER", "page.identifier is required")))
                    .build());
        }

        final PageInfo pageInfo = resolvePage(body.getIdentifier(), body.getLanguageId(), request);
        if (pageInfo == null) {
            return AgentContext.error(Response.status(Response.Status.NOT_FOUND)
                    .entity(new ResponseEntityView<>(new ErrorEntity(
                            "PAGE_NOT_FOUND", "No page found for identifier: " + body.getIdentifier())))
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
        final String runId = "r_" + UUID.randomUUID().toString().replace("-", "");
        final String payload = buildAgentPayload(runId, dotcmsBaseUrl, pageInfo,
                body.isSkipCss());

        return new AgentContext(agentUrl, authToken, shortLivedToken, payload, null);
    }

    /** Builds context for /stop and /active-run (no page needed, only auth + token). */
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
                return Response.status(Response.Status.BAD_GATEWAY)
                        .entity(new ResponseEntityView<>(new ErrorEntity(
                                "A11Y_AGENT_AUTH_FAILED", "Agent service authentication failed.")))
                        .build();
            }

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
     * without buffering (plan §8.6). Runs on a virtual thread.
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
                writeErrorEvent(output, status,
                        "Agent service authentication failed — check apiAuthToken");
                return;
            }
            if (status >= 400) {
                writeErrorEvent(output, status,
                        "Agent returned " + status);
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

    private static void writeErrorEvent(final EventOutput output, final int status,
            final String message) {
        try {
            final String data = "{\"type\":\"error\",\"status\":" + status
                    + ",\"message\":" + jsonString(message) + "}";
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
     * so {@code apiUrl} is the shared base — we append {@code /agent/a11y} to reach
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
        return Optional.of(new String[]{ base + "/agent/a11y", apiAuthToken });
    }

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
            final String runId,
            final String dotcmsBaseUrl,
            final PageInfo p,
            final boolean skipCss) {

        // The minted token goes in Authorization: Bearer (plan §8.2), not the body.
        // The body carries only the resolved page fields (FixRequestSchema contract).
        return "{"
                + "\"runId\":" + jsonString(runId) + ","
                + "\"dotcmsBaseUrl\":" + jsonString(dotcmsBaseUrl) + ","
                + "\"page\":{"
                + "\"identifier\":" + jsonString(p.identifier) + ","
                + "\"uri\":" + jsonString(p.uri) + ","
                + "\"liveUrl\":" + jsonString(p.liveUrl) + ","
                + "\"host\":" + jsonString(p.host) + ","
                + "\"hostId\":" + jsonString(p.hostId) + ","
                + "\"languageId\":" + p.languageId
                + "},"
                + "\"options\":{\"skipCss\":" + skipCss + "}"
                + "}";
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

    private static String jsonString(final String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
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
