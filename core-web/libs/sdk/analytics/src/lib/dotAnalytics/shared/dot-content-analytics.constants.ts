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
 * The type of event.
 */
export enum EventType {
    Track = 'track',
    PageView = 'pageview'
}
