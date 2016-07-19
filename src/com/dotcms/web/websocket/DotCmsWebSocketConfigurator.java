package com.dotcms.web.websocket;

import static com.dotcms.util.CollectionsUtils.list;

import java.util.List;

import javax.websocket.server.ServerEndpointConfig.Configurator;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;

/**
 * This {@link Configurator} is in charge of the single instantiation of the
 * ServerSocket End points. It delegates the {@link WebSocketContainerAPI} the
 * task to keep just one instance of the web sockets. Consequently, we can get
 * the endpoint instance in any other place of the application.
 *
 * @author jsanca
 * @version 3.7
 */
public class DotCmsWebSocketConfigurator extends Configurator {

	private WebSocketContainerAPI webSocketContainerAPI = APILocator.getWebSocketContainerAPI();

	@Override
	public <T> T getEndpointInstance(final Class<T> endpointClass) throws InstantiationException {
		return this.webSocketContainerAPI.getEndpointInstance(endpointClass);
	} // E:O:F:getEndpointInstance.

	private final static List<Class> WEB_SOCKET_CLASSES = list(SystemEventsWebSocketEndPoint.class);

	static {
		for (Class endpointClass : WEB_SOCKET_CLASSES) {
			if (null == APILocator.getWebSocketContainerAPI().getEndpointInstance(endpointClass)) {
				Logger.debug(DotCmsWebSocketConfigurator.class, "Instance of Websocket End-Point class [" + endpointClass
						+ "] could not be retrieved.");
			}
		}
	}

} // E:O:F:DotCmsWebSocketConfigurator.
