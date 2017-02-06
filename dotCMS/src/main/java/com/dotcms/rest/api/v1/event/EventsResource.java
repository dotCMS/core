package com.dotcms.rest.api.v1.event;

import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.web.WebSessionContext;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.container.AsyncResponse;
import com.dotcms.repackage.javax.ws.rs.container.Suspended;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.system.AppContext;
import com.dotcms.system.SimpleMapAppContext;
import com.dotcms.util.LongPollingService;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * This resource is an alternative fallback to get events when the websockets @{@link com.dotcms.rest.api.v1.system.websocket.SystemEventsWebSocketEndPoint}
 * is not able to be called by any reason.
 * The @{@link EventsResource} use the long polling approach to get all the events, the call basically will wait for N seconds
 * (use the property events.longpolling.seconds, on the dotmarketing-config-ext.properties to custom the seconds, by default it is 15 seconds)
 *
 * @author jsanca
 * @version 3.7
 * @since Jul 7, 2016
 */
@SuppressWarnings("serial")
@Path("/ws/v1/system")
public class EventsResource implements Serializable {

    public static final String SYSTEM_EVENT_LONGPOLLING_DEFAULTMILLIS = "system.events.longpolling.defaultmillis";

    private final WebResource webResource;
    private final LongPollingService longPollingService;


    /**
     * Default constructor.
     */
    public EventsResource() {
        this(new WebResource(), new LongPollingService
                (Config.getLongProperty(SYSTEM_EVENT_LONGPOLLING_DEFAULTMILLIS, 15000),
                        new SystemEventsDelegate()));
    }

    @VisibleForTesting
    protected EventsResource(final WebResource webResource,
                             final LongPollingService longPollingService) {

        this.webResource        = webResource;
        this.longPollingService = longPollingService;
    }

    @GET
    @Path("/hello")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response echo(@Context final HttpServletRequest request) {

        return Response.ok(new ResponseEntityView("Hello"))
                .build(); // 200
    }

    @GET
    @Path("/syncevents")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getSyncEvents(@Context final HttpServletRequest request,
                                        @QueryParam("lastcallback") Long lastCallback) {


        Response response = null;
        final InitDataObject initData = this.webResource.init(null, true, request, true, null);
        final AppContext appContext =  WebSessionContext.getInstance(request);
        List<SystemEvent> systemEvents = null;
        try {

            if (null != initData.getUser()) {

                Logger.debug(this, "Getting syncr system events with a lastcallback as: " + lastCallback);
                appContext.setAttribute(SystemEventsDelegate.LAST_CALLBACK, lastCallback);
                appContext.setAttribute(SystemEventsDelegate.DO_MARSHALL,   false);

                this.longPollingService.execute(appContext);

                systemEvents = appContext.getAttribute(SystemEventsDelegate.RESULT);

                response = Response.ok(systemEvents).build();
            }
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // getSyncEvents.

    @GET
    @Path("/events")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final void getEvents(@Context final HttpServletRequest request,
                                @Suspended final AsyncResponse asyncResponse,
                                @QueryParam("lastcallback") Long lastCallback) {


        Response response = null;
        final InitDataObject initData = this.webResource.init(null, true, request, true, null);
        final AppContext appContext =  new SimpleMapAppContext();

        try {

            if (null != initData.getUser()) {

                Logger.debug(this, "Getting syncr system events with a lastcallback as: " + lastCallback);
                appContext.setAttribute(SystemEventsDelegate.LAST_CALLBACK, lastCallback);
                appContext.setAttribute(SystemEventsDelegate.RESPONSE, asyncResponse);

                this.longPollingService.executeAsync(appContext);
            }
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
            asyncResponse.resume(response);
        }
    } // getEvents.

    /* @GET
    @Path("/events")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final void getEvents(@Context final HttpServletRequest request,
                                    @Suspended final AsyncResponse asyncResponse) {

        Response response = null;
        final InitDataObject initData = this.webResource.init(null, true, request, true, null);

        try {
            // todo:
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        System.out.println("Running a long polling" + new Date());
                        String result = veryExpensiveOperation();
                        System.out.println("Running a long polling" + new Date());
                        asyncResponse.resume(EventsResource.this.marshalUtils.marshal(map("message", result)));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }

                private String veryExpensiveOperation() throws InterruptedException {
                    // ... very expensive operation
                    Thread.sleep(5000);
                    return "Test fallback";
                }
            }).start();
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
            asyncResponse.resume(response);
        }
    } // getEvents */
} // E:O:F:EventsResource.
