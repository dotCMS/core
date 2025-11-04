import { DotCMSPredefinedEventType } from '../../shared/constants/dot-content-analytics.constants';
import { enrichPagePayloadOptimized, getLocalTime } from '../../shared/dot-content-analytics.utils';
import {
    AnalyticsBasePayloadWithContext,
    AnalyticsTrackPayloadWithContext,
    DotCMSContentImpressionPageData,
    DotCMSContentImpressionPayload,
    DotCMSEventPageData,
    DotCMSEventUtmData,
    EnrichedAnalyticsPayload
} from '../../shared/models';

/**
 * Enriched payload interface for track events.
 * Fields are added to the root based on event type to avoid duplication.
 */
export interface EnrichedTrackPayload extends AnalyticsTrackPayloadWithContext {
    local_time: string;
    // Optional fields added based on event type
    page?: DotCMSContentImpressionPageData | DotCMSEventPageData;
    content?: DotCMSContentImpressionPayload['content'];
    position?: DotCMSContentImpressionPayload['position'];
    utm?: DotCMSEventUtmData;
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
         * Adds data to the root of the payload based on event type to avoid duplication.
         *
         * For content_impression events:
         * - Extracts content and position from properties to root
         * - Adds minimal page data (title, url) to root
         *
         * For custom events:
         * - Only adds local_time (properties are passed as-is)
         *
         * @returns {EnrichedTrackPayload} Enriched payload ready for event creation
         */
        'track:dot-analytics': ({
            payload
        }: {
            payload: AnalyticsTrackPayloadWithContext;
        }): EnrichedTrackPayload => {
            const { event, properties } = payload;
            const local_time = getLocalTime();

            // For content_impression events, extract fields to root level
            if (event === DotCMSPredefinedEventType.CONTENT_IMPRESSION) {
                const impressionPayload = properties as DotCMSContentImpressionPayload;

                return {
                    ...payload,
                    // Extract content and position to root (no duplication)
                    content: impressionPayload.content,
                    position: impressionPayload.position,
                    // Add minimal page data to root
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
