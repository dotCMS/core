import { DotCMSPredefinedEventType } from '../../shared/constants/dot-content-analytics.constants';
import { enrichPagePayloadOptimized } from '../../shared/dot-content-analytics.utils';
import {
    AnalyticsBasePayloadWithContext,
    AnalyticsTrackPayloadWithContext,
    DotCMSContentImpressionPayload,
    EnrichedAnalyticsPayload
} from '../../shared/models';

/**
 * Enriched payload interface that includes page, utm, and custom data ready for event creation.
 * This is the return type from the enricher plugin methods.
 */
export interface EnrichedTrackPayload extends AnalyticsTrackPayloadWithContext {
    page: EnrichedAnalyticsPayload['page'];
    utm?: EnrichedAnalyticsPayload['utm'];
    custom?: EnrichedAnalyticsPayload['custom'];
    local_time: string;
}

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
export const dotAnalyticsEnricherPlugin = () => {
    return {
        name: 'enrich-dot-analytics',

        /**
         * PAGE VIEW ENRICHMENT - Runs after identity context injection
         * Returns enriched payload with page, utm, and custom data added
         * @returns {EnrichedAnalyticsPayload} Enriched payload ready for event creation
         */
        'page:dot-analytics': ({
            payload
        }: {
            payload: AnalyticsBasePayloadWithContext;
        }): EnrichedAnalyticsPayload => {
            const enrichedData = enrichPagePayloadOptimized(payload);

            if (!enrichedData.page) {
                throw new Error('DotCMS Analytics: Missing required page data');
            }

            return enrichedData;
        },

        /**
         * TRACK EVENT ENRICHMENT - Runs after identity context injection
         * Returns enriched payload with page data added for content_impression events
         *
         * For content_impression events:
         * - Producer plugins send only { content, position }
         * - This enricher adds page data automatically
         *
         * For other events:
         * - Just pass through the payload as-is
         *
         * @returns {EnrichedTrackPayload} Enriched payload ready for event creation
         */
        'track:dot-analytics': ({
            payload
        }: {
            payload: AnalyticsTrackPayloadWithContext;
        }): EnrichedTrackPayload => {
            const { event, properties } = payload;

            // For content_impression events, add page data
            if (event === DotCMSPredefinedEventType.CONTENT_IMPRESSION) {
                const impressionPayload = properties as DotCMSContentImpressionPayload;
                const { page, local_time } = enrichPagePayloadOptimized(payload);

                return {
                    ...payload,
                    properties: {
                        ...impressionPayload,
                        page
                    },
                    page,
                    local_time
                };
            }

            // For all other events, just pass through
            const { page, utm, custom, local_time } = enrichPagePayloadOptimized(payload);

            return {
                ...payload,
                page,
                ...(utm && { utm }),
                ...(custom && { custom }),
                local_time
            };
        }
    };
};
