/* eslint-disable @typescript-eslint/no-explicit-any */
import { GraphQLPageOptions, PageClient } from './page-api';
import * as utils from './utils';

import { graphqlToPageEntity } from '../../utils';
import { DotCMSClientConfig, RequestOptions } from '../client';
import { ErrorMessages } from '../models';

describe('PageClient', () => {
    const mockFetch = jest.fn();
    const originalFetch = global.fetch;
    const mockFetchGraphQL = jest.fn();

    const validConfig: DotCMSClientConfig = {
        dotcmsUrl: 'https://demo.dotcms.com',
        authToken: 'test-token',
        siteId: 'test-site'
    };

    const requestOptions: RequestOptions = {
        headers: {
            Authorization: 'Bearer test-token'
        }
    };

    const mockPageData = {
        entity: {
            title: 'Test Page',
            url: '/test-page',
            contentType: 'htmlpage',
            layout: {
                body: {
                    rows: []
                }
            }
        }
    };

    const mockGraphQLResponse = {
        data: {
            page: {
                title: 'GraphQL Page',
                url: '/graphql-page'
            },
            testContent: {
                items: [{ title: 'Content Item' }]
            },
            testNav: {
                items: [{ label: 'Nav Item', url: '/nav' }]
            }
        },
        errors: null
    };

    beforeEach(() => {
        mockFetch.mockReset();
        mockFetchGraphQL.mockReset();
        global.fetch = mockFetch;
        global.console.error = jest.fn(); // Mock console.error to prevent actual errors from being logged in the console when running tests

        mockFetch.mockResolvedValue({
            ok: true,
            json: async () => mockPageData
        });

        jest.spyOn(utils, 'fetchGraphQL').mockImplementation(mockFetchGraphQL);
        mockFetchGraphQL.mockResolvedValue(mockGraphQLResponse);

        jest.spyOn(utils, 'buildPageQuery').mockReturnValue('mock-page-query');
        jest.spyOn(utils, 'buildQuery').mockReturnValue('mock-query');
        jest.spyOn(utils, 'mapResponseData').mockImplementation((data, keys) => {
            const result: Record<string, any> = {};

            keys.forEach((key) => {
                result[key] = data[`test${key.charAt(0).toUpperCase() + key.slice(1)}`];
            });

            return result;
        });
    });

    afterAll(() => {
        global.fetch = originalFetch;
        jest.restoreAllMocks();
    });

    describe('REST API', () => {
        it('should fetch page successfully using REST API', async () => {
            const pageClient = new PageClient(validConfig, requestOptions);
            const result = await pageClient.get('/test-page');

            expect(mockFetch).toHaveBeenCalledWith(
                'https://demo.dotcms.com/api/v1/page/json/test-page?hostId=test-site',
                requestOptions
            );

            expect(result).toEqual(mockPageData.entity);
        });

        it('should throw error when path is not provided', async () => {
            const pageClient = new PageClient(validConfig, requestOptions);

            await expect(pageClient.get('')).rejects.toThrow(
                "The 'path' parameter is required for the Page API"
            );
        });

        it('should include all provided parameters in the request URL', async () => {
            const pageClient = new PageClient(validConfig, requestOptions);
            const params = {
                siteId: 'custom-site',
                languageId: 2,
                mode: 'PREVIEW_MODE' as const,
                personaId: 'test-persona',
                fireRules: true,
                depth: 2 as const,
                publishDate: '2023-01-01'
            };

            await pageClient.get('/test-page', params);

            expect(mockFetch).toHaveBeenCalledWith(
                expect.stringContaining('https://demo.dotcms.com/api/v1/page/json/test-page?'),
                requestOptions
            );

            const url = mockFetch.mock.calls[0][0];
            expect(url).toContain('hostId=custom-site');
            expect(url).toContain('mode=PREVIEW_MODE');
            expect(url).toContain('language_id=2');
            expect(url).toContain('com.dotmarketing.persona.id=test-persona');
            expect(url).toContain('fireRules=true');
            expect(url).toContain('depth=2');
            expect(url).toContain('publishDate=2023-01-01');
        });

        it('should handle API error responses', async () => {
            mockFetch.mockResolvedValue({
                ok: false,
                status: 404,
                statusText: 'Not Found'
            });

            const pageClient = new PageClient(validConfig, requestOptions);

            await expect(pageClient.get('/not-found')).rejects.toEqual({
                status: 404,
                message: ErrorMessages[404]
            });
        });
    });

    describe('GraphQL API', () => {
        it('should fetch page using GraphQL when query option is provided', async () => {
            const pageClient = new PageClient(validConfig, requestOptions);

            const graphQLOptions: GraphQLPageOptions = {
                graphql: {
                    page: 'fragment PageFields on Page { title url }',
                    content: { content: 'query Content { items { title } }' }
                },
                languageId: '1',
                mode: 'LIVE'
            };

            const result = await pageClient.get('/graphql-page', graphQLOptions);

            expect(utils.buildPageQuery).toHaveBeenCalled();
            expect(utils.buildQuery).toHaveBeenCalledTimes(1);
            expect(utils.fetchGraphQL).toHaveBeenCalledWith({
                body: expect.any(String),
                headers: requestOptions.headers,
                baseURL: 'https://demo.dotcms.com'
            });

            // const pageResponse = graphqlToPageEntity(mockGraphQLResponse |  );
            expect(result).toEqual({
                page: graphqlToPageEntity(mockGraphQLResponse.data),
                content: { content: mockGraphQLResponse.data.testContent },
                errors: null
            });
        });

        it('should pass correct variables to GraphQL query', async () => {
            const pageClient = new PageClient(validConfig, requestOptions);
            const graphQLOptions = {
                graphql: { page: 'fragment PageFields on Page { title }' },
                languageId: '2',
                mode: 'PREVIEW_MODE'
            };

            await pageClient.get('/custom-page', graphQLOptions as any);

            const requestBody = JSON.parse(mockFetchGraphQL.mock.calls[0][0].body);
            expect(requestBody.variables).toEqual({
                url: '/custom-page',
                mode: 'PREVIEW_MODE',
                languageId: '2'
            });
        });

        it('should handle GraphQL errors', async () => {
            mockFetchGraphQL.mockRejectedValue(new Error('GraphQL error'));

            const pageClient = new PageClient(validConfig, requestOptions);
            const graphQLOptions = {
                graphql: { page: 'fragment PageFields on Page { title }' }
            };
            try {
                await pageClient.get('/page', graphQLOptions);
            } catch (error: any) {
                expect(error.message).toBe('Failed to retrieve page data');
            }
        });

        it('should throw errors from GraphQL', async () => {
            mockFetchGraphQL.mockResolvedValue({
                errors: [{ message: 'GraphQL error' }]
            });

            const pageClient = new PageClient(validConfig, requestOptions);
            const graphQLOptions = {
                graphql: { page: 'fragment PageFields on Page { title }' }
            };

            try {
                await pageClient.get('/page', graphQLOptions);
            } catch (error: any) {
                expect(error.message).toBe('Failed to retrieve page data');
            }
        });

        it('should use default values for languageId and mode if not provided', async () => {
            const pageClient = new PageClient(validConfig, requestOptions);
            const graphQLOptions = {
                graphql: { page: 'fragment PageFields on Page { title }' }
            };

            await pageClient.get('/default-page', graphQLOptions);

            const requestBody = JSON.parse(mockFetchGraphQL.mock.calls[0][0].body);
            expect(requestBody.variables).toEqual({
                url: '/default-page',
                mode: 'LIVE',
                languageId: '1'
            });
        });
    });

    describe('Client initialization', () => {
        it('should use siteId from config when not provided in params', async () => {
            const pageClient = new PageClient(validConfig, requestOptions);

            await pageClient.get('/test-page');

            expect(mockFetch).toHaveBeenCalledWith(
                'https://demo.dotcms.com/api/v1/page/json/test-page?hostId=test-site',
                requestOptions
            );
        });
    });
});
