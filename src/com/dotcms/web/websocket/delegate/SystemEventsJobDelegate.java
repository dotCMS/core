package com.dotcms.web.websocket.delegate;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.web.websocket.JobDelegate;
import com.dotcms.web.websocket.SystemEventsWebSocketEndPoint;
import com.dotcms.web.websocket.WebSocketContainerAPI;
import com.dotcms.web.websocket.delegate.bean.JobDelegateDataBean;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;

/**
 * 
 * @author Jose Castro
 * @version 1.0
 * @since Jul 13, 2016
 *
 */
@SuppressWarnings("serial")
public class SystemEventsJobDelegate implements JobDelegate, Serializable {

	private final SystemEventsAPI systemEventsAPI = APILocator.getSystemEventsAPI();
	private final WebSocketContainerAPI webSocketContainerAPI = APILocator.getWebSocketContainerAPI();

	@Override
	public void execute(JobDelegateDataBean data) {
		try {
			System.out.println("Getting messages created since " + new Date(data.getLastCallback()));
			List<SystemEvent> newEvents = (List<SystemEvent>) this.systemEventsAPI.getEventsSince(data.getLastCallback());
			if (!newEvents.isEmpty()) {
				System.out.println("Found " + newEvents.size() + " new event(s)!");
				SystemEventsWebSocketEndPoint webSocketEndPoint = this.webSocketContainerAPI
						.getEndpointInstance(SystemEventsWebSocketEndPoint.class);
				for (SystemEvent event : newEvents) {
					webSocketEndPoint.sendSystemEvent(event);
				}
			} else {
				System.out.println("List is empty :(");
			}
		} catch (DotDataException e) {
			Logger.error(this, "An error occurred when retrieving new System Events since [" + new Date(data.getLastCallback()) + "]",
					e);
		} finally {
			try {
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.warn(this, e.getMessage(), e);
			} finally {
				DbConnectionFactory.closeConnection();
			}
		}
	}

}
