import { ANALYTICS_ENDPOINT } from './dot-content-analytics.constants';
import {
    DotCMSAnalyticsConfig,
    DotCMSPageViewRequestBody,
    DotCMSTrackRequestBody
} from './dot-content-analytics.model';

/**
 * Send an analytics event to the server
 * @param data - The event data
 * @param options - The options for the event
 * @returns A promise that resolves to the response from the server
 */
export const sendAnalyticsEventToServer = async (
    payload: DotCMSPageViewRequestBody | DotCMSTrackRequestBody,
    options: DotCMSAnalyticsConfig
): Promise<void> => {
    try {
        const response = await fetch(`${options.server}${ANALYTICS_ENDPOINT}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            try {
                const errorData = await response.json();
                if (errorData.message) {
                    console.warn(`DotAnalytics: ${errorData.message}`);

                    return;
                }
            } catch (parseError) {
                console.error('DotAnalytics: Error parsing error response:', parseError);
            }
        }
    } catch (error) {
        console.error('DotAnalytics: Error sending event:', error);
    }
};
