import { ANALYTICS_ENDPOINT } from './constants';
import { DotCMSAnalyticsConfig, DotCMSEvent, DotCMSRequestBody } from './models';

/**
 * Send an analytics event to the server using fetch
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

        const endpoint = `${config.server}${ANALYTICS_ENDPOINT}`;

        const response = await fetch(endpoint, {
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

/**
 * Send analytics events using sendBeacon for reliable page unload delivery
 * sendBeacon is fire-and-forget and not cancelled when the page unloads
 * @param payload - The event payload data
 * @param config - The analytics configuration
 */
export const sendAnalyticsEventWithBeacon = (
    payload: DotCMSRequestBody<DotCMSEvent>,
    config: DotCMSAnalyticsConfig
): void => {
    if (config.debug) {
        console.warn(
            `DotCMS Analytics: Sending ${payload.events.length} events with sendBeacon (page unload)`
        );
    }

    const endpoint = `${config.server}${ANALYTICS_ENDPOINT}`;
    const body = JSON.stringify(payload);

    if (navigator.sendBeacon) {
        // Create Blob with correct Content-Type for JSON
        const blob = new Blob([body], { type: 'application/json' });
        const sent = navigator.sendBeacon(endpoint, blob);

        if (!sent && config.debug) {
            console.warn('DotCMS Analytics: sendBeacon failed (queue might be full)');
        }
    } else {
        // Fallback: attempt synchronous send (may not complete)
        if (config.debug) {
            console.warn('DotCMS Analytics: sendBeacon not available, using fetch fallback');
        }
        sendAnalyticsEventToServer(payload, config);
    }
};
