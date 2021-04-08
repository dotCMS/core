package com.dotcms.rest.api.v1.event;

import com.dotcms.api.system.event.*;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.system.websocket.WebSocketUserSessionData;
import com.dotcms.system.AppContext;
import com.dotcms.util.Delegate;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.util.List;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * This delegate executes the logic to get all events since SystemEventsDelegate.LAST_CALLBACK (long),
 * filter them and finally stores the result on the {@link AppContext} as SystemEventsDelegate.RESULT.
 * The value stored on the RESULT will be a json of the list of events by default, but you can set {@link SystemEventsDelegate}.DO_MARSHALL in false
 * if you want to receive the {@link List} of {@link SystemEvent}
 * @author jsanca
 */
public class SystemEventsDelegate implements Delegate<AppContext> {

    public static final String RESULT        = "result";
    public static final String LAST_CALLBACK = "lastcallback";
    public static final String DO_MARSHALL   = "domarshall";
    public static final String RESPONSE      = "response";
    public static final String USER          = "user";

    private final SystemEventsAPI systemEventsAPI;
    private final MarshalUtils marshalUtils;
    private final PayloadVerifierFactory payloadVerifierFactory;
    private final SystemEventProcessorFactory systemEventProcessorFactory;

    public SystemEventsDelegate() {
        this (APILocator.getSystemEventsAPI(), MarshalFactory.getInstance().getMarshalUtils(),
                PayloadVerifierFactory.getInstance(), SystemEventProcessorFactory.getInstance());
    }

    public SystemEventsDelegate(final SystemEventsAPI systemEventsAPI,
                                final MarshalUtils marshalUtils,
                                final PayloadVerifierFactory payloadVerifierFactory,
                                final SystemEventProcessorFactory systemEventProcessorFactory) {

        this.systemEventsAPI             = systemEventsAPI;
        this.marshalUtils                = marshalUtils;
        this.payloadVerifierFactory      = payloadVerifierFactory;
        this.systemEventProcessorFactory = systemEventProcessorFactory;
    }

    @Override
    @CloseDBIfOpened
    public void execute(final AppContext context) {

        List<SystemEvent> newEvents = null;
        final long lastCallback = (null != context.getAttribute(LAST_CALLBACK))?
                context.getAttribute(LAST_CALLBACK):System.currentTimeMillis();

        try {

            Logger.debug(this, "Getting events, last callback: " + lastCallback);
            newEvents = (List<SystemEvent>) this.systemEventsAPI.getEventsSince(lastCallback);
        } catch (Exception e) {

            Logger.debug(this, e.getMessage(), e);
        }

        if (null != newEvents) {

            this.processEventsResults(context, newEvents);
        }
    } // execute.

    private void processEventsResults (final AppContext context, final List<SystemEvent> newEvents) {

        final boolean doMarshall = (null != context.getAttribute(DO_MARSHALL))?
                context.getAttribute(DO_MARSHALL):true;

        final List<SystemEvent> filteredEventList = this.processAndFilter (context, newEvents);

        if (doMarshall) {

            this.doMarshall(context, filteredEventList);
        } else {

            context.setAttribute(RESULT, filteredEventList);
        }
    } // processEventsResults.

    private List<SystemEvent> processAndFilter(final AppContext context,
                                               final List<SystemEvent> newEvents) {

        final List<SystemEvent> filteredEventList = list();
        final User sessionUser                    = context.getAttribute(USER);
        final String userSessionId                = context.getId();

        if (null != newEvents) {

            for (final SystemEvent systemEvent : newEvents) {

                try {
                    if (this.apply(systemEvent, sessionUser, userSessionId)) {

                        filteredEventList.add(this.processEvent(sessionUser, systemEvent));
                    } else {
                        Logger.debug(this, "The event: " + systemEvent
                                + ", has been filtered for the user: " + sessionUser);
                    }
                } catch (DotDataException e) {
                    Logger.error(this, e.getMessage(), e);
                }
            }
        }

        return filteredEventList;
    } // processAndFilter.

    private SystemEvent processEvent(final User sessionUser,
                                     final SystemEvent event) {

        final SystemEventProcessor processor =
                this.systemEventProcessorFactory.createProcessor(event.getEventType());

        return null != processor? processor.process(event, sessionUser)
                : event;
    } // processEvent.


    /**
     * Determine if the event applies for the sessionUser
     * @param event {@link SystemEvent}
     * @param sessionUser {@link User}
     * @return Boolean
     * @throws DotDataException
     */
    private boolean apply(final SystemEvent event,
                          final User sessionUser,
                          final String userSessionId) throws DotDataException {

        final Payload payload = event.getPayload();
        boolean apply         = false; // if the payload is null, must not send to the session.

        if  (null != payload && null != payload.getVisibility() && null != sessionUser) {

            //Get the verifier associated to this Payload
            final PayloadVerifier verifier = this.payloadVerifierFactory.getVerifier(payload);

            //Check if we have the "visibility" rights to use this payload
            apply = (null != verifier) ? verifier.verified(payload,
                    new WebSocketUserSessionData() {
                        @Override
                        public User getUser() {
                            return sessionUser;
                        }

                        @Override
                        public String getUserSessionId() {
                            return userSessionId;
                        }
                    }) : true;
        }

        return apply;
    } // apply.

    private void doMarshall (final AppContext context, final List<SystemEvent> newEvents) {

        final AsyncResponse asyncResponse = context.getAttribute(RESPONSE);
        final User user = context.getAttribute(USER);
        Response response;
        if (user == null) {
            response = ExceptionMapperUtil.createResponse(
                    new SecurityException("Not logged user"), Response.Status.UNAUTHORIZED);
        } else {
            final String json = this.marshalUtils.marshal(new ResponseEntityView(newEvents));
            response = Response.ok(json).build();
        }
        asyncResponse.resume(response);

    } // doMarshall.

} // E:O:F:SystemEventsDelegate.
