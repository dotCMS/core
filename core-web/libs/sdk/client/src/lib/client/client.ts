import { Content } from './content/content-api';
import { NavigationClient } from './navigation/navigation-api';
import { PageClient } from './page/page-api';

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
}

/**
 * Parses a string into a URL object.
 *
 * @param url - The URL string to parse
 * @returns A URL object if parsing is successful, undefined otherwise
 */
function parseUrl(url: string): URL | undefined {
    try {
        return new URL(url);
    } catch {
        return undefined;
    }
}

/**
 * Default configuration for the DotCMS client.
 */
const defaultConfig: DotCMSClientConfig = {
    dotcmsUrl: '',
    authToken: '',
    requestOptions: {}
};

/**
 * Client for interacting with the DotCMS REST API.
 * Provides access to content, page, and navigation functionality.
 */
class DotCMSClient {
    private config: DotCMSClientConfig;
    private requestOptions!: RequestOptions;

    /**
     * Client for content-related operations.
     */
    content: Content;

    /**
     * Client for page-related operations.
     */
    page: PageClient;

    /**
     * Client for navigation-related operations.
     */
    nav: NavigationClient;

    /**
     * Creates a new DotCMS client instance.
     *
     * @param config - Configuration options for the client
     * @throws Warning if dotcmsUrl is invalid or authToken is missing
     */
    constructor(config: DotCMSClientConfig = defaultConfig) {
        this.config = config;
        this.requestOptions = this.createAuthenticatedRequestOptions(this.config);

        // Initialize clients
        this.page = new PageClient(this.config, this.requestOptions);
        this.nav = new NavigationClient(this.config, this.requestOptions);
        this.content = new Content(this.requestOptions, this.config.dotcmsUrl);
    }

    /**
     * Creates request options with authentication headers.
     *
     * @param config - The client configuration
     * @returns Request options with authorization headers
     */
    private createAuthenticatedRequestOptions(config: DotCMSClientConfig): RequestOptions {
        return {
            ...config.requestOptions,
            headers: {
                ...config.requestOptions?.headers,
                Authorization: `Bearer ${config.authToken}`
            }
        };
    }
}

/**
 * Creates and returns a new DotCMS client instance.
 *
 * @param config - Configuration options for the client
 * @returns A configured DotCMS client instance
 * @example
 * ```typescript
 * const client = dotCMSCreateClient({
 *   dotcmsUrl: 'https://demo.dotcms.com',
 *   authToken: 'your-auth-token'
 * });
 *
 * // Use the client to fetch content
 * const pages = await client.page.get('/about-us');
 * ```
 */
export const createDotCMSClient = (clientConfig: DotCMSClientConfig) => {
    const { dotcmsUrl, authToken } = clientConfig || {};
    const instanceUrl = parseUrl(dotcmsUrl)?.origin;

    if (!instanceUrl) {
        throw new TypeError("Invalid configuration - 'dotcmsUrl' must be a valid URL");
    }

    if (!authToken) {
        throw new TypeError("Invalid configuration - 'authToken' is required");
    }

    const config = {
        ...clientConfig,
        authToken,
        dotcmsUrl: instanceUrl
    };

    return new DotCMSClient(config);
};
