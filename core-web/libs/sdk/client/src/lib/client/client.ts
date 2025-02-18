import { Content } from './content/content-api';
import { NavClient } from './nav';
import { PageClient } from './page';

export type ClientOptions = Omit<RequestInit, 'body' | 'method'>;

export interface ClientConfig {
    /**
     * The URL of the dotCMS instance.
     *
     * @description This is the URL of the dotCMS instance you want to interact with. Ensure to include the protocol (http or https).
     * @example `https://demo.dotcms.com`
     * @type {string}
     * @required
     */
    dotcmsUrl: string;
    /**
     * The id of the site you want to interact with. If not  provided, it will use the default site.
     *
     * More information here: {@link https://www.dotcms.com/docs/latest/multi-site-management}
     *
     * @description To get the site id, go to the site you want to interact with and copy the id from the History tab.
     * @type {string}
     * @optional
     */
    siteId?: string;
    /**
     * The authentication token to use for the requests.
     *
     * @description You can get the auth token from our UI {@link https://www.dotcms.com/docs/latest/rest-api-authentication#creating-an-api-token-in-the-ui}
     * @type {string}
     * @required
     */
    authToken: string;
    /**
     * Additional options to pass to the fetch request.
     *
     * @description These options will be used in the fetch request. Any option can be specified except for 'body' and 'method' which are omitted.
     * @example `{ headers: { 'Content-Type': 'application/json' } }`
     * @type {ClientOptions}
     * @optional
     */
    requestOptions?: ClientOptions;
}

function getHostURL(url: string): URL | undefined {
    try {
        return new URL(url);
    } catch (error) {
        return undefined;
    }
}

/**
 * `DotCmsClient` is a TypeScript class that provides methods to interact with the DotCMS REST API.
 * DotCMS is a hybrid-headless CMS and digital experience platform.
 *
 * @class DotCmsClient
 * @property {ClientConfig} config - The configuration object for the DotCMS client.
 * @property {Content} content - Provides methods to interact with content in DotCMS.
 *
 * @method constructor(config: ClientConfig) - Constructs a new instance of the DotCmsClient class.
 *
 * @method page.get(options: PageApiOptions): Promise<PageApiResponse> - Retrieves all the elements of any Page in your dotCMS system in JSON format.
 * The Page API enables you to retrieve page information, layout, template, content blocks, and more.
 * @see {@link https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas}
 *
 * @method nav.get(options: NavApiOptions = { depth: 0, path: '/', languageId: 1 }): Promise<NavApiResponse> - Retrieves information about the dotCMS file and folder tree.
 * The Navigation API allows you to fetch the site structure and menu items.
 * @see {@link https://www.dotcms.com/docs/latest/navigation-rest-api}
 *
 * @method content.get(options: ContentApiOptions): Promise<ContentApiResponse> - Retrieves content items based on specified criteria.
 * The Content API allows you to query and retrieve content by ID, inode, or using Lucene queries.
 * @see {@link https://www.dotcms.com/docs/latest/content-api-retrieval-and-querying}
 *
 * @method editor.on(action: string, callbackFn: (payload: unknown) => void) - Allows you to react to actions issued by the Universal Visual Editor (UVE).
 * @method editor.off(action: string) - Stops listening to an action issued by UVE.
 *
 * @static
 * @method init(config: ClientConfig): DotCmsClient - Initializes and returns a DotCmsClient instance.
 * @method dotcmsUrl: string - Retrieves the DotCMS URL from the instance configuration.
 *
 * @example <caption>Basic usage</caption>
 * ```javascript
 * const client = DotCmsClient.init({ dotcmsUrl: 'https://demo.dotcms.com', authToken: 'your-auth-token' });
 *
 * // Get a page
 * client.page.get({ path: '/about-us' }).then(response => console.log(response));
 *
 * // Get navigation
 * client.nav.get({ path: '/about-us', depth: 2 }).then(response => console.log(response));
 *
 * // Get content
 * client.content.get({ query: '+contentType:Blog +languageId:1', limit: 10 }).then(response => console.log(response));
 *
 * // Listen to editor changes
 * client.editor.on('changes', (payload) => console.log('Changes detected:', payload));
 * ```
 */
export class DotCmsClient {
    static instance: DotCmsClient;
    #config: ClientConfig;
    #requestOptions!: ClientOptions;

    dotcmsUrl?: string;
    content: Content;
    page: PageClient;
    nav: NavClient;

    constructor(
        config: ClientConfig = { dotcmsUrl: '', authToken: '', requestOptions: {}, siteId: '' }
    ) {
        this.#config = this.#getClientConfig(config);
        this.#requestOptions = this.#getRequestOptions(this.#config);
        this.content = new Content(this.#requestOptions, this.#config.dotcmsUrl);
        this.page = new PageClient(this.#config, this.#requestOptions);
        this.nav = new NavClient(this.#config, this.#requestOptions);
    }

    #getClientConfig(config: ClientConfig): ClientConfig {
        const dotcmsUrl = getHostURL(config.dotcmsUrl)?.origin ?? '';

        if (!this.dotcmsUrl) {
            throw new Error("Invalid configuration - 'dotcmsUrl' must be a valid URL");
        }

        if (!config.authToken) {
            throw new Error("Invalid configuration - 'authToken' is required");
        }

        this.#config = {
            ...config,
            dotcmsUrl
        };

        return this.#config;
    }

    #getRequestOptions(config: ClientConfig): ClientOptions {
        return {
            ...config.requestOptions,
            headers: {
                Authorization: `Bearer ${config.authToken}`,
                ...config.requestOptions?.headers
            }
        };
    }
}
