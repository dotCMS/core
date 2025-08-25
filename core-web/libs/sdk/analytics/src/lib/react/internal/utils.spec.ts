import { beforeEach, describe, expect, it, jest } from '@jest/globals';

// Mock initializeContentAnalytics to avoid real initialization
const mockInitialize = jest.fn(() => ({
    pageView: jest.fn(),
    track: jest.fn()
}));

jest.mock('../../dotAnalytics/dot-content-analytics', () => ({
    initializeContentAnalytics: (...args: unknown[]) => mockInitialize(...args)
}));

// Helpers to load a fresh copy of the utils module (resets singletons)
const loadUtils = () => {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const utils = require('./utils') as typeof import('./utils');
    return utils;
};

describe('react/internal/utils', () => {
    const originalEnv = process.env;
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => { });

    beforeEach(() => {
        jest.clearAllMocks();
        jest.resetModules();
        process.env = { ...originalEnv };
        delete process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY;
        delete process.env.NEXT_PUBLIC_DOTCMS_HOST;
        delete process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_DEBUG;
        delete process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_AUTO_PAGE_VIEW;
    });

    it('returns null and warns when siteKey is missing', () => {
        const { getAnalyticsConfigFromEnv } = loadUtils();
        const cfg = getAnalyticsConfigFromEnv();
        expect(cfg).toBeNull();
        expect(warnSpy).toHaveBeenCalled();
    });

    it('returns null and warns when server is missing', () => {
        process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY = 'site';
        const { getAnalyticsConfigFromEnv } = loadUtils();
        const cfg = getAnalyticsConfigFromEnv();
        expect(cfg).toBeNull();
        expect(warnSpy).toHaveBeenCalled();
    });

    it('builds config from env when required vars exist', () => {
        process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY = 'site';
        process.env.NEXT_PUBLIC_DOTCMS_HOST = 'https://demo.dotcms.com';
        process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_DEBUG = 'true';

        const { getAnalyticsConfigFromEnv } = loadUtils();
        const cfg = getAnalyticsConfigFromEnv();
        expect(cfg).toEqual(
            expect.objectContaining({
                server: 'https://demo.dotcms.com',
                siteKey: 'site',
                debug: true
            })
        );
    });

    it('initializes singleton once and caches config', () => {
        process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY = 'site';
        process.env.NEXT_PUBLIC_DOTCMS_HOST = 'https://demo.dotcms.com';

        const { getAnalyticsInstance, getCachedAnalyticsConfig } = loadUtils();

        const a1 = getAnalyticsInstance();
        const a2 = getAnalyticsInstance();

        expect(a1).toBeTruthy();
        expect(a2).toBe(a1);
        expect(mockInitialize).toHaveBeenCalledTimes(1);
        expect(getCachedAnalyticsConfig()).toEqual(
            expect.objectContaining({ server: 'https://demo.dotcms.com', siteKey: 'site' })
        );
    });
});


