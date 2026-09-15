package com.dotcms.inference.rest;

import com.dotcms.ai.client.langchain4j.InferenceAIClient;
import com.dotcms.ai.client.langchain4j.LangChain4jAIClient;
import com.dotcms.ai.rest.AiHostResolver;
import com.dotcms.ai.rest.ResolvedAiContext;
import com.dotcms.cost.RequestCost;
import com.dotcms.cost.RequestPrices.Price;
import com.dotcms.inference.model.InferenceError;
import com.dotcms.inference.model.InferenceUsage;
import com.dotcms.inference.rest.view.EmbeddingListView;
import com.dotcms.inference.rest.view.EmbeddingsRequestView;
import com.dotcms.inference.rest.view.InferenceErrorView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.NoCors;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.JsonNode;
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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Serves {@code POST /api/inference/v1/embeddings} — the embeddings endpoint of the
 * OpenAI-wire-format family.
 *
 * <p>Two things here are not a copy of the sibling completions endpoint with a different noun.</p>
 *
 * <p><strong>The model gate reads the site's {@code embeddings} section, not its chat models.</strong>
 * A site configures the two separately, so validating against chat would accept a chat model for an
 * embeddings call and refuse the embeddings model the site actually configured — and would do so
 * while returning a perfectly well-shaped 200 for the wrong request.</p>
 *
 * <p><strong>{@code input} is a string or an array of strings.</strong> Batching is how content is
 * ordinarily embedded — anyone indexing a site sends an array — and the array form is what makes
 * the response a list of more than one entry, and therefore what makes {@code index} load-bearing:
 * it is the caller's only means of correlating a vector back to the text they sent. That is also
 * why a batch with one bad element is refused whole rather than cleaned: dropping an element would
 * shift the index of every entry after it, and the caller would correlate against the wrong
 * text.</p>
 *
 * <p>Blank and whitespace-only strings are refused wherever they appear. There is no meaningful
 * embedding of nothing, and providers differ in whether they error on it or hand back a zero
 * vector — which is exactly the inconsistency this family exists to hide, so it cannot be left to
 * whichever provider a site configured.</p>
 *
 * <p>Nothing here logs the input text: it is customer content by definition, and it is not echoed
 * into a refusal either — a message naming the offending <em>position</em> tells the caller what
 * they need without repeating what they sent.</p>
 */
@Path("/inference/v1/embeddings")
@Tag(name = "AI", description = "AI-powered content generation and analysis endpoints")
@NoCors
public class EmbeddingsResource {

    /** Section of the site's {@code providerConfig} JSON that configures embeddings. */
    private static final String EMBEDDINGS_SECTION = "embeddings";

    /** The field a refusal about what to embed names. */
    private static final String INPUT_PARAM = "input";

    /**
     * Header form of the site override. Server-side callers routinely sit behind a proxy that
     * rewrites the Host header, and a header is the only override they can set without rewriting
     * the URL a standard client library builds. It wins over the query parameter because it is
     * the more specific of the two.
     */
    private static final String SITE_HEADER = "X-dotCMS-Site";

    /** What a caller is told when the provider failed; never the provider's own words. */
    private static final String UPSTREAM_FAILURE_MESSAGE =
            "The model provider failed to complete the request";

    /** Longest model name echoed back in a refusal, so a huge value cannot be reflected whole. */
    private static final int MAX_ECHOED_MODEL_LENGTH = 120;

    /**
     * Embeds one string, or a batch of them.
     *
     * @param request     the inbound request
     * @param response    the outbound response, used only by the authentication handshake
     * @param siteId      optional site id or host name whose dotAI configuration should serve the
     *                    request; the {@code X-dotCMS-Site} header overrides it
     * @param requestView what to embed, in the standard wire shape
     * @return the vectors, or an {@link InferenceErrorView} refusal
     */
    @Operation(
            operationId = "createEmbeddings",
            summary = "Create embeddings",
            description = "Embeds text against the model the resolved site has configured for "
                    + "embeddings, in the OpenAI-compatible request and response shape. The input "
                    + "field accepts either a single string or an array of strings embedded as one "
                    + "batch; the response is always a list with one entry per input, each carrying "
                    + "the index of the input it corresponds to. An absent, null or empty input is "
                    + "refused, as is a blank or whitespace-only string wherever it appears, and "
                    + "every element of an array must be a string — arrays of token ids are not "
                    + "supported. The model field is required and is validated against the site's "
                    + "embeddings configuration, not its chat models; there is no implicit default. "
                    + "Every response reports the serving site in the X-dotCMS-Resolved-Site header."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "The vectors, one per input",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = EmbeddingListView.class))),
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
            @ApiResponse(responseCode = "502",
                    description = "The model provider failed to complete the request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = InferenceErrorView.class)))
    })
    @POST
    @JSONP
    @NoCache
    @InferenceEndpoint
    @RequestCost(Price.HTTP_FETCH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response embeddings(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     @QueryParam("siteId") final String siteId,
                                     @RequestBody(description = "What to embed",
                                             content = @Content(schema = @Schema(
                                                     implementation = EmbeddingsRequestView.class)))
                                     final EmbeddingsRequestView requestView) {

        // Bearer only. Checked here as well as in BearerOnlyAuthFilter because the filter runs only
        // inside the JAX-RS chain, while the rule has to hold wherever this method is reached.
        final Optional<InferenceError> credentialProblem =
                BearerOnlyAuthFilter.bearerCredentialProblem(
                        request.getHeader(HttpHeaders.AUTHORIZATION));
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

        // Published before anything else can fail, so the response filter can name the serving site
        // on refusals as well as on the happy path.
        request.setAttribute(InferenceRequestAttributes.RESOLVED_SITE_ID, context.servingSiteId());

        final Response modelRefusal = refuseUnconfiguredModel(context, requestView);
        if (modelRefusal != null) {
            return modelRefusal;
        }

        final JsonNode input = requestView.input();
        final Optional<InferenceError> inputProblem = inputProblem(input);
        if (inputProblem.isPresent()) {
            return errorResponse(inputProblem.get());
        }

        return embed(context, requestView.model(), toInputs(input));
    }

    /**
     * Embeds the batch and renders the answer.
     *
     * @param context        the caller, the serving site and its configuration
     * @param requestedModel the model the caller named, already known to be configured
     * @param inputs         the texts to embed, in the order the caller sent them
     * @return the vectors, or a refusal carrying a safe description of what failed
     */
    private Response embed(final ResolvedAiContext context,
                           final String requestedModel,
                           final List<String> inputs) {
        try {
            final InferenceAIClient.EmbeddingBatch batch =
                    InferenceAIClient.get().embed(context.config(), inputs);

            if (batch.vectors().size() != inputs.size()) {
                // Without one vector per input there is no honest index to stamp on the entries,
                // and index is the caller's only means of correlating results back to what they
                // sent. A short answer is an upstream failure, not a partial success.
                Logger.error(this, "The embeddings provider returned " + batch.vectors().size()
                        + " vectors for " + inputs.size() + " inputs on site "
                        + AiHostResolver.sanitize(context.servingSiteId()));
                return errorResponse(InferenceError.upstream(UPSTREAM_FAILURE_MESSAGE));
            }

            return Response.ok(toView(batch, requestedModel))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (final RuntimeException e) {
            // The provider's own message can carry its endpoint, its account identifiers and
            // occasionally a fragment of the input, so it is logged and never returned.
            Logger.error(this, "Embeddings failed for site "
                    + AiHostResolver.sanitize(context.servingSiteId()), e);
            return errorResponse(InferenceError.upstream(UPSTREAM_FAILURE_MESSAGE));
        }
    }

    /**
     * Renders the vectors in the standard listing shape.
     *
     * <p>Each entry is stamped with the position of the input it embedded, which is why the batch
     * is sent and received as one ordered whole rather than input by input.</p>
     *
     * @param batch          what the provider produced
     * @param requestedModel the model the caller named, used when the provider reported none
     * @return the answer
     */
    private static EmbeddingListView toView(final InferenceAIClient.EmbeddingBatch batch,
                                            final String requestedModel) {
        final List<EmbeddingListView.EmbeddingView> data = new ArrayList<>(batch.vectors().size());
        for (int index = 0; index < batch.vectors().size(); index++) {
            data.add(new EmbeddingListView.EmbeddingView(
                    EmbeddingListView.EmbeddingView.OBJECT, index, batch.vectors().get(index)));
        }

        final InferenceUsage usage = batch.usage();
        return new EmbeddingListView(
                EmbeddingListView.OBJECT,
                StringUtils.isNotBlank(batch.model()) ? batch.model() : requestedModel.trim(),
                List.copyOf(data),
                usage.isReported()
                        ? new EmbeddingListView.UsageView(usage.inputTokens(), usage.totalTokens())
                        : null);
    }

    /**
     * Refuses a request whose model the resolved site has not configured <em>for embeddings</em>.
     *
     * <p>Applied to every caller, administrators included. The check is not about privilege — it is
     * what stops a site's credentials being spent on a model its owner never chose for this
     * operation. A site's chat model is refused here however well configured it is for chat.</p>
     *
     * @param context     the caller, the serving site and its configuration
     * @param requestView the inbound payload
     * @return a refusal, or null when the requested model is configured
     */
    private Response refuseUnconfiguredModel(final ResolvedAiContext context,
                                             final EmbeddingsRequestView requestView) {

        final String requestedModel = requestView == null ? null : requestView.model();
        if (StringUtils.isBlank(requestedModel)) {
            return errorResponse(InferenceError.invalidRequest(
                    "The model field is required; there is no implicit default model", "model"));
        }

        final List<String> configuredModels =
                LangChain4jAIClient.get().configuredModels(context.config(), EMBEDDINGS_SECTION);
        if (!configuredModels.contains(requestedModel.trim())) {
            Logger.warn(this, "Site " + AiHostResolver.sanitize(context.servingSiteId())
                    + " has no embeddings model matching the requested one");
            return errorResponse(InferenceError.noSuchModel(echoable(requestedModel)));
        }

        return null;
    }

    /**
     * Checks that there is something to embed, and that all of it is text.
     *
     * <p>Every element of an array is inspected rather than only the first: a mixed array is the
     * shape that actually arrives when a caller's collection was assembled from two sources, and
     * an implementation that stopped at {@code input.get(0)} would send it upstream to fail as
     * whatever the provider client makes of a heterogeneous list.</p>
     *
     * <p>When one element is at fault the message says which. {@code param} can only ever say
     * {@code input} — that is the field the caller sent — so the message is the only place the
     * position can appear, and a caller who batched five hundred strings should not have to bisect
     * their own payload to find the bad one. The position is 0-based, the same number the
     * response's own {@code index} correlates on.</p>
     *
     * @param input the {@code input} value as it arrived, possibly null
     * @return the refusal, or empty when there is something to embed
     */
    private static Optional<InferenceError> inputProblem(final JsonNode input) {
        // A JSON null deserializes to a NullNode — present, non-null, and nothing to embed — while
        // an omitted field leaves the component null outright. Two code paths, one answer.
        if (input == null || input.isNull()) {
            return Optional.of(InferenceError.invalidRequest(
                    "The input field is required; there is nothing to embed", INPUT_PARAM));
        }

        if (input.isTextual()) {
            return StringUtils.isBlank(input.asText())
                    ? Optional.of(InferenceError.invalidRequest(
                            "The input field must not be blank; there is nothing to embed",
                            INPUT_PARAM))
                    : Optional.empty();
        }

        if (!input.isArray()) {
            return Optional.of(InferenceError.invalidRequest(
                    "The input field must be a string or an array of strings", INPUT_PARAM));
        }

        if (input.isEmpty()) {
            return Optional.of(InferenceError.invalidRequest(
                    "The input field must not be an empty array; there is nothing to embed",
                    INPUT_PARAM));
        }

        for (int index = 0; index < input.size(); index++) {
            final JsonNode element = input.get(index);
            if (element == null || !element.isTextual()) {
                return Optional.of(InferenceError.invalidRequest(
                        "The element of input at index " + index + " must be a string; arrays of "
                                + "token ids are not supported", INPUT_PARAM));
            }
            if (StringUtils.isBlank(element.asText())) {
                return Optional.of(InferenceError.invalidRequest(
                        "The element of input at index " + index + " must not be blank; there is "
                                + "nothing to embed", INPUT_PARAM));
            }
        }

        return Optional.empty();
    }

    /**
     * Reads the texts to embed out of an {@code input} already known to be valid.
     *
     * @param input the {@code input} value, a string or an array of strings
     * @return the texts, in the order the caller sent them
     */
    private static List<String> toInputs(final JsonNode input) {
        if (input.isTextual()) {
            return List.of(input.asText());
        }
        final List<String> inputs = new ArrayList<>(input.size());
        input.forEach(element -> inputs.add(element.asText()));
        return List.copyOf(inputs);
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
}
