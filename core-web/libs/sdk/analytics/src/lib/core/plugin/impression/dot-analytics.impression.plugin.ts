import { AnalyticsInstance } from 'analytics';

import {
    DotCMSImpressionTracker,
    ImpressionSubscription
} from './dot-analytics.impression-tracker';

import { DotCMSAnalyticsConfig } from '../../shared/models';
import {
    createPluginLogger,
    isBrowser,
    setupPluginCleanup
} from '../../shared/utils/dot-analytics.utils';

/**
 * Impression Plugin for DotAnalytics
 * Handles automatic tracking of content visibility and impressions.
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
 * Note: This plugin is only registered if config.impressions is enabled.
 * See getEnhancedTrackingPlugins() for conditional loading logic.
 *
 * @param {DotCMSAnalyticsConfig} config - Configuration with impressions settings
 * @returns {Object} Plugin object with lifecycle methods
 */
export const dotAnalyticsImpressionPlugin = (config: DotCMSAnalyticsConfig) => {
    let impressionTracker: DotCMSImpressionTracker | null = null;
    let subscription: ImpressionSubscription | null = null;
    const logger = createPluginLogger('Impression', config);

    return {
        name: 'dot-analytics-impression',

        /**
         * Initialize impression tracking
         * Called when Analytics.js initializes the plugin with instance context
         * @param instance - Analytics.js instance with track method
         */
        initialize: ({ instance }: { instance: AnalyticsInstance }) => {
            // Create and initialize tracker
            impressionTracker = new DotCMSImpressionTracker(config);
            impressionTracker.initialize();

            // Subscribe to impression events and call analytics track
            subscription = impressionTracker.onImpression((eventName, payload) => {
                instance.track(eventName, payload);
            });

            logger.info('Impression tracking plugin initialized');

            return Promise.resolve();
        },

        /**
         * Setup cleanup handlers when plugin is loaded
         * Called after Analytics.js completes plugin loading
         */
        loaded: () => {
            if (isBrowser() && impressionTracker) {
                const cleanup = () => {
                    // Unsubscribe before cleanup
                    if (subscription) {
                        subscription.unsubscribe();
                        subscription = null;
                    }

                    if (impressionTracker) {
                        impressionTracker.cleanup();
                        impressionTracker = null;

                        logger.info('Impression tracking cleaned up on page unload');
                    }
                };

                // Cleanup on page unload
                setupPluginCleanup(cleanup);
            }

            return true;
        }
    };
};
