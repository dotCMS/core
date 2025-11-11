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

    //  This is my proposal to add this method to the class. We will need to stream data on AI Gen/Chat

    // async requestStream(
    //     url: string,
    //     options?: DotRequestOptions
    // ): Promise<{ body: ReadableStream<BufferSource>; contentLength: number }> {
    //     try {
    //         const response = await fetch(url, options);

    //         if (!response.ok) {
    //             throw this.createHttpError(
    //                 response.status,
    //                 response.statusText,
    //                 Object.fromEntries(response.headers.entries()),
    //                 await response.text()
    //             );
    //         }
    //         console.log(response.headers.entries());

    // This is needed to inform the client about the progress of the request.

    //         const length = response.headers.get('content-length');
    //         console.log('content-length', length);

    //         return {
    //             body: response.body as ReadableStream<BufferSource>,
    //             contentLength: length ? parseInt(length, 10) : 0
    //         };
    //     } catch (error) {
    //         // Handle network errors (fetch throws TypeError for network issues)
    //         if (error instanceof TypeError) {
    //             throw this.createNetworkError(error);
    //         }

    //         throw error;
    //     }
    // }
}

// Here is an example of how to stream data from the server to the client.
// The idea behind this is to let the user execute a callback every time there is a new chunk of data.
// That way the can do something like

// - Display a progress bar
// - Display the data in real time
// - Do something else with the data

// Example:
// ```typescript
// const client = new FetchHttpClient();

// client.stream((data) => {
//     console.log(data.value);
//     console.log(data.progress);
//     console.log(data.done);
// });
// ```

// async stream(
//     callback: (data: {
//         done: boolean;
//         value: string | null;
//         progress: number;
//     }) => void
// ): Promise<void> {
//     const { body, contentLength } = await this.fetchStream();

//     // We need to decode the data to a string, because we recieve a ReadableStream<Uint8Array>
//     // and we need to process it as a string.
//     // The TextDecoderStream is a transform stream that decodes a stream of bytes into a stream of strings.
//     const reader = body.pipeThrough(new TextDecoderStream()).getReader();

//     let recieved = 0;

//     try {
//         while (true) {
//             const { done, value } = await reader.read();

//                 recieved += value?.length ?? 0;

// We need to avoid divisions by 0, but this is the calculation to get the progress 0-100
//           callback({ done, value: value, progress: recieved / (contentLength * 100) });\

//           if (done) break;
//         }
//     } catch (error) {
//         throw new Error(
//             `AI Search failed for '${this.#prompt}' (stream): ${error instanceof Error ? error.message : 'Unknown error'}`
//         );
//     } finally {
//         reader.releaseLock();
//     }
// }
// private async fetchStream(): Promise<{
//     body: ReadableStream<BufferSource>;
//     contentLength: number;
// }> {
//     const searchParams = this.buildSearchParams(this.#prompt, {
//         ...this.#params
//     });

// This is an example using this API, but we just need to be sure that the request has the header:
// transfer-encoding: chunked

//     const url = new URL('/api/v1/ai/search', this.dotcmsUrl);
//     url.search = searchParams.toString() + '&stream=true';

//     const response = await this.httpClient.requestStream(url.toString(), {
//         ...this.requestOptions,
//         headers: {
//             ...this.requestOptions.headers
//         },
//         method: 'GET'
//     });

//     return response;
// }
