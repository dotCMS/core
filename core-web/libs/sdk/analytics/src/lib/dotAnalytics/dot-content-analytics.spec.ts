/* eslint-disable @typescript-eslint/no-explicit-any */

import Analytics from 'analytics';

import { DotContentAnalytics } from './dot-content-analytics';
import { dotAnalytics } from './plugin/dot-analytics.plugin';
import { DotContentAnalyticsConfig } from './shared/dot-content-analytics.model';

// Mock the analytics library
jest.mock('analytics');
jest.mock('./plugin/dot-analytics.plugin');

describe('DotAnalytics', () => {
    const mockConfig: DotContentAnalyticsConfig = {
        debug: false,
        server: 'http://test.com',
        apiKey: 'test-key',
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
            (dotAnalytics as jest.Mock).mockReturnValue({ name: 'mock-plugin' });

            // Mock del enricher plugin
            jest.mock('./plugin/dot-analytics.enricher.plugin', () => ({
                dotAnalyticsEnricherPlugin: {
                    name: 'enrich-dot-analytics',
                    'page:dot-analytics': jest.fn(),
                    'track:dot-analytics': jest.fn()
                }
            }));

            await instance.ready();

            expect(Analytics).toHaveBeenCalledWith({
                app: 'dotAnalytics',
                debug: false,
                plugins: [
                    {
                        name: 'enrich-dot-analytics',
                        'page:dot-analytics': expect.any(Function),
                        'track:dot-analytics': expect.any(Function)
                    },
                    { name: 'mock-plugin' }
                ]
            });
            expect(dotAnalytics).toHaveBeenCalledWith(mockConfig);
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

            const consoleErrorMock = jest.spyOn(console, 'error').mockImplementation(() => {
                // Do nothing
            });

            try {
                await instance.ready();
                fail('Should have thrown an error');
            } catch (e) {
                expect(e).toEqual(error);
                expect(console.error).toHaveBeenCalledWith(
                    '[dotCMS DotContentAnalytics] Failed to initialize: Error: Init failed'
                );
            }

            consoleErrorMock.mockRestore();
        });
    });
});
