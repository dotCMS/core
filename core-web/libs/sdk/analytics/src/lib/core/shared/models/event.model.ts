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
 * Base structure for all analytics events.
 * All events share this common structure.
 */
export interface DotCMSEventBase {
    /** The type of event being tracked */
    event_type: DotCMSEventType;
    /** Local timestamp when the event occurred */
    local_time: string;
}

/**
 * Pageview-specific analytics event structure.
 * Contains data specific to page view tracking.
 */
export interface DotCMSPageViewEvent extends DotCMSEventBase {
    event_type: typeof DotCMSPredefinedEventType.PAGEVIEW;
    /** Pageview-specific event data with structured format */
    data: {
        /** Page data associated with the event */
        page: DotCMSEventPageData;
        /** Device and browser information */
        device: DotCMSEventDeviceData;
        /** UTM parameters for campaign tracking (optional) */
        utm?: DotCMSEventUtmData;
        /** Custom data associated with the event */
        custom?: Record<string, unknown>;
    };
}

/**
 * Custom event structure.
 * Contains data for user-defined custom events.
 */
export interface DotCMSCustomEvent extends DotCMSEventBase {
    /** The type of event being tracked (any string except structured events) */
    event_type: DotCMSCustomEventType;
    /** Custom event data with structured format */
    data: {
        /** Custom data associated with the event */
        custom: Record<string, unknown>;
    };
}

/**
 * Union type for all possible analytics events.
 */
export type DotCMSEvent = DotCMSPageViewEvent | DotCMSCustomEvent;
