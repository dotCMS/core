package com.dotcms.system.event.local.business;

import java.io.Serializable;


/**
 * This singleton class provides access to the {@link LocalSystemEventsAPI} class.
 * 
 * @author jsanca
 */
@SuppressWarnings("serial")
public class LocalSystemEventsAPIFactory implements Serializable {

	private final LocalSystemEventsAPI localSystemEventsAPI;

	/**
	 * Private constructor for singleton creation.
	 */
	private LocalSystemEventsAPIFactory() {

		this.localSystemEventsAPI = new LocalSystemEventsAPIImpl();
	}

	/**
	 * Singleton holder using initialization on demand
	 */
	private static class SingletonHolder {

		private static final LocalSystemEventsAPIFactory INSTANCE = new LocalSystemEventsAPIFactory();


		private  SingletonHolder() {
			throw new IllegalStateException("Utility class");
		}
	}

	/**
	 * Returns a single instance of this factory.
	 *
	 * @return A unique {@link LocalSystemEventsAPIFactory} instance.
	 */
	public static LocalSystemEventsAPIFactory getInstance() {
		return LocalSystemEventsAPIFactory.SingletonHolder.INSTANCE;
	}

	public LocalSystemEventsAPI getLocalSystemEventsAPI() {

		return this.localSystemEventsAPI;
	}

} // E:O:F:LocalSystemEventsAPIFactory.
