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
            const { config, payload } = params;
            if (config.debug) {
                console.warn('DotAnalytics: Initialized with config', config);
            }

            isInitialized = true;

            // If autoPageView is enabled, send a page view event, used in IIFE
            if (config.autoPageView) {
                const body = {
                    ...payload.properties,
                    key: config.apiKey
                };

                return sendAnalyticsEventToServer(body, config);
            }

            return Promise.resolve();
        },

        /**
         * Track a page view event
         */
        page: (params: DotAnalyticsParams) => {
            const { config, payload } = params;

            if (!isInitialized) {
                throw new Error('DotAnalytics: Plugin not initialized');
            }

            const body = {
                ...payload.properties,
                key: config.apiKey
            };

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

            const body = {
                ...payload.properties,
                key: config.apiKey
            };

            return sendAnalyticsEventToServer(body, config);
        },

        /**
         * Check if the plugin is loaded
         */
        loaded: () => isInitialized
    };
};
