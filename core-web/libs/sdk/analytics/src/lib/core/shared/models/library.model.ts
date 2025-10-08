/**
 * Library internal models for DotCMS Analytics
 * Contains interfaces for SDK/library internal structures (not for end users)
 */

import {
    DotCMSAnalyticsEventContext,
    DotCMSEventDeviceData,
    DotCMSEventPageData,
    DotCMSEventUtmData
} from './data.model';
import { JsonObject } from './event.model';
import { DotCMSAnalyticsRequestBody } from './request.model';

import { DotCMSCustomEventType } from '../constants';

/**
 * Main interface for the DotCMS Analytics SDK.
 * Provides the core methods for tracking page views and custom events.
 */
export interface DotCMSAnalytics {
    /**
     * Track a page view event.
     * @param payload - Optional custom data to include with the page view (any valid JSON object)
     */
    pageView: (payload?: JsonObject) => void;

    /**
     * Track a custom event.
     * @param eventName - The name/type of the event to track
     * @param payload - Custom data to include with the event (any valid JSON object)
     */
    track: (eventName: string, payload: JsonObject) => void;
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
     * The site auth for authenticating with the Analytics service.
     */
    siteAuth: string;
}

/**
 * Track event payload with context.
 * This is the payload for custom track events after the identity plugin adds context.
 * Used in the track:dot-analytics enricher plugin.
 */
export interface AnalyticsTrackPayloadWithContext extends AnalyticsBasePayload {
    /** The custom event name (any string except 'pageview') */
    event: DotCMSCustomEventType;
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
 */
export interface AnalyticsBasePayload {
    /** The type of analytics event */
    type: AnalyticsBasePayloadType;
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
 * and then adding page, device, UTM, and custom data (from enricher plugin).
 */
export type EnrichedAnalyticsPayload = AnalyticsBasePayloadWithContext & {
    /** Page data for the current page */
    page: DotCMSEventPageData;
    /** Device and browser information */
    device: DotCMSEventDeviceData;
    /** UTM parameters for campaign tracking */
    utm?: DotCMSEventUtmData;
    /** Custom data associated with the event (any valid JSON) */
    custom?: JsonObject;
    /** Local timestamp when the event occurred */
    local_time: string;
};

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
