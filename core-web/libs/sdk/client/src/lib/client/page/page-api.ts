import { consola } from 'consola';

import {
    DotCMSClientConfig,
    DotCMSComposedPageResponse,
    DotCMSExtendedPageResponse,
    DotCMSPageRequestParams,
    DotCMSPageResponse,
    DotErrorPage,
    DotHttpClient,
    DotHttpError,
    DotRequestOptions,
    UVE_MODE
} from '@dotcms/types';

import {
    buildPageQuery,
    buildQuery,
    fetchGraphQL,
    mapContentResponse,
    removeUndefinedValues
} from './utils';

import { graphqlToPageEntity } from '../../utils';
import { BaseApiClient } from '../base/api/base-api';

function logVerboseError(
    url: string,
    message: string,
    details: { status?: number; code?: string; variables: Record<string, unknown> }
) {
    const statusLine =
        details.status !== undefined ? `\n  status: ${details.status} | code: ${details.code}` : '';
    const variables = JSON.stringify(details.variables, null, 2).replace(/\n/g, '\n  ');
    consola.error(
        `[DotCMS GraphQL Error] ${url}: ${message}${statusLine}\n\n  variables:\n  ${variables}\n\n  (full query available at error.graphql.query)`
    );
}

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

        const verbose = this.config.logLevel === 'verbose';
        const contentQuery = buildQuery(content);
        const completeQuery = buildPageQuery({
            page,
            fragments,
            additionalQueries: contentQuery,
            verbose
        });

        const newURL = url.startsWith('/') ? url : `/${url}`;
        const requestVariables: Record<string, unknown> = removeUndefinedValues({
            url: newURL,
            mode: UVE_MODE[mode], // Translate the UVE_MODE key ('EDIT' | 'PREVIEW' | ...) to the value the backend PageMode enum expects ('EDIT_MODE' | 'PREVIEW_MODE' | ...)
            languageId,
            personaId,
            fireRules,
            publishDate,
            siteId,
            variantName,
            ...variables
        });

        const requestHeaders = this.requestOptions.headers;
        const requestBody = JSON.stringify({ query: completeQuery, variables: requestVariables });

        try {
            const response = await fetchGraphQL({
                baseURL: this.dotcmsUrl,
                body: requestBody,
                headers: requestHeaders,
                httpClient: this.httpClient
            });

            // 1. Log unstructured GraphQL errors (structured ones are logged with enriched messages below)
            if (response.errors?.length) {
                response.errors
                    .filter((error: { extensions?: { code?: string } }) => !error.extensions?.code)
                    .forEach((error: { message: string }) => {
                        if (verbose) {
                            logVerboseError(newURL, error.message, {
                                variables: requestVariables
                            });
                        } else {
                            consola.error(`[DotCMS GraphQL Error] ${newURL}: `, error.message);
                        }
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
            //    Only fatal when the page itself failed (data.page is null/undefined).
            //    If data.page exists, partial errors (e.g. secondary content) surface via errors[].
            if (response.errors?.length && !response.data.page) {
                const structuredError = response.errors.find(
                    (error: { extensions?: { code?: string } }) => error.extensions?.code
                );

                if (structuredError) {
                    const code = structuredError.extensions?.code;
                    const status =
                        structuredError.extensions?.status ??
                        (code === 'NOT_FOUND' ? 404 : code === 'PERMISSION_DENIED' ? 403 : 400);
                    const message =
                        code === 'NOT_FOUND'
                            ? `Page '${newURL}' was not found`
                            : code === 'PERMISSION_DENIED'
                              ? `Permission denied: you do not have access to page '${newURL}'. Verify the page permissions in dotCMS and that the auth token has sufficient access.`
                              : `Page '${newURL}' could not be loaded (${code})`;

                    if (verbose) {
                        logVerboseError(newURL, message, {
                            status,
                            code,
                            variables: requestVariables
                        });
                    } else {
                        consola.error(`[DotCMS GraphQL Error] ${newURL}: `, message);
                    }

                    throw new DotErrorPage(message, status, code, undefined, {
                        query: completeQuery,
                        variables: requestVariables
                    });
                }
            }

            // 4. Transform and check page — null page with no structured error = 404
            const pageResponse = response.data.page
                ? graphqlToPageEntity(response.data.page)
                : null;

            const styleEditorSchemas = pageResponse ? pageResponse.page.styleEditorSchemas : [];

            if (!pageResponse) {
                throw new DotErrorPage(
                    `Page '${newURL}' was not found`,
                    404,
                    'NOT_FOUND',
                    new DotHttpError({
                        status: 404,
                        statusText: 'Not Found',
                        message: `Page '${newURL}' was not found`,
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
                // Always return an array (never `undefined`) so the response stays JSON-serializable
                // for consumers like Next.js Pages Router (getServerSideProps/getStaticProps throw on undefined).
                errors: response.errors?.length ? response.errors : [],
                ...(styleEditorSchemas?.length && { styleEditorSchemas })
            };
        } catch (error) {
            if (error instanceof DotErrorPage) {
                throw error;
            }

            if (error instanceof DotHttpError) {
                throw new DotErrorPage(
                    `Page request failed for URL '${newURL}': ${error.message}`,
                    error.status,
                    'UNKNOWN',
                    error,
                    { query: completeQuery, variables: requestVariables }
                );
            }

            throw new DotErrorPage(
                `Page request failed for URL '${newURL}': ${error instanceof Error ? error.message : 'Unknown error'}`,
                500,
                'UNKNOWN',
                undefined,
                { query: completeQuery, variables: requestVariables }
            );
        }
    }
}
