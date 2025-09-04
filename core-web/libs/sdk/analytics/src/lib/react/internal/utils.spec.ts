import { beforeEach, describe, expect, it, jest } from '@jest/globals';

import { DotCMSAnalytics } from '../../core/shared/dot-content-analytics.model';

// Mock initializeContentAnalytics to avoid real initialization
const mockAnalyticsInstance = {
    pageView: jest.fn(),
    track: jest.fn()
} as unknown as DotCMSAnalytics;

const mockInitialize = jest.fn(() => mockAnalyticsInstance);

jest.mock('../../dotAnalytics/dot-content-analytics', () => ({
    initializeContentAnalytics: mockInitialize
}));

// Helpers to load a fresh copy of the utils module (resets singletons)
const loadUtils = () => {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const utils = require('./utils') as typeof import('./utils');
    return utils;
};

describe('react/internal/utils', () => {
    const mockConfig = {
        server: 'https://demo.dotcms.com',
        siteKey: 'test-site',
        debug: false
    };

    beforeEach(() => {
        jest.clearAllMocks();
        jest.resetModules();
    });

    describe('initializeAnalytics', () => {
        it('initializes and returns singleton instance', () => {
            const { initializeAnalytics } = loadUtils();

            const instance1 = initializeAnalytics(mockConfig);
            const instance2 = initializeAnalytics(mockConfig);

            expect(instance1).toBe(mockAnalyticsInstance);
            expect(instance2).toBe(instance1);
            expect(mockInitialize).toHaveBeenCalledTimes(1);
            expect(mockInitialize).toHaveBeenCalledWith(mockConfig);
        });

        it('resets singleton when server changes', () => {
            const { initializeAnalytics } = loadUtils();

            const instance1 = initializeAnalytics(mockConfig);
            const instance2 = initializeAnalytics({
                ...mockConfig,
                server: 'https://new-server.com'
            });

            expect(instance1).toBe(mockAnalyticsInstance);
            expect(instance2).toBe(mockAnalyticsInstance);
            expect(mockInitialize).toHaveBeenCalledTimes(2);
        });

        it('resets singleton when siteKey changes', () => {
            const { initializeAnalytics } = loadUtils();

            const instance1 = initializeAnalytics(mockConfig);
            const instance2 = initializeAnalytics({
                ...mockConfig,
                siteKey: 'new-site'
            });

            expect(instance1).toBe(mockAnalyticsInstance);
            expect(instance2).toBe(mockAnalyticsInstance);
            expect(mockInitialize).toHaveBeenCalledTimes(2);
        });

        it('does not reset singleton when debug changes', () => {
            const { initializeAnalytics } = loadUtils();

            const instance1 = initializeAnalytics(mockConfig);
            const instance2 = initializeAnalytics({
                ...mockConfig,
                debug: true
            });

            expect(instance1).toBe(mockAnalyticsInstance);
            expect(instance2).toBe(instance1);
            expect(mockInitialize).toHaveBeenCalledTimes(1);
        });
    });
});
