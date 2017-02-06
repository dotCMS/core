package com.dotcms.rest.api.v1.event;

import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.repackage.javax.ws.rs.container.AsyncResponse;
import com.dotcms.system.AppContext;
import com.dotcms.util.Delegate;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;

import java.util.List;

/**
 * This delegate executes the logic to get all events since SystemEventsDelegate.LAST_CALLBACK (long),
 * filter them and finally stores the result on the {@link AppContext} as SystemEventsDelegate.RESULT.
 * The value stored on the RESULT will be a json of the list of events by default, but you can set {@link SystemEventsDelegate}.DO_MARSHALL in false
 * if you want to receive the {@link List} of {@link SystemEvent}
 * @author jsanca
 */
public class SystemEventsDelegate implements Delegate<AppContext> {

    public static final String RESULT = "result";
    public static final String LAST_CALLBACK = "lastcallback";
    public static final String DO_MARSHALL = "domarshall";
    public static final String RESPONSE = "response";

    private final SystemEventsAPI systemEventsAPI = APILocator.getSystemEventsAPI();
    private final MarshalUtils marshalUtils = MarshalFactory.getInstance().getMarshalUtils();

    @Override
    public void execute(final AppContext context) {

        List<SystemEvent> newEvents = null;
        final long lastCallback = (null != context.getAttribute(LAST_CALLBACK))?
                context.getAttribute(LAST_CALLBACK):System.currentTimeMillis();

        try {

            Logger.debug(this, "Getting events, last callback: " + lastCallback);
            newEvents = (List<SystemEvent>) this.systemEventsAPI.getEventsSince(lastCallback);
        } catch (Exception e) {

            Logger.debug(this, e.getMessage(), e);
        } finally {

            // The main reason for this abstraction is to ensure that the
            // database connection is released and closed after executing this
            try {
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.warn(this, e.getMessage(), e);
            } finally {
                DbConnectionFactory.closeConnection();
            }
        }

        if (null != newEvents && !newEvents.isEmpty()) {

            this.processEventsResults(context, newEvents);
        }
    } // execute.

    private void processEventsResults (final AppContext context, final List<SystemEvent> newEvents) {

        final boolean doMarshall = (null != context.getAttribute(DO_MARSHALL))?
                context.getAttribute(DO_MARSHALL):true;
        final AsyncResponse asyncResponse;

        if (doMarshall) {

            asyncResponse = context.getAttribute(RESPONSE);
            final String json = this.marshalUtils.marshal(newEvents);
            asyncResponse.resume(json);
        } else {

            context.setAttribute(RESULT, newEvents);
        }
    } // processEventsResults.

} // E:O:F:SystemEventsDelegate.
