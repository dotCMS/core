package com.dotcms.web.websocket;

import java.io.Serializable;

/**
 * This service is in charge of the instantiation and storing into the app the web sockets instances
 * @author jsanca
 */
public interface WebSocketContainerAPI extends Serializable {

    /**
     * Gets the Endpoint instance
     * @param endpointClass Class
     * @param <T>
     * @return T
     */
    public <T> T getEndpointInstance(Class<T> endpointClass);
} // E:O:F:WebSocketContainerAPI.
