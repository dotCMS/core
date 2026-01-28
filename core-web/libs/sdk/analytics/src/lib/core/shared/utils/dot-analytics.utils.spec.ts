/* eslint-disable @typescript-eslint/no-explicit-any */
import { beforeEach, describe, expect, it } from '@jest/globals';

import {
    defaultRedirectFn,
    enrichPagePayloadOptimized,
    extractUTMParameters,
    generateSecureId,
    getAnalyticsConfig,
    getAnalyticsContext,
    getBrowserEventData,
    getDeviceData,
    getLocalTime,
    getPageData,
    getSessionId,
    getUserId,
    getUtmData,
    validateAnalyticsConfig
} from './dot-analytics.utils';

import { ANALYTICS_MINIFIED_SCRIPT_NAME } from '../constants/dot-analytics.constants';
import { DotCMSAnalyticsConfig } from '../models';

describe('Analytics Utils', () => {
    let mockLocation: Location;

    beforeAll(() => {
        jest.useFakeTimers({ doNotFake: [] });
        jest.setSystemTime(new Date('2024-01-01T00:00:00Z'));
    });

    beforeEach(() => {
        // Mock Location object
        mockLocation = {
            href: 'https://example.com/page?param=1',
            pathname: '/page',
            hostname: 'example.com',
            protocol: 'https:',
            hash: '#section1',
            search: '?param=1',
            origin: 'https://example.com'
        } as Location;

        // Clean up any previous script tags
        document.querySelectorAll('script').forEach((script) => script.remove());
    });

    describe('validateAnalyticsConfig', () => {
        it('should return null when all required fields are present', () => {
            const validConfig: DotCMSAnalyticsConfig = {
                server: 'https://example.com',
                siteAuth: 'test-auth-key',
                debug: false,
                autoPageView: false
            };

            const result = validateAnalyticsConfig(validConfig);

            expect(result).toBeNull();
        });

        it('should return ["siteAuth"] when siteAuth is missing', () => {
            const invalidConfig: DotCMSAnalyticsConfig = {
                server: 'https://example.com',
                siteAuth: '',
                debug: false,
                autoPageView: false
            };

            const result = validateAnalyticsConfig(invalidConfig);

            expect(result).toEqual(['"siteAuth"']);
        });

        it('should return ["server"] when server is missing', () => {
            const invalidConfig: DotCMSAnalyticsConfig = {
                server: '',
                siteAuth: 'test-auth-key',
                debug: false,
                autoPageView: false
            };

            const result = validateAnalyticsConfig(invalidConfig);

            expect(result).toEqual(['"server"']);
        });

        it('should return both fields when both are missing', () => {
            const invalidConfig: DotCMSAnalyticsConfig = {
                server: '',
                siteAuth: '',
                debug: false,
                autoPageView: false
            };

            const result = validateAnalyticsConfig(invalidConfig);

            expect(result).toEqual(['"siteAuth"', '"server"']);
        });

        it('should treat whitespace-only strings as invalid', () => {
            const invalidConfig: DotCMSAnalyticsConfig = {
                server: '   ',
                siteAuth: '  ',
                debug: false,
                autoPageView: false
            };

            const result = validateAnalyticsConfig(invalidConfig);

            expect(result).toEqual(['"siteAuth"', '"server"']);
        });

        it('should handle undefined values', () => {
            const invalidConfig: DotCMSAnalyticsConfig = {
                server: undefined as any,
                siteAuth: undefined as any,
                debug: false,
                autoPageView: false
            };

            const result = validateAnalyticsConfig(invalidConfig);

            expect(result).toEqual(['"siteAuth"', '"server"']);
        });

        it('should validate only siteAuth when server is valid but siteAuth is whitespace', () => {
            const invalidConfig: DotCMSAnalyticsConfig = {
                server: 'https://example.com',
                siteAuth: '   ',
                debug: false,
                autoPageView: false
            };

            const result = validateAnalyticsConfig(invalidConfig);

            expect(result).toEqual(['"siteAuth"']);
        });
    });

    describe('getAnalyticsConfig', () => {
        beforeEach(() => {
            const script = document.createElement('script');
            script.setAttribute('src', `https://example.com/${ANALYTICS_MINIFIED_SCRIPT_NAME}`);
            script.setAttribute('data-analytics-server', 'https://analytics.dotcms.com');
            script.setAttribute('data-analytics-auth', 'test-key');
            document.body.appendChild(script);
        });

        it('should return default values when attributes are not set', () => {
            const result = getAnalyticsConfig();

            expect(result).toEqual({
                server: 'https://analytics.dotcms.com',
                debug: false,
                autoPageView: false,
                siteAuth: 'test-key'
            });
        });

        it('should enable debug when debug attribute is true', () => {
            const script = document.querySelector('script[data-analytics-auth]');
            script?.setAttribute('data-analytics-debug', 'true');

            const result = getAnalyticsConfig();

            expect(result).toEqual({
                server: 'https://analytics.dotcms.com',
                debug: true,
                autoPageView: false,
                siteAuth: 'test-key'
            });
        });

        it('should enable autoPageView when auto-page-view attribute is true', () => {
            const script = document.querySelector('script[data-analytics-auth]');
            script?.setAttribute('data-analytics-auto-page-view', 'true');

            const result = getAnalyticsConfig();

            expect(result).toEqual({
                server: 'https://analytics.dotcms.com',
                debug: false,
                autoPageView: true,
                siteAuth: 'test-key'
            });
        });

        it('should handle all attributes together', () => {
            const script = document.querySelector('script[data-analytics-auth]');
            script?.setAttribute('data-analytics-debug', 'true');
            script?.setAttribute('data-analytics-auto-page-view', 'true');
            script?.setAttribute('data-analytics-auth', 'custom-site-key');

            const result = getAnalyticsConfig();

            expect(result).toEqual({
                server: 'https://analytics.dotcms.com',
                debug: true,
                autoPageView: true,
                siteAuth: 'custom-site-key'
            });
        });

        it('should use window.location.origin when data-analytics-server is missing', () => {
            // Clear existing scripts
            document.querySelectorAll('script').forEach((script) => script.remove());

            const script = document.createElement('script');
            script.setAttribute('src', `https://example.com/${ANALYTICS_MINIFIED_SCRIPT_NAME}`);
            script.setAttribute('data-analytics-auth', 'test-key');
            // No data-analytics-server attribute
            document.body.appendChild(script);

            const result = getAnalyticsConfig();

            expect(result).toEqual({
                server: window.location.origin,
                debug: false,
                autoPageView: false,
                siteAuth: 'test-key'
            });
        });

        it('should return defaults when no analytics script is found', () => {
            // Clear all scripts
            document.querySelectorAll('script').forEach((script) => script.remove());

            const result = getAnalyticsConfig();

            expect(result).toEqual({
                server: window.location.origin,
                debug: false,
                autoPageView: false,
                siteAuth: ''
            });
        });

        it('should ignore scripts without ca.min.js in src', () => {
            // Clear existing scripts
            document.querySelectorAll('script').forEach((script) => script.remove());

            // Add a script without ca.min.js but with data-analytics-auth
            const wrongScript = document.createElement('script');
            wrongScript.setAttribute('src', 'https://example.com/other-script.js');
            wrongScript.setAttribute('data-analytics-auth', 'wrong-key');
            document.body.appendChild(wrongScript);

            const result = getAnalyticsConfig();

            // Should return defaults since no analytics script was found
            expect(result).toEqual({
                server: window.location.origin,
                debug: false,
                autoPageView: false,
                siteAuth: ''
            });
        });

        it('should handle debug and autoPageView with non-true values', () => {
            const script = document.querySelector('script[data-analytics-auth]');
            script?.setAttribute('data-analytics-debug', 'false');
            script?.setAttribute('data-analytics-auto-page-view', 'false');

            const result = getAnalyticsConfig();

            expect(result).toEqual({
                server: 'https://analytics.dotcms.com',
                debug: false,
                autoPageView: false,
                siteAuth: 'test-key'
            });
        });

        it('should parse queue batch size from JSON config', () => {
            const script = document.querySelector('script[data-analytics-auth]');
            script?.setAttribute(
                'data-analytics-config',
                JSON.stringify({
                    queue: { eventBatchSize: 5 }
                })
            );

            const result = getAnalyticsConfig();

            expect(result).toEqual({
                server: 'https://analytics.dotcms.com',
                debug: false,
                autoPageView: false,
                siteAuth: 'test-key',
                queue: {
                    eventBatchSize: 5
                }
            });
        });

        it('should parse queue flush interval from JSON config', () => {
            const script = document.querySelector('script[data-analytics-auth]');
            script?.setAttribute(
                'data-analytics-config',
                JSON.stringify({
                    queue: { flushInterval: 2000 }
                })
            );

            const result = getAnalyticsConfig();

            expect(result).toEqual({
                server: 'https://analytics.dotcms.com',
                debug: false,
                autoPageView: false,
                siteAuth: 'test-key',
                queue: {
                    flushInterval: 2000
                }
            });
        });

        it('should parse both queue config values from JSON', () => {
            const script = document.querySelector('script[data-analytics-auth]');
            script?.setAttribute(
                'data-analytics-config',
                JSON.stringify({
                    queue: {
                        eventBatchSize: 3,
                        flushInterval: 1500
                    }
                })
            );

            const result = getAnalyticsConfig();

            expect(result).toEqual({
                server: 'https://analytics.dotcms.com',
                debug: false,
                autoPageView: false,
                siteAuth: 'test-key',
                queue: {
                    eventBatchSize: 3,
                    flushInterval: 1500
                }
            });
        });

        it('should ignore invalid batch size values in JSON (string instead of number)', () => {
            const script = document.querySelector('script[data-analytics-auth]');
            script?.setAttribute(
                'data-analytics-config',
                JSON.stringify({
                    queue: { eventBatchSize: 'invalid' }
                })
            );

            const result = getAnalyticsConfig();

            // Queue object should not be created if no valid properties exist
            expect(result).toEqual({
                server: 'https://analytics.dotcms.com',
                debug: false,
                autoPageView: false,
                siteAuth: 'test-key'
            });
        });

        it('should ignore invalid flush interval values in JSON (string instead of number)', () => {
            const script = document.querySelector('script[data-analytics-auth]');
            script?.setAttribute(
                'data-analytics-config',
                JSON.stringify({
                    queue: { flushInterval: 'abc' }
                })
            );

            const result = getAnalyticsConfig();

            expect(result).toEqual({
                server: 'https://analytics.dotcms.com',
                debug: false,
                autoPageView: false,
                siteAuth: 'test-key'
            });
        });

        it('should parse valid batch size but ignore invalid flush interval in JSON', () => {
            const script = document.querySelector('script[data-analytics-auth]');
            script?.setAttribute(
                'data-analytics-config',
                JSON.stringify({
                    queue: {
                        eventBatchSize: 10,
                        flushInterval: 'invalid'
                    }
                })
            );

            const result = getAnalyticsConfig();

            expect(result).toEqual({
                server: 'https://analytics.dotcms.com',
                debug: false,
                autoPageView: false,
                siteAuth: 'test-key',
                queue: {
                    eventBatchSize: 10
                }
            });
        });

        it('should parse impressions toggle from data attribute', () => {
            const script = document.querySelector('script[data-analytics-auth]');
            script?.setAttribute('data-analytics-impressions', 'true');

            const result = getAnalyticsConfig();

            expect(result.impressions).toBe(true);
        });

        it('should parse clicks toggle from data attribute', () => {
            const script = document.querySelector('script[data-analytics-auth]');
            script?.setAttribute('data-analytics-clicks', 'true');

            const result = getAnalyticsConfig();

            expect(result.clicks).toBe(true);
        });

        it('should parse granular impression configuration from JSON in data-analytics-config', () => {
            const script = document.querySelector('script[data-analytics-auth]');
            if (script) {
                script.setAttribute('data-analytics-auto-page-view', 'true');
                script.setAttribute(
                    'data-analytics-config',
                    '{"queue": {"eventBatchSize": 15, "flushInterval": 5000}, "impressions": {"visibilityThreshold": 0.8}}'
                );
                document.head.appendChild(script);

                const config = getAnalyticsConfig();
                expect(config.queue).toEqual({ eventBatchSize: 15, flushInterval: 5000 });
                expect(config.impressions).toEqual({ visibilityThreshold: 0.8 });
            }
        });

        it('should perform boolean override: disable impressions via attribute even if config is present', () => {
            const script = document.querySelector('script[data-analytics-auth]');
            script?.setAttribute('data-analytics-impressions', 'false');
            script?.setAttribute(
                'data-analytics-config',
                '{"impressions": {"visibilityThreshold": 0.8}}'
            );

            const result = getAnalyticsConfig();

            // Explicit attribute 'false' overrides check (merge logic depends on implementation)
            // Current impl: ...(impressionsAttr && { impressions: impressionsAttr === 'true' })
            // So if attr is present and 'false', it sets impressions: false, overriding spread ...advancedConfig
            expect(result.impressions).toBe(false);
        });

        it('should ignore invalid numeric values in impression config via JSON', () => {
            const script = document.querySelector('script[data-analytics-auth]');
            script?.setAttribute(
                'data-analytics-config',
                JSON.stringify({
                    impressions: {
                        visibilityThreshold: 'invalid',
                        dwellMs: 100
                    }
                })
            );

            const result = getAnalyticsConfig();

            // Should contain valid dwellMs but ignore invalid visibilityThreshold
            expect(result.impressions).toEqual({ dwellMs: 100 });
        });

        it('should handle apostrophes in valid JSON configuration without corruption', () => {
            const script = document.querySelector('script[data-analytics-auth]');
            // Clear explicit server attribute to let JSON config win for 'server' field
            script?.setAttribute('data-analytics-server', '');

            const configWithApostrophe = {
                server: "https://arcadio's-server.com"
            };
            script?.setAttribute('data-analytics-config', JSON.stringify(configWithApostrophe));

            const result = getAnalyticsConfig();

            expect(result.server).toBe("https://arcadio's-server.com");
        });

        it('should handle legacy VTL single-quoted JSON via fallback', () => {
            const script = document.querySelector('script[data-analytics-auth]');
            // Clear explicit server attribute to let JSON config win for 'server' field
            script?.setAttribute('data-analytics-server', '');

            // Simulating VTL output: { 'server': 'https://vtl.com' }
            script?.setAttribute(
                'data-analytics-config',
                "{ 'server': 'https://vtl-server.com', 'impressions': { 'dwellMs': 500 } }"
            );

            const result = getAnalyticsConfig();

            expect(result.server).toBe('https://vtl-server.com');
            expect(result.impressions).toEqual({ dwellMs: 500 });
        });

        it('should ensure explicit attributes override JSON config values (debug, autoPageView, server, and siteAuth)', () => {
            const script = document.querySelector('script[data-analytics-auth]');
            script?.setAttribute('data-analytics-debug', 'true');
            script?.setAttribute('data-analytics-auto-page-view', 'false');
            script?.setAttribute('data-analytics-server', 'https://explicit.server.com');
            script?.setAttribute('data-analytics-auth', 'explicit-site-key');
            script?.setAttribute(
                'data-analytics-config',
                JSON.stringify({
                    debug: false,
                    autoPageView: true,
                    server: 'https://json.server.com',
                    siteAuth: 'json-site-key'
                })
            );

            const result = getAnalyticsConfig();

            expect(result.debug).toBe(true);
            expect(result.autoPageView).toBe(false);
            expect(result.server).toBe('https://explicit.server.com');
            expect(result.siteAuth).toBe('explicit-site-key');
        });
    });

    describe('getBrowserEventData', () => {
        beforeEach(() => {
            mockLocation = {
                href: 'https://example.com/page',
                pathname: '/page',
                hostname: 'example.com',
                protocol: 'https:',
                hash: '#section1',
                search: '?param=1',
                origin: 'https://example.com'
            } as Location;

            // Mock window properties
            Object.defineProperty(window, 'innerWidth', { value: 1024 });
            Object.defineProperty(window, 'innerHeight', { value: 768 });
            Object.defineProperty(window.screen, 'width', { value: 1920 });
            Object.defineProperty(window.screen, 'height', { value: 1080 });

            // Mock navigator
            Object.defineProperty(navigator, 'language', { value: 'es-ES' });
            Object.defineProperty(navigator, 'userAgent', { value: 'test-agent' });

            // Mock document properties
            Object.defineProperty(document, 'title', { value: 'Test Page' });
            Object.defineProperty(document, 'referrer', { value: 'https://referrer.com' });
            Object.defineProperty(document, 'characterSet', { value: 'UTF-8' });
        });

        it('should create page view data with basic properties', () => {
            const result = getBrowserEventData(mockLocation);

            const mockDate = new Date('2024-01-01T00:00:00Z');
            const expectedOffset = mockDate.getTimezoneOffset();

            expect(result).toEqual(
                expect.objectContaining({
                    local_tz_offset: expectedOffset,
                    page_title: 'Test Page',
                    doc_path: '/page',
                    doc_host: 'example.com',
                    doc_protocol: 'https:',
                    doc_hash: '#section1',
                    doc_search: '?param=1',
                    screen_resolution: '1920x1080',
                    vp_size: '1024x768',
                    user_language: 'es-ES',
                    doc_encoding: 'UTF-8',
                    referrer: 'https://referrer.com',
                    utc_time: expect.any(String),
                    url: 'https://example.com/page',
                    utm: expect.any(Object)
                })
            );
        });
    });

    describe('extractUTMParameters', () => {
        const mockLocation = (search: string): Location => ({
            ...window.location,
            search
        });

        it('should return an empty object when no UTM parameters are present', () => {
            const location = mockLocation('');
            const result = extractUTMParameters(location);
            expect(result).toEqual({});
        });

        it('should extract UTM parameters correctly', () => {
            const location = mockLocation(
                '?utm_source=google&utm_medium=cpc&utm_campaign=spring_sale'
            );
            const result = extractUTMParameters(location);
            expect(result).toEqual({
                source: 'google',
                medium: 'cpc',
                campaign: 'spring_sale'
            });
        });

        it('should ignore non-UTM parameters', () => {
            const location = mockLocation('?utm_source=google&non_utm_param=value');
            const result = extractUTMParameters(location);
            expect(result).toEqual({
                source: 'google'
            });
        });

        it('should handle missing UTM parameters gracefully', () => {
            const location = mockLocation('?utm_source=google&utm_campaign=spring_sale');
            const result = extractUTMParameters(location);
            expect(result).toEqual({
                source: 'google',
                campaign: 'spring_sale'
            });
        });

        it('should handle all expected UTM parameters', () => {
            const location = mockLocation(
                '?utm_source=google&utm_medium=cpc&utm_campaign=spring_sale&utm_term=test&utm_content=ad1'
            );
            const result = extractUTMParameters(location);
            expect(result).toEqual({
                source: 'google',
                medium: 'cpc',
                campaign: 'spring_sale',
                term: 'test',
                content: 'ad1'
            });
        });
    });

    describe('defaultRedirectFn', () => {
        const originalLocation = window.location;

        beforeEach(() => {
            // Mock window.location
            delete (window as any).location;
            (window as any).location = { ...originalLocation };
        });

        afterEach(() => {
            (window as any).location = originalLocation;
        });

        it('should update window.location.href with provided URL', () => {
            const testUrl = 'https://test.com';
            defaultRedirectFn(testUrl);
            expect(window.location.href).toBe(testUrl);
        });
    });

    // NEW TESTS FOR MISSING FUNCTIONS

    describe('generateSecureId', () => {
        it('should generate unique IDs with given prefix', () => {
            const id1 = generateSecureId('test');
            const id2 = generateSecureId('test');

            expect(id1).toMatch(/^test_\d+_[a-z0-9]+$/);
            expect(id2).toMatch(/^test_\d+_[a-z0-9]+$/);
            expect(id1).not.toBe(id2);
        });

        it('should handle different prefixes', () => {
            const userId = generateSecureId('user');
            const sessionId = generateSecureId('session');

            expect(userId).toMatch(/^user_/);
            expect(sessionId).toMatch(/^session_/);
        });

        it('should include timestamp in generated ID', () => {
            jest.setSystemTime(new Date('2024-01-01T12:00:00Z'));
            const id = generateSecureId('test');

            expect(id).toContain('1704110400000'); // timestamp
        });
    });

    describe('getUserId', () => {
        const mockLocalStorage = {
            getItem: jest.fn(),
            setItem: jest.fn()
        };

        beforeEach(() => {
            Object.defineProperty(window, 'localStorage', {
                value: mockLocalStorage,
                writable: true
            });
            mockLocalStorage.getItem.mockClear();
            mockLocalStorage.setItem.mockClear();
        });

        it('should return existing user ID from localStorage', () => {
            const existingId = 'user_12345_abc';
            mockLocalStorage.getItem.mockReturnValue(existingId);

            const result = getUserId();

            expect(result).toBe(existingId);
            expect(mockLocalStorage.getItem).toHaveBeenCalledWith('dot_analytics_user_id');
        });

        it('should generate new user ID when none exists', () => {
            mockLocalStorage.getItem.mockReturnValue(null);

            const result = getUserId();

            expect(result).toMatch(/^user_\d+_[a-z0-9]+$/);
            expect(mockLocalStorage.setItem).toHaveBeenCalledWith('dot_analytics_user_id', result);
        });
    });

    describe('getSessionId', () => {
        const mockSessionStorage = {
            getItem: jest.fn(),
            setItem: jest.fn()
        };

        beforeEach(() => {
            Object.defineProperty(window, 'sessionStorage', {
                value: mockSessionStorage,
                writable: true
            });
            mockSessionStorage.getItem.mockClear();
            mockSessionStorage.setItem.mockClear();

            // Mock window.location for UTM extraction
            Object.defineProperty(window, 'location', {
                value: {
                    search: '?utm_source=test'
                },
                writable: true
            });
        });

        it('should generate new session ID when none exists', () => {
            mockSessionStorage.getItem.mockReturnValue(null);

            const result = getSessionId();

            expect(result).toMatch(/^session_\d+_[a-z0-9]+$/);
            expect(mockSessionStorage.setItem).toHaveBeenCalledTimes(1);
        });

        it('should return existing valid session ID', () => {
            const sessionData = {
                sessionId: 'session_12345_abc',
                startTime: Date.now() - 1000, // 1 second ago
                lastActivity: Date.now() - 1000
            };
            mockSessionStorage.getItem.mockReturnValue(JSON.stringify(sessionData));

            const result = getSessionId();

            expect(result).toBe('session_12345_abc');
        });

        it('should create new session when session is expired', () => {
            const sessionData = {
                sessionId: 'session_12345_abc',
                startTime: Date.now() - 31 * 60 * 1000, // 31 minutes ago
                lastActivity: Date.now() - 31 * 60 * 1000 // 31 minutes ago
            };
            mockSessionStorage.getItem.mockReturnValue(JSON.stringify(sessionData));

            const result = getSessionId();

            expect(result).toMatch(/^session_\d+_[a-z0-9]+$/);
            expect(result).not.toBe('session_12345_abc');
        });
    });

    describe('getLocalTime', () => {
        it('should return local time in ISO 8601 format with timezone offset', () => {
            const result = getLocalTime();

            // Should match ISO 8601 format with timezone offset without milliseconds
            // Examples: "2024-01-01T12:30:45Z", "2024-01-01T07:30:45-05:00", "2024-01-01T13:30:45+01:00"
            expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{2}:\d{2}$/);
        });

        it('should handle formatter fallback for older browsers', () => {
            const originalIntl = global.Intl;
            global.Intl = undefined as any;

            const result = getLocalTime();

            // When Intl is not available, it still should include timezone offset without milliseconds
            expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{2}:\d{2}$/);

            global.Intl = originalIntl;
        });

        it('should return correct offset for UTC timezone', () => {
            const originalGetTimezoneOffset = Date.prototype.getTimezoneOffset;
            Date.prototype.getTimezoneOffset = jest.fn().mockReturnValue(0);

            const result = getLocalTime();

            expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\+00:00$/);

            // Restore original method
            Date.prototype.getTimezoneOffset = originalGetTimezoneOffset;
        });

        it('should return correct offset for different timezones', () => {
            const originalGetTimezoneOffset = Date.prototype.getTimezoneOffset;
            Date.prototype.getTimezoneOffset = jest.fn().mockReturnValue(300); // UTC-5

            const result = getLocalTime();

            expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}-05:00$/);

            // Restore original method
            Date.prototype.getTimezoneOffset = originalGetTimezoneOffset;
        });

        it('should return correct offset for positive timezone', () => {
            const originalGetTimezoneOffset = Date.prototype.getTimezoneOffset;
            Date.prototype.getTimezoneOffset = jest.fn().mockReturnValue(-120); // UTC+2

            const result = getLocalTime();

            expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\+02:00$/);

            // Restore original method
            Date.prototype.getTimezoneOffset = originalGetTimezoneOffset;
        });
    });

    describe('getPageData', () => {
        it('should extract page data from browser event data and payload', () => {
            const browserData = {
                url: 'https://example.com/page',
                doc_encoding: 'UTF-8',
                page_title: 'Test Page',
                doc_path: '/page',
                doc_host: 'example.com',
                doc_protocol: 'https:',
                doc_hash: '#section',
                doc_search: '?param=1',
                utm: {},
                referrer: 'https://referrer.com'
            } as any;

            const payload = {
                properties: {
                    width: 1024,
                    height: 768,
                    title: 'Test Page'
                }
            } as any;

            const result = getPageData(browserData, payload);

            expect(result).toEqual({
                url: 'https://example.com/page',
                path: '/page',
                hash: '#section',
                search: '?param=1',
                title: 'Test Page',
                width: '1024',
                height: '768',
                referrer: 'https://referrer.com'
            });
        });
    });

    describe('getDeviceData', () => {
        it('should extract device data from browser event data', () => {
            const browserData = {
                screen_resolution: '1920x1080',
                user_language: 'en-US',
                vp_size: '1024x768'
            } as any;

            const result = getDeviceData(browserData);

            expect(result).toEqual({
                screen_resolution: '1920x1080',
                language: 'en-US',
                viewport_width: '1024',
                viewport_height: '768'
            });
        });
    });

    describe('getUtmData', () => {
        it('should extract UTM data from browser event data', () => {
            const browserData = {
                utm: {
                    source: 'google',
                    medium: 'cpc',
                    campaign: 'spring_sale',
                    term: 'shoes',
                    content: 'ad1'
                }
            } as any;

            const result = getUtmData(browserData);

            expect(result).toEqual({
                source: 'google',
                medium: 'cpc',
                campaign: 'spring_sale',
                term: 'shoes',
                content: 'ad1'
            });
        });

        it('should return empty object when no UTM data exists or is empty', () => {
            const browserData = { utm: {} } as any;

            const result = getUtmData(browserData);

            expect(result).toEqual({});
        });

        it('should handle partial UTM data', () => {
            const browserData = {
                utm: {
                    source: 'facebook',
                    campaign: 'summer'
                }
            } as any;

            const result = getUtmData(browserData);

            expect(result).toEqual({
                source: 'facebook',
                campaign: 'summer'
            });
        });
    });

    describe('getAnalyticsContext', () => {
        const mockLocalStorage = {
            getItem: jest.fn(),
            setItem: jest.fn()
        };

        const mockSessionStorage = {
            getItem: jest.fn(),
            setItem: jest.fn()
        };

        beforeEach(() => {
            Object.defineProperty(window, 'localStorage', {
                value: mockLocalStorage,
                writable: true
            });
            Object.defineProperty(window, 'sessionStorage', {
                value: mockSessionStorage,
                writable: true
            });
            Object.defineProperty(window, 'location', {
                value: { search: '' },
                writable: true
            });

            mockLocalStorage.getItem.mockClear();
            mockSessionStorage.getItem.mockClear();
        });

        it('should return analytics context with session, user IDs, and device data', () => {
            mockLocalStorage.getItem.mockReturnValue('user_12345');

            const sessionData = {
                sessionId: 'session_67890',
                startTime: Date.now() - 1000,
                lastActivity: Date.now() - 1000
            };
            mockSessionStorage.getItem.mockReturnValue(JSON.stringify(sessionData));

            const config = { siteAuth: 'test-site', debug: false } as any;

            const result = getAnalyticsContext(config);

            expect(result).toEqual({
                site_auth: 'test-site',
                session_id: 'session_67890',
                user_id: 'user_12345',
                device: {
                    screen_resolution: '1920x1080',
                    language: 'es-ES',
                    viewport_width: '1024',
                    viewport_height: '768'
                }
            });
        });
    });

    describe('enrichPagePayloadOptimized', () => {
        beforeEach(() => {
            Object.defineProperty(window, 'location', {
                value: {
                    href: 'https://example.com/page',
                    pathname: '/page',
                    hostname: 'example.com',
                    protocol: 'https:',
                    hash: '#section',
                    search: '?utm_source=google'
                },
                writable: true
            });

            Object.defineProperty(window, 'innerWidth', { value: 1024 });
            Object.defineProperty(window, 'innerHeight', { value: 768 });
            Object.defineProperty(window.screen, 'width', { value: 1920 });
            Object.defineProperty(window.screen, 'height', { value: 1080 });
            Object.defineProperty(navigator, 'language', { value: 'es-ES' });
            Object.defineProperty(document, 'title', { value: 'Test Page' });
            Object.defineProperty(document, 'referrer', { value: 'https://referrer.com' });
        });

        it('should enrich payload with page and UTM data (device in context)', () => {
            const payload = {
                event: 'pageview',
                context: {
                    site_auth: 'test-key',
                    session_id: 'session123',
                    user_id: 'user456',
                    device: {
                        screen_resolution: '1920x1080',
                        language: 'es-ES',
                        viewport_width: '1024',
                        viewport_height: '768'
                    }
                },
                properties: {
                    language_id: 'en-US',
                    persona: 'default',
                    url: 'https://example.com/page',
                    title: 'Test Page',
                    width: 1024,
                    height: 768,
                    utm: {
                        source: 'google'
                    }
                }
            } as any;

            const result = enrichPagePayloadOptimized(payload);

            expect(result).toEqual({
                event: 'pageview',
                context: payload.context,
                properties: payload.properties,
                page: {
                    url: 'https://example.com/page',
                    doc_encoding: 'UTF-8',
                    doc_hash: '#section',
                    doc_protocol: 'https:',
                    doc_search: '?utm_source=google',
                    doc_host: 'example.com',
                    doc_path: '/page',
                    title: 'Test Page',
                    language_id: undefined,
                    persona: undefined
                },
                local_time: expect.stringMatching(
                    /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{2}:\d{2}$/
                ),
                utm: {
                    source: 'google'
                },
                custom: {
                    language_id: 'en-US',
                    persona: 'default',
                    utm: {
                        source: 'google'
                    }
                }
            });
        });

        it('should not include UTM data when no UTM parameters exist', () => {
            Object.defineProperty(window, 'location', {
                value: {
                    href: 'https://example.com/page',
                    pathname: '/page',
                    hostname: 'example.com',
                    protocol: 'https:',
                    hash: '',
                    search: ''
                },
                writable: true
            });

            const payload = {
                event: 'pageview',
                context: {
                    site_auth: 'test-key',
                    session_id: 'session123',
                    user_id: 'user456',
                    device: {
                        screen_resolution: '1920x1080',
                        language: 'es-ES',
                        viewport_width: '1024',
                        viewport_height: '768'
                    }
                },
                properties: {
                    language_id: 'en-US',
                    persona: 'default',
                    title: 'Test Page',
                    width: 1024,
                    height: 768
                }
            } as any;

            const result = enrichPagePayloadOptimized(payload);

            expect(result).not.toHaveProperty('utm');
            expect(result.context.device).toBeDefined();
        });
    });
});
