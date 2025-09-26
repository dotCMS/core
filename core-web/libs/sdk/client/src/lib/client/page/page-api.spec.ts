/* eslint-disable @typescript-eslint/no-explicit-any */
jest.mock('consola');

import * as consola from 'consola';

import {
    DotCMSClientConfig,
    DotCMSPageRequestParams,
    DotRequestOptions,
    DotHttpError,
} from '@dotcms/types';

import { PageClient, DotCMSPageError } from './page-api';

import { FetchHttpClient } from '../adapters/fetch-http-client';

// Mock the FetchHttpClient
jest.mock('../adapters/fetch-http-client');

describe('PageClient', () => {
    const mockRequest = jest.fn();
    const MockedFetchHttpClient = FetchHttpClient as jest.MockedClass<typeof FetchHttpClient>;

    const validConfig: DotCMSClientConfig = {
        dotcmsUrl: 'https://demo.dotcms.com',
        authToken: 'test-token',
        siteId: 'test-site'
    };

    const requestOptions: DotRequestOptions = {
        headers: {
            Authorization: 'Bearer test-token'
        }
    };

    const mockGraphQLResponse = {
        data: {
            page: {
                title: 'GraphQL Page',
                url: '/graphql-page',
                layout: {
                    header: {
                        title: 'Header'
                    }
                },
                viewAs: {
                    visitor: {
                        persona: {
                            title: 'Visitor Persona'
                        }
                    }
                },
                containers: [
                    {
                        path: 'demo.dotcms.com',
                        identifier: 'test-container',
                        maxContentlets: 10,
                        containerStructures: [],
                        containerContentlets: []
                    }
                ]
            },
            content: {
                items: [{ title: 'Content Item' }]
            },
            nav: {
                items: [{ label: 'Nav Item', url: '/nav' }]
            }
        },
        errors: null
    };

    beforeEach(() => {
        mockRequest.mockReset();
        global.console.error = jest.fn(); // Mock console.error to prevent actual errors from being logged in the console when running tests

        MockedFetchHttpClient.mockImplementation(
            () =>
                ({
                    request: mockRequest
                }) as Partial<FetchHttpClient> as FetchHttpClient
        );

        mockRequest.mockResolvedValue(mockGraphQLResponse);
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    describe('GraphQL API', () => {
        it('should fetch page using GraphQL when query option is provided', async () => {
            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());

            const graphQLOptions: DotCMSPageRequestParams = {
                graphql: {
                    page: `containers {
      containerContentlets {
        contentlets {
         ... on Banner {
            title
          }
        }
      }
    }`,
                    content: { content: 'query Content { items { title } }' }
                },
                languageId: '1',
                mode: 'LIVE'
            };

            const result = await pageClient.get('/graphql-page', graphQLOptions);

            expect(mockRequest).toHaveBeenCalledWith('https://demo.dotcms.com/api/v1/graphql', {
                method: 'POST',
                headers: {
                    Authorization: 'Bearer test-token'
                },
                body: expect.stringContaining(`... on Banner`)
            });

            expect(result).toEqual({
                pageAsset: {
                    layout: {
                        header: {
                            title: 'Header'
                        }
                    },
                    viewAs: {
                        visitor: {
                            persona: {
                                title: 'Visitor Persona'
                            }
                        }
                    },
                    containers: {
                        'demo.dotcms.com': {
                            containerStructures: [],
                            container: {
                                path: 'demo.dotcms.com',
                                identifier: 'test-container',
                                maxContentlets: 10
                            },
                            contentlets: {}
                        }
                    },
                    page: {
                        title: 'GraphQL Page',
                        url: '/graphql-page'
                    },
                    site: undefined,
                    template: undefined,
                    runningExperimentId: undefined,
                    urlContentMap: {},
                    vanityUrl: undefined
                },
                content: {
                    content: {
                        items: [{ title: 'Content Item' }]
                    }
                },
                graphql: {
                    query: expect.any(String),
                    variables: {
                        url: '/graphql-page',
                        mode: 'LIVE',
                        languageId: '1',
                        fireRules: false,
                        siteId: 'test-site',
                        personaId: undefined,
                        publishDate: undefined,
                        variantName: undefined
                    }
                }
            });
        });

        it('should print graphql errors', async () => {
            const consolaSpy = jest.spyOn(consola, 'error');
            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());

            mockRequest.mockResolvedValue({
                data: {
                    page: {
                        title: 'GraphQL Page'
                    }
                },
                errors: [{ message: 'Some internal server error' }]
            });

            await pageClient.get('/graphql-page');

            expect(consolaSpy).toHaveBeenCalledWith(
                '[DotCMS GraphQL Error]: ',
                'Some internal server error'
            );
        });

        it('should return an error if the page is not found', async () => {
            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());
            const graphQLOptions = {
                graphql: {
                    page: `containers {
                        containerContentlets {
                            contentlets {
                                ... on Banner {
                                    title
                                }
                            }
                        }
                    }`,
                    content: { content: 'query Content { items { title } }' }
                }
            };

            mockRequest.mockResolvedValue({
                data: {
                    page: null
                },
                errors: [{ message: 'No page data found' }]
            });

            try {
                await pageClient.get('/graphql-page', graphQLOptions);
            } catch (error: unknown) {
                expect(error).toBeInstanceOf(DotCMSPageError);
                if (error instanceof DotCMSPageError) {
                    expect(error.message).toBe('Page request failed for URL \'/graphql-page\': Page /graphql-page not found. Check the page URL and permissions.');
                    expect(error.graphql).toBeDefined();
                    expect(error.graphql?.query).toContain('containers');
                }
            }
        });

        it('should add leading slash to url if it does not have it', async () => {
            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());

            // No leading slash
            const result = await pageClient.get('graphql-page', {});

            expect(result.graphql.variables['url']).toEqual('/graphql-page');
        });

        it('should pass correct variables to GraphQL query', async () => {
            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());
            const graphQLOptions = {
                graphql: {
                    page: `containers {
      containerContentlets {
        contentlets {
         ... on Banner {
            title
          }
        }
      }
    }`,
                    content: { content: 'query Content { items { title } }' }
                },
                languageId: '2',
                mode: 'PREVIEW_MODE'
            };

            await pageClient.get('/custom-page', graphQLOptions as any);

            const requestBody = JSON.parse(mockRequest.mock.calls[0][1].body);
            expect(requestBody.variables).toEqual({
                url: '/custom-page',
                mode: 'PREVIEW_MODE',
                languageId: '2',
                fireRules: false,
                siteId: 'test-site'
            });
        });

        it('should throw errors from GraphQL', async () => {
            mockRequest.mockResolvedValue({
                errors: [{ message: 'GraphQL error' }]
            });

            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());
            const graphQLOptions = {
                graphql: {
                    page: `containers {
      containerContentlets {
        contentlets {
         ... on Banner {
            title
          }
        }`,
                    content: { content: 'query Content { items { title } }' }
                }
            };
            try {
                await pageClient.get('/page', graphQLOptions);
            } catch (error: unknown) {
                expect(error).toBeInstanceOf(DotCMSPageError);
                if (error instanceof DotCMSPageError) {
                    expect(error.message).toBe('Page request failed for URL \'/page\': Cannot read properties of undefined (reading \'page\')');
                    expect(error.graphql).toBeDefined();
                }
            }
        });

        it('should handle HTTP errors', async () => {
            const httpError = new DotHttpError({
                status: 404,
                statusText: 'Not Found',
                message: 'Page not found',
                data: { error: 'Page not found' }
            });
            mockRequest.mockRejectedValue(httpError);

            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());
            const graphQLOptions = {
                graphql: {
                    page: `containers { title }`,
                    content: { content: 'query Content { items { title } }' }
                }
            };

            try {
                await pageClient.get('/page', graphQLOptions);
            } catch (error: unknown) {
                expect(error).toBeInstanceOf(DotCMSPageError);
                if (error instanceof DotCMSPageError) {
                    expect(error.message).toBe('Page request failed for URL \'/page\': Page not found');
                    expect(error.httpError).toBe(httpError);
                    expect(error.graphql).toBeDefined();
                }
            }
        });

        it('should use default values for languageId and mode if not provided', async () => {
            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());
            const graphQLOptions = {
                graphql: {
                    page: `containers {
      containerContentlets {
        contentlets {
         ... on Banner {
            title
          }
        }`,
                    content: { content: 'query Content { items { title } }' }
                }
            };
            await pageClient.get('/default-page', graphQLOptions);

            const requestBody = JSON.parse(mockRequest.mock.calls[0][1].body);
            expect(requestBody.variables).toEqual({
                url: '/default-page',
                mode: 'LIVE',
                languageId: '1',
                fireRules: false,
                siteId: 'test-site'
            });
        });

        it('should fetch using graphQL even if there is no graphql option', async () => {
            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());

            await pageClient.get('/why-obi-wan-had-the-high-ground');

            expect(mockRequest).toHaveBeenCalledWith(
                'https://demo.dotcms.com/api/v1/graphql',
                expect.objectContaining({
                    method: 'POST',
                    headers: expect.objectContaining({
                        Authorization: 'Bearer test-token'
                    }),
                    body: expect.stringContaining(`"url":"/why-obi-wan-had-the-high-ground"`)
                })
            );
        });
    });

    describe('Client initialization', () => {
        it('should use siteId from config when not provided in params', async () => {
            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());

            await pageClient.get('/test-page');

            expect(mockRequest).toHaveBeenCalledWith(
                'https://demo.dotcms.com/api/v1/graphql',
                expect.objectContaining({
                    method: 'POST',
                    headers: expect.objectContaining({
                        Authorization: 'Bearer test-token'
                    }),
                    body: expect.stringContaining('"siteId":"test-site"')
                })
            );
        });
    });
});
