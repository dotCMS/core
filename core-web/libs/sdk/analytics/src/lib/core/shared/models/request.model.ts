/**
 * Request/Response models for DotCMS Analytics
 * Contains interfaces for HTTP request and response structures
 */

import { DotCMSAnalyticsContext } from './data.model';
import { DotCMSCustomEvent, DotCMSEvent, DotCMSPageViewEvent } from './event.model';

/**
 * Analytics request body for DotCMS Analytics.
 * Generic structure sent to the DotCMS analytics server.
 */
export interface DotCMSRequestBody<T extends DotCMSEvent> {
    /** Context information shared across all events */
    context: DotCMSAnalyticsContext;
    /** Array of analytics events to be tracked */
    events: T[];
}

/**
 * Specific request body type for PageView events
 */
export type DotCMSPageViewRequestBody = DotCMSRequestBody<DotCMSPageViewEvent>;

/**
 * Specific request body type for Custom events
 */
export type DotCMSCustomEventRequestBody = DotCMSRequestBody<DotCMSCustomEvent>;

/**
 * Union type for all possible request bodies
 */
export type DotCMSAnalyticsRequestBody = DotCMSPageViewRequestBody | DotCMSCustomEventRequestBody;
