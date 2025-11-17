import { AnalyticsInstance } from 'analytics';

import {
    DotCMSImpressionTracker,
    ImpressionSubscription
} from '../../shared/dot-content-analytics.impression-tracker';
import { DotCMSAnalyticsConfig } from '../../shared/models';

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
    let impressionTracker: DotCMSImpressionTracker | null = null;
    let subscription: ImpressionSubscription | null = null;

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
                // Create and initialize tracker
                impressionTracker = new DotCMSImpressionTracker(config);
                impressionTracker.initialize();

                // Subscribe to impression events and call analytics track
                subscription = impressionTracker.onImpression((eventName, payload) => {
                    instance.track(eventName, payload);
                });

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
                    // Unsubscribe before cleanup
                    if (subscription) {
                        subscription.unsubscribe();
                        subscription = null;
                    }

                    if (impressionTracker) {
                        impressionTracker.cleanup();
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
