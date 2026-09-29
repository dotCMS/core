/** The window property that traditional pages and plain scripts reach `events` through. */
export const EVENTS_WINDOW_KEY = 'dotEvents';

/**
 * The Analytics.js name of the sender plugin. The enricher keys its hooks with it
 * (`page:<name>`, `track:<name>`), so they run for the sender's calls only.
 */
export const SENDER_PLUGIN_NAME = 'dot-events-sender';

// Analytics endpoint for sending events to server
export const ANALYTICS_ENDPOINT = '/api/v1/analytics/content/event';

/**
 * The event types dotCMS accepts, each with its own schema. It rejects any other type, so the
 * SDK sends no custom events.
 */
export const DotCMSPredefinedEventType = {
    PAGEVIEW: 'pageview',
    CONTENT_IMPRESSION: 'content_impression',
    CONTENT_CLICK: 'content_click',
    CONVERSION: 'conversion'
} as const;

/**
 * Type for structured events
 */
export type DotCMSPredefinedEventType =
    (typeof DotCMSPredefinedEventType)[keyof typeof DotCMSPredefinedEventType];

/**
 * Expected UTM parameter keys for campaign tracking
 */
export const EXPECTED_UTM_KEYS = [
    'utm_source',
    'utm_medium',
    'utm_campaign',
    'utm_term',
    'utm_content'
] as const;

/**
 * Session configuration constants
 */
export const DEFAULT_SESSION_TIMEOUT_MINUTES = 30;

/**
 * Session storage key for session ID
 */
export const SESSION_STORAGE_KEY = 'dot_events_session_id';

/**
 * User ID configuration constants
 */
export const USER_ID_KEY = 'dot_events_user_id';

/**
 * Default queue configuration batch size
 */
const DEFAULT_QUEUE_CONFIG_BATCH_SIZE = 15;

/**
 * Default queue configuration flush interval
 */
const DEFAULT_QUEUE_CONFIG_FLUSH_INTERVAL = 5000;

/**
 * Activity tracking configuration
 * Events used to detect user activity for session management
 * - click: Detects real user interaction with minimal performance impact
 * - visibilitychange: Handled separately to detect tab changes
 */
export const ACTIVITY_EVENTS = ['click'] as const;

/**
 * Default queue configuration for event batching
 */
export const DEFAULT_QUEUE_CONFIG = {
    eventBatchSize: DEFAULT_QUEUE_CONFIG_BATCH_SIZE, // Max events per batch - auto-sends when reached
    flushInterval: DEFAULT_QUEUE_CONFIG_FLUSH_INTERVAL // Time between flushes - sends whatever is queued
} as const;

/**
 * Default properties that Analytics.js adds automatically
 * These should be filtered out to only keep user-provided properties
 */
export const ANALYTICS_JS_DEFAULT_PROPERTIES = [
    'title',
    'url',
    'path',
    'hash',
    'search',
    'width',
    'height',
    'referrer'
] as const;

/**
 * Window flag that says the SDK is active on the page. Not this package's name to change:
 * `@dotcms/uve/internal` declares it too (`ANALYTICS_ACTIVE_WINDOW_KEY`), and the React, Vue
 * and Angular SDKs read it to print the contentlet attributes impressions and clicks need.
 */
export const ANALYTICS_WINDOWS_ACTIVE_KEY = '__dotAnalyticsActive__';

/**
 * Window event sent once the SDK is active, so the renderers that read
 * `ANALYTICS_WINDOWS_ACTIVE_KEY` render again. Shared with `@dotcms/uve/internal`
 * (`ANALYTICS_READY_EVENT`) like that flag.
 */
export const ANALYTICS_READY_EVENT = 'dotcms:analytics:ready';

/**
 * Queue persistence configuration constants
 * Used for storing events in sessionStorage for traditional page navigations
 */

/**
 * Session storage key for persistent tab ID
 * This ID remains constant across page navigations within the same browser tab
 */
export const TAB_ID_STORAGE_KEY = 'dot_events_tab_id';

/**
 * Prefix for queue storage key in sessionStorage
 * Full key format: dot_events_queue_{tabId}
 */
export const QUEUE_STORAGE_KEY_PREFIX = 'dot_events_queue';

/**
 * Maximum age in milliseconds for persisted events
 * Events older than this will be discarded (24 hours)
 */
export const MAX_EVENT_AGE_MS = 24 * 60 * 60 * 1000;
