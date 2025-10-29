import { ANALYTICS_ENDPOINT } from './constants';
import { DotCMSAnalyticsConfig, DotCMSEvent, DotCMSRequestBody } from './models';

/**
 * Available transport methods for sending analytics events
 */
export const TRANSPORT_TYPES = {
    FETCH: 'fetch',
    BEACON: 'beacon'
} as const;

export type TransportType = (typeof TRANSPORT_TYPES)[keyof typeof TRANSPORT_TYPES];

/**
 * Send analytics events to the server
 * @param payload - The event payload data
 * @param config - The analytics configuration
 * @param transportType - Transport method: 'fetch' (default) or 'beacon' (for page unload)
 * @returns A promise that resolves when the request is complete (fetch only)
 */
export const sendAnalyticsEvent = async (
    payload: DotCMSRequestBody<DotCMSEvent>,
    config: DotCMSAnalyticsConfig,
    transportType: TransportType = 'fetch'
): Promise<void> => {
    const endpoint = `${config.server}${ANALYTICS_ENDPOINT}`;
    const body = JSON.stringify(payload);

    if (config.debug) {
        console.warn(
            `DotCMS Analytics: Sending ${payload.events.length} event(s) via ${transportType}`,
            transportType === 'fetch' ? { payload } : undefined
        );
    }

    // Use sendBeacon for page unload scenarios
    if (transportType === 'beacon') {
        if (navigator.sendBeacon) {
            // Create Blob with correct Content-Type for JSON
            const blob = new Blob([body], { type: 'application/json' });
            const sent = navigator.sendBeacon(endpoint, blob);

            if (!sent && config.debug) {
                console.warn('DotCMS Analytics: sendBeacon failed (queue might be full)');
            }
        } else {
            // Fallback to fetch if sendBeacon not available
            if (config.debug) {
                console.warn('DotCMS Analytics: sendBeacon not available, using fetch fallback');
            }
            return sendAnalyticsEvent(payload, config, 'fetch');
        }
        return;
    }

    // Use fetch for normal scenarios
    try {
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body
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
