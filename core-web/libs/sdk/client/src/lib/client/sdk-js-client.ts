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
     * The id of the site you want to interact with.
     *
     * @description to get the site id, go to the site you want to interact with and copy the id from the History tab
     *
     * @type {string}
     * @required
     */
    siteId?: string;
    /**
     * The authentication token to use for the requests. If not provided, it will fallback to default site.
     *
     * @description you can get the auth token from our UI {@link https://www.dotcms.com/docs/latest/rest-api-authentication#creating-an-api-token-in-the-ui}
     *
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

type PageApiOptions = {
    /**
     * The path of the page you want to retrieve.
     *
     * @type {string}
     */
    path: string;
    /**
     * The id of the site you want to interact with. If not provided, the one from the config will be used.
     *
     * @type {number}
     */
    siteId?: string;
    /**
     * The mode of the page you want to retrieve. If not provided will use the default mode of the site.
     *
     * @type {number}
     */
    mode?: string;
    /**
     * The language id of the page you want to retrieve. If not provided will use the default language of the site.
     *
     * @type {number}
     */
    language_id?: number;
    /**
     * The id of the persona you want to retrieve the page for.
     *
     * @type {string}
     */
    personaId?: string;
    /**
     * If you want to fire the rules set on the page
     *
     * @type {boolean}
     */
    fireRules?: boolean;
    /**
     * Allows access to related content via the Relationship fields of contentlets on a Page; 0 (default)
     *
     * @type {number}
     */
    depth?: number;
};

type NavApiOptions = {
    /**
     * The root path to begin traversing the folder tree.
     *
     * @example
     * `/api/v1/nav/` starts from the root of the site
     * @example
     * `/about-us` starts from the "About Us" folder
     *
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
     *
     * @type {number}
     */
    depth?: number;
    /**
     * The language ID of content to return.
     * @example
     * `1` (or unspecified) returns content in the default language of the site.
     *
     * @link https://www.dotcms.com/docs/latest/system-language-properties#DefaultLanguage
     * @link https://www.dotcms.com/docs/latest/adding-and-editing-languages#LanguageID
     * @type {number}
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
 * It requires a configuration object on instantiation, which includes the DotCMS URL, site ID, and authentication token.
 *
 * @class DotCmsClient
 *
 * @property {ClientConfig} config - The configuration object for the DotCMS client.
 *
 * @method constructor(config: ClientConfig) - Constructs a new instance of the DotCmsClient class.
 *
 * @method page.get(options: PageApiOptions): Promise<unknown> - Retrieves all the elements of any Page in your dotCMS system in JSON format.
 *
 * @method nav.get(options: NavApiOptions = { depth: 0, path: '/', languageId: 1 }): Promise<unknown> - Retrieves information about the dotCMS file and folder tree.
 *
 */
export class DotCmsClient {
    private config: ClientConfig;
    private requestOptions!: ClientOptions;
    private listeners: DotcmsClientListener[] = [];

    content: Content;

    constructor(
        config: ClientConfig = { dotcmsUrl: '', authToken: '', requestOptions: {}, siteId: '' }
    ) {
        if (!config.dotcmsUrl) {
            throw new Error("Invalid configuration - 'dotcmsUrl' is required");
        }

        const dotcmsHost = getHostURL(config.dotcmsUrl);

        if (!dotcmsHost) {
            throw new Error("Invalid configuration - 'dotcmsUrl' must be a valid URL");
        }

        if (!config.authToken) {
            throw new Error("Invalid configuration - 'authToken' is required");
        }

        this.config = {
            ...config,
            dotcmsUrl: dotcmsHost.origin
        };

        this.requestOptions = {
            ...this.config.requestOptions,
            headers: {
                Authorization: `Bearer ${this.config.authToken}`,
                ...this.config.requestOptions?.headers
            }
        };

        this.content = new Content(this.requestOptions, this.config.dotcmsUrl);
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

            const queryHostId = options.siteId ?? this.config.siteId ?? '';

            if (queryHostId) {
                queryParamsObj['host_id'] = queryHostId;
            }

            const queryParams = new URLSearchParams(queryParamsObj).toString();

            const formattedPath = options.path.startsWith('/') ? options.path : `/${options.path}`;
            const url = `${this.config.dotcmsUrl}/api/v1/page/json${formattedPath}${
                queryParams ? `?${queryParams}` : ''
            }`;

            const response = await fetch(url, this.requestOptions);
            if (!response.ok) {
                const error = {
                    status: response.status,
                    message: ErrorMessages[response.status] || response.statusText
                };

                console.error(error);
                throw error;
            }

            return response.json();
        }
    };

    editor = {
        /**
         * `editor.on` is an asynchronous method of the `DotCmsClient` class that allows you to react to actions issued by the UVE.
         *
         *  NOTE: This is being used by the development team - This logic is probably varied or moved to another function/object.
         * @param action - The name of the name emitted by UVE
         * @param callbackFn - The function to execute when the UVE emits the action
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
                this.listeners.push({ event: 'message', callback: messageCallback, action });
            }
        },
        /**
         * `editor.off` is an synchronous method of the `DotCmsClient` class that allows you to stop listening and reacting to an action issued by UVE.
         *
         *  NOTE: This is being used by the development team - This logic is probably varied or moved to another function/object.
         * @param action
         */
        off: (action: string) => {
            const listenerIndex = this.listeners.findIndex(
                (listener) => listener.action === action
            );
            if (listenerIndex !== -1) {
                const listener = this.listeners[listenerIndex];
                window.removeEventListener(listener.event, listener.callback);
                this.listeners.splice(listenerIndex, 1);
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
            const url = `${this.config.dotcmsUrl}/api/v1/nav${formattedPath}${
                queryParams ? `?${queryParams}` : ''
            }`;

            const response = await fetch(url, this.requestOptions);

            return response.json();
        }
    };

    private validatePageOptions(options: PageApiOptions): void {
        if (!options.path) {
            throw new Error("The 'path' parameter is required for the Page API");
        }
    }

    private validateNavOptions(options: NavApiOptions): void {
        if (!options.path) {
            throw new Error("The 'path' parameter is required for the Nav API");
        }
    }
}

/**
 * `dotcmsClient` is an object that provides a method to initialize the DotCMS SDK client.
 * It has a single method `init` which takes a configuration object and returns an instance of the `DotCmsClient` class.
 *
 * @namespace dotcmsClient
 *
 * @method init(config: ClientConfig): DotCmsClient - Initializes the SDK client.
 */
export const dotcmsClient = {
    /**
     * `init` is a method of the `dotcmsClient` object that initializes the SDK client.
     * It takes a configuration object as a parameter and returns an instance of the `DotCmsClient` class.
     *
     * @method init
     * @param {ClientConfig} config - The configuration object for the DotCMS client.
     * @returns {DotCmsClient} - An instance of the {@link DotCmsClient} class.
     */
    init: (config: ClientConfig): DotCmsClient => {
        return new DotCmsClient(config);
    }
};
