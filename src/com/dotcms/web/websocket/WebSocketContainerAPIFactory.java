package com.dotcms.web.websocket;

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
 * @since Jul 12, 2016
 */
@SuppressWarnings("serial")
public class WebSocketContainerAPIFactory implements Serializable {

	private final WebSocketContainerAPI webSocketContainerAPI = new WebSocketContainerAPIImpl();

	/**
	 * Singleton constructor
	 */
	private WebSocketContainerAPIFactory() {
		// singleton
	}

	/**
	 * Singleton holder using initialization on demand.
	 */
	private static class SingletonHolder {
		private static final WebSocketContainerAPIFactory INSTANCE = new WebSocketContainerAPIFactory();
	}

	/**
	 * Returns a unique instance of this class.
	 * 
	 * @return A singleton {@code WebSocketContainerAPIFactory}.
	 */
	public static WebSocketContainerAPIFactory getInstance() {
		return WebSocketContainerAPIFactory.SingletonHolder.INSTANCE;
	} // getInstance.

	/**
	 * Returns an instance of the {@link WebSocketContainerAPI} class.
	 * 
	 * @return An instance of the {@code WebSocketContainerAPI} class.
	 */
	public WebSocketContainerAPI getWebSocketContainerAPI() {
		return this.webSocketContainerAPI;
	}

	/**
	 * The concrete implementation of the {@link WebSocketContainerAPI} class.
	 * 
	 * @author jsanca
	 * @version 3.7
	 * @since Jul 12, 2016
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
					Logger.error(this, "An instance of the '" + endpointClass + "' class could not be created.");
					return null;
				}
			}
			this.instanceCache.put(endpointClass, websocket);
			return websocket;
		}
	}

} // E:O:F:WebSocketContainerAPIFactory.
