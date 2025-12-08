/**
 * Library internal models for DotCMS Analytics
 * Contains interfaces for SDK/library internal structures (not for end users)
 */

import {
    DotCMSAnalyticsEventContext,
    DotCMSContentImpressionPageData,
    DotCMSEventPageData,
    DotCMSEventUtmData
} from './data.model';
import { DotCMSContentImpressionPayload, JsonObject } from './event.model';
import { DotCMSAnalyticsRequestBody } from './request.model';

import { LogLevel } from '../dot-analytics.logger';

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
 * Main interface for the DotCMS Analytics SDK.
 * Provides the core methods for tracking page views, custom events, and conversions.
 */
export interface DotCMSAnalytics {
    /**
     * Track a page view event.
     * @param payload - Optional custom data to include with the page view (any valid JSON object)
     */
    pageView(): void;
    pageView(payload: JsonObject): void;

    /**
     * Track a custom event.
     * @param eventName - The name/type of the event to track
     * @param payload - Custom data to include with the event (any valid JSON object)
     */
    track(eventName: string): void;
    track(eventName: string, payload: JsonObject): void;

    /**
     * Track a conversion event.
     * @param name - Name of the conversion (e.g., 'purchase', 'download', 'signup')
     * @param options - Optional custom data and element information
     */
    conversion(name: string): void;
    conversion(name: string, options: JsonObject): void;
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
     * Automatically track page views when set to true.
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
}

/**
 * Track event payload with context.
 * This is the payload for track events after the identity plugin adds context.
 * Used in the track:dot-analytics enricher plugin.
 */
export interface AnalyticsTrackPayloadWithContext extends AnalyticsBasePayload {
    /** The event name (can be predefined or custom) */
    event: string;
    /** Analytics context added by identity plugin */
    context: DotCMSAnalyticsEventContext;
}

/**
 * Parameters passed to DotCMS Analytics plugin methods (after enrichment).
 * The payload is the complete request body ready to send to the server.
 */
export interface DotCMSAnalyticsParams {
    /** Configuration for the analytics client */
    config: DotCMSAnalyticsConfig;
    /** The complete request body */
    payload: DotCMSAnalyticsRequestBody;
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
    context: DotCMSAnalyticsEventContext;
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
export interface AnalyticsBaseParams {
    /** The event payload data */
    payload: AnalyticsBasePayload;
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
