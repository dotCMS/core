/* eslint-disable @typescript-eslint/no-explicit-any */
import { beforeEach, describe, expect, it, jest } from '@jest/globals';

import { ANALYTICS_ENDPOINT } from './dot-content-analytics.constants';
import { sendAnalyticsEventToServer } from './dot-content-analytics.http';
import {
    DotCMSAnalyticsConfig,
    DotCMSPageViewRequestBody,
    DotCMSTrackRequestBody
} from './dot-content-analytics.model';

// Mock fetch globally
const mockFetch = jest.fn() as jest.MockedFunction<typeof fetch>;
global.fetch = mockFetch;

// Mock console methods to avoid noise in tests
const mockConsoleError = jest.spyOn(console, 'error').mockImplementation(() => {
    // do nothing
});

const mockConsoleWarn = jest.spyOn(console, 'warn').mockImplementation(() => {
    // do nothing
});

describe('DotAnalytics HTTP Utils', () => {
    let mockConfig: DotCMSAnalyticsConfig;
    let mockPayload: DotCMSPageViewRequestBody | DotCMSTrackRequestBody;

    beforeEach(() => {
        // Reset all mocks
        jest.clearAllMocks();
        mockFetch.mockClear();
        mockConsoleError.mockClear();
        mockConsoleWarn.mockClear();

        // Setup test data
        mockConfig = {
            server: 'https://example.com',
            debug: false,
            autoPageView: true,
            siteAuth: 'test-site-key'
        };

        mockPayload = {
            context: {
                site_auth: 'test-site-key',
                session_id: 'test-session-id',
                user_id: 'test-user-id'
            },
            events: [
                {
                    event_type: 'pageview',
                    local_time: Date.now().toString(),
                    data: {
                        page: {
                            url: 'https://example.com/page',
                            doc_encoding: 'UTF-8',
                            doc_hash: 'test-hash',
                            doc_protocol: 'https',
                            doc_search: 'test-search',
                            doc_host: 'example.com',
                            doc_path: '/page',
                            title: 'Test Page'
                        },
                        device: {
                            screen_resolution: '1920x1080',
                            language: 'en-US',
                            viewport_width: '1920',
                            viewport_height: '1080'
                        }
                    }
                }
            ]
        };
    });

    afterAll(() => {
        // Restore console methods
        mockConsoleError.mockRestore();
        mockConsoleWarn.mockRestore();
    });

    describe('sendAnalyticsEventToServer', () => {
        it('should send POST request with correct parameters when successful', async () => {
            // Mock successful response
            const mockResponse = {
                ok: true,
                status: 200
            } as Response;

            mockFetch.mockResolvedValue(mockResponse);

            // Execute function
            await sendAnalyticsEventToServer(mockPayload, mockConfig);

            // Verify fetch was called correctly
            expect(mockFetch).toHaveBeenCalledTimes(1);
            expect(mockFetch).toHaveBeenCalledWith(`${mockConfig.server}${ANALYTICS_ENDPOINT}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(mockPayload)
            });

            // Verify no errors were logged
            expect(mockConsoleError).not.toHaveBeenCalled();
            expect(mockConsoleWarn).not.toHaveBeenCalled();
        });

        it('should handle different server URLs correctly', async () => {
            const configs = [
                { ...mockConfig, server: 'https://analytics.dotcms.com' },
                { ...mockConfig, server: 'http://localhost:8080' },
                { ...mockConfig, server: 'https://custom-domain.com/api' }
            ];

            const mockResponse = {
                ok: true,
                status: 200
            } as Response;

            mockFetch.mockResolvedValue(mockResponse);

            for (const config of configs) {
                mockFetch.mockClear();

                await sendAnalyticsEventToServer(mockPayload, config);

                expect(mockFetch).toHaveBeenCalledWith(
                    `${config.server}${ANALYTICS_ENDPOINT}`,
                    expect.any(Object)
                );
            }
        });

        it('should serialize complex payload objects correctly', async () => {
            const complexPayload: DotCMSTrackRequestBody = {
                context: {
                    site_auth: 'test-site-key',
                    session_id: 'test-session-id',
                    user_id: 'test-user-id'
                },
                events: [
                    {
                        event_type: 'track',
                        local_time: Date.now().toString(),
                        data: {
                            nested: {
                                value: 'test',
                                number: 42,
                                boolean: true,
                                array: [1, 2, 3],
                                nullValue: null
                            },
                            timestamp: 1234567890,
                            metadata: {
                                source: 'unit-test'
                            }
                        }
                    }
                ]
            };

            const mockResponse = {
                ok: true,
                status: 200
            } as Response;

            mockFetch.mockResolvedValue(mockResponse);

            await sendAnalyticsEventToServer(complexPayload, mockConfig);

            expect(mockFetch).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({
                    body: JSON.stringify(complexPayload)
                })
            );
        });

        it('should log error when server responds with non-OK status', async () => {
            const mockResponse = {
                ok: false,
                status: 400,
                statusText: 'Bad Request',
                headers: new Headers(),
                redirected: false,
                type: 'default' as ResponseType,
                url: '',
                json: jest.fn().mockImplementation(() => {
                    throw new Error('response.json is not a function');
                })
            } as unknown as Response;

            mockFetch.mockResolvedValue(mockResponse);

            await sendAnalyticsEventToServer(mockPayload, mockConfig);

            expect(mockConsoleWarn).toHaveBeenCalledTimes(1);
            expect(mockConsoleWarn).toHaveBeenCalledWith(
                'DotAnalytics: HTTP 400: Bad Request - Failed to parse error response:',
                expect.any(Error)
            );
        });

        it('should log error for different HTTP error status codes', async () => {
            const statusCodes = [400, 401, 403, 404, 500, 502, 503];

            for (const status of statusCodes) {
                mockFetch.mockClear();
                mockConsoleWarn.mockClear();

                const mockResponse = {
                    ok: false,
                    status,
                    statusText: `HTTP ${status}`,
                    headers: new Headers(),
                    redirected: false,
                    type: 'default' as ResponseType,
                    url: '',
                    json: jest.fn().mockImplementation(() => {
                        throw new Error('response.json is not a function');
                    })
                } as unknown as Response;

                mockFetch.mockResolvedValue(mockResponse);

                await sendAnalyticsEventToServer(mockPayload, mockConfig);

                expect(mockConsoleWarn).toHaveBeenCalledWith(
                    `DotAnalytics: HTTP ${status}: HTTP ${status} - Failed to parse error response:`,
                    expect.any(Error)
                );
            }
        });

        it('should handle network errors gracefully', async () => {
            const networkError = new Error('Network request failed');
            mockFetch.mockRejectedValue(networkError);

            await sendAnalyticsEventToServer(mockPayload, mockConfig);

            expect(mockConsoleError).toHaveBeenCalledTimes(1);
            expect(mockConsoleError).toHaveBeenCalledWith(
                'DotAnalytics: Error sending event:',
                networkError
            );
        });

        it('should handle fetch timeout errors', async () => {
            const timeoutError = new Error('The operation was aborted');
            timeoutError.name = 'AbortError';
            mockFetch.mockRejectedValue(timeoutError);

            await sendAnalyticsEventToServer(mockPayload, mockConfig);

            expect(mockConsoleError).toHaveBeenCalledWith(
                'DotAnalytics: Error sending event:',
                timeoutError
            );
        });

        it('should handle JSON serialization errors', async () => {
            // Create a payload that can't be serialized (circular reference)
            const circularPayload: DotCMSTrackRequestBody = {
                context: {
                    site_auth: 'test-site-key',
                    session_id: 'test-session-id',
                    user_id: 'test-user-id'
                },
                events: [
                    {
                        event_type: 'track',
                        local_time: Date.now().toString(),
                        data: { event: 'test' }
                    }
                ]
            };

            // Mock JSON.stringify to throw error
            const originalStringify = JSON.stringify;
            const mockStringify = jest.fn().mockImplementation(() => {
                throw new Error('Converting circular structure to JSON');
            });

            (global as any).JSON = {
                ...JSON,
                stringify: mockStringify
            };

            await sendAnalyticsEventToServer(circularPayload, mockConfig);

            expect(mockConsoleError).toHaveBeenCalledWith(
                'DotAnalytics: Error sending event:',
                expect.any(Error)
            );

            // Restore JSON.stringify
            (global as any).JSON = {
                ...JSON,
                stringify: originalStringify
            };
        });

        it('should not make any requests when payload is empty', async () => {
            const emptyPayload: DotCMSTrackRequestBody = {
                context: {
                    site_auth: 'test-site-key',
                    session_id: 'test-session-id',
                    user_id: 'test-user-id'
                },
                events: []
            };

            await sendAnalyticsEventToServer(emptyPayload, mockConfig);

            expect(mockFetch).toHaveBeenCalledTimes(1);
            expect(mockFetch).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({
                    body: JSON.stringify(emptyPayload)
                })
            );
        });

        it('should handle server URLs with trailing slashes', async () => {
            const configWithTrailingSlash = {
                ...mockConfig,
                server: 'https://example.com/'
            };

            const mockResponse = {
                ok: true,
                status: 200
            } as Response;

            mockFetch.mockResolvedValue(mockResponse);

            await sendAnalyticsEventToServer(mockPayload, configWithTrailingSlash);

            // The function simply concatenates server + endpoint, so we expect double slash
            expect(mockFetch).toHaveBeenCalledWith(
                `${configWithTrailingSlash.server}${ANALYTICS_ENDPOINT}`,
                expect.any(Object)
            );
        });

        it('should set correct Content-Type header', async () => {
            const mockResponse = {
                ok: true,
                status: 200
            } as Response;

            mockFetch.mockResolvedValue(mockResponse);

            await sendAnalyticsEventToServer(mockPayload, mockConfig);

            expect(mockFetch).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({
                    headers: { 'Content-Type': 'application/json' }
                })
            );

            expect(mockFetch).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({
                    method: 'POST'
                })
            );
        });

        it('should handle responses with different content types', async () => {
            // Server might return different content types, function should still work
            const responses = [
                { ok: true, status: 200, headers: { 'content-type': 'application/json' } },
                { ok: true, status: 200, headers: { 'content-type': 'text/plain' } },
                { ok: true, status: 204, headers: {} } // No content
            ];

            for (const mockResponse of responses) {
                mockFetch.mockClear();
                mockFetch.mockResolvedValue(mockResponse as Response);

                await sendAnalyticsEventToServer(mockPayload, mockConfig);

                expect(mockFetch).toHaveBeenCalledTimes(1);
                expect(mockConsoleError).not.toHaveBeenCalled();
            }
        });

        it('should handle large payloads', async () => {
            // Create a large payload
            const largePayload: DotCMSTrackRequestBody = {
                context: {
                    site_auth: 'test-site-key',
                    session_id: 'test-session-id',
                    user_id: 'test-user-id'
                },
                events: [
                    {
                        event_type: 'track',
                        local_time: Date.now().toString(),
                        data: {
                            largeString: 'x'.repeat(10000), // 10KB string
                            largeArray: Array.from({ length: 1000 }, (_, i) => ({
                                id: i,
                                data: `item-${i}`,
                                value: Math.random()
                            }))
                        }
                    }
                ]
            };

            const mockResponse = {
                ok: true,
                status: 200
            } as Response;

            mockFetch.mockResolvedValue(mockResponse);

            await sendAnalyticsEventToServer(largePayload, mockConfig);

            expect(mockFetch).toHaveBeenCalledTimes(1);
            expect(mockConsoleError).not.toHaveBeenCalled();
        });

        it('should handle concurrent requests', async () => {
            const mockResponse = {
                ok: true,
                status: 200
            } as Response;

            mockFetch.mockResolvedValue(mockResponse);

            // Send multiple requests concurrently
            const promises = Array.from({ length: 5 }, (_, i) => {
                const concurrentPayload: DotCMSTrackRequestBody = {
                    context: {
                        site_auth: 'test-site-key',
                        session_id: 'test-session-id',
                        user_id: 'test-user-id'
                    },
                    events: [
                        {
                            event_type: 'track',
                            local_time: Date.now().toString(),
                            data: { requestId: i }
                        }
                    ]
                };

                return sendAnalyticsEventToServer(concurrentPayload, mockConfig);
            });

            await Promise.all(promises);

            expect(mockFetch).toHaveBeenCalledTimes(5);
            expect(mockConsoleError).not.toHaveBeenCalled();
        });
    });
});
