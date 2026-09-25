/**
 * System event types emitted by the dotCMS server over the
 * `/api/ws/v1/system/events` WebSocket and consumed via {@link DotEventsSocket}.
 *
 * Mirrors the backend `com.dotcms.api.system.event.SystemEventType` enum.
 * Use these constants instead of raw string literals so consumers stay in sync.
 */
export enum DotSystemEventType {
    SAVE_SITE = 'SAVE_SITE',
    PUBLISH_SITE = 'PUBLISH_SITE',
    UN_PUBLISH_SITE = 'UN_PUBLISH_SITE',
    UPDATE_SITE = 'UPDATE_SITE',
    ARCHIVE_SITE = 'ARCHIVE_SITE',
    UN_ARCHIVE_SITE = 'UN_ARCHIVE_SITE',
    DELETE_SITE = 'DELETE_SITE',
    SWITCH_SITE = 'SWITCH_SITE',
    UPDATE_SITE_PERMISSIONS = 'UPDATE_SITE_PERMISSIONS',
    UPDATE_PORTLET_LAYOUTS = 'UPDATE_PORTLET_LAYOUTS',
    /** A bulk content reindex finished; the payload carries the run's counters. */
    BULK_REFRESH_COMPLETED = 'BULK_REFRESH_COMPLETED',
    BULK_UPLOAD_COMPLETED = 'BULK_UPLOAD_COMPLETED',
    /**
     * A folder entered a bulk delete. Announced to **everyone who may read that folder**, not only
     * to whoever submitted the run — which is what lets a second author's listing mark it before
     * they walk into a folder that is being destroyed (#37063, backend FR-035a/FR-035b).
     */
    FOLDER_DELETE_STARTED = 'FOLDER_DELETE_STARTED',
    /**
     * A folder left a bulk delete — **whether or not the delete succeeded**. A failed delete leaves
     * the folder intact and usable, so this must clear the marking just as a success does.
     */
    FOLDER_DELETE_FINISHED = 'FOLDER_DELETE_FINISHED',
    /** A bulk folder delete finished; scoped to the submitter, and carries the run's outcome. */
    BULK_FOLDER_DELETE_COMPLETED = 'BULK_FOLDER_DELETE_COMPLETED'
}

/**
 * Site events that mean the site is no longer accessible — when one of these
 * targets the currently selected site, the UI should switch to the default site.
 */
export const SITE_UNAVAILABLE_EVENTS: ReadonlySet<DotSystemEventType> = new Set([
    DotSystemEventType.ARCHIVE_SITE,
    DotSystemEventType.UN_PUBLISH_SITE,
    DotSystemEventType.DELETE_SITE
]);

/**
 * Site events that should trigger a refresh of site lists/selectors.
 */
export const SITE_REFRESH_EVENTS: readonly DotSystemEventType[] = [
    DotSystemEventType.SAVE_SITE,
    DotSystemEventType.PUBLISH_SITE,
    DotSystemEventType.UPDATE_SITE,
    DotSystemEventType.ARCHIVE_SITE,
    DotSystemEventType.UN_ARCHIVE_SITE,
    DotSystemEventType.UN_PUBLISH_SITE,
    DotSystemEventType.DELETE_SITE
];
