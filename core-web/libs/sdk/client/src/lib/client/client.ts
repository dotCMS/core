import { consola } from 'consola';
import { firstValueFrom } from 'rxjs';

import { DotCMSClientConfig, DotRequestOptions, DotHttpClient } from '@dotcms/types';


import { FetchHttpClient } from './adapters/fetch-http-client';
import { Content } from './content/content-api';
import { NavigationClient } from './navigation/navigation-api';
import { PageClient } from './page/page-api';

/**
 * Parses a string into a URL object.
 *
 * @param url - The URL string to parse
 * @returns A URL object if parsing is successful, undefined otherwise
 */
function parseURL(url: string): URL | undefined {
    try {
        return new URL(url);
    } catch {
        consola.error('[DotCMS Client]: Invalid URL:', url);

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
    private requestOptions!: DotRequestOptions;
    private httpClient: DotHttpClient;

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
        console.log(config.ngClient);
        this.httpClient = config.httpClient || new FetchHttpClient();
        this.requestOptions = this.createAuthenticatedRequestOptions(this.config);

        // Initialize clients with httpClient
        this.page = new PageClient(this.config, this.requestOptions, this.httpClient);
        this.nav = new NavigationClient(this.config, this.requestOptions, this.httpClient);
        this.content = new Content(this.requestOptions, this.config.dotcmsUrl, this.httpClient, this.config.ngClient);
    }

    fetchUsingNgClient() {
        const request = this.config.ngClient
            .post('http://localhost:8080/api/content/_search', {
            query: '+contentType:Blog +languageId:1 +live:true',
            render: false,
            sort: 'Blog.postingDate asc',
            limit: 3,
            offset: 0,
            depth: 0
        })
        return firstValueFrom(request);
    }

    /**
     * Creates request options with authentication headers.
     *
     * @param config - The client configuration
     * @returns Request options with authorization headers
     */
    private createAuthenticatedRequestOptions(config: DotCMSClientConfig): DotRequestOptions {
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
export const createDotCMSClient = (clientConfig: DotCMSClientConfig): DotCMSClient => {
    const { dotcmsUrl, authToken } = clientConfig || {};
    const instanceUrl = parseURL(dotcmsUrl)?.origin;

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
