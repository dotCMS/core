import { Content } from './content/content-api';
import { ErrorMessages } from './models';
import { DotcmsClientListener } from './models/types';

import { isInsideEditor } from '../editor/sdk-editor';

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

export type PageApiOptions = {
    /**
     * The path of the page you want to retrieve.
     * @type {string}
     */
    path: string;
    /**
     * The id of the site you want to interact with. If not provided, the one from the config will be used.
     *
     * More information here: {@link https://www.dotcms.com/docs/latest/multi-site-management}
     * @type {string}
     * @optional
     */
    siteId?: string;
    /**
     * The mode of the page you want to retrieve. If not provided, will use the default mode of the site.
     *
     * More information here: {@link https://www.dotcms.com/docs/latest/page-viewing-modes}
     * @type {string}
     * @optional
     */
    mode?: 'EDIT_MODE' | 'PREVIEW_MODE' | 'LIVE_MODE';
    /**
     * The language id of the page you want to retrieve. If not provided, will use the default language of the site.
     * @type {number}
     * @optional
     */
    language_id?: number;
    /**
     * The id of the persona you want to retrieve the page for.
     *
     * More information here: {@link https://www.dotcms.com/docs/latest/personas}
     * @type {string}
     * @optional
     */
    personaId?: string;
    /**
     * If you want to fire the rules set on the page.
     *
     * More information here: {@link https://www.dotcms.com/docs/latest/adding-rules-to-pages}
     *
     * @type {boolean}
     * @optional
     */
    fireRules?: boolean;
    /**
     * Allows access to related content via the Relationship fields of contentlets on a Page; 0 (default).
     * @type {number}
     * @optional
     */
    depth?: number;
};

type NavApiOptions = {
    /**
     * The root path to begin traversing the folder tree.
     * @example
     * `/api/v1/nav/` starts from the root of the site
     * @example
     * `/about-us` starts from the "About Us" folder
     * @type {string}
     */
    path: string;
    /**
     * The depth of the folder tree to return.
     * @example
     * `1` returns only the element specified in the path.
     * @example
     * `2` returns the element specified in the path, and if that element is a folder, returns all direct children of that folder.
     * @example
     * `3` returns all children and grandchildren of the element specified in the path.
     * @type {number}
     * @optional
     */
    depth?: number;
    /**
     * The language ID of content to return.
     * @example
     * `1` (or unspecified) returns content in the default language of the site.
     * @link https://www.dotcms.com/docs/latest/system-language-properties#DefaultLanguage
     * @link https://www.dotcms.com/docs/latest/adding-and-editing-languages#LanguageID
     * @type {number}
     * @optional
     */
    languageId?: number;
};

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
    #listeners: DotcmsClientListener[] = [];

    dotcmsUrl?: string;
    content: Content;

    constructor(
        config: ClientConfig = { dotcmsUrl: '', authToken: '', requestOptions: {}, siteId: '' }
    ) {
        if (!config.dotcmsUrl) {
            throw new Error("Invalid configuration - 'dotcmsUrl' is required");
        }

        this.dotcmsUrl = getHostURL(config.dotcmsUrl)?.origin;

        if (!this.dotcmsUrl) {
            throw new Error("Invalid configuration - 'dotcmsUrl' must be a valid URL");
        }

        if (!config.authToken) {
            throw new Error("Invalid configuration - 'authToken' is required");
        }

        this.#config = {
            ...config,
            dotcmsUrl: this.dotcmsUrl
        };

        this.#requestOptions = {
            ...this.#config.requestOptions,
            headers: {
                Authorization: `Bearer ${this.#config.authToken}`,
                ...this.#config.requestOptions?.headers
            }
        };

        this.content = new Content(this.#requestOptions, this.#config.dotcmsUrl);
    }

    page = {
        /**
         * `page.get` is an asynchronous method of the `DotCmsClient` class that retrieves all the elements of any Page in your dotCMS system in JSON format.
         * It takes a `PageApiOptions` object as a parameter and returns a Promise that resolves to the response from the DotCMS API.
         *
         * The Page API enables you to retrieve all the elements of any Page in your dotCMS system.
         * The elements may be retrieved in JSON format.
         *
         * @link https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas
         * @async
         * @param {PageApiOptions} options - The options for the Page API call.
         * @returns {Promise<unknown>} - A Promise that resolves to the response from the DotCMS API.
         * @throws {Error} - Throws an error if the options are not valid.
         * @example
         * ```ts
         * const client = new DotCmsClient({ dotcmsUrl: 'https://your.dotcms.com', authToken: 'your-auth-token', siteId: 'your-site-id' });
         * client.page.get({ path: '/about-us' }).then(response => console.log(response));
         * ```
         */
        get: async (options: PageApiOptions): Promise<unknown> => {
            this.validatePageOptions(options);

            const queryParamsObj: Record<string, string> = {};
            for (const [key, value] of Object.entries(options)) {
                if (value === undefined || key === 'path' || key === 'siteId') continue;

                if (key === 'personaId') {
                    queryParamsObj['com.dotmarketing.persona.id'] = String(value);
                } else if (key === 'mode' && value) {
                    queryParamsObj['mode'] = String(value);
                } else {
                    queryParamsObj[key] = String(value);
                }
            }

            const queryHostId = options.siteId ?? this.#config.siteId ?? '';

            if (queryHostId) {
                queryParamsObj['host_id'] = queryHostId;
            }

            const queryParams = new URLSearchParams(queryParamsObj).toString();

            const formattedPath = options.path.startsWith('/') ? options.path : `/${options.path}`;
            const url = `${this.#config.dotcmsUrl}/api/v1/page/json${formattedPath}${
                queryParams ? `?${queryParams}` : ''
            }`;

            const response = await fetch(url, this.#requestOptions);
            if (!response.ok) {
                const error = {
                    status: response.status,
                    message: ErrorMessages[response.status] || response.statusText
                };

                console.error(error);
                throw error;
            }

            return response.json().then((data) => data.entity);
        }
    };

    editor = {
        /**
         * `editor.on` is an asynchronous method of the `DotCmsClient` class that allows you to react to actions issued by the UVE.
         *
         *  NOTE: This is being used by the development team - This logic is probably varied or moved to another function/object.
         * @param {string} action - The name of the action emitted by UVE.
         * @param {function} callbackFn - The function to execute when the UVE emits the action.
         * @example
         * ```ts
         * client.editor.on('changes', (payload) => {
         *     console.log('Changes detected:', payload);
         * });
         * ```
         */
        on: (action: string, callbackFn: (payload: unknown) => void) => {
            if (!isInsideEditor()) {
                return;
            }

            if (action === 'changes') {
                const messageCallback = (event: MessageEvent) => {
                    if (event.data.name === 'SET_PAGE_DATA') {
                        callbackFn(event.data.payload);
                    }
                };

                window.addEventListener('message', messageCallback);
                this.#listeners.push({ event: 'message', callback: messageCallback, action });
            }
        },
        /**
         * `editor.off` is a synchronous method of the `DotCmsClient` class that allows you to stop listening and reacting to an action issued by UVE.
         *
         * NOTE: This is being used by the development team - This logic is probably varied or moved to another function/object.
         * @param {string} action - The name of the action to stop listening to.
         * @example
         * ```ts
         * client.editor.off('changes');
         * ```
         */
        off: (action: string) => {
            const listenerIndex = this.#listeners.findIndex(
                (listener) => listener.action === action
            );
            if (listenerIndex !== -1) {
                const listener = this.#listeners[listenerIndex];
                window.removeEventListener(listener.event, listener.callback);
                this.#listeners.splice(listenerIndex, 1);
            }
        }
    };

    nav = {
        /**
         * `nav.get` is an asynchronous method of the `DotCmsClient` class that retrieves information about the dotCMS file and folder tree.
         * It takes a `NavApiOptions` object as a parameter (with default values) and returns a Promise that resolves to the response from the DotCMS API.
         *
         * The navigation REST API enables you to retrieve information about the dotCMS file and folder tree through REST API calls.
         * @link https://www.dotcms.com/docs/latest/navigation-rest-api
         * @async
         * @param {NavApiOptions} options - The options for the Nav API call. Defaults to `{ depth: 0, path: '/', languageId: 1 }`.
         * @returns {Promise<unknown>} - A Promise that resolves to the response from the DotCMS API.
         * @throws {Error} - Throws an error if the options are not valid.
         * @example
         * ```ts
         * const client = new DotCmsClient({ dotcmsUrl: 'https://your.dotcms.com', authToken: 'your-auth-token', siteId: 'your-site-id' }});
         * client.nav.get({ path: '/about-us', depth: 2 }).then(response => console.log(response));
         * ```
         */
        get: async (
            options: NavApiOptions = { depth: 0, path: '/', languageId: 1 }
        ): Promise<unknown> => {
            this.validateNavOptions(options);

            // Extract the 'path' from the options and prepare the rest as query parameters
            const { path, ...queryParamsOptions } = options;
            const queryParamsObj: Record<string, string> = {};
            Object.entries(queryParamsOptions).forEach(([key, value]) => {
                if (value !== undefined) {
                    queryParamsObj[key] = String(value);
                }
            });

            const queryParams = new URLSearchParams(queryParamsObj).toString();

            // Format the URL correctly depending on the 'path' value
            const formattedPath = path === '/' ? '/' : `/${path}`;
            const url = `${this.#config.dotcmsUrl}/api/v1/nav${formattedPath}${
                queryParams ? `?${queryParams}` : ''
            }`;

            const response = await fetch(url, this.#requestOptions);

            return response.json();
        }
    };

    /**
     * Initializes the DotCmsClient instance with the provided configuration.
     * If an instance already exists, it returns the existing instance.
     *
     * @param {ClientConfig} config - The configuration object for the DotCMS client.
     * @returns {DotCmsClient} - The initialized DotCmsClient instance.
     * @example
     * ```ts
     * const client = DotCmsClient.init({ dotcmsUrl: 'https://demo.dotcms.com', authToken: 'your-auth-token' });
     * ```
     */
    static init(config: ClientConfig): DotCmsClient {
        if (this.instance) {
            console.warn(
                'DotCmsClient has already been initialized. Please use the instance to interact with the DotCMS API.'
            );
        }

        return this.instance ?? (this.instance = new DotCmsClient(config));
    }

    /**
     * Retrieves the DotCMS URL from the instance configuration.
     *
     * @returns {string} - The DotCMS URL.
     */
    static get dotcmsUrl(): string {
        return (this.instance && this.instance.#config.dotcmsUrl) || '';
    }

    /**
     * Throws an error if the path is not valid.
     *
     * @returns {string} - The authentication token.
     */
    private validatePageOptions(options: PageApiOptions): void {
        if (!options.path) {
            throw new Error("The 'path' parameter is required for the Page API");
        }
    }

    /**
     * Throws an error if the path is not valid.
     *
     *  @returns {string} - The authentication token.
     */
    private validateNavOptions(options: NavApiOptions): void {
        if (!options.path) {
            throw new Error("The 'path' parameter is required for the Nav API");
        }
    }
}
