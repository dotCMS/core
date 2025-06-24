import {
    AnalyticsHookParams,
    DotContentAnalyticsConfig
} from '../../shared/dot-content-analytics.model';
import {
    cleanupActivityTracking,
    getAnalyticsContext,
    initializeActivityTracking
} from '../../shared/dot-content-analytics.utils';

/**
 * Identity Plugin for DotAnalytics
 * Handles user ID generation, session management, and activity tracking.
 * This plugin provides consistent identity context across all analytics events.
 *
 * Plugin execution order:
 * 1. Identity Plugin (this) - Injects context
 * 2. Enricher Plugin - Adds page/device/utm data
 * 3. Main Plugin - Sends to server
 */
export const dotAnalyticsIdentityPlugin = (config: DotContentAnalyticsConfig) => {
    return {
        name: 'dot-analytics-identity',

        /**
         * Initialize the identity plugin
         * Sets up activity tracking for session management
         */
        initialize: () => {
            initializeActivityTracking(config);

            return Promise.resolve();
        },

        /**
         * Inject identity context into page events
         * This runs BEFORE the enricher plugin
         */
        pageStart: ({ payload }: AnalyticsHookParams) => {
            const context = getAnalyticsContext(config);

            return {
                ...payload,
                context
            };
        },

        /**
         * Inject identity context into track events
         * This runs BEFORE the enricher plugin
         */
        trackStart: ({ payload }: AnalyticsHookParams) => {
            const context = getAnalyticsContext(config);

            return {
                ...payload,
                context
            };
        },

        /**
         * Clean up on plugin unload
         * Sets up cleanup handlers for activity tracking
         */
        loaded: () => {
            // Set up cleanup on page unload
            if (typeof window !== 'undefined') {
                // beforeunload for traditional browsers and desktop
                window.addEventListener('beforeunload', cleanupActivityTracking);

                // pagehide for mobile/tablet scenarios and modern browsers
                // Handles cases where page goes to bfcache or gets suspended
                window.addEventListener('pagehide', cleanupActivityTracking);
            }

            return true;
        }
    };
};
