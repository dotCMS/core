import { ANALYTICS_ENDPOINT } from '../constants';
import { DotCMSAnalyticsConfig, DotCMSAnalyticsRequestBody } from '../models';
import { createPluginLogger } from '../utils/dot-analytics.utils';

/**
 * Send analytics events to the server using fetch API
 * @param payload - The event payload data
 * @param config - The analytics configuration
 * @param keepalive - Use keepalive mode for page unload scenarios (default: false)
 * @returns A promise that resolves when the request is complete
 */
export const sendAnalyticsEvent = async (
    payload: DotCMSAnalyticsRequestBody,
    config: DotCMSAnalyticsConfig,
    keepalive = false
): Promise<void> => {
    const logger = createPluginLogger('HTTP', config);
    const endpoint = `${config.server}${ANALYTICS_ENDPOINT}`;
    const body = JSON.stringify(payload);

    logger.info(`Sending ${payload.events.length} event(s)${keepalive ? ' (keepalive)' : ''}`, {
        payload
    });

    try {
        const fetchOptions: RequestInit = {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body
        };

        // Only add keepalive-specific options when keepalive is true
        if (keepalive) {
            fetchOptions.keepalive = true;
            fetchOptions.credentials = 'omit'; // Required for keepalive requests

            // Fire and forget - don't await response with keepalive
            // The browser will send the request even if the page unloads
            fetch(endpoint, fetchOptions);
            return;
        }

        // Normal request - await and check response
        const response = await fetch(endpoint, fetchOptions);

        if (!response.ok) {
            // Always log the HTTP status code
            const statusText = response.statusText || 'Unknown Error';
            const baseErrorMessage = `HTTP ${response.status}: ${statusText}`;

            try {
                const errorData = await response.json();
                if (errorData.message) {
                    logger.warn(`${errorData.message} (${baseErrorMessage})`);
                } else {
                    // JSON parsed successfully but no message property
                    logger.warn(`${baseErrorMessage} - No error message in response`);
                }
            } catch (parseError) {
                // JSON parsing failed, log the HTTP status with parse error
                logger.warn(`${baseErrorMessage} - Failed to parse error response:`, parseError);
            }
        }
    } catch (error) {
        logger.error('Error sending event:', error);
    }
};
