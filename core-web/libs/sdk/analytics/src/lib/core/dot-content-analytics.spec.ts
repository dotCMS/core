/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable @typescript-eslint/no-non-null-assertion */
import { Analytics } from 'analytics';

import { initializeContentAnalytics } from './dot-content-analytics';
import { dotAnalytics } from './plugin/dot-analytics.plugin';
import { dotAnalyticsEnricherPlugin } from './plugin/enricher/dot-analytics.enricher.plugin';
import { dotAnalyticsIdentityPlugin } from './plugin/identity/dot-analytics.identity.plugin';
import { DotCMSAnalyticsConfig } from './shared/dot-content-analytics.model';
import { updateSessionActivity } from './shared/dot-content-analytics.utils';

// Mock dependencies
jest.mock('analytics');
jest.mock('./plugin/dot-analytics.plugin');
jest.mock('./plugin/enricher/dot-analytics.enricher.plugin');
jest.mock('./plugin/identity/dot-analytics.identity.plugin');
jest.mock('./shared/dot-content-analytics.utils');

const mockAnalytics = Analytics as jest.MockedFunction<typeof Analytics>;
const mockDotAnalytics = dotAnalytics as jest.MockedFunction<typeof dotAnalytics>;
const mockDotAnalyticsEnricherPlugin = dotAnalyticsEnricherPlugin as jest.MockedFunction<
    typeof dotAnalyticsEnricherPlugin
>;
const mockDotAnalyticsIdentityPlugin = dotAnalyticsIdentityPlugin as jest.MockedFunction<
    typeof dotAnalyticsIdentityPlugin
>;
const mockUpdateSessionActivity = updateSessionActivity as jest.MockedFunction<
    typeof updateSessionActivity
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

        // Setup mocks
        mockAnalytics.mockReturnValue(mockAnalyticsInstance as any);
        mockDotAnalytics.mockReturnValue({} as any);
        mockDotAnalyticsEnricherPlugin.mockReturnValue({} as any);
        mockDotAnalyticsIdentityPlugin.mockReturnValue({} as any);

        // Mock global window
        Object.defineProperty(global, 'window', {
            value: {
                addEventListener: jest.fn(),
                __dotAnalyticsCleanup: null
            },
            writable: true
        });
    });

    afterEach(() => {
        // Clean up global mocks
        delete (global as any).window;
    });

    it('should create analytics instance with correct config and plugins', () => {
        const analytics = initializeContentAnalytics(mockConfig);

        expect(analytics).not.toBeNull();
        expect(mockAnalytics).toHaveBeenCalledWith({
            app: 'dotAnalytics',
            debug: false,
            plugins: [
                expect.any(Object), // dotAnalyticsIdentityPlugin result
                expect.any(Object), // dotAnalyticsEnricherPlugin result
                expect.any(Object) // dotAnalytics result
            ]
        });

        expect(mockDotAnalyticsIdentityPlugin).toHaveBeenCalledWith(mockConfig);
        expect(mockDotAnalyticsEnricherPlugin).toHaveBeenCalled();
        expect(mockDotAnalytics).toHaveBeenCalledWith(mockConfig);
    });

    it('should setup window event listeners for cleanup', () => {
        const mockAddEventListener = jest.fn();
        Object.defineProperty(global, 'window', {
            value: {
                addEventListener: mockAddEventListener
            },
            writable: true
        });

        initializeContentAnalytics(mockConfig);

        expect(mockAddEventListener).toHaveBeenCalledWith('beforeunload', expect.any(Function));
    });

    it('should return null when siteAuth is missing', () => {
        const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
        const configWithoutSiteKey = { ...mockConfig, siteAuth: '' };

        const analytics = initializeContentAnalytics(configWithoutSiteKey);

        expect(analytics).toBeNull();
        expect(consoleSpy).toHaveBeenCalledWith(
            'DotContentAnalytics: Missing "siteAuth" in configuration'
        );

        consoleSpy.mockRestore();
    });

    it('should return null when server is missing', () => {
        const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
        const configWithoutServer = { ...mockConfig, server: '' };

        const analytics = initializeContentAnalytics(configWithoutServer);

        expect(analytics).toBeNull();
        expect(consoleSpy).toHaveBeenCalledWith(
            'DotContentAnalytics: Missing "server" in configuration'
        );

        consoleSpy.mockRestore();
    });

    describe('pageView', () => {
        it('should call analytics.page with provided payload and update session activity', () => {
            const payload = { path: '/test', title: 'Test Page' };
            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            analytics!.pageView(payload);

            expect(mockUpdateSessionActivity).toHaveBeenCalled();
            expect(mockAnalyticsInstance.page).toHaveBeenCalledWith(payload);
        });

        it('should call analytics.page with empty object when no payload provided', () => {
            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            analytics!.pageView();

            expect(mockUpdateSessionActivity).toHaveBeenCalled();
            expect(mockAnalyticsInstance.page).toHaveBeenCalledWith({});
        });

        it('should handle case when analytics instance is null', () => {
            mockAnalytics.mockReturnValue(null as any);
            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            // Should not throw error even if internal analytics is null
            expect(() => analytics!.pageView({ path: '/test' })).not.toThrow();
        });
    });

    describe('track', () => {
        it('should call analytics.track with event name, payload and update session activity', () => {
            const eventName = 'button_click';
            const payload = { buttonId: 'submit', value: 123 };
            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            analytics!.track(eventName, payload);

            expect(mockUpdateSessionActivity).toHaveBeenCalled();
            expect(mockAnalyticsInstance.track).toHaveBeenCalledWith(eventName, payload);
        });

        it('should call analytics.track with empty object when no payload provided', () => {
            const eventName = 'custom_event';
            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            analytics!.track(eventName);

            expect(mockUpdateSessionActivity).toHaveBeenCalled();
            expect(mockAnalyticsInstance.track).toHaveBeenCalledWith(eventName, {});
        });

        it('should handle case when analytics instance is null', () => {
            mockAnalytics.mockReturnValue(null as any);
            const analytics = initializeContentAnalytics(mockConfig);

            expect(analytics).not.toBeNull();
            // Should not throw error even if internal analytics is null
            expect(() => analytics!.track('test_event', { value: 123 })).not.toThrow();
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
