import { ANALYTICS_ENDPOINT } from './constants';
import { DotCMSAnalyticsConfig, DotCMSEvent, DotCMSRequestBody } from './models';

/**
 * Send analytics events to the server using fetch API
 * @param payload - The event payload data
 * @param config - The analytics configuration
 * @param keepalive - Use keepalive mode for page unload scenarios (visibilitychange, pagehide)
 * @returns A promise that resolves when the request is complete
 */
export const sendAnalyticsEvent = async (
    payload: DotCMSRequestBody<DotCMSEvent>,
    config: DotCMSAnalyticsConfig,
    keepalive = false
): Promise<void> => {
    const endpoint = `${config.server}${ANALYTICS_ENDPOINT}`;
    const body = JSON.stringify(payload);

    if (config.debug) {
        console.warn(
            `DotCMS Analytics: Sending ${payload.events.length} event(s)${keepalive ? ' (keepalive)' : ''}`,
            { payload }
        );
    }

    try {
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body,
            keepalive
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
