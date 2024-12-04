import { beforeEach, describe, expect, it } from '@jest/globals';

import { ANALYTICS_SOURCE_TYPE } from './dot-content-analytics.constants';
import {
    createAnalyticsPageViewData,
    extractUTMParameters,
    getAnalyticsScriptTag,
    getDataAnalyticsAttributes
} from './dot-content-analytics.utils';

describe('Analytics Utils', () => {
    let mockLocation: Location;

    beforeEach(() => {
        // Mock Location object
        mockLocation = {
            href: 'https://example.com/page?param=1',
            pathname: '/page',
            hostname: 'example.com',
            protocol: 'https:',
            hash: '#section1',
            search: '?param=1'
        } as Location;

        // Clean up any previous script tags
        document.querySelectorAll('script').forEach((script) => script.remove());
    });

    describe('getAnalyticsScriptTag', () => {
        it('should return analytics script tag when present', () => {
            const script = document.createElement('script');
            script.setAttribute('data-analytics-server', 'https://analytics.example.com');
            document.body.appendChild(script);

            const result = getAnalyticsScriptTag();
            expect(result).toBeTruthy();
            expect(result.getAttribute('data-analytics-server')).toBe(
                'https://analytics.example.com'
            );
        });

        it('should throw error when analytics script tag is not found', () => {
            expect(() => getAnalyticsScriptTag()).toThrow('Dot Analytics: Script not found');
        });
    });

    describe('getDataAnalyticsAttributes', () => {
        beforeEach(() => {
            const script = document.createElement('script');
            script.setAttribute('data-analytics-server', 'https://analytics.example.com');
            document.body.appendChild(script);
        });

        it('should return default values when attributes are not set', () => {
            const result = getDataAnalyticsAttributes(mockLocation);

            expect(result).toEqual({
                server: 'https://analytics.example.com',
                debug: false,
                autoPageView: false,
                key: ''
            });
        });

        it('should enable debug when debug attribute exists', () => {
            const script = document.querySelector('script[data-analytics-server]');
            script?.setAttribute('data-analytics-debug', '');

            const result = getDataAnalyticsAttributes(mockLocation);

            expect(result).toEqual({
                server: 'https://analytics.example.com',
                debug: true,
                autoPageView: false,
                key: ''
            });
        });

        it('should disable autoPageView when auto-page-view attribute exists', () => {
            const script = document.querySelector('script[data-analytics-server]');
            script?.setAttribute('data-analytics-auto-page-view', '');

            const result = getDataAnalyticsAttributes(mockLocation);

            expect(result).toEqual({
                server: 'https://analytics.example.com',
                debug: false,
                autoPageView: true,
                key: ''
            });
        });

        it('should handle all attributes together', () => {
            const script = document.querySelector('script[data-analytics-server]');
            script?.setAttribute('data-analytics-debug', '');
            script?.setAttribute('data-analytics-auto-page-view', '');
            script?.setAttribute('data-analytics-key', 'test-key');

            const result = getDataAnalyticsAttributes(mockLocation);

            expect(result).toEqual({
                server: 'https://analytics.example.com',
                debug: true,
                autoPageView: true,
                key: 'test-key'
            });
        });

        it('should handle key attribute', () => {
            const script = document.querySelector('script[data-analytics-server]');
            script?.setAttribute('data-analytics-key', 'test-key');

            const result = getDataAnalyticsAttributes(mockLocation);

            expect(result).toEqual({
                server: 'https://analytics.example.com',
                debug: false,
                autoPageView: false,
                key: 'test-key'
            });
        });
    });

    describe('createAnalyticsPageViewData', () => {
        beforeEach(() => {
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
            const result = createAnalyticsPageViewData('page_view', mockLocation);

            expect(result).toEqual(
                expect.objectContaining({
                    event_type: 'page_view',
                    page_title: 'Test Page',
                    doc_path: '/page',
                    doc_host: 'example.com',
                    doc_protocol: 'https:',
                    doc_hash: '#section1',
                    doc_search: '?param=1',
                    screen_resolution: '1920x1080',
                    vp_size: '1024x768',
                    user_agent: 'test-agent',
                    user_language: 'es-ES',
                    doc_encoding: 'UTF-8',
                    referer: 'https://referrer.com',
                    src: ANALYTICS_SOURCE_TYPE
                })
            );
        });

        it('should handle UTM parameters correctly', () => {
            mockLocation.search =
                '?utm_source=test&utm_medium=email&utm_campaign=welcome&utm_id=123';

            const result = createAnalyticsPageViewData('page_view', mockLocation);

            expect(result.utm).toEqual({
                source: 'test',
                medium: 'email',
                campaign: 'welcome',
                id: '123'
            });
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
});
