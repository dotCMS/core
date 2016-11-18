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
	DELETE_BASE_CONTENT_TYPE,

	SAVE_FOLDER,
	UPDATE_FOLDER,
	DELETE_FOLDER,

	SAVE_PAGE_ASSET,
	UPDATE_PAGE_ASSET,
	ARCHIVE_PAGE_ASSET,
	UN_ARCHIVE_PAGE_ASSET,

	SAVE_FILE_ASSET,
	UPDATE_FILE_ASSET,
	ARCHIVE_FILE_ASSET,
	UN_ARCHIVE_FILE_ASSET,

	SAVE_LINK,
	UPDATE_LINK,
	ARCHIVE_LINK,
	UN_ARCHIVE_LINK,
	MOVE_LINK,
	COPY_LINK,

	MOVE_FOLDER,
	COPY_FOLDER,
	MOVE_FILE_ASSET,
	COPY_FILE_ASSET,
	MOVE_PAGE_ASSET, //not working
	COPY_PAGE_ASSET,
	
	UPDATE_PORTLET_LAYOUTS,
}
