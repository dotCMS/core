/**
 * The pipeline's internal types: its config, the payloads its plugins pass along, and the
 * shapes of the events and requests dotCMS receives.
 */

import type { DotCMSPredefinedEventType } from './constants';
import type { LogLevel } from './logger';

/**
 * Configuration for event queue management.
 * Controls how events are batched before sending to the server.
 */
export interface QueueConfig {
    /** Maximum events per batch - auto-sends when reached (default: 15) */
    eventBatchSize?: number;
    /** Time in milliseconds between flushes - sends pending events (default: 5000) */
    flushInterval?: number;
}

/**
 * Configuration for content impression tracking.
 * Controls how content visibility is detected and tracked.
 */
export interface ImpressionConfig {
    /** Minimum percentage of element visible (0.0 to 1.0) - default: 0.5 */
    visibilityThreshold?: number;
    /** Minimum time in milliseconds element must be visible - default: 750 */
    dwellMs?: number;
    /** Maximum number of elements to track (performance limit) - default: 1000 */
    maxNodes?: number;
    /** Throttle time in milliseconds for intersection callbacks - default: 100 */
    throttleMs?: number;
}

/**
 * Interface for contentlet data extracted from DOM elements
 */
export interface ContentletData {
    identifier: string;
    inode: string;
    contentType: string;
    title: string;
    baseType: string;
}

/**
 * Interface for viewport metrics
 */
export interface ViewportMetrics {
    offsetPercentage: number;
    visibilityRatio: number;
}

/**
 * Configuration interface for DotCMS Analytics SDK.
 * Contains all necessary settings for initializing and configuring the analytics client.
 */
export interface PipelineConfig {
    /**
     * The URL of the Analytics server endpoint.
     */
    server: string;

    /**
     * Enable debug mode to get additional logging information.
     */
    debug: boolean;

    /**
     * Set the minimum log level for console output.
     * - 'debug': Show all logs including detailed debugging information
     * - 'info': Show informational messages, warnings, and errors
     * - 'warn': Show only warnings and errors
     * - 'error': Show only errors
     *
     * If not specified, falls back to debug flag (debug=true → 'debug', debug=false → 'warn')
     */
    logLevel?: LogLevel;

    /**
     * Automatically track page views. Defaults to `true` (opt-out).
     * Set explicitly to `false` to disable automatic page view tracking.
     * Via the HTML `data-analytics-auto-page-view` attribute, only the literal string "false"
     * disables it (any other value, including "", enables it).
     */
    autoPageView?: boolean;

    /**
     * The site auth for authenticating with the Analytics service.
     */
    siteAuth: string;

    /**
     * Queue configuration for event batching:
     * - `false`: Disable queuing, send events immediately
     * - `true` or `undefined` (default): Enable queuing with default settings
     * - `QueueConfig`: Enable queuing with custom settings
     */
    queue?: QueueConfig | boolean;

    /**
     * Content impression tracking configuration (default: undefined - disabled):
     * - `undefined` or `false`: Impression tracking disabled
     * - `true`: Enable with default settings (threshold: 0.5, dwell: 750ms, maxNodes: 1000)
     * - `ImpressionConfig`: Enable with custom settings
     */
    impressions?: ImpressionConfig | boolean;

    /**
     * Content click tracking configuration (default: undefined - disabled):
     * - `undefined` or `false`: Click tracking disabled
     * - `true`: Enable click tracking
     */
    clicks?: boolean;

    /** Called when an events request fails. It must not throw. */
    onError?: (error: PipelineError) => void;
}

/**
 * An events request that failed. Events are never retried.
 */
export interface PipelineError {
    /**
     * `REJECTED`: dotCMS answered with an error, or accepted the request but failed some of its
     * events. `NETWORK`: the request got no answer.
     */
    code: 'REJECTED' | 'NETWORK';
    message: string;
    /** The HTTP status, when dotCMS answered. */
    status?: number;
    /** What dotCMS said about the failure: its error list, when it sent one. */
    detail?: unknown;
    /** How many events the request carried. */
    events: number;
}

/**
 * Track event payload with context.
 * This is the payload for track events after the identity plugin adds context.
 * Used in the enricher's track hook.
 */
export interface AnalyticsTrackPayloadWithContext extends AnalyticsBasePayload {
    /** The event name (can be predefined or custom) */
    event: string;
    /** Analytics context added by identity plugin */
    context: EventContext;
}

type AnalyticsBasePayloadType = 'page' | 'track';

/**
 * Analytics.js hook parameter types for DotCMS.
 * Represents the payload structure used by Analytics.js lifecycle hooks
 * for intercepting and modifying analytics events.
 *
 * Properties are flexible (Record<string, unknown>) to support both:
 * - Page events: with page-specific fields (title, url, path, etc.)
 * - Track events: with any custom event data structure
 */
export interface AnalyticsBasePayload {
    /** The type of analytics event */
    type: AnalyticsBasePayloadType;
    /** Properties associated with the event (flexible structure) */
    properties: Record<string, unknown>;
    /** Configuration options for the event */
    options: Record<string, unknown>;
    /** User identifier */
    userId: string;
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
 * Analytics.js payload with context.
 * This is the result of enriching the base Analytics.js payload
 * with context data added by the identity plugin.
 */
export interface AnalyticsBasePayloadWithContext extends AnalyticsBasePayload {
    context: EventContext;
}

/**
 * Enriched analytics payload with DotCMS-specific data.
 * This is the result of enriching the base Analytics.js payload with context (from identity plugin)
 * and then adding page, UTM, and custom data (from enricher plugin).
 */
export type EnrichedAnalyticsPayload = AnalyticsBasePayloadWithContext & {
    /** Page data for the current page */
    page: DotCMSEventPageData;
    /** UTM parameters for campaign tracking */
    utm?: DotCMSEventUtmData;
    /** Custom data associated with the event (any valid JSON) */
    custom?: JsonObject;
    /** Local timestamp when the event occurred */
    local_time: string;
};

/**
 * Enriched track event payload with fields added to root based on event type.
 * Used by the enricher plugin for track events.
 */
export interface EnrichedTrackPayload extends AnalyticsTrackPayloadWithContext {
    local_time: string;
    page?: DotCMSContentImpressionPageData | DotCMSEventPageData;
    content?: DotCMSContentImpressionPayload['content'];
    position?: DotCMSContentImpressionPayload['position'];
    utm?: DotCMSEventUtmData;
}

/**
 * Analytics.js instance structure for DotCMS.
 * Represents the internal structure of an Analytics.js instance,
 * providing access to plugins, storage, and event configuration.
 */
export interface AnalyticsPluginInstance {
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
 * Base parameters structure passed by Analytics.js to plugin hooks.
 * Contains all the context and data needed for Analytics.js lifecycle hooks
 * to process and modify analytics events.
 */
export interface AnalyticsBaseParams {
    /** The event payload data */
    payload: AnalyticsBasePayload;
    /** The analytics instance */
    instance: AnalyticsPluginInstance;
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

/**
 * Browser event data collected from the user's session in DotCMS.
 * Contains comprehensive information about the user's browser environment,
 * page context, and session details for analytics tracking.
 *
 * This is an internal type used by utility functions.
 */
export interface DotCMSBrowserData {
    /** UTC timestamp when the event occurred */
    utc_time: string;
    /** Local timezone offset in minutes */
    local_tz_offset: number;
    /** Screen resolution as a string (e.g., "1920x1080") */
    screen_resolution: string;
    /** Viewport size as a string (e.g., "1200x800") */
    vp_size: string;
    /** User's preferred language */
    user_language: string;
    /** Document encoding */
    doc_encoding: string;
    /** Document path */
    doc_path: string;
    /** Document host */
    doc_host: string;
    /** Document protocol (http/https) */
    doc_protocol: string;
    /** Document hash fragment */
    doc_hash: string;
    /** Document search parameters */
    doc_search: string;
    /** Referrer URL */
    referrer: string;
    /** Page title */
    page_title: string;
    /** Current page URL */
    url: string;
    /** UTM parameters for campaign tracking */
    utm: DotCMSEventUtmData;
}

/**
 * Analytics context shared across all events in DotCMS.
 * Contains session and user identification data that provides
 * continuity across multiple analytics events.
 */
export interface EventContext {
    /** The site key for the DotCMS instance */
    site_auth: string;
    /** Unique session identifier */
    session_id: string;
    /** Unique user identifier */
    user_id: string;

    /** Device and browser information */
    device: DotCMSEventDeviceData;

    /**
     * Experiments whose page the visitor reached in this session, cumulative.
     * Left out when the visitor has reached none.
     */
    experiments?: DotCMSEventContextExperiment[];
}

/**
 * One experiment in `context.experiments`, in the shape CAEM stores.
 */
export interface DotCMSEventContextExperiment {
    /** Experiment identifier */
    id: string;
    /** Identifier of the experiment's current run */
    running_id: string;
    /** Name of the variant the visitor was assigned */
    variant: string;
}

/**
 * Device and browser information for DotCMS analytics tracking.
 * Contains technical details about the user's device and browser environment.
 */
export interface DotCMSEventDeviceData {
    /** Screen resolution as a string (e.g., "1920x1080") */
    screen_resolution: string;
    /** User's preferred language */
    language: string;
    /** Viewport width in pixels */
    viewport_width: string;
    /** Viewport height in pixels */
    viewport_height: string;
}

/**
 * UTM (Urchin Tracking Module) parameters for DotCMS campaign tracking.
 * Contains marketing campaign attribution data extracted from URL parameters.
 */
export interface DotCMSEventUtmData {
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
    /** The campaign ID for tracking specific campaigns */
    id?: string;
}

/**
 * Page data structure for DotCMS analytics (used in pageview events).
 * Contains comprehensive information about the current page and its context
 * within the DotCMS environment.
 */
export type DotCMSEventPageData = Pick<
    DotCMSBrowserData,
    'url' | 'doc_path' | 'doc_hash' | 'doc_search' | 'doc_host' | 'doc_protocol' | 'doc_encoding'
> & {
    /** Page title */
    title: string | undefined;
    /** Locale identifier (e.g., es-es, en-us) */
    locale_id: string;
    /** Persona identifier */
    persona?: string;
};

/**
 * Minimal page data for content impression events.
 * Contains only essential page information (title and url) to keep payload lightweight.
 */
export type DotCMSContentImpressionPageData = Pick<DotCMSEventPageData, 'title' | 'url'>;

/**
 * Data structure for content impression events.
 * Tracks when a contentlet becomes visible in the viewport.
 */
export interface DotCMSImpressionEventData {
    /** Contentlet identification data extracted from data-dot-analytics-* attributes */
    contentlet: {
        /** Unique identifier of the contentlet */
        identifier: string;
        /** Inode of the contentlet */
        inode: string;
        /** Content type name */
        contentType: string;
        /** Title of the contentlet */
        title: string;
        /** Base type of the contentlet (e.g., CONTENT, WIDGET) */
        baseType: string;
    };
    /** Viewport position and visibility metrics */
    viewport: {
        /** Percentage offset from top of viewport (0-100) */
        offsetPercentage: number;
        /** Percentage of element visible in viewport (0-1) */
        visibilityRatio: number;
    };
    /** Timing information about the impression */
    timing: {
        /** Time in milliseconds the element was continuously visible */
        dwellTime: number;
        /** ISO 8601 timestamp when the impression was fired */
        timestamp: string;
    };
}

/**
 * JSON value type for analytics custom data.
 * Represents any valid JSON value that can be serialized and sent to the analytics server.
 */
export type JsonValue = string | number | boolean | null | undefined | JsonObject | JsonArray;

/**
 * JSON object type for analytics custom data.
 */
export type JsonObject = { [key: string]: JsonValue };

/**
 * JSON array type for analytics custom data.
 */
export type JsonArray = JsonValue[];

/**
 * Generic base event structure for DotCMS Analytics.
 * All events share this base structure with customizable event type and data.
 *
 * @template TEventType - The type of the event (pageview, custom event name, etc.)
 * @template TData - The data structure for the event
 */
export interface DotCMSEventBase<TEventType extends DotCMSPredefinedEventType, TData> {
    /** The type of event being tracked */
    event_type: TEventType;
    /** Local timestamp when the event occurred */
    local_time: string;
    /** Event-specific data with structured format */
    data: TData;
}

/**
 * Data structure for pageview events.
 * Contains page and optional UTM/custom data.
 */
export type DotCMSPageViewEventData = {
    /** Page data associated with the event */
    page: DotCMSEventPageData;
    /** UTM parameters for campaign tracking (optional) */
    utm?: DotCMSEventUtmData;
    /** Custom data associated with the event (any valid JSON) */
    custom?: JsonObject;
};

/**
 * Element information for analytics events.
 * Base type for element metadata in click and conversion events.
 */
export type DotCMSElementData = {
    /** Text content of the element */
    text: string;
    /** Type of element (anchor, button, input, etc.) */
    type: string;
    /** Element ID (required by backend, empty string if not present) */
    id: string;
    /** Element classes (required by backend, empty string if not present) */
    class: string;
    /** Link destination as written in HTML (relative path, only for <a> elements, empty string for buttons) */
    href: string;
    /** Additional element attributes in key:value format (e.g., ['data-category:val', 'data-campaign:val2']) */
    attributes: string[];
};

/**
 * Partial content impression data sent by producer plugins.
 * Contains only impression-specific data (content and position).
 * The enricher plugin will add page data automatically.
 */
export type DotCMSContentImpressionPayload = {
    /** Content information */
    content: {
        /** Content identifier */
        identifier: string;
        /** Content inode */
        inode: string;
        /** Content title */
        title: string;
        /** Content type name */
        content_type: string;
    };
    /** Position information in the viewport and DOM */
    position: {
        /** Viewport offset percentage from top */
        viewport_offset_pct: number;
        /** DOM index position */
        dom_index: number;
    };
};

/**
 * Partial content click data sent by producer plugins.
 * Extends impression payload with element metadata.
 */
export type DotCMSContentClickPayload = DotCMSContentImpressionPayload & {
    /** Clicked element information */
    element: DotCMSElementData;
};

/**
 * Conversion payload sent when tracking conversions: its name. dotCMS's conversion schema
 * accepts no custom data.
 */
export type DotCMSConversionPayload = {
    /** Name of the conversion event */
    name: string;
};

/**
 * Complete data structure for content impression events after enrichment.
 * Includes minimal page data (title and url) added by the enricher plugin.
 */
export type DotCMSContentImpressionEventData = DotCMSContentImpressionPayload & {
    /** Minimal page data where the impression occurred (added by enricher) */
    page: DotCMSContentImpressionPageData;
};

/**
 * Complete data structure for content click events after enrichment.
 * Includes minimal page data (title and url) added by the enricher plugin.
 */
export type DotCMSContentClickEventData = DotCMSContentClickPayload & {
    /** Minimal page data where the click occurred (added by enricher) */
    page: DotCMSContentImpressionPageData;
};

/**
 * Complete data structure for conversion events after enrichment.
 * Includes page data added by the enricher plugin.
 */
export type DotCMSConversionEventData = {
    /** Conversion information */
    conversion: {
        /** Name of the user-defined conversion */
        name: string;
    };
    /** Page data where the conversion occurred (added by enricher) */
    page: DotCMSContentImpressionPageData;
};

/**
 * Pageview event structure.
 */
export type DotCMSPageViewEvent = DotCMSEventBase<
    typeof DotCMSPredefinedEventType.PAGEVIEW,
    DotCMSPageViewEventData
>;

/**
 * Content impression event structure.
 */
export type DotCMSContentImpressionEvent = DotCMSEventBase<
    typeof DotCMSPredefinedEventType.CONTENT_IMPRESSION,
    DotCMSContentImpressionEventData
>;

/**
 * Content click event structure.
 */
export type DotCMSContentClickEvent = DotCMSEventBase<
    typeof DotCMSPredefinedEventType.CONTENT_CLICK,
    DotCMSContentClickEventData
>;

/**
 * Conversion event structure.
 */
export type DotCMSConversionEvent = DotCMSEventBase<
    typeof DotCMSPredefinedEventType.CONVERSION,
    DotCMSConversionEventData
>;

/**
 * Union type for all possible analytics events.
 * Used primarily for type documentation and validation.
 */
export type DotCMSEvent =
    | DotCMSPageViewEvent
    | DotCMSContentImpressionEvent
    | DotCMSContentClickEvent
    | DotCMSConversionEvent;

/**
 * Structure for persisted queue in sessionStorage.
 * Used to preserve events across traditional page navigations.
 */
export interface PersistedQueue {
    /** Unique identifier for this browser tab */
    tabId: string;
    /** Timestamp when the queue was last persisted */
    timestamp: number;
    /** Array of events waiting to be sent */
    events: DotCMSEvent[];
}

/**
 * Analytics request body for DotCMS Analytics.
 * Generic structure sent to the DotCMS analytics server.
 *
 * This structure contains properly typed events that match the DotCMS event specifications.
 * Events can be pageviews, content impressions, or custom events, each with their own data structure.
 */
export interface DotCMSRequestBody {
    /** Context information shared across all events */
    context: EventContext;
    /** Array of analytics events to be tracked */
    events: DotCMSEvent[];
}

/**
 * Main type for analytics request bodies.
 * It carries any of the four event types dotCMS accepts.
 */
export type EventRequestBody = DotCMSRequestBody;

/**
 * Specific request body type for PageView events (for type documentation)
 */
export type DotCMSPageViewRequestBody = DotCMSRequestBody;

/**
 * Specific request body type for ContentImpression events (for type documentation)
 */
export type DotCMSContentImpressionRequestBody = DotCMSRequestBody;
