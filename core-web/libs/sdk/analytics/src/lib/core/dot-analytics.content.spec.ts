/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable @typescript-eslint/no-non-null-assertion */
import { Analytics } from 'analytics';

import { initializeContentAnalytics } from './dot-analytics.content';
import { dotAnalyticsEnricherPlugin } from './plugin/enricher/dot-analytics.enricher.plugin';
import { dotAnalyticsIdentityPlugin } from './plugin/identity/dot-analytics.identity.plugin';
import { dotAnalyticsImpressionPlugin } from './plugin/impression/dot-analytics.impression.plugin';
import { dotAnalytics } from './plugin/main/dot-analytics.plugin';
import { DotCMSAnalyticsConfig } from './shared/models';

// Mock dependencies
jest.mock('analytics');
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

    let originalWindow: any;

    beforeEach(() => {
        jest.clearAllMocks();

        // Save original window
        originalWindow = global.window;

        // Setup mocks
        mockAnalytics.mockReturnValue(mockAnalyticsInstance as any);
        mockDotAnalytics.mockReturnValue({} as any);
        mockDotAnalyticsEnricherPlugin.mockReturnValue({} as any);
        mockDotAnalyticsIdentityPlugin.mockReturnValue({} as any);
        mockDotAnalyticsImpressionPlugin.mockReturnValue({} as any);

        // Mock global window
        Object.defineProperty(global, 'window', {
            value: {
                addEventListener: jest.fn(),
                dispatchEvent: jest.fn(),
                __dotAnalyticsCleanup: null,
                document: {
                    addEventListener: jest.fn(),
                    removeEventListener: jest.fn()
                }
            },
            writable: true,
            configurable: true
        });
    });

    afterEach(() => {
        // Restore original window
        if (originalWindow) {
            Object.defineProperty(global, 'window', {
                value: originalWindow,
                writable: true,
                configurable: true
            });
        }
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
        const mockAddEventListener = jest.fn();
        const mockDispatchEvent = jest.fn();
        Object.defineProperty(global, 'window', {
            value: {
                addEventListener: mockAddEventListener,
                dispatchEvent: mockDispatchEvent
            },
            writable: true
        });

        initializeContentAnalytics(mockConfig);

        expect(mockAddEventListener).toHaveBeenCalledWith('beforeunload', expect.any(Function));
        expect(mockDispatchEvent).toHaveBeenCalledWith(expect.any(CustomEvent));
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

    describe('when window is not available (SSR)', () => {
        it('should work without window object', () => {
            delete (global as any).window;

            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            expect(analytics!.pageView).toBeDefined();
            expect(analytics!.track).toBeDefined();
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
