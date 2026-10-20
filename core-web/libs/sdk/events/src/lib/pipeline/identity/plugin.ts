import {
    cleanupActivityTracking,
    initializeActivityTracking,
    updateSessionActivity
} from './activity';

import { getEventContext, onPageDiscard } from '../utils';

import type { AnalyticsBaseParams, PipelineConfig } from '../models';

/**
 * Identity plugin of the events pipeline.
 * Handles user ID generation, session management, and activity tracking.
 * This plugin provides consistent identity context across all analytics events.
 *
 * It runs first: events.ts registers identity, experiments, impressions, clicks, enricher and
 * sender, in that order, and each plugin reads what the ones before it wrote.
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
         * Cleans up activity tracking when the browser discards the page. A page kept in the
         * back/forward cache keeps it, and keeps `__dotAnalyticsActive__` set, so the
         * renderers still print the contentlet attributes once the visitor goes back to it.
         */
        loaded: () => {
            if (typeof window !== 'undefined') {
                onPageDiscard(cleanupActivityTracking);
            }

            return true;
        }
    };
};
