package com.dotcms.web.websocket;

import static com.dotcms.util.CollectionsUtils.*;

import com.dotmarketing.business.APILocator;

import javax.websocket.server.ServerEndpointConfig.Configurator;
import java.util.List;

/**
 * This Configurator is in charge of the single instantiation of the ServerSocket End points, it delegates to the {@link WebSocketContainerAPI}
 * the task to keep just one instance of the web sockets, consequently we can get the endpoint instance in any other place of the app.
 *
 * @author jsanca
 */
public class DotCmsWebSocketConfigurator extends Configurator {

    private WebSocketContainerAPI webSocketContainerAPI =
            APILocator.getWebSocketContainerAPI();


    @SuppressWarnings("unchecked")
    @Override
    public <T> T getEndpointInstance(final Class<T> endpointClass) throws InstantiationException {

        return this.webSocketContainerAPI.getEndpointInstance(endpointClass);
    } // E:O:F:getEndpointInstance.

    private final static List<Class> WEB_SOCKET_CLASSES = list(
            SystemEventsWebSocketEndPoint.class
    );


    static {

        for(Class endpointClass : WEB_SOCKET_CLASSES) {

            if (null == APILocator.getWebSocketContainerAPI().getEndpointInstance(endpointClass)) {

                // todo: log an error
            }
        }
    }
} // E:O:F:DotCmsWebSocketConfigurator.
