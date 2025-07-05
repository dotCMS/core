/**
 * AgnosticClient provides a base HTTP client for dotCMS REST API services.
 *
 * This class automatically reads the dotCMS URL and authentication token from environment variables.
 * It provides a protected fetch method that child service classes can use to make authenticated requests.
 */
import { Logger } from '../utils/logger';

export class AgnosticClient {
    /**
     * The base URL for the dotCMS API, read from the DOTCMS_URL environment variable.
     */
    protected dotcmsUrl: string;

    /**
     * The authentication token for dotCMS API requests, read from the AUTH_TOKEN environment variable.
     * This value is required.
     */
    protected authToken: string;

    /**
     * Logger instance for this client
     */
    protected logger: Logger;

    /**
     * Constructs an AgnosticClient instance.
     *
     * @throws {Error} If the DOTCMS_URL or AUTH_TOKEN environment variable is not set.
     */
    constructor() {
        this.dotcmsUrl = process.env.DOTCMS_URL || '';
        this.authToken = process.env.AUTH_TOKEN || '';

        if (!this.dotcmsUrl) {
            throw new Error('DOTCMS_URL environment variable is required');
        }

        if (!this.authToken) {
            throw new Error('AUTH_TOKEN environment variable is required');
        }

        this.logger = new Logger('AGNOSTIC_CLIENT');
    }

    /**
     * Makes an HTTP request to the specified URL with the provided options, automatically adding the Authorization header.
     * Only logs when requests fail, including raw data from dotCMS.
     * If a relative URL is provided, it will be automatically prefixed with the dotCMS base URL.
     *
     * @param url - The URL to request. Can be a full URL or a relative path (e.g., '/api/v1/contenttype').
     * @param options - Optional fetch options (method, headers, body, etc.).
     * @returns A Promise resolving to the fetch Response object.
     */
    protected async fetch(url: string, options: RequestInit = {}): Promise<Response> {
        const method = options.method || 'GET';

        // If the URL is relative (doesn't start with http:// or https://), prepend the dotCMS base URL
        const fullUrl = url.startsWith('http://') || url.startsWith('https://')
            ? url
            : `${this.dotcmsUrl}${url.startsWith('/') ? url : `/${url}`}`;

        const headers: Record<string, string> = {
            ...(options.headers as Record<string, string> || {}),
        };
        headers['Authorization'] = `Bearer ${this.authToken}`;

        // Add Content-Type: application/json by default if not already set
        if (!headers['Content-Type'] && !headers['content-type']) {
            headers['Content-Type'] = 'application/json';
        }

        try {
            const response = await fetch(fullUrl, { ...options, headers });

            if (!response.ok) {
                // Try to get error details from response
                let errorDetails = '';
                let rawData = null;
                try {
                    const errorData = await response.text();
                    errorDetails = errorData;

                    try {
                        rawData = JSON.parse(errorData);
                    } catch {
                        rawData = errorData;
                    }
                } catch (e) {
                    errorDetails = 'Could not read error response';
                }

                this.logger.error('dotCMS server returned error', {
                    status: response.status,
                    statusText: response.statusText,
                    url: fullUrl,
                    method,
                    rawData,
                    headers: Object.fromEntries(response.headers.entries())
                });

                throw new Error(`Request failed: ${response.status} ${response.statusText}. Details: ${errorDetails}`);
            }

            return response;

        } catch (error) {
            this.logger.error('Error during request', {
                error: error instanceof Error ? { message: error.message, stack: error.stack } : error,
                url: fullUrl,
                method
            });

            throw new Error('Error during request: ' + error);
        }
    }
}
