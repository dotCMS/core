/// <reference types="jest" />

import {
    DotRequestOptions,
    DotCMSClientConfig,
    DotHttpError,
    DotErrorAISearch,
    DotCMSAISearchParams,
    DotCMSBasicContentlet,
    DISTANCE_FUNCTIONS
} from '@dotcms/types';

import { AISearch } from './search';

import { FetchHttpClient } from '../../adapters/fetch-http-client';
import { DotCMSAISearchResponse } from '../shared/types';

// Mock the FetchHttpClient
jest.mock('../../adapters/fetch-http-client');

describe('AISearch', () => {
    const mockRequest = jest.fn();
    const MockedFetchHttpClient = FetchHttpClient as jest.MockedClass<typeof FetchHttpClient>;

    const requestOptions: DotRequestOptions = {
        cache: 'no-cache' // To simulate a valid request
    };

    const config: DotCMSClientConfig = {
        dotcmsUrl: 'http://localhost:8080',
        authToken: 'test-token',
        siteId: 'test-site'
    };

    const baseRequest = {
        method: 'GET',
        ...requestOptions
    };

    const mockResponseData: DotCMSAISearchResponse<DotCMSBasicContentlet> = {
        dotCMSResults: [
            {
                identifier: '123',
                title: 'Test Content',
                archived: false,
                baseType: 'CONTENT',
                contentType: 'TestType',
                folder: 'folder-id',
                hasLiveVersion: true,
                hasTitleImage: false,
                host: 'test-site',
                hostName: 'test-site-name',
                inode: 'inode-123',
                languageId: 1,
                live: true,
                locked: false,
                modDate: '2023-01-01',
                modUser: 'test-user',
                modUserName: 'Test User',
                owner: 'test-owner',
                sortOrder: 0,
                stInode: 'st-inode',
                titleImage: 'off',
                url: '/test-content',
                working: true,
                matches: [
                    {
                        distance: 0.85,
                        extractedText: 'Sample text content'
                    }
                ]
            } as DotCMSBasicContentlet
        ]
    };

    beforeEach(() => {
        mockRequest.mockReset();
        MockedFetchHttpClient.mockImplementation(
            () =>
                ({
                    request: mockRequest
                }) as Partial<FetchHttpClient> as FetchHttpClient
        );

        mockRequest.mockResolvedValue(mockResponseData);
    });

    it('should initialize with valid configuration', () => {
        const prompt = 'machine learning';
        const params: DotCMSAISearchParams = {};
        const aiSearch = new AISearch(
            config,
            requestOptions,
            new FetchHttpClient(),
            params,
            prompt
        );
        expect(aiSearch).toBeDefined();
    });

    describe('successful requests', () => {
        it('should build a query for a basic AI search with default parameters', async () => {
            const prompt = 'machine learning articles';
            const params: DotCMSAISearchParams = {};
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            await aiSearch;

            const expectedUrl = new URL('http://localhost:8080/api/v1/ai/search');
            expectedUrl.searchParams.append('searchLimit', '1000');
            expectedUrl.searchParams.append('searchOffset', '0');
            expectedUrl.searchParams.append('site', 'test-site');
            expectedUrl.searchParams.append('indexName', 'default');
            expectedUrl.searchParams.append('threshold', '0.5');
            expectedUrl.searchParams.append('distanceFunction', '<=>');
            expectedUrl.searchParams.append('responseLength', '1024');
            expectedUrl.searchParams.append('query', 'machine learning articles');

            expect(mockRequest).toHaveBeenCalledWith(expectedUrl.toString(), {
                ...baseRequest,
                headers: {}
            });
        });

        it('should return the AI search results in the response', async () => {
            const prompt = 'artificial intelligence';
            const params: DotCMSAISearchParams = {};
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            const response = await aiSearch;

            expect(response).toEqual(mockResponseData);
            expect(response.dotCMSResults).toHaveLength(1);
            expect(response.dotCMSResults[0].title).toBe('Test Content');
        });

        it('should build a query with custom query parameters', async () => {
            const prompt = 'technology trends';
            const params: DotCMSAISearchParams = {
                query: {
                    limit: 50,
                    offset: 10,
                    contentType: 'BlogPost',
                    languageId: '2',
                    indexName: 'custom_index'
                }
            };
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            await aiSearch;

            const expectedUrl = new URL('http://localhost:8080/api/v1/ai/search');
            expectedUrl.searchParams.append('searchLimit', '50');
            expectedUrl.searchParams.append('searchOffset', '10');
            expectedUrl.searchParams.append('site', 'test-site');
            expectedUrl.searchParams.append('language', '2');
            expectedUrl.searchParams.append('contentType', 'BlogPost');
            expectedUrl.searchParams.append('indexName', 'custom_index');
            expectedUrl.searchParams.append('threshold', '0.5');
            expectedUrl.searchParams.append('distanceFunction', '<=>');
            expectedUrl.searchParams.append('responseLength', '1024');
            expectedUrl.searchParams.append('query', 'technology trends');

            expect(mockRequest).toHaveBeenCalledWith(expectedUrl.toString(), {
                ...baseRequest,
                headers: {}
            });
        });

        it('should build a query with custom AI configuration', async () => {
            const prompt = 'data science';
            const params: DotCMSAISearchParams = {
                ai: {
                    threshold: 0.75,
                    distanceFunction: DISTANCE_FUNCTIONS.innerProduct,
                    responseLength: 2048
                }
            };
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            await aiSearch;

            const expectedUrl = new URL('http://localhost:8080/api/v1/ai/search');
            expectedUrl.searchParams.append('searchLimit', '1000');
            expectedUrl.searchParams.append('searchOffset', '0');
            expectedUrl.searchParams.append('site', 'test-site');
            expectedUrl.searchParams.append('indexName', 'default');
            expectedUrl.searchParams.append('threshold', '0.75');
            expectedUrl.searchParams.append('distanceFunction', '<#>');
            expectedUrl.searchParams.append('responseLength', '2048');
            expectedUrl.searchParams.append('query', 'data science');

            expect(mockRequest).toHaveBeenCalledWith(expectedUrl.toString(), {
                ...baseRequest,
                headers: {}
            });
        });

        it('should build a query with both custom query and AI parameters', async () => {
            const prompt = 'deep learning tutorials';
            const params: DotCMSAISearchParams = {
                query: {
                    limit: 25,
                    offset: 5,
                    contentType: 'Tutorial',
                    languageId: '1',
                    siteId: 'my-site',
                    indexName: 'tutorials_index'
                },
                ai: {
                    threshold: 0.8,
                    distanceFunction: DISTANCE_FUNCTIONS.innerProduct,
                    responseLength: 512
                }
            };
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            await aiSearch;

            const expectedUrl = new URL('http://localhost:8080/api/v1/ai/search');
            expectedUrl.searchParams.append('searchLimit', '25');
            expectedUrl.searchParams.append('searchOffset', '5');
            expectedUrl.searchParams.append('site', 'my-site');
            expectedUrl.searchParams.append('language', '1');
            expectedUrl.searchParams.append('contentType', 'Tutorial');
            expectedUrl.searchParams.append('indexName', 'tutorials_index');
            expectedUrl.searchParams.append('threshold', '0.8');
            expectedUrl.searchParams.append('distanceFunction', '<#>');
            expectedUrl.searchParams.append('responseLength', '512');
            expectedUrl.searchParams.append('query', 'deep learning tutorials');

            expect(mockRequest).toHaveBeenCalledWith(expectedUrl.toString(), {
                ...baseRequest,
                headers: {}
            });
        });

        it('should override default siteId with user-provided siteId', async () => {
            const prompt = 'test prompt';
            const params: DotCMSAISearchParams = {
                query: {
                    siteId: 'custom-site'
                }
            };
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            await aiSearch;

            const expectedUrl = new URL('http://localhost:8080/api/v1/ai/search');
            expectedUrl.searchParams.append('searchLimit', '1000');
            expectedUrl.searchParams.append('searchOffset', '0');
            expectedUrl.searchParams.append('site', 'custom-site');
            expectedUrl.searchParams.append('indexName', 'default');
            expectedUrl.searchParams.append('threshold', '0.5');
            expectedUrl.searchParams.append('distanceFunction', '<=>');
            expectedUrl.searchParams.append('responseLength', '1024');
            expectedUrl.searchParams.append('query', 'test prompt');

            expect(mockRequest).toHaveBeenCalledWith(expectedUrl.toString(), {
                ...baseRequest,
                headers: {}
            });
        });

        it('should handle onfulfilled callback', async () => {
            const prompt = 'callback test';
            const params: DotCMSAISearchParams = {};
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            const onfulfilledCallback = jest.fn((data) => {
                expect(data).toEqual(mockResponseData);
                return data;
            });

            await aiSearch.then(onfulfilledCallback);

            expect(onfulfilledCallback).toHaveBeenCalledWith(mockResponseData);
        });

        it('should return original data when onfulfilled callback returns undefined', async () => {
            const prompt = 'undefined callback test';
            const params: DotCMSAISearchParams = {};
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            const onfulfilledCallback = jest.fn((_data) => {
                return undefined as unknown as DotCMSAISearchResponse<DotCMSBasicContentlet>;
            });

            const result = await aiSearch.then(onfulfilledCallback);

            expect(onfulfilledCallback).toHaveBeenCalledWith(mockResponseData);
            expect(result).toEqual(mockResponseData);
        });

        it('should handle special characters in prompt', async () => {
            const prompt = 'search: "artificial intelligence" AND machine learning';
            const params: DotCMSAISearchParams = {};
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            await aiSearch;

            const calledUrl = mockRequest.mock.calls[0][0];
            const url = new URL(calledUrl);
            expect(url.searchParams.get('query')).toBe(
                'search: "artificial intelligence" AND machine learning'
            );
        });
    });

    describe('fetch is rejected', () => {
        it('should trigger onrejected callback with generic error', (done) => {
            const prompt = 'error test';
            const params: DotCMSAISearchParams = {};
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            // Mock the request to return a rejected promise
            mockRequest.mockRejectedValue(new Error('Network connection failed'));

            aiSearch.then(
                () => {
                    fail('Expected onrejected callback to be called');
                },
                (error) => {
                    expect(error).toBeInstanceOf(DotErrorAISearch);
                    if (error instanceof DotErrorAISearch) {
                        expect(error.prompt).toBe('error test');
                        expect(error.params).toEqual(params);
                        expect(error.message).toBe(
                            "AI Search failed for 'error test' (fetch): Network connection failed"
                        );
                        expect(error.httpError).toBeUndefined();
                    }
                    done();
                    return error;
                }
            );
        });

        it('should trigger catch method with generic error', (done) => {
            const prompt = 'catch test';
            const params: DotCMSAISearchParams = {
                query: {
                    contentType: 'Article'
                }
            };
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            // Mock the request to return a rejected promise
            mockRequest.mockRejectedValue(new Error('Request timeout'));

            aiSearch.then().catch((error) => {
                expect(error).toBeInstanceOf(DotErrorAISearch);
                if (error instanceof DotErrorAISearch) {
                    expect(error.prompt).toBe('catch test');
                    expect(error.params).toEqual(params);
                    expect(error.message).toBe(
                        "AI Search failed for 'catch test' (fetch): Request timeout"
                    );
                    expect(error.httpError).toBeUndefined();
                }
                done();
            });
        });

        it('should trigger catch of try catch block with generic error', async () => {
            const prompt = 'try catch test';
            const params: DotCMSAISearchParams = {};
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            // Mock a network error
            mockRequest.mockRejectedValue(new Error('DNS resolution failed'));

            try {
                await aiSearch;
                fail('Expected DotErrorAISearch to be thrown');
            } catch (e) {
                expect(e).toBeInstanceOf(DotErrorAISearch);
                if (e instanceof DotErrorAISearch) {
                    expect(e.prompt).toBe('try catch test');
                    expect(e.params).toEqual(params);
                    expect(e.message).toBe(
                        "AI Search failed for 'try catch test' (fetch): DNS resolution failed"
                    );
                    expect(e.httpError).toBeUndefined();
                }
            }
        });

        it('should throw DotErrorAISearch when HTTP request fails with 404', async () => {
            const prompt = 'http error test';
            const params: DotCMSAISearchParams = {
                query: {
                    indexName: 'non_existent_index'
                }
            };
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            const httpError = new DotHttpError({
                status: 404,
                statusText: 'Not Found',
                message: 'AI search endpoint not found',
                data: { error: 'Index does not exist' }
            });

            // Mock the request to throw an HttpError
            mockRequest.mockRejectedValue(httpError);

            try {
                await aiSearch;
                fail('Expected DotErrorAISearch to be thrown');
            } catch (error) {
                expect(error).toBeInstanceOf(DotErrorAISearch);
                if (error instanceof DotErrorAISearch) {
                    expect(error.prompt).toBe('http error test');
                    expect(error.params).toEqual(params);
                    expect(error.httpError).toBe(httpError);
                    expect(error.message).toBe(
                        "AI Search failed for 'http error test' (fetch): AI search endpoint not found"
                    );
                }
            }
        });

        it('should throw DotErrorAISearch when HTTP request fails with 500', async () => {
            const prompt = 'server error test';
            const params: DotCMSAISearchParams = {
                ai: {
                    threshold: 0.9
                }
            };
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            const httpError = new DotHttpError({
                status: 500,
                statusText: 'Internal Server Error',
                message: 'AI service unavailable',
                data: { error: 'Internal server error' }
            });

            // Mock the request to throw an HttpError
            mockRequest.mockRejectedValue(httpError);

            try {
                await aiSearch;
                fail('Expected DotErrorAISearch to be thrown');
            } catch (error) {
                expect(error).toBeInstanceOf(DotErrorAISearch);
                if (error instanceof DotErrorAISearch) {
                    expect(error.prompt).toBe('server error test');
                    expect(error.params).toEqual(params);
                    expect(error.httpError).toBe(httpError);
                    expect(error.message).toBe(
                        "AI Search failed for 'server error test' (fetch): AI service unavailable"
                    );
                }
            }
        });

        it('should handle HttpError in onrejected callback', (done) => {
            const prompt = 'onrejected http error test';
            const params: DotCMSAISearchParams = {
                query: {
                    languageId: '2'
                }
            };
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            const httpError = new DotHttpError({
                status: 403,
                statusText: 'Forbidden',
                message: 'Access denied to AI search',
                data: { error: 'Insufficient permissions' }
            });

            // Mock the request to throw an HttpError
            mockRequest.mockRejectedValue(httpError);

            aiSearch.then(
                (response) => {
                    fail('Expected onrejected callback to be called');
                    return response;
                },
                (error) => {
                    expect(error).toBeInstanceOf(DotErrorAISearch);
                    if (error instanceof DotErrorAISearch) {
                        expect(error.prompt).toBe('onrejected http error test');
                        expect(error.params).toEqual(params);
                        expect(error.httpError).toBe(httpError);
                        expect(error.message).toBe(
                            "AI Search failed for 'onrejected http error test' (fetch): Access denied to AI search"
                        );
                    }
                    done();
                    return error;
                }
            );
        });

        it('should return original error when onrejected callback returns undefined', async () => {
            const prompt = 'undefined error callback test';
            const params: DotCMSAISearchParams = {};
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            mockRequest.mockRejectedValue(new Error('Test error'));

            const onrejectedCallback = jest.fn((_error) => {
                return undefined as unknown as DotErrorAISearch;
            });

            const result = await aiSearch.then(undefined, onrejectedCallback);

            expect(onrejectedCallback).toHaveBeenCalled();
            expect(result).toBeInstanceOf(DotErrorAISearch);
        });

        it('should handle non-Error objects in rejection', async () => {
            const prompt = 'non-error rejection test';
            const params: DotCMSAISearchParams = {};
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            // Mock the request to reject with a non-Error object
            mockRequest.mockRejectedValue('String error');

            try {
                await aiSearch;
                fail('Expected DotErrorAISearch to be thrown');
            } catch (error) {
                expect(error).toBeInstanceOf(DotErrorAISearch);
                if (error instanceof DotErrorAISearch) {
                    expect(error.prompt).toBe('non-error rejection test');
                    expect(error.message).toBe(
                        "AI Search failed for 'non-error rejection test' (fetch): Unknown error"
                    );
                    expect(error.httpError).toBeUndefined();
                }
            }
        });
    });

    describe('parameter handling', () => {
        it('should not include undefined query parameters in URL', async () => {
            const prompt = 'minimal params';
            const params: DotCMSAISearchParams = {
                query: {
                    // Only some parameters defined
                    limit: 100
                }
            };
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            await aiSearch;

            const calledUrl = mockRequest.mock.calls[0][0];
            const url = new URL(calledUrl);

            expect(url.searchParams.get('searchLimit')).toBe('100');
            expect(url.searchParams.get('searchOffset')).toBe('0');
            expect(url.searchParams.get('site')).toBe('test-site');
            expect(url.searchParams.get('indexName')).toBe('default');
            expect(url.searchParams.get('query')).toBe('minimal params');
            // Should not have contentType or language since they weren't provided
            expect(url.searchParams.has('contentType')).toBe(false);
            expect(url.searchParams.has('language')).toBe(false);
        });

        it('should handle empty params object', async () => {
            const prompt = 'empty params test';
            const params: DotCMSAISearchParams = {};
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            await aiSearch;

            const calledUrl = mockRequest.mock.calls[0][0];
            const url = new URL(calledUrl);

            // Should have default values
            expect(url.searchParams.get('searchLimit')).toBe('1000');
            expect(url.searchParams.get('searchOffset')).toBe('0');
            expect(url.searchParams.get('site')).toBe('test-site');
            expect(url.searchParams.get('indexName')).toBe('default');
            expect(url.searchParams.get('threshold')).toBe('0.5');
            expect(url.searchParams.get('distanceFunction')).toBe('<=>');
            expect(url.searchParams.get('responseLength')).toBe('1024');
            expect(url.searchParams.get('query')).toBe('empty params test');
        });

        it('should handle numeric values as strings in URL params', async () => {
            const prompt = 'numeric test';
            const params: DotCMSAISearchParams = {
                query: {
                    limit: 50,
                    offset: 10,
                    languageId: '2'
                },
                ai: {
                    threshold: 0.85,
                    responseLength: 2048
                }
            };
            const aiSearch = new AISearch(
                config,
                requestOptions,
                new FetchHttpClient(),
                params,
                prompt
            );

            await aiSearch;

            const calledUrl = mockRequest.mock.calls[0][0];
            const url = new URL(calledUrl);

            expect(url.searchParams.get('searchLimit')).toBe('50');
            expect(url.searchParams.get('searchOffset')).toBe('10');
            expect(url.searchParams.get('language')).toBe('2');
            expect(url.searchParams.get('threshold')).toBe('0.85');
            expect(url.searchParams.get('responseLength')).toBe('2048');
        });
    });
});
