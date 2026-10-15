import { sendEvents } from './http';
import { createEventQueue } from './queue';

import { DotCMSPredefinedEventType, SENDER_PLUGIN_NAME } from '../constants';

import type {
    PipelineConfig,
    EventRequestBody,
    DotCMSContentClickEvent,
    DotCMSContentClickPayload,
    DotCMSContentImpressionEvent,
    DotCMSContentImpressionPayload,
    DotCMSConversionEvent,
    DotCMSConversionPayload,
    EnrichedAnalyticsPayload,
    EnrichedTrackPayload
} from '../models';

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
 * @param {PipelineConfig} config - Configuration object containing API key, server URL,
 *                                     debug mode, auto page view settings, and queue config
 * @returns {Object} Plugin object with methods for initialization and event tracking
 */
export const senderPlugin = (config: PipelineConfig) => {
    let isInitialized = false;
    // Queue is enabled if queue is not explicitly false
    const enableQueue = config.queue !== false;
    let queue: ReturnType<typeof createEventQueue> | null = null;

    /**
     * Dispatches event to queue or directly to server
     */
    const dispatchEvent = (requestBody: EventRequestBody): void => {
        const event = requestBody.events[0];
        const context = requestBody.context;

        // Use queue or send directly
        if (enableQueue && queue) {
            if (event) {
                queue.enqueue(event, context);
            }
        } else {
            // Direct send without queue (when queue === false)
            sendEvents(requestBody, config);
        }
    };

    return {
        name: SENDER_PLUGIN_NAME,
        config,

        /**
         * Initialize the plugin with optional queue management
         */
        initialize: () => {
            isInitialized = true;

            // Initialize queue if enabled (queue is undefined or an object)
            if (enableQueue) {
                queue = createEventQueue(config);
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
                throw new Error('[dotCMS events] Plugin not initialized');
            }

            const { context, page, utm, custom, local_time } = payload;

            if (!page) {
                throw new Error('[dotCMS events] Missing required page data');
            }

            const requestBody: EventRequestBody = {
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
         * Builds a content impression, content click or conversion from the enriched payload.
         *
         * - content_impression → extracts from properties, combines with enriched page data
         * - content_click → extracts from properties, combines with enriched page data
         * - conversion → its name and the page
         *
         * Those, with pageviews, are the only event types dotCMS accepts.
         */
        track: ({ payload }: { payload: EnrichedTrackPayload }): void => {
            if (!isInitialized) {
                throw new Error('[dotCMS events] Plugin not initialized');
            }

            const { event, properties, context, local_time } = payload;

            let analyticsEvent:
                | DotCMSContentImpressionEvent
                | DotCMSContentClickEvent
                | DotCMSConversionEvent;

            // One case per event type dotCMS accepts
            switch (event) {
                case DotCMSPredefinedEventType.CONTENT_IMPRESSION: {
                    // Extract impression data from properties (sent by tracker)
                    const impressionPayload = properties as DotCMSContentImpressionPayload;
                    const { content, position } = impressionPayload;
                    const { page } = payload; // Added by enricher

                    if (!content || !position || !page) {
                        throw new Error('[dotCMS events] Missing required impression data');
                    }

                    analyticsEvent = {
                        event_type: DotCMSPredefinedEventType.CONTENT_IMPRESSION,
                        local_time,
                        data: {
                            content,
                            position,
                            page
                        }
                    } satisfies DotCMSContentImpressionEvent;
                    break;
                }

                case DotCMSPredefinedEventType.CONTENT_CLICK: {
                    // Extract click data from properties (sent by click plugin)
                    const clickPayload = properties as DotCMSContentClickPayload;
                    const { content, position, element } = clickPayload;
                    const { page } = payload; // Added by enricher

                    if (!content || !position || !element || !page) {
                        throw new Error('[dotCMS events] Missing required click data');
                    }

                    analyticsEvent = {
                        event_type: DotCMSPredefinedEventType.CONTENT_CLICK,
                        local_time,
                        data: {
                            content,
                            position,
                            element,
                            page
                        }
                    } satisfies DotCMSContentClickEvent;
                    break;
                }

                case DotCMSPredefinedEventType.CONVERSION: {
                    // Extract conversion data from properties (sent by user)
                    const { name } = properties as DotCMSConversionPayload;
                    const { page } = payload; // Added by enricher

                    if (!name || !page) {
                        throw new Error('[dotCMS events] Missing required conversion data');
                    }

                    analyticsEvent = {
                        event_type: DotCMSPredefinedEventType.CONVERSION,
                        local_time,
                        data: {
                            conversion: { name },
                            page
                        }
                    } satisfies DotCMSConversionEvent;
                    break;
                }

                default:
                    // Only the SDK calls track, with the types above
                    throw new Error(`[dotCMS events] ${event} is not an event type dotCMS accepts`);
            }

            const requestBody: EventRequestBody = {
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
