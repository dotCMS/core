package com.dotcms.rest.api.v1.analytics.content;

import com.dotcms.analytics.content.ContentAnalyticsAPI;
import com.dotcms.analytics.content.ReportResponse;
import com.dotcms.analytics.model.ResultSetItem;
import com.dotcms.analytics.track.collectors.WebEventsCollectorServiceFactory;
import com.dotcms.analytics.track.matchers.UserCustomDefinedRequestMatcher;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
        description = "Endpoints that exposes information related to how dotCMS content is accessed and interacted with by users.")
public class ContentAnalyticsResource {

    private static final UserCustomDefinedRequestMatcher USER_CUSTOM_DEFINED_REQUEST_MATCHER =  new UserCustomDefinedRequestMatcher();

    private final WebResource webResource;
    private final ContentAnalyticsAPI contentAnalyticsAPI;

    @SuppressWarnings("unused")
    public ContentAnalyticsResource() {
        this(CDIUtils.getBean(ContentAnalyticsAPI.class).orElseGet(APILocator::getContentAnalyticsAPI));
    }

    //@Inject
    @VisibleForTesting
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
            description = "Returns information of specific dotCMS objects whose health and engagement data is tracked.",
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
        DotPreconditions.checkNotNull(queryForm, IllegalArgumentException.class, "The 'query' JSON data cannot be null");
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
            description = "Returns information of specific dotCMS objects whose health and engagement data is tracked.",
            tags = {"Content Analytics"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Content Analytics data being queried",
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
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
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
        DotPreconditions.checkNotNull(cubeJsQueryJson, IllegalArgumentException.class, "The 'query' JSON data cannot be null");
        Logger.debug(this,  ()->"Querying content analytics data with the cube query json: " + cubeJsQueryJson);
        final ReportResponse reportResponse =
                this.contentAnalyticsAPI.runRawReport(cubeJsQueryJson, user);
        return new ReportResponseEntityView(reportResponse.getResults().stream().map(ResultSetItem::getAll).collect(Collectors.toList()));
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
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    @POST
    @Path("/event")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityStringView fireUserCustomEvent(@Context final HttpServletRequest request,
                                                @Context final HttpServletResponse response,
                                                final Map<String, Serializable> userEventPayload) {

        new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        DotPreconditions.checkNotNull(userEventPayload, IllegalArgumentException.class, "The 'userEventPayload' JSON cannot be null");
        DotPreconditions.checkNotNull(userEventPayload.get("event_type"), IllegalArgumentException.class, "The 'event_type' field is required");
        Logger.debug(this,  ()->"Creating an user custom event with the payload: " + userEventPayload);
        request.setAttribute("requestId", Objects.nonNull(request.getAttribute("requestId")) ? request.getAttribute("requestId") : UUIDUtil.uuid());
        WebEventsCollectorServiceFactory.getInstance().getWebEventsCollectorService().fireCollectorsAndEmitEvent(request, response, USER_CUSTOM_DEFINED_REQUEST_MATCHER, userEventPayload);
        return new ResponseEntityStringView("User event created successfully");
    }

}
