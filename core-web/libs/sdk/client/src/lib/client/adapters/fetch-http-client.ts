import { SDK_VERSION } from 'virtual:sdk-version';

import { BaseHttpClient, DotRequestOptions } from '@dotcms/types';

import { checkSdkCompatibility } from '../../utils/sdk-compatibility';
// Build-time constant — see sdkVersionPlugin in rollup.config.cjs.

/**
 * HTTP client implementation using the Fetch API.
 *
 * Extends BaseHttpClient to provide a standard interface for making HTTP requests.
 * This implementation uses the native Fetch API and handles:
 * - JSON response parsing
 * - HTTP error response parsing and conversion to DotHttpError
 * - Network error handling and wrapping
 * - Diagnosing misconfiguration: a response in a format other than JSON, or a request
 *   that was redirected to another origin, is named as such in the error message
 *
 * Every SDK caller talks to a dotCMS JSON endpoint, so a successful response with a
 * non-JSON content type is rejected rather than returned.
 *
 * @example
 * ```typescript
 * const client = new FetchHttpClient();
 *
 * const data = await client.request<MyType>('/api/data', {
 *   method: 'GET',
 *   headers: { 'Authorization': 'Bearer token' }
 * });
 * ```
 */
export class FetchHttpClient extends BaseHttpClient {
    /**
     * Sends an HTTP request using the Fetch API.
     *
     * Implements the abstract request method from BaseHttpClient using the native Fetch API.
     * Automatically handles response parsing based on Content-Type headers and converts
     * HTTP errors to standardized DotHttpError instances.
     *
     * @template T - The parsed JSON body type.
     * @param url - The URL to send the request to.
     * @param options - Optional fetch options including method, headers, body, etc.
     * @returns Promise that resolves with the parsed JSON body, or the Response object when the
     *          response carries no content type at all.
     * @throws {DotHttpError} - Throws DotHttpError for HTTP errors (4xx/5xx status codes).
     * @throws {DotHttpError} - Throws DotHttpError (status 502) for a successful response whose content type is not JSON.
     * @throws {DotHttpError} - Throws DotHttpError for network errors (connection issues, timeouts).
     *
     * @example
     * ```typescript
     * // JSON API request
     * const user = await client.request<User>('/api/users/123', {
     *   method: 'GET',
     *   headers: { 'Accept': 'application/json' }
     * });
     *
     * // POST request with JSON body
     * const result = await client.request<CreateResult>('/api/users', {
     *   method: 'POST',
     *   headers: { 'Content-Type': 'application/json' },
     *   body: JSON.stringify({ name: 'John', email: 'john@example.com' })
     * });
     * ```
     */
    async request<T = unknown>(url: string, options?: DotRequestOptions): Promise<T> {
        try {
            // Use native fetch API - no additional configuration needed
            const response = await fetch(url, options);

            // Fire-and-forget: reads X-DotCMS-Version / X-DotCMS-Min-SDK off the
            // response and logs a console warning on mismatch. Fails open (no headers,
            // e.g. an older server) and never throws, so this can't affect the actual
            // request/response handling below.
            checkSdkCompatibility(response.headers, SDK_VERSION);

            const contentType = response.headers.get('content-type');
            // application/json and the structured-syntax variants (problem+json,
            // graphql-response+json, ...), in any case.
            const isJson = /^application\/([\w.-]+\+)?json\b/i.test(contentType ?? '');

            if (!response.ok) {
                // Parse response body for error context
                let errorBody: string | unknown;
                try {
                    errorBody = isJson ? await response.json() : await response.text();
                } catch {
                    errorBody = response.statusText;
                }

                // The status alone hides the cause, so name what else went wrong: the redirect
                // first (it is usually the root cause), then the unexpected format.
                const hints = [
                    describeCrossOriginRedirect(url, response),
                    contentType && !isJson
                        ? `The response was '${contentType}', not JSON, so it may not have come from the dotCMS API: a proxy, a load balancer or another server can answer this way.`
                        : undefined
                ].filter(Boolean);
                const status = `HTTP ${response.status}: ${response.statusText}`;
                const message = hints.length ? `${status}. ${hints.join(' ')}` : status;

                throw this.createHttpError(
                    response.status,
                    response.statusText,
                    toPlainHeaders(response.headers),
                    errorBody,
                    message
                );
            }

            if (isJson) {
                return response.json();
            }

            // Every SDK caller talks to a JSON endpoint, so a successful answer in another
            // format means something other than the dotCMS API answered (a login page, a
            // proxy, the wrong port). Returning the Response would only move the failure
            // downstream.
            if (contentType) {
                let body: string | undefined;
                try {
                    body = await response.text();
                } catch {
                    body = undefined;
                }

                const crossOriginRedirect = describeCrossOriginRedirect(url, response);
                const sameOriginRedirect =
                    !crossOriginRedirect && response.redirected && response.url
                        ? ` after a redirect to '${response.url}'`
                        : '';
                const message = [
                    `Expected a JSON response from '${url}' but received '${contentType}' (HTTP ${response.status})${sameOriginRedirect}, which is not JSON. dotCMS API endpoints answer in JSON, so something other than the dotCMS API answered (a login page, a proxy or another server): check that dotcmsUrl points at your dotCMS instance.`,
                    crossOriginRedirect
                ]
                    .filter(Boolean)
                    .join(' ');

                // Reported as 502 Bad Gateway, not the real 2xx: callers forward error.status
                // as their own response status, and an error must never read as success. The
                // real status is in the message and the body in data.
                throw this.createHttpError(
                    502,
                    'Bad Gateway',
                    toPlainHeaders(response.headers),
                    body,
                    message
                );
            }

            // No content type at all: hand back the Response untouched.
            return response as T;
        } catch (error) {
            // Handle network errors (fetch throws TypeError for network issues)
            if (error instanceof TypeError) {
                throw this.createNetworkError(error);
            }

            throw error;
        }
    }
}

/**
 * Explains a redirect that took the request to a different origin, or returns undefined
 * when there was no redirect, it stayed on the same origin, or either URL can't be parsed.
 *
 * A changed origin almost always means `dotcmsUrl` names a scheme or host the server moves
 * away from (e.g. `http://` answered with a 301 to `https://`), and the hop itself breaks the
 * request: fetch drops the Authorization header on a cross-origin redirect, and a 301, 302 or
 * 303 turns a POST into a GET without its body.
 *
 * @param requestUrl - The URL the SDK asked for.
 * @param response - The response fetch ended on after following redirects.
 * @returns A sentence naming both URLs and `dotcmsUrl`, or undefined.
 */
function describeCrossOriginRedirect(requestUrl: string, response: Response): string | undefined {
    if (!response.redirected || !response.url) {
        return undefined;
    }

    try {
        if (new URL(requestUrl).origin === new URL(response.url).origin) {
            return undefined;
        }
    } catch {
        return undefined;
    }

    return `The request to '${requestUrl}' was redirected to '${response.url}', a different origin. Check that dotcmsUrl uses the exact scheme and host your dotCMS instance serves: a cross-origin redirect drops the Authorization header, and a 301, 302 or 303 turns a POST into a GET without its body.`;
}

/**
 * Copies fetch Headers into a plain object, the shape createHttpError expects.
 *
 * @param headers - The response headers.
 * @returns The headers keyed by lower-case name.
 */
function toPlainHeaders(headers: Headers): Record<string, string> {
    const plain: Record<string, string> = {};
    headers.forEach((value, key) => {
        plain[key] = value;
    });

    return plain;
}
