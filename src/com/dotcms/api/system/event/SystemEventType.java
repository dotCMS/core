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

	NOTIFICATION,

	SAVE_SITE,
	UPDATE_SITE,
	ARCHIVE_SITE,
	UN_ARCHIVE_SITE,

	SAVE_BASE_CONTENT_TYPE,
	UPDATE_BASE_CONTENT_TYPE,
	DELETE_BASE_CONTENT_TYPE;

}
