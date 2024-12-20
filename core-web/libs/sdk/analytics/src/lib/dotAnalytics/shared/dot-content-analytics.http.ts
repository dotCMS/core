import { ANALYTICS_ENDPOINT } from './dot-content-analytics.constants';
import { DotContentAnalyticsConfig, ServerEvent } from './dot-content-analytics.model';

/**
 * Send an analytics event to the server
 * @param data - The event data
 * @param options - The options for the event
 * @returns A promise that resolves to the response from the server
 */
export const sendAnalyticsEventToServer = async (
    data: Record<string, unknown>,
    options: DotContentAnalyticsConfig
): Promise<void> => {
    const serverEvent: ServerEvent = {
        ...data,
        timestamp: new Date().toISOString(),
        key: options.apiKey
    };

    if (options.debug) {
        console.warn('DotAnalytics: Event sent:', serverEvent);
    }

    try {
        const response = await fetch(`${options.server}${ANALYTICS_ENDPOINT}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(serverEvent)
        });

        if (!response.ok) {
            console.error(`DotAnalytics: Server responded with status ${response.status}`);
        }
    } catch (error) {
        console.error('DotAnalytics: Error sending event:', error);
    }
};
