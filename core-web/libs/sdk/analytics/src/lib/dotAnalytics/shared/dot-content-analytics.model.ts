/**
 * Configuration interface for DotAnalytics SDK.
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
     * The site key for authenticating with the Analytics service.
     */
    siteKey: string;

    /**
     * Custom redirect function handler.
     * When provided, this function will be called instead of the default browser redirect
     * for handling URL redirections.
     */
    redirectFn?: (url: string) => void;
}

/**
 * Individual analytics event structure.
 * Represents a single event within an analytics request.
 */
export interface DotAnalyticsEvent {
    event_type: 'pageview' | 'track';
    page: PageData;
    device: DeviceData;
    utm?: UtmData;
}

/**
 * Analytics request body for page view events.
 * Structure sent to the analytics server for page tracking.
 */
export interface PageViewRequestBody extends Record<string, unknown> {
    context: DotAnalyticsContext;
    events: DotAnalyticsEvent[];
}

/**
 * Analytics request body for track events.
 * Structure sent to the analytics server for custom event tracking.
 */
export interface TrackRequestBody extends Record<string, unknown> {
    key: string;
}

/**
 * Browser event data collected from the user's session.
 * Contains comprehensive information about the user's browser environment,
 * page context, and session details for analytics tracking.
 */
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
    utm: Record<string, string>;
}

/**
 * The payload structure for analytics events.
 * This interface represents the complete data structure that flows through
 * the analytics pipeline, including original event data and enriched context.
 */
export interface DotAnalyticsPayload {
    type: string;
    properties: Record<string, unknown>;
    event: string;
    options: Record<string, unknown>;

    // Properties added by enricher plugin
    context: DotAnalyticsContext;
    page: PageData;
    device: DeviceData;
    utm?: UtmData;
}

/**
 * Parameters passed to DotAnalytics plugin methods.
 * Contains the configuration and payload data needed for processing analytics events.
 */
export interface DotAnalyticsParams {
    config: DotContentAnalyticsConfig;
    payload: DotAnalyticsPayload;
}

/**
 * Main interface for the DotAnalytics SDK.
 * Provides the core methods for tracking page views and custom events.
 */
export interface DotAnalytics {
    /**
     * Track a page view event.
     * @param payload - Optional additional data to include with the page view
     */
    pageView: (payload?: Record<string, unknown>) => void;

    /**
     * Track a custom event.
     * @param eventName - The name/type of the event to track
     * @param payload - Optional additional data to include with the event
     */
    track: (eventName: string, payload?: Record<string, unknown>) => void;
}

/**
 * Analytics context shared across all events.
 * Contains session and user identification data that provides
 * continuity across multiple analytics events.
 */
export interface DotAnalyticsContext {
    site_key: string;
    session_id: string;
    user_id: string;
}

/**
 * Device and browser information for analytics tracking.
 * Contains technical details about the user's device and browser environment.
 */
export interface DeviceData {
    screen_resolution: string;
    language: string;
    viewport_width: string;
    viewport_height: string;
}

/**
 * UTM (Urchin Tracking Module) parameters for campaign tracking.
 * Contains marketing campaign attribution data extracted from URL parameters.
 */
export interface UtmData {
    medium?: string;
    source?: string;
    campaign?: string;
    term?: string;
    content?: string;
}

/**
 * Page data structure for DotCMS analytics.
 * Contains comprehensive information about the current page and its context
 * within the DotCMS environment.
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
 * Analytics.js hook parameter types.
 * Represents the payload structure used by Analytics.js lifecycle hooks
 * for intercepting and modifying analytics events.
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

/**
 * Analytics.js instance structure.
 * Represents the internal structure of an Analytics.js instance,
 * providing access to plugins, storage, and event configuration.
 */
export interface AnalyticsInstance {
    plugins: Record<string, unknown>;
    storage: Record<string, unknown>;
    events: {
        core: string[];
        plugins: string[];
    };
}

/**
 * Parameters passed to Analytics.js hook functions.
 * Contains all the context and data needed for Analytics.js lifecycle hooks
 * to process and modify analytics events.
 */
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
