package com.dotcms.rest.api.v1.system.websocket;

import javax.websocket.server.ServerEndpointConfig.Configurator;

import com.dotmarketing.business.APILocator;

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
	}

}
