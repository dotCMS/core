// Extend Window interface to include our custom properties
declare global {
    interface Window {
        __dotAnalyticsCleanup?: () => void;
    }
}

/**
 * Configuration interface for DotCMS Analytics SDK.
 * Contains all necessary settings for initializing and configuring the analytics client.
 */
export interface DotCMSAnalyticsConfig {
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
 * Individual analytics event structure for DotCMS.
 * Represents a single event within an analytics request sent to the DotCMS analytics server.
 */
export interface DotCMSAnalyticsEvent {
    /** The type of event being tracked */
    event_type: 'pageview' | 'track';
    /** Page data associated with the event */
    page: DotCMSPageData;
    /** Device and browser information */
    device: DotCMSDeviceData;
    /** UTM parameters for campaign tracking */
    utm?: DotCMSUtmData;
    /** Local timestamp when the event occurred */
    local_time: string;
}

/**
 * Analytics request body for page view events in DotCMS.
 * Structure sent to the DotCMS analytics server for page tracking.
 */
export interface DotCMSPageViewRequestBody {
    /** Context information shared across all events */
    context: DotCMSAnalyticsContext;
    /** Array of analytics events to be tracked */
    events: DotCMSAnalyticsEvent[];
}

// TODO: Add properties to the track request body
/**
 * Analytics request body for track events in DotCMS.
 * Structure sent to the DotCMS analytics server for custom event tracking.
 */
export interface DotCMSTrackRequestBody {
    /** Context information shared across all events */
    context: DotCMSAnalyticsContext;
    /** Array of analytics events to be tracked */
    events?: DotCMSAnalyticsEvent[];
}

/**
 * Browser event data collected from the user's session in DotCMS.
 * Contains comprehensive information about the user's browser environment,
 * page context, and session details for analytics tracking.
 */
export interface DotCMSBrowserEventData {
    /** UTC timestamp when the event occurred */
    utc_time: string;
    /** Local timezone offset in minutes */
    local_tz_offset: number;
    /** Screen resolution as a string (e.g., "1920x1080") */
    screen_resolution: string | undefined;
    /** Viewport size as a string (e.g., "1200x800") */
    vp_size: string | undefined;
    /** User's preferred language */
    user_language: string | undefined;
    /** Document encoding */
    doc_encoding: string | undefined;
    /** Document path */
    doc_path: string | undefined;
    /** Document host */
    doc_host: string | undefined;
    /** Document protocol (http/https) */
    doc_protocol: string | undefined;
    /** Document hash fragment */
    doc_hash: string;
    /** Document search parameters */
    doc_search: string;
    /** Referrer URL */
    referrer: string | undefined;
    /** Page title */
    page_title: string | undefined;
    /** Current page URL */
    url: string | undefined;
    /** UTM parameters for campaign tracking */
    utm: Record<string, string>;
}

/**
 * The payload structure for DotCMS analytics events.
 * This interface represents the complete data structure that flows through
 * the analytics pipeline, including original event data and enriched context.
 */
export interface DotCMSAnalyticsPayload {
    /** The type of analytics event */
    type: string;
    /** Additional properties associated with the event */
    properties: Record<string, unknown>;
    /** The event name or identifier */
    event: string;
    /** Configuration options for the event */
    options: Record<string, unknown>;

    // Properties added by enricher plugin
    /** Analytics context shared across events */
    context: DotCMSAnalyticsContext;
    /** Page data for the current page */
    page: DotCMSPageData;
    /** Device and browser information */
    device: DotCMSDeviceData;
    /** UTM parameters for campaign tracking */
    utm?: DotCMSUtmData;
    /** Local timestamp when the event occurred */
    local_time: string;
}

/**
 * Parameters passed to DotCMS Analytics plugin methods.
 * Contains the configuration and payload data needed for processing analytics events.
 */
export interface DotCMSAnalyticsParams {
    /** Configuration for the analytics client */
    config: DotCMSAnalyticsConfig;
    /** The event payload to be processed */
    payload: DotCMSAnalyticsPayload;
}

/**
 * Main interface for the DotCMS Analytics SDK.
 * Provides the core methods for tracking page views and custom events.
 */
export interface DotCMSAnalytics {
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
 * Analytics context shared across all events in DotCMS.
 * Contains session and user identification data that provides
 * continuity across multiple analytics events.
 */
export interface DotCMSAnalyticsContext {
    /** The site key for the DotCMS instance */
    site_key: string;
    /** Unique session identifier */
    session_id: string;
    /** Unique user identifier */
    user_id: string;
}

/**
 * Device and browser information for DotCMS analytics tracking.
 * Contains technical details about the user's device and browser environment.
 */
export interface DotCMSDeviceData {
    /** Screen resolution as a string (e.g., "1920x1080") */
    screen_resolution: string | undefined;
    /** User's preferred language */
    language: string | undefined;
    /** Viewport width in pixels */
    viewport_width: string | undefined;
    /** Viewport height in pixels */
    viewport_height: string | undefined;
}

/**
 * UTM (Urchin Tracking Module) parameters for DotCMS campaign tracking.
 * Contains marketing campaign attribution data extracted from URL parameters.
 */
export interface DotCMSUtmData {
    /** The marketing medium (e.g., email, social, cpc) */
    medium?: string;
    /** The traffic source (e.g., google, newsletter) */
    source?: string;
    /** The campaign name */
    campaign?: string;
    /** The campaign term or keyword */
    term?: string;
    /** The campaign content or ad variation */
    content?: string;
}

/**
 * Page data structure for DotCMS analytics.
 * Contains comprehensive information about the current page and its context
 * within the DotCMS environment.
 */
export interface DotCMSPageData {
    /** The current page URL */
    url: string | undefined;
    /** Document encoding */
    doc_encoding: string | undefined;
    /** Document hash fragment */
    doc_hash: string;
    /** Document protocol (http/https) */
    doc_protocol: string | undefined;
    /** Document search parameters */
    doc_search: string;
    /** DotCMS host domain */
    dot_host: string | undefined;
    /** DotCMS page path */
    dot_path: string | undefined;
    /** Page title */
    title: string | undefined;
    /** User agent string */
    user_agent?: string;
    /** Language identifier */
    language_id?: string;
    /** Persona identifier */
    persona?: string;
}

/**
 * Analytics.js hook parameter types for DotCMS.
 * Represents the payload structure used by Analytics.js lifecycle hooks
 * for intercepting and modifying analytics events.
 */
export interface DotCMSAnalyticsHookPayload {
    /** The type of analytics event */
    type: string;
    /** Properties associated with the event */
    properties: {
        /** Page title */
        title: string;
        /** Page URL */
        url: string;
        /** Page path */
        path: string;
        /** URL hash fragment */
        hash: string;
        /** URL search parameters */
        search: string;
        /** Viewport width */
        width: number;
        /** Viewport height */
        height: number;
        /** Referrer URL */
        referrer?: string;
    };
    /** Configuration options for the event */
    options: Record<string, unknown>;
    /** User identifier */
    userId: string | null;
    /** Anonymous user identifier */
    anonymousId: string;
    /** Metadata about the event */
    meta: {
        /** Request identifier */
        rid: string;
        /** Timestamp */
        ts: number;
        /** Whether the event has a callback function */
        hasCallback: boolean;
    };
}

/**
 * Analytics.js instance structure for DotCMS.
 * Represents the internal structure of an Analytics.js instance,
 * providing access to plugins, storage, and event configuration.
 */
export interface DotCMSAnalyticsInstance {
    /** Available plugins and their configurations */
    plugins: Record<string, unknown>;
    /** Storage mechanisms for analytics data */
    storage: Record<string, unknown>;
    /** Event configuration */
    events: {
        /** Core event types */
        core: string[];
        /** Plugin-specific event types */
        plugins: string[];
    };
}

/**
 * Parameters passed to Analytics.js hook functions in DotCMS.
 * Contains all the context and data needed for Analytics.js lifecycle hooks
 * to process and modify analytics events.
 */
export interface DotCMSAnalyticsHookParams {
    /** The event payload data */
    payload: DotCMSAnalyticsHookPayload;
    /** The analytics instance */
    instance: DotCMSAnalyticsInstance;
    /** Global configuration settings */
    config: Record<string, unknown>;
    /** Available plugins and their status */
    plugins: Record<
        string,
        {
            /** Whether the plugin is enabled */
            enabled: boolean;
            /** Whether the plugin is initialized */
            initialized: boolean;
            /** Whether the plugin is loaded */
            loaded: boolean;
            /** Plugin-specific configuration */
            config: Record<string, unknown>;
        }
    >;
}
