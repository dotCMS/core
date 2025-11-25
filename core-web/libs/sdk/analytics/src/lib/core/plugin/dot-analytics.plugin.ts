import { DotCMSPredefinedEventType } from '../shared/constants/dot-content-analytics.constants';
import { sendAnalyticsEvent } from '../shared/dot-content-analytics.http';
import {
    DotCMSAnalyticsConfig,
    DotCMSAnalyticsRequestBody,
    DotCMSContentImpressionPayload,
    EnrichedAnalyticsPayload,
    EnrichedTrackPayload,
    JsonObject
} from '../shared/models';
import { createAnalyticsQueue } from '../shared/queue';

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
     * Dispatches event to queue or directly to server
     */
    const dispatchEvent = (requestBody: DotCMSAnalyticsRequestBody): void => {
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

            const requestBody: DotCMSAnalyticsRequestBody = {
                context,
                events: [
                    {
                        event_type: DotCMSPredefinedEventType.PAGEVIEW,
                        local_time,
                        data: {
                            page,
                            ...(utm && { utm }),
                            ...(custom && { custom })
                        }
                    }
                ]
            };

            dispatchEvent(requestBody);
        },

        /**
         * Track a custom or predefined event
         * Receives enriched payload from enricher plugin and structures it into proper event format.
         *
         * - content_impression → extracts from properties, combines with enriched page data
         * - custom events → wraps properties in custom object
         */
        track: ({ payload }: { payload: EnrichedTrackPayload }): void => {
            if (!isInitialized) {
                throw new Error('DotCMS Analytics: Plugin not initialized');
            }

            const { event, properties, context, local_time } = payload;

            let analyticsEvent;

            // Handle predefined and custom events using switch for extensibility
            switch (event) {
                case DotCMSPredefinedEventType.CONTENT_IMPRESSION: {
                    // Extract impression data from properties (sent by tracker)
                    const impressionPayload = properties as DotCMSContentImpressionPayload;
                    const { content, position } = impressionPayload;
                    const { page } = payload; // Added by enricher

                    if (!content || !position || !page) {
                        throw new Error('DotCMS Analytics: Missing required impression data');
                    }

                    analyticsEvent = {
                        event_type: DotCMSPredefinedEventType.CONTENT_IMPRESSION,
                        local_time,
                        data: {
                            content,
                            position,
                            page
                        }
                    };
                    break;
                }

                default: {
                    // Custom events - wrap properties in custom object as required by API
                    analyticsEvent = {
                        event_type: event,
                        local_time,
                        data: {
                            custom: properties as JsonObject
                        }
                    };
                    break;
                }
            }

            const requestBody: DotCMSAnalyticsRequestBody = {
                context,
                events: [analyticsEvent]
            };

            dispatchEvent(requestBody);
        },

        /**
         * Check if the plugin is loaded
         */
        loaded: () => isInitialized
    };
};
