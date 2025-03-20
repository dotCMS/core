import { DotAnalytics, DotContentAnalyticsConfig } from './shared/dot-content-analytics.model';
import { createAnalyticsInstance } from './shared/dot-content-analytics.utils';

/**
 * Creates an analytics instance.
 *
 * @param {DotContentAnalyticsConfig} config - The configuration object for the analytics instance.
 * @returns {DotAnalytics} - The analytics instance.
 */
export const initializeContentAnalytics = (config: DotContentAnalyticsConfig): DotAnalytics => {
    const analytics = createAnalyticsInstance(config);

    return {
        /**
         * Track a page view.
         * @param {Record<string, unknown>} payload - The payload to track.
         */
        pageView: (payload: Record<string, unknown> = {}) => {
            analytics?.page(payload);
        },

        /**
         * Track a custom event.
         * @param {string} eventName - The name of the event to track.
         * @param {Record<string, unknown>} payload - The payload to track.
         */
        track: (eventName: string, payload: Record<string, unknown> = {}) => {
            analytics?.track(eventName, payload);
        }
    };
};
