import { DotCMSClientConfig, RequestOptions } from '../client';
import { ErrorMessages } from '../models';

export type PageApiOptions = {
    /**
     * The path of the page you want to retrieve.
     */
    path: string;

    /**
     * The id of the site you want to interact with. Defaults to the one from the config if not provided.
     */
    siteId?: string;

    /**
     * The mode of the page you want to retrieve. Defaults to the site's default mode if not provided.
     */
    mode?: 'EDIT_MODE' | 'PREVIEW_MODE' | 'LIVE';

    /**
     * The language id of the page you want to retrieve. Defaults to the site's default language if not provided.
     */
    languageId?: number;

    /**
     * The id of the persona for which you want to retrieve the page.
     */
    personaId?: string;

    /**
     * Whether to fire the rules set on the page.
     */
    fireRules?: boolean;

    /**
     * Allows access to related content via the Relationship fields of contentlets on a Page; 0 (default).
     */
    depth?: number;
};

export class PageClient {
    private config: DotCMSClientConfig;
    private requestOptions: RequestOptions;

    constructor(config: DotCMSClientConfig, requestOptions: RequestOptions) {
        this.config = config;
        this.requestOptions = requestOptions;
    }

    /**
     * Retrieves all the elements of a Page in your dotCMS system in JSON format.
     * @param {PageApiOptions} options - The options for the Page API call.
     * @returns {Promise<unknown>} - A Promise that resolves to the response from the DotCMS API.
     * @throws {Error} - Throws an error if the options are not valid.
     */
    async getPage(options: PageApiOptions): Promise<unknown> {
        this.validatePageOptions(options);
        const queryParams = this.buildQueryParams(options);
        const queryString = new URLSearchParams(queryParams).toString();

        const path = options.path.startsWith('/') ? options.path : `/${options.path}`;
        const baseUrl = this.config.dotcmsUrl;

        const url = `${baseUrl}/api/v1/page/json${path}${queryString ? `?${queryString}` : ''}`;
        const response = await fetch(url, this.requestOptions);

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
     * Validates the page options.
     * @param {PageApiOptions} options - The options to validate.
     * @throws {Error} - Throws an error if the path is not valid.
     */
    private validatePageOptions(options: PageApiOptions): void {
        if (!options.path) {
            throw new Error("The 'path' parameter is required for the Page API");
        }
    }

    /**
     * Builds query parameters from the page options.
     * @param {PageApiOptions} options - The options to convert into query parameters.
     * @returns {Record<string, string>} - The query parameters.
     */
    private buildQueryParams(options: PageApiOptions): Record<string, string> {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const { path, ...pageApiOptions } = this.stringifyPageOptions(options);

        const hostId = pageApiOptions['siteId'] ?? this.config.siteId ?? '';
        const personaId = pageApiOptions['personaId'];

        if (pageApiOptions) {
            pageApiOptions['host_id'] = hostId;
            delete pageApiOptions['siteId'];
        }

        if (personaId) {
            pageApiOptions['com.dotmarketing.persona.id'] = personaId;
            delete pageApiOptions['personaId'];
        }

        return pageApiOptions;
    }

    /**
     * Converts page options to string format.
     * @param {PageApiOptions} options - The options to convert.
     * @returns {Record<string, string>} - The stringified options.
     */
    private stringifyPageOptions(options: PageApiOptions): Record<string, string> {
        const { depth, languageId, fireRules, ...restApiOptions } = options;

        return {
            ...restApiOptions,
            depth: String(depth ?? ''),
            language_id: String(languageId ?? ''),
            fireRules: String(fireRules ?? 'false')
        };
    }
}
