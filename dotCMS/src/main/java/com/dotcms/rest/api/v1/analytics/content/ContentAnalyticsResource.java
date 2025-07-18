package com.dotcms.rest.api.v1.analytics.content;

import com.dotcms.analytics.content.ContentAnalyticsAPI;
import com.dotcms.analytics.content.ContentAnalyticsQuery;
import com.dotcms.analytics.content.ReportResponse;
import com.dotcms.analytics.model.ResultSetItem;
import com.dotcms.experiments.business.ConfigExperimentUtil;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.analytics.content.util.AnalyticsEventsResult;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.Lazy;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.dotcms.util.DotPreconditions.checkArgument;
import static com.dotcms.util.DotPreconditions.checkNotNull;
import static com.dotmarketing.util.Constants.DONT_RESPECT_FRONT_END_ROLES;

/**
 * Resource class that exposes endpoints to query content analytics data.
 * This REST Endpoint exposes different operations to query Content Analytics data. Content
 * Analytics will enable customers to track the health and engagement of their content at the level
 * of individual content items.
 *
 * @author Jose Castro
 * @since Sep 13th, 2024
 */
@Path("/v1/analytics/content")
@Tag(name = "Content Analytics",
        description = "This REST Endpoint exposes information related to how dotCMS content is accessed and interacted with by users.")
public class ContentAnalyticsResource {

    private final WebResource webResource;
    private final ContentAnalyticsAPI contentAnalyticsAPI;

    @Inject
    public ContentAnalyticsResource(final ContentAnalyticsAPI contentAnalyticsAPI) {
        this(new WebResource(), contentAnalyticsAPI);
    }

    @VisibleForTesting
    public ContentAnalyticsResource(final WebResource webResource,
                                    final ContentAnalyticsAPI contentAnalyticsAPI) {
        this.webResource = webResource;
        this.contentAnalyticsAPI = contentAnalyticsAPI;
    }

    /**
     * Query Content Analytics data.
     *
     * @param request   the HTTP request.
     * @param response  the HTTP response.
     * @param queryForm the query form.
     * @return the report response entity view.
     */
    @Operation(
            operationId = "postContentAnalyticsQuery",
            summary = "Retrieve Content Analytics data",
            description = "Returns information of specific dotCMS objects whose health and " +
                    "engagement data is tracked. This method takes a specific less verbose JSON " +
                    "format to query the data.",
            tags = {"Content Analytics"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Content Analytics data being queried",
                            content = @Content(mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    value = "{\n" +
                                                            "    \"query\": {\n" +
                                                            "        \"measures\": [\n" +
                                                            "            \"request.count\"\n" +
                                                            "        ],\n" +
                                                            "        \"order\": \"request.count DESC\",\n" +
                                                            "        \"dimensions\": [\n" +
                                                            "            \"request.url\",\n" +
                                                            "            \"request.pageId\",\n" +
                                                            "            \"request.pageTitle\"\n" +
                                                            "        ],\n" +
                                                            "        \"filters\": \"request.whatAmI = ['PAGE']\",\n" +
                                                            "        \"limit\": 100,\n" +
                                                            "        \"offset\": 1\n" +
                                                            "    }\n" +
                                                            "}"
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad Request"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    @POST
    @Path("/_query")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ReportResponseEntityView query(@Context final HttpServletRequest request,
                                          @Context final HttpServletResponse response,
                                          final QueryForm queryForm) {

        final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init();

        final User user = initDataObject.getUser();
        checkNotNull(queryForm, IllegalArgumentException.class, "The 'query' JSON data cannot be null");
        Logger.debug(this, () -> "Querying content analytics data with the form: " + queryForm);
        final ReportResponse reportResponse =
                this.contentAnalyticsAPI.runReport(queryForm.getQuery(), user);
        return new ReportResponseEntityView(reportResponse.getResults().stream().map(ResultSetItem::getAll).collect(Collectors.toList()));
    }

    /**
     * Query Content Analytics data.
     *
     * @param request   the HTTP request.
     * @param response  the HTTP response.
     * @param cubeJsQueryJson the query form.
     * @return the report response entity view.
     */
    @Operation(
            operationId = "postContentAnalyticsQuery",
            summary = "Retrieve Content Analytics data",
            description = "Returns information of specific dotCMS objects whose health and " +
                    "engagement data is tracked, using a CubeJS JSON query.",
            tags = {"Content Analytics"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Content Analytics data " +
                            "being queried",
                            content = @Content(mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    value = "{\n" +
                                                            "    \"dimensions\": [\n" +
                                                            "        \"Events.experiment\",\n" +
                                                            "        \"Events.variant\"\n" +
                                                            "    ]\n" +
                                                            "}"
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad Request"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    @POST
    @Path("/_query/cube")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ReportResponseEntityView queryCubeJs(@Context final HttpServletRequest request,
                                          @Context final HttpServletResponse response,
                                          final String cubeJsQueryJson) {

        final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init();

        final User user = initDataObject.getUser();
        checkNotNull(cubeJsQueryJson, IllegalArgumentException.class, "The 'query' JSON data cannot be null");
        Logger.debug(this,  ()->"Querying content analytics data with the cube query json: " + cubeJsQueryJson);
        final ReportResponse reportResponse =
                this.contentAnalyticsAPI.runRawReport(cubeJsQueryJson, user);
        return new ReportResponseEntityView(reportResponse.getResults().stream().map(ResultSetItem::getAll).collect(Collectors.toList()));
    }


    /**
     * Returns information of specific dotCMS objects whose health and engagement data is tracked,
     * using simplified version of a query sent to the Content Analytics service. This helps
     * abstract the complexity of the underlying JSON format for users that need an easier way to
     * query for specific data.
     *
     * @param request  The current instance of the {@link HttpServletRequest} object.
     * @param response The current instance of the {@link HttpServletResponse} object.
     * @param form     A Map with the parameters that will be used to generate the query
     *                 internally.
     *
     * @return The request information from the Content Analytics server.
     */
    @Operation(
            operationId = "postContentAnalyticsSimpleQuery",
            summary = "Returns Content Analytics data",
            description = "Returns information of specific dotCMS objects whose health and " +
                    "engagement data is tracked, using Path Parameters instead of a CubeJS JSON " +
                    "query. This helps abstract the complexity of the underlying JSON format for " +
                    "users that need an easier way to query for specific data.",
            tags = {"Content Analytics"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Content Analytics data " +
                            "being queried",
                            content = @Content(mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    value = "{\n" +
                                                            "    \"measures\": \"request.count request.totalSessions\",\n" +
                                                            "    \"dimensions\": \"request.host request.whatAmI request.url\",\n" +
                                                            "    \"timeDimensions\": \"request.createdAt:day:Last month\",\n" +
                                                            "    \"filters\": \"request.totalRequest gt 0:request.whatAmI contains PAGE,FILE\",\n" +
                                                            "    \"order\": \"request.count asc:request.createdAt asc\",\n" +
                                                            "    \"limit\": 15,\n" +
                                                            "    \"offset\": 0\n" +
                                                            "}"
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad Request"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ReportResponseEntityView query(@Context final HttpServletRequest request,
                                          @Context final HttpServletResponse response,
                                          final Map<String, Object> form) {
        final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init();
        final User user = initDataObject.getUser();
        Logger.debug(this, () -> "Querying content analytics data with the following parameters: " + form);
        checkNotNull(form, IllegalArgumentException.class, "The 'form' JSON data cannot be null");
        checkArgument(!form.isEmpty(), IllegalArgumentException.class, "The 'form' JSON data cannot be empty");
        final ContentAnalyticsQuery contentAnalyticsQuery = new ContentAnalyticsQuery.Builder().build(form);
        final String cubeJsQuery = JsonUtil.getJsonStringFromObject(contentAnalyticsQuery);
        Logger.debug(this, () -> "Generated query: " + cubeJsQuery);
        final ReportResponse reportResponse = this.contentAnalyticsAPI.runRawReport(cubeJsQuery, user);
        return new ReportResponseEntityView(reportResponse.getResults()
                .stream().map(ResultSetItem::getAll).collect(Collectors.toList()));
    }

    /**
     * Fire an user custom event.
     *
     * @param request   the HTTP request.
     * @param response  the HTTP response.
     * @param userEventPayload the query form.
     * @return the report response entity view.
     */
    @Operation(
            operationId = "fireUserCustomEvent",
            summary = "Fire an user custom event.",
            description = "receives a custom event payload and fires the event to the collectors",
            tags = {"Content Analytics"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "If the event was created successfully",
                            content = @Content(mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    value = "TBD"
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad Request"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    @POST
    @Path("/event")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response fireUserCustomEvent(@Context final HttpServletRequest request,
                                                @Context final HttpServletResponse response,
                                                final Map<String, Serializable> userEventPayload) throws DotSecurityException {
        checkNotNull(userEventPayload, IllegalArgumentException.class, "The 'userEventPayload' JSON cannot be null");
        new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .requiredAnonAccess(AnonymousAccess.READ)
                .rejectWhenNoUser(false)
                .init();
        Logger.debug(this,  ()->"Creating an user custom event with the payload: " + userEventPayload);

        final AnalyticsEventsResult analyticsEventsResult =
                ContentAnalyticsUtil.registerContentAnalyticsRestEvent(request, userEventPayload);

        return Response.status(getResponseStatus(analyticsEventsResult)).entity(analyticsEventsResult).build();
    }

    @Operation(
            operationId = "generateSiteKey",
            summary = "Generate Site Key",
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
    @Path("/sitekey/generate/{siteId}")
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

    private int getResponseStatus(final AnalyticsEventsResult analyticsEventsResult) {
        return analyticsEventsResult.getStatus() == AnalyticsEventsResult.ResponseStatus.ERROR ? 400
                : analyticsEventsResult.getStatus() == AnalyticsEventsResult.ResponseStatus.SUCCESS ? 200
                : 207;
    }

    // Isnt valid if the payload does not contain the key or the key is different from the one in the site
    private boolean isNotValidKey(final Map<String, Serializable> userEventPayload, final Host site) {

        return !userEventPayload.containsKey("key") || !ConfigExperimentUtil.INSTANCE.getAnalyticsKey(site).equals(userEventPayload.get("key"));
    }

}
