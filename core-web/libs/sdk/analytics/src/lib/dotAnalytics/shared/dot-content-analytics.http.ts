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
            // Always log the HTTP status code
            const statusText = response.statusText || 'Unknown Error';
            const baseErrorMessage = `HTTP ${response.status}: ${statusText}`;

            try {
                const errorData = await response.json();
                if (errorData.message) {
                    console.warn(`DotAnalytics: ${errorData.message} (${baseErrorMessage})`);
                } else {
                    // JSON parsed successfully but no message property
                    console.warn(
                        `DotAnalytics: ${baseErrorMessage} - No error message in response`
                    );
                }
            } catch (parseError) {
                // JSON parsing failed, log the HTTP status with parse error
                console.warn(
                    `DotAnalytics: ${baseErrorMessage} - Failed to parse error response:`,
                    parseError
                );
            }
        }
    } catch (error) {
        console.error('DotAnalytics: Error sending event:', error);
    }
};
