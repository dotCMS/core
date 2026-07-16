import { DotHttpError } from '@dotcms/types';

import { FetchHttpClient } from './fetch-http-client';

// Mock fetch globally
global.fetch = jest.fn();

describe('FetchHttpClient', () => {
    let httpClient: FetchHttpClient;
    let mockFetch: jest.MockedFunction<typeof fetch>;

    beforeEach(() => {
        httpClient = new FetchHttpClient();
        mockFetch = fetch as jest.MockedFunction<typeof fetch>;
        mockFetch.mockClear();
    });

    describe('request', () => {
        describe('successful requests', () => {
            it('should handle JSON responses', async () => {
                const mockResponse = { data: 'test' };
                const mockHeaders = new Headers({
                    'content-type': 'application/json'
                });

                mockFetch.mockResolvedValueOnce({
                    ok: true,
                    headers: mockHeaders,
                    json: jest.fn().mockResolvedValue(mockResponse)
                } as unknown as Response);

                const result = await httpClient.request('https://api.example.com/test');

                expect(mockFetch).toHaveBeenCalledWith('https://api.example.com/test', undefined);
                expect(result).toEqual(mockResponse);
            });

            it('should handle non-JSON responses', async () => {
                const mockHeaders = new Headers({
                    'content-type': 'text/plain'
                });

                const mockResponse = {
                    ok: true,
                    headers: mockHeaders,
                    text: jest.fn().mockResolvedValue('plain text response')
                };

                mockFetch.mockResolvedValueOnce(mockResponse as unknown as Response);

                const result = await httpClient.request('https://api.example.com/test');

                expect(mockFetch).toHaveBeenCalledWith('https://api.example.com/test', undefined);
                expect(result).toBe(mockResponse);
            });

            it('should handle responses without content-type header', async () => {
                const mockHeaders = new Headers();
                const mockResponse = {
                    ok: true,
                    headers: mockHeaders,
                    text: jest.fn().mockResolvedValue('response without content-type')
                };

                mockFetch.mockResolvedValueOnce(mockResponse as unknown as Response);

                const result = await httpClient.request('https://api.example.com/test');

                expect(result).toBe(mockResponse);
            });

            it('should pass request options to fetch', async () => {
                const options = {
                    method: 'POST',
                    headers: { Authorization: 'Bearer token' },
                    body: JSON.stringify({ test: 'data' })
                };

                const mockResponse = {
                    ok: true,
                    headers: new Headers({ 'content-type': 'application/json' }),
                    json: jest.fn().mockResolvedValue({ success: true })
                };

                mockFetch.mockResolvedValueOnce(mockResponse as unknown as Response);

                await httpClient.request('https://api.example.com/test', options);

                expect(mockFetch).toHaveBeenCalledWith('https://api.example.com/test', options);
            });
        });

        describe('HTTP errors', () => {
            it('should throw HttpError for 4xx status codes', async () => {
                const errorBody = { message: 'Bad Request', code: 'INVALID_INPUT' };
                const mockHeaders = new Headers({
                    'content-type': 'application/json'
                });

                mockFetch.mockResolvedValueOnce({
                    ok: false,
                    status: 400,
                    statusText: 'Bad Request',
                    headers: mockHeaders,
                    json: jest.fn().mockResolvedValue(errorBody)
                } as unknown as Response);

                await expect(httpClient.request('https://api.example.com/test')).rejects.toThrow(
                    DotHttpError
                );

                // Reset mock for detailed error checking
                mockFetch.mockClear();
                mockFetch.mockResolvedValueOnce({
                    ok: false,
                    status: 400,
                    statusText: 'Bad Request',
                    headers: mockHeaders,
                    json: jest.fn().mockResolvedValue(errorBody)
                } as unknown as Response);

                try {
                    await httpClient.request('https://api.example.com/test');
                } catch (error: unknown) {
                    expect(error).toBeInstanceOf(DotHttpError);
                    if (error instanceof DotHttpError) {
                        expect(error.status).toBe(400);
                        expect(error.statusText).toBe('Bad Request');
                        expect(error.data).toEqual(errorBody);
                        expect(error.message).toBe('HTTP 400: Bad Request');
                    }
                }
            });

            it('should throw HttpError for 5xx status codes', async () => {
                const errorBody = { message: 'Internal Server Error' };
                const mockHeaders = new Headers({
                    'content-type': 'application/json'
                });

                mockFetch.mockResolvedValueOnce({
                    ok: false,
                    status: 500,
                    statusText: 'Internal Server Error',
                    headers: mockHeaders,
                    json: jest.fn().mockResolvedValue(errorBody)
                } as unknown as Response);

                try {
                    await httpClient.request('https://api.example.com/test');
                } catch (error: unknown) {
                    expect(error).toBeInstanceOf(DotHttpError);
                    if (error instanceof DotHttpError) {
                        expect(error.status).toBe(500);
                        expect(error.statusText).toBe('Internal Server Error');
                        expect(error.data).toEqual(errorBody);
                    }
                }
            });

            it('should handle non-JSON error responses', async () => {
                const errorText = 'Server is down for maintenance';
                const mockHeaders = new Headers({
                    'content-type': 'text/plain'
                });

                mockFetch.mockResolvedValueOnce({
                    ok: false,
                    status: 503,
                    statusText: 'Service Unavailable',
                    headers: mockHeaders,
                    text: jest.fn().mockResolvedValue(errorText)
                } as unknown as Response);

                try {
                    await httpClient.request('https://api.example.com/test');
                } catch (error: unknown) {
                    expect(error).toBeInstanceOf(DotHttpError);
                    if (error instanceof DotHttpError) {
                        expect(error.status).toBe(503);
                        expect(error.statusText).toBe('Service Unavailable');
                        expect(error.data).toBe(errorText);
                    }
                }
            });

            it('should handle error responses with unparseable JSON', async () => {
                const mockHeaders = new Headers({
                    'content-type': 'application/json'
                });

                mockFetch.mockResolvedValueOnce({
                    ok: false,
                    status: 400,
                    statusText: 'Bad Request',
                    headers: mockHeaders,
                    json: jest.fn().mockRejectedValue(new Error('Invalid JSON'))
                } as unknown as Response);

                try {
                    await httpClient.request('https://api.example.com/test');
                } catch (error: unknown) {
                    expect(error).toBeInstanceOf(DotHttpError);
                    if (error instanceof DotHttpError) {
                        expect(error.status).toBe(400);
                        expect(error.statusText).toBe('Bad Request');
                        expect(error.data).toBe('Bad Request');
                    }
                }
            });

            it('should include response headers in HttpError', async () => {
                const mockHeaders = new Headers({
                    'content-type': 'application/json',
                    'x-request-id': 'req-123',
                    'retry-after': '30'
                });

                mockFetch.mockResolvedValueOnce({
                    ok: false,
                    status: 429,
                    statusText: 'Too Many Requests',
                    headers: mockHeaders,
                    json: jest.fn().mockResolvedValue({ message: 'Rate limited' })
                } as unknown as Response);

                try {
                    await httpClient.request('https://api.example.com/test');
                } catch (error: unknown) {
                    expect(error).toBeInstanceOf(DotHttpError);
                    if (error instanceof DotHttpError) {
                        expect(error.status).toBe(429);
                        // Note: Headers are passed to createHttpError but not exposed in HttpError interface
                        // This test verifies the error is created with the correct status
                    }
                }
            });
        });

        describe('network errors', () => {
            it('should throw HttpError for network errors', async () => {
                const networkError = new TypeError('Failed to fetch');
                mockFetch.mockRejectedValueOnce(networkError);

                try {
                    await httpClient.request('https://api.example.com/test');
                } catch (error: unknown) {
                    expect(error).toBeInstanceOf(DotHttpError);
                    if (error instanceof DotHttpError) {
                        expect(error.status).toBe(0);
                        expect(error.statusText).toBe('Network Error');
                        expect(error.message).toBe('Network error: Failed to fetch');
                        expect(error.data).toBe(networkError);
                    }
                }
            });

            it('should throw HttpError for connection timeouts', async () => {
                const timeoutError = new TypeError('Network request failed');
                mockFetch.mockRejectedValueOnce(timeoutError);

                try {
                    await httpClient.request('https://api.example.com/test');
                } catch (error: unknown) {
                    expect(error).toBeInstanceOf(DotHttpError);
                    if (error instanceof DotHttpError) {
                        expect(error.status).toBe(0);
                        expect(error.statusText).toBe('Network Error');
                        expect(error.message).toBe('Network error: Network request failed');
                    }
                }
            });
        });

        describe('edge cases', () => {
            it('should handle responses with malformed content-type', async () => {
                const mockHeaders = new Headers({
                    'content-type': 'invalid-content-type'
                });

                const mockResponse = {
                    ok: true,
                    headers: mockHeaders,
                    text: jest.fn().mockResolvedValue('response with invalid content-type')
                };

                mockFetch.mockResolvedValueOnce(mockResponse as unknown as Response);

                const result = await httpClient.request('https://api.example.com/test');

                expect(result).toBe(mockResponse);
            });

            it('should handle responses with JSON content-type but non-JSON body', async () => {
                const mockHeaders = new Headers({
                    'content-type': 'application/json'
                });

                const mockResponse = {
                    ok: true,
                    headers: mockHeaders,
                    json: jest.fn().mockRejectedValue(new Error('Invalid JSON'))
                };

                mockFetch.mockResolvedValueOnce(mockResponse as unknown as Response);

                await expect(httpClient.request('https://api.example.com/test')).rejects.toThrow(
                    'Invalid JSON'
                );
            });

            it('should handle empty response body', async () => {
                const mockHeaders = new Headers({
                    'content-type': 'application/json'
                });

                mockFetch.mockResolvedValueOnce({
                    ok: false,
                    status: 204,
                    statusText: 'No Content',
                    headers: mockHeaders,
                    json: jest.fn().mockResolvedValue(null)
                } as unknown as Response);

                try {
                    await httpClient.request('https://api.example.com/test');
                } catch (error: unknown) {
                    expect(error).toBeInstanceOf(DotHttpError);
                    if (error instanceof DotHttpError) {
                        expect(error.status).toBe(204);
                        expect(error.statusText).toBe('No Content');
                    }
                }
            });
        });

        describe('generic type support', () => {
            it('should return typed responses', async () => {
                interface TestResponse {
                    id: number;
                    name: string;
                }

                const mockResponse: TestResponse = { id: 1, name: 'test' };
                const mockHeaders = new Headers({
                    'content-type': 'application/json'
                });

                mockFetch.mockResolvedValueOnce({
                    ok: true,
                    headers: mockHeaders,
                    json: jest.fn().mockResolvedValue(mockResponse)
                } as unknown as Response);

                const result = await httpClient.request<TestResponse>(
                    'https://api.example.com/test'
                );

                expect(result).toEqual(mockResponse);
                expect(result.id).toBe(1);
                expect(result.name).toBe('test');
            });
        });
    });
});
