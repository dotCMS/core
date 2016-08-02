package com.dotcms.rest.api.v1.system.websocket;

import static com.dotcms.util.ReflectionUtils.newInstance;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dotmarketing.util.Logger;

/**
 * This singleton class provides access to the {@link WebSocketContainerAPI}
 * class.
 * 
 * @author jsanca
 * @version 3.7
 */
@SuppressWarnings("serial")
public class WebSocketContainerAPIFactory implements Serializable {

	private final WebSocketContainerAPI webSocketContainerAPI = new WebSocketContainerAPIImpl();

	/**
	 * Private constructor for singleton creation.
	 */
	private WebSocketContainerAPIFactory() {

	}

	/**
	 * Singleton holder using initialization on demand
	 */
	private static class SingletonHolder {
		private static final WebSocketContainerAPIFactory INSTANCE = new WebSocketContainerAPIFactory();
	}

	/**
	 * Returns the singleton instance of the
	 * {@link WebSocketContainerAPIFactory} class.
	 * 
	 * @return The {@link WebSocketContainerAPIFactory} class.
	 */
	public static WebSocketContainerAPIFactory getInstance() {

		return WebSocketContainerAPIFactory.SingletonHolder.INSTANCE;

	}

	/**
	 * Returns a unique instance of the {@link WebSocketContainerAPI} class.
	 * 
	 * @return The {@link WebSocketContainerAPI} object.
	 */
	public WebSocketContainerAPI getWebSocketContainerAPI() {
		return webSocketContainerAPI;
	}

	/**
	 * The concrete implementaiton of the {@link WebSocketContainerAPI} class.
	 * 
	 * @author jsanca
	 * @version 3.7
	 *
	 */
	private class WebSocketContainerAPIImpl implements WebSocketContainerAPI {

		private final Map<Class<?>, Object> instanceCache = new ConcurrentHashMap<>();

		@Override
		public <T> T getEndpointInstance(final Class<T> endpointClass) {
			if (this.instanceCache.containsKey(endpointClass)) {
				return (T) this.instanceCache.get(endpointClass);
			}
			T websocket = null;
			synchronized (WebSocketContainerAPIImpl.class) {
				websocket = newInstance(endpointClass);
				if (null == websocket) {
					Logger.error(this, "An instance of the [" + endpointClass + "]  class could not be created.");
					return null;
				}
			}
			this.instanceCache.put(endpointClass, websocket);
			return websocket;
		}
	}

}
