import { ANALYTICS_ENDPOINT } from '../constants';
import { createPluginLogger } from '../utils';

import type { PipelineConfig, PipelineError, EventRequestBody } from '../models';

/** What dotCMS answers to an events request. */
interface EventsResponseBody {
    message?: string;
    entity?: { failed?: number; errors?: { message?: string }[] };
}

/**
 * Hands a failure to the configured handler. A handler that throws must not break the page.
 *
 * @param config - The analytics configuration
 * @param error - The failure
 */
const notify = (config: PipelineConfig, error: PipelineError): void => {
    try {
        config.onError?.(error);
    } catch {
        // The app's handler failed; the events pipeline carries on
    }
};

/**
 * Reads the response body, when it is JSON.
 *
 * @param response - The events response
 * @returns The body, or null
 */
const readBody = async (response: Response): Promise<EventsResponseBody | null> => {
    try {
        return (await response.json()) as EventsResponseBody;
    } catch {
        return null;
    }
};

/**
 * Reports what dotCMS answered to an events request: an error status, or an accepted request
 * whose body says some events failed.
 *
 * @param response - The events response
 * @param body - Its body, when JSON
 * @param config - The analytics configuration
 * @param events - How many events the request carried
 */
const reportResponse = (
    response: Response,
    body: EventsResponseBody | null,
    config: PipelineConfig,
    events: number
): void => {
    const errors = body?.entity?.errors;
    const failed = body?.entity?.failed ?? 0;

    if (response.ok && failed === 0) {
        return;
    }

    const reason =
        errors?.[0]?.message ?? body?.message ?? (response.statusText || 'Unknown Error');

    notify(config, {
        code: 'REJECTED',
        message: response.ok
            ? `${failed} of ${events} event(s) failed: ${reason}`
            : `HTTP ${response.status}: ${reason}`,
        status: response.status,
        detail: errors ?? body,
        events
    });
};

/**
 * Send analytics events to the server using fetch API
 * @param payload - The event payload data
 * @param config - The analytics configuration
 * @param keepalive - Use keepalive mode for page unload scenarios (default: false)
 * @returns A promise that resolves when the request is complete
 */
export const sendEvents = async (
    payload: EventRequestBody,
    config: PipelineConfig,
    keepalive = false
): Promise<boolean> => {
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
            fetch(endpoint, fetchOptions)
                .then(async (response) => {
                    reportResponse(
                        response,
                        await readBody(response),
                        config,
                        payload.events.length
                    );
                })
                .catch((err) => {
                    logger.error('Keepalive request failed (browser may have ignored it):', err);
                    notify(config, {
                        code: 'NETWORK',
                        message: String(err),
                        events: payload.events.length
                    });
                });
            // We can't know if it succeeded, but for keepalive=true contexts we usually assume "sent"
            // or we return false since we can't confirm.
            // However, the caller usually ignores the return value for keepalive.
            return true;
        }

        // Normal request - await and check response
        const response = await fetch(endpoint, fetchOptions);

        if (!response.ok) {
            // Always log the HTTP status code
            const statusText = response.statusText || 'Unknown Error';
            const baseErrorMessage = `HTTP ${response.status}: ${statusText}`;

            const errorData = await readBody(response);

            if (errorData?.message) {
                logger.warn(`${errorData.message} (${baseErrorMessage})`);
            } else if (errorData) {
                // JSON parsed successfully but no message property
                logger.warn(`${baseErrorMessage} - No error message in response`);
            } else {
                logger.warn(`${baseErrorMessage} - Failed to parse error response`);
            }

            reportResponse(response, errorData, config, payload.events.length);

            return false;
        }

        // Accepted: dotCMS may still have failed some of the events
        reportResponse(response, await readBody(response), config, payload.events.length);

        return true;
    } catch (error) {
        logger.error('Error sending event:', error);
        notify(config, { code: 'NETWORK', message: String(error), events: payload.events.length });

        return false;
    }
};
