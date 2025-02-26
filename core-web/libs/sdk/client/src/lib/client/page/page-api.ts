import {
    buildPageQuery,
    buildQueries,
    fetchGraphQL,
    mapResponseData
} from './utils';

import { DotCMSClientConfig, RequestOptions } from '../client';
import { ErrorMessages } from '../models';

/**
 * The parameters for the Page API.
 * @public
 */
export interface PageRequestParams {
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
    depth?: 0 | 1 | 2 | 3;

    /**
     * The publish date of the page you want to retrieve.
     */
    publishDate?: string;
}

/**
 * The private parameters for the Page API.
 * @internal
 */
export interface BackendPageParams {
    /**
     * The id of the site you want to interact with.
     */
    hostId?: string;

    /**
     * The mode of the page you want to retrieve.
     */
    mode?: string;

    /**
     * The id of the persona for which you want to retrieve the page.
     */
    'com.dotmarketing.persona.id'?: string;

    /**
     * The language id of the page you want to retrieve.
     */
    language_id?: string;

    /**
     * Whether to fire the rules set on the page.
     */
    fireRules?: string;

    /**
     * The depth of the page you want to retrieve.
     */
    depth?: string;

    /**
     * The publish date of the page you want to retrieve.
     */
    publishDate?: string;
}

export class PageClient {
    private requestOptions: RequestOptions;
    private BASE_URL: string;
    private siteId: string;

    constructor(config: DotCMSClientConfig, requestOptions: RequestOptions) {
        this.requestOptions = requestOptions;
        this.BASE_URL = `${config?.dotcmsUrl}/api/v1/page`;
        this.siteId = config.siteId || '';
    }

    /**
     * Retrieves all the elements of a Page in your dotCMS system in JSON format.
     * @param {PageRequestParams} options - The options for the Page API call.
     * @returns {Promise<unknown>} - A Promise that resolves to the response from the DotCMS API.
     * @throws {Error} - Throws an error if the options are not valid.
     */
    async get(path: string, params: PageRequestParams): Promise<unknown> {
        if (!path) {
            throw new Error("The 'path' parameter is required for the Page API");
        }

        const pageParams = this.mapToBackendParams(params) as Record<string, string>;
        const urlParams = new URLSearchParams(pageParams).toString();
        const url = `${this.BASE_URL}/json/${path}${urlParams ? `?${urlParams}` : ''}`;

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

    async getPersonalizedPage({
        url,
        nav,
        content,
        pageFragment,
        languageId = '1',
        mode = 'LIVE',
    }: {
        url: string;
        languageId: string;
        mode: string;
        pageFragment: string;
        content: Record<string, string>;
        nav: Record<string, string>;
    }) {
        const contentQueries = buildQueries(content);
        const navQueries = buildQueries(nav);

        const query = buildPageQuery({
            pageFragment,
            contentQueries,
            navQueries
        });

        const variables = {
            url,
            mode,
            languageId
        };

        const body = JSON.stringify({ query, variables });
        const data = await fetchGraphQL({ body, requestOptions: this.requestOptions });

        const page = data.page;
        const contentResponse = mapResponseData(data, Object.keys(content || {}));
        const navResponse = mapResponseData(data, Object.keys(nav || {}));

        /**
         * const { template, viewAs, layout, container, ...restPage } = page;
         * return {
         *  pageAsset {
         *      page: restPage,
         *      tempalte
         *      viewAs,
         *      layout
         *      container
         *  }
         * }
         *
         */
        return {
            page,
            content: contentResponse,
            nav: navResponse,
            query
        };
    }

    /**
     * Maps public API parameters to private API parameters.
     * @param {PageRequestParams} params - The public API parameters.
     * @returns {BackendPageParams} - The private API parameters.
     */
    private mapToBackendParams(params: PageRequestParams): BackendPageParams {
        return {
            hostId: params.siteId || this.siteId,
            mode: params.mode,
            language_id: params.languageId ? String(params.languageId) : '',
            'com.dotmarketing.persona.id': params.personaId,
            fireRules: params.fireRules ? String(params.fireRules) : '',
            depth: params.depth ? String(params.depth) : '',
            publishDate: params.publishDate
        };
    }
}
