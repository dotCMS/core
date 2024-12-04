import { DotContentAnalytics } from './dot-content-analytics';
import { ANALYTICS_WINDOWS_KEY } from './shared/dot-content-analytics.constants';
import { getDataAnalyticsAttributes } from './shared/dot-content-analytics.utils';

/**
 * Initialize the analytics library in standalone mode.
 */
declare global {
    interface Window {
        [ANALYTICS_WINDOWS_KEY]: DotContentAnalytics;
    }
}

(async () => {
    const dataAttributes = getDataAnalyticsAttributes(window.location);
    const analytics = DotContentAnalytics.getInstance({ ...dataAttributes });
    await analytics.ready();
    window[ANALYTICS_WINDOWS_KEY] = analytics;
})().catch((error) => {
    console.error('Failed to initialize analytics:', error);
});
