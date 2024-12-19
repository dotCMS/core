import {
    ANALYTICS_PAGEVIEW_EVENT,
    EventType,
    EXPECTED_UTM_KEYS
} from './dot-content-analytics.constants';

/**
 * Configuration interface for DotAnalytics SDK.
 *
 * @interface DotAnalyticsConfig
 */
export interface DotContentAnalyticsConfig {
    /**
     * The URL of the Analytics server endpoint.
     */
    server: string;

    /**
     * Enable debug mode to get additional logging information.
     */
    debug: boolean;

    /**
     * Automatically track page views when set to true.
     */
    autoPageView?: boolean;

    /**
     * The API key for authenticating with the Analytics service.
     */
    apiKey: string;

    /**
     * Custom redirect function handler.
     * When provided, this function will be called instead of the default browser redirect
     * for handling URL redirections.
     *
     * @param {string} url - The URL to redirect to
     */
    redirectFn?: (url: string) => void;
}

// UTM parameters generated from the expected UTM keys
type UTMParams = {
    [key in (typeof EXPECTED_UTM_KEYS)[number] as key extends `utm_${infer U}`
        ? U
        : never]?: string;
};

// Base event data that all events must have
interface BaseEventData {
    anonymousId?: string;
    src: string;
    utc_time: string;
    local_tz_offset: number;
    doc_path: string;
    doc_host: string;
}

// Browser-specific data that we collect
export interface BrowserEventData {
    utc_time: string;
    local_tz_offset: number;
    screen_resolution: string;
    vp_size: string;
    userAgent: string;
    user_language: string;
    doc_encoding: string;
    doc_path: string;
    doc_host: string;
    doc_protocol: string;
    doc_hash: string;
    doc_search: string;
    referrer: string;
    page_title: string;
    utm: UTMParams;
}

// PageView specific data
export interface PageViewEvent extends BaseEventData, BrowserEventData {
    event_type: typeof ANALYTICS_PAGEVIEW_EVENT;
}

export interface TrackEvent {
    event_type: 'track';
    custom_event: string; // Name of the custom event sent by the client
    // Allow any additional properties from client
    [key: string]: unknown;
}

// Payload structure for incoming track events
export interface TrackPayload extends DotAnalyticsPayload {
    type: EventType.Track;
    properties: Record<string, unknown> & {
        title: string;
        url: string;
        path: string;
        hash: string;
        search: string;
        width: number;
        height: number;
        source: string;
    };
}

// Payload structure for incoming pageview events
export interface PageViewPayload extends DotAnalyticsPayload {
    type: EventType.PageView;
    properties: {
        title: string;
        url: string;
        path: string;
        hash: string;
        search: string;
        width: number;
        height: number;
        source: string;
    };
}

/**
 * The payload for a track event.
 */
export interface DotAnalyticsPayload {
    type: string;
    properties: {
        title: string;
        url: string;
        path: string;
        hash: string;
        search: string;
        width: number;
        height: number;
        source: string;
    };
    event: string;
    options: Record<string, unknown>;
    userId: string | null;
    anonymousId: string | null;
}

/**
 * Add ServerEvent type for HTTP layer
 */
export interface ServerEvent extends Record<string, unknown> {
    timestamp: string;
    key: string;
}

/**
 * Interface for the AnalyticsTracker.
 */
export interface DotContentAnalyticsCustomHook {
    track: (eventName: string, payload?: Record<string, unknown>) => void;
}

/**
 * Params for the DotAnalytics plugin
 */
export interface DotAnalyticsParams {
    config: DotContentAnalyticsConfig;
    payload: DotAnalyticsPayload;
}

/**
 * Shared interface for the DotAnalytics plugin
 */
export interface DotAnalytics {
    pageView: (payload?: Record<string, unknown>) => void;
    track: (eventName: string, payload?: Record<string, unknown>) => void;
}
