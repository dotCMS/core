import {
    ANALYTICS_PAGEVIEW_EVENT,
    ANALYTICS_SOURCE_TYPE,
    EventType
} from '../shared/dot-content-analytics.constants';
import {
    DotAnalyticsPayload,
    PageViewEvent,
    TrackEvent
} from '../shared/dot-content-analytics.model';
import { getBrowserEventData } from '../shared/dot-content-analytics.utils';

/**
 * Plugin that enriches the analytics payload data based on the event type.
 * For page view events, it adds browser data like viewport size, user agent, etc.
 * For track events, it adds custom event properties and standardizes the event format.
 * All events are enriched with source type and anonymous ID if available.
 */
export const dotAnalyticsEnricherPlugin = {
    name: 'enrich-dot-analytics',

    'page:dot-analytics': ({ payload }: { payload: DotAnalyticsPayload }) => {
        const enrichedProperties: PageViewEvent = {
            ...getBrowserEventData(window.location),
            ...payload.properties,
            event_type: ANALYTICS_PAGEVIEW_EVENT,
            anonymousId: payload.anonymousId || undefined,
            src: ANALYTICS_SOURCE_TYPE
        };

        return { ...payload, properties: enrichedProperties };
    },

    'track:dot-analytics': ({ payload }: { payload: DotAnalyticsPayload }) => {
        const enrichedProperties: TrackEvent = {
            ...payload.properties,
            custom_event: payload.event,
            event_type: EventType.Track,
            anonymousId: payload.anonymousId || undefined,
            src: ANALYTICS_SOURCE_TYPE
        };

        return { ...payload, properties: enrichedProperties };
    }
};
