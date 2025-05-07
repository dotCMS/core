import {
    DotCMSClientConfig,
    DotCMSComposedPageResponse,
    DotCMSExtendedPageResponse,
    DotCMSPageResponse,
    DotCMSPageRequestParams,
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

    get<T extends DotCMSExtendedPageResponse = DotCMSPageResponse>(
        url: string,
        options?: DotCMSPageRequestParams
    ): Promise<DotCMSComposedPageResponse<T>> {
        return this.#getPageFromGraphQL(url, options) as Promise<DotCMSComposedPageResponse<T>>;
    }

    /**
     * Private implementation method that fetches page data using GraphQL.
     * This method is used internally by the public get() method.
     *
     * @private
     * @param {string} url - The URL of the page to retrieve
     * @param {DotCMSPageRequestParams} [options] - Options including languageId, mode, and GraphQL parameters
     * @returns {Promise<DotCMSPageResponse>} A Promise that resolves to the page data
     */
    async #getPageFromGraphQL(
        url: string,
        options?: DotCMSPageRequestParams
    ): Promise<DotCMSPageResponse> {
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
