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
    StyleEditorFormSchema
} from '@dotcms/types';

import { buildPageQuery, buildQuery, fetchGraphQL, mapContentResponse } from './utils';

import { graphqlToPageEntity } from '../../utils';
import { BaseApiClient } from '../base/api/base-api';

/**
 * Fetches style editor schemas for the given page URL.
 *
 * TODO: Replace mock with real endpoint call like:
 * GET /api/v1/style-editor/schemas?pageUrl=<url>
 *
 * @internal
 */
async function fetchStyleEditorSchemas(
    _url: string,
    _config: DotCMSClientConfig,
    _requestOptions: DotRequestOptions,
    _httpClient: DotHttpClient
): Promise<StyleEditorFormSchema[]> {
    // TODO: Replace mock with real endpoint call like:
    // GET /api/v1/style-editor/schemas?pageUrl=<url>
    return Promise.resolve([{
        "contentType": "Activity",
        "sections": [
            {
                "title": "Typography",
                "fields": [
                    {
                        "type": "dropdown",
                        "label": "Title Size",
                        "id": "title-size",
                        "config": {
                            "options": [
                                {
                                    "label": "Small",
                                    "value": "text-lg"
                                },
                                {
                                    "label": "Medium",
                                    "value": "text-xl"
                                },
                                {
                                    "label": "Large",
                                    "value": "text-2xl"
                                },
                                {
                                    "label": "Extra Large",
                                    "value": "text-3xl"
                                }
                            ]
                        }
                    },
                    {
                        "type": "dropdown",
                        "label": "Description Size",
                        "id": "description-size",
                        "config": {
                            "options": [
                                {
                                    "label": "Small",
                                    "value": "text-sm"
                                },
                                {
                                    "label": "Medium",
                                    "value": "text-base"
                                },
                                {
                                    "label": "Large",
                                    "value": "text-lg"
                                }
                            ]
                        }
                    },
                    {
                        "type": "checkboxGroup",
                        "label": "Title Style",
                        "id": "title-style",
                        "config": {
                            "options": [
                                {
                                    "label": "Bold",
                                    "value": "bold"
                                },
                                {
                                    "label": "Italic",
                                    "value": "italic"
                                },
                                {
                                    "label": "Underline",
                                    "value": "underline"
                                }
                            ]
                        }
                    }
                ]
            },
            {
                "title": "Layout",
                "fields": [
                    {
                        "type": "radio",
                        "label": "Layout",
                        "id": "layout",
                        "config": {
                            "options": [
                                {
                                    "label": "Left",
                                    "value": "left",
                                    "imageURL": "https://i.ibb.co/cXv3tfYd/Screenshot-2025-12-23-at-11-58-32-AM.png"
                                },
                                {
                                    "label": "Right",
                                    "value": "right",
                                    "imageURL": "https://i.ibb.co/v4cJxyLZ/Screenshot-2025-12-23-at-11-59-01-AM.png"
                                },
                                {
                                    "label": "Center",
                                    "value": "center",
                                    "imageURL": "https://i.ibb.co/kVntSyzn/Screenshot-2025-12-23-at-11-58-50-AM.png"
                                },
                                {
                                    "label": "Overlap",
                                    "value": "overlap",
                                    "imageURL": "https://i.ibb.co/43Y5KLY/placeholder-icon-design-free-vector.jpg"
                                }
                            ],
                            "columns": 2
                        }
                    },
                    {
                        "type": "dropdown",
                        "label": "Image Height",
                        "id": "image-height",
                        "config": {
                            "options": [
                                {
                                    "label": "Small",
                                    "value": "h-40"
                                },
                                {
                                    "label": "Medium",
                                    "value": "h-56"
                                },
                                {
                                    "label": "Large",
                                    "value": "h-72"
                                },
                                {
                                    "label": "Extra Large",
                                    "value": "h-96"
                                }
                            ]
                        }
                    }
                ]
            },
            {
                "title": "Card Style",
                "fields": [
                    {
                        "type": "radio",
                        "label": "Card Background",
                        "id": "card-background",
                        "config": {
                            "options": [
                                {
                                    "label": "White",
                                    "value": "white"
                                },
                                {
                                    "label": "Gray",
                                    "value": "gray"
                                },
                                {
                                    "label": "Light Blue",
                                    "value": "light-blue"
                                },
                                {
                                    "label": "Light Green",
                                    "value": "light-green"
                                }
                            ],
                            "columns": 2
                        }
                    },
                    {
                        "type": "radio",
                        "label": "Border Radius",
                        "id": "border-radius",
                        "config": {
                            "options": [
                                {
                                    "label": "None",
                                    "value": "none"
                                },
                                {
                                    "label": "Small",
                                    "value": "small"
                                },
                                {
                                    "label": "Medium",
                                    "value": "medium"
                                },
                                {
                                    "label": "Large",
                                    "value": "large"
                                }
                            ],
                            "columns": 2
                        }
                    },
                    {
                        "type": "checkboxGroup",
                        "label": "Card Effects",
                        "id": "card-effects",
                        "config": {
                            "options": [
                                {
                                    "label": "Shadow",
                                    "value": "shadow"
                                },
                                {
                                    "label": "Border",
                                    "value": "border"
                                }
                            ]
                        }
                    }
                ]
            },
            {
                "title": "Button",
                "fields": [
                    {
                        "type": "radio",
                        "label": "Button Color",
                        "id": "button-color",
                        "config": {
                            "options": [
                                {
                                    "label": "Blue",
                                    "value": "blue"
                                },
                                {
                                    "label": "Green",
                                    "value": "green"
                                },
                                {
                                    "label": "Red",
                                    "value": "red"
                                },
                                {
                                    "label": "Purple",
                                    "value": "purple"
                                },
                                {
                                    "label": "Orange",
                                    "value": "orange"
                                },
                                {
                                    "label": "Teal",
                                    "value": "teal"
                                }
                            ],
                            "columns": 2
                        }
                    },
                    {
                        "type": "dropdown",
                        "label": "Button Size",
                        "id": "button-size",
                        "config": {
                            "options": [
                                {
                                    "label": "Small",
                                    "value": "small"
                                },
                                {
                                    "label": "Medium",
                                    "value": "medium"
                                },
                                {
                                    "label": "Large",
                                    "value": "large"
                                }
                            ]
                        }
                    },
                    {
                        "type": "checkboxGroup",
                        "label": "Button Style",
                        "id": "button-style",
                        "config": {
                            "options": [
                                {
                                    "label": "Rounded",
                                    "value": "rounded"
                                },
                                {
                                    "label": "Full Rounded",
                                    "value": "full-rounded"
                                },
                                {
                                    "label": "Shadow",
                                    "value": "shadow"
                                }
                            ]
                        }
                    }
                ]
            }
        ]
    }]);
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
            const [response, styleEditorSchemas] = await Promise.all([
                fetchGraphQL({
                    baseURL: this.dotcmsUrl,
                    body: requestBody,
                    headers: requestHeaders,
                    httpClient: this.httpClient
                }),
                fetchStyleEditorSchemas(
                          url,
                          this.config,
                          this.requestOptions,
                          this.httpClient
                      )
            ]);

            console.log('styleEditorSchemas', styleEditorSchemas);

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
                },
                ...(styleEditorSchemas && { styleEditorSchemas })
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
