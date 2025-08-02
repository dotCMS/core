import { UVE_MODE } from '../editor/public';

/**
 * Interface for HTTP client implementations.
 * Allows the SDK to work with different HTTP libraries.
 */
export interface HttpClient {
  /**
   * Makes an HTTP request.
   *
   * @param url - The URL to request
   * @param options - Request options (method, headers, body, etc.)
   * @returns A promise that resolves with the response data
   */
  request<T = unknown>(url: string, options?: RequestOptions): Promise<T>;
}

/**
 * Request options for HTTP client implementations.
 * Extends the native RequestInit interface to be compatible with fetch.
 */
export interface HttpClientRequestOptions extends Omit<RequestInit, 'body' | 'method'> {
    /**
     * The HTTP method to use for the request.
     */
    method?: string;

    /**
     * The request body.
     */
    body?: string | FormData | URLSearchParams | ReadableStream | null;
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
export type RequestOptions = Omit<RequestInit, 'body' | 'method'>;

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
    requestOptions?: RequestOptions;

    /**
     * Custom HTTP client implementation.
     * If not provided, the default FetchHttpClient will be used.
     * @example `{ httpClient: new AxiosHttpClient() }`
     */
    httpClient?: HttpClient;
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
