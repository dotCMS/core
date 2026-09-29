import { DotCMSPredefinedEventType, SENDER_PLUGIN_NAME } from '../constants';
import { enrichPagePayloadOptimized, getLocalTime } from '../utils';

import type {
    AnalyticsBasePayloadWithContext,
    AnalyticsTrackPayloadWithContext,
    EnrichedAnalyticsPayload,
    EnrichedTrackPayload
} from '../models';

/**
 * Plugin that enriches the analytics payload data with page, UTM, and custom data.
 * Uses Analytics.js lifecycle events to inject enriched data before the main plugin processes it.
 *
 * The identity plugin runs FIRST to inject context: { session_id, site_auth, user_id, device }
 * This enricher plugin runs SECOND to add page/utm/custom data.
 * The main plugin runs THIRD to structure events and send to server.
 *
 * This plugin is ONLY responsible for data enrichment - NOT for event structuring or business logic.
 */
export const enricherPlugin = () => {
    return {
        name: 'dot-events-enricher',

        /**
         * PAGE VIEW ENRICHMENT - Runs after identity context injection
         * Returns enriched payload with page, utm, and custom data added
         * @returns {EnrichedAnalyticsPayload} Enriched payload ready for event creation
         */
        [`page:${SENDER_PLUGIN_NAME}`]: ({
            payload
        }: {
            payload: AnalyticsBasePayloadWithContext;
        }): EnrichedAnalyticsPayload => {
            const enrichedData = enrichPagePayloadOptimized(payload);

            if (!enrichedData.page) {
                throw new Error('[dotCMS events] Missing required page data');
            }

            return enrichedData;
        },

        /**
         * TRACK EVENT ENRICHMENT - Runs after identity context injection
         * Adds page data and timestamp for predefined content events.
         * For custom events, only adds timestamp.
         *
         * @returns {EnrichedTrackPayload} Enriched payload ready for event structuring
         */
        [`track:${SENDER_PLUGIN_NAME}`]: ({
            payload
        }: {
            payload: AnalyticsTrackPayloadWithContext;
        }): EnrichedTrackPayload => {
            const { event } = payload;
            const local_time = getLocalTime();

            // For content_impression, content_click, and conversion events, add page data
            if (
                event === DotCMSPredefinedEventType.CONTENT_IMPRESSION ||
                event === DotCMSPredefinedEventType.CONTENT_CLICK ||
                event === DotCMSPredefinedEventType.CONVERSION
            ) {
                return {
                    ...payload,
                    page: {
                        title: document.title,
                        url: window.location.href
                    },
                    local_time
                };
            }

            // For custom events, just add local_time
            return {
                ...payload,
                local_time
            };
        }
    };
};
