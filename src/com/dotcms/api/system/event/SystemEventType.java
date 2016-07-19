package com.dotcms.api.system.event;

/**
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 11, 2016
 *
 */
public enum SystemEventType {

	NOTIFICATION("notification");

	private final String eventId;

	/**
	 * 
	 * @param eventId
	 */
	private SystemEventType(String eventId) {
		this.eventId = eventId;
	}

	/**
	 * 
	 * @return
	 */
	public String getId() {
		return this.eventId;
	}

}
