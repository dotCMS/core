import { UVE_MODE } from '../editor/public';

/**
 * Standardized HTTP error details
 */
export interface HttpErrorDetails {
    status: number;
    statusText: string;
    message: string;
    data?: unknown; // Error response body if available
}

/**
 * Standardized HTTP error class for all HTTP client implementations
 */
export class DotHttpError extends Error implements HttpErrorDetails {
    status: number;
    statusText: string;
    data?: unknown;

    constructor(details: HttpErrorDetails) {
        super(details.message);
        this.name = 'HttpError';
        this.status = details.status;
        this.statusText = details.statusText;
        this.data = details.data;
    }
}

/**
 * Interface for HTTP client implementations.
 * Allows the SDK to work with different HTTP libraries.
 *
 * @important IMPLEMENTATION REQUIREMENTS:
 * - ALL implementations MUST throw HttpError instances for failed requests
 * - NEVER throw generic Error or other error types
 * - Include response status, statusText, and message in HttpError
 * - Attempt to parse error response body and include in HttpError.data
 * - Handle network errors by wrapping them in HttpError (use createNetworkError helper)
 *
 * @example
 * ```typescript
 * // CORRECT - Throws HttpError
 * if (!response.ok) {
 *   throw new HttpError({
 *     status: response.status,
 *     statusText: response.statusText,
 *     message: `Request failed: ${response.status}`,
 *     data: await response.json()
 *   });
 * }
 *
 * // WRONG - Throws generic Error
 * if (!response.ok) {
 *   throw new Error(`HTTP ${response.status}`);
 * }
 * ```
 */
export interface DotHttpClient {
    /**
     * Makes an HTTP request.
     *
     * @template T - The expected response type
     * @param url - The URL to request
     * @param options - Request options (method, headers, body, etc.)
     * @returns A promise that resolves with the response data
     * @throws {DotHttpError} When the request fails (non-2xx status or network error)
     *
     * @important IMPLEMENTATION REQUIREMENTS:
     * - MUST throw DotHttpError instances for failed requests, never generic Error objects
     * - Include response status, statusText, and message in DotHttpError
     * - Attempt to parse error response body and include in DotHttpError.data
     * - Handle network errors by wrapping them in DotHttpError
     * - Consumers expect DotHttpError with status, statusText, and data properties for proper error handling
     */
    request<T = unknown>(url: string, options?: DotRequestOptions): Promise<T>;
}

/**
 * Abstract base class for HTTP client implementations.
 * Provides common error handling utilities and enforces HttpError contract.
 *
 * @example
 * ```typescript
 * // Fetch API example
 * export class FetchHttpClient extends BaseHttpClient {
 *   async request<T>(url: string, options?: DotRequestOptions): Promise<T> {
 *     try {
 *       const response = await fetch(url, options);
 *
 *       if (!response.ok) {
 *         // Parse response body and headers
 *         let errorBody: string | unknown;
 *         try {
 *           const contentType = response.headers.get('content-type');
 *           if (contentType?.includes('application/json')) {
 *             errorBody = await response.json();
 *           } else {
 *             errorBody = await response.text();
 *           }
 *         } catch {
 *           errorBody = response.statusText;
 *         }
 *
 *         const headers: Record<string, string> = {};
 *         response.headers.forEach((value, key) => {
 *           headers[key] = value;
 *         });
 *
 *         throw this.createHttpError(
 *           response.status,
 *           response.statusText,
 *           headers,
 *           errorBody
 *         );
 *       }
 *
 *       return response.json();
 *     } catch (error) {
 *       if (error instanceof TypeError) {
 *         throw this.createNetworkError(error);
 *       }
 *       throw error;
 *     }
 *   }
 * }
 *
 * // Axios example
 * export class AxiosHttpClient extends BaseHttpClient {
 *   async request<T>(url: string, options?: DotRequestOptions): Promise<T> {
 *     try {
 *       const response = await axios(url, options);
 *       return response.data;
 *     } catch (error) {
 *       if (axios.isAxiosError(error)) {
 *         throw this.createHttpError(
 *           error.response?.status || 0,
 *           error.response?.statusText || 'Network Error',
 *           error.response?.headers,
 *           error.response?.data
 *         );
 *       }
 *       throw this.createNetworkError(error);
 *     }
 *   }
 * }
 * ```
 */

export abstract class BaseHttpClient implements DotHttpClient {
    /**
     * Makes an HTTP request.
     *
     * @template T - The expected response type
     * @param url - The URL to request
     * @param options - Request options (method, headers, body, etc.)
     * @returns A promise that resolves with the response data
     * @throws {DotHttpError} When the request fails (non-2xx status or network error)
     *
     * @important IMPLEMENTATION REQUIREMENTS:
     * - MUST throw DotHttpError instances for failed requests, never generic Error objects
     * - Include response status, statusText, and message in DotHttpError
     * - Attempt to parse error response body and include in DotHttpError.data
     * - Handle network errors by wrapping them in DotHttpError using createNetworkError()
     * - Use createHttpError() helper for HTTP response errors
     */
    abstract request<T = unknown>(url: string, options?: DotRequestOptions): Promise<T>;

    /**
     * Creates a standardized HttpError from HTTP response details.
     * Handles parsing of error response body automatically.
     *
     * @param status - HTTP status code
     * @param statusText - HTTP status text
     * @param headers - Response headers (optional)
     * @param body - Response body (optional)
     * @param customMessage - Optional custom error message
     * @returns HttpError instance with parsed response data
     */
    protected createHttpError(
        status: number,
        statusText: string,
        headers?: Record<string, string>,
        body?: string | unknown,
        customMessage?: string
    ): DotHttpError {
        let errorData: unknown = body;

        // If body is a string, try to parse as JSON
        if (typeof body === 'string') {
            try {
                const contentType = headers?.['content-type'] || headers?.['Content-Type'];
                if (contentType?.includes('application/json')) {
                    errorData = JSON.parse(body);
                } else {
                    errorData = body;
                }
            } catch {
                errorData = body;
            }
        }

        return new DotHttpError({
            status,
            statusText,
            message: customMessage || `HTTP ${status}: ${statusText}`,
            data: errorData
        });
    }

    /**
     * Creates a standardized HttpError for network/connection errors.
     *
     * @param originalError - The original network error
     * @returns HttpError instance representing a network error
     */
    protected createNetworkError(originalError: Error): DotHttpError {
        return new DotHttpError({
            status: 0, // Network error status
            statusText: 'Network Error',
            message: `Network error: ${originalError.message}`,
            data: originalError
        });
    }
}

/**
 * The GraphQL parameters for a page request.
 * @public
 */
export interface DotCMSGraphQLParams {
    /**
     * The GraphQL query for the page.
     * @property {string} page - The GraphQL query for the page.
     */
    page?: string;
    /**
     * A record of GraphQL queries for content.
     * @property {Record<string, string>} content - A record of GraphQL queries for content.
     */
    content?: Record<string, string>;
    /**
     * Variables for the GraphQL queries.
     * @property {Record<string, string>} variables - Variables for the GraphQL queries.
     */
    variables?: Record<string, string>;
    /**
     * An array of GraphQL fragment strings.
     * @property {string[]} fragments - An array of GraphQL fragment strings.
     */
    fragments?: string[];
}

/**
 * Parameters for making a page request to DotCMS.
 * @public
 * @interface DotCMSPageRequestParams
 */
export interface DotCMSPageRequestParams {
    /**
     * The id of the site you want to interact with. Defaults to the one from the config if not provided.
     */
    siteId?: string;

    /**
     * The mode of the page you want to retrieve. Defaults to the site's default mode if not provided.
     */
    mode?: keyof typeof UVE_MODE;

    /**
     * The language id of the page you want to retrieve. Defaults to the site's default language if not provided.
     */
    languageId?: number | string;

    /**
     * The id of the persona for which you want to retrieve the page.
     */
    personaId?: string;

    /**
     * Whether to fire the rules set on the page.
     */
    fireRules?: boolean | string;

    /**
     * The publish date of the page you want to retrieve.
     * Must be an ISO 8601 formatted date string (e.g. '2025-06-19T12:59:41Z').
     *
     * @example
     * // ✓ Correct usage:
     * publishDate: '2025-06-19T12:59:41Z'
     * publishDate: '2023-12-25T00:00:00Z'
     *
     * // ✗ Incorrect usage:
     * publishDate: '19/06/2025'           // Wrong format
     * publishDate: '2025-06-19 12:59:41'  // Not ISO 8601 format
     */
    publishDate?: string;

    /**
     * The variant name of the page you want to retrieve.
     */
    variantName?: string;
    /**
     * The GraphQL options for the page.
     */
    graphql?: DotCMSGraphQLParams;
}

/**
 * Options for configuring fetch requests, excluding body and method properties.
 */
export type DotRequestOptions = RequestInit;

/**
 * Configuration options for the DotCMS client.
 */
export interface DotCMSClientConfig {
    /**
     * The URL of the dotCMS instance.
     * Ensure to include the protocol (http or https).
     * @example `https://demo.dotcms.com`
     */
    dotcmsUrl: string;

    /**
     * The authentication token for requests.
     * Obtainable from the dotCMS UI.
     */
    authToken: string;

    /**
     * The id of the site you want to interact with. Defaults to the default site if not provided.
     */
    siteId?: string;

    /**
     * Additional options for the fetch request.
     * @example `{ headers: { 'Content-Type': 'application/json' } }`
     */
    requestOptions?: DotRequestOptions;

    /**
     * Custom HTTP client implementation.
     * If not provided, the default FetchHttpClient will be used.
     * @example `{ httpClient: new AxiosHttpClient() }`
     */
    httpClient?: DotHttpClient;
}

/**
 * The parameters for the Navigation API.
 * @public
 */
export interface DotCMSNavigationRequestParams {
    /**
     * The depth of the folder tree to return.
     * @example
     * `1` returns only the element specified in the path.
     * `2` returns the element specified in the path, and if that element is a folder, returns all direct children of that folder.
     * `3` returns all children and grandchildren of the element specified in the path.
     */
    depth?: number;

    /**
     * The language ID of content to return.
     * @example
     * `1` (or unspecified) returns content in the default language of the site.
     */
    languageId?: number;
}
