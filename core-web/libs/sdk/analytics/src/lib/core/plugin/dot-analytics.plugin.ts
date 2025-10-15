import { sendAnalyticsEvent } from '../shared/dot-content-analytics.http';
import { DotCMSAnalyticsConfig, DotCMSAnalyticsParams } from '../shared/models';
import { createAnalyticsQueue } from '../shared/queue';

/**
 * Analytics plugin for tracking page views and custom events in DotCMS applications.
 * This plugin handles sending analytics data to the DotCMS server, managing initialization,
 * and processing both automatic and manual tracking events.
 * Supports optional queue management for batching events before sending.
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
     * Common handler for both page views and custom events
     * Processes events by either queuing them or sending directly
     */
    const handleEvent = (params: DotCMSAnalyticsParams): void => {
        const { config, payload } = params;

        if (!isInitialized) {
            throw new Error('DotCMS Analytics: Plugin not initialized');
        }

        const event = payload.events[0];
        const context = payload.context;

        // Use queue or send directly
        if (enableQueue && queue) {
            queue.enqueue(event, context);
        } else {
            // Direct send without queue (when queue === false)
            const body = {
                context,
                events: [event]
            };
            sendAnalyticsEvent(body, config);
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
         * The enricher plugin has already built the complete request body
         */
        page: handleEvent,

        /**
         * Track a custom event
         * The enricher plugin has already built the complete request body
         */
        track: handleEvent,

        /**
         * Check if the plugin is loaded
         */
        loaded: () => isInitialized
    };
};
