/* eslint-disable @typescript-eslint/no-explicit-any */
import { beforeEach, describe, expect, it } from '@jest/globals';

import {
    defaultRedirectFn,
    extractUTMParameters,
    getAnalyticsScriptTag,
    getBrowserEventData,
    getDataAnalyticsAttributes,
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
                apiKey: 'test-key'
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
                apiKey: 'test-key'
            });
        });

        it('should disable autoPageView when auto-page-view attribute exists', () => {
            const script = document.querySelector('script[data-analytics-key]');
            script?.setAttribute('data-analytics-auto-page-view', '');

            const result = getDataAnalyticsAttributes(mockLocation);

            expect(result).toEqual({
                server: mockLocation.origin,
                debug: false,
                autoPageView: true,
                apiKey: 'test-key'
            });
        });

        it('should handle all attributes together', () => {
            const script = document.querySelector('script[data-analytics-key]');
            script?.setAttribute('data-analytics-debug', '');
            script?.setAttribute('data-analytics-auto-page-view', '');
            script?.setAttribute('data-analytics-key', 'test-key');

            const result = getDataAnalyticsAttributes(mockLocation);

            expect(result).toEqual({
                server: mockLocation.origin,
                debug: true,
                autoPageView: true,
                apiKey: 'test-key'
            });
        });

        it('should handle key attribute', () => {
            const script = document.querySelector('script[data-analytics-key]');
            script?.setAttribute('data-analytics-key', 'test-key2');

            const result = getDataAnalyticsAttributes(mockLocation);

            expect(result).toEqual({
                server: mockLocation.origin,
                debug: false,
                autoPageView: false,
                apiKey: 'test-key2'
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
                    userAgent: 'test-agent',
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
});
