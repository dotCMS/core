// Analytics windows key
export const ANALYTICS_WINDOWS_KEY = 'dotAnalytics';

// Analytics source type
export const ANALYTICS_SOURCE_TYPE = ANALYTICS_WINDOWS_KEY;

// Analytics endpoint for sending events to server
export const ANALYTICS_ENDPOINT = '/api/v1/analytics/content/event';

/**
 * Event Types
 * Only two event types are supported in DotCMS Analytics
 */
export const EVENT_TYPES = {
    PAGEVIEW: 'pageview',
    TRACK: 'track'
} as const;

/**
 * Expected UTM parameter keys for campaign tracking
 */
export const EXPECTED_UTM_KEYS = [
    'utm_source',
    'utm_medium',
    'utm_campaign',
    'utm_term',
    'utm_content',
    'utm_id'
] as const;

/**
 * Session configuration constants
 */
export const DEFAULT_SESSION_TIMEOUT_MINUTES = 30;

export const SESSION_STORAGE_KEY = 'dot_analytics_session_id';

export const SESSION_START_KEY = 'dot_analytics_session_start';

export const SESSION_UTM_KEY = 'dot_analytics_session_utm';

/**
 * User ID configuration constants
 */
export const USER_ID_KEY = 'dot_analytics_user_id';

/**
 * Activity tracking configuration
 * Events used to detect user activity for session management
 * - click: Detects real user interaction with minimal performance impact
 * - visibilitychange: Handled separately to detect tab changes
 */
export const ACTIVITY_EVENTS = ['click'] as const;

/**
 * The name of the analytics minified script.
 */
export const ANALYTICS_MINIFIED_SCRIPT_NAME = 'ca.min.js';
