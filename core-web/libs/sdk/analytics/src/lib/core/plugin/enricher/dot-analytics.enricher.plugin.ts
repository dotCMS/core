import { DotCMSPredefinedEventType } from '../../shared/constants/dot-content-analytics.constants';
import { enrichPagePayloadOptimized, getLocalTime } from '../../shared/dot-content-analytics.utils';
import {
    AnalyticsBasePayloadWithContext,
    AnalyticsTrackPayloadWithContext,
    DotCMSAnalyticsRequestBody
} from '../../shared/models';

/**
 * Plugin that enriches the analytics payload data based on the event type.
 * Uses Analytics.js lifecycle events to inject context before processing.
 * The identity plugin runs FIRST to inject context: { session_id, site_auth, user_id }
 * This enricher plugin runs SECOND to add page/device/utm data.
 *
 * Returns the final request body structure ready to send to the server.
 */
export const dotAnalyticsEnricherPlugin = () => {
    return {
        name: 'enrich-dot-analytics',

        /**
         * PAGE VIEW ENRICHMENT - Runs after identity context injection
         * Returns the complete request body for pageview events
         * @returns {DotCMSAnalyticsRequestBody} Complete request body ready to send
         */
        'page:dot-analytics': ({
            payload
        }: {
            payload: AnalyticsBasePayloadWithContext;
        }): DotCMSAnalyticsRequestBody => {
            const { context, page, device, utm, custom, local_time } =
                enrichPagePayloadOptimized(payload);

            if (!page || !device) {
                throw new Error('DotCMS Analytics: Missing required page or device data');
            }

            return {
                context,
                events: [
                    {
                        event_type: DotCMSPredefinedEventType.PAGEVIEW,
                        local_time,
                        data: {
                            page,
                            device,
                            ...(utm && { utm }),
                            ...(custom && { custom })
                        }
                    }
                ]
            };
        },

        /**
         * TRACK EVENT ENRICHMENT - Runs after identity context injection
         * Returns the complete request body for custom events
         * @returns {DotCMSAnalyticsRequestBody} Complete request body ready to send
         */
        'track:dot-analytics': ({
            payload
        }: {
            payload: AnalyticsTrackPayloadWithContext;
        }): DotCMSAnalyticsRequestBody => {
            const { event, properties, context } = payload;
            const local_time = getLocalTime();

            return {
                context,
                events: [
                    {
                        event_type: event,
                        local_time,
                        data: {
                            custom: properties
                        }
                    }
                ]
            };
        }
    };
};
