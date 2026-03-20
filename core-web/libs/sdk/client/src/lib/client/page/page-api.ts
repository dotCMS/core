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
import { BaseApiClient } from '../base/api/base-api';

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

            // 1. Log all GraphQL errors
            if (response.errors?.length) {
                response.errors.forEach((error: { message: string }) => {
                    consola.error('[DotCMS GraphQL Error]: ', error.message);
                });
            }

            // 2. BAD QUERY — data is null/undefined means the entire query failed
            //    (syntax error, unknown type, validation error)
            //    Must check BEFORE accessing response.data.page
            if (!response.data) {
                const firstError = response.errors?.[0];

                throw new DotErrorPage(
                    firstError?.message ?? 'GraphQL query failed',
                    400,
                    'BAD_REQUEST',
                    new DotHttpError({
                        status: 400,
                        statusText: 'Bad Request',
                        message: firstError?.message ?? 'GraphQL query failed',
                        data: response.errors
                    }),
                    { query: completeQuery, variables: requestVariables }
                );
            }

            // 3. STRUCTURED ERRORS — check extensions.code for NOT_FOUND, PERMISSION_DENIED, etc.
            if (response.errors?.length) {
                const structuredError = response.errors.find(
                    (error: { extensions?: { code?: string } }) => error.extensions?.code
                );

                if (structuredError) {
                    const code = structuredError.extensions.code!;
                    const status =
                        structuredError.extensions.status ??
                        (code === 'NOT_FOUND' ? 404 : code === 'PERMISSION_DENIED' ? 403 : 400);

                    throw new DotErrorPage(
                        structuredError.message,
                        status,
                        code,
                        new DotHttpError({
                            status,
                            statusText: code,
                            message: structuredError.message,
                            data: response.errors
                        }),
                        { query: completeQuery, variables: requestVariables }
                    );
                }
            }

            // 4. Transform and check page — null page with no structured error = 404
            const pageResponse = graphqlToPageEntity(response.data.page);

            if (!pageResponse) {
                throw new DotErrorPage(
                    `Page ${url} not found`,
                    404,
                    'NOT_FOUND',
                    new DotHttpError({
                        status: 404,
                        statusText: 'Not Found',
                        message: `Page ${url} not found`,
                        data: response.errors
                    }),
                    { query: completeQuery, variables: requestVariables }
                );
            }

            // 5. Build response — include any non-fatal errors for consumers to inspect
            const contentResponse = mapContentResponse(response.data, Object.keys(content));

            return {
                pageAsset: pageResponse,
                content: contentResponse,
                graphql: {
                    query: completeQuery,
                    variables: requestVariables
                },
                errors: response.errors ?? undefined
            };
        } catch (error) {
            if (error instanceof DotErrorPage) {
                throw error;
            }

            if (error instanceof DotHttpError) {
                throw new DotErrorPage(
                    `Page request failed for URL '${url}': ${error.message}`,
                    error.status,
                    'UNKNOWN',
                    error,
                    { query: completeQuery, variables: requestVariables }
                );
            }

            throw new DotErrorPage(
                `Page request failed for URL '${url}': ${error instanceof Error ? error.message : 'Unknown error'}`,
                500,
                'UNKNOWN',
                undefined,
                { query: completeQuery, variables: requestVariables }
            );
        }
    }
}
