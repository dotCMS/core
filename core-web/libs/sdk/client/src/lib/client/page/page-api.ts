import { consola } from 'consola';

import {
    DotCMSClientConfig,
    DotCMSPageRequestParams,
    DotCMSPageResponse,
    DotCMSExtendedPageResponse,
    DotCMSComposedPageResponse,
    DotHttpClient,
    DotRequestOptions,
    DotHttpError,
    DotErrorPage
} from '@dotcms/types';

import { buildPageQuery, buildQuery, fetchGraphQL, mapContentResponse } from './utils';

import { graphqlToPageEntity } from '../../utils';
import { BaseApiClient } from '../base/base-api';

/**
 * Client for interacting with the DotCMS Page API.
 * Provides methods to retrieve and manipulate pages.
 */
export class PageClient extends BaseApiClient {
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
        super(config, requestOptions, httpClient);
    }

    /**
     * Retrieves a page from DotCMS using GraphQL.
     *
     * @param {string} url - The URL of the page to retrieve
     * @param {DotCMSPageRequestParams} [options] - Options for the request
     * @template T - The type of the page and content, defaults to DotCMSBasicPage and Record<string, unknown> | unknown
     * @returns {Promise<DotCMSComposedPageResponse<T>>} A Promise that resolves to the page data
     * @throws {DotErrorPage} - Throws a page-specific error if the request fails or page is not found
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

                const pageError = response.errors.find((error: { message: string }) =>
                    error.message.includes('DotPage')
                );

                if (pageError) {
                    // Throw HTTP error - will be caught and wrapped in DotErrorPage below
                    throw new DotHttpError({
                        status: 400,
                        statusText: 'Bad Request',
                        message: `GraphQL query failed for URL '${url}': ${pageError.message}`,
                        data: response.errors
                    });
                }
            }

            const pageResponse = graphqlToPageEntity(response.data.page);

            if (!pageResponse) {
                // Throw HTTP error - will be caught and wrapped in DotErrorPage below
                throw new DotHttpError({
                    status: 404,
                    statusText: 'Not Found',
                    message: `Page ${url} not found. Check the page URL and permissions.`,
                    data: response.errors
                });
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
            // Handle DotHttpError instances
            if (error instanceof DotHttpError) {
                throw new DotErrorPage(
                    `Page request failed for URL '${url}': ${error.message}`,
                    error,
                    {
                        query: completeQuery,
                        variables: requestVariables
                    }
                );
            }

            // Handle other errors (GraphQL errors, validation errors, etc.)
            throw new DotErrorPage(
                `Page request failed for URL '${url}': ${error instanceof Error ? error.message : 'Unknown error'}`,
                undefined,
                {
                    query: completeQuery,
                    variables: requestVariables
                }
            );
        }
    }
}
