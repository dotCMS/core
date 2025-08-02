import { HttpClient, HttpClientRequestOptions } from '@dotcms/types';

/**
 * Default HTTP client implementation using the native fetch API.
 */
export class FetchHttpClient implements HttpClient {
  async request<T = unknown>(url: string, options?: HttpClientRequestOptions): Promise<T> {
    const response = await fetch(url, options);

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    // Handle different response types
    const contentType = response.headers.get('content-type');
    if (contentType?.includes('application/json')) {
      return response.json();
    }

    // For non-JSON responses, return the response object
    // Sub-clients can handle specific response types as needed
    return response as T;
  }
}
