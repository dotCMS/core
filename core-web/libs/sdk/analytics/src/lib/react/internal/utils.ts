import { initializeContentAnalytics } from '../../core/dot-content-analytics';
import {
    DotCMSAnalytics,
    DotCMSAnalyticsConfig
} from '../../core/shared/dot-content-analytics.model';

/**
 * Internal utilities for initializing and accessing the analytics singleton in React apps.
 *
 * Design goals:
 * - Single source of truth for configuration via props
 * - Lazy, memoized initialization (singleton) to avoid duplicate instances
 * - Reset singleton when configuration changes
 */

let singleton: DotCMSAnalytics | null | undefined;
let cachedConfig: DotCMSAnalyticsConfig | null | undefined;

/**
 * Initializes analytics with explicit configuration.
 * Resets singleton if config changes.
 */
export const initializeAnalytics = (config: DotCMSAnalyticsConfig): DotCMSAnalytics | null => {
    // Reset singleton if config changes
    if (
        cachedConfig &&
        (cachedConfig.server !== config.server || cachedConfig.siteKey !== config.siteKey)
    ) {
        singleton = undefined;
    }

    if (singleton !== undefined) {
        return singleton;
    }

    cachedConfig = config;
    singleton = initializeContentAnalytics(config);
    return singleton;
};
