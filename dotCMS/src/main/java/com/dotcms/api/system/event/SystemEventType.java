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
	PUBLISH_SITE,

	/**
	 * When a site is updated
	 */
	UPDATE_SITE, // todo: not used

	/**
	 * When a site is unpublished (stopped)
	 */
	UN_PUBLISH_SITE,

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

	/**
	 * Fired when a bundle fails to process during the async upload pipeline.
	 * Covers failures in package extraction, file moves, and configuration updates.
	 */
	OSGI_BUNDLES_UPLOAD_FAILED,

	// Logout Event
	SESSION_LOGOUT,

	// Analytics App
	ANALYTICS_APP,

	/** A Contentlet has been updated by the AI Service */
	AI_CONTENT_PROMPT,

	/**
	 * A bulk content reindex ({@code POST /api/v1/content/_bulkrefresh}) has finished.
	 * <p>
	 * Carries the run's counters so the client can report the outcome without asking for it. Pushed
	 * with {@link Visibility#USER} scoped to whoever submitted the run, because a reindex is nobody
	 * else's business — unlike the legacy batch reindex, which told every CMS Administrator.
	 */
	BULK_REFRESH_COMPLETED,

	/**
	 * A bulk file upload ({@code POST /api/v1/assets/_bulkupload}) has finished.
	 * <p>
	 * Its own type rather than a shared "batch finished" event, so a client can tell an upload from
	 * a reindex without inspecting the payload — they are different operations with different copy
	 * and different follow-up actions.
	 * <p>
	 * Carries the counts <b>and the per-file results</b>, not counts alone: an author told "27 of 30
	 * created" with no way to learn which three cannot act on it, and those names are exactly what
	 * tell them which files to choose again. Pushed with {@link Visibility#USER} scoped to whoever
	 * submitted the run.
	 */
	BULK_UPLOAD_COMPLETED,

	/**
	 * A bulk folder delete ({@code POST /v1/assets/folders/_bulkdelete}, #37063) has finished.
	 * <p>
	 * Its own type for the same reason {@link #BULK_UPLOAD_COMPLETED} has one — a client tells this
	 * apart from any other background work without inspecting the payload. Carries the counts
	 * <b>and the per-path results</b>, not counts alone, plus {@code stoppedAt} on a cancelled run.
	 * Pushed with {@link Visibility#USER} scoped to whoever submitted the run — never every
	 * administrator.
	 * <p>
	 * <b>This exact name is fixed by the frontend half of #37063</b>
	 * ({@code DotSystemEventType.BULK_FOLDER_DELETE_COMPLETED}, PR dotCMS/core#37612) — do not
	 * rename it independently on this side.
	 */
	BULK_FOLDER_DELETE_COMPLETED,

	/**
	 * A folder is about to be deleted as part of a bulk folder delete run (#37063, FR-035a).
	 * <p>
	 * Distinct from {@link #DELETE_FOLDER} in both audience and purpose: this reaches
	 * <b>everyone who may read the folder</b> (excluding whoever is doing the deleting, the same
	 * {@link Visibility#EXCLUDE_OWNER} pattern {@code FolderAPIImpl#delete} already uses), while
	 * {@link #BULK_FOLDER_DELETE_COMPLETED} stays scoped to the submitter and carries how the run
	 * went. This one carries only {@code {jobId, path}} — that the folder is now busy, nothing
	 * about the run's outcome — because an author working inside a folder that is being destroyed
	 * under them needs to know regardless of whether they submitted the run.
	 * <p>
	 * <b>This exact name is fixed by the frontend half of #37063</b>
	 * ({@code DotSystemEventType.FOLDER_DELETE_STARTED}, PR dotCMS/core#37612).
	 */
	FOLDER_DELETE_STARTED,

	/**
	 * A folder has left a bulk folder delete run — deleted, or the delete attempt ended, one way
	 * or the other (#37063, FR-035a).
	 * <p>
	 * <b>Fires whether that folder's delete succeeded or was caught as a classified failure</b> —
	 * unlike the existing {@link #DELETE_FOLDER} event, whose push only runs when
	 * {@code FolderAPIImpl#delete} returns normally. Reusing {@code DELETE_FOLDER} for this was
	 * the original plan and was wrong: an author told a folder had "entered" a delete but never
	 * told it left, because the delete happened to fail, would be left believing it is still busy
	 * indefinitely.
	 * <p>
	 * <b>This exact name is fixed by the frontend half of #37063</b>
	 * ({@code DotSystemEventType.FOLDER_DELETE_FINISHED}, PR dotCMS/core#37612).
	 */
	FOLDER_DELETE_FINISHED

}
