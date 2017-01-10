package com.dotcms.rest.api.v1.system.websocket;

import java.io.Serializable;

/**
 * This service is in charge of the instantiation and storage of Websocket
 * instances in a dotCMS server.
 * <p>
 * Websockets are particularly useful for adding notification features to the
 * system given that it removes the need for an open HTTP connection to retrieve
 * information. The Websockets protocol allows for bi-directional communication,
 * low latency, small overhead, and unlike a common HTTP connection, it doesn't
 * need to deal with the disadvantage of the browser session being kept alive
 * because of requests asking for new data.
 * <p>
 * If you'd like more information, please visit <a
 * href="https://tyrus.java.net/">Project Tyrus</a>.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jul 12, 2016
 */
public interface WebSocketContainerAPI extends Serializable {

	/**
	 * Returns an instance of the requested end-point class.
	 * 
	 * @param endpointClass
	 *            - The Websocket that will be instantiated.
	 * @return A unique instance of the Websocket.
	 */
	public <T> T getEndpointInstance(Class<T> endpointClass);

} // E:O:F:WebSocketContainerAPI.
