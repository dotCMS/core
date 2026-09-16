package com.dotcms.inference.rest;

import com.dotcms.ai.client.langchain4j.InferenceAIClient;
import com.dotcms.ai.client.langchain4j.LangChain4jAIClient;
import com.dotcms.ai.rest.AiHostResolver;
import com.dotcms.ai.rest.ResolvedAiContext;
import com.dotcms.cost.RequestCost;
import com.dotcms.cost.RequestPrices.Price;
import com.dotcms.inference.model.InferenceError;
import com.dotcms.inference.model.InferenceLimits;
import com.dotcms.inference.model.MultipleImagesUnsupportedException;
import com.dotcms.inference.rest.view.ImageGenerationRequestView;
import com.dotcms.inference.rest.view.ImageGenerationView;
import com.dotcms.inference.rest.view.InferenceErrorView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Serves {@code POST /api/inference/v1/images/generations} — the image-generation endpoint of the
 * OpenAI-wire-format family.
 *
 * <p><strong>The image comes back inline, as base64, and never as a URL.</strong> A hosted URL
 * would mean deciding storage, authentication and lifetime for an artifact generated from a prompt
 * that may carry customer data, so this family declines to create a separately-addressable
 * artifact at all. The answer shape has no {@code url} component, which is what keeps the decision
 * from reversing quietly — a provider's own link passed through would otherwise be a 200 with a
 * perfectly plausible body. Where a provider offers the choice dotCMS also asks it for the inline
 * form, so nothing is minted upstream either; where a provider returns a link regardless, dotCMS
 * fetches and re-encodes it rather than refuse the provider. On those providers an artifact really
 * does exist upstream: the guarantee is that a caller never receives one.</p>
 *
 * <p><strong>{@code n} is honoured, and omitting it means one.</strong> The adopted format
 * supports generating several images in one request, so refusing outright would be this family
 * declining something the standard supports. Three things bound it, and all three are refusals
 * naming the field rather than quiet clamps — clamping answers a request for four hundred images
 * with four, and a bill, without the caller ever learning that what they asked for is not what
 * they got. A value below one is refused. A value above the configured ceiling is refused; the
 * default ceiling is the one the standard itself documents for this field. And a request for
 * several images against a model that can only ever produce one is refused here rather than
 * translated as an upstream failure, because a retryable status would send a client's back-off
 * into retrying something that cannot succeed. Support is asked of the model itself rather than
 * read from a per-vendor table kept in this file, which would rot the first time a library
 * release changed it.</p>
 *
 * <p><strong>{@code size} is passed through to the provider.</strong> It changes both what the
 * caller receives and what the site pays, so dropping it is a cost and correctness failure rather
 * than the harmless compatibility courtesy that ignoring an incidental field is — and it is
 * invisible in the response, since a provider asked for nothing in particular still returns a
 * well-formed image.</p>
 *
 * <p><strong>The model gate reads the site's {@code image} section</strong>, not its chat models
 * and not its embeddings model. A site configures all three separately, so reusing another
 * section's gate would accept the wrong model and refuse the right one while still answering
 * 200 and 404 in the right shapes.</p>
 *
 * <p>Nothing here logs the prompt: it is customer content, and it is not echoed into a refusal
 * either.</p>
 */
@Path("/inference/v1/images")
@Tag(name = "AI", description = "AI-powered content generation and analysis endpoints")
public class ImagesResource {

    /** Section of the site's {@code providerConfig} JSON that configures image generation. */
    private static final String IMAGE_SECTION = "image";

    /** How many images a request that says nothing about it asks for. */
    private static final int DEFAULT_IMAGE_COUNT = 1;

    /** The field a refusal about how many images to generate names. */
    private static final String COUNT_PARAM = "n";

    /** The field a refusal about what to generate names. */
    private static final String PROMPT_PARAM = "prompt";

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
     * Generates images from a prompt.
     *
     * @param request     the inbound request
     * @param response    the outbound response, used only by the authentication handshake
     * @param siteId      optional site id or host name whose dotAI configuration should serve the
     *                    request; the {@code X-dotCMS-Site} header overrides it
     * @param requestView what to generate, in the standard wire shape
     * @return the images as base64, or an {@link InferenceErrorView} refusal
     */
    @Operation(
            operationId = "createImageGeneration",
            summary = "Generate images",
            description = "Generates images against the model the resolved site has configured "
                    + "for images, in the OpenAI-compatible request and response shape. Every image "
                    + "is returned inline as b64_json: no hosted, separately-addressable "
                    + "artifact is created, and no url is ever returned. Both model and prompt are "
                    + "required, and model is validated against the site's image configuration "
                    + "rather than its chat or embeddings models; there is no implicit default. The "
                    + "n field is honored — omitting it means one image. A value below 1, a value "
                    + "above the configured maximum, or any value above 1 on a site whose image "
                    + "model can only produce one, is refused with a 400 naming the field. The size field is passed through to the provider as a WIDTHxHEIGHT "
                    + "string; where the site carries an image size setting the caller's value wins "
                    + "and the site's is the default. Every response reports the serving site in "
                    + "the X-dotCMS-Resolved-Site header."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "The generated images, each inline as base64",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ImageGenerationView.class))),
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
    @Path("/generations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generations(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                      @QueryParam("siteId") final String siteId,
                                      @RequestBody(description = "What to generate",
                                              content = @Content(schema = @Schema(
                                                      implementation = ImageGenerationRequestView.class)))
                                      final ImageGenerationRequestView requestView) {

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

        if (StringUtils.isBlank(requestView.prompt())) {
            // Named as its own field rather than folded into a generic "malformed request": a
            // caller sent back to guessing which of two required fields they missed is a support
            // call.
            return errorResponse(InferenceError.invalidRequest(
                    "The prompt field is required; there is nothing to generate without one",
                    PROMPT_PARAM));
        }

        // Absent means one. Below one is refused rather than clamped: a clamp answers a request
        // for no images with an image and a bill, and the caller never learns that what they asked
        // for was not what they got.
        final int count = requestView.n() == null ? DEFAULT_IMAGE_COUNT : requestView.n();
        if (count < DEFAULT_IMAGE_COUNT) {
            return errorResponse(InferenceError.invalidRequest(
                    "The n field must be at least " + DEFAULT_IMAGE_COUNT
                            + "; omit it for one image", COUNT_PARAM));
        }

        // Images are priced per image, so an unbounded count makes one accepted request an
        // unbounded bill. Refused rather than clamped, for the same reason as below one: silently
        // serving four images to a request for four hundred bills for work nobody can explain.
        final InferenceLimits limits = InferenceLimits.current();
        if (limits.exceedsMaxImagesPerRequest(count)) {
            return errorResponse(InferenceError.invalidRequest(
                    "The n field must be at most " + limits.maxImagesPerRequest(), COUNT_PARAM));
        }

        return generate(context, requestView.prompt(), requestView.size(), count);
    }

    /**
     * Generates the images and renders the answer.
     *
     * @param context the caller, the serving site and its configuration
     * @param prompt  what to generate
     * @param size    the size the caller asked for, passed through to the provider
     * @param count   how many images to generate
     * @return the images, or a refusal carrying a safe description of what failed
     */
    private Response generate(final ResolvedAiContext context,
                              final String prompt,
                              final String size,
                              final int count) {
        try {
            final InferenceAIClient.GeneratedImages generated =
                    InferenceAIClient.get().generateImages(context.config(), prompt, size, count);

            final List<ImageGenerationView.ImageView> data =
                    new ArrayList<>(generated.images().size());
            for (final InferenceAIClient.GeneratedImage image : generated.images()) {
                data.add(new ImageGenerationView.ImageView(
                        image.base64Data(), image.revisedPrompt()));
            }

            return Response.ok(new ImageGenerationView(
                            Instant.now().getEpochSecond(), List.copyOf(data)))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (final MultipleImagesUnsupportedException e) {
            // Caught ahead of the generic case on purpose. This is the site's provider declining
            // something it can never do, not an upstream failure: answering with a retryable 502
            // would send a standard client's back-off into retrying a request that cannot succeed
            // however long it waits. The field is named so the caller can drop it and move on.
            Logger.warn(this, "Multiple images requested from a model that supports one, on site "
                    + AiHostResolver.sanitize(context.servingSiteId()) + ": " + e.getMessage());
            return errorResponse(InferenceError.invalidRequest(
                    "The configured image model for this site can only produce one image per "
                            + "request; omit n or set it to 1", COUNT_PARAM));
        } catch (final RuntimeException e) {
            // The provider's own message can carry its endpoint, its account identifiers and
            // occasionally a fragment of the prompt, so it is logged and never returned.
            Logger.error(this, "Image generation failed for site "
                    + AiHostResolver.sanitize(context.servingSiteId()), e);
            return errorResponse(InferenceError.upstream(UPSTREAM_FAILURE_MESSAGE));
        }
    }

    /**
     * Refuses a request whose model the resolved site has not configured <em>for images</em>.
     *
     * <p>Applied to every caller, administrators included. The check is not about privilege — it is
     * what stops a site's credentials being spent on a model its owner never chose for this
     * operation. A site's chat and embeddings models are both refused here, however well
     * configured they are elsewhere on the same site.</p>
     *
     * @param context     the caller, the serving site and its configuration
     * @param requestView the inbound payload
     * @return a refusal, or null when the requested model is configured
     */
    private Response refuseUnconfiguredModel(final ResolvedAiContext context,
                                             final ImageGenerationRequestView requestView) {

        final String requestedModel = requestView == null ? null : requestView.model();
        if (StringUtils.isBlank(requestedModel)) {
            return errorResponse(InferenceError.invalidRequest(
                    "The model field is required; there is no implicit default model", "model"));
        }

        final List<String> configuredModels =
                LangChain4jAIClient.get().configuredModels(context.config(), IMAGE_SECTION);
        if (!configuredModels.contains(requestedModel.trim())) {
            Logger.warn(this, "Site " + AiHostResolver.sanitize(context.servingSiteId())
                    + " has no image model matching the requested one");
            return errorResponse(InferenceError.noSuchModel(echoable(requestedModel)));
        }

        return null;
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
