import { ANALYTICS_ENDPOINT } from './dot-content-analytics.constants';
import { DotCMSAnalyticsConfig, DotCMSAnalyticsRequestBody } from './dot-content-analytics.model';

/**
 * Send an analytics event to the server
 * @param payload - The event payload data
 * @param config - The analytics configuration
 * @returns A promise that resolves when the request is complete
 */
export const sendAnalyticsEventToServer = async (
    payload: DotCMSAnalyticsRequestBody,
    config: DotCMSAnalyticsConfig
): Promise<void> => {
    try {
        const response = await fetch(`${config.server}${ANALYTICS_ENDPOINT}`, {
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
