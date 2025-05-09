/* eslint-disable @typescript-eslint/no-explicit-any */
import {
    DotCMSClientConfig,
    DotCMSPageRequestParams,
    RequestOptions,
    DotCMSGraphQLPageResponse
} from '@dotcms/types';

import { PageClient } from './page-api';
import * as utils from './utils';

import { graphqlToPageEntity } from '../../utils';

describe('PageClient', () => {
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
        mockFetchGraphQL.mockReset();
        global.console.error = jest.fn(); // Mock console.error to prevent actual errors from being logged in the console when running tests

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

    afterEach(() => {
        jest.restoreAllMocks();
    });

    describe('GraphQL API', () => {
        it('should fetch page using GraphQL when query option is provided', async () => {
            const pageClient = new PageClient(validConfig, requestOptions);

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

            expect(utils.buildPageQuery).toHaveBeenCalled();
            expect(utils.buildQuery).toHaveBeenCalledTimes(1);
            expect(utils.fetchGraphQL).toHaveBeenCalledWith({
                body: expect.any(String),
                headers: requestOptions.headers,
                baseURL: 'https://demo.dotcms.com'
            });

            expect(result).toEqual({
                pageAsset: graphqlToPageEntity(
                    mockGraphQLResponse.data as unknown as DotCMSGraphQLPageResponse
                ),
                content: { content: mockGraphQLResponse.data.testContent },
                graphql: {
                    query: expect.any(String),
                    variables: expect.any(Object)
                }
            });
        });

        it('should pass correct variables to GraphQL query', async () => {
            const pageClient = new PageClient(validConfig, requestOptions);
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

            const requestBody = JSON.parse(mockFetchGraphQL.mock.calls[0][0].body);
            expect(requestBody.variables).toEqual({
                url: '/custom-page',
                mode: 'PREVIEW_MODE',
                languageId: '2',
                fireRules: false,
                siteId: 'test-site'
            });
        });

        it('should handle GraphQL errors', async () => {
            mockFetchGraphQL.mockRejectedValue(new Error('GraphQL error'));

            const pageClient = new PageClient(validConfig, requestOptions);
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
            } catch (error: any) {
                expect(error.message).toBe('Failed to retrieve page data');
            }
        });

        it('should use default values for languageId and mode if not provided', async () => {
            const pageClient = new PageClient(validConfig, requestOptions);
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

            const requestBody = JSON.parse(mockFetchGraphQL.mock.calls[0][0].body);
            expect(requestBody.variables).toEqual({
                url: '/default-page',
                mode: 'LIVE',
                languageId: '1',
                fireRules: false,
                siteId: 'test-site'
            });
        });

        it('should fetch using graphQL even if there is no graphql option', async () => {
            const pageClient = new PageClient(validConfig, requestOptions);

            await pageClient.get('/why-obi-wan-had-the-high-ground');

            expect(mockFetchGraphQL).toHaveBeenCalled();
            expect(utils.buildPageQuery).toHaveBeenCalled();
            expect(utils.buildQuery).toHaveBeenCalledTimes(1);
            expect(utils.fetchGraphQL).toHaveBeenCalledWith({
                body: expect.stringContaining('"url":"/why-obi-wan-had-the-high-ground"'),
                headers: requestOptions.headers,
                baseURL: 'https://demo.dotcms.com'
            });
        });
    });

    describe('Client initialization', () => {
        it('should use siteId from config when not provided in params', async () => {
            const pageClient = new PageClient(validConfig, requestOptions);

            await pageClient.get('/test-page');

            expect(mockFetchGraphQL).toHaveBeenCalledWith({
                baseURL: 'https://demo.dotcms.com',
                body: expect.stringContaining('"siteId":"test-site"'),
                headers: requestOptions.headers
            });
        });
    });
});
