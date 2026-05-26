package com.dotcms.rest.api.v1.analytics.event;

import com.dotcms.jitsu.validators.AnalyticsValidator.AnalyticsValidationException;
import com.dotcms.jitsu.validators.SiteAuthValidator;
import com.dotcms.jitsu.validators.ValidationErrorCode;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.server.JSONP;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.dotmarketing.util.Constants.DONT_RESPECT_FRONT_END_ROLES;

/**
 * REST proxy resource that intercepts requests to {@code /v1/analytics/**} and forwards them to
 * the {@code dot-ca-event-manager} service at {@code /v1/**} (stripping the {@code /analytics}
 * segment). Requests under {@code /v1/analytics/event/**} are validated using
 * {@link SiteAuthValidator} before being forwarded. All other GET requests require an authenticated
 * backend user.
 *
 * <p>All proxying mechanics (URL building, auth header, HTTP call, response mapping) are handled
 * by {@link EventAnalyticsProxyHelper}.
 *
 * @author dotCMS
 * @since 2026
 */
@Path("/v1/analytics")
@Tag(name = "Content Analytics",
        description = "Proxy endpoints that forward analytics requests to the dot-ca-event-manager service.")
public class EventAnalyticsProxyResource {

    private final WebResource webResource;

    @Inject
    public EventAnalyticsProxyResource() {
        this(new WebResource());
    }

    @VisibleForTesting
    public EventAnalyticsProxyResource(final WebResource webResource) {
        this.webResource = webResource;
    }

    /**
     * Event ingest proxy. Validates the {@code site_auth} field in the JSON body via
     * {@link SiteAuthValidator}, resolves the current site, and asynchronously forwards
     * the body to the upstream event manager's ingest endpoint. The dotCMS-side path is
     * a fixed match (no sub-paths); the upstream path is hard-coded to {@code event/ingest}.
     *
     * <p>Example routing:
     * <pre>
     *   POST /api/v1/analytics/content/event   {"context": {"site_auth": "..."}, "events": [...]}
     *     → POST {DOT_ANALYTICS_BASE_URL}/v1/event/ingest   {body with site_id injected}
     * </pre>
     *
     * <p>Read-side endpoints (e.g. {@code total-events}, {@code unique-visitors}) are GETs
     * served by {@link #proxyGetRequest} via the catch-all {@code /v1/analytics/{path:.*}}.
     *
     * @param request        the HTTP servlet request
     * @param response       the HTTP servlet response
     * @param asyncResponse  async response handle
     * @param uriInfo        URI info used to forward query parameters
     * @param body           JSON request body to forward upstream
     */
    @Operation(
            operationId = "proxyAnalyticsEventRequest",
            summary = "Proxy analytics event ingest with site auth validation",
            description = "Validates the site_auth field in the JSON body via SiteAuthValidator, " +
                    "resolves the active site, injects site_id into context, and asynchronously " +
                    "forwards the body to {DOT_ANALYTICS_BASE_URL}/v1/event/ingest. " +
                    "The dotCMS-side path is a fixed match; the upstream sub-path is not " +
                    "caller-controllable.",
            tags = {"Content Analytics"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful upstream response",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "object",
                                    description = "dotCMS response envelope containing the upstream analytics data"))),
            @ApiResponse(responseCode = "400", description = "Invalid siteAuth or upstream returned an error",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error or upstream unreachable",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/content/event")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void proxyEventRequest(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Suspended final AsyncResponse asyncResponse,
            @Context final UriInfo uriInfo,
            @Parameter(description = "JSON request body with a `context.site_auth` value and event payload",
                    required = true)
            final String body) {

        if (body == null || body.isBlank()) {
            asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ResponseEntityView<>(
                            List.of(new ErrorEntity(ValidationErrorCode.INVALID_JSON.name(),
                                    "Request body is required"))))
                    .build());
            return;
        }

        String proxyBody = body;
        Host site = null;
        try {
            final Map<String, Object> bodyMap = JsonUtil.getJsonFromString(body);
            // The literal JSON token "null" parses to a Java null — guard before deref.
            if (bodyMap == null) {
                asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ResponseEntityView<>(
                                List.of(new ErrorEntity(ValidationErrorCode.INVALID_JSON.name(),
                                        "Request body must be a JSON object"))))
                        .build());
                return;
            }
            Object context = bodyMap.get("context");

            if (context == null) {
                Logger.warn(this, "Context is required");
                asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ResponseEntityView<>(
                                List.of(new ErrorEntity(ValidationErrorCode.INVALID_SITE_AUTH.name(), "SiteAuth is required"))))
                        .build());
                return;
            }

            if (!(context instanceof Map)) {
                Logger.warn(this, "\"context\" must be a JSON object");
                asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ResponseEntityView<>(
                                List.of(new ErrorEntity(ValidationErrorCode.INVALID_JSON.name(), "\"context\" must be a JSON object"))))
                        .build());
                return;
            }

            @SuppressWarnings("unchecked")
            final Map<String, Object> contextMap = (Map<String, Object>) context;
            final Object siteAuth = contextMap.get("site_auth");

            if (siteAuth == null) {
                Logger.warn(this, "SiteAuth is required");
                asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ResponseEntityView<>(
                                List.of(new ErrorEntity(ValidationErrorCode.INVALID_SITE_AUTH.name(), "SiteAuth is required"))))
                        .build());
                return;
            }

            new SiteAuthValidator().validate(siteAuth.toString());

            site = ContentAnalyticsUtil.getSiteFromRequest(request);
            contextMap.put("site_id", site.getIdentifier());
            proxyBody = JsonUtil.getJsonStringFromObject(bodyMap);
        } catch (final AnalyticsValidationException e) {
            Logger.warn(this, "SiteAuth validation failed for analytics proxy: " + e.getMessage());
            asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ResponseEntityView<>(
                            List.of(new ErrorEntity(e.getCode().name(), e.getMessage()))))
                    .build());
            return;
        } catch (IOException | IllegalArgumentException e) {
            Logger.warn(this, "Malformed body for analytics proxy: " + e.getMessage());
            asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ResponseEntityView<>(
                            List.of(new ErrorEntity(ValidationErrorCode.INVALID_JSON.name(), e.getMessage()))))
                    .build());
            return;
        }

        final String finalProxyBody = proxyBody;
        final Host finalSite = site;
        ResponseUtil.handleAsyncResponse(
                () -> EventAnalyticsProxyHelper.proxy("event/ingest", uriInfo, finalProxyBody,
                        request.getHeader("User-Agent"), finalSite),
                asyncResponse);
    }

    /**
     * Catch-all proxy endpoint for GET requests to any path under {@code /v1/analytics/**} not
     * handled by a more specific endpoint. Requires an authenticated backend user.
     *
     * <p>Example routing:
     * <pre>
     *   GET /v1/analytics/event/top-content?limit=50
     *     → GET {DOT_CA_EVENT_MANAGER_BASE_URL}/v1/event/top-content?limit=50
     * </pre>
     *
     * @param request  the HTTP servlet request
     * @param response the HTTP servlet response
     * @param uriInfo  URI info used to forward query parameters
     * @param path     path segment(s) after {@code /v1/analytics/}
     * @return proxied response wrapped in the dotCMS response envelope
     */
    @Operation(
            operationId = "proxyAnalyticsGetRequest",
            summary = "Proxy any analytics GET request",
            description = "Forwards any authenticated GET request to /v1/analytics/** to the " +
                    "dot-ca-event-manager service at /v1/**, preserving all query parameters. " +
                    "Requests are site-permission gated: the caller must have READ on the " +
                    "site resolved from ?siteId=, ?host_id=, or the session fallback.",
            tags = {"Content Analytics"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful upstream response",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "object",
                                    description = "dotCMS response envelope containing the upstream analytics data"))),
            @ApiResponse(responseCode = "400", description = "Site identifier could not be resolved",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized – backend user required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "User lacks READ permission on the resolved site (error code SITE_ACCESS_DENIED)",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error or upstream unreachable",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/{path:.*}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response proxyGetRequest(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Context final UriInfo uriInfo,
            @Parameter(description = "Path to forward to the upstream analytics service")
            @PathParam("path") final String path) {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init()
                .getUser();

        final Host site;
        try {
            site = resolveSiteForUser(request, user);
        } catch (DotSecurityException e) {
            Logger.warn(this, "User '" + user.getUserId()
                    + "' denied access to site for analytics proxy: " + e.getMessage());
            // Match what the global DotForbiddenExceptionMapper would log — keeps denied-site
            // accesses visible in the SecurityLogger audit trail even though we're catching.
            SecurityLogger.logInfo(this.getClass(), "User '" + user.getUserId()
                    + "' denied site access on analytics proxy: " + e.getMessage());
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ResponseEntityView<>(
                            List.of(new ErrorEntity("SITE_ACCESS_DENIED",
                                    "User does not have access to the requested site"))))
                    .build();
        } catch (DotDataException e) {
            Logger.warn(this, "Failed to resolve site for analytics proxy: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ResponseEntityView<>(
                            List.of(new ErrorEntity("INVALID_SITE_ID",
                                    "Could not resolve a site for the request"))))
                    .build();
        }
        return EventAnalyticsProxyHelper.proxy(path, uriInfo, null, request.getHeader("User-Agent"), site);
    }

    /**
     * Resolves the target site for an authenticated analytics request using the logged-in
     * user, so {@link com.dotmarketing.business.PermissionAPI} gates access — a backend user
     * without READ on the requested site gets 403, even if they hand-craft a {@code siteId}
     * to bypass the (already-permissioned) site picker. Explicit {@code ?siteId=} wins over
     * the session-bound active host; the session fallback is only used when no siteId is
     * given.
     *
     * <p>Load-bearing: this MUST stay on {@code getCurrentHost(request, user)}, not
     * {@code getCurrentHostNoThrow(request)}. The user-aware overload runs
     * {@code PermissionAPI.READ} via {@code checkHostPermission}; the no-throw variant
     * skips it and would silently regress the permission gate. Inside the fallback,
     * {@code getCurrentHostFromRequest} also honors a {@code ?host_id=} parameter — same
     * permission gate applies, just a second param name.
     *
     * @throws DotSecurityException the user lacks READ permission on the resolved site
     * @throws DotDataException     the siteId could not be resolved (not found, malformed)
     */
    private Host resolveSiteForUser(final HttpServletRequest request, final User user)
            throws DotDataException, DotSecurityException {
        final String siteIdParam = request.getParameter("siteId");
        if (com.dotmarketing.util.UtilMethods.isSet(siteIdParam)) {
            final Host site = APILocator.getHostAPI()
                    .find(siteIdParam, user, DONT_RESPECT_FRONT_END_ROLES);
            if (site == null) {
                throw new DotDataException(
                        "siteId '" + siteIdParam + "' could not be resolved");
            }
            return site;
        }
        return WebAPILocator.getHostWebAPI().getCurrentHost(request, user);
    }

    @Operation(
            operationId = "generateSiteAuth",
            summary = "Generate Site Auth",
            description = "Generates and returns a Site Key that must be used by the client-side JS " +
                    "code to send custom Content Analytics Events",
            tags = {"Content Analytics"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "The Site key was generated and " +
                            "returned successfully"),
                    @ApiResponse(responseCode = "400", description = "Bad Request"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Site ID in path is not found or " +
                            "incorrect path"),
                    @ApiResponse(responseCode = "405", description = "Method Not Allowed"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    @GET
    @Path("/content/siteauth/generate/{siteId}")
    @JSONP
    @NoCache
    @Produces({MediaType.TEXT_PLAIN, "text/plain"})
    public Response generateSiteKey(@PathParam("siteId") final String siteId,
                                    @Context final HttpServletRequest request,
                                    @Context final HttpServletResponse response) throws DotDataException, DotSecurityException {
        final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init();
        final User user = initDataObject.getUser();
        final Host site = APILocator.getHostAPI().find(siteId, user, DONT_RESPECT_FRONT_END_ROLES);
        Objects.requireNonNull(site, String.format("Site with ID '%s' was not found", siteId));
        return Response.ok().entity(ContentAnalyticsUtil.generateInternalSiteKey(site.getIdentifier())).build();
    }
}