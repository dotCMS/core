import { Mock, MockedFunction, vi } from 'vitest';

import { DotHttpError } from '@dotcms/types';

import { FetchHttpClient } from './fetch-http-client';

import { checkSdkCompatibility } from '../../utils/sdk-compatibility';

// Mock fetch globally
global.fetch = vi.fn();

/** Awaits a promise that must reject and returns its rejection as a DotHttpError. */
async function captureError(promise: Promise<unknown>): Promise<DotHttpError> {
    try {
        await promise;
    } catch (error) {
        return error as DotHttpError;
    }

    throw new Error('Expected the request to reject, but it resolved');
}

vi.mock('../../utils/sdk-compatibility', () => ({
    checkSdkCompatibility: vi.fn()
}));

describe('FetchHttpClient', () => {
    let httpClient: FetchHttpClient;
    let mockFetch: MockedFunction<typeof fetch>;

    beforeEach(() => {
        httpClient = new FetchHttpClient();
        mockFetch = fetch as MockedFunction<typeof fetch>;
        mockFetch.mockClear();
        (checkSdkCompatibility as Mock).mockClear();
    });

    describe('SDK compatibility check', () => {
        it('calls checkSdkCompatibility with the response headers and the SDK version', async () => {
            const mockHeaders = new Headers({
                'content-type': 'application/json',
                'x-dotcms-version': '26.7.13',
                'x-dotcms-min-sdk': '26.5.1'
            });

            mockFetch.mockResolvedValueOnce({
                ok: true,
                headers: mockHeaders,
                json: vi.fn().mockResolvedValue({ data: 'test' })
            } as unknown as Response);

            await httpClient.request('https://api.example.com/test');

            expect(checkSdkCompatibility).toHaveBeenCalledTimes(1);
            expect(checkSdkCompatibility).toHaveBeenCalledWith(mockHeaders, '0.0.0-test');
        });
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
                    json: vi.fn().mockResolvedValue(mockResponse)
                } as unknown as Response);

                const result = await httpClient.request('https://api.example.com/test');

                expect(mockFetch).toHaveBeenCalledWith('https://api.example.com/test', undefined);
                expect(result).toEqual(mockResponse);
            });

            it.each([
                'application/problem+json',
                'application/graphql-response+json; charset=utf-8',
                'Application/JSON'
            ])('should parse a successful %s response as JSON', async (contentType) => {
                const body = { data: 'test' };
                mockFetch.mockResolvedValueOnce({
                    ok: true,
                    status: 200,
                    statusText: 'OK',
                    headers: new Headers({ 'content-type': contentType }),
                    json: vi.fn().mockResolvedValue(body)
                } as unknown as Response);

                const result = await httpClient.request('https://api.example.com/test');

                expect(result).toEqual(body);
            });

            it('should reject a successful response whose content type is not JSON', async () => {
                mockFetch.mockResolvedValueOnce({
                    ok: true,
                    status: 200,
                    statusText: 'OK',
                    url: 'https://api.example.com/test',
                    redirected: false,
                    headers: new Headers({ 'content-type': 'text/html; charset=utf-8' }),
                    text: vi.fn().mockResolvedValue('<html>Not dotCMS</html>')
                } as unknown as Response);

                const error = await captureError(
                    httpClient.request('https://api.example.com/test')
                );

                expect(error).toBeInstanceOf(DotHttpError);
                // Never a 2xx on an error: downstream code forwards error.status as the HTTP
                // status of its own response. The real status stays in the message.
                expect(error.status).toBe(502);
                expect(error.statusText).toBe('Bad Gateway');
                expect(error.message).toContain('(HTTP 200)');
                expect(error.data).toBe('<html>Not dotCMS</html>');
                expect(error.message).toContain('https://api.example.com/test');
                expect(error.message).toContain("'text/html; charset=utf-8'");
                expect(error.message).toContain('not JSON');
                expect(error.message).toContain('dotcmsUrl');
            });

            it('should name both URLs when a successful non-JSON response came from another origin', async () => {
                // e.g. an SSO proxy redirecting the API call to its login page
                mockFetch.mockResolvedValueOnce({
                    ok: true,
                    status: 200,
                    statusText: 'OK',
                    url: 'https://login.example.com/sso',
                    redirected: true,
                    headers: new Headers({ 'content-type': 'text/html' }),
                    text: vi.fn().mockResolvedValue('<form>Sign in</form>')
                } as unknown as Response);

                const error = await captureError(
                    httpClient.request('https://api.example.com/api/v1/nav/')
                );

                expect(error).toBeInstanceOf(DotHttpError);
                expect(error.message).toContain("'https://api.example.com/api/v1/nav/'");
                expect(error.message).toContain("'https://login.example.com/sso'");
                expect(error.message).toContain('dotcmsUrl');
            });

            it('should name the final URL when a successful non-JSON response came from a same-origin redirect', async () => {
                // e.g. the host bouncing an unauthenticated API call to its own login page
                mockFetch.mockResolvedValueOnce({
                    ok: true,
                    status: 200,
                    statusText: 'OK',
                    url: 'https://api.example.com/login',
                    redirected: true,
                    headers: new Headers({ 'content-type': 'text/html' }),
                    text: vi.fn().mockResolvedValue('<form>Sign in</form>')
                } as unknown as Response);

                const error = await captureError(
                    httpClient.request('https://api.example.com/api/v1/nav/')
                );

                expect(error.message).toContain("'https://api.example.com/login'");
                expect(error.message).not.toContain('a different origin');
                // The login page may be dotCMS's own, so don't claim another server answered.
                expect(error.message).not.toContain('another server answered');
                expect(error.message).toContain('something other than the dotCMS API answered');
            });

            it('should handle responses without content-type header', async () => {
                const mockHeaders = new Headers();
                const mockResponse = {
                    ok: true,
                    headers: mockHeaders,
                    text: vi.fn().mockResolvedValue('response without content-type')
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
                    json: vi.fn().mockResolvedValue({ success: true })
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
                    json: vi.fn().mockResolvedValue(errorBody)
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
                    json: vi.fn().mockResolvedValue(errorBody)
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
                    json: vi.fn().mockResolvedValue(errorBody)
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
                    text: vi.fn().mockResolvedValue(errorText)
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
                    json: vi.fn().mockRejectedValue(new Error('Invalid JSON'))
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

            it('should name the requested URL, the final URL and dotcmsUrl when a failed request was redirected to another origin', async () => {
                // The demo.dotcms.com case: http:// is 301'd to https://, the POST does
                // not survive the hop, and the server answers 500 with an empty HTML body.
                mockFetch.mockResolvedValueOnce({
                    ok: false,
                    status: 500,
                    statusText: 'Internal Server Error',
                    url: 'https://demo.dotcms.com/api/v1/graphql',
                    redirected: true,
                    headers: new Headers({ 'content-type': 'text/html' }),
                    text: vi.fn().mockResolvedValue('')
                } as unknown as Response);

                const error = await captureError(
                    httpClient.request('http://demo.dotcms.com/api/v1/graphql', { method: 'POST' })
                );

                expect(error).toBeInstanceOf(DotHttpError);
                expect(error.status).toBe(500);
                expect(error.statusText).toBe('Internal Server Error');
                expect(error.message).toMatch(/^HTTP 500: Internal Server Error/);
                expect(error.message).toContain("'http://demo.dotcms.com/api/v1/graphql'");
                expect(error.message).toContain("'https://demo.dotcms.com/api/v1/graphql'");
                expect(error.message).toContain('dotcmsUrl');
            });

            it('should not blame dotcmsUrl when a failed request was redirected within the same origin', async () => {
                mockFetch.mockResolvedValueOnce({
                    ok: false,
                    status: 500,
                    statusText: 'Internal Server Error',
                    url: 'https://demo.dotcms.com/api/v2/graphql',
                    redirected: true,
                    headers: new Headers({ 'content-type': 'application/json' }),
                    json: vi.fn().mockResolvedValue({ message: 'boom' })
                } as unknown as Response);

                const error = await captureError(
                    httpClient.request('https://demo.dotcms.com/api/v1/graphql')
                );

                expect(error.message).toBe('HTTP 500: Internal Server Error');
            });

            it('should parse a failed application/problem+json body as JSON without a non-JSON hint', async () => {
                const problem = { title: 'Bad Request', status: 400 };
                mockFetch.mockResolvedValueOnce({
                    ok: false,
                    status: 400,
                    statusText: 'Bad Request',
                    headers: new Headers({ 'content-type': 'application/problem+json' }),
                    json: vi.fn().mockResolvedValue(problem)
                } as unknown as Response);

                const error = await captureError(
                    httpClient.request('https://api.example.com/test')
                );

                expect(error.data).toEqual(problem);
                expect(error.message).toBe('HTTP 400: Bad Request');
            });

            it('should report a non-JSON content type on a failed response', async () => {
                // e.g. a proxy or load balancer answering in front of dotCMS
                mockFetch.mockResolvedValueOnce({
                    ok: false,
                    status: 502,
                    statusText: 'Bad Gateway',
                    url: 'https://api.example.com/test',
                    redirected: false,
                    headers: new Headers({ 'content-type': 'text/html' }),
                    text: vi.fn().mockResolvedValue('<html>502 Bad Gateway</html>')
                } as unknown as Response);

                const error = await captureError(
                    httpClient.request('https://api.example.com/test')
                );

                expect(error).toBeInstanceOf(DotHttpError);
                expect(error.status).toBe(502);
                expect(error.data).toBe('<html>502 Bad Gateway</html>');
                expect(error.message).toMatch(/^HTTP 502: Bad Gateway/);
                expect(error.message).toContain("'text/html'");
                expect(error.message).toContain('not JSON');
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
                    json: vi.fn().mockResolvedValue({ message: 'Rate limited' })
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
            it('should reject a successful response with a malformed content-type', async () => {
                mockFetch.mockResolvedValueOnce({
                    ok: true,
                    status: 200,
                    statusText: 'OK',
                    headers: new Headers({ 'content-type': 'invalid-content-type' }),
                    text: vi.fn().mockResolvedValue('response with invalid content-type')
                } as unknown as Response);

                const error = await captureError(
                    httpClient.request('https://api.example.com/test')
                );

                expect(error).toBeInstanceOf(DotHttpError);
                expect(error.message).toContain("'invalid-content-type'");
            });

            it('should handle responses with JSON content-type but non-JSON body', async () => {
                const mockHeaders = new Headers({
                    'content-type': 'application/json'
                });

                const mockResponse = {
                    ok: true,
                    headers: mockHeaders,
                    json: vi.fn().mockRejectedValue(new Error('Invalid JSON'))
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
                    json: vi.fn().mockResolvedValue(null)
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
                    json: vi.fn().mockResolvedValue(mockResponse)
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
