import { BaseHttpClient, DotRequestOptions } from '@dotcms/types';


/**
 * HTTP client implementation using the Fetch API.
 *
 * Extends BaseHttpClient to provide a standard interface for making HTTP requests.
 * Handles JSON and non-JSON responses, error parsing, and network error handling.
 */
export class FetchHttpClient extends BaseHttpClient {
  /**
   * Sends an HTTP request using the Fetch API.
   *
   * @template T - The expected response type.
   * @param {string} url - The URL to send the request to.
   * @param {DotRequestOptions} [options] - Optional fetch options (headers, method, body, etc).
   * @returns {Promise<T>} - Resolves with the parsed response or throws an error.
   * @throws {HttpError} - Throws if the response is not ok (status 4xx/5xx).
   * @throws {NetworkError} - Throws if a network error occurs.
   */
  async request<T = unknown>(url: string, options?: DotRequestOptions): Promise<T> {
    try {
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
