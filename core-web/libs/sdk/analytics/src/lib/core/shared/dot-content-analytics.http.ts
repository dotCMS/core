import { ANALYTICS_ENDPOINT } from './constants';
import { DotCMSAnalyticsConfig, DotCMSEvent, DotCMSRequestBody } from './models';

/**
 * Send an analytics event to the server
 * @param payload - The event payload data
 * @param config - The analytics configuration
 * @returns A promise that resolves when the request is complete
 */
export const sendAnalyticsEventToServer = async (
    payload: DotCMSRequestBody<DotCMSEvent>,
    config: DotCMSAnalyticsConfig
): Promise<void> => {
    try {
        if (config.debug) {
            console.warn('DotCMS Analytics: HTTP Body to send:', JSON.stringify(payload, null, 2));
        }

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
                    console.warn(`DotCMS Analytics: ${errorData.message} (${baseErrorMessage})`);
                } else {
                    // JSON parsed successfully but no message property
                    console.warn(
                        `DotCMS Analytics: ${baseErrorMessage} - No error message in response`
                    );
                }
            } catch (parseError) {
                // JSON parsing failed, log the HTTP status with parse error
                console.warn(
                    `DotCMS Analytics: ${baseErrorMessage} - Failed to parse error response:`,
                    parseError
                );
            }
        }
    } catch (error) {
        console.error('DotCMS Analytics: Error sending event:', error);
    }
};
