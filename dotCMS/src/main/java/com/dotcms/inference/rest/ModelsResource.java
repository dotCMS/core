package com.dotcms.inference.rest;

import com.dotcms.ai.client.langchain4j.LangChain4jAIClient;
import com.dotcms.ai.rest.AiHostResolver;
import com.dotcms.ai.rest.ResolvedAiContext;
import com.dotcms.cost.RequestCost;
import com.dotcms.cost.RequestPrices.Price;
import com.dotcms.inference.model.InferenceError;
import com.dotcms.inference.rest.view.InferenceErrorView;
import com.dotcms.inference.rest.view.ModelListView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.NoCors;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Serves {@code GET /api/inference/v1/models} — model discovery for the OpenAI-wire-format family.
 *
 * <p>This endpoint is the discovery mechanism the rest of the family depends on. There is no
 * implicit default model and no reserved alias, so the names listed here are exactly the names
 * this family accepts — a caller who wants "whatever this site runs" reads the
 * list and takes the first entry. Two consequences follow, and both are deliberate.</p>
 *
 * <ul>
 *     <li><strong>No provider is contacted.</strong> The list is read from the resolved site's
 *     configuration and nothing else. Asking a vendor what it offers would return names the site
 *     has not configured and the chat endpoint would refuse, which is worse than no list at
 *     all.</li>
 *     <li><strong>Nothing synthetic is added, and every fallback-chain entry is listed.</strong>
 *     Each entry of a chain is a name the chat endpoint accepts, so listing only the primary would
 *     hide choices the caller is entitled to make; and an invented entry would advertise a name
 *     that endpoint refuses.</li>
 * </ul>
 *
 * <p>A site with no dotAI configuration — and on an instance with none at the system level either
 * — gets an empty list with a 200. Falling back to whichever site happens to be configured would
 * hand a caller model names their own site will refuse, and would disclose that some other site
 * has dotAI set up.</p>
 *
 * <p>Nothing here is wrapped in the dotCMS {@code ResponseEntityView} envelope, and refusals are
 * rendered as {@link InferenceErrorView} rather than left to dotCMS's generic exception mappers,
 * for the reason the whole family exists: a client library has to deserialize both the answer and
 * the refusal into its own types with no adapter.</p>
 */
@Path("/inference/v1/models")
@Tag(name = "AI", description = "AI-powered content generation and analysis endpoints")
@NoCors
public class ModelsResource {

    /** Section of the site's {@code providerConfig} JSON the listed models come from. */
    private static final String CHAT_SECTION = "chat";
    private static final String EMBEDDINGS_SECTION = "embeddings";
    private static final String IMAGE_SECTION = "image";

    /**
     * The sections this listing draws from, chat first.
     *
     * <p>Order decides behaviour here, it is not presentation. There is no implicit default
     * model, and the documented way to ask for "whatever this site runs" is to read this list
     * and take the first
     * entry — so the first entry has to remain the site's primary chat model, as it was when this
     * listed nothing else.</p>
     */
    private static final List<String> LISTED_SECTIONS =
            List.of(CHAT_SECTION, EMBEDDINGS_SECTION, IMAGE_SECTION);

    /**
     * Header form of the site override. Server-side callers routinely sit behind a proxy that
     * rewrites the Host header, and a header is the only override they can set without rewriting
     * the URL a standard client library builds. It wins over the query parameter because it is
     * the more specific of the two.
     */
    private static final String SITE_HEADER = "X-dotCMS-Site";

    /**
     * Lists the models the resolved site has configured, across every capability it serves.
     *
     * @param request  the inbound request
     * @param response the outbound response, used only by the authentication handshake
     * @param siteId   optional site id or host name whose dotAI configuration should be read; the
     *                 {@code X-dotCMS-Site} header overrides it
     * @return the listing, or an {@link InferenceErrorView} refusal
     */
    @Operation(
            operationId = "listInferenceModels",
            summary = "List the models this site has configured",
            description = "Returns the models the resolved site has configured for chat, including "
                    + "every entry of a fallback chain, in configured order — the first is the "
                    + "site's primary model. The list is exactly the set of values the chat "
                    + "completions endpoint accepts as \"model\": there is no implicit default and "
                    + "no reserved alias, and nothing synthetic is added. No model provider is "
                    + "contacted. A site with no AI configuration, on an instance with none at the "
                    + "system level either, returns an empty data array rather than another site's "
                    + "models. Every response reports the serving site in the "
                    + "X-dotCMS-Resolved-Site header."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "The configured models, possibly none",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ModelListView.class))),
            @ApiResponse(responseCode = "400",
                    description = "The requested site could not be resolved",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = InferenceErrorView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = InferenceErrorView.class))),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - the caller cannot read the requested site",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = InferenceErrorView.class)))
    })
    @GET
    @JSONP
    @NoCache
    @InferenceEndpoint
    @RequestCost(Price.HTTP_FETCH)
    @Produces(MediaType.APPLICATION_JSON)
    public Response models(@Context final HttpServletRequest request,
                                 @Context final HttpServletResponse response,
                                 @QueryParam("siteId") final String siteId) {

        // Bearer only. Checked here as well as in BearerOnlyAuthFilter because the filter runs only
        // inside the JAX-RS chain, while the rule has to hold wherever this method is reached.
        // The list names the site's configured models, which is exactly the reconnaissance an
        // anonymous caller should not get for free.
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

        // Published before anything else can fail, so the response filter can name the serving site
        // on refusals as well as on the happy path.
        request.setAttribute(InferenceRequestAttributes.RESOLVED_SITE_ID, context.servingSiteId());

        return Response.ok(toView(configuredModels(context)))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Collects the model names this site has configured, across every capability it serves.
     *
     * <p>All of them, not just chat. The adopted format has one flat model list and its model
     * object carries no field for what a model is for — no type, no mode, no modality — so the
     * reference implementation returns chat, embedding and image models together and leaves a
     * client to know which is which. Listing only chat would be the narrower, tidier answer and
     * was what shipped first, but it leaves the embeddings and image operations with no discovery
     * at all: both require an exact model name, and this is the only place a caller could learn
     * one.</p>
     *
     * <p>The alternative — adding a capability field to each entry — was rejected after looking at
     * what other gateways do. Nobody extends the standard model object that way: implementations
     * either keep this endpoint's shape and expose capability on a separate, non-standard one, or
     * publish an entirely different object of their own. Inventing a field inside the standard
     * shape is the one thing none of them does, and it is exactly what the no-adapter promise of
     * this family exists to avoid.</p>
     *
     * <p>A name configured in more than one section is listed once. Duplicates would be harmless
     * to a client but would misrepresent the site as running two things.</p>
     *
     * @param context the resolved site and its configuration
     * @return the configured names, chat first, in configured order, without repeats
     */
    private static List<String> configuredModels(final ResolvedAiContext context) {
        final LinkedHashSet<String> names = new LinkedHashSet<>();
        for (final String section : LISTED_SECTIONS) {
            names.addAll(LangChain4jAIClient.get().configuredModels(context.config(), section));
        }
        return List.copyOf(names);
    }

    /**
     * Wraps the configured model names in the listing shape.
     *
     * <p>Every entry reports the same creation time, read once here. dotCMS serves a
     * configuration, not a catalogue: it does not know when a vendor published a model, and
     * inventing a plausible per-model date would be indistinguishable from a real one to anyone
     * who trusted it.</p>
     *
     * @param modelNames the configured names, in fallback order
     * @return the listing
     */
    private static ModelListView toView(final List<String> modelNames) {
        final long created = Instant.now().getEpochSecond();
        final List<ModelListView.ModelView> models = new ArrayList<>(modelNames.size());
        for (final String modelName : modelNames) {
            models.add(new ModelListView.ModelView(
                    modelName,
                    ModelListView.ModelView.OBJECT,
                    created,
                    ModelListView.ModelView.OWNED_BY));
        }
        return new ModelListView(ModelListView.OBJECT, List.copyOf(models));
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
