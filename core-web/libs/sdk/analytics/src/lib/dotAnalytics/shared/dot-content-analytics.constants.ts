// Analytics windows key
export const ANALYTICS_WINDOWS_KEY = 'dotAnalytics';

// Analytics source type
export const ANALYTICS_SOURCE_TYPE = ANALYTICS_WINDOWS_KEY;

// Analytics endpoint
export const ANALYTICS_ENDPOINT = '/api/v1/analytics/content/event';

// Analytics pageview event
export const ANALYTICS_PAGEVIEW_EVENT = 'PAGE_REQUEST';

// Analytics track event
export const ANALYTICS_TRACK_EVENT = 'TRACK_EVENT';

// Expected UTM keys
export const EXPECTED_UTM_KEYS = ['utm_source', 'utm_medium', 'utm_campaign', 'utm_id'];

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
 */
export const ACTIVITY_EVENTS = ['click', 'keydown', 'touchstart', 'focus'];

/**
 * The type of event.
 */
export enum EventType {
    Track = 'track',
    PageView = 'pageview'
}
