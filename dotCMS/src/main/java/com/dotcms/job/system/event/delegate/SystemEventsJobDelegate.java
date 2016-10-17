package com.dotcms.job.system.event.delegate;

import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.job.system.event.AbstractJobDelegate;
import com.dotcms.job.system.event.SystemEventsJob;
import com.dotcms.job.system.event.delegate.bean.JobDelegateDataBean;
import com.dotcms.rest.api.v1.system.websocket.SystemEventsWebSocketEndPoint;
import com.dotcms.rest.api.v1.system.websocket.WebSocketContainerAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;

import java.util.List;

/**
 * This delegate class is registered to the {@link SystemEventsJob}, which is
 * the Job that checks for incoming System Events every specified amount of
 * time. The data sent over from the Job indicates the last time that the
 * application checked for new System Events. Based on such a date, this
 * delegate can query the database for new incoming events <b>ONLY</b>.
 * <p>
 * Querying the most recent events that entered the message queue is useful for
 * displaying notifications to the users in order to get synchronous updates on
 * new System Events generated either by dotCMS or custom code.
 * 
 * @author Jose Castro
 * @version 3.7
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
