package com.dotcms.rest.api.v1.analytics.event;

import com.dotcms.jitsu.validators.AnalyticsValidator.AnalyticsValidationException;
import com.dotcms.jitsu.validators.SiteAuthValidator;
import com.dotcms.jitsu.validators.ValidationErrorCode;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
     * Proxy endpoint for POST requests under {@code /v1/analytics/event/**}. Validates the
     * {@code siteAuth} query parameter via {@link SiteAuthValidator} before asynchronously
     * forwarding the request (with body) to the upstream analytics service.
     *
     * <p>Example routing:
     * <pre>
     *   POST /v1/analytics/event/total-events?siteAuth=xxx  {body}
     *     → POST {DOT_CA_EVENT_MANAGER_BASE_URL}/v1/event/total-events?siteAuth=xxx  {body}
     * </pre>
     *
     * @param request        the HTTP servlet request
     * @param response       the HTTP servlet response
     * @param asyncResponse  async response handle
     * @param uriInfo        URI info used to forward query parameters
     * @param body           JSON request body to forward upstream
     */
    @Operation(
            operationId = "proxyAnalyticsEventRequest",
            summary = "Proxy analytics event POST request with site auth validation",
            description = "Validates the siteAuth query parameter and asynchronously forwards " +
                    "POST requests to /v1/analytics/event/** to the dot-ca-event-manager service " +
                    "at /v1/event/**. All query parameters and the request body are preserved.",
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
    @Path("/event/ingest")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void proxyEventRequest(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Suspended final AsyncResponse asyncResponse,
            @Context final UriInfo uriInfo,
            @Parameter(description = "Sub-path after /event/")
            final String body) {

        try {
            final Map<String, Object> bodyMap = JsonUtil.getJsonFromString(body);
            Object context = bodyMap.get("context");

            if (context == null) {
                Logger.warn(this, "Context is required");
                asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ResponseEntityView<>(
                                List.of(new ErrorEntity(ValidationErrorCode.INVALID_SITE_AUTH.name(), "SiteAuth is required"))))
                        .build());
                return;
            }

            Object siteAuth = ((Map<String, Object>) context).get("site_auth");

            if (siteAuth == null) {
                Logger.warn(this, "SiteAuth is required");
                asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ResponseEntityView<>(
                                List.of(new ErrorEntity(ValidationErrorCode.INVALID_SITE_AUTH.name(), "SiteAuth is required"))))
                        .build());
                return;
            }

            new SiteAuthValidator().validate(siteAuth.toString());
        } catch (final AnalyticsValidationException e) {
            Logger.warn(this, "SiteAuth validation failed for analytics proxy: " + e.getMessage());
            asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ResponseEntityView<>(
                            List.of(new ErrorEntity(e.getCode().name(), e.getMessage()))))
                    .build());
            return;
        } catch (IOException e) {
            Logger.warn(this, "SiteAuth validation failed for analytics proxy: " + e.getMessage());
            asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ResponseEntityView<>(
                            List.of(new ErrorEntity(ValidationErrorCode.INVALID_JSON.name(), e.getMessage()))))
                    .build());
            return;
        }

        ResponseUtil.handleAsyncResponse(
                () -> EventAnalyticsProxyHelper.proxy("event/ingest", uriInfo, body),
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
                    "dot-ca-event-manager service at /v1/**, preserving all query parameters.",
            tags = {"Content Analytics"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful upstream response",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "object",
                                    description = "dotCMS response envelope containing the upstream analytics data"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized – backend user required",
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

        new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init();

        return EventAnalyticsProxyHelper.proxy(path, uriInfo, null);
    }

}