/* eslint-disable @typescript-eslint/no-explicit-any */

import Analytics from 'analytics';

import { DotContentAnalytics } from './dot-content-analytics';
import { dotAnalyticsPlugin } from './plugin/dot-analytics.plugin';

// Mock the analytics library
jest.mock('analytics');
jest.mock('./plugin/dot-analytics.plugin');

describe('DotAnalytics', () => {
    const mockConfig = {
        debug: false,
        server: 'http://test.com',
        key: 'test-key',
        autoPageView: false
    };

    beforeEach(() => {
        jest.clearAllMocks();
        // Reset singleton instance between tests
        (DotContentAnalytics as any).instance = null;
    });

    describe('getInstance', () => {
        it('should create single instance', () => {
            const instance1 = DotContentAnalytics.getInstance(mockConfig);
            const instance2 = DotContentAnalytics.getInstance(mockConfig);

            expect(instance1).toBe(instance2);
        });

        it('should maintain same instance even with different config', () => {
            const instance1 = DotContentAnalytics.getInstance(mockConfig);
            const instance2 = DotContentAnalytics.getInstance({ ...mockConfig, debug: true });

            expect(instance1).toBe(instance2);
        });
    });

    describe('ready', () => {
        it('should initialize analytics with correct config', async () => {
            const instance = DotContentAnalytics.getInstance(mockConfig);
            const mockAnalytics = {};
            (Analytics as jest.Mock).mockReturnValue(mockAnalytics);
            (dotAnalyticsPlugin as jest.Mock).mockReturnValue({ name: 'mock-plugin' });

            await instance.ready();

            expect(Analytics).toHaveBeenCalledWith({
                app: 'dotAnalytics',
                debug: false,
                plugins: [{ name: 'mock-plugin' }]
            });
            expect(dotAnalyticsPlugin).toHaveBeenCalledWith(mockConfig);
        });

        it('should only initialize once', async () => {
            const instance = DotContentAnalytics.getInstance(mockConfig);

            await instance.ready();
            await instance.ready();

            expect(Analytics).toHaveBeenCalledTimes(1);
        });

        it('should throw error if initialization fails', async () => {
            const instance = DotContentAnalytics.getInstance(mockConfig);
            const error = new Error('Init failed');
            (Analytics as jest.Mock).mockImplementation(() => {
                throw error;
            });

            // eslint-disable-next-line @typescript-eslint/no-empty-function
            const consoleErrorMock = jest.spyOn(console, 'error').mockImplementation(() => {});

            try {
                await instance.ready();
            } catch (e) {
                expect(e).toEqual(error);
                expect(console.error).toHaveBeenCalledWith(
                    'Failed to initialize DotAnalytics:',
                    error
                );
            }

            // Restore console.error
            consoleErrorMock.mockRestore();
        });
    });
});
