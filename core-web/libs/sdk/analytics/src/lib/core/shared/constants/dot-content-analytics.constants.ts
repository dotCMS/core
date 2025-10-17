// Analytics windows key
export const ANALYTICS_WINDOWS_KEY = 'dotAnalytics';

// Analytics active flag key
export const ANALYTICS_WINDOWS_ACTIVE_KEY = '__dotAnalyticsActive__';

// Analytics cleanup function key
export const ANALYTICS_WINDOWS_CLEANUP_KEY = '__dotAnalyticsCleanup';

// Analytics source type
export const ANALYTICS_SOURCE_TYPE = ANALYTICS_WINDOWS_KEY;

// Analytics endpoint for sending events to server
export const ANALYTICS_ENDPOINT = '/api/v1/analytics/content/event';

/**
 * Structured event types - events with predefined data shapes
 * These events have specific data structures and validation
 */
export const DotCMSPredefinedEventType = {
    PAGEVIEW: 'pageview'
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
