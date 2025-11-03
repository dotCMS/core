import { AnalyticsInstance } from 'analytics';

import {
    cleanupImpressionTracking,
    initializeImpressionTracking
} from './dot-analytics.impression.utils';

import { DotCMSAnalyticsConfig } from '../../shared/models';

/**
 * Type for the impression tracker instance
 */
type ImpressionTracker = ReturnType<typeof initializeImpressionTracking>;

/**
 * Impression Plugin for DotAnalytics
 * Handles automatic tracking of content visibility and impressions.
 * Only activates when config.impressions is enabled (true or config object).
 *
 * This plugin initializes the impression tracker which:
 * - Uses IntersectionObserver to detect when contentlets are visible
 * - Tracks dwell time (how long elements are visible)
 * - Fires 'content-impression' events via instance.track()
 * - Deduplicates impressions per session
 *
 * Plugin execution in Analytics.js pipeline:
 * 1. Identity Plugin - Injects context
 * 2. Enricher Plugin - Enriches event data
 * 3. Main Plugin - Sends to queue/server
 * 4. Impression Plugin - Runs independently, fires events via instance.track()
 *
 * @param {DotCMSAnalyticsConfig} config - Configuration with impressions settings
 * @returns {Object} Plugin object with lifecycle methods
 */
export const dotAnalyticsImpressionPlugin = (config: DotCMSAnalyticsConfig) => {
    let impressionTracker: ImpressionTracker | null = null;

    return {
        name: 'dot-analytics-impression',

        /**
         * Initialize impression tracking if enabled
         * Called when Analytics.js initializes the plugin with instance context
         * @param instance - Analytics.js instance with track method
         */
        initialize: ({ instance }: { instance: AnalyticsInstance }) => {
            // Only initialize if impressions config exists
            // Can be true (use defaults) or an object (custom config)
            if (config.impressions) {
                // Pass instance.track directly - simpler and more efficient
                // Bind to maintain proper context when called from tracker
                impressionTracker = initializeImpressionTracking(
                    config,
                    instance.track.bind(instance)
                );

                if (config.debug) {
                    console.warn('DotCMS Analytics: Impression tracking plugin initialized');
                }
            } else if (config.debug) {
                console.warn(
                    'DotCMS Analytics: Impression tracking disabled (config.impressions not set)'
                );
            }

            return Promise.resolve();
        },

        /**
         * Setup cleanup handlers when plugin is loaded
         * Called after Analytics.js completes plugin loading
         */
        loaded: () => {
            if (typeof window !== 'undefined' && impressionTracker) {
                const cleanup = () => {
                    if (impressionTracker) {
                        cleanupImpressionTracking(impressionTracker);
                        impressionTracker = null;

                        if (config.debug) {
                            console.warn(
                                'DotCMS Analytics: Impression tracking cleaned up on page unload'
                            );
                        }
                    }
                };

                // Cleanup on page unload
                // Use both events for maximum compatibility
                window.addEventListener('beforeunload', cleanup);
                window.addEventListener('pagehide', cleanup);
            }

            return true;
        }
    };
};
