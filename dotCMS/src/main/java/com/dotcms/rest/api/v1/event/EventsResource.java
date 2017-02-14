package com.dotcms.rest.api.v1.event;

import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.web.WebSessionContext;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.QueryParam;
import com.dotcms.repackage.javax.ws.rs.container.AsyncResponse;
import com.dotcms.repackage.javax.ws.rs.container.Suspended;
import com.dotcms.repackage.javax.ws.rs.container.TimeoutHandler;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ErrorEntity;
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
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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

    private final long timeoutSeconds;
    private final WebResource webResource;
    private final LongPollingService longPollingService;
    private final MarshalUtils marshalUtils;


    /**
     * Default constructor.
     */
    public EventsResource() {
        this(new WebResource(),
                Config.getLongProperty(SYSTEM_EVENT_LONGPOLLING_DEFAULTMILLIS, 15000)*2/1000,
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
                appContext.setAttribute(SystemEventsDelegate.USER,   initData.getUser());

                this.longPollingService.execute(appContext);

                systemEvents = appContext.getAttribute(SystemEventsDelegate.RESULT);

                response = Response.ok(marshalUtils.marshal(new ResponseEntityView(systemEvents))).build();
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

                asyncResponse.setTimeoutHandler(new EventTimeoutHandler(initData.getUser().getLocale()));
                asyncResponse.setTimeout(this.timeoutSeconds, TimeUnit.SECONDS);

                Logger.debug(this, "Getting syncr system events with a lastcallback as: " + lastCallback);
                appContext.setAttribute(SystemEventsDelegate.LAST_CALLBACK, lastCallback != null ? lastCallback :
                        System.currentTimeMillis());
                appContext.setAttribute(SystemEventsDelegate.RESPONSE, asyncResponse);
                appContext.setAttribute(SystemEventsDelegate.USER,   initData.getUser());

                this.longPollingService.executeAsync(appContext);
            }
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

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

            final Response response = Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(
                    new ResponseEntityView(Arrays.asList(new ErrorEntity("operation-timeout", message)))).build();

            asyncResponse.resume(response);
        }
    }
} // E:O:F:EventsResource.
