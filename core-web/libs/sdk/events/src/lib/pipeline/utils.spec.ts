/* eslint-disable @typescript-eslint/no-explicit-any */
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
    enrichPagePayloadOptimized,
    extractUTMParameters,
    generateSecureId,
    getEventContext,
    getBrowserEventData,
    getLocalTime,
    getSessionId,
    getUserId,
    onPageDiscard
} from './utils';

describe('Analytics Utils', () => {
    let mockLocation: Location;

    beforeAll(() => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date('2024-01-01T00:00:00Z'));
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
            vi.setSystemTime(new Date('2024-01-01T12:00:00Z'));
            const id = generateSecureId('test');

            expect(id).toContain('1704110400000'); // timestamp
        });
    });

    describe('getUserId', () => {
        const mockLocalStorage = {
            getItem: vi.fn(),
            setItem: vi.fn()
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
            expect(mockLocalStorage.getItem).toHaveBeenCalledWith('dot_events_user_id');
        });

        it('should generate new user ID when none exists', () => {
            mockLocalStorage.getItem.mockReturnValue(null);

            const result = getUserId();

            expect(result).toMatch(/^user_\d+_[a-z0-9]+$/);
            expect(mockLocalStorage.setItem).toHaveBeenCalledWith('dot_events_user_id', result);
        });
    });

    describe('getSessionId', () => {
        const mockSessionStorage = {
            getItem: vi.fn(),
            setItem: vi.fn()
        };

        beforeEach(() => {
            Object.defineProperty(window, 'sessionStorage', {
                value: mockSessionStorage,
                writable: true
            });
            mockSessionStorage.getItem.mockClear();
            mockSessionStorage.setItem.mockClear();

            // Set URL with UTM via history (window.location is not mockable in JSDOM)
            history.replaceState({}, '', window.location.pathname + '?utm_source=test');
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
            Date.prototype.getTimezoneOffset = vi.fn().mockReturnValue(0);

            const result = getLocalTime();

            expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\+00:00$/);

            // Restore original method
            Date.prototype.getTimezoneOffset = originalGetTimezoneOffset;
        });

        it('should return correct offset for different timezones', () => {
            const originalGetTimezoneOffset = Date.prototype.getTimezoneOffset;
            Date.prototype.getTimezoneOffset = vi.fn().mockReturnValue(300); // UTC-5

            const result = getLocalTime();

            expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}-05:00$/);

            // Restore original method
            Date.prototype.getTimezoneOffset = originalGetTimezoneOffset;
        });

        it('should return correct offset for positive timezone', () => {
            const originalGetTimezoneOffset = Date.prototype.getTimezoneOffset;
            Date.prototype.getTimezoneOffset = vi.fn().mockReturnValue(-120); // UTC+2

            const result = getLocalTime();

            expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\+02:00$/);

            // Restore original method
            Date.prototype.getTimezoneOffset = originalGetTimezoneOffset;
        });
    });

    describe('getEventContext', () => {
        const mockLocalStorage = {
            getItem: vi.fn(),
            setItem: vi.fn()
        };

        const mockSessionStorage = {
            getItem: vi.fn(),
            setItem: vi.fn()
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
            history.replaceState({}, '', window.location.pathname || '/');

            // The device the test expects, set here so it does not depend on an earlier
            // describe; the values match that describe's, which it defines once for the file
            Object.defineProperty(window, 'innerWidth', { value: 1024 });
            Object.defineProperty(window, 'innerHeight', { value: 768 });
            Object.defineProperty(window.screen, 'width', { value: 1920 });
            Object.defineProperty(window.screen, 'height', { value: 1080 });
            Object.defineProperty(navigator, 'language', { value: 'es-ES' });
            // Midday, so a session that started a second ago started the same day
            vi.setSystemTime(new Date('2024-01-01T12:00:00Z'));

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

            const result = getEventContext(config);

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
        const mockLocationWithUtm: Location = {
            href: 'https://example.com/page',
            pathname: '/page',
            host: 'example.com',
            hostname: 'example.com',
            protocol: 'https:',
            hash: '#section',
            search: '?utm_source=google',
            origin: 'https://example.com',
            port: '',
            assign: vi.fn(),
            replace: vi.fn(),
            reload: vi.fn(),
            toString: () => 'https://example.com/page',
            ancestorOrigins: {} as DOMStringList
        };

        const mockLocationNoUtm: Location = {
            ...mockLocationWithUtm,
            hash: '',
            search: ''
        };

        beforeEach(() => {
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
                    locale_id: 'en-US',
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

            const result = enrichPagePayloadOptimized(payload, mockLocationWithUtm);

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
                    locale_id: 'es-es'
                },
                local_time: expect.stringMatching(
                    /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{2}:\d{2}$/
                ),
                utm: {
                    source: 'google'
                },
                custom: {
                    locale_id: 'en-US',
                    persona: 'default',
                    utm: {
                        source: 'google'
                    }
                }
            });
        });

        it('should not include UTM data when no UTM parameters exist', () => {
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
                    locale_id: 'en-US',
                    persona: 'default',
                    title: 'Test Page',
                    width: 1024,
                    height: 768
                }
            } as any;

            const result = enrichPagePayloadOptimized(payload, mockLocationNoUtm);

            expect(result).not.toHaveProperty('utm');
            expect(result.context.device).toBeDefined();
            expect(result.page.locale_id).toBe('es-es');
        });
    });
});

describe('onPageDiscard', () => {
    const pagehide = (persisted: boolean) =>
        window.dispatchEvent(Object.assign(new Event('pagehide'), { persisted }));

    it('runs on the pagehide that discards the page', () => {
        const discarded = vi.fn();
        onPageDiscard(discarded);

        pagehide(false);

        expect(discarded).toHaveBeenCalledTimes(1);
    });

    it('leaves a page the browser keeps in the back/forward cache as it is, so it works when restored', () => {
        const discarded = vi.fn();
        onPageDiscard(discarded);

        pagehide(true);

        expect(discarded).not.toHaveBeenCalled();
    });

    it('does not run on beforeunload, which also fires before the page goes into that cache', () => {
        const discarded = vi.fn();
        onPageDiscard(discarded);

        window.dispatchEvent(new Event('beforeunload'));

        expect(discarded).not.toHaveBeenCalled();
    });
});
