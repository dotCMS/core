/* eslint-disable @typescript-eslint/no-explicit-any */
import { beforeEach, describe, expect, it, jest } from '@jest/globals';

import { ANALYTICS_ENDPOINT } from './constants';
import { sendAnalyticsEvent } from './dot-content-analytics.http';
import {
    DotCMSAnalyticsConfig,
    DotCMSCustomEventRequestBody,
    DotCMSPageViewRequestBody
} from './models';

// Type alias for backward compatibility with tests
type DotCMSTrackRequestBody = DotCMSCustomEventRequestBody;

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

    describe('sendAnalyticsEvent', () => {
        describe('Happy Path', () => {
            it('should send POST request with correct parameters when successful', async () => {
                // Mock successful response
                const mockResponse = {
                    ok: true,
                    status: 200
                } as Response;

                mockFetch.mockResolvedValue(mockResponse);

                // Execute function
                await sendAnalyticsEvent(mockPayload, mockConfig); // defaults to keepalive=false

                // Verify fetch was called correctly
                expect(mockFetch).toHaveBeenCalledTimes(1);
                expect(mockFetch).toHaveBeenCalledWith(
                    `${mockConfig.server}${ANALYTICS_ENDPOINT}`,
                    {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(mockPayload)
                    }
                );

                // Verify no errors were logged
                expect(mockConsoleError).not.toHaveBeenCalled();
                expect(mockConsoleWarn).not.toHaveBeenCalled();
            });

            it('should send POST request with keepalive and credentials omit when keepalive=true', async () => {
                // Mock successful response
                const mockResponse = {
                    ok: true,
                    status: 200
                } as Response;

                mockFetch.mockResolvedValue(mockResponse);

                // Execute function with keepalive
                await sendAnalyticsEvent(mockPayload, mockConfig, true);

                // Verify fetch was called with keepalive options
                expect(mockFetch).toHaveBeenCalledTimes(1);
                expect(mockFetch).toHaveBeenCalledWith(
                    `${mockConfig.server}${ANALYTICS_ENDPOINT}`,
                    {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(mockPayload),
                        keepalive: true,
                        credentials: 'omit'
                    }
                );

                // Verify no errors were logged
                expect(mockConsoleError).not.toHaveBeenCalled();
                expect(mockConsoleWarn).not.toHaveBeenCalled();
            });
        });

        describe('Debug Mode', () => {
            it('should log debug information when debug mode is enabled', async () => {
                const debugConfig = { ...mockConfig, debug: true };
                const mockResponse = {
                    ok: true,
                    status: 200
                } as Response;

                mockFetch.mockResolvedValue(mockResponse);

                await sendAnalyticsEvent(mockPayload, debugConfig); // defaults to keepalive=false

                expect(mockConsoleWarn).toHaveBeenCalledWith(
                    `DotCMS Analytics: Sending ${mockPayload.events.length} event(s)`,
                    { payload: mockPayload }
                );
            });

            it('should log keepalive mode in debug information when keepalive=true', async () => {
                const debugConfig = { ...mockConfig, debug: true };
                const mockResponse = {
                    ok: true,
                    status: 200
                } as Response;

                mockFetch.mockResolvedValue(mockResponse);

                await sendAnalyticsEvent(mockPayload, debugConfig, true);

                expect(mockConsoleWarn).toHaveBeenCalledWith(
                    `DotCMS Analytics: Sending ${mockPayload.events.length} event(s) (keepalive)`,
                    { payload: mockPayload }
                );
            });

            it('should not log debug information when debug mode is disabled', async () => {
                const mockResponse = {
                    ok: true,
                    status: 200
                } as Response;

                mockFetch.mockResolvedValue(mockResponse);

                await sendAnalyticsEvent(mockPayload, mockConfig); // defaults to keepalive=false

                // Should not log the body
                expect(mockConsoleWarn).not.toHaveBeenCalled();
            });
        });

        describe('Error Handling - HTTP Errors', () => {
            it('should log server error message when response contains valid JSON with message', async () => {
                const errorMessage = 'Invalid site key provided';
                const mockResponse = {
                    ok: false,
                    status: 400,
                    statusText: 'Bad Request',
                    json: jest.fn().mockResolvedValue({ message: errorMessage })
                } as unknown as Response;

                mockFetch.mockResolvedValue(mockResponse);

                await sendAnalyticsEvent(mockPayload, mockConfig); // defaults to keepalive=false

                expect(mockConsoleWarn).toHaveBeenCalledTimes(1);
                expect(mockConsoleWarn).toHaveBeenCalledWith(
                    `DotCMS Analytics: ${errorMessage} (HTTP 400: Bad Request)`
                );
            });

            it('should log appropriate message when error response has no message property', async () => {
                const mockResponse = {
                    ok: false,
                    status: 500,
                    statusText: 'Internal Server Error',
                    json: jest.fn().mockResolvedValue({ error: 'some error', code: 'ERR_500' })
                } as unknown as Response;

                mockFetch.mockResolvedValue(mockResponse);

                await sendAnalyticsEvent(mockPayload, mockConfig); // defaults to keepalive=false

                expect(mockConsoleWarn).toHaveBeenCalledTimes(1);
                expect(mockConsoleWarn).toHaveBeenCalledWith(
                    'DotCMS Analytics: HTTP 500: Internal Server Error - No error message in response'
                );
            });

            it('should log error when response JSON parsing fails', async () => {
                const mockResponse = {
                    ok: false,
                    status: 400,
                    statusText: 'Bad Request',
                    json: jest.fn().mockRejectedValue(new Error('Invalid JSON'))
                } as unknown as Response;

                mockFetch.mockResolvedValue(mockResponse);

                await sendAnalyticsEvent(mockPayload, mockConfig); // defaults to keepalive=false

                expect(mockConsoleWarn).toHaveBeenCalledTimes(1);
                expect(mockConsoleWarn).toHaveBeenCalledWith(
                    'DotCMS Analytics: HTTP 400: Bad Request - Failed to parse error response:',
                    expect.any(Error)
                );
            });
        });

        describe('Error Handling - Network Errors', () => {
            it('should handle network errors gracefully', async () => {
                const networkError = new Error('Network request failed');
                mockFetch.mockRejectedValue(networkError);

                await sendAnalyticsEvent(mockPayload, mockConfig); // defaults to keepalive=false

                expect(mockConsoleError).toHaveBeenCalledTimes(1);
                expect(mockConsoleError).toHaveBeenCalledWith(
                    'DotCMS Analytics: Error sending event:',
                    networkError
                );
            });
        });
    });
});
