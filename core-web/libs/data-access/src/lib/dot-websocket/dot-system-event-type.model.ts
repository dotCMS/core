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
    UPDATE_PORTLET_LAYOUTS = 'UPDATE_PORTLET_LAYOUTS'
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
