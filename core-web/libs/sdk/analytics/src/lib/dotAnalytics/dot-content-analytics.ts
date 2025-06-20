import { DotAnalytics, DotContentAnalyticsConfig } from './shared/dot-content-analytics.model';
import {
    cleanupActivityTracking,
    createAnalyticsInstance,
    updateSessionActivity
} from './shared/dot-content-analytics.utils';

/**
 * Creates an analytics instance for anonymous user tracking.
 *
 * @param {DotContentAnalyticsConfig} config - The configuration object for the analytics instance.
 * @returns {DotAnalytics} - The analytics instance.
 */
export const initializeContentAnalytics = (config: DotContentAnalyticsConfig): DotAnalytics => {
    const analytics = createAnalyticsInstance(config);

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
            updateSessionActivity(); // Update session activity on page view
            analytics?.page(payload);
        },

        /**
         * Track a custom event.
         * @param {string} eventName - The name of the event to track.
         * @param {Record<string, unknown>} payload - The payload to track.
         */
        track: (eventName: string, payload: Record<string, unknown> = {}) => {
            updateSessionActivity(); // Update session activity on custom events
            analytics?.track(eventName, payload);
        }
    };
};
