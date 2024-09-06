package com.dotcms.api.system.event;

/**
 * A System Event can be generated for a variety of reasons, such as:
 * <ul>
 * 	   <li>A cluster wide event triggered on the server nodes <b>CLUSTER_WIDE_EVENT</b>.</li>
 *     <li>A notification for the user <b>NOTIFICATION</b>.</li>
 * 	   <li>A site change: <b>SAVE_SITE, UPDATE_SITE, ARCHIVE_SITE, UN_ARCHIVE_SITE,
 * 	   UPDATE_SITE_PERMISSIONS</b>.</li>
 * 	   <li>A content type change: <b>SAVE_BASE_CONTENT_TYPE, UPDATE_BASE_CONTENT_TYPE,
 * 	   DELETE_BASE_CONTENT_TYPE</b>.</li>
 * 	   <li>A folder change: <b>SAVE_FOLDER, UPDATE_FOLDER, DELETE_FOLDER</b>.</li>
 * 	   <li>A page change: <b>SAVE_PAGE_ASSET, UPDATE_PAGE_ASSET, ARCHIVE_PAGE_ASSET,
 * 	   UN_ARCHIVE_PAGE_ASSET, DELETE_PAGE_ASSET, PUBLISH_PAGE_ASSET, UN_PUBLISH_PAGE_ASSET</b>
 * 	   .</li>
 * 	   <li>A file change: <b>SAVE_FILE_ASSET, UPDATE_FILE_ASSET, ARCHIVE_FILE_ASSET,
 * 	   UN_ARCHIVE_FILE_ASSET, DELETE_FILE_ASSET, PUBLISH_FILE_ASSET, UN_PUBLISH_FILE_ASSET</b>
 * 	   .</li>
 * 	   <li>A file change: <b>SAVE_LINK, UPDATE_LINK, ARCHIVE_LINK, UN_ARCHIVE_LINK, MOVE_LINK,
 * 	   COPY_LINK, DELETE_LINK, PUBLISH_LINK, UN_PUBLISH_LINK</b>.</li>
 * 	   <li>A file change: <b>MOVE_FOLDER, COPY_FOLDER, MOVE_FILE_ASSET, COPY_FILE_ASSET,
 * 	   MOVE_PAGE_ASSET, COPY_PAGE_ASSET</b>.</li>
 * 	   <li>When a session is created or destroyed: <b>SESSION_CREATED, SESSION_DESTROYED</b>.</li>
 * 	   <li>Any other System Event that can be added to this list in the future.</li>
 * </ul>
 * <p>The idea behind this class is to map a type of event with different possible scenarios that
 * developers or third-party systems would create System Events for. This way, client-side code
 * can handle specific types of system events and make dotCMS react accordingly.
 *
 * @author Jose Castro
 * @version 3.7
 * @since Jul 11, 2016
 */
public enum SystemEventType {

	/**
	 * This event type propagate a Local System Event cluster wide (except the node that push the message)
	 * Note: this do not push a message on the web socket to the UI
	 */
	CLUSTER_WIDE_EVENT,

	/**
	 * A notification for the user, this is being showed on the UI bell.
	 */
	NOTIFICATION,

	/**
	 * When a site is created
	 */
	CREATED_SITE,
	/**
	 * When a site is saved
	 */
	SAVE_SITE, // todo: not used

	/**
	 * When a site is published
	 */
	PUBLISH_SITE, // todo: not used

	/**
	 * When a site is updated
	 */
	UPDATE_SITE, // todo: not used

	/**
	 * When a site is archived
	 */
	ARCHIVE_SITE,

	/**
	 * When a site is deleted
	 */
	DELETE_SITE,

	/**
	 * When a site is unarchived
	 */
	UN_ARCHIVE_SITE,

	/**
	 * When removes, saves or assign permissions for a site
	 */
	UPDATE_SITE_PERMISSIONS,

	/**
	 * When a site is being switched
	 */
	SWITCH_SITE,

	/**
	 * When saving a content type
	 */
	SAVE_BASE_CONTENT_TYPE,

	/**
	 * When updates a content type
	 */
	UPDATE_BASE_CONTENT_TYPE,

	/**
	 * When deletes a content type
	 */
	DELETE_BASE_CONTENT_TYPE,

	/**
	 * When saving a folder
	 */
	SAVE_FOLDER,

	/**
	 * When updates an existing folder
	 */
	UPDATE_FOLDER,

	/**
	 * When deletes a folder
	 */
	DELETE_FOLDER,

	/**
	 * When saving a page
	 */
	SAVE_PAGE_ASSET,  // todo: not used

	/**
	 * When updates a page
	 */
	UPDATE_PAGE_ASSET, // todo: not used

	/**
	 * When archive a page
	 */
	ARCHIVE_PAGE_ASSET, // todo: not used

	/**
	 * When un archives a page
	 */
	UN_ARCHIVE_PAGE_ASSET,  // todo: not used

	/**
	 * When deletes a page
	 */
	DELETE_PAGE_ASSET,  // todo: not used

	/**
	 * When published a page
	 */
	PUBLISH_PAGE_ASSET,  // todo: not used

	/**
	 * When unpublish a page
	 */
	UN_PUBLISH_PAGE_ASSET, // todo: not used

	SAVE_FILE_ASSET, // todo: not used
	UPDATE_FILE_ASSET, // todo: not used
	ARCHIVE_FILE_ASSET,  // todo: not used
	UN_ARCHIVE_FILE_ASSET,  // todo: not used
	DELETE_FILE_ASSET,  // todo: not used
	PUBLISH_FILE_ASSET,   // todo: not used
	UN_PUBLISH_FILE_ASSET,   // todo: not used

	/**
	 * When creates a link
	 */
	SAVE_LINK,

	/**
	 * When updates a link
	 */
	UPDATE_LINK,

	/**
	 * When archives a link
	 */
	ARCHIVE_LINK,
	UN_ARCHIVE_LINK,  // todo: not used

	/**
	 * When moves a link
	 */
	MOVE_LINK,

	/**
	 * When copies a link
	 */
	COPY_LINK,

	/**
	 * When deletes a link
	 */
	DELETE_LINK,

	/**
	 * When publish a link
	 */
	PUBLISH_LINK,

	/**
	 * When unpublish a link
	 */
	UN_PUBLISH_LINK,

	/**
	 * When move a folder
	 */
	MOVE_FOLDER,

	/**
	 * When copy a folder
	 */
	COPY_FOLDER,

	/**
	 * Move the file asset
	 */
	MOVE_FILE_ASSET,

	COPY_FILE_ASSET,  // todo: not used

	/**
	 * When move a page
	 */
	MOVE_PAGE_ASSET,

	COPY_PAGE_ASSET,  // todo: not used

	SESSION_CREATED,   // todo: not used

	/**
	 * When the session is being destroyed
	 */
	SESSION_DESTROYED,

	/**
	 * When updates the layouts
	 */
	UPDATE_PORTLET_LAYOUTS,

	/**
	 * When deletes the layouts
	 */
	DELETE_PORTLET_LAYOUTS,

	/**
	 * Represents a simple toast
	 */
	MESSAGE,

	/**
	 * Represents a large toast
	 */
	LARGE_MESSAGE,

	/**
	 * When deletes a bundle.
	 */
	DELETE_BUNDLE,

	// Osgi restart
	OSGI_FRAMEWORK_RESTART,

	// Osgi bundles push on the load folder
	OSGI_BUNDLES_LOADED,

	// Logout Event
	SESSION_LOGOUT,

	// Analytics App
	ANALYTICS_APP,

	/** A Contentlet has been updated by the AI Service */
	AI_CONTENT_PROMPT

}
