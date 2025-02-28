import { buildPageQuery, buildQuery, fetchGraphQL, mapResponseData } from './utils';

import { DotCMSClientConfig, RequestOptions } from '../client';
import { ErrorMessages } from '../models';
import { DotCMSGraphQLPageResponse, DotCMSPageAsset } from '../models/types';

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
    languageId?: number | string;

    /**
     * The id of the persona for which you want to retrieve the page.
     */
    personaId?: string;

    /**
     * Whether to fire the rules set on the page.
     */
    fireRules?: boolean | string;

    /**
     * Allows access to related content via the Relationship fields of contentlets on a Page; 0 (default).
     */
    depth?: 0 | 1 | 2 | 3 | '0' | '1' | '2' | '3';

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

export interface GraphQLPageOptions extends PageRequestParams {
    query: {
        page?: string;
        content?: Record<string, string>;
        nav?: Record<string, string>;
        variables?: Record<string, string>;
    };
}

/**
 * Client for interacting with the DotCMS Page API.
 * Provides methods to retrieve and manipulate pages.
 */
export class PageClient {
    /**
     * Request options including authorization headers.
     * @private
     */
    private requestOptions: RequestOptions;

    /**
     * Base URL for page API endpoints.
     * @private
     */
    private BASE_URL: string;

    /**
     * Site ID for page requests.
     * @private
     */
    private siteId: string;

    /**
     * Creates a new PageClient instance.
     *
     * @param {DotCMSClientConfig} config - Configuration options for the DotCMS client
     * @param {RequestOptions} requestOptions - Options for fetch requests including authorization headers
     * @example
     * ```typescript
     * const pageClient = new PageClient(
     *   {
     *     dotcmsUrl: 'https://demo.dotcms.com',
     *     authToken: 'your-auth-token',
     *     siteId: 'demo.dotcms.com'
     *   },
     *   {
     *     headers: {
     *       Authorization: 'Bearer your-auth-token'
     *     }
     *   }
     * );
     * ```
     */
    constructor(config: DotCMSClientConfig, requestOptions: RequestOptions) {
        this.requestOptions = requestOptions;
        this.BASE_URL = `${config?.dotcmsUrl}/api/v1/page`;
        this.siteId = config.siteId || '';
    }

    /**
     * Retrieves a page from DotCMS using REST API.
     *
     * @param {string} url - The URL of the page to retrieve
     * @param {PageRequestParams} [options] - Options for retrieving the page via REST API
     * @returns {Promise<DotCMSPageAsset>} A Promise that resolves to the page data
     */
    get(url: string, options?: PageRequestParams): Promise<DotCMSPageAsset>;

    /**
     * Retrieves a page from DotCMS using GraphQL.
     *
     * @param {string} url - The URL of the page to retrieve
     * @param {GraphQLPageOptions} options - Options for retrieving the page via GraphQL
     * @returns {Promise<{page: any, content: Record<string, any>, nav: Record<string, any>, errors?: any}>}
     *          A Promise that resolves to the page data with content and navigation
     */
    get(url: string, options?: GraphQLPageOptions): Promise<unknown>;

    /**
     * Implementation of the get method that handles both REST API and GraphQL requests.
     *
     * @param {string} url - The URL of the page to retrieve
     * @param {PageRequestParams | GraphQLPageOptions} [options] - Options for the request
     * @returns {Promise<DotCMSPageAsset | {page: any, content: Record<string, any>, nav: Record<string, any>, errors?: any}>}
     *          A Promise that resolves to the page data
     */
    async get(
        url: string,
        options?: PageRequestParams | GraphQLPageOptions
    ): Promise<DotCMSPageAsset | DotCMSGraphQLPageResponse> {
        const isGraphQLRequest = !!(options as GraphQLPageOptions)?.['query'];

        if (isGraphQLRequest) {
            return this.getPageFromGraphQL(url, options as GraphQLPageOptions);
        }

        return this.getPageFromAPI(url, options);
    }

    /**
     * Retrieves all the elements of a Page in your dotCMS system in JSON format.
     *
     * @param {string} path - The path of the page to retrieve
     * @param {PageRequestParams} params - The options for the Page API call
     * @returns {Promise<unknown>} A Promise that resolves to the page data
     * @throws {Error} Throws an error if the path parameter is missing or if the fetch request fails
     * @example
     * ```typescript
     * // Get a page with default parameters
     * const homePage = await pageClient.get('/');
     *
     * // Get a page with specific language and mode
     * const aboutPage = await pageClient.get('/about-us', {
     *   languageId: 2,
     *   mode: 'PREVIEW_MODE'
     * });
     *
     * // Get a page with persona targeting
     * const productPage = await pageClient.get('/products', {
     *   personaId: 'persona-123',
     *   depth: 2
     * });
     * ```
     */
    private async getPageFromAPI(
        path: string,
        params?: PageRequestParams
    ): Promise<DotCMSPageAsset> {
        if (!path) {
            throw new Error("The 'path' parameter is required for the Page API");
        }

        const pagePath = path.replace(/^\//, '');
        const pageParams = this.mapToBackendParams(params || {});
        const urlParams = new URLSearchParams(pageParams as Record<string, string>).toString();
        const url = `${this.BASE_URL}/json/${pagePath}${urlParams ? `?${urlParams}` : ''}`;

        const response = await fetch(url, this.requestOptions);

        if (!response.ok) {
            const error = {
                status: response.status,
                message: ErrorMessages[response.status] || response.statusText
            };

            throw error;
        }

        return response.json().then((data) => data.entity);
    }

    /**
     * Retrieves a personalized page with associated content and navigation.
     *
     * @param {Object} options - Options for the personalized page request
     * @param {string} options.url - The URL of the page to retrieve
     * @param {string} options.languageId - The language ID for the page content
     * @param {string} options.mode - The rendering mode for the page
     * @param {string} options.pageFragment - GraphQL fragment for page data
     * @param {Record<string, string>} options.content - Content queries to include
     * @param {Record<string, string>} options.nav - Navigation queries to include
     * @returns {Promise<Object>} A Promise that resolves to the personalized page data with content and navigation
     * @example
     * ```typescript
     * // Get a personalized page with content and navigation
     * const personalizedPage = await pageClient.getPersonalizedPage({
     *   url: '/about-us',
     *   languageId: '1',
     *   mode: 'LIVE',
     *   pageFragment: `
     *     fragment PageFields on Page {
     *       title
     *       description
     *       modDate
     *     }
     *   `,
     *   content: {
     *     blogPosts: `
     *       query BlogPosts {
     *         BlogCollection(limit: 3) {
     *           title
     *           urlTitle
     *           publishDate
     *         }
     *       }
     *     `
     *   },
     *   nav: {
     *     mainNav: `
     *       query MainNav {
     *         Nav(identifier: "main-nav") {
     *           title
     *           items {
     *             label
     *             url
     *           }
     *         }
     *       }
     *     `
     *   }
     * });
     * ```
     */
    async getPageFromGraphQL(
        url: string,
        options?: GraphQLPageOptions
    ): Promise<DotCMSGraphQLPageResponse> {
        const { languageId = '1', mode = 'LIVE', query = {} } = options || {};
        const { page = '', content = {}, nav = {}, variables = {} } = query;

        const contentQuery = buildQuery(content);
        const navQuery = buildQuery(nav);
        const completeQuery = buildPageQuery(page, `${contentQuery} ${navQuery}`);

        const requestVariables = {
            url,
            mode,
            languageId,
            ...variables
        };

        const requestHeaders = this.requestOptions.headers as Record<string, string>;
        const requestBody = JSON.stringify({ query: completeQuery, variables: requestVariables });

        try {
            const { data, errors } = await fetchGraphQL({
                body: requestBody,
                headers: requestHeaders
            });
            const pageResponse = data.page;
            const contentResponse = mapResponseData(data, Object.keys(content));
            const navResponse = mapResponseData(data, Object.keys(nav));

            return {
                page: pageResponse,
                content: contentResponse,
                nav: navResponse,
                errors
            };
        } catch (error) {
            console.error('Error fetching page data:', error);
            throw new Error('Failed to retrieve page data');
        }
    }

    /**
     * Maps public API parameters to private API parameters.
     *
     * @param {PageRequestParams} params - The public API parameters
     * @returns {BackendPageParams} The private API parameters
     * @private
     * @example
     * ```typescript
     * // Internal usage
     * const backendParams = this.mapToBackendParams({
     *   siteId: 'demo.dotcms.com',
     *   languageId: 1,
     *   mode: 'LIVE'
     * });
     * // Returns: {
     *   hostId: 'demo.dotcms.com',
     *   language_id: '1',
     *   mode: 'LIVE'
     * }
     * ```
     */
    private mapToBackendParams(params: PageRequestParams): BackendPageParams {
        const backendParams = {
            hostId: params.siteId || this.siteId,
            mode: params.mode,
            language_id: params.languageId ? String(params.languageId) : undefined,
            'com.dotmarketing.persona.id': params.personaId,
            fireRules: params.fireRules ? String(params.fireRules) : undefined,
            depth: params.depth ? String(params.depth) : undefined,
            publishDate: params.publishDate
        };

        // Remove undefined values
        return Object.fromEntries(
            Object.entries(backendParams).filter(([_, value]) => value !== undefined)
        );
    }
}
