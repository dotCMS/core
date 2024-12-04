import { ANALYTICS_PAGEVIEW_EVENT } from '../shared/dot-content-analytics.constants';
import { sendAnalyticsEventToServer } from '../shared/dot-content-analytics.http';
import {
    DotContentAnalyticsConfig,
    EventType,
    PageViewEvent
} from '../shared/dot-content-analytics.model';
import { createAnalyticsPageViewData } from '../shared/dot-content-analytics.utils';

/**
 * The dotAnalytics plugin.
 *
 * @param {DotAnalyticsConfig} config - The analytics configuration.
 * @returns {Object} - The dotAnalytics plugin.
 */
export const dotAnalyticsPlugin = (config: DotContentAnalyticsConfig) => {
    let isInitialized = false;

    return {
        name: 'dot-analytics',
        config,

        initialize: ({ config }: { config: DotContentAnalyticsConfig }) => {
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
