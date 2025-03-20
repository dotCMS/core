import { initializeContentAnalytics } from './dot-content-analytics';
import { DotContentAnalyticsConfig } from './shared/dot-content-analytics.model';
import { createAnalyticsInstance } from './shared/dot-content-analytics.utils';

// Mock dependencies
jest.mock('./shared/dot-content-analytics.utils');

describe('initializeContentAnalytics', () => {
    const mockConfig: DotContentAnalyticsConfig = {
        debug: false,
        server: 'http://test.com',
        apiKey: 'test-key'
    };

    const mockAnalyticsInstance = {
        page: jest.fn(),
        track: jest.fn()
    };

    beforeEach(() => {
        jest.clearAllMocks();
        (createAnalyticsInstance as jest.Mock).mockReturnValue(mockAnalyticsInstance);
    });

    it('should create analytics instance with correct config', () => {
        initializeContentAnalytics(mockConfig);
        expect(createAnalyticsInstance).toHaveBeenCalledWith(mockConfig);
    });

    describe('pageView', () => {
        it('should call analytics.page with provided payload', () => {
            const payload = { path: '/test' };
            const analytics = initializeContentAnalytics(mockConfig);

            analytics.pageView(payload);

            expect(mockAnalyticsInstance.page).toHaveBeenCalledWith(payload);
        });

        it('should call analytics.page with empty object when no payload provided', () => {
            const analytics = initializeContentAnalytics(mockConfig);

            analytics.pageView();

            expect(mockAnalyticsInstance.page).toHaveBeenCalledWith({});
        });
    });

    describe('track', () => {
        it('should call analytics.track with event name and payload', () => {
            const eventName = 'test-event';
            const payload = { value: 123 };
            const analytics = initializeContentAnalytics(mockConfig);

            analytics.track(eventName, payload);

            expect(mockAnalyticsInstance.track).toHaveBeenCalledWith(eventName, payload);
        });

        it('should call analytics.track with empty object when no payload provided', () => {
            const eventName = 'test-event';
            const analytics = initializeContentAnalytics(mockConfig);

            analytics.track(eventName);

            expect(mockAnalyticsInstance.track).toHaveBeenCalledWith(eventName, {});
        });
    });

    describe('when analytics instance is null', () => {
        beforeEach(() => {
            (createAnalyticsInstance as jest.Mock).mockReturnValue(null);
        });

        it('should handle null analytics instance for pageView', () => {
            const analytics = initializeContentAnalytics(mockConfig);
            analytics.pageView({ path: '/test' });
            expect(mockAnalyticsInstance.page).not.toHaveBeenCalled();
        });

        it('should handle null analytics instance for track', () => {
            const analytics = initializeContentAnalytics(mockConfig);
            analytics.track('test-event', { value: 123 });
            expect(mockAnalyticsInstance.track).not.toHaveBeenCalled();
        });
    });
});
