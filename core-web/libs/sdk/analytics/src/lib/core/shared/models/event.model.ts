/**
 * Event models for DotCMS Analytics
 * Contains interfaces for different types of analytics events
 */

import {
    DotCMSContentImpressionPageData,
    DotCMSEventPageData,
    DotCMSEventUtmData
} from './data.model';

import {
    DotCMSCustomEventType,
    DotCMSEventType,
    DotCMSPredefinedEventType
} from '../constants/dot-analytics.constants';

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
export interface DotCMSEventBase<TEventType extends DotCMSEventType, TData> {
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
 * Data structure for custom events.
 * Contains user-defined custom data.
 */
export type DotCMSCustomEventData = {
    /** Custom data associated with the event (any valid JSON) */
    custom: JsonObject;
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
 * Conversion payload sent when tracking conversions.
 * Contains conversion name and optional custom data.
 */
export type DotCMSConversionPayload = {
    /** Name of the conversion event */
    name: string;
    /** Optional custom user data (any valid JSON) */
    custom?: JsonObject;
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
    /** Optional custom user data (any valid JSON) */
    custom?: JsonObject;
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
 * Custom event structure.
 */
export type DotCMSCustomEvent = DotCMSEventBase<DotCMSCustomEventType, DotCMSCustomEventData>;

/**
 * Union type for all possible analytics events.
 * Used primarily for type documentation and validation.
 */
export type DotCMSEvent =
    | DotCMSPageViewEvent
    | DotCMSContentImpressionEvent
    | DotCMSContentClickEvent
    | DotCMSConversionEvent
    | DotCMSCustomEvent;

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
