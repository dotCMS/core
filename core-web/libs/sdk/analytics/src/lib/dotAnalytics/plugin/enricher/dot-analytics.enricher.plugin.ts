import { ANALYTICS_SOURCE_TYPE } from '../../shared/dot-content-analytics.constants';
import { DotAnalyticsPayload } from '../../shared/dot-content-analytics.model';
import { enrichPagePayloadOptimized, getLocalTime } from '../../shared/dot-content-analytics.utils';

/**
 * Plugin that enriches the analytics payload data based on the event type.
 * Uses Analytics.js lifecycle events to inject context before processing.
 * The identity plugin runs FIRST to inject context: { session_id, site_key, user_id }
 * This enricher plugin runs SECOND to add page/device/utm data.
 *
 * OPTIMIZED: Uses existing payload.properties data to avoid duplication
 */
export const dotAnalyticsEnricherPlugin = () => {
    return {
        name: 'enrich-dot-analytics',

        /**
         * PAGE VIEW ENRICHMENT - Runs after identity context injection
         * Uses optimized enrichment that leverages analytics.js payload data
         */
        'page:dot-analytics': ({ payload }: { payload: DotAnalyticsPayload }) => {
            return enrichPagePayloadOptimized(payload);
        },

        /**
         * TRACK EVENT ENRICHMENT - Runs after identity context injection
         * Creates structured track events with pre-injected context
         */
        'track:dot-analytics': ({ payload }: { payload: DotAnalyticsPayload }) => {
            const local_time = getLocalTime();

            const enrichedPayload = {
                events: [
                    {
                        event_type: 'track',
                        custom_event: payload.event,
                        local_time: local_time,
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
