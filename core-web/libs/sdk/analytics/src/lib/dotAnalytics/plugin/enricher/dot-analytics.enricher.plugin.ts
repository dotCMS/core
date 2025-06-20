import {
    getBrowserEventData,
    getDeviceData,
    getPageData,
    getUtmData
} from './dot-analytics.enricher.utils';

import { ANALYTICS_SOURCE_TYPE } from '../../shared/dot-content-analytics.constants';
import { DotAnalyticsPayload } from '../../shared/dot-content-analytics.model';

/**
 * Plugin that enriches the analytics payload data based on the event type.
 * Uses Analytics.js lifecycle events to inject context before processing.
 * The identity plugin runs FIRST to inject context: { session_id, site_key, user_id }
 * This enricher plugin runs SECOND to add page/device/utm data.
 */
export const dotAnalyticsEnricherPlugin = () => {
    return {
        name: 'enrich-dot-analytics',

        /**
         * 📄 PAGE VIEW ENRICHMENT - Runs after identity context injection
         * Parses browser data and puts it in properties for the main plugin to use
         */
        'page:dot-analytics': ({ payload }: { payload: DotAnalyticsPayload }) => {
            // Get browser data once
            const browserData = getBrowserEventData(window.location);

            // Parse separate data objects using focused utilities
            const pageData = getPageData(browserData);
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
         * 🎯 TRACK EVENT ENRICHMENT - Runs after identity context injection
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
