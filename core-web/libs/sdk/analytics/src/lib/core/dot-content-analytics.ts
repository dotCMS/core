import { Analytics } from 'analytics';

import { dotAnalytics } from './plugin/dot-analytics.plugin';
import { dotAnalyticsEnricherPlugin } from './plugin/enricher/dot-analytics.enricher.plugin';
import { dotAnalyticsIdentityPlugin } from './plugin/identity/dot-analytics.identity.plugin';
import { ANALYTICS_WINDOWS_ACTIVE_KEY, ANALYTICS_WINDOWS_CLEANUP_KEY } from './shared/constants';
import {
    cleanupActivityTracking,
    validateAnalyticsConfig
} from './shared/dot-content-analytics.utils';
import { DotCMSAnalytics, DotCMSAnalyticsConfig, JsonObject } from './shared/models';

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

    const analytics = Analytics({
        app: 'dotAnalytics',
        debug: config.debug,
        plugins: [
            dotAnalyticsIdentityPlugin(config), // Inject identity context (user_id, session_id, local_tz)
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
            analytics?.page(payload);
        },

        /**
         * Track a custom event.
         * Session activity is automatically updated by the identity plugin.
         * @param eventName - The name of the event to track
         * @param payload - Custom data to include with the event (any valid JSON object)
         */
        track: (eventName: string, payload: JsonObject = {}) => {
            analytics?.track(eventName, payload);
        }
    };
};
