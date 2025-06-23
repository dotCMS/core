import { initializeContentAnalytics } from './dotAnalytics/dot-content-analytics';
import { ANALYTICS_WINDOWS_KEY } from './dotAnalytics/shared/dot-content-analytics.constants';
import { DotAnalytics } from './dotAnalytics/shared/dot-content-analytics.model';
import { getDataAnalyticsAttributes } from './dotAnalytics/shared/dot-content-analytics.utils';

declare global {
    interface Window {
        [ANALYTICS_WINDOWS_KEY]: DotAnalytics | null;
    }
}

(() => {
    const dataAttributes = getDataAnalyticsAttributes(window.location);
    const analytics = initializeContentAnalytics(dataAttributes);

    // Only assign to window if analytics was successfully initialized
    if (analytics) {
        window[ANALYTICS_WINDOWS_KEY] = analytics;
    } else {
        console.warn('DotAnalytics: Failed to initialize analytics instance');
        window[ANALYTICS_WINDOWS_KEY] = null;
    }
})();
