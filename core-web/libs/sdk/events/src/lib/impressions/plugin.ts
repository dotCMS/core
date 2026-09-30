import { DotCMSImpressionTracker } from './tracker';

import { setupPluginCleanup } from '../contentlets/utils';
import { createPluginLogger, isBrowser } from '../pipeline/utils';

import type { ImpressionSubscription } from './tracker';
import type { PipelineConfig } from '../pipeline/models';
import type { AnalyticsInstance } from 'analytics';

/**
 * Impressions plugin of the events pipeline.
 * Handles automatic tracking of content visibility and impressions.
 *
 * This plugin initializes the impression tracker which:
 * - Uses IntersectionObserver to detect when contentlets are visible
 * - Tracks dwell time (how long elements are visible)
 * - Fires 'content-impression' events via instance.track()
 * - Deduplicates impressions per session
 *
 * Analytics.js runs the plugins in the order events.ts registers them: identity, experiments,
 * impressions, clicks, enricher, sender. This one hooks into no event: its tracker fires
 * events through instance.track(), which runs them through the other plugins like any event.
 *
 * Note: This plugin is only registered if config.impressions is enabled.
 * See getEnhancedTrackingPlugins() for conditional loading logic.
 *
 * @param {PipelineConfig} config - Configuration with impressions settings
 * @returns {Object} Plugin object with lifecycle methods
 */
export const impressionsPlugin = (config: PipelineConfig) => {
    let impressionTracker: DotCMSImpressionTracker | null = null;
    let subscription: ImpressionSubscription | null = null;
    const logger = createPluginLogger('Impression', config);

    return {
        name: 'dot-events-impressions',

        /**
         * Initialize impression tracking
         * Called when Analytics.js initializes the plugin with instance context
         * @param instance - Analytics.js instance with track method
         */
        initialize: ({ instance }: { instance: AnalyticsInstance }) => {
            // Only initialize if impressions are enabled
            if (!config.impressions) {
                logger.info('Impression tracking disabled (config.impressions not set)');
                return Promise.resolve();
            }

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
            // Only setup cleanup if tracker was initialized
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
