/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable @typescript-eslint/no-non-null-assertion */
import { Analytics } from 'analytics';

import { getUVEState } from '@dotcms/uve';

import { initializeContentAnalytics } from './dot-analytics.content';
import { dotAnalyticsEnricherPlugin } from './plugin/enricher/dot-analytics.enricher.plugin';
import { dotAnalyticsIdentityPlugin } from './plugin/identity/dot-analytics.identity.plugin';
import { dotAnalyticsImpressionPlugin } from './plugin/impression/dot-analytics.impression.plugin';
import { dotAnalytics } from './plugin/main/dot-analytics.plugin';
import { ANALYTICS_WINDOWS_ACTIVE_KEY } from './shared/constants/dot-analytics.constants';
import { DotCMSAnalyticsConfig } from './shared/models';

// Mock dependencies
jest.mock('analytics');
jest.mock('@dotcms/uve');
jest.mock('./plugin/main/dot-analytics.plugin');
jest.mock('./plugin/enricher/dot-analytics.enricher.plugin');
jest.mock('./plugin/identity/dot-analytics.identity.plugin');
jest.mock('./plugin/impression/dot-analytics.impression.plugin');

// Partially mock utils - keep validateAnalyticsConfig but mock cleanupActivityTracking
jest.mock('./shared/utils/dot-analytics.utils', () => {
    const actual = jest.requireActual('./shared/utils/dot-analytics.utils') as Record<
        string,
        unknown
    >;
    return {
        ...actual,
        cleanupActivityTracking: jest.fn()
    };
});

const mockAnalytics = Analytics as jest.MockedFunction<typeof Analytics>;
const mockDotAnalytics = dotAnalytics as jest.MockedFunction<typeof dotAnalytics>;
const mockDotAnalyticsEnricherPlugin = dotAnalyticsEnricherPlugin as jest.MockedFunction<
    typeof dotAnalyticsEnricherPlugin
>;
const mockDotAnalyticsIdentityPlugin = dotAnalyticsIdentityPlugin as jest.MockedFunction<
    typeof dotAnalyticsIdentityPlugin
>;
const mockDotAnalyticsImpressionPlugin = dotAnalyticsImpressionPlugin as jest.MockedFunction<
    typeof dotAnalyticsImpressionPlugin
>;

describe('initializeContentAnalytics', () => {
    const mockConfig: DotCMSAnalyticsConfig = {
        debug: false,
        server: 'https://test.com',
        siteAuth: 'test-site-key',
        autoPageView: false
    };

    const mockAnalyticsInstance = {
        page: jest.fn(),
        track: jest.fn()
    };

    beforeEach(() => {
        jest.clearAllMocks();

        // Mock getUVEState (not in editor by default)
        (getUVEState as jest.Mock).mockReturnValue(undefined);

        // Setup mocks
        mockAnalytics.mockReturnValue(mockAnalyticsInstance as any);
        mockDotAnalytics.mockReturnValue({} as any);
        mockDotAnalyticsEnricherPlugin.mockReturnValue({} as any);
        mockDotAnalyticsIdentityPlugin.mockReturnValue({} as any);
        mockDotAnalyticsImpressionPlugin.mockReturnValue({} as any);
    });

    it('should create analytics instance with correct config and plugins', () => {
        const analytics = initializeContentAnalytics(mockConfig);

        expect(analytics).not.toBeNull();
        expect(mockAnalytics).toHaveBeenCalledWith({
            app: 'dotAnalytics',
            debug: false,
            plugins: expect.any(Array)
        });

        expect(mockDotAnalyticsIdentityPlugin).toHaveBeenCalledWith(mockConfig);
        // impressions and clicks not enabled in mockConfig, so these should NOT be called
        expect(mockDotAnalyticsImpressionPlugin).not.toHaveBeenCalled();
        expect(mockDotAnalyticsEnricherPlugin).toHaveBeenCalled();
        expect(mockDotAnalytics).toHaveBeenCalledWith(mockConfig);
    });

    it('should setup window event listeners for cleanup', () => {
        const addEventListenerSpy = jest.spyOn(window, 'addEventListener');
        const dispatchEventSpy = jest.spyOn(window, 'dispatchEvent');

        initializeContentAnalytics(mockConfig);

        expect(addEventListenerSpy).toHaveBeenCalledWith('beforeunload', expect.any(Function));
        expect(dispatchEventSpy).toHaveBeenCalledWith(expect.any(CustomEvent));

        addEventListenerSpy.mockRestore();
        dispatchEventSpy.mockRestore();
    });

    it('should return null when siteAuth is missing', () => {
        const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
        const configWithoutSiteKey = { ...mockConfig, siteAuth: '' };

        const analytics = initializeContentAnalytics(configWithoutSiteKey);

        expect(analytics).toBeNull();
        expect(consoleSpy).toHaveBeenCalledWith(
            'DotCMS Analytics [Core]: Missing "siteAuth" in configuration'
        );

        consoleSpy.mockRestore();
    });

    it('should return null when server is missing', () => {
        const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
        const configWithoutServer = { ...mockConfig, server: '' };

        const analytics = initializeContentAnalytics(configWithoutServer);

        expect(analytics).toBeNull();
        expect(consoleSpy).toHaveBeenCalledWith(
            'DotCMS Analytics [Core]: Missing "server" in configuration'
        );

        consoleSpy.mockRestore();
    });

    it('should return null and not initialize plugins when inside UVE editor', () => {
        const consoleSpy = jest.spyOn(console, 'warn').mockImplementation();
        (getUVEState as jest.Mock).mockReturnValue({ mode: 'edit' });

        const analytics = initializeContentAnalytics(mockConfig);

        expect(analytics).toBeNull();
        expect(consoleSpy).toHaveBeenCalledWith(
            'DotCMS Analytics [Core]: Analytics disabled inside UVE editor'
        );

        // No plugins should be initialized
        expect(mockAnalytics).not.toHaveBeenCalled();
        expect(mockDotAnalyticsIdentityPlugin).not.toHaveBeenCalled();
        expect(mockDotAnalyticsEnricherPlugin).not.toHaveBeenCalled();
        expect(mockDotAnalytics).not.toHaveBeenCalled();

        // Window active flag should be false
        expect((window as any)[ANALYTICS_WINDOWS_ACTIVE_KEY]).toBe(false);

        consoleSpy.mockRestore();
    });

    describe('pageView', () => {
        it('should call analytics.page with provided payload', () => {
            const payload = { path: '/test', title: 'Test Page' };
            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            analytics!.pageView(payload);

            expect(mockAnalyticsInstance.page).toHaveBeenCalledWith(payload);
        });

        it('should call analytics.page with empty object when no payload provided', () => {
            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            analytics!.pageView();

            expect(mockAnalyticsInstance.page).toHaveBeenCalledWith({});
        });

        it('should handle case when analytics instance is null', () => {
            const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();
            mockAnalytics.mockReturnValue(null as any);
            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            // Should not throw error even if internal analytics is null
            expect(() => analytics!.pageView({ path: '/test' })).not.toThrow();
            expect(consoleWarnSpy).toHaveBeenCalledWith(
                'DotCMS Analytics [Core]: Analytics instance not initialized'
            );

            consoleWarnSpy.mockRestore();
        });
    });

    describe('track', () => {
        it('should call analytics.track with event name and payload', () => {
            const eventName = 'button_click';
            const payload = { buttonId: 'submit', value: 123 };
            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            analytics!.track(eventName, payload);

            expect(mockAnalyticsInstance.track).toHaveBeenCalledWith(eventName, payload);
        });

        it('should call analytics.track with empty object when no payload provided', () => {
            const eventName = 'custom_event';
            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            analytics!.track(eventName, {});

            expect(mockAnalyticsInstance.track).toHaveBeenCalledWith(eventName, {});
        });

        it('should handle case when analytics instance is null', () => {
            const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();
            mockAnalytics.mockReturnValue(null as any);
            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            // Should not throw error even if internal analytics is null
            expect(() => analytics!.track('test_event', { value: 123 })).not.toThrow();
            expect(consoleWarnSpy).toHaveBeenCalledWith(
                'DotCMS Analytics [Core]: Analytics instance not initialized'
            );

            consoleWarnSpy.mockRestore();
        });
    });

    describe('conversion', () => {
        it('should call analytics.track with conversion event type and name', () => {
            const conversionName = 'download';
            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            analytics!.conversion(conversionName);

            expect(mockAnalyticsInstance.track).toHaveBeenCalledWith('conversion', {
                name: conversionName
            });
        });

        it('should call analytics.track with conversion name and custom metadata', () => {
            const conversionName = 'purchase';
            const options = {
                value: 99.99,
                currency: 'USD',
                category: 'ecommerce',
                productId: 'SKU-12345'
            };
            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            analytics!.conversion(conversionName, options);

            expect(mockAnalyticsInstance.track).toHaveBeenCalledWith('conversion', {
                name: conversionName,
                custom: options
            });
        });

        it('should call analytics.track with conversion name and custom data including element', () => {
            const conversionName = 'signup';
            const options = {
                element: {
                    type: 'button',
                    text: 'Subscribe Now',
                    id: 'newsletter-btn',
                    class: 'btn-primary'
                }
            };
            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            analytics!.conversion(conversionName, options);

            expect(mockAnalyticsInstance.track).toHaveBeenCalledWith('conversion', {
                name: conversionName,
                custom: options
            });
        });

        it('should call analytics.track with conversion name and all custom metadata', () => {
            const conversionName = 'purchase';
            const options = {
                element: {
                    type: 'button',
                    text: 'Buy Now',
                    id: 'buy-btn'
                },
                value: 99.99,
                currency: 'USD',
                productId: 'SKU-12345'
            };
            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            analytics!.conversion(conversionName, options);

            expect(mockAnalyticsInstance.track).toHaveBeenCalledWith('conversion', {
                name: conversionName,
                custom: options
            });
        });

        it('should handle case when analytics instance is null', () => {
            const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();
            mockAnalytics.mockReturnValue(null as any);
            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            // Should not throw error even if internal analytics is null
            expect(() => analytics!.conversion('test_conversion')).not.toThrow();
            expect(consoleWarnSpy).toHaveBeenCalledWith(
                'DotCMS Analytics [Core]: Analytics instance not initialized'
            );

            consoleWarnSpy.mockRestore();
        });
    });

    describe('when window is not available (SSR)', () => {
        it('should work without window object', () => {
            const savedWindow = (global as any).window;
            (global as any).window = undefined;

            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            expect(analytics!.pageView).toBeDefined();
            expect(analytics!.track).toBeDefined();

            (global as any).window = savedWindow;
        });
    });

    describe('debug mode', () => {
        it('should pass debug flag to Analytics constructor', () => {
            const debugConfig = { ...mockConfig, debug: true };

            initializeContentAnalytics(debugConfig);

            expect(mockAnalytics).toHaveBeenCalledWith(
                expect.objectContaining({
                    debug: true
                })
            );
        });
    });
});
