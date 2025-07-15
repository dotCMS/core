package com.dotcms.rest.api.v1.event;

import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.web.WebSessionContext;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.system.AppContext;
import com.dotcms.system.CompositeAppContext;
import com.dotcms.system.SimpleMapAppContext;
import com.dotcms.util.LongPollingService;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * This resource is an alternative fallback to get events when the websockets @{@link com.dotcms.rest.api.v1.system.websocket.SystemEventsWebSocketEndPoint}
 * is not able to be called by any reason.
 * The @{@link EventsResource} use the long polling approach to get all the events, the call basically will wait for N seconds
 * (use the property system.events.longpolling.seconds, on the dotmarketing-config-ext.properties to custom the seconds, by default it is 15 seconds)
 *
 * @author jsanca
 * @version 3.7
 * @since Jul 7, 2016
 */
@SuppressWarnings("serial")
@SwaggerCompliant(value = "Modern APIs and specialized services", batch = 7)
@Tag(name = "Administration")
@Path("/ws/v1/system")
public class EventsResource implements Serializable {

    public static final String SYSTEM_EVENT_LONGPOLLING_DEFAULTMILLIS = "system.events.longpolling.defaultmillis";

    private final long timeoutSeconds;
    private final WebResource webResource;
    private final LongPollingService longPollingService;
    private final MarshalUtils marshalUtils;


    /**
     * Default constructor.
     */
    public EventsResource() {
        this(new WebResource(),
                Config.getLongProperty(SYSTEM_EVENT_LONGPOLLING_DEFAULTMILLIS, 15000)*2/1000, // the timeout for a asyn response will be the double of the long polling and in seconds.
                new LongPollingService
                (Config.getLongProperty(SYSTEM_EVENT_LONGPOLLING_DEFAULTMILLIS, 15000),
                        new SystemEventsDelegate()),
                MarshalFactory.getInstance().getMarshalUtils());
    }

    @VisibleForTesting
    protected EventsResource(final WebResource webResource,
                             final long timeoutSeconds,
                             final LongPollingService longPollingService,
                             final MarshalUtils marshalUtils) {

        this.webResource                  = webResource;
        this.timeoutSeconds               = timeoutSeconds;
        this.longPollingService           = longPollingService;
        this.marshalUtils                 = marshalUtils;
    }


    @Operation(
        summary = "Get synchronous system events",
        description = "Retrieves system events synchronously using long polling. Alternative to WebSocket when WebSocket connection is not available. Returns immediately with available events."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "System events retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntitySystemEventsView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/syncevents")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getSyncEvents(@Context final HttpServletRequest httpServletRequest,
                                        @Context final HttpServletResponse httpServletResponse,
                                        @Parameter(description = "Timestamp of last callback to filter events (optional)") @QueryParam("lastcallback") Long lastCallback) {


        Response response              = null;

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .rejectWhenNoUser(true).init();

        final AppContext appContext    =  WebSessionContext.getInstance(httpServletRequest);
        List<SystemEvent> systemEvents = null;

        try {

            if (null != initData.getUser()) {

                Logger.debug(this, "Getting syncr system events with a lastcallback as: " + lastCallback);
                appContext.setAttribute(SystemEventsDelegate.LAST_CALLBACK, (null != lastCallback)?lastCallback:System.currentTimeMillis());
                appContext.setAttribute(SystemEventsDelegate.DO_MARSHALL,   false);
                appContext.setAttribute(SystemEventsDelegate.USER,   initData.getUser());

                this.longPollingService.execute(appContext);

                systemEvents = appContext.getAttribute(SystemEventsDelegate.RESULT);

                response = Response.ok(marshalUtils.marshal(new ResponseEntitySystemEventsView(systemEvents))).build();
            }
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // getSyncEvents.

    @Operation(
        summary = "Get asynchronous system events",
        description = "Establishes an asynchronous long polling connection to receive system events. Connection will timeout after configured seconds (default 30s). Uses suspended AsyncResponse for efficient resource utilization."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "System events retrieved successfully via async response",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntitySystemEventsView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "503", 
                    description = "Service unavailable - operation timeout",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/events")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final void getEvents(@Context final HttpServletRequest httpServletRequest,
                                @Context final HttpServletResponse httpServletResponse,
                                @Suspended final AsyncResponse asyncResponse,
                                @Parameter(description = "Timestamp of last callback to filter events (optional)") @QueryParam("lastcallback") Long lastCallback) {
        Response response;
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .rejectWhenNoUser(true).init();

        final AppContext simpleAppContext =  new SimpleMapAppContext();
        final AppContext webAppContext    =  WebSessionContext.getInstance(httpServletRequest);

        try {

            if (null != initData.getUser()) {

                Logger.debug(this, "Getting asyncr system events with a lastcallback as: " + lastCallback);
                asyncResponse.setTimeoutHandler(new EventTimeoutHandler(initData.getUser().getLocale()));
                asyncResponse.setTimeout(this.timeoutSeconds, TimeUnit.SECONDS);

                Logger.debug(this, "Getting syncr system events with a lastcallback as: " + lastCallback);
                webAppContext.setAttribute(SystemEventsDelegate.LAST_CALLBACK, lastCallback != null ? lastCallback :
                        System.currentTimeMillis());

                webAppContext.setAttribute(SystemEventsDelegate.LAST_CALLBACK, (null != lastCallback)?lastCallback:System.currentTimeMillis());
                webAppContext.setAttribute(SystemEventsDelegate.USER,   initData.getUser());
                // The AsyncResponse is not serializable. So, it can't be stored in the Session because it causes
                // problems with Long Polling when using the Redis Session Manager feature
                simpleAppContext.setAttribute(SystemEventsDelegate.RESPONSE, asyncResponse);
                this.longPollingService.executeAsync(new CompositeAppContext(webAppContext, webAppContext, simpleAppContext));
            }
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            Logger.error(this, e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
            asyncResponse.resume(response);
        }
    } // getEvents.

    private static class EventTimeoutHandler implements TimeoutHandler {

        private final Locale locale;

        public EventTimeoutHandler(final Locale locale) {
            this.locale = locale;
        }

        @Override
        public void handleTimeout(final AsyncResponse asyncResponse) {

            String message = "Operation time out.";

            try {

                message = LanguageUtil.get(locale, "operation-timeout");
            } catch (LanguageException e) {
                message = "Operation time out.";
            }

            Logger.debug(this, "Operation time out for a asyn response on Events long polling");
            final Response response = Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(
                    new ResponseEntityEventErrorView(Arrays.asList(new ErrorEntity("operation-timeout", message)))).build();

            asyncResponse.resume(response);
        }
    }
} // E:O:F:EventsResource.
