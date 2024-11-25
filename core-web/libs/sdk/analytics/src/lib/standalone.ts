import { DotAnalytics } from './analytics';
import { ANALYTICS_WINDOWS_KEY } from './shared/analytics.constants';
import { getDataAnalyticsAttributes } from './shared/analytics.utils';

/**
 * Initialize the analytics library in standalone mode.
 */
declare global {
    interface Window {
        dotAnalytics: DotAnalytics;
    }
}

(async () => {
    const dataAttributes = getDataAnalyticsAttributes(window.location);
    const analytics = DotAnalytics.getInstance({ ...dataAttributes });
    await analytics.ready();
    window[ANALYTICS_WINDOWS_KEY] = analytics;
})().catch((error) => {
    console.error('Failed to initialize analytics:', error);
});
