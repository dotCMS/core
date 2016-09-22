package com.dotcms.api.system.event;

/**
 * A System Event can be generated for a variety of reasons, such as:
 * <ul>
 * <li>A notification for the user.</li>
 * <li>An error during the execution of a scheduled job.</li>
 * <li>The login of a specific user.</li>
 * </ul>
 * <p>
 * The idea behind this class is to map a type of event with different possible
 * scenarios that developers or third-party systems would create System Events
 * for. This way, client-side code can handle specific types of system events
 * and make dotCMS react accordingly.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 11, 2016
 *
 */
public enum SystemEventType {

	NOTIFICATION("notification"),
	SAVE_SITE("save_site_testing"),
	UPDATE_SITE("update_site_testing"),
	ARCHIVE_SITE("archive_site"),
	UN_ARCHIVE_SITE("un_archive_site");

	private final String eventName;

	/**
	 * Creates an instance of this class.
	 * 
	 * @param eventName
	 *            - The name of the event type.
	 */
	private SystemEventType(String eventName) {
		this.eventName = eventName;
	}

	/**
	 * Returns the name of this event type.
	 * 
	 * @return The name of the event type.
	 */
	public String getName() {
		return this.eventName;
	}

}
