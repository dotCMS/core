import { Analytics } from 'analytics';

import { ANALYTICS_WINDOWS_ACTIVE_KEY, ANALYTICS_WINDOWS_CLEANUP_KEY } from '@dotcms/uve/internal';

import { dotAnalytics } from './plugin/dot-analytics.plugin';
import { dotAnalyticsEnricherPlugin } from './plugin/enricher/dot-analytics.enricher.plugin';
import { dotAnalyticsIdentityPlugin } from './plugin/identity/dot-analytics.identity.plugin';
import { dotAnalyticsImpressionPlugin } from './plugin/impression/dot-analytics.impression.plugin';
import {
    cleanupActivityTracking,
    validateAnalyticsConfig
} from './shared/dot-content-analytics.utils';
import { DotCMSAnalytics, DotCMSAnalyticsConfig, JsonObject } from './shared/models';

// Extend Window interface for analytics properties
declare global {
    interface Window {
        [ANALYTICS_WINDOWS_ACTIVE_KEY]?: boolean;
        [ANALYTICS_WINDOWS_CLEANUP_KEY]?: () => void;
    }
}

/**
 * Creates an analytics instance for content analytics tracking.
 *
 * @param {DotCMSAnalyticsConfig} config - The configuration object for the analytics instance.
 * @returns {DotCMSAnalytics} - The analytics instance.
 */
export const initializeContentAnalytics = (
    config: DotCMSAnalyticsConfig
): DotCMSAnalytics | null => {
    // Validate required configuration
    const missingFields = validateAnalyticsConfig(config);
    if (missingFields) {
        console.error(`DotCMS Analytics: Missing ${missingFields.join(' and ')} in configuration`);

        if (typeof window !== 'undefined') {
            window[ANALYTICS_WINDOWS_ACTIVE_KEY] = false;
        }

        return null;
    }

    // Create Analytics.js instance with all plugins
    const analyticsInstance = Analytics({
        app: 'dotAnalytics',
        debug: config.debug,
        plugins: [
            dotAnalyticsIdentityPlugin(config), // Inject identity context
            dotAnalyticsImpressionPlugin(config), // Track content impressions
            dotAnalyticsEnricherPlugin(), // Enrich and clean payload with page, device, utm data and custom data
            dotAnalytics(config) // Send events to server
        ]
    });

    // Store cleanup function globally for use when the page unloads
    const cleanup = () => cleanupActivityTracking();

    if (typeof window !== 'undefined') {
        window.addEventListener('beforeunload', cleanup);
        window[ANALYTICS_WINDOWS_CLEANUP_KEY] = cleanup;
        window[ANALYTICS_WINDOWS_ACTIVE_KEY] = true;

        // Dispatch custom event to notify subscribers that analytics is ready
        window.dispatchEvent(new CustomEvent('dotcms:analytics:ready'));
    }

    return {
        /**
         * Track a page view.
         * Session activity is automatically updated by the identity plugin.
         * @param payload - Optional custom data to include with the page view (any valid JSON object)
         */
        pageView: (payload: JsonObject = {}) => {
            if (!analyticsInstance) {
                console.warn('DotCMS Analytics: Analytics instance not initialized');
                return;
            }
            analyticsInstance.page(payload);
        },

        /**
         * Track a custom event.
         * @param eventName - The name of the event to track
         * @param payload - Custom data to include with the event (any valid JSON object)
         */
        track: (eventName: string, payload: JsonObject = {}) => {
            if (!analyticsInstance) {
                console.warn('DotCMS Analytics: Analytics instance not initialized');
                return;
            }
            analyticsInstance.track(eventName, payload);
        }
    };
};
