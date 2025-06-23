import { Analytics } from 'analytics';

import { dotAnalytics } from './plugin/dot-analytics.plugin';
import { dotAnalyticsEnricherPlugin } from './plugin/enricher/dot-analytics.enricher.plugin';
import { dotAnalyticsIdentityPlugin } from './plugin/identity/dot-analytics.identity.plugin';
import { DotAnalytics, DotContentAnalyticsConfig } from './shared/dot-content-analytics.model';
import {
    cleanupActivityTracking,
    updateSessionActivity
} from './shared/dot-content-analytics.utils';

/**
 * Creates an analytics instance for content analytics tracking.
 *
 * @param {DotContentAnalyticsConfig} config - The configuration object for the analytics instance.
 * @returns {DotAnalytics} - The analytics instance.
 */
export const initializeContentAnalytics = (
    config: DotContentAnalyticsConfig
): DotAnalytics | null => {
    if (!config.siteKey) {
        console.error('DotContentAnalytics: Missing "siteKey" in configuration');

        return null;
    }

    if (!config.server) {
        console.error('DotContentAnalytics: Missing "server" in configuration');

        return null;
    }

    const analytics = Analytics({
        app: 'dotAnalytics',
        debug: config.debug,
        plugins: [
            dotAnalyticsIdentityPlugin(config), // Inject identity context (user_id, session_id, local_tz)
            dotAnalyticsEnricherPlugin(), // Enrich with page, device, utm data
            dotAnalytics(config) // Send events to server
        ]
    });

    // Store cleanup function globally for use when the page unloads
    const cleanup = () => cleanupActivityTracking();

    if (typeof window !== 'undefined') {
        window.addEventListener('beforeunload', cleanup);
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (window as any).__dotAnalyticsCleanup = cleanup;
    }

    return {
        /**
         * Track a page view.
         * @param {Record<string, unknown>} payload - The payload to track.
         */
        pageView: (payload: Record<string, unknown> = {}) => {
            updateSessionActivity();
            analytics?.page(payload);
        },

        /**
         * Track a custom event.
         * @param {string} eventName - The name of the event to track.
         * @param {Record<string, unknown>} payload - The payload to track.
         */
        track: (eventName: string, payload: Record<string, unknown> = {}) => {
            updateSessionActivity();
            analytics?.track(eventName, payload);
        }
    };
};
