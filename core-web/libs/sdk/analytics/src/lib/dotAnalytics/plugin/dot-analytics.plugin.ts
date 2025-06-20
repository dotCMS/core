import { sendAnalyticsEventToServer } from '../shared/dot-content-analytics.http';
import {
    DotAnalyticsParams,
    DotContentAnalyticsConfig
} from '../shared/dot-content-analytics.model';

/**
 * Analytics plugin for tracking page views and custom events in DotCMS applications.
 * This plugin handles sending analytics data to the DotCMS server, managing initialization,
 * and processing both automatic and manual tracking events.
 *
 * @param {DotAnalyticsConfig} config - Configuration object containing API key, server URL,
 *                                     debug mode and auto page view settings
 * @returns {Object} Plugin object with methods for initialization and event tracking
 */
export const dotAnalytics = (config: DotContentAnalyticsConfig) => {
    let isInitialized = false;

    return {
        name: 'dot-analytics',
        config,

        /**
         * Initialize the plugin
         */
        initialize: (params: DotAnalyticsParams) => {
            const { config } = params;
            if (config.debug) {
                console.warn('DotAnalytics: Initialized with config', config);
            }

            isInitialized = true;

            // No automatic page view sending - let useRouterTracker handle it
            // This ensures all page views go through the enrichment process
            return Promise.resolve();
        },

        /**
         * Track a page view event
         * Takes enriched data from properties and creates final structured event
         */
        page: (params: DotAnalyticsParams) => {
            const { config, payload } = params;
            const { context, page, device, utm } = payload;

            if (!isInitialized) {
                throw new Error('DotAnalytics: Plugin not initialized');
            }

            // Build final structured event
            const body = {
                context,
                events: [
                    {
                        event_type: 'pageview',
                        page,
                        device,
                        ...(utm && { utm: utm })
                    }
                ]
            };

            if (config.debug) {
                console.warn('Event to send:', body);
            }

            return sendAnalyticsEventToServer(body, config);
        },

        /**
         * Track a custom event
         */
        track: (params: DotAnalyticsParams) => {
            const { config, payload } = params;

            if (!isInitialized) {
                throw new Error('DotAnalytics: Plugin not initialized');
            }

            // For track events, the enricher plugin should handle enrichment too
            const body = {
                ...payload,
                key: config.siteKey
            };

            return sendAnalyticsEventToServer(body, config);
        },

        /**
         * Check if the plugin is loaded
         */
        loaded: () => isInitialized
    };
};
