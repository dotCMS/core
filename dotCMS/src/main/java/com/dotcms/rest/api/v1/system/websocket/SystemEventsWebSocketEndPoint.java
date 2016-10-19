package com.dotcms.rest.api.v1.system.websocket;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventProcessor;
import com.dotcms.api.system.event.SystemEventProcessorFactory;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This Websocket end-point allows other parts of the system (such as the User
 * Notification component) to register to this service and receive information
 * regarding new notifications or system events. Other application services can
 * get an instance of this end-point via the {@link WebSocketContainerAPI} and
 * send System Events so that other components can read and process them.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 11, 2016
 *
 */
@SuppressWarnings("serial")
@ServerEndpoint(value = "/api/v1/system/events", encoders = { SystemEventEncoder.class }, configurator = DotCmsWebSocketConfigurator.class)
public class SystemEventsWebSocketEndPoint implements Serializable {

	public static final String ID = "userId";
	public static final String USER = "user";
	private final Queue<Session> queue;
	private final UserAPI userAPI;
	private final SystemEventProcessorFactory systemEventProcessorFactory;


	public SystemEventsWebSocketEndPoint() {

		this(new ConcurrentLinkedQueue<Session>(),
				APILocator.getUserAPI(),
				SystemEventProcessorFactory.getInstance());
	}

	@VisibleForTesting
	public SystemEventsWebSocketEndPoint(final Queue<Session> queue,
										 final UserAPI userAPI,
										 final SystemEventProcessorFactory systemEventProcessorFactory) {

		this.queue      = queue;
		this.userAPI    = userAPI;
		this.systemEventProcessorFactory =
				systemEventProcessorFactory;
	}

	@OnOpen
	public void open(final Session session) {

		User user = null;
		boolean addToNormalSession = true;

		if (session.getUserProperties().containsKey(USER)) {

			try {

				user = (User) session.getUserProperties().get(USER);
				this.queue.add(new SessionWrapper(session, user));
				addToNormalSession = false; // not need to add the normal session, since the wrapper was added.
			} catch (Exception e) {

				if (Logger.isErrorEnabled(this.getClass())) {

					Logger.error(this.getClass(), e.getMessage(), e);
				}
			}
		}

		if (addToNormalSession) {

			this.queue.add(session);
		}
	} // open.

	@OnError
	public void error(Session session, Throwable t) {
		queue.remove(session);
	}

	@OnClose
	public void closedConnection(Session session) {
		queue.remove(session);
	}

	/**
	 * Sends the specified {@link SystemEvent} object to all the clients
	 * (front-end or back-end services) that are registered to this Websocket
	 * end-point.
	 * 
	 * @param event
	 *            - A new System Event that has been generated.
	 */
	public void sendSystemEvent(final SystemEvent event) {

		final ArrayList<Session> closedSessions = new ArrayList<>();


		try {

			for (Session session : queue) {

				if (!session.isOpen()) {

					closedSessions.add(session);
				} else {

					if (this.apply (event, session)) {

						session.getAsyncRemote().sendObject
								(this.processEvent(session, event));
					}
				}
			}

			this.queue.removeAll(closedSessions);
		} catch (Throwable e) {

			Logger.error(this, "An error occurred when sending a message through the " + this.getClass().getName(), e);
		}
	} // sendSystemEvent.

	private SystemEvent processEvent(final Session session,
									 final SystemEvent event) {

		final SystemEventProcessor processor =
				this.systemEventProcessorFactory.createProcessor(event.getEventType());

		return null != processor? processor.process(event, session): event;
	} // processEvent.

	private boolean apply(final SystemEvent event,
						  final Session session) throws DotDataException  {

		final Payload payload = event.getPayload();
		boolean apply = true; // by default consider it as Visibility.GLOBAL

		if (null != payload) {

			if (null != payload.getVisibility()) {

				if (session instanceof SessionWrapper) {

					if (null != SessionWrapper.class.cast(session).getUser()) {
						apply = payload.verified((SessionWrapper) session);
					}
				}
			}
		} else {

			apply = false; // if the payload is null, must not send to the session.
		}

		return apply;
	} // apply.



} // E:O:F:SystemEventsWebSocketEndPoint.
