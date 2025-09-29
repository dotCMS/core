/**
 * Library internal models for DotCMS Analytics
 * Contains interfaces for SDK/library internal structures (not for end users)
 */

import {
    DotCMSAnalyticsContext,
    DotCMSDeviceData,
    DotCMSPageData,
    DotCMSUtmData
} from './data.model';
import { DotCMSAnalyticsRequestBody } from './request.model';

// Internal event type used by Analytics.js
type DotAnalyticsInternalEventType = 'pageview' | 'track';

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
     * The site auth for authenticating with the Analytics service.
     */
    siteAuth: string;
}

/**
 * The payload structure for DotCMS analytics events.
 * This interface represents the complete data structure that flows through
 * the analytics pipeline, including original event data and enriched context.
 *
 * This is the internal payload used by Analytics.js and our plugins.
 */
export interface DotCMSAnalyticsPayload {
    /** The event name or identifier */
    event: string;
    /** Additional properties associated with the event */
    properties: Record<string, unknown>;
    /** Configuration options for the event */
    options: Record<string, unknown>;

    // Properties added by plugins during processing
    /** Analytics context shared across events */
    context: DotCMSAnalyticsContext;
    /** Page data for the current page */
    page?: DotCMSPageData;
    /** Device and browser information */
    device?: DotCMSDeviceData;
    /** UTM parameters for campaign tracking */
    utm?: DotCMSUtmData;
    /** Local timestamp when the event occurred */
    local_time: string;

    /** Internal event type */
    type: DotAnalyticsInternalEventType;

    /** Custom data associated with the event */
    custom?: Record<string, unknown>;
}

/**
 * Parameters passed to DotCMS Analytics plugin methods (before enrichment).
 * Contains the configuration and raw payload data to be enriched.
 */
export interface DotCMSAnalyticsParams {
    /** Configuration for the analytics client */
    config: DotCMSAnalyticsConfig;
    /** The event payload to be processed (before enrichment) */
    payload: DotCMSAnalyticsPayload;
}

/**
 * Parameters passed to DotCMS Analytics plugin methods (after enrichment).
 * The payload is the complete request body ready to send to the server.
 */
export interface DotCMSAnalyticsEnrichedParams {
    /** Configuration for the analytics client */
    config: DotCMSAnalyticsConfig;
    /** The complete request body (enriched and ready to send) */
    payload: DotCMSAnalyticsRequestBody;
}

/**
 * Main interface for the DotCMS Analytics SDK.
 * Provides the core methods for tracking page views and custom events.
 */
export interface DotCMSAnalytics {
    /**
     * Track a page view event.
     */
    pageView: (payload?: Record<string, unknown>) => void;

    /**
     * Track a custom event.
     * @param eventName - The name/type of the event to track
     * @param payload - Optional additional data to include with the event
     */
    track: (eventName: string, payload: Record<string, unknown>) => void;
}

/**
 * Browser event data collected from the user's session in DotCMS.
 * Contains comprehensive information about the user's browser environment,
 * page context, and session details for analytics tracking.
 *
 * This is an internal type used by utility functions.
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
 * Base parameters structure passed by Analytics.js to plugin hooks.
 * Contains all the context and data needed for Analytics.js lifecycle hooks
 * to process and modify analytics events.
 */
export interface DotCMSAnalyticsBaseParams {
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
