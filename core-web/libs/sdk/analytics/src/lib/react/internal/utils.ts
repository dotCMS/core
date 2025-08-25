import { initializeContentAnalytics } from '../../dotAnalytics/dot-content-analytics';
import { DotCMSAnalytics, DotCMSAnalyticsConfig } from '../../dotAnalytics/shared/dot-content-analytics.model';

/**
 * Internal utilities for initializing and accessing the analytics singleton in React apps.
 *
 * Design goals:
 * - Single source of truth for env-driven configuration
 * - Lazy, memoized initialization (singleton) to avoid duplicate instances
 * - SSR-safe: returns null when required env vars are missing
 */

let singleton: DotCMSAnalytics | null | undefined;
let cachedConfig: DotCMSAnalyticsConfig | null | undefined;

/**
 * Builds analytics configuration from public Next env variables.
 *
 * Reads and validates:
 * - NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY (required)
 * - NEXT_PUBLIC_DOTCMS_HOST (required)
 * - NEXT_PUBLIC_DOTCMS_ANALYTICS_DEBUG (optional, boolean-ish)
 * - NEXT_PUBLIC_DOTCMS_ANALYTICS_AUTO_PAGE_VIEW (optional, boolean-ish)
 *
 * Returns null and logs a warning if required values are missing.
 */
export const getAnalyticsConfigFromEnv = (): DotCMSAnalyticsConfig | null => {
    const siteKey = typeof process !== 'undefined' ? process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY : undefined;
    const server = typeof process !== 'undefined' ? process.env.NEXT_PUBLIC_DOTCMS_HOST : undefined;
    const debugEnv = typeof process !== 'undefined' ? process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_DEBUG : false;
    const autoPageView = typeof process !== 'undefined' ? process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_AUTO_PAGE_VIEW : true;

    if (!siteKey) {
        // eslint-disable-next-line no-console
        console.warn('DotContentAnalytics: NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY is missing. Analytics is disabled.');
        return null;
    }
    if (!server) {
        // eslint-disable-next-line no-console
        console.warn('DotContentAnalytics: NEXT_PUBLIC_DOTCMS_HOST is missing. Analytics is disabled.');
        return null;
    }

    return {
        server,
        siteKey,
        debug: Boolean(debugEnv),
        autoPageView: Boolean(autoPageView)
    };
};

/**
 * Returns the memoized analytics singleton.
 *
 * - First call reads env config and initializes the instance
 * - Subsequent calls return the cached instance
 * - Returns null if config is invalid/missing
 */
export const getAnalyticsInstance = (): DotCMSAnalytics | null => {
    if (singleton !== undefined) {
        return singleton;
    }
    const config = getAnalyticsConfigFromEnv();
    cachedConfig = config;
    singleton = config ? initializeContentAnalytics(config) : null;
    return singleton;
};

/**
 * Exposes the last config used to initialize the singleton.
 * Useful for passing flags (e.g., debug) to hooks without re-reading env.
 */
export const getCachedAnalyticsConfig = (): DotCMSAnalyticsConfig | null | undefined => cachedConfig;


