/**
 * Request/Response models for DotCMS Analytics
 * Contains interfaces for HTTP request and response structures
 */

import { DotCMSAnalyticsEventContext } from './data.model';
import { DotCMSEvent } from './event.model';

/**
 * Analytics request body for DotCMS Analytics.
 * Generic structure sent to the DotCMS analytics server.
 *
 * This structure contains properly typed events that match the DotCMS event specifications.
 * Events can be pageviews, content impressions, or custom events, each with their own data structure.
 */
export interface DotCMSRequestBody {
    /** Context information shared across all events */
    context: DotCMSAnalyticsEventContext;
    /** Array of analytics events to be tracked */
    events: DotCMSEvent[];
}

/**
 * Main type for analytics request bodies.
 * Flexible enough to accept any event type (pageview, content_impression, custom, etc.)
 */
export type DotCMSAnalyticsRequestBody = DotCMSRequestBody;

/**
 * Specific request body type for PageView events (for type documentation)
 */
export type DotCMSPageViewRequestBody = DotCMSRequestBody;

/**
 * Specific request body type for ContentImpression events (for type documentation)
 */
export type DotCMSContentImpressionRequestBody = DotCMSRequestBody;

/**
 * Specific request body type for Custom events (for type documentation)
 */
export type DotCMSCustomEventRequestBody = DotCMSRequestBody;
