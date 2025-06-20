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
    siteKey: string;

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
    user_language: string;
    doc_encoding: string;
    doc_path: string;
    doc_host: string;
    doc_protocol: string;
    doc_hash: string;
    doc_search: string;
    referrer: string;
    page_title: string;
    url: string;
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
    properties: DotCMSAnalyticsProperties;
}

// Payload structure for incoming pageview events
export interface PageViewPayload extends DotAnalyticsPayload {
    type: EventType.PageView;
    properties: DotCMSAnalyticsProperties;
}

/**
 * Base properties from Analytics.js
 */
export interface BaseAnalyticsProperties {
    title: string;
    url: string;
    path: string;
    hash: string;
    search: string;
    width: number;
    height: number;
    source: string;
}

/**
 * Extended properties specific to DotCMS analytics
 */
export interface DotCMSAnalyticsProperties extends BaseAnalyticsProperties {
    // DotCMS-specific properties that can come from query params or context
    language_id?: string;
    persona?: string;
    // Allow additional dynamic properties
    [key: string]: unknown;
}

/**
 * The payload for a track event.
 */
export interface DotAnalyticsPayload {
    type: string;
    properties: DotCMSAnalyticsProperties;
    event: string;
    options: Record<string, unknown>;

    // Properties added by enricher plugin
    context: AnalyticsContext;
    page: PageData;
    device: DeviceData;
    utm?: UtmData;
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

/**
 * Analytics payload structure for page view events
 */
export interface AnalyticsContext {
    site_key: string;
    session_id: string;
    user_id: string;
}

/**
 * Device and browser information for analytics tracking
 */
export interface DeviceData {
    screen_resolution: string;
    language: string;
    viewport_width: string;
    viewport_height: string;
}

/**
 * UTM (Urchin Tracking Module) parameters for campaign tracking
 */
export interface UtmData {
    medium?: string;
    source?: string;
    campaign?: string;
    term?: string;
    content?: string;
}

/**
 * Page data structure for DotCMS analytics
 */
export interface PageData {
    url: string;
    doc_encoding: string;
    doc_hash: string;
    doc_protocol: string;
    doc_search: string;
    dot_host: string;
    dot_path: string;
    title: string;
    user_agent?: string;
    language_id?: string;
    persona?: string;
}

/**
 * Analytics.js hook parameter types
 */
export interface AnalyticsHookPayload {
    type: string;
    properties: {
        title: string;
        url: string;
        path: string;
        hash: string;
        search: string;
        width: number;
        height: number;
        referrer?: string;
    };
    options: Record<string, unknown>;
    userId: string | null;
    anonymousId: string;
    meta: {
        rid: string;
        ts: number;
        hasCallback: boolean;
    };
}

export interface AnalyticsInstance {
    plugins: Record<string, unknown>;
    storage: Record<string, unknown>;
    events: {
        core: string[];
        plugins: string[];
    };
}

export interface AnalyticsHookParams {
    payload: AnalyticsHookPayload;
    instance: AnalyticsInstance;
    config: Record<string, unknown>;
    plugins: Record<
        string,
        {
            enabled: boolean;
            initialized: boolean;
            loaded: boolean;
            config: Record<string, unknown>;
        }
    >;
}
