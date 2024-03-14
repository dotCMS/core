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
     * @type {Omit<RequestInit, 'body' | 'method'>}
     * @optional
     */
    requestOptions?: Omit<RequestInit, 'body' | 'method'>;
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
export declare class DotCmsClient {
    private config;
    private requestOptions;
    constructor(config?: ClientConfig);
    page: {
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
        get: (options: PageApiOptions) => Promise<unknown>;
    };
    nav: {
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
        get: (options?: NavApiOptions) => Promise<unknown>;
    };
    private validatePageOptions;
    private validateNavOptions;
}
/**
 * `dotcmsClient` is an object that provides a method to initialize the DotCMS SDK client.
 * It has a single method `init` which takes a configuration object and returns an instance of the `DotCmsClient` class.
 *
 * @namespace dotcmsClient
 *
 * @method init(config: ClientConfig): DotCmsClient - Initializes the SDK client.
 */
export declare const dotcmsClient: {
    /**
     * `init` is a method of the `dotcmsClient` object that initializes the SDK client.
     * It takes a configuration object as a parameter and returns an instance of the `DotCmsClient` class.
     *
     * @method init
     * @param {ClientConfig} config - The configuration object for the DotCMS client.
     * @returns {DotCmsClient} - An instance of the {@link DotCmsClient} class.
     */
    init: (config: ClientConfig) => DotCmsClient;
};
export {};
