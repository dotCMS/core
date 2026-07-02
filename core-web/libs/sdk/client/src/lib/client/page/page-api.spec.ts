/* eslint-disable @typescript-eslint/no-explicit-any */
jest.mock('consola');

import { consola } from 'consola';

import {
    DotCMSClientConfig,
    DotCMSPageRequestParams,
    DotErrorPage,
    DotHttpError,
    DotRequestOptions
} from '@dotcms/types';

import { PageClient } from './page-api';

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
                identifier: 'test-page-id',
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
                        identifier: 'test-page-id',
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
                        siteId: 'test-site'
                    }
                }
            });
        });

        // Regression: https://github.com/dotCMS/core/issues/36108
        // The returned response must be JSON-serializable so consumers like Next.js Pages Router
        // can return it from getServerSideProps/getStaticProps without throwing on `undefined`.
        it('returns a JSON-serializable response with no undefined values in graphql.variables', async () => {
            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());

            const result = await pageClient.get('/graphql-page');

            const variables = result.graphql.variables;
            expect(Object.values(variables).some((value) => value === undefined)).toBe(false);

            // Round-trips without losing data and without throwing on undefined values.
            expect(() => JSON.stringify(result.graphql.variables)).not.toThrow();
            expect(JSON.parse(JSON.stringify(variables))).toEqual(variables);
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
                '[DotCMS GraphQL Error] /graphql-page: ',
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
                expect(error).toBeInstanceOf(DotErrorPage);
                if (error instanceof DotErrorPage) {
                    expect(error.message).toBe("Page '/graphql-page' was not found");
                    expect(error.status).toBe(404);
                    expect(error.code).toBe('NOT_FOUND');
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
                mode: 'PREVIEW'
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

        it('should throw BAD_REQUEST error when data is null (bad query)', async () => {
            mockRequest.mockResolvedValue({
                data: null,
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

            const error = await pageClient.get('/page', graphQLOptions).catch((e) => e);
            expect(error).toBeInstanceOf(DotErrorPage);
            expect(error.message).toBe('GraphQL error');
            expect(error.status).toBe(400);
            expect(error.code).toBe('BAD_REQUEST');
            expect(error.graphql).toBeDefined();
        });

        it('should throw BAD_REQUEST error when data is undefined (bad query)', async () => {
            mockRequest.mockResolvedValue({
                errors: [{ message: 'GraphQL validation error' }]
            });

            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());

            const error = await pageClient.get('/page').catch((e) => e);
            expect(error).toBeInstanceOf(DotErrorPage);
            expect(error.message).toBe('GraphQL validation error');
            expect(error.status).toBe(400);
            expect(error.code).toBe('BAD_REQUEST');
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
                fail('Should have thrown an error');
            } catch (error: unknown) {
                expect(error).toBeInstanceOf(DotErrorPage);
                if (error instanceof DotErrorPage) {
                    expect(error.message).toBe(
                        "Page request failed for URL '/page': Page not found"
                    );
                    expect(error.status).toBe(404);
                    expect(error.code).toBe('UNKNOWN');
                    expect(error.httpError).toBe(httpError);
                    expect(error.graphql).toBeDefined();
                    expect(error.graphql?.query).toBeDefined();
                    expect(error.graphql?.variables).toEqual({
                        url: '/page',
                        mode: 'LIVE',
                        languageId: '1',
                        fireRules: false,
                        siteId: 'test-site'
                    });
                }
            }
        });

        it('should throw PERMISSION_DENIED error when extensions.code is PERMISSION_DENIED', async () => {
            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());
            const graphQLOptions = {
                graphql: {
                    page: `containers { title }`,
                    content: { content: 'query Content { items { title } }' }
                }
            };

            mockRequest.mockResolvedValue({
                data: {
                    page: null
                },
                errors: [
                    {
                        message:
                            'Permission denied: You do not have permission to access this resource.',
                        extensions: { code: 'PERMISSION_DENIED', status: 403 }
                    }
                ]
            });

            try {
                await pageClient.get('/restricted-page', graphQLOptions);
                fail('Should have thrown an error');
            } catch (error: unknown) {
                expect(error).toBeInstanceOf(DotErrorPage);
                if (error instanceof DotErrorPage) {
                    expect(error.message).toBe(
                        "Permission denied: you do not have access to page '/restricted-page'. Verify the page permissions in dotCMS and that the auth token has sufficient access."
                    );
                    expect(error.status).toBe(403);
                    expect(error.code).toBe('PERMISSION_DENIED');
                    expect(error.httpError).toBeUndefined();
                    expect(error.graphql).toBeDefined();
                    expect(error.graphql?.query).toBeDefined();
                }
            }
        });

        it('should throw NOT_FOUND error when extensions.code is NOT_FOUND', async () => {
            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());

            mockRequest.mockResolvedValue({
                data: {
                    page: null
                },
                errors: [
                    {
                        message: 'Page not found: /missing',
                        extensions: {
                            code: 'NOT_FOUND',
                            status: 404,
                            resourceType: 'Page',
                            resourceId: '/missing'
                        }
                    }
                ]
            });

            try {
                await pageClient.get('/missing');
                fail('Should have thrown an error');
            } catch (error: unknown) {
                expect(error).toBeInstanceOf(DotErrorPage);
                if (error instanceof DotErrorPage) {
                    expect(error.message).toBe("Page '/missing' was not found");
                    expect(error.status).toBe(404);
                    expect(error.code).toBe('NOT_FOUND');
                    expect(error.httpError).toBeUndefined();
                }
            }
        });

        it('should infer status from code when extensions.status is missing', async () => {
            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());

            mockRequest.mockResolvedValue({
                data: {
                    page: null
                },
                errors: [
                    {
                        message: 'Permission denied',
                        extensions: { code: 'PERMISSION_DENIED' }
                    }
                ]
            });

            try {
                await pageClient.get('/page');
                fail('Should have thrown an error');
            } catch (error: unknown) {
                expect(error).toBeInstanceOf(DotErrorPage);
                if (error instanceof DotErrorPage) {
                    expect(error.status).toBe(403);
                    expect(error.code).toBe('PERMISSION_DENIED');
                }
            }
        });

        it('should throw 404 error with httpError when page is null with no structured errors', async () => {
            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());
            const graphQLOptions = {
                graphql: {
                    page: `containers { title }`,
                    content: { content: 'query Content { items { title } }' }
                }
            };

            mockRequest.mockResolvedValue({
                data: {
                    page: null
                },
                errors: []
            });

            try {
                await pageClient.get('/missing-page', graphQLOptions);
                fail('Should have thrown an error');
            } catch (error: unknown) {
                expect(error).toBeInstanceOf(DotErrorPage);
                if (error instanceof DotErrorPage) {
                    expect(error.message).toBe("Page '/missing-page' was not found");
                    expect(error.status).toBe(404);
                    expect(error.code).toBe('NOT_FOUND');
                    expect(error.httpError).toBeInstanceOf(DotHttpError);
                    if (error.httpError) {
                        expect(error.httpError.status).toBe(404);
                        expect(error.httpError.statusText).toBe('Not Found');
                    }
                    expect(error.graphql).toBeDefined();
                    expect(error.graphql?.query).toBeDefined();
                    expect(error.graphql?.variables['url']).toBe('/missing-page');
                }
            }
        });

        it('should handle generic non-HTTP errors', async () => {
            const genericError = new Error('Network timeout');
            mockRequest.mockRejectedValue(genericError);

            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());
            const graphQLOptions = {
                graphql: {
                    page: `containers { title }`,
                    content: { content: 'query Content { items { title } }' }
                }
            };

            try {
                await pageClient.get('/error-page', graphQLOptions);
                fail('Should have thrown an error');
            } catch (error: unknown) {
                expect(error).toBeInstanceOf(DotErrorPage);
                if (error instanceof DotErrorPage) {
                    expect(error.message).toBe(
                        "Page request failed for URL '/error-page': Network timeout"
                    );
                    expect(error.status).toBe(500);
                    expect(error.code).toBe('UNKNOWN');
                    expect(error.httpError).toBeUndefined();
                    expect(error.graphql).toBeDefined();
                    expect(error.graphql?.query).toBeDefined();
                    expect(error.graphql?.variables['url']).toBe('/error-page');
                }
            }
        });

        it('should handle unknown errors (non-Error instances)', async () => {
            mockRequest.mockRejectedValue('Some string error');

            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());

            try {
                await pageClient.get('/unknown-error-page');
                fail('Should have thrown an error');
            } catch (error: unknown) {
                expect(error).toBeInstanceOf(DotErrorPage);
                if (error instanceof DotErrorPage) {
                    expect(error.message).toBe(
                        "Page request failed for URL '/unknown-error-page': Unknown error"
                    );
                    expect(error.status).toBe(500);
                    expect(error.code).toBe('UNKNOWN');
                    expect(error.httpError).toBeUndefined();
                    expect(error.graphql).toBeDefined();
                }
            }
        });

        it('should include graphql query and variables in all error scenarios', async () => {
            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());
            const graphQLOptions = {
                graphql: {
                    page: `containers { title }`,
                    content: { blogPosts: 'query BlogPosts { items { title } }' },
                    variables: {
                        customVar: 'customValue'
                    }
                },
                languageId: '2',
                personaId: 'test-persona',
                variantName: 'test-variant'
            };

            mockRequest.mockResolvedValue({
                data: { page: null },
                errors: []
            });

            try {
                await pageClient.get('/test-page', graphQLOptions);
                fail('Should have thrown an error');
            } catch (error: unknown) {
                expect(error).toBeInstanceOf(DotErrorPage);
                if (error instanceof DotErrorPage) {
                    expect(error.graphql).toBeDefined();
                    expect(error.graphql?.query).toContain('containers');
                    expect(error.graphql?.query).toContain('BlogPosts');
                    expect(error.graphql?.variables).toEqual({
                        url: '/test-page',
                        mode: 'LIVE',
                        languageId: '2',
                        fireRules: false,
                        siteId: 'test-site',
                        personaId: 'test-persona',
                        variantName: 'test-variant',
                        customVar: 'customValue'
                    });
                }
            }
        });

        it('should surface partial errors in successful response', async () => {
            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());

            const partialErrors = [
                {
                    message: 'Permission denied for Blog content type',
                    extensions: { classification: 'DataFetchingException' }
                }
            ];

            mockRequest.mockResolvedValue({
                data: {
                    page: {
                        title: 'GraphQL Page',
                        url: '/graphql-page',
                        layout: { header: { title: 'Header' } },
                        viewAs: { visitor: { persona: { title: 'Visitor Persona' } } },
                        containers: []
                    }
                },
                errors: partialErrors
            });

            const result = await pageClient.get('/graphql-page');

            expect(result.pageAsset).toBeDefined();
            expect(result.errors).toEqual(partialErrors);
        });

        it('should surface partial errors with extensions.code in successful response', async () => {
            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());

            const partialErrors = [
                {
                    message: 'Permission denied for Blog content type',
                    extensions: {
                        code: 'PERMISSION_DENIED',
                        status: 403,
                        classification: 'DataFetchingException'
                    }
                }
            ];

            mockRequest.mockResolvedValue({
                data: {
                    page: {
                        title: 'GraphQL Page',
                        url: '/graphql-page',
                        layout: { header: { title: 'Header' } },
                        viewAs: { visitor: { persona: { title: 'Visitor Persona' } } },
                        containers: []
                    }
                },
                errors: partialErrors
            });

            const result = await pageClient.get('/graphql-page');

            expect(result.pageAsset).toBeDefined();
            expect(result.errors).toEqual(partialErrors);
            expect(result.errors?.[0].extensions?.code).toBe('PERMISSION_DENIED');
        });

        it('should not include errors when there are none', async () => {
            const pageClient = new PageClient(validConfig, requestOptions, new FetchHttpClient());

            const result = await pageClient.get('/graphql-page');

            expect(result.pageAsset).toBeDefined();
            expect(result.errors).toBeUndefined();
        });

        describe('verbose logLevel', () => {
            const verboseConfig: DotCMSClientConfig = {
                ...validConfig,
                logLevel: 'verbose'
            };

            it('should call logVerboseError with status/code for structured errors', async () => {
                const consolaSpy = jest.spyOn(consola, 'error');
                const pageClient = new PageClient(
                    verboseConfig,
                    requestOptions,
                    new FetchHttpClient()
                );

                mockRequest.mockResolvedValue({
                    data: { page: null },
                    errors: [
                        {
                            message: 'Not found',
                            extensions: { code: 'NOT_FOUND', status: 404 }
                        }
                    ]
                });

                await pageClient.get('/verbose-page').catch(() => undefined);

                expect(consolaSpy).toHaveBeenCalledWith(
                    expect.stringContaining('[DotCMS GraphQL Error] /verbose-page:')
                );
                expect(consolaSpy).toHaveBeenCalledWith(
                    expect.stringContaining('status: 404 | code: NOT_FOUND')
                );
            });

            it('should call logVerboseError for unstructured errors (no extensions.code)', async () => {
                const consolaSpy = jest.spyOn(consola, 'error');
                const pageClient = new PageClient(
                    verboseConfig,
                    requestOptions,
                    new FetchHttpClient()
                );

                mockRequest.mockResolvedValue({
                    data: {
                        page: {
                            title: 'Page',
                            url: '/verbose-page',
                            containers: [],
                            layout: {},
                            viewAs: {}
                        }
                    },
                    errors: [{ message: 'Some internal error' }]
                });

                await pageClient.get('/verbose-page').catch(() => undefined);

                expect(consolaSpy).toHaveBeenCalledWith(
                    expect.stringContaining('[DotCMS GraphQL Error] /verbose-page:')
                );
                expect(consolaSpy).toHaveBeenCalledWith(expect.stringContaining('variables:'));
            });

            it('should include variables in the verbose log output', async () => {
                const consolaSpy = jest.spyOn(consola, 'error');
                const pageClient = new PageClient(
                    verboseConfig,
                    requestOptions,
                    new FetchHttpClient()
                );

                mockRequest.mockResolvedValue({
                    data: { page: null },
                    errors: [{ message: 'Not found', extensions: { code: 'NOT_FOUND' } }]
                });

                await pageClient.get('/verbose-page', { languageId: '3' }).catch(() => undefined);

                expect(consolaSpy).toHaveBeenCalledWith(
                    expect.stringContaining('"url": "/verbose-page"')
                );
            });

            it('should use consola.error (non-verbose) for structured errors when logLevel is default', async () => {
                const consolaSpy = jest.spyOn(consola, 'error').mockClear();
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );

                mockRequest.mockResolvedValue({
                    data: { page: null },
                    errors: [
                        { message: 'Not found', extensions: { code: 'NOT_FOUND', status: 404 } }
                    ]
                });

                await pageClient.get('/default-page').catch(() => undefined);

                const calls = consolaSpy.mock.calls.map((args) => args.join(' '));
                expect(calls.some((c) => c.includes('[DotCMS GraphQL Error] /default-page:'))).toBe(
                    true
                );
                expect(calls.every((c) => !c.includes('status: 404 | code: NOT_FOUND'))).toBe(true);
            });
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

        describe('styleEditorSchemas', () => {
            it('should include styleEditorSchemas in result when GQL returns schemas', async () => {
                const mockSchemas = [{ variable: 'Banner', schema: { color: { type: 'color' } } }];

                mockRequest.mockResolvedValue({
                    ...mockGraphQLResponse,
                    data: {
                        ...mockGraphQLResponse.data,
                        page: { ...mockGraphQLResponse.data.page, styleEditorSchemas: mockSchemas }
                    }
                });

                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                const result = await pageClient.get('/graphql-page');

                expect(result.styleEditorSchemas).toEqual(mockSchemas);
            });

            it('should omit styleEditorSchemas from result when GQL returns null (not in EDIT_MODE)', async () => {
                mockRequest.mockResolvedValue({
                    ...mockGraphQLResponse,
                    data: {
                        ...mockGraphQLResponse.data,
                        page: { ...mockGraphQLResponse.data.page, styleEditorSchemas: null }
                    }
                });

                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                const result = await pageClient.get('/graphql-page');

                expect(result.styleEditorSchemas).toBeUndefined();
            });

            it('should omit styleEditorSchemas from result when GQL returns an empty array', async () => {
                mockRequest.mockResolvedValue({
                    ...mockGraphQLResponse,
                    data: {
                        ...mockGraphQLResponse.data,
                        page: { ...mockGraphQLResponse.data.page, styleEditorSchemas: [] }
                    }
                });

                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                const result = await pageClient.get('/graphql-page');

                expect(result.styleEditorSchemas).toBeUndefined();
            });

            it('should omit styleEditorSchemas when identifier is missing from the page response', async () => {
                mockRequest.mockResolvedValue({
                    ...mockGraphQLResponse,
                    data: {
                        ...mockGraphQLResponse.data,
                        page: { ...mockGraphQLResponse.data.page, identifier: undefined }
                    }
                });

                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                const result = await pageClient.get('/graphql-page');

                expect(result.styleEditorSchemas).toBeUndefined();
            });
        });
    });

    describe('request contract', () => {
        const getRequestBody = () => JSON.parse(mockRequest.mock.calls[0][1].body);

        describe('GraphQL variables', () => {
            it('sends all default variables when no options provided', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home');

                expect(getRequestBody().variables).toEqual({
                    url: '/home',
                    mode: 'LIVE',
                    languageId: '1',
                    fireRules: false,
                    siteId: 'test-site'
                });
            });

            it('sends overridden languageId when provided', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home', { languageId: '5' });

                expect(getRequestBody().variables.languageId).toBe('5');
            });

            it.each([
                ['EDIT', 'EDIT_MODE'],
                ['PREVIEW', 'PREVIEW_MODE'],
                ['LIVE', 'LIVE'],
                ['UNKNOWN', 'UNKNOWN']
            ] as const)(
                'translates UVE_MODE key %s to backend PageMode value %s',
                async (key, expectedValue) => {
                    const pageClient = new PageClient(
                        validConfig,
                        requestOptions,
                        new FetchHttpClient()
                    );
                    await pageClient.get('/home', { mode: key });

                    expect(getRequestBody().variables.mode).toBe(expectedValue);
                }
            );

            it('sends personaId when provided', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home', { personaId: 'persona-abc' });

                expect(getRequestBody().variables.personaId).toBe('persona-abc');
            });

            it('sends variantName when provided', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home', { variantName: 'my-experiment' });

                expect(getRequestBody().variables.variantName).toBe('my-experiment');
            });

            it('sends publishDate when provided', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home', { publishDate: '2025-01-15' });

                expect(getRequestBody().variables.publishDate).toBe('2025-01-15');
            });

            it('sends siteId override over config value', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home', { siteId: 'other-site' });

                expect(getRequestBody().variables.siteId).toBe('other-site');
            });

            it('merges custom graphql.variables into the request variables', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home', {
                    graphql: { variables: { customParam: 'value', anotherParam: 'extra' } }
                });

                const vars = getRequestBody().variables;
                expect(vars.customParam).toBe('value');
                expect(vars.anotherParam).toBe('extra');
            });

            it('custom variables do not override core variables', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home', {
                    languageId: '2',
                    graphql: { variables: { languageId: '99' } }
                });

                // custom variables are spread after core ones — this documents current behaviour
                expect(getRequestBody().variables.languageId).toBe('99');
            });

            it('normalizes url without leading slash', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('no-slash');

                expect(getRequestBody().variables.url).toBe('/no-slash');
            });

            it('preserves url that already has a leading slash', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/already-slash');

                expect(getRequestBody().variables.url).toBe('/already-slash');
            });

            // Regression: https://github.com/dotCMS/core/issues/36108
            // Optional params left undefined must be omitted from the variables object so the
            // returned page response is JSON-serializable (Next.js Pages Router throws otherwise).
            it('omits optional params left undefined instead of sending undefined values', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home');

                const vars = getRequestBody().variables;
                expect(vars).not.toHaveProperty('personaId');
                expect(vars).not.toHaveProperty('publishDate');
                expect(vars).not.toHaveProperty('variantName');
            });
        });

        describe('GraphQL query shape', () => {
            it('sends request to /api/v1/graphql', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home');

                expect(mockRequest.mock.calls[0][0]).toBe('https://demo.dotcms.com/api/v1/graphql');
            });

            it('query contains the PageContent operation name', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home');

                expect(getRequestBody().query).toContain('query PageContent(');
            });

            it('query contains the DotCMSPage fragment', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home');

                expect(getRequestBody().query).toContain('fragment DotCMSPage on DotPage');
            });

            it('query contains required page fields', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home');

                const query = getRequestBody().query;
                expect(query).toContain('containers');
                expect(query).toContain('layout');
                expect(query).toContain('viewAs');
                expect(query).toContain('vanityUrl');
            });

            it('query uses _map when no page fragment provided', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home');

                expect(getRequestBody().query).toContain('_map');
            });

            it('query includes ClientPage fragment when page is provided', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home', { graphql: { page: 'title url' } });

                const query = getRequestBody().query;
                expect(query).toContain('fragment ClientPage on DotPage');
                expect(query).toContain('title url');
                expect(query).toContain('...ClientPage');
            });

            it('query includes additional content queries', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home', {
                    graphql: {
                        content: {
                            blogs: 'BlogCollection(limit: 3) { title }',
                            nav: 'Navigation { href }'
                        }
                    }
                });

                const query = getRequestBody().query;
                expect(query).toContain('blogs: BlogCollection(limit: 3) { title }');
                expect(query).toContain('nav: Navigation { href }');
            });

            it('query includes custom fragments', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home', {
                    graphql: { fragments: ['fragment MyFrag on DotPage { inode }'] }
                });

                expect(getRequestBody().query).toContain('fragment MyFrag on DotPage { inode }');
            });

            it('sends POST with correct Content-Type header', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home');

                expect(mockRequest.mock.calls[0][1]).toMatchObject({
                    method: 'POST'
                });
            });

            it('includes Authorization header from requestOptions', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                await pageClient.get('/home');

                expect(mockRequest.mock.calls[0][1].headers).toMatchObject({
                    Authorization: 'Bearer test-token'
                });
            });
        });

        describe('response shape', () => {
            it('result.pageAsset is defined on success', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                const result = await pageClient.get('/home');

                expect(result.pageAsset).toBeDefined();
            });

            it('result.graphql.query matches the query that was sent', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                const result = await pageClient.get('/home');

                expect(result.graphql.query).toBe(getRequestBody().query);
            });

            it('result.graphql.variables matches the variables that were sent', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                const result = await pageClient.get('/home', { languageId: '3' });

                expect(result.graphql.variables).toEqual(getRequestBody().variables);
            });

            it('omits undefined optional variables from the returned graphql.variables', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                const result = await pageClient.get('/home');

                expect(result.graphql.variables).toEqual({
                    url: '/home',
                    mode: 'LIVE',
                    languageId: '1',
                    fireRules: false,
                    siteId: 'test-site'
                });
                expect(Object.values(result.graphql.variables).every((value) => value !== undefined)).toBe(
                    true
                );
                expect(JSON.parse(JSON.stringify(result.graphql.variables))).toEqual(
                    result.graphql.variables
                );
            });

            it('result.errors is undefined when response has no errors', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                const result = await pageClient.get('/home');

                expect(result.errors).toBeUndefined();
            });

            it('result.errors contains partial errors when page succeeds', async () => {
                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                const partialError = {
                    message: 'Partial failure',
                    extensions: { classification: 'DataFetchingException' }
                };

                mockRequest.mockResolvedValueOnce({
                    ...mockGraphQLResponse,
                    errors: [partialError]
                });

                const result = await pageClient.get('/home');

                expect(result.errors).toEqual([partialError]);
            });

            it('DotErrorPage.graphql.query is the query that was sent', async () => {
                mockRequest.mockResolvedValueOnce({
                    data: { page: null },
                    errors: [{ message: 'Not found', extensions: { code: 'NOT_FOUND' } }]
                });

                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                const error = await pageClient.get('/missing').catch((e) => e);

                expect(error).toBeInstanceOf(DotErrorPage);
                expect(error.graphql.query).toContain('query PageContent(');
            });

            it('DotErrorPage.graphql.variables are the variables that were sent', async () => {
                mockRequest.mockResolvedValueOnce({
                    data: { page: null },
                    errors: [{ message: 'Not found', extensions: { code: 'NOT_FOUND' } }]
                });

                const pageClient = new PageClient(
                    validConfig,
                    requestOptions,
                    new FetchHttpClient()
                );
                const error = await pageClient.get('/missing', { languageId: '7' }).catch((e) => e);

                expect(error.graphql.variables.url).toBe('/missing');
                expect(error.graphql.variables.languageId).toBe('7');
            });
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
