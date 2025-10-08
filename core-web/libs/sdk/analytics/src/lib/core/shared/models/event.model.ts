/**
 * Event models for DotCMS Analytics
 * Contains interfaces for different types of analytics events
 */

import { DotCMSEventDeviceData, DotCMSEventPageData, DotCMSEventUtmData } from './data.model';

import {
    DotCMSCustomEventType,
    DotCMSEventType,
    DotCMSPredefinedEventType
} from '../constants/dot-content-analytics.constants';

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
 * Contains page, device, and optional UTM/custom data.
 */
export type DotCMSPageViewEventData = {
    /** Page data associated with the event */
    page: DotCMSEventPageData;
    /** Device and browser information */
    device: DotCMSEventDeviceData;
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
 * Pageview event structure.
 */
export type DotCMSPageViewEvent = DotCMSEventBase<
    typeof DotCMSPredefinedEventType.PAGEVIEW,
    DotCMSPageViewEventData
>;

/**
 * Custom event structure.
 */
export type DotCMSCustomEvent = DotCMSEventBase<DotCMSCustomEventType, DotCMSCustomEventData>;

/**
 * Union type for all possible analytics events.
 */
export type DotCMSEvent = DotCMSPageViewEvent | DotCMSCustomEvent;
