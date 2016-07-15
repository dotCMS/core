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
 * This class is the Websocket end-point used by dotCMS to push System Events to
 * a client, such as a back-end service, or a JavaScript component.
 * <p>
 * System Events can be processed by any component or service in dotCMS to make
 * the system react to a specific event or notification. Such services can
 * register to this Websocket in order to receive periodic data and handle it
 * appropriately.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jul 16, 2016
 *
 */
@SuppressWarnings("serial")
@ServerEndpoint(value = "/system/events", encoders = { SystemEventEncoder.class }, configurator = DotCmsWebSocketConfigurator.class)
public class SystemEventsWebSocketEndPoint implements Serializable {

	private final Queue<Session> queue = new ConcurrentLinkedQueue<>();

	@OnOpen
	public void open(Session session) {
		this.queue.add(session);
		Logger.debug(this, "New session opened: " + session.getId());
	}

	@OnError
	public void error(Session session, Throwable t) {
		this.queue.remove(session);
		Logger.debug(this, "Error on session " + session.getId());
	}

	@OnClose
	public void closedConnection(Session session) {
		this.queue.remove(session);
		Logger.debug(this, "session closed: " + session.getId());
	}

	/**
	 * Sends the information of a System Event to all the active clients
	 * (Websocket sessions) that are registered to this end-point.
	 * 
	 * @param event
	 *            - The {@link SystemEvent} object that clients will receive.
	 */
	public void sendSystemEvent(final SystemEvent event) {
		try {
			final ArrayList<Session> closedSessions = new ArrayList<>();
			for (Session session : queue) {
				if (!session.isOpen()) {
					Logger.debug(this, "Closed session: " + session.getId());
					closedSessions.add(session);
				} else {
					session.getBasicRemote().sendObject(event);
				}
			}
			queue.removeAll(closedSessions);
			Logger.debug(this, "Sending " + event + " to " + queue.size() + " clients");
		} catch (Throwable e) {
			Logger.error(this, e.getMessage(), e);
		}
	}

} // E:O:F:SystemEventsWebSocketEndPoint.
