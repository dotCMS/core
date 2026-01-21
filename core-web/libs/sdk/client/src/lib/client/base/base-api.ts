import { DotCMSClientConfig, DotHttpClient, DotRequestOptions } from '@dotcms/types';

/**
 * Base class for all DotCMS API clients.
 * Provides common constructor parameters and properties that all API clients need.
 * Uses JavaScript private fields (#) for true runtime privacy.
 */
export abstract class BaseApiClient {
    /**
     * Request options including authorization headers.
     * @private
     */
    #requestOptions: DotRequestOptions;

    /**
     * DotCMS client configuration.
     * @private
     */
    #config: DotCMSClientConfig;

    /**
     * HTTP client for making requests.
     * @private
     */
    #httpClient: DotHttpClient;

    /**
     * Creates a new API client instance.
     *
     * @param {DotCMSClientConfig} config - Configuration options for the DotCMS client
     * @param {DotRequestOptions} requestOptions - Options for fetch requests including authorization headers
     * @param {DotHttpClient} httpClient - HTTP client for making requests
     */
    constructor(
        config: DotCMSClientConfig,
        requestOptions: DotRequestOptions = {},
        httpClient: DotHttpClient
    ) {
        this.#config = {
            siteId: '',
            ...config
        };
        this.#requestOptions = {
            ...requestOptions
        };
        this.#httpClient = httpClient;
    }

    /**
     * Gets the request options including authorization headers.
     * @protected
     */
    protected get requestOptions(): DotRequestOptions {
        return this.#requestOptions;
    }

    /**
     * Gets the DotCMS client configuration.
     * @protected
     */
    protected get config(): DotCMSClientConfig {
        return this.#config;
    }

    /**
     * Gets the HTTP client for making requests.
     * @protected
     */
    protected get httpClient(): DotHttpClient {
        return this.#httpClient;
    }

    /**
     * Gets the DotCMS URL from config.
     * @protected
     */
    protected get dotcmsUrl(): string {
        return this.#config.dotcmsUrl;
    }

    /**
     * Gets the site ID from config.
     * @protected
     */
    protected get siteId(): string {
        return this.#config.siteId || '';
    }
}
