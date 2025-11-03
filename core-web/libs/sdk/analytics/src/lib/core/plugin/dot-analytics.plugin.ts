import { EnrichedTrackPayload } from './enricher/dot-analytics.enricher.plugin';

import { DotCMSPredefinedEventType } from '../shared/constants/dot-content-analytics.constants';
import { sendAnalyticsEvent } from '../shared/dot-content-analytics.http';
import { isPredefinedEventType } from '../shared/dot-content-analytics.utils';
import {
    DotCMSAnalyticsConfig,
    DotCMSAnalyticsRequestBody,
    DotCMSContentImpressionEvent,
    DotCMSContentImpressionEventData,
    DotCMSCustomEvent,
    DotCMSPageViewEvent,
    DotCMSPageViewEventData,
    EnrichedAnalyticsPayload,
    JsonObject
} from '../shared/models';
import { createAnalyticsQueue } from '../shared/queue';

/**
 * Creates a properly typed pageview event
 */
function createPageViewEvent(
    data: DotCMSPageViewEventData,
    localTime: string
): DotCMSPageViewEvent {
    return {
        event_type: DotCMSPredefinedEventType.PAGEVIEW,
        local_time: localTime,
        data
    };
}

/**
 * Creates a properly typed content impression event
 */
function createContentImpressionEvent(
    data: DotCMSContentImpressionEventData,
    localTime: string
): DotCMSContentImpressionEvent {
    const { title, url } = data.page;
    return {
        event_type: DotCMSPredefinedEventType.CONTENT_IMPRESSION,
        local_time: localTime,
        data: {
            content: data.content,
            position: data.position,
            page: { title, url }
        }
    };
}

/**
 * Creates a properly typed custom event
 */
function createCustomEvent(
    eventType: string,
    customData: JsonObject,
    localTime: string
): DotCMSCustomEvent {
    return {
        event_type: eventType,
        local_time: localTime,
        data: {
            custom: customData
        }
    };
}

/**
 * Analytics plugin for tracking page views and custom events in DotCMS applications.
 * This plugin handles:
 * 1. Event structuring (deciding between predefined and custom events)
 * 2. Building complete request bodies
 * 3. Sending analytics data to the DotCMS server
 * 4. Managing initialization and queue management
 *
 * The enricher plugin runs BEFORE this plugin and adds page/utm/custom data.
 * This plugin receives enriched payloads and structures them into proper events.
 *
 * @param {DotCMSAnalyticsConfig} config - Configuration object containing API key, server URL,
 *                                     debug mode, auto page view settings, and queue config
 * @returns {Object} Plugin object with methods for initialization and event tracking
 */
export const dotAnalytics = (config: DotCMSAnalyticsConfig) => {
    let isInitialized = false;
    // Queue is enabled if queue is not explicitly false
    const enableQueue = config.queue !== false;
    let queue: ReturnType<typeof createAnalyticsQueue> | null = null;

    /**
     * Sends event to queue or directly to server
     */
    const sendEvent = (requestBody: DotCMSAnalyticsRequestBody): void => {
        const event = requestBody.events[0];
        const context = requestBody.context;

        // Use queue or send directly
        if (enableQueue && queue) {
            queue.enqueue(event, context);
        } else {
            // Direct send without queue (when queue === false)
            sendAnalyticsEvent(requestBody, config);
        }
    };

    return {
        name: 'dot-analytics',
        config,

        /**
         * Initialize the plugin with optional queue management
         */
        initialize: () => {
            isInitialized = true;

            // Initialize queue if enabled (queue is undefined or an object)
            if (enableQueue) {
                queue = createAnalyticsQueue(config);
                queue.initialize();
            }

            return Promise.resolve();
        },

        /**
         * Track a page view event
         * Receives enriched payload from the enricher plugin and structures it into a pageview event
         */
        page: ({ payload }: { payload: EnrichedAnalyticsPayload }): void => {
            if (!isInitialized) {
                throw new Error('DotCMS Analytics: Plugin not initialized');
            }

            const { context, page, utm, custom, local_time } = payload;

            if (!page) {
                throw new Error('DotCMS Analytics: Missing required page data');
            }

            const pageViewData: DotCMSPageViewEventData = {
                page,
                ...(utm && { utm }),
                ...(custom && { custom })
            };

            const pageViewEvent = createPageViewEvent(pageViewData, local_time);

            const requestBody: DotCMSAnalyticsRequestBody = {
                context,
                events: [pageViewEvent]
            };

            sendEvent(requestBody);
        },

        /**
         * Track a custom or predefined event
         * Receives enriched payload from the enricher plugin and structures it based on event type:
         *
         * - Predefined events (content_impression, etc.) → structured data as-is
         * - Custom events (any other string) → properties are already the custom data to send
         */
        track: ({ payload }: { payload: EnrichedTrackPayload }): void => {
            if (!isInitialized) {
                throw new Error('DotCMS Analytics: Plugin not initialized');
            }

            const { event, properties, context, local_time } = payload;

            let analyticsEvent;

            // Use type guard to distinguish predefined events from custom events
            if (isPredefinedEventType(event)) {
                // Handle predefined events (currently only content_impression)
                if (event === DotCMSPredefinedEventType.CONTENT_IMPRESSION) {
                    // Content impression events come with page data already added by enricher
                    const completeImpressionData = properties as DotCMSContentImpressionEventData;

                    analyticsEvent = createContentImpressionEvent(
                        completeImpressionData,
                        local_time
                    );
                } else {
                    // Fallback for other predefined events we might add in the future
                    // For future predefined events, wrap in custom as fallback
                    analyticsEvent = createCustomEvent(event, properties as JsonObject, local_time);
                }
            } else {
                // Custom events - properties are the custom data (already prepared by user)
                // Wrap them in 'custom' wrapper as required by DotCMS Analytics API
                analyticsEvent = createCustomEvent(event, properties as JsonObject, local_time);
            }

            const requestBody: DotCMSAnalyticsRequestBody = {
                context,
                events: [analyticsEvent]
            };

            sendEvent(requestBody);
        },

        /**
         * Check if the plugin is loaded
         */
        loaded: () => isInitialized
    };
};
