import { ANALYTICS_ENDPOINT } from './dot-content-analytics.constants';
import { DotContentAnalyticsConfig } from './dot-content-analytics.model';

/**
 * Send an analytics event to the server
 * @param data - The event data
 * @param options - The options for the event
 * @returns A promise that resolves to the response from the server
 */
export const sendAnalyticsEventToServer = async (
    payload: Record<string, unknown>,
    options: DotContentAnalyticsConfig
): Promise<void> => {
    try {
        const response = await fetch(`${options.server}${ANALYTICS_ENDPOINT}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            console.error(`DotAnalytics: Server responded with status ${response.status}`);
        }
    } catch (error) {
        console.error('DotAnalytics: Error sending event:', error);
    }
};
