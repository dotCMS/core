import { DotCMSClientConfig, DotRequestOptions, DotHttpClient } from '@dotcms/types';

import { AIClient } from './ai/ai-api';
import { Content } from './content/content-api';
import { NavigationClient } from './navigation/navigation-api';
import { PageClient } from './page/page-api';
import { createClientContext } from './shared/client-context';

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
     * Client for AI-related operations.
     * @experimental This client is experimental and may be subject to change.
     */
    ai: AIClient;

    /**
     * Creates a new DotCMS client instance.
     *
     * @param config - Validated configuration and transport for the client
     */
    constructor({ config, requestOptions, httpClient }: ReturnType<typeof createClientContext>) {
        this.config = config;
        this.httpClient = httpClient;
        this.requestOptions = requestOptions;

        // Initialize clients with httpClient
        this.page = new PageClient(this.config, this.requestOptions, this.httpClient);
        this.nav = new NavigationClient(this.config, this.requestOptions, this.httpClient);
        this.content = new Content(this.config, this.requestOptions, this.httpClient);
        this.ai = new AIClient(this.config, this.requestOptions, this.httpClient);
    }
}

/**
 * Creates and returns a new DotCMS client instance.
 *
 * This is the full-featured entry point: it exposes page, navigation, content and AI in one
 * object, which means a bundle that imports it retains all four. Consumers who only need one
 * area can import a focused factory instead — `@dotcms/client/page`, `/navigation`,
 * `/content` or `/ai` — and leave the rest out of their bundle entirely.
 *
 * @param config - Configuration options for the client
 * @returns A configured DotCMS client instance
 * @example
 * ```typescript
 * const client = createDotCMSClient({
 *   dotcmsUrl: 'https://demo.dotcms.com',
 *   authToken: 'your-auth-token'
 * });
 *
 * // Use the client to fetch content
 * const pages = await client.page.get('/about-us');
 * ```
 */
export const createDotCMSClient = (clientConfig: DotCMSClientConfig): DotCMSClient => {
    return new DotCMSClient(createClientContext(clientConfig));
};
