package com.dotcms.rest.api.v1.analytics.content;

import com.dotcms.analytics.content.ContentAnalyticsAPI;
import com.dotcms.analytics.content.ReportResponse;
import com.dotcms.analytics.model.ResultSetItem;
import com.dotcms.analytics.track.collectors.Collector;
import com.dotcms.analytics.track.collectors.EventSource;
import com.dotcms.analytics.track.collectors.WebEventsCollectorServiceFactory;
import com.dotcms.analytics.track.matchers.FilesRequestMatcher;
import com.dotcms.analytics.track.matchers.PagesAndUrlMapsRequestMatcher;
import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotcms.analytics.track.matchers.UserCustomDefinedRequestMatcher;
import com.dotcms.analytics.track.matchers.VanitiesRequestMatcher;
import com.dotcms.experiments.business.ConfigExperimentUtil;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.analytics.track.collectors.EventType;

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

    private static final Map<String, Supplier<RequestMatcher>> MATCHER_MAP = Map.of(
            EventType.FILE_REQUEST.getType(), FilesRequestMatcher::new,
            EventType.PAGE_REQUEST.getType(), PagesAndUrlMapsRequestMatcher::new,
            EventType.URL_MAP.getType(), PagesAndUrlMapsRequestMatcher::new,
            EventType.VANITY_REQUEST.getType(), VanitiesRequestMatcher::new
    );

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
                                                final Map<String, Serializable> userEventPayload) throws DotSecurityException {

        DotPreconditions.checkNotNull(userEventPayload, IllegalArgumentException.class, "The 'userEventPayload' JSON cannot be null");
        if (userEventPayload.containsKey(Collector.EVENT_SOURCE)) {
            throw new IllegalArgumentException("The 'event_source' field is reserved and cannot be used");
        }

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .requiredAnonAccess(AnonymousAccess.READ)
                .rejectWhenNoUser(false)
                .init().getUser();

        if (user.isAnonymousUser() && isNotValidKey(userEventPayload, WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request))) {
            throw new DotSecurityException("The user is not allowed to fire an event");
        }

        Logger.debug(this,  ()->"Creating an user custom event with the payload: " + userEventPayload);
        request.setAttribute("requestId", Objects.nonNull(request.getAttribute("requestId")) ? request.getAttribute("requestId") : UUIDUtil.uuid());
        final Map<String, Serializable> userEventPayloadWithDefaults = new HashMap<>(userEventPayload);
        userEventPayloadWithDefaults.put(Collector.EVENT_SOURCE, EventSource.REST_API.getName());
        userEventPayloadWithDefaults.put(Collector.EVENT_TYPE,   userEventPayload.getOrDefault(Collector.EVENT_TYPE, EventType.CUSTOM_USER_EVENT.getType()));
        WebEventsCollectorServiceFactory.getInstance().getWebEventsCollectorService().fireCollectorsAndEmitEvent(request, response,
                loadRequestMatcher(userEventPayload), userEventPayloadWithDefaults, fromPayload(userEventPayload));

        return new ResponseEntityStringView("User event created successfully");
    }

    // Isnt valid if the payload does not contain the key or the key is different from the one in the site
    private boolean isNotValidKey(final Map<String, Serializable> userEventPayload, final Host site) {

        return !userEventPayload.containsKey("key") || !ConfigExperimentUtil.INSTANCE.getAnalyticsKey(site).equals(userEventPayload.get("key"));
    }

    private Map<String, Object> fromPayload(final Map<String, Serializable> userEventPayload) {
        final Map<String, Object> baseContextMap = new HashMap<>();

        if (userEventPayload.containsKey("url")) {

            baseContextMap.put("uri", userEventPayload.get("url"));
        }

        if (userEventPayload.containsKey("doc_path")) {

            baseContextMap.put("uri", userEventPayload.get("doc_path"));
        }

        return baseContextMap;
    }

    private RequestMatcher loadRequestMatcher(final Map<String, Serializable> userEventPayload) {

        String eventType = (String) userEventPayload.getOrDefault(Collector.EVENT_TYPE, EventType.CUSTOM_USER_EVENT.getType());
        return MATCHER_MAP.getOrDefault(eventType, () -> USER_CUSTOM_DEFINED_REQUEST_MATCHER).get();
    }

}
