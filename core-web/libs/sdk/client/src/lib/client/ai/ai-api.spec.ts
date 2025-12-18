/// <reference types="jest" />

import {
    DotCMSClientConfig,
    DotRequestOptions,
    DotCMSAISearchParams,
    DotCMSBasicContentlet,
    DISTANCE_FUNCTIONS
} from '@dotcms/types';

import { AIClient } from './ai-api';
import { AISearch } from './search/search';

import { FetchHttpClient } from '../adapters/fetch-http-client';

// Mock dependencies
jest.mock('../adapters/fetch-http-client');
jest.mock('./search/search');

describe('AIClient', () => {
    const MockedFetchHttpClient = FetchHttpClient as jest.MockedClass<typeof FetchHttpClient>;
    const MockedAISearch = AISearch as jest.MockedClass<typeof AISearch>;

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

    beforeEach(() => {
        jest.clearAllMocks();
        MockedFetchHttpClient.mockImplementation(
            () =>
                ({
                    request: jest.fn()
                }) as Partial<FetchHttpClient> as FetchHttpClient
        );
    });

    describe('initialization', () => {
        it('should create an AIClient instance with valid configuration', () => {
            const aiClient = new AIClient(validConfig, requestOptions, new FetchHttpClient());

            expect(aiClient).toBeDefined();
            expect(aiClient).toBeInstanceOf(AIClient);
        });

        it('should create an AIClient instance without optional siteId', () => {
            const configWithoutSite: DotCMSClientConfig = {
                dotcmsUrl: 'https://demo.dotcms.com',
                authToken: 'test-token'
            };

            const aiClient = new AIClient(configWithoutSite, requestOptions, new FetchHttpClient());

            expect(aiClient).toBeDefined();
            expect(aiClient).toBeInstanceOf(AIClient);
        });

        it('should create an AIClient instance with empty request options', () => {
            const aiClient = new AIClient(validConfig, {}, new FetchHttpClient());

            expect(aiClient).toBeDefined();
            expect(aiClient).toBeInstanceOf(AIClient);
        });
    });

    describe('search', () => {
        it('should return an AISearch instance', () => {
            const httpClient = new FetchHttpClient();
            const aiClient = new AIClient(validConfig, requestOptions, httpClient);

            const result = aiClient.search('machine learning', 'default');

            expect(result).toBeInstanceOf(AISearch);
        });

        it('should pass correct parameters to AISearch with default params', () => {
            const httpClient = new FetchHttpClient();
            const aiClient = new AIClient(validConfig, requestOptions, httpClient);
            const prompt = 'artificial intelligence';
            const indexName = 'content_index';

            aiClient.search(prompt, indexName);

            expect(MockedAISearch).toHaveBeenCalledWith(
                expect.objectContaining({
                    dotcmsUrl: validConfig.dotcmsUrl,
                    authToken: validConfig.authToken,
                    siteId: validConfig.siteId
                }),
                requestOptions,
                httpClient,
                prompt,
                indexName,
                {}
            );
        });

        it('should pass correct parameters to AISearch with custom params', () => {
            const httpClient = new FetchHttpClient();
            const aiClient = new AIClient(validConfig, requestOptions, httpClient);
            const prompt = 'deep learning tutorials';
            const indexName = 'tutorials_index';
            const params: DotCMSAISearchParams = {
                query: {
                    limit: 25,
                    offset: 5,
                    contentType: 'Tutorial',
                    languageId: '1'
                },
                config: {
                    threshold: 0.8,
                    distanceFunction: DISTANCE_FUNCTIONS.innerProduct,
                    responseLength: 512
                }
            };

            aiClient.search(prompt, indexName, params);

            expect(MockedAISearch).toHaveBeenCalledWith(
                expect.objectContaining({
                    dotcmsUrl: validConfig.dotcmsUrl,
                    authToken: validConfig.authToken,
                    siteId: validConfig.siteId
                }),
                requestOptions,
                httpClient,
                prompt,
                indexName,
                params
            );
        });

        it('should create different AISearch instances for different searches', () => {
            const httpClient = new FetchHttpClient();
            const aiClient = new AIClient(validConfig, requestOptions, httpClient);

            aiClient.search('machine learning', 'index1');
            aiClient.search('data science', 'index2');
            aiClient.search('neural networks', 'index3');

            expect(MockedAISearch).toHaveBeenCalledTimes(3);
            expect(MockedAISearch).toHaveBeenNthCalledWith(
                1,
                expect.anything(),
                expect.anything(),
                expect.anything(),
                'machine learning',
                'index1',
                {}
            );
            expect(MockedAISearch).toHaveBeenNthCalledWith(
                2,
                expect.anything(),
                expect.anything(),
                expect.anything(),
                'data science',
                'index2',
                {}
            );
            expect(MockedAISearch).toHaveBeenNthCalledWith(
                3,
                expect.anything(),
                expect.anything(),
                expect.anything(),
                'neural networks',
                'index3',
                {}
            );
        });

        it('should support generic type parameter for contentlet type', () => {
            interface CustomContentlet extends DotCMSBasicContentlet {
                customField: string;
                category: string;
            }

            const httpClient = new FetchHttpClient();
            const aiClient = new AIClient(validConfig, requestOptions, httpClient);

            // This test mainly verifies TypeScript compilation works correctly
            const result = aiClient.search<CustomContentlet>('test prompt', 'default');

            expect(result).toBeInstanceOf(AISearch);
            expect(MockedAISearch).toHaveBeenCalledWith(
                expect.anything(),
                expect.anything(),
                httpClient,
                'test prompt',
                'default',
                {}
            );
        });

        it('should handle prompt with special characters', () => {
            const httpClient = new FetchHttpClient();
            const aiClient = new AIClient(validConfig, requestOptions, httpClient);
            const prompt = 'search: "artificial intelligence" AND machine learning';

            aiClient.search(prompt, 'default');

            expect(MockedAISearch).toHaveBeenCalledWith(
                expect.anything(),
                expect.anything(),
                expect.anything(),
                prompt,
                'default',
                {}
            );
        });

        it('should handle empty prompt string', () => {
            const httpClient = new FetchHttpClient();
            const aiClient = new AIClient(validConfig, requestOptions, httpClient);

            aiClient.search('', 'default');

            expect(MockedAISearch).toHaveBeenCalledWith(
                expect.anything(),
                expect.anything(),
                expect.anything(),
                '',
                'default',
                {}
            );
        });

        it('should handle search with only query params', () => {
            const httpClient = new FetchHttpClient();
            const aiClient = new AIClient(validConfig, requestOptions, httpClient);
            const params: DotCMSAISearchParams = {
                query: {
                    limit: 100,
                    contentType: 'BlogPost'
                }
            };

            aiClient.search('blog posts', 'blog_index', params);

            expect(MockedAISearch).toHaveBeenCalledWith(
                expect.anything(),
                expect.anything(),
                expect.anything(),
                'blog posts',
                'blog_index',
                params
            );
        });

        it('should handle search with only config params', () => {
            const httpClient = new FetchHttpClient();
            const aiClient = new AIClient(validConfig, requestOptions, httpClient);
            const params: DotCMSAISearchParams = {
                config: {
                    threshold: 0.9,
                    responseLength: 2048
                }
            };

            aiClient.search('high precision search', 'default', params);

            expect(MockedAISearch).toHaveBeenCalledWith(
                expect.anything(),
                expect.anything(),
                expect.anything(),
                'high precision search',
                'default',
                params
            );
        });

        it.each([
            ['cosine', DISTANCE_FUNCTIONS.cosine],
            ['innerProduct', DISTANCE_FUNCTIONS.innerProduct],
            ['L2', DISTANCE_FUNCTIONS.L2]
        ])('should handle %s distance function', (_, distanceFunction) => {
            const httpClient = new FetchHttpClient();
            const aiClient = new AIClient(validConfig, requestOptions, httpClient);

            aiClient.search('test', 'default', {
                config: { distanceFunction }
            });

            expect(MockedAISearch).toHaveBeenCalledWith(
                expect.anything(),
                expect.anything(),
                expect.anything(),
                'test',
                'default',
                { config: { distanceFunction } }
            );
        });
    });
});
