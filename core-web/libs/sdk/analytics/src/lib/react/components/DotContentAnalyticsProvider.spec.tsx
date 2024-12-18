import { jest } from '@jest/globals';
import '@testing-library/jest-dom';
import { render, waitFor } from '@testing-library/react';

import { DotContentAnalyticsProvider } from './DotContentAnalyticsProvider';

import { DotContentAnalytics } from '../../dotAnalytics/dot-content-analytics';
import { DotContentAnalyticsConfig } from '../../dotAnalytics/shared/dot-content-analytics.model';

// Mock dependencies
jest.mock('../../dotAnalytics/dot-content-analytics');
jest.mock('../hook/useRouterTracker');

describe('DotContentAnalyticsProvider', () => {
    const mockConfig: DotContentAnalyticsConfig = {
        apiKey: 'test-key',
        server: 'test-server',
        debug: false
    };

    const mockDotContentAnalyticsInstance = {
        ready: jest.fn<() => Promise<void>>().mockResolvedValue(),
        pageView: jest.fn(),
        track: jest.fn(),
        getInstance: jest.fn<() => Promise<boolean>>().mockResolvedValue(true)
    } as Partial<DotContentAnalytics>;

    beforeEach(() => {
        jest.clearAllMocks();
        DotContentAnalytics.getInstance = jest
            .fn()
            .mockReturnValue(mockDotContentAnalyticsInstance);
    });

    it('should initialize analytics instance with config', () => {
        render(
            <DotContentAnalyticsProvider config={mockConfig}>
                <div>Test Child</div>
            </DotContentAnalyticsProvider>
        );

        expect(DotContentAnalytics.getInstance).toHaveBeenCalledWith(mockConfig);
    });

    it('should call ready() on mount', async () => {
        render(
            <DotContentAnalyticsProvider config={mockConfig}>
                <div>Test Child</div>
            </DotContentAnalyticsProvider>
        );

        await waitFor(() => {
            expect(mockDotContentAnalyticsInstance.ready).toHaveBeenCalled();
        });
    });

    it('should render children', () => {
        const { getByText } = render(
            <DotContentAnalyticsProvider config={mockConfig}>
                <div>Test Child</div>
            </DotContentAnalyticsProvider>
        );

        expect(getByText('Test Child')).toBeInTheDocument();
    });

    it('should handle ready() rejection', async () => {
        const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {
            // Do nothing
        });
        const error = new Error('Test error');
        mockDotContentAnalyticsInstance.ready.mockRejectedValueOnce(error);

        render(
            <DotContentAnalyticsProvider config={mockConfig}>
                <div>Test Child</div>
            </DotContentAnalyticsProvider>
        );

        await waitFor(() => {
            expect(consoleSpy).toHaveBeenCalledWith('Error initializing analytics:', error);
        });

        consoleSpy.mockRestore();
    });
});
