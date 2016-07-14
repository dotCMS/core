package com.dotcms.web.websocket;

import com.dotcms.api.system.event.SystemEvent;
import com.dotmarketing.util.Logger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 11, 2016
 *
 */
@ServerEndpoint(value = "/system/events",
        encoders = { SystemEventEncoder.class },
        configurator = DotCmsWebSocketConfigurator.class)
public class SystemEventsWebSocketEndPoint implements Serializable {

    private final Queue<Session> queue =
            new ConcurrentLinkedQueue<>();

    @OnOpen
    public void open(Session session) {

        this.queue.add(session);
        Logger.error(this, "New session opened: " + session.getId());
    }

    @OnError
    public void error(Session session, Throwable t) {
        this.queue.remove(session);
        Logger.error(this, "Error on session " + session.getId());
    }

    @OnClose
    public void closedConnection(Session session) {
        this.queue.remove(session);
        Logger.error(this, "session closed: " + session.getId());
    }

    public void sendSystemEvent(final SystemEvent event) {

        try {
			/* Send the new rate to all open WebSocket sessions */
            final ArrayList<Session> closedSessions = new ArrayList<>();
            for (Session session : queue) {
                if (!session.isOpen()) {
                    // todo log me System.err.println("Closed session: " + session.getId());

                    closedSessions.add(session);
                } else {

                    session.getBasicRemote().sendObject(event);
                }
            }

            queue.removeAll(closedSessions);
            // log me System.out.println("Sending " + msg + " to " + queue.size() + " clients");
        } catch (Throwable e) {

            // todo log an error
        }
    }

} // E:O:F:SystemEventsWebSocketEndPoint.
