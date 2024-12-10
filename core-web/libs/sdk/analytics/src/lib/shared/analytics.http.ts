import { ANALYTICS_ENDPOINT } from './analytics.constants';
import { DotAnalyticsConfig, PageViewEvent } from './analytics.model';

/**
 * Send an analytics event to the server
 * @param data - The event data
 * @param options - The options for the event
 * @returns A promise that resolves to the response from the server
 */
export const sendAnalyticsEventToServer = async (
    data: PageViewEvent,
    options: DotAnalyticsConfig
): Promise<Response> => {
    const eventData = {
        ...data,
        timestamp: new Date().toISOString()
    };

    if (options.debug) {
        console.warn('DotAnalytics: Event sent:', eventData);
    }

    try {
        return await fetch(`${options.server}${ANALYTICS_ENDPOINT}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(eventData)
        });
    } catch (error) {
        console.error('DotAnalytics: Error sending event:', error);
        throw error;
    }
};
