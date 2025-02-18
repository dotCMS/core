import { ClientConfig, ClientOptions } from './client';
import { ErrorMessages } from './models';

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
    mode?: 'EDIT_MODE' | 'PREVIEW_MODE' | 'LIVE';
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

export class PageClient {
    #config: ClientConfig;
    #requestOptions!: ClientOptions;

    constructor(config: ClientConfig, requestOptions: ClientOptions) {
        this.#config = config;
        this.#requestOptions = requestOptions;
    }

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
    async get(options: PageApiOptions) {
        this.validatePageOptions(options);
        const queryParams = this.getQueryParams(options);
        const qpString = new URLSearchParams(queryParams).toString();

        const path = options.path.startsWith('/') ? options.path : `/${options.path}`;
        const baseUrl = this.#config.dotcmsUrl;

        const url = `${baseUrl}/api/v1/page/json${path}${qpString ? `?${qpString}` : ''}`;
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

    private getQueryParams(options: PageApiOptions): Record<string, string> {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const { path, ...pageApiOptions }: Record<string, string> =
            this.getStringifyPageOptions(options);

        const queryHostId = pageApiOptions['siteId'] ?? this.#config.siteId ?? '';
        const personaId = pageApiOptions['personaId'];

        if (pageApiOptions) {
            pageApiOptions['host_id'] = queryHostId;
            delete pageApiOptions['siteId'];
        }

        if (personaId) {
            pageApiOptions['com.dotmarketing.persona.id'] = personaId;
            delete pageApiOptions['personaId'];
        }

        return pageApiOptions;
    }

    private getStringifyPageOptions(options: PageApiOptions): Record<string, string> {
        const { depth, language_id, fireRules, ...restApiOptions } = options;

        return {
            ...restApiOptions,
            depth: String(depth ?? ''),
            language_id: String(language_id ?? ''),
            fireRules: String(fireRules ?? 'false')
        };
    }
}
