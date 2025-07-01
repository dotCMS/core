import '@testing-library/jest-dom';
import { render } from '@testing-library/react';

import { DotContentAnalyticsProvider } from './DotContentAnalyticsProvider';

import { initializeContentAnalytics } from '../../dotAnalytics/dot-content-analytics';
import { DotContentAnalyticsConfig } from '../../dotAnalytics/shared/dot-content-analytics.model';
import * as RouterTrackerHook from '../hook/useRouterTracker';

// Mock dependencies
jest.mock('../../dotAnalytics/dot-content-analytics');
jest.mock('../hook/useRouterTracker');

describe('DotContentAnalyticsProvider', () => {
    const mockConfig: DotContentAnalyticsConfig = {
        siteKey: 'test-key',
        server: 'test-server',
        debug: false
    };

    const mockAnalyticsInstance = {
        pageView: jest.fn(),
        track: jest.fn()
    };

    let useRouterTrackerSpy: jest.SpyInstance;

    beforeEach(() => {
        jest.clearAllMocks();
        (initializeContentAnalytics as jest.Mock).mockReturnValue(mockAnalyticsInstance);
        useRouterTrackerSpy = jest
            .spyOn(RouterTrackerHook, 'useRouterTracker')
            .mockImplementation();
    });

    it('should initialize analytics instance with config', () => {
        render(
            <DotContentAnalyticsProvider config={mockConfig}>
                <div>Test Content</div>
            </DotContentAnalyticsProvider>
        );

        expect(initializeContentAnalytics).toHaveBeenCalledWith(mockConfig);
    });

    it('should render children', () => {
        const { getByText } = render(
            <DotContentAnalyticsProvider config={mockConfig}>
                <div>Test Content</div>
            </DotContentAnalyticsProvider>
        );

        expect(getByText('Test Content')).toBeInTheDocument();
    });

    it('should enable router tracking when autoPageView is not false', () => {
        render(
            <DotContentAnalyticsProvider config={mockConfig}>
                <div>Test Content</div>
            </DotContentAnalyticsProvider>
        );

        expect(useRouterTrackerSpy).toHaveBeenCalledWith(mockAnalyticsInstance);
    });

    it('should not enable router tracking when autoPageView is false', () => {
        const configWithAutoPageViewDisabled = { ...mockConfig, autoPageView: false };

        render(
            <DotContentAnalyticsProvider config={configWithAutoPageViewDisabled}>
                <div>Test Content</div>
            </DotContentAnalyticsProvider>
        );

        expect(useRouterTrackerSpy).not.toHaveBeenCalled();
    });

    it('should handle null analytics instance gracefully', () => {
        (initializeContentAnalytics as jest.Mock).mockReturnValue(null);

        const { getByText } = render(
            <DotContentAnalyticsProvider config={mockConfig}>
                <div>Test Content</div>
            </DotContentAnalyticsProvider>
        );

        expect(getByText('Test Content')).toBeInTheDocument();
        expect(useRouterTrackerSpy).not.toHaveBeenCalled();
    });

    it('should not enable router tracking when analytics initialization fails', () => {
        (initializeContentAnalytics as jest.Mock).mockReturnValue(null);

        render(
            <DotContentAnalyticsProvider config={mockConfig}>
                <div>Test Content</div>
            </DotContentAnalyticsProvider>
        );

        expect(useRouterTrackerSpy).not.toHaveBeenCalled();
    });
});
