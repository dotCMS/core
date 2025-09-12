import { EVENT_TYPES } from '../shared/dot-content-analytics.constants';
import { sendAnalyticsEventToServer } from '../shared/dot-content-analytics.http';
import {
    DotCMSAnalyticsConfig,
    DotCMSAnalyticsParams,
    DotCMSEnrichedPayload,
    DotCMSPageViewRequestBody,
    DotCMSTrackEvent,
    DotCMSTrackRequestBody
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
         * Takes enriched data from properties and creates final structured event
         */
        page: (params: DotCMSAnalyticsParams) => {
            const { config, payload } = params;
            const { context, page, device, utm, local_time } = payload;

            if (!isInitialized) {
                throw new Error('DotAnalytics: Plugin not initialized');
            }

            if (!context || !page || !device || !local_time) {
                throw new Error('DotAnalytics: Missing required payload data for pageview event');
            }

            // Build final structured event with data property
            const body: DotCMSPageViewRequestBody = {
                context,
                events: [
                    {
                        event_type: EVENT_TYPES.PAGEVIEW,
                        local_time,
                        data: {
                            page,
                            device,
                            ...(utm && { utm: utm })
                        }
                    }
                ]
            };

            if (config.debug) {
                console.warn('DotAnalytics: Pageview event to send:', body);
            }

            return sendAnalyticsEventToServer(body, config);
        },

        // TODO: Fix this when we haver the final design for the track event
        /**
         * Track a custom event
         * Takes enriched data and sends it to the analytics server
         */
        track: (params: DotCMSAnalyticsParams) => {
            const { config, payload } = params;

            if (!isInitialized) {
                throw new Error('DotAnalytics: Plugin not initialized');
            }

            // Check if payload has events array (from enricher plugin)
            if ('events' in payload && Array.isArray((payload as DotCMSEnrichedPayload).events)) {
                // Use the enriched payload structure directly
                const enrichedPayload = payload as DotCMSEnrichedPayload;
                const body: DotCMSTrackRequestBody = {
                    context: enrichedPayload.context,
                    events: enrichedPayload.events as DotCMSTrackEvent[]
                };

                if (config.debug) {
                    console.warn('DotAnalytics: Track event to send:', body);
                }

                return sendAnalyticsEventToServer(body, config);
            }

            // Fallback for legacy payload structure (should not happen with enricher plugin)
            if (!payload.context || !payload.local_time) {
                throw new Error('DotAnalytics: Missing required payload data for track event');
            }

            const body: DotCMSTrackRequestBody = {
                context: payload.context,
                events: [
                    {
                        event_type: EVENT_TYPES.TRACK,
                        local_time: payload.local_time,
                        data: {
                            event: payload.event,
                            ...payload.properties
                        }
                    }
                ]
            };

            if (config.debug) {
                console.warn('DotAnalytics: Track event to send (fallback):', body);
            }

            return sendAnalyticsEventToServer(body, config);
        },

        /**
         * Check if the plugin is loaded
         */
        loaded: () => isInitialized
    };
};
