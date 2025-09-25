import { BaseHttpClient, DotRequestOptions } from '@dotcms/types';

/**
 * HTTP client implementation using the Fetch API.
 *
 * Extends BaseHttpClient to provide a standard interface for making HTTP requests.
 * This implementation uses the native Fetch API and handles:
 * - JSON and non-JSON response parsing
 * - HTTP error response parsing and conversion to DotHttpError
 * - Network error handling and wrapping
 * - Content-Type detection for proper response handling
 *
 * @example
 * ```typescript
 * const client = new FetchHttpClient();
 *
 * // JSON request
 * const data = await client.request<MyType>('/api/data', {
 *   method: 'GET',
 *   headers: { 'Authorization': 'Bearer token' }
 * });
 *
 * // Non-JSON request (e.g., file download)
 * const response = await client.request<Response>('/api/file.pdf', {
 *   method: 'GET'
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
     * @template T - The expected response type. For JSON responses, T should be the parsed object type.
     *               For non-JSON responses, T should be Response or the expected response type.
     * @param url - The URL to send the request to.
     * @param options - Optional fetch options including method, headers, body, etc.
     * @returns Promise that resolves with the parsed response data or the Response object for non-JSON.
     * @throws {DotHttpError} - Throws DotHttpError for HTTP errors (4xx/5xx status codes).
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
     *
     * // File download (non-JSON response)
     * const response = await client.request<Response>('/api/files/document.pdf', {
     *   method: 'GET'
     * });
     * ```
     */
    async request<T = unknown>(url: string, options?: DotRequestOptions): Promise<T> {
        try {
            // Use native fetch API - no additional configuration needed
            const response = await fetch(url, options);

            if (!response.ok) {
                // Parse response body for error context
                let errorBody: string | unknown;
                try {
                    const contentType = response.headers.get('content-type');
                    if (contentType?.includes('application/json')) {
                        errorBody = await response.json();
                    } else {
                        errorBody = await response.text();
                    }
                } catch {
                    errorBody = response.statusText;
                }

                // Convert headers to plain object
                const headers: Record<string, string> = {};
                response.headers.forEach((value, key) => {
                    headers[key] = value;
                });

                throw this.createHttpError(
                    response.status,
                    response.statusText,
                    headers,
                    errorBody
                );
            }

            // Handle different response types
            const contentType = response.headers.get('content-type');
            if (contentType?.includes('application/json')) {
                return response.json();
            }

            // For non-JSON responses, return the response object
            // Sub-clients can handle specific response types as needed
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
