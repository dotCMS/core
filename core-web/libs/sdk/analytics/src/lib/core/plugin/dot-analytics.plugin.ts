import { sendAnalyticsEventToServer } from '../shared/dot-content-analytics.http';
import {
    DotCMSAnalyticsConfig,
    DotCMSAnalyticsEnrichedParams
} from '../shared/dot-content-analytics.model';

/**
 * Analytics plugin for tracking page views and custom events in DotCMS applications.
 * This plugin handles sending analytics data to the DotCMS server, managing initialization,
 * and processing both automatic and manual tracking events.
 *
 * @param {DotCMSAnalyticsConfig} config - Configuration object containing API key, server URL,
 *                                     debug mode and auto page view settings
 * @returns {Object} Plugin object with methods for initialization and event tracking
 */
export const dotAnalytics = (config: DotCMSAnalyticsConfig) => {
    let isInitialized = false;

    return {
        name: 'dot-analytics',
        config,

        /**
         * Initialize the plugin
         */
        initialize: () => {
            isInitialized = true;

            // No automatic page view sending - let useRouterTracker handle it
            // This ensures all page views go through the enrichment process
            return Promise.resolve();
        },

        /**
         * Track a page view event
         * The enricher plugin has already built the complete request body
         */
        page: (params: DotCMSAnalyticsEnrichedParams) => {
            const { config, payload } = params;

            if (!isInitialized) {
                throw new Error('DotAnalytics: Plugin not initialized');
            }

            // Extract only context and events (strip any extra properties from Analytics.js)
            const body = {
                context: payload.context,
                events: payload.events
            };

            return sendAnalyticsEventToServer(body, config);
        },

        /**
         * Track a custom event
         * The enricher plugin has already built the complete request body
         */
        track: (params: DotCMSAnalyticsEnrichedParams) => {
            const { config, payload } = params;

            if (!isInitialized) {
                throw new Error('DotAnalytics: Plugin not initialized');
            }

            // Extract only context and events (strip any extra properties from Analytics.js)
            const body = {
                context: payload.context,
                events: payload.events
            };

            return sendAnalyticsEventToServer(body, config);
        },

        /**
         * Check if the plugin is loaded
         */
        loaded: () => isInitialized
    };
};
