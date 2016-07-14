package com.dotcms.web.websocket;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dotcms.util.ReflectionUtils.newInstance;

/**
 * Factory for the {@link WebSocketContainerAPI}, please use the {@link com.dotmarketing.business.APILocator}
 * in order to get the {@link WebSocketContainerAPI} instance, instead of calling this one
 * @author jsanca
 */
public class WebSocketContainerAPIFactory implements Serializable {

    private final WebSocketContainerAPI webSocketContainerAPI = new WebSocketContainerAPIImpl();

    private WebSocketContainerAPIFactory () {
        // singleton
    }

    private static class SingletonHolder {
        private static final WebSocketContainerAPIFactory INSTANCE = new WebSocketContainerAPIFactory();
    }
    /**
     * Get the instance.
     * @return WebSocketContainerAPIFactory
     */
    public static WebSocketContainerAPIFactory getInstance() {

        return WebSocketContainerAPIFactory.SingletonHolder.INSTANCE;
    } // getInstance.

    public WebSocketContainerAPI getWebSocketContainerAPI() {

        return webSocketContainerAPI;
    }

    private class WebSocketContainerAPIImpl implements WebSocketContainerAPI {

        private final Map<Class<?>, Object> instanceCache =
                new ConcurrentHashMap<>();

        @Override
        public <T> T getEndpointInstance(final Class<T> endpointClass) {

            if (this.instanceCache.containsKey(endpointClass)) {

                return (T)this.instanceCache.get(endpointClass);
            }

            T websocket = null;

            synchronized (WebSocketContainerAPIImpl.class) {

                websocket = newInstance(endpointClass);

                if (null == websocket) {

                    // todo: report an several error
                    return null;
                }
            }

            this.instanceCache.put(endpointClass, websocket);

            return websocket;
        }
    }
} // E:O:F:WebSocketContainerAPIFactory.
