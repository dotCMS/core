import {
    DotCMSClientConfig,
    DotCMSGraphQLPageResponse,
    PageRequestParams,
    RequestOptions
} from '@dotcms/types';

import { buildPageQuery, buildQuery, fetchGraphQL, mapResponseData } from './utils';

import { graphqlToPageEntity } from '../../utils';

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
     * Retrieves a page from DotCMS using GraphQL.
     *
     * @param {string} url - The URL of the page to retrieve
     * @param {GraphQLPageOptions} [options] - Options for the request
     * @returns {Promise<DotCMSGraphQLPageResponse>} A Promise that resolves to the page data
     *
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

    get<T extends DotCMSGraphQLPageResponse>(url: string, options?: PageRequestParams): Promise<T> {
        return this.#getPageFromGraphQL(url, options) as Promise<T>;
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
        options?: PageRequestParams
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
}
