package com.dotcms.job.system.event.delegate;

import java.util.List;

import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.job.system.event.AbstractJobDelegate;
import com.dotcms.job.system.event.SystemEventsJob;
import com.dotcms.job.system.event.delegate.bean.JobDelegateDataBean;
import com.dotcms.web.websocket.SystemEventsWebSocketEndPoint;
import com.dotcms.web.websocket.WebSocketContainerAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;

/**
 * A Job Delegate is a class that can be registered to the
 * {@link SystemEventsJob}, which is the Job that checks for incoming System
 * Events every specified amount of time. Delegate classes will receive basic
 * information that will allow them to identify new events and perform any
 * custom action.
 * <p>
 * For example, a delegate can connect to a running service and notify a
 * component that one or more new events have been pushed. This way, other
 * services and even UI components can react to the new information and provide
 * a useful output to the user.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since Jul 13, 2016
 *
 */
@SuppressWarnings("serial")
public class SystemEventsJobDelegate extends AbstractJobDelegate {

	private final SystemEventsAPI systemEventsAPI = APILocator.getSystemEventsAPI();
	private final WebSocketContainerAPI webSocketContainerAPI = APILocator.getWebSocketContainerAPI();

	@Override
	public void executeDelegate(final JobDelegateDataBean data) throws DotDataException {
		final List<SystemEvent> newEvents = (List<SystemEvent>) this.systemEventsAPI.getEventsSince(data.getLastCallback());
		if (!newEvents.isEmpty()) {
			final SystemEventsWebSocketEndPoint webSocketEndPoint = this.webSocketContainerAPI
					.getEndpointInstance(SystemEventsWebSocketEndPoint.class);
			for (SystemEvent event : newEvents) {
				webSocketEndPoint.sendSystemEvent(event);
			}
		}
	}

}
