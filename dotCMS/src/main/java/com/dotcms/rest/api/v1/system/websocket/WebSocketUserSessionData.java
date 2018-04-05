package com.dotcms.rest.api.v1.system.websocket;

import com.liferay.portal.model.User;

/**
 * Encapsulates the User Session Data for the WebSocket.
 * @author jsanca
 */
public interface WebSocketUserSessionData {

    /**
     * The session user
     * @return User
     */
    User getUser ();

    /**
     * Get User session id
     * The user session id is the relationship between the container (user session) and the
     * web socket session.
     * @return String
     */
    String getUserSessionId();
}
