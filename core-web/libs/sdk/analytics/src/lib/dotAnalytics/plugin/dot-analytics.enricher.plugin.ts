import { ANALYTICS_SOURCE_TYPE } from '../shared/dot-content-analytics.constants';
import {
    AnalyticsHookParams,
    DotAnalyticsPayload,
    DotContentAnalyticsConfig
} from '../shared/dot-content-analytics.model';
import {
    getAnalyticsContext,
    getBrowserEventData,
    getDeviceData,
    getPageData,
    getUtmData
} from '../shared/dot-content-analytics.utils';

/**
 * Plugin that enriches the analytics payload data based on the event type.
 * Uses Analytics.js lifecycle events to inject global context before processing.
 * All events get consistent context: { session_id, site_key, user_Id }
 */
export const dotAnalyticsEnricherPlugin = (config: DotContentAnalyticsConfig) => {
    return {
        name: 'enrich-dot-analytics',

        /**
         * 🌐 UNIVERSAL CONTEXT INJECTION - Runs before ANY page event
         * This is the perfect place to add global context to ALL page events
         */
        pageStart: ({ payload }: AnalyticsHookParams) => {
            const context = getAnalyticsContext(config);

            return {
                ...payload,
                context
            };
        },

        /**
         * 🌐 UNIVERSAL CONTEXT INJECTION - Runs before ANY track event
         * This ensures track events also get the global context
         */
        trackStart: ({ payload }: AnalyticsHookParams) => {
            // Inject global context into payload before track processing
            const context = getAnalyticsContext(config);

            return {
                ...payload,
                context
            };
        },

        /**
         * 📄 PAGE VIEW ENRICHMENT - Runs after context injection
         * Parses browser data and puts it in properties for the main plugin to use
         */
        'page:dot-analytics': ({ payload }: { payload: DotAnalyticsPayload }) => {
            // Get browser data once
            const browserData = getBrowserEventData(window.location);

            // Parse separate data objects using focused utilities
            const pageData = getPageData(browserData, payload);
            const deviceData = getDeviceData(browserData);
            const utmData = getUtmData(browserData);

            return {
                ...payload,
                page: pageData,
                device: deviceData,
                ...(Object.keys(utmData).length > 0 && { utm: utmData })
            };
        },

        /**
         * 🎯 TRACK EVENT ENRICHMENT - Runs after context injection
         * Creates structured track events with pre-injected context
         */
        'track:dot-analytics': ({ payload }: { payload: DotAnalyticsPayload }) => {
            const enrichedPayload = {
                events: [
                    {
                        event_type: 'track',
                        custom_event: payload.event,
                        properties: {
                            ...payload.properties,

                            src: ANALYTICS_SOURCE_TYPE
                        }
                    }
                ]
            };

            return { payload: enrichedPayload };
        }
    };
};
