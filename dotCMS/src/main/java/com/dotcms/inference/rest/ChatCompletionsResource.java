package com.dotcms.inference.rest;

import com.dotcms.ai.client.langchain4j.InferenceAIClient;
import com.dotcms.ai.client.langchain4j.ProviderConfig;
import com.dotcms.ai.rest.AiHostResolver;
import com.dotcms.ai.rest.ResolvedAiContext;
import com.dotcms.cost.RequestCost;
import com.dotcms.cost.RequestPrices.Price;
import com.dotcms.inference.model.InferenceError;
import com.dotcms.inference.model.InferenceLimits;
import com.dotcms.inference.model.InferenceRequest;
import com.dotcms.inference.model.InferenceResponse;
import com.dotcms.inference.model.InferenceStreamEvent;
import com.dotcms.inference.rest.mapper.ChatCompletionMapper;
import com.dotcms.inference.rest.mapper.SseSerializer;
import com.dotcms.inference.rest.view.ChatCompletionRequestView;
import com.dotcms.inference.rest.view.ChatCompletionView;
import com.dotcms.inference.rest.view.InferenceErrorView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.NoCors;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Serves {@code POST /api/inference/v1/chat/completions} — the chat-completions endpoint of the
 * OpenAI-wire-format family.
 *
 * <p>Byte compatibility with the external standard is the whole point of this family, so nothing
 * here is wrapped in the dotCMS {@code ResponseEntityView} envelope: a client library has to be
 * able to deserialize both the answer and the refusal into its own types with no adapter. That is
 * also why refusals are rendered as {@link InferenceErrorView} rather than left to dotCMS's
 * generic exception mappers.</p>
 *
 * <p>This class is deliberately thin. Every decision that could be made differently by a second
 * wire format lives elsewhere — the vocabulary translation in {@link ChatCompletionMapper}, the
 * frame shapes in {@link SseSerializer}, the provider exchange in {@link InferenceAIClient}, and
 * site plus configuration resolution in {@link AiHostResolver}. What is left here, and only here,
 * is the HTTP contract: who may call, which site pays, which model is allowed, how many streams a
 * node will hold open at once, and how a failure becomes a status.</p>
 *
 * <p>Two behaviours are worth calling out because getting either wrong is silent:</p>
 *
 * <ul>
 *     <li>The resolved site id is published into the request as
 *     {@link InferenceRequestAttributes#RESOLVED_SITE_ID} the instant it is known, before anything
 *     that can fail. {@link ResolvedSiteHeaderFilter} reads it to report the serving site on
 *     <em>every</em> response, and a value only the happy path could set would not deliver that.</li>
 *     <li>A streamed answer closes with {@link SseSerializer#DONE_MARKER} only when
 *     {@link SseSerializer#shouldWriteDoneMarker(InferenceStreamEvent)} says so for the last event
 *     the stream produced. Once the first frame is written the HTTP status is already on the wire
 *     and can no longer carry a failure, so withholding the marker is the only thing that stops a
 *     client which does not parse the error frame from reading a truncated answer as a finished
 *     one.</li>
 * </ul>
 */
@Path("/inference/v1/chat")
@Tag(name = "AI", description = "AI-powered content generation and analysis endpoints")
@NoCors
public class ChatCompletionsResource {

    /** Media type a streamed completion is served as. */
    private static final String EVENT_STREAM = "text/event-stream";

    /** Section of the site's {@code providerConfig} JSON that configures chat. */
    private static final String CHAT_SECTION = "chat";

    /**
     * Header form of the site override. Server-side callers routinely sit behind a proxy that
     * rewrites the Host header, and a header is the only override they can set without rewriting
     * the URL a standard client library builds. It wins over the query parameter because it is
     * the more specific of the two — a caller that sets both meant the one they had to go out of
     * their way to add.
     */
    private static final String SITE_HEADER = "X-dotCMS-Site";

    /** Prefix the completion id carries, as standard clients expect. */
    /** The header a standard client reads to learn how long to wait before retrying. */
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    /**
     * How long to tell a caller to wait when this node is at its streaming ceiling.
     *
     * <p>Five seconds, which is a judgement rather than a measurement: a streamed completion runs
     * for seconds to minutes, so no single number is right for every deployment. It is chosen to be
     * long enough that a refused client does not immediately return and re-refuse — turning one
     * ceiling into a retry storm that keeps the node at capacity — and short enough that capacity
     * freed a moment later does not sit idle. It is not a configurable limit because it is not a
     * limit: the ceiling itself is configurable, and this only says how long to wait for it.</p>
     */
    private static final int CAPACITY_RETRY_AFTER_SECONDS = 5;

    private static final String COMPLETION_ID_PREFIX = "chatcmpl-";

    /** What a caller is told when the provider failed; never the provider's own words. */
    private static final String UPSTREAM_FAILURE_MESSAGE =
            "The model provider failed to complete the request";

    /** Longest model name echoed back in a refusal, so a huge value cannot be reflected whole. */
    private static final int MAX_ECHOED_MODEL_LENGTH = 120;

    private static final ObjectMapper MAPPER = DotObjectMapperProvider.createDefaultMapper();

    /**
     * Streams in flight on this node. A streamed completion parks a request thread for the whole
     * generation rather than for one round trip, so concurrency — not request rate — is the scarce
     * resource this family has to bound. Counted rather than held as a fixed-permit semaphore so
     * that an operator raising {@link InferenceLimits#MAX_CONCURRENT_STREAMS_KEY} takes effect
     * without a restart.
     */
    private static final AtomicInteger ACTIVE_STREAMS = new AtomicInteger();

    /**
     * Runs one chat completion, streamed or whole.
     *
     * @param request     the inbound request
     * @param response    the outbound response, used only by the authentication handshake
     * @param siteId      optional site id or host name whose dotAI configuration should serve the
     *                    request; the {@code X-dotCMS-Site} header overrides it
     * @param requestView the completion to run, in the standard wire shape
     * @return the completed answer, a {@link StreamingOutput} of server-sent events when
     *         {@code stream} was asked for, or an {@link InferenceErrorView} refusal
     */
    @Operation(
            operationId = "createChatCompletion",
            summary = "Create a chat completion",
            description = "Runs one chat completion against the model the resolved site has "
                    + "configured, in the OpenAI-compatible request and response shape. Set "
                    + "\"stream\": true to receive the answer as server-sent events, each frame a "
                    + "chat.completion.chunk, closing with data: [DONE] — a stream that failed "
                    + "ends on an error frame and never carries that marker. The model field is "
                    + "required and must be one the site has configured; there is no implicit "
                    + "default. Every response reports the serving site in the "
                    + "X-dotCMS-Resolved-Site header."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "The completion, or the event stream when stream was requested",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ChatCompletionView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Malformed request, or one asking for something unsupported",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = InferenceErrorView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = InferenceErrorView.class))),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - the caller cannot read the requested site",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = InferenceErrorView.class))),
            @ApiResponse(responseCode = "404",
                    description = "The requested model is not configured for the resolved site",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = InferenceErrorView.class))),
            @ApiResponse(responseCode = "429",
                    description = "Too many streamed completions are already in flight on this node",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = InferenceErrorView.class))),
            @ApiResponse(responseCode = "502",
                    description = "The model provider failed to complete the request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = InferenceErrorView.class)))
    })
    @POST
    @JSONP
    @NoCache
    @InferenceEndpoint
    // One flat price for an operation whose real cost varies by orders of magnitude, and it
    // under-counts a stream worst of all: a streamed completion holds a request thread for the
    // whole generation rather than for one round trip, and the tokens it spends are unknown until
    // it ends. Deliberately not "fixed" here by picking a larger constant — a bigger flat number
    // is the same mistake scaled, and it would silently re-price every non-streaming caller too.
    // Pricing this honestly means charging on what a request actually consumed, token counts being
    // the unit that matches what the provider bills, which is a design this endpoint cannot settle
    // on its own: it needs a decision about where usage is metered, what happens when a stream
    // fails halfway, and how it reconciles with per-site spend attribution, which is not
    // something this endpoint family answers. Until that exists, the flat price stands and is
    // known to be wrong for streams.
    @RequestCost(Price.HTTP_FETCH)
    @Path("/completions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, EVENT_STREAM})
    public Response completions(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                      @QueryParam("siteId") final String siteId,
                                      @RequestBody(description = "The completion to run",
                                              content = @Content(schema = @Schema(
                                                      implementation = ChatCompletionRequestView.class)))
                                      final ChatCompletionRequestView requestView) {

        // Bearer only. Checked here as well as in BearerOnlyAuthFilter because the filter runs
        // only inside the JAX-RS chain, while the rule has to hold wherever this method is
        // reached. Without it the surrounding authentication accepts a session cookie or basic
        // auth, which would undo the reasoning behind emitting no CORS headers: that the
        // credential is a token someone deliberately issued and placed on a server, not one a
        // browser attaches by itself.
        final Optional<InferenceError> credentialProblem =
                BearerOnlyAuthFilter.bearerCredentialProblem(
                        request.getHeader(javax.ws.rs.core.HttpHeaders.AUTHORIZATION));
        if (credentialProblem.isPresent()) {
            return errorResponse(credentialProblem.get());
        }

        // Any authenticated user, backend or frontend; an anonymous caller is rejected here with a
        // 401 the builder produces itself.
        final User user = new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(true)
                .init()
                .getUser();

        // The token, not the session, says who this is. The builder above reads
        // PortalUtil.getUser(request) before it authenticates anything, so on a request carrying
        // both a session and a bearer header it answers with the session's owner and never looks
        // at the token. Resolved here from the token alone and compared, so an unparseable token
        // cannot ride a session in, and a valid token cannot be overridden by one.
        final User bearerUser = BearerOnlyAuthFilter.bearerUser(request);
        if (bearerUser == null) {
            return errorResponse(BearerOnlyAuthFilter.invalidBearerToken());
        }
        if (!bearerUser.getUserId().equals(user.getUserId())) {
            Logger.warn(this, "Bearer token and session name different users; refusing");
            return errorResponse(BearerOnlyAuthFilter.credentialConflict());
        }

        final ResolvedAiContext context;
        try {
            context = AiHostResolver.resolve(request, siteOverride(request, siteId), user);
        } catch (final DotSecurityException e) {
            Logger.error(this, "Caller cannot read the requested site '"
                    + AiHostResolver.sanitize(siteId) + "'", e);
            return errorResponse(new InferenceError(
                    "invalid_request_error", "Access denied to the requested site", "siteId", 403));
        } catch (final IllegalArgumentException e) {
            Logger.error(this, "Could not resolve the requested site '"
                    + AiHostResolver.sanitize(siteId) + "'", e);
            return errorResponse(InferenceError.invalidRequest(
                    "The requested site could not be resolved", "siteId"));
        }

        // Published before anything else can fail, so the response filter can name the serving
        // site on refusals and on streams that broke after they began, not only on the happy path.
        request.setAttribute(InferenceRequestAttributes.RESOLVED_SITE_ID, context.servingSiteId());

        final Response modelRefusal = refuseUnconfiguredModel(context, requestView);
        if (modelRefusal != null) {
            return modelRefusal;
        }

        final InferenceRequest inferenceRequest;
        try {
            inferenceRequest = ChatCompletionMapper.toInferenceRequest(requestView);
        } catch (final IllegalArgumentException e) {
            // The mapper's messages are dotCMS's own and name the offending field, so they are
            // safe to return verbatim; nothing from a provider has been read at this point.
            return errorResponse(InferenceError.invalidRequest(e.getMessage(), null));
        }

        return inferenceRequest.stream()
                ? streamCompletion(context, inferenceRequest)
                : completeWhole(context, inferenceRequest);
    }

    /**
     * Runs a completion and returns the whole answer.
     *
     * @param context          the caller, the serving site and its configuration
     * @param inferenceRequest the completion to run
     * @return the answer, or a refusal carrying a safe description of what failed
     */
    private Response completeWhole(final ResolvedAiContext context,
                                   final InferenceRequest inferenceRequest) {
        try {
            final InferenceResponse inferenceResponse =
                    InferenceAIClient.get().complete(context.config(), inferenceRequest);
            return Response.ok(ChatCompletionMapper.toView(inferenceResponse))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (final RuntimeException e) {
            // The provider's own message can carry its endpoint, its account identifiers and
            // occasionally a fragment of the prompt, so it is logged and never returned.
            Logger.error(this, "Chat completion failed for site "
                    + AiHostResolver.sanitize(context.servingSiteId()), e);
            return errorResponse(InferenceError.fromProviderFailure(e, UPSTREAM_FAILURE_MESSAGE));
        }
    }

    /**
     * Returns the answer as a stream of server-sent events.
     *
     * <p>The entity is a {@link StreamingOutput} rather than an already-written body: the frames
     * have to reach the caller as the model produces them, which is the only thing that makes
     * streaming worth its cost in held threads. That held thread is also why the concurrency
     * ceiling is claimed here, before the response is returned — once the container starts writing
     * the body, a refusal can no longer be expressed as a status.</p>
     *
     * @param context          the caller, the serving site and its configuration
     * @param inferenceRequest the completion to run
     * @return the event stream, or a 429 refusal when the node is already at its ceiling
     */
    private Response streamCompletion(final ResolvedAiContext context,
                                      final InferenceRequest inferenceRequest) {

        final int maxConcurrentStreams = InferenceLimits.current().maxConcurrentStreams();
        if (ACTIVE_STREAMS.incrementAndGet() > maxConcurrentStreams) {
            ACTIVE_STREAMS.decrementAndGet();
            Logger.warn(this, "Refusing a streamed completion: the node is already serving its "
                    + "ceiling of " + maxConcurrentStreams + " concurrent streams");
            // Carries Retry-After because this refusal is dotCMS's own: the wait is until a
            // stream on this node finishes, not until some provider decides to let us back in.
            // "Retry shortly" in the message is prose a client cannot act on; the header is the
            // same instruction in the form every standard client already honours.
            return errorResponse(new InferenceError(
                    "rate_limit_error",
                    "This node is already serving its ceiling of " + maxConcurrentStreams
                            + " concurrent streamed completions; retry shortly",
                    null,
                    429),
                    CAPACITY_RETRY_AFTER_SECONDS);
        }

        // Fixed for the whole stream: a client correlates the chunks it stitches together by the
        // completion id, so re-deriving any of these per event would break the correlation.
        final String completionId = COMPLETION_ID_PREFIX + UUID.randomUUID();
        final long createdEpochSeconds = Instant.now().getEpochSecond();
        final String model = inferenceRequest.model();

        final StreamingOutput streamingOutput = output -> {
            final SseFrameWriter writer =
                    new SseFrameWriter(output, completionId, model, createdEpochSeconds);
            try {
                InferenceAIClient.get().stream(context.config(), inferenceRequest, writer);
                writer.close();
            } finally {
                // In a finally so a stream that failed, or a caller that walked away mid-answer,
                // gives its slot back instead of leaking it for the life of the node.
                ACTIVE_STREAMS.decrementAndGet();
            }
        };

        return Response.ok(streamingOutput).type(EVENT_STREAM).build();
    }

    /**
     * Refuses a request whose model the resolved site has not configured.
     *
     * <p>Applied to every caller, administrators included. The check is not about privilege — it
     * is what stops a site's credentials being spent on a model its owner never chose, and an
     * administrator of one site is not thereby entitled to spend another site's budget on an
     * arbitrary model name.</p>
     *
     * @param context     the caller, the serving site and its configuration
     * @param requestView the inbound payload
     * @return a refusal, or null when the requested model is configured
     */
    private Response refuseUnconfiguredModel(final ResolvedAiContext context,
                                             final ChatCompletionRequestView requestView) {

        final String requestedModel = requestView == null ? null : requestView.model();
        if (StringUtils.isBlank(requestedModel)) {
            // Named here rather than left to the mapper so the refusal can point at the field; the
            // mapper's contract is a message, and a caller sent back to guessing is a support call.
            return errorResponse(InferenceError.invalidRequest(
                    "The model field is required; there is no implicit default model", "model"));
        }

        final List<String> configuredModels = configuredChatModels(context);
        if (!configuredModels.contains(requestedModel.trim())) {
            Logger.warn(this, "Site " + AiHostResolver.sanitize(context.servingSiteId())
                    + " has no chat model matching the requested one");
            return errorResponse(InferenceError.noSuchModel(echoable(requestedModel), "chat"));
        }

        return null;
    }

    /**
     * Reads the chat models the resolved site has configured.
     *
     * <p>A site with no usable chat configuration yields an empty list rather than an exception,
     * which lands the caller on the same 404 as an unknown model. That is the honest answer: from
     * where the caller stands, a model nobody configured and a model on a site nobody configured
     * are the same absence, and distinguishing them would tell an unauthenticated-adjacent caller
     * which sites have dotAI set up.</p>
     *
     * @param context the caller, the serving site and its configuration
     * @return the configured chat model names, in fallback order; empty when there are none
     */
    private List<String> configuredChatModels(final ResolvedAiContext context) {

        final String providerConfigJson = context.config().getProviderConfig();
        if (StringUtils.isBlank(providerConfigJson)) {
            return List.of();
        }

        try {
            final JsonNode section = MAPPER.readTree(providerConfigJson).get(CHAT_SECTION);
            if (section == null || section.isNull()) {
                return List.of();
            }

            final ProviderConfig chatConfig = MAPPER.treeToValue(section, ProviderConfig.class);
            final List<String> models = new ArrayList<>(chatConfig.allModels());
            if (models.isEmpty() && StringUtils.isNotBlank(chatConfig.deploymentName())) {
                // Azure names the served model by its deployment, exactly as the chat client does
                // when it builds its own fallback chain.
                models.add(chatConfig.deploymentName());
            }

            return List.copyOf(models);
        } catch (final Exception e) {
            Logger.error(this, "Could not read the chat section of providerConfig for site "
                    + AiHostResolver.sanitize(context.servingSiteId()), e);
            return List.of();
        }
    }

    /**
     * Picks the site override, preferring the header.
     *
     * @param request the inbound request
     * @param siteId  the {@code siteId} query parameter, possibly blank
     * @return the override to resolve against, or null to resolve from the request as usual
     */
    private static String siteOverride(final HttpServletRequest request, final String siteId) {
        final String header = request.getHeader(SITE_HEADER);
        return StringUtils.isNotBlank(header) ? header : siteId;
    }

    /**
     * @param model the model name the caller asked for
     * @return a bounded, single-line version safe to repeat back in a refusal
     */
    private static String echoable(final String model) {
        final String sanitized = AiHostResolver.sanitize(model);
        return sanitized.length() > MAX_ECHOED_MODEL_LENGTH
                ? sanitized.substring(0, MAX_ECHOED_MODEL_LENGTH)
                : sanitized;
    }

    /**
     * @param error the refusal
     * @return the refusal as a response, in the standard error shape
     */
    private static Response errorResponse(final InferenceError error) {
        return Response.status(error.httpStatus())
                .entity(InferenceErrorView.of(error))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Renders a refusal that tells the caller when to come back.
     *
     * <p>Only for refusals whose duration dotCMS actually knows — its own capacity ceilings. An
     * upstream rate limit is not one of those: the provider knows how long it wants to be left
     * alone and says so in a header the provider abstraction discards before dotCMS sees it, and
     * inventing a number there would be a guess presented to the client as an instruction.</p>
     *
     * @param error             the refusal
     * @param retryAfterSeconds how long the caller should wait before trying again
     * @return the refusal, carrying {@code Retry-After}
     */
    private static Response errorResponse(final InferenceError error, final int retryAfterSeconds) {
        return Response.status(error.httpStatus())
                .entity(InferenceErrorView.of(error))
                .type(MediaType.APPLICATION_JSON)
                .header(RETRY_AFTER_HEADER, retryAfterSeconds)
                .build();
    }

    /**
     * Writes stream events out as server-sent event frames, and decides how the stream ends.
     *
     * <p>Kept as a type rather than a lambda because ending the stream correctly needs two facts
     * the writing itself produces: which event came last, and whether the connection is still
     * usable. Both are written on the provider's threads and read on the request thread once
     * {@link InferenceAIClient#stream} has returned, so both are volatile.</p>
     */
    private static final class SseFrameWriter implements Consumer<InferenceStreamEvent> {

        private final OutputStream output;
        private final String completionId;
        private final String model;
        private final long createdEpochSeconds;

        private volatile InferenceStreamEvent lastEvent;
        private volatile boolean broken;

        private SseFrameWriter(final OutputStream output,
                               final String completionId,
                               final String model,
                               final long createdEpochSeconds) {
            this.output = output;
            this.completionId = completionId;
            this.model = model;
            this.createdEpochSeconds = createdEpochSeconds;
        }

        /**
         * Writes one event out.
         *
         * @param event the event the provider produced
         */
        @Override
        public void accept(final InferenceStreamEvent event) {
            this.lastEvent = event;
            write(SseSerializer.toFrame(event, completionId, model, createdEpochSeconds));
        }

        /**
         * Closes the stream the way its last event dictates.
         *
         * <p>The terminal marker follows a stream that ended normally and is withheld from one
         * that did not — that withholding is the entire failure signal a streamed answer has left
         * once its status line is gone. A stream that produced nothing at all gets an error frame
         * of its own, for the same reason: silence and success look identical to a reader.</p>
         */
        private void close() {
            final InferenceStreamEvent last = this.lastEvent;

            if (last == null) {
                writeQuietly(SseSerializer.toFrame(
                        new InferenceStreamEvent.Error(InferenceError.upstream(
                                "The model provider produced no answer")),
                        completionId,
                        model,
                        createdEpochSeconds));
                return;
            }

            if (SseSerializer.shouldWriteDoneMarker(last)) {
                writeQuietly(SseSerializer.DONE_MARKER);
            }
        }

        /**
         * @param frame the frame to write
         * @throws UncheckedIOException when the caller has gone away, which ends the stream: the
         *                              client is the only channel a streamed answer has, so there
         *                              is nothing left to do but stop
         */
        private void write(final String frame) {
            if (broken) {
                return;
            }
            try {
                output.write(frame.getBytes(StandardCharsets.UTF_8));
                output.flush();
            } catch (final IOException e) {
                broken = true;
                throw new UncheckedIOException(e);
            }
        }

        /**
         * Writes a closing frame, tolerating a connection that has already gone.
         *
         * @param frame the frame to write
         */
        private void writeQuietly(final String frame) {
            try {
                write(frame);
            } catch (final UncheckedIOException e) {
                Logger.warn(ChatCompletionsResource.class,
                        "Could not close the inference stream; the caller has gone away");
            }
        }
    }
}
