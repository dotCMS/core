import { Content } from './content/content-api';
import { NavigationClient } from './navigation/navigation-api';
import { PageClient } from './page/page-client';

export type RequestOptions = Omit<RequestInit, 'body' | 'method'>;

export interface DotCMSClientConfig {
    /**
     * The URL of the dotCMS instance.
     * Ensure to include the protocol (http or https).
     * @example `https://demo.dotcms.com`
     */
    dotcmsUrl: string;

    /**
     * The id of the site you want to interact with. Defaults to the default site if not provided.
     */
    siteId?: string;

    /**
     * The authentication token for requests.
     * Obtainable from the dotCMS UI.
     */
    authToken: string;

    /**
     * Additional options for the fetch request.
     */
    requestOptions?: RequestOptions;
}

function parseUrl(url: string): URL | undefined {
    try {
        return new URL(url);
    } catch {
        return undefined;
    }
}

const defaultConfig: DotCMSClientConfig = {
    dotcmsUrl: '',
    authToken: '',
    requestOptions: {}
};

/**
 * `DotCMSClient` provides methods to interact with the DotCMS REST API.
 */
class DotCMSClient {
    private config: DotCMSClientConfig;
    private requestOptions!: RequestOptions;

    dotcmsUrl?: string;
    content: Content;
    page: PageClient;
    nav: NavigationClient;

    constructor(config: DotCMSClientConfig = defaultConfig) {
        this.config = this.initializeConfig(config);
        this.requestOptions = this.initializeRequestOptions(this.config);

        // Initialize clients
        this.page = new PageClient(this.config, this.requestOptions);
        this.content = new Content(this.requestOptions, this.config.dotcmsUrl);
        this.nav = new NavigationClient(this.config, this.requestOptions);
    }

    private initializeConfig(config: DotCMSClientConfig): DotCMSClientConfig {
        const dotcmsUrl = parseUrl(config.dotcmsUrl)?.origin ?? '';

        if (!dotcmsUrl) {
            throw new Error("Invalid configuration - 'dotcmsUrl' must be a valid URL");
        }

        if (!config.authToken) {
            throw new Error("Invalid configuration - 'authToken' is required");
        }

        return {
            ...config,
            dotcmsUrl
        };
    }

    private initializeRequestOptions(config: DotCMSClientConfig): RequestOptions {
        return {
            ...config.requestOptions,
            headers: {
                Authorization: `Bearer ${config.authToken}`,
                ...config.requestOptions?.headers
            }
        };
    }
}

export const dotCMSCreateClient = (config: DotCMSClientConfig) => new DotCMSClient(config);
