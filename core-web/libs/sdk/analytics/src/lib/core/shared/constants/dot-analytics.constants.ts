// Analytics windows key
export const ANALYTICS_WINDOWS_KEY = 'dotAnalytics';

// Analytics source type
export const ANALYTICS_SOURCE_TYPE = ANALYTICS_WINDOWS_KEY;

// Analytics endpoint for sending events to server
export const ANALYTICS_ENDPOINT = '/api/v1/analytics/content/event';

/**
 * Structured event types - events with predefined data shapes
 * These events have specific data structures and validation
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
 * Custom event type - any string except predefined event types
 * These events have flexible data structures defined by the user
 */
export type DotCMSCustomEventType = Exclude<string, DotCMSPredefinedEventType>;

/**
 * Union type for all possible event types
 */
export type DotCMSEventType = DotCMSPredefinedEventType | DotCMSCustomEventType;

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
export const SESSION_STORAGE_KEY = 'dot_analytics_session_id';

/**
 * Session storage key for session start time
 */
export const SESSION_START_KEY = 'dot_analytics_session_start';

/**
 * Session storage key for session UTM data
 */
export const SESSION_UTM_KEY = 'dot_analytics_session_utm';

/**
 * User ID configuration constants
 */
export const USER_ID_KEY = 'dot_analytics_user_id';

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
 * The name of the analytics minified script.
 */
export const ANALYTICS_MINIFIED_SCRIPT_NAME = 'ca.min.js';

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
 * Impression tracking configuration constants
 */

/**
 * Default minimum percentage of element that must be visible (0.0 to 1.0)
 */
export const DEFAULT_IMPRESSION_VISIBILITY_THRESHOLD = 0.5;

/**
 * Default minimum time in milliseconds element must be visible
 */
export const DEFAULT_IMPRESSION_DWELL_MS = 750;

/**
 * Default maximum number of elements to track (performance limit)
 */
export const DEFAULT_IMPRESSION_MAX_NODES = 100;

/**
 * Default throttle time in milliseconds for intersection callbacks
 */
export const DEFAULT_IMPRESSION_THROTTLE_MS = 100;

/**
 * Default debounce time in milliseconds for MutationObserver
 */
export const DEFAULT_IMPRESSION_MUTATION_OBSERVER_DEBOUNCE_MS = 250;

/**
 * Default impression tracking configuration
 */
export const DEFAULT_IMPRESSION_CONFIG = {
    visibilityThreshold: DEFAULT_IMPRESSION_VISIBILITY_THRESHOLD,
    dwellMs: DEFAULT_IMPRESSION_DWELL_MS,
    maxNodes: DEFAULT_IMPRESSION_MAX_NODES,
    throttleMs: DEFAULT_IMPRESSION_THROTTLE_MS
} as const;

/**
 * Event type for content impressions
 * Must match DotCMSPredefinedEventType.CONTENT_IMPRESSION
 */
export const IMPRESSION_EVENT_TYPE = 'content_impression';

/**
 * Event type for content clicks
 * Must match DotCMSPredefinedEventType.CONTENT_CLICK
 */
export const CLICK_EVENT_TYPE = 'content_click';

/**
 * Default debounce time in milliseconds for clicks
 */
export const DEFAULT_CLICK_THROTTLE_MS = 300;

/**
 * CSS selector for clickable elements to track
 * Only clicks on <a> and <button> elements are tracked
 */
export const CLICKABLE_ELEMENTS_SELECTOR = 'a, button';

/**
 * Session storage key for tracked impressions (deduplication)
 */
export const IMPRESSION_SESSION_KEY = 'dot_analytics_impressions';

/**
 * Window property key for analytics active state
 * Used to track if analytics is initialized and active
 */
export const ANALYTICS_WINDOWS_ACTIVE_KEY = '__dotAnalyticsActive__';

/**
 * Window property key for analytics cleanup function
 * Used to store the cleanup function for analytics instance
 */
export const ANALYTICS_WINDOWS_CLEANUP_KEY = '__dotAnalyticsCleanup__';

/**
 * CSS class selector for contentlet elements
 */
export const CONTENTLET_CLASS = 'dotcms-contentlet';
