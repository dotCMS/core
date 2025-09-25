import { consola } from 'consola';

import {
    DotCMSClientConfig,
    DotCMSPageRequestParams,
    DotCMSPageResponse,
    DotCMSExtendedPageResponse,
    DotCMSComposedPageResponse,
    DotHttpClient,
    DotRequestOptions
} from '@dotcms/types';

import { buildPageQuery, buildQuery, fetchGraphQL, mapContentResponse } from './utils';

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
    private requestOptions: DotRequestOptions;

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
     * HTTP client for making requests.
     * @private
     */
    private httpClient: DotHttpClient;

    /**
     * Creates a new PageClient instance.
     *
     * @param {DotCMSClientConfig} config - Configuration options for the DotCMS client
     * @param {DotRequestOptions} requestOptions - Options for fetch requests including authorization headers
     * @param {DotHttpClient} httpClient - HTTP client for making requests
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
     *   },
     *   httpClient
     * );
     * ```
     */
    constructor(
        config: DotCMSClientConfig,
        requestOptions: DotRequestOptions,
        httpClient: DotHttpClient
    ) {
        this.requestOptions = requestOptions;
        this.siteId = config.siteId || '';
        this.dotcmsUrl = config.dotcmsUrl;
        this.httpClient = httpClient;
    }

    /**
     * Retrieves a page from DotCMS using GraphQL.
     *
     * @param {string} url - The URL of the page to retrieve
     * @param {DotCMSPageRequestParams} [options] - Options for the request
     * @template T - The type of the page and content, defaults to DotCMSBasicPage and Record<string, unknown> | unknown
     * @returns {Promise<DotCMSComposedPageResponse<T>>} A Promise that resolves to the page data
     *
     * @example Using GraphQL
     * ```typescript
     * const page = await pageClient.get<{ page: MyPageWithBanners; content: { blogPosts: { blogTitle: string } } }>(
     *     '/index',
     *     {
     *         languageId: '1',
     *         mode: 'LIVE',
     *         graphql: {
     *             page: `
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
     * ```
     */
    async get<T extends DotCMSExtendedPageResponse = DotCMSPageResponse>(
        url: string,
        options?: DotCMSPageRequestParams
    ): Promise<DotCMSComposedPageResponse<T>> {
        const {
            languageId = '1',
            mode = 'LIVE',
            siteId = this.siteId,
            fireRules = false,
            personaId,
            publishDate,
            variantName,
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
            // The url is expected to have a leading slash to comply on VanityURL Matching, some frameworks like Angular will not add the leading slash
            url: url.startsWith('/') ? url : `/${url}`,
            mode,
            languageId,
            personaId,
            fireRules,
            publishDate,
            siteId,
            variantName,
            ...variables
        };

        const requestHeaders = this.requestOptions.headers;
        const requestBody = JSON.stringify({ query: completeQuery, variables: requestVariables });

        try {
            const response = await fetchGraphQL({
                baseURL: this.dotcmsUrl,
                body: requestBody,
                headers: requestHeaders,
                httpClient: this.httpClient
            });
            // The GQL endpoint can return errors and data, we need to handle both
            if (response.errors) {
                response.errors.forEach((error: { message: string }) => {
                    consola.error('[DotCMS GraphQL Error]: ', error.message);
                });
            }

            const pageResponse = graphqlToPageEntity(response.data.page);

            if (!pageResponse) {
                throw new Error(
                    'No page data found. Please check the page URL and the GraphQL query.'
                );
            }

            const contentResponse = mapContentResponse(response.data, Object.keys(content));

            return {
                pageAsset: pageResponse,
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

// async get<T extends DotCMSExtendedPageResponse = DotCMSPageResponse>(
//     url: string,
//     options?: DotCMSPageRequestParams
// ): Promise<DotCMSComposedPageResponse<T>> {
//     const {
//         languageId = '1',
//         mode = 'LIVE',
//         siteId = this.siteId,
//         fireRules = false,
//         personaId,
//         publishDate,
//         variantName,
//         graphql = {}
//     } = options || {};
//     const { page, content = {}, variables, fragments } = graphql;

//     const contentQuery = buildQuery(content);
//     const completeQuery = buildPageQuery({
//         page,
//         fragments,
//         additionalQueries: contentQuery
//     });

//     const requestVariables: Record<string, unknown> = {
//         // The url is expected to have a leading slash to comply on VanityURL Matching, some frameworks like Angular will not add the leading slash
//         url: url.startsWith('/') ? url : `/${url}`,
//         mode,
//         languageId,
//         personaId,
//         fireRules,
//         publishDate,
//         siteId,
//         variantName,
//         ...variables
//     };

//     const requestHeaders = this.requestOptions.headers as Record<string, string>;
//     const requestBody = JSON.stringify({ query: completeQuery, variables: requestVariables });

//     try {
//         const { data, errors } = await fetchGraphQL({
//             baseURL: this.dotcmsUrl,
//             body: requestBody,
//             headers: requestHeaders
//         });

//         if (errors) {
//             errors.forEach((error: { message: string }) => {
//                 consola.error('[DotCMS GraphQL Error]: ', error.message);
//             });
//         }

//         const pageResponse = graphqlToPageEntity(data);

//         if (!pageResponse) {
//             throw new Error('No page data found');
//         }

//         const contentResponse = mapResponseData(data, Object.keys(content));

//         return {
//             pageAsset: pageResponse,
//             content: contentResponse,
//             graphql: {
//                 query: completeQuery,
//                 variables: requestVariables
//             }
//         };
//     } catch (error) {
//         const errorMessage = {
//             error,
//             message: 'Failed to retrieve page data',
//             graphql: {
//                 query: completeQuery,
//                 variables: requestVariables
//             }
//         };

//         throw errorMessage;
//     }
// }
