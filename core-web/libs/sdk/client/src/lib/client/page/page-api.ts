import { buildPageQuery, buildQuery, fetchGraphQL, mapResponseData } from './utils';

import { graphqlToPageEntity } from '../../utils';
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

    /**
     * The variant name of the page you want to retrieve.
     */
    variantName?: string;
}

type StringifyParam<T> = T extends string | number | boolean ? string : never;

type PageToBackendParamsMapping = {
    siteId: 'hostId';
    languageId: 'language_id';
    personaId: 'com.dotmarketing.persona.id';
};

/**
 * The private parameters for the Page API.
 * @internal
 */
export type BackendPageParams = {
    [K in keyof PageRequestParams as K extends keyof PageToBackendParamsMapping
        ? PageToBackendParamsMapping[K]
        : K]?: StringifyParam<PageRequestParams[K]>;
};

/**
 * The options for the GraphQL Page API.
 * @public
 */
export interface GraphQLPageOptions extends PageRequestParams {
    /**
     * The GraphQL options for the page.
     */
    graphql: {
        page?: string;
        content?: Record<string, string>;
        variables?: Record<string, string>;
        fragments?: string[];
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
     * Site ID for page requests.
     * @private
     */
    private siteId: string;

    /**
     * DotCMS URL for page requests.
     * @private
     */
    private dotcmsUrl: string;

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
        this.siteId = config.siteId || '';
        this.dotcmsUrl = config.dotcmsUrl;
    }

    /**
     * Retrieves a page from DotCMS using either REST API or GraphQL.
     * This method is polymorphic and can handle both REST API and GraphQL requests based on the options provided.
     *
     * @param {string} url - The URL of the page to retrieve
     * @param {PageRequestParams | GraphQLPageOptions} [options] - Options for the request
     * @returns {Promise<DotCMSPageAsset | DotCMSGraphQLPageResponse>} A Promise that resolves to the page data
     *
     * @example Using REST API with options
     * ```typescript
     * const page = await pageClient.get('/about-us', {
     *   mode: 'PREVIEW_MODE',
     *   languageId: 1,
     *   siteId: 'demo.dotcms.com'
     * });
     * ```
     *
     * @example Using GraphQL
     * ```typescript
     * const page = await pageClient.get('/index', {
     *      languageId: '1',
     *      mode: 'LIVE',
     *      graphql: {
     *          page: `
     *              containers {
     *                  containerContentlets {
     *                      contentlets {
     *                          ... on 	Banner {
     *                              ...bannerFragment
     *                          }
     *                      }
     *                  }
     *              `,
     *              content: {
     *                  blogPosts: `
     *                      BlogCollection(limit: 3) {
     *                          ...blogFragment
     *                      }
     *                  `,
     *              },
     *              fragments: [
     *                  `
     *                      fragment bannerFragment on Banner {
     *                          caption
     *                      }
     *                  `,
     *                  `
     *                      fragment blogFragment on Blog {
     *                          title
     *                          urlTitle
     *                      }
     *                  `
     *              ]
     *          }
     *      });
     *```
     */
    get(url: string, options?: PageRequestParams): Promise<DotCMSPageAsset>;
    get(url: string, options?: GraphQLPageOptions): Promise<DotCMSGraphQLPageResponse>;
    get(
        url: string,
        options?: PageRequestParams | GraphQLPageOptions
    ): Promise<DotCMSPageAsset | DotCMSGraphQLPageResponse> {
        if (!options) {
            return this.#getPageFromAPI(url);
        }

        if (this.#isGraphQLRequest(options)) {
            return this.#getPageFromGraphQL(url, options);
        }

        return this.#getPageFromAPI(url, options);
    }

    /**
     * Determines if the provided options object is for a GraphQL request.
     *
     * @param {PageRequestParams | GraphQLPageOptions} options - The options object to check
     * @returns {boolean} True if the options are for a GraphQL request, false otherwise
     * @internal
     */
    #isGraphQLRequest(
        options: PageRequestParams | GraphQLPageOptions
    ): options is GraphQLPageOptions {
        return 'graphql' in options;
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
    async #getPageFromAPI(path: string, params?: PageRequestParams): Promise<DotCMSPageAsset> {
        if (!path) {
            throw new Error("The 'path' parameter is required for the Page API");
        }

        // If the siteId is not provided, use the one from the config
        const completedParams = {
            ...(params ?? {}),
            siteId: params?.siteId || this.siteId
        };

        // Map the public parameters to the one used by the API
        const normalizedParams = this.#mapToBackendParams(completedParams || {});

        // Build the query params
        const queryParams = new URLSearchParams(normalizedParams).toString();

        // If the path starts with a slash, remove it to avoid double slashes in the final URL
        // Because the page path is part of api url path
        const pagePath = path.startsWith('/') ? path.slice(1) : path;
        const url = `${this.dotcmsUrl}/api/v1/page/json/${pagePath}?${queryParams}`;
        const response = await fetch(url, this.requestOptions);

        if (!response.ok) {
            const error = {
                status: response.status,
                message: ErrorMessages[response.status] || response.statusText
            };

            throw error;
        }

        return response.json().then<DotCMSPageAsset>((data) => ({
            ...data.entity,
            params: completedParams // We retrieve the params from the API response, to make the same fetch on UVE
        }));
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
     * const personalizedPage = await pageClient.getPageFromGraphQL({
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
     *     blogs: `
     *       search(query: "+contentType: blog", limit: 3) {
     *         title
     *         ...on Blog {
     *             author {
     *                 title
     *             }
     *         }
                                }
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
    async #getPageFromGraphQL(
        url: string,
        options?: GraphQLPageOptions
    ): Promise<DotCMSGraphQLPageResponse> {
        const {
            languageId = '1',
            mode = 'LIVE',
            siteId = this.siteId,
            fireRules = false,
            personaId,
            publishDate,
            graphql = {}
        } = options || {};
        const { page, content = {}, variables, fragments } = graphql;

        const contentQuery = buildQuery(content);
        const completeQuery = buildPageQuery({
            page,
            fragments,
            additionalQueries: contentQuery
        });

        const requestVariables: Record<string, unknown> = {
            url,
            mode,
            languageId,
            personaId,
            fireRules,
            publishDate,
            siteId,
            ...variables
        };

        const requestHeaders = this.requestOptions.headers as Record<string, string>;
        const requestBody = JSON.stringify({ query: completeQuery, variables: requestVariables });

        try {
            const { data, errors } = await fetchGraphQL({
                baseURL: this.dotcmsUrl,
                body: requestBody,
                headers: requestHeaders
            });

            if (errors) {
                errors.forEach((error: { message: string }) => {
                    throw new Error(error.message);
                });
            }

            const pageResponse = graphqlToPageEntity(data);

            if (!pageResponse) {
                throw new Error('No page data found');
            }

            const contentResponse = mapResponseData(data, Object.keys(content));

            return {
                page: pageResponse,
                content: contentResponse,
                graphql: {
                    query: completeQuery,
                    variables: requestVariables
                }
            };
        } catch (error) {
            const errorMessage = {
                error,
                message: 'Failed to retrieve page data',
                graphql: {
                    query: completeQuery,
                    variables: requestVariables
                }
            };

            throw errorMessage;
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
    #mapToBackendParams(params: PageRequestParams): BackendPageParams {
        const backendParams = {
            hostId: params.siteId,
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
