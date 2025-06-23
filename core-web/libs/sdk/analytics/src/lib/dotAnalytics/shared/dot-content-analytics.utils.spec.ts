/* eslint-disable @typescript-eslint/no-explicit-any */
import { beforeEach, describe, expect, it } from '@jest/globals';

import {
    cleanupActivityTracking,
    defaultRedirectFn,
    enrichPagePayloadOptimized,
    extractUTMParameters,
    generateSecureId,
    getAnalyticsContext,
    getAnalyticsScriptTag,
    getBrowserEventData,
    getDataAnalyticsAttributes,
    getDeviceData,
    getLocalTime,
    getLocalTimezone,
    getPageData,
    getSessionId,
    getUserId,
    getUtmData,
    initializeActivityTracking,
    isInsideEditor
} from './dot-content-analytics.utils';

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

    describe('getAnalyticsScriptTag', () => {
        it('should return analytics script tag when present', () => {
            const script = document.createElement('script');
            script.setAttribute('data-analytics-key', 'test-key');
            document.body.appendChild(script);

            const result = getAnalyticsScriptTag();
            expect(result).toBeTruthy();
            expect(result.getAttribute('data-analytics-key')).toBe('test-key');
        });

        it('should throw error when analytics script tag is not found', () => {
            expect(() => getAnalyticsScriptTag()).toThrow('Dot Analytics: Script not found');
        });
    });

    describe('getDataAnalyticsAttributes', () => {
        beforeEach(() => {
            const script = document.createElement('script');
            script.setAttribute('data-analytics-key', 'test-key');
            document.body.appendChild(script);
        });

        it('should return default values when attributes are not set', () => {
            const result = getDataAnalyticsAttributes(mockLocation);

            expect(result).toEqual({
                server: mockLocation.origin,
                debug: false,
                autoPageView: false,
                siteKey: ''
            });
        });

        it('should enable debug when debug attribute exists', () => {
            const script = document.querySelector('script[data-analytics-key]');
            script?.setAttribute('data-analytics-debug', '');

            const result = getDataAnalyticsAttributes(mockLocation);

            expect(result).toEqual({
                server: mockLocation.origin,
                debug: true,
                autoPageView: false,
                siteKey: ''
            });
        });

        it('should enable autoPageView when auto-page-view attribute exists', () => {
            const script = document.querySelector('script[data-analytics-key]');
            script?.setAttribute('data-analytics-auto-page-view', '');

            const result = getDataAnalyticsAttributes(mockLocation);

            expect(result).toEqual({
                server: mockLocation.origin,
                debug: false,
                autoPageView: true,
                siteKey: ''
            });
        });

        it('should handle all attributes together', () => {
            const script = document.querySelector('script[data-analytics-key]');
            script?.setAttribute('data-analytics-debug', '');
            script?.setAttribute('data-analytics-auto-page-view', '');
            script?.setAttribute('data-analytics-site-key', 'test-site-key');

            const result = getDataAnalyticsAttributes(mockLocation);

            expect(result).toEqual({
                server: mockLocation.origin,
                debug: true,
                autoPageView: true,
                siteKey: 'test-site-key'
            });
        });
    });

    describe('createAnalyticsPageViewData', () => {
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
                    referrer: 'https://referrer.com'
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
                '?utm_source=google&utm_medium=cpc&utm_campaign=spring_sale&utm_id=12345'
            );
            const result = extractUTMParameters(location);
            expect(result).toEqual({
                source: 'google',
                medium: 'cpc',
                campaign: 'spring_sale',
                id: '12345'
            });
        });
    });

    describe('defaultRedirectFn', () => {
        const originalLocation = window.location;

        beforeEach(() => {
            // Mock window.location
            delete (window as any).location;
            window.location = { ...originalLocation };
        });

        afterEach(() => {
            window.location = originalLocation;
        });

        it('should update window.location.href with provided URL', () => {
            const testUrl = 'https://test.com';
            defaultRedirectFn(testUrl);
            expect(window.location.href).toBe(testUrl);
        });
    });

    describe('isInsideEditor', () => {
        const originalWindow = window;

        beforeEach(() => {
            // Reset window to original state before each test
            (global as any).window = { ...originalWindow };
        });

        afterEach(() => {
            // Restore window object
            (global as any).window = originalWindow;
        });

        it('should return false when window is undefined', () => {
            (global as any).window = undefined;
            expect(isInsideEditor()).toBe(false);
        });

        it('should return false when window.parent is undefined', () => {
            (window as any).parent = undefined;
            expect(isInsideEditor()).toBe(false);
        });

        it('should return false when window.parent equals window', () => {
            window.parent = window;
            expect(isInsideEditor()).toBe(false);
        });

        it('should return true when window.parent differs from window', () => {
            // Create a new window-like object that's definitely different from window
            const mockParent = { ...window, someUniqueProperty: true };
            Object.defineProperty(window, 'parent', {
                value: mockParent,
                writable: true,
                configurable: true
            });
            expect(isInsideEditor()).toBe(true);
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
            expect(mockSessionStorage.setItem).toHaveBeenCalledTimes(3); // session, start, utm
        });

        it('should return existing valid session ID', () => {
            const existingSessionId = 'session_12345_abc';
            const sessionStartTime = Date.now().toString();

            mockSessionStorage.getItem
                .mockReturnValueOnce(existingSessionId) // session ID
                .mockReturnValueOnce(sessionStartTime) // session start
                .mockReturnValueOnce('{"source":"test"}'); // UTM data

            const result = getSessionId();

            expect(result).toBe(existingSessionId);
        });

        it('should create new session when UTM parameters change', () => {
            const existingSessionId = 'session_12345_abc';
            const sessionStartTime = Date.now().toString();

            mockSessionStorage.getItem
                .mockReturnValueOnce(existingSessionId)
                .mockReturnValueOnce(sessionStartTime)
                .mockReturnValueOnce('{"source":"different"}'); // Different UTM

            const result = getSessionId();

            expect(result).toMatch(/^session_\d+_[a-z0-9]+$/);
            expect(result).not.toBe(existingSessionId);
        });
    });

    describe('getLocalTimezone', () => {
        it('should return timezone using Intl.DateTimeFormat', () => {
            const mockIntl = {
                DateTimeFormat: jest.fn(() => ({
                    resolvedOptions: () => ({ timeZone: 'America/New_York' })
                }))
            };

            Object.defineProperty(global, 'Intl', {
                value: mockIntl,
                writable: true
            });

            const result = getLocalTimezone();

            expect(result).toBe('America/New_York');
        });

        it('should fallback to UTC when Intl is not available', () => {
            const originalIntl = global.Intl;
            delete (global as any).Intl;

            const consoleSpy = jest.spyOn(console, 'warn').mockImplementation();

            const result = getLocalTimezone();

            expect(result).toBe('UTC');
            expect(consoleSpy).toHaveBeenCalledWith(
                'DotAnalytics: Intl.DateTimeFormat not supported, using UTC'
            );

            global.Intl = originalIntl;
            consoleSpy.mockRestore();
        });
    });

    describe('getLocalTime', () => {
        it('should return local time in ISO 8601 format with timezone offset', () => {
            jest.setSystemTime(new Date('2024-01-01T12:30:45Z'));

            const result = getLocalTime();

            // Should match ISO 8601 format with timezone offset
            // Examples: "2024-01-01T12:30:45Z", "2024-01-01T07:30:45-05:00", "2024-01-01T13:30:45+01:00"
            expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(Z|[+-]\d{2}:\d{2})$/);
        });

        it('should handle formatter fallback for older browsers', () => {
            const originalIntl = global.Intl;
            delete (global as any).Intl;

            jest.setSystemTime(new Date('2024-01-01T12:30:45Z'));

            const result = getLocalTime();

            // When Intl is not available, it still should include timezone offset
            expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(Z|[+-]\d{2}:\d{2})$/);

            global.Intl = originalIntl;
        });

        it('should return Z suffix for UTC timezone', () => {
            // Mock getTimezoneOffset to return 0 (UTC)
            const originalGetTimezoneOffset = Date.prototype.getTimezoneOffset;
            Date.prototype.getTimezoneOffset = jest.fn(() => 0);

            jest.setSystemTime(new Date('2024-01-01T12:30:45Z'));

            const result = getLocalTime();

            expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$/);

            // Restore original method
            Date.prototype.getTimezoneOffset = originalGetTimezoneOffset;
        });

        it('should return correct offset for different timezones', () => {
            // Mock getTimezoneOffset to return 300 minutes (UTC-5, like EST)
            const originalGetTimezoneOffset = Date.prototype.getTimezoneOffset;
            Date.prototype.getTimezoneOffset = jest.fn(() => 300);

            jest.setSystemTime(new Date('2024-01-01T12:30:45Z'));

            const result = getLocalTime();

            expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}-05:00$/);

            // Restore original method
            Date.prototype.getTimezoneOffset = originalGetTimezoneOffset;
        });

        it('should return correct offset for positive timezone', () => {
            // Mock getTimezoneOffset to return -120 minutes (UTC+2, like CEST)
            const originalGetTimezoneOffset = Date.prototype.getTimezoneOffset;
            Date.prototype.getTimezoneOffset = jest.fn(() => -120);

            jest.setSystemTime(new Date('2024-01-01T12:30:45Z'));

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
                utm: {}
            } as any;

            const payload = {
                properties: {
                    language_id: 'en-US',
                    persona: 'default'
                }
            } as any;

            const result = getPageData(browserData, payload);

            expect(result).toEqual({
                url: 'https://example.com/page',
                doc_encoding: 'UTF-8',
                title: 'Test Page',
                language_id: 'en-US',
                persona: 'default',
                dot_path: '/page',
                dot_host: 'example.com',
                doc_protocol: 'https:',
                doc_hash: '#section',
                doc_search: '?param=1'
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

    describe('initializeActivityTracking', () => {
        let addEventListenerSpy: jest.SpyInstance;

        beforeEach(() => {
            addEventListenerSpy = jest.spyOn(window, 'addEventListener');
        });

        afterEach(() => {
            addEventListenerSpy.mockRestore();
            cleanupActivityTracking(); // Clean up after each test
        });

        it('should initialize event listeners for activity tracking', () => {
            const config = { debug: false } as any;

            initializeActivityTracking(config);

            expect(addEventListenerSpy).toHaveBeenCalledWith('click', expect.any(Function), {
                passive: true
            });
            expect(addEventListenerSpy).toHaveBeenCalledWith('keydown', expect.any(Function), {
                passive: true
            });
            expect(addEventListenerSpy).toHaveBeenCalledWith('focus', expect.any(Function), {
                passive: true
            });
        });
    });

    describe('cleanupActivityTracking', () => {
        let removeEventListenerSpy: jest.SpyInstance;

        beforeEach(() => {
            removeEventListenerSpy = jest.spyOn(window, 'removeEventListener');
        });

        afterEach(() => {
            removeEventListenerSpy.mockRestore();
        });

        it('should remove all activity event listeners', () => {
            const config = { debug: false } as any;

            // Initialize first
            initializeActivityTracking(config);

            // Then cleanup
            cleanupActivityTracking();

            expect(removeEventListenerSpy).toHaveBeenCalledWith('click', expect.any(Function));
            expect(removeEventListenerSpy).toHaveBeenCalledWith('keydown', expect.any(Function));
            expect(removeEventListenerSpy).toHaveBeenCalledWith('focus', expect.any(Function));
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

        it('should return analytics context with session and user IDs', () => {
            mockLocalStorage.getItem.mockReturnValue('user_12345');
            mockSessionStorage.getItem
                .mockReturnValueOnce('session_67890')
                .mockReturnValueOnce(Date.now().toString())
                .mockReturnValueOnce('{}');

            const config = { siteKey: 'test-site', debug: false } as any;

            const result = getAnalyticsContext(config);

            expect(result).toEqual({
                site_key: 'test-site',
                session_id: 'session_67890',
                user_id: 'user_12345',
                local_tz: expect.any(String)
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
            Object.defineProperty(document, 'title', { value: 'Test Page' });
        });

        it('should enrich payload with page, device, and UTM data', () => {
            const payload = {
                event: 'page_view',
                properties: {
                    language_id: 'en-US',
                    persona: 'default',
                    url: 'https://example.com/page',
                    title: 'Test Page'
                }
            } as any;

            const result = enrichPagePayloadOptimized(payload);

            expect(result).toEqual(
                expect.objectContaining({
                    event: 'page_view',
                    properties: payload.properties,
                    page: expect.objectContaining({
                        url: 'https://example.com/page',
                        title: 'Test Page',
                        language_id: 'en-US',
                        persona: 'default'
                    }),
                    device: expect.objectContaining({
                        viewport_width: '1024',
                        viewport_height: '768'
                    }),
                    local_time: expect.stringMatching(
                        /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(Z|[+-]\d{2}:\d{2})$/
                    ),
                    utm: expect.objectContaining({
                        source: 'google'
                    })
                })
            );
        });

        it('should not include UTM data when no UTM parameters exist', () => {
            Object.defineProperty(window, 'location', {
                value: {
                    href: 'https://example.com/page',
                    search: ''
                },
                writable: true
            });

            const payload = {
                event: 'page_view',
                properties: {
                    language_id: 'en-US',
                    persona: 'default'
                }
            } as any;

            const result = enrichPagePayloadOptimized(payload);

            expect(result).not.toHaveProperty('utm');
        });
    });
});
