import { ANALYTICS_PAGEVIEW_EVENT } from '../shared/analytics.constants';
import { sendAnalyticsEventToServer } from '../shared/analytics.http';
import { DotAnalyticsConfig, EventType, PageViewEvent } from '../shared/analytics.model';
import { createAnalyticsPageViewData } from '../shared/analytics.utils';

/**
 * The dotAnalytics plugin.
 *
 * @param {DotAnalyticsConfig} config - The analytics configuration.
 * @returns {Object} - The dotAnalytics plugin.
 */
export const dotAnalyticsPlugin = (config: DotAnalyticsConfig) => {
    let isInitialized = false;

    return {
        name: 'dot-analytics',
        config,

        initialize: ({ config }: { config: DotAnalyticsConfig }) => {
            if (!config.server) {
                throw new Error('DotAnalytics: Server URL is required');
            }

            if (config.debug) {
                console.warn('DotAnalytics: Initialized with config', config);
            }

            isInitialized = true;

            if (config.autoPageView) {
                const body: PageViewEvent = {
                    ...createAnalyticsPageViewData(ANALYTICS_PAGEVIEW_EVENT, window.location),
                    type: EventType.Track,
                    key: config.key
                };

                return sendAnalyticsEventToServer(body, config);
            }

            return Promise.resolve();
        },

        loaded: () => isInitialized
    };
};
