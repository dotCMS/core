import { AnalyticsInstance } from 'analytics';

import { ClickSubscription, DotCMSClickTracker } from './dot-analytics.click-tracker';

import { DotCMSAnalyticsConfig } from '../../shared/models';
import {
    createPluginLogger,
    isBrowser,
    setupPluginCleanup
} from '../../shared/utils/dot-analytics.utils';

/**
 * Click Plugin for DotAnalytics
 * Handles automatic tracking of clicks on content elements.
 *
 * This plugin initializes the click tracker which:
 * - Uses MutationObserver to detect contentlet containers
 * - Attaches click listeners to each .dotcms-analytics-contentlet element
 * - Filters for clicks on <a> or <button> elements inside tracked contentlets
 * - Extracts contentlet data and element metadata
 * - Throttles clicks to prevent duplicates
 * - Fires 'content_click' events via subscription callback
 *
 * Note: This plugin is only registered if config.clicks is enabled.
 * See getEnhancedTrackingPlugins() for conditional loading logic.
 *
 * @param {DotCMSAnalyticsConfig} config - Configuration with clicks settings
 * @returns {Object} Plugin object with lifecycle methods
 */
export const dotAnalyticsClickPlugin = (config: DotCMSAnalyticsConfig) => {
    let clickTracker: DotCMSClickTracker | null = null;
    let subscription: ClickSubscription | null = null;
    const logger = createPluginLogger('Click', config);

    return {
        name: 'dot-analytics-click',

        /**
         * Initialize click tracking
         * Called when Analytics.js initializes the plugin with instance context
         * @param instance - Analytics.js instance with track method
         */
        initialize: ({ instance }: { instance: AnalyticsInstance }) => {
            // Create and initialize tracker
            clickTracker = new DotCMSClickTracker(config);

            // Subscribe to click events
            subscription = clickTracker.onClick((eventName, payload) => {
                instance.track(eventName, payload);
            });

            // Start tracking
            clickTracker.initialize();

            logger.info('Click tracking plugin initialized');

            return Promise.resolve();
        },

        /**
         * Setup cleanup handlers when plugin is loaded
         * Called after Analytics.js completes plugin loading
         */
        loaded: () => {
            if (isBrowser() && clickTracker) {
                const cleanup = () => {
                    if (subscription) {
                        subscription.unsubscribe();
                        subscription = null;
                    }

                    if (clickTracker) {
                        clickTracker.cleanup();
                        clickTracker = null;

                        logger.info('Click tracking cleaned up on page unload');
                    }
                };

                // Cleanup on page unload
                setupPluginCleanup(cleanup);
            }

            return true;
        }
    };
};
