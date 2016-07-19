package com.dotcms.web.websocket;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.dotcms.api.system.event.SystemEvent;
import com.dotmarketing.util.Logger;

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
@ServerEndpoint(value = "/system/events", encoders = { SystemEventEncoder.class }, configurator = DotCmsWebSocketConfigurator.class)
public class SystemEventsWebSocketEndPoint implements Serializable {

	private final Queue<Session> queue = new ConcurrentLinkedQueue<>();

	@OnOpen
	public void open(Session session) {
		queue.add(session);
	}

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
		try {
			final ArrayList<Session> closedSessions = new ArrayList<>();
			for (Session session : queue) {
				if (!session.isOpen()) {
					closedSessions.add(session);
				} else {
					session.getBasicRemote().sendObject(event);
				}
			}
			queue.removeAll(closedSessions);
		} catch (Throwable e) {
			Logger.error(this, "An error occurred when sending a message through the " + this.getClass().getName(), e);
		}
	}

}
