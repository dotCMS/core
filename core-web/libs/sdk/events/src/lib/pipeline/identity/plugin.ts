import {
    cleanupActivityTracking,
    initializeActivityTracking,
    updateSessionActivity
} from './activity';

import { getEventContext } from '../utils';

import type { AnalyticsBaseParams, PipelineConfig } from '../models';

/**
 * Identity Plugin for DotAnalytics
 * Handles user ID generation, session management, and activity tracking.
 * This plugin provides consistent identity context across all analytics events.
 *
 * Plugin execution order:
 * 1. Identity Plugin (this) - Injects context
 * 2. Enricher Plugin - Adds page/device/utm data
 * 3. Main Plugin - Sends to server
 *
 * @param {PipelineConfig} config - Configuration object containing server URL, site key, and debug settings
 * @returns {Object} Plugin object with methods for initialization and event processing
 */
export const identityPlugin = (config: PipelineConfig) => {
    return {
        name: 'dot-events-identity',

        /**
         * Initialize the identity plugin
         * Sets up activity tracking for session management
         */
        initialize: () => {
            initializeActivityTracking(config);

            return Promise.resolve();
        },

        /**
         * Inject identity context into page events and updates session activity for session management
         * This runs BEFORE the enricher plugin
         */
        pageStart: ({ payload }: AnalyticsBaseParams) => {
            updateSessionActivity();
            const context = getEventContext(config);

            return {
                ...payload,
                context
            };
        },

        /**
         * Inject identity context into track events and updates session activity for session management
         * This runs BEFORE the enricher plugin
         */
        trackStart: ({ payload }: AnalyticsBaseParams) => {
            updateSessionActivity();
            const context = getEventContext(config);

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
