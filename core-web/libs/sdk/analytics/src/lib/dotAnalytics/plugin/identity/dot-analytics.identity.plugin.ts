import {
    cleanupActivityTracking,
    generateSessionId,
    getLastActivityTime,
    getUserId,
    initializeActivityTracking
} from './dot-analytics.identity.utils';

import {
    AnalyticsHookParams,
    DotAnalyticsContext,
    DotContentAnalyticsConfig
} from '../../shared/dot-content-analytics.model';

/**
 * Gets analytics context with user and session identification
 */
const getIdentityContext = (config: DotContentAnalyticsConfig): DotAnalyticsContext => {
    const sessionId = generateSessionId();
    const userId = getUserId();

    if (config.debug) {
        console.warn('DotAnalytics Identity Context:', {
            sessionId,
            userId,
            lastActivity: getLastActivityTime()
        });
    }

    return {
        site_key: config.siteKey,
        session_id: sessionId,
        user_id: userId
    };
};

/**
 * Identity Plugin for DotAnalytics
 * Handles user ID generation, session management, and activity tracking.
 * This plugin provides consistent identity context across all analytics events.
 */
export const dotAnalyticsIdentityPlugin = (config: DotContentAnalyticsConfig) => {
    return {
        name: 'dot-analytics-identity',

        /**
         * Initialize the identity plugin
         */
        initialize: () => {
            initializeActivityTracking(config);

            if (config.debug) {
                console.warn('DotAnalytics: Identity plugin initialized');
            }

            return Promise.resolve();
        },

        /**
         * Inject identity context into page events
         * This runs BEFORE the enricher plugin
         */
        pageStart: ({ payload }: AnalyticsHookParams) => {
            const context = getIdentityContext(config);

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
            const context = getIdentityContext(config);

            return {
                ...payload,
                context
            };
        },

        /**
         * Clean up on plugin unload
         */
        loaded: () => {
            // Set up cleanup on page unload
            if (typeof window !== 'undefined') {
                window.addEventListener('beforeunload', cleanupActivityTracking);
            }

            return true;
        }
    };
};
