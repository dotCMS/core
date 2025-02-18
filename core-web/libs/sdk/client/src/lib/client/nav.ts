import { ClientConfig, ClientOptions } from './client';

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

export class NavClient {
    #config: ClientConfig;
    #requestOptions: ClientOptions;

    constructor(config: ClientConfig, requestOptions: ClientOptions) {
        this.#config = config;
        this.#requestOptions = requestOptions;
    }
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
    async get(options: NavApiOptions = { depth: 0, path: '/', languageId: 1 }): Promise<unknown> {
        // this.validateNavOptions(options);a

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
}
