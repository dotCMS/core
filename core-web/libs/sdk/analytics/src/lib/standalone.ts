import { initializeContentAnalytics } from './core/dot-content-analytics';
import { ANALYTICS_WINDOWS_KEY } from './core/shared/constants';
import { getAnalyticsConfig } from './core/shared/dot-content-analytics.utils';
import { DotCMSAnalytics } from './core/shared/models';

declare global {
    interface Window {
        [ANALYTICS_WINDOWS_KEY]: DotCMSAnalytics | null;
    }
}

(() => {
    // Get configuration and validation
    const config = getAnalyticsConfig();

    // Check if critical attribute is missing (server is auto-detected)
    if (!config.siteAuth) {
        // Don't initialize if siteAuth is missing
        // eslint-disable-next-line no-console
        console.warn(
            'DotAnalytics: Cannot initialize without required configuration: missing data-analytics-auth'
        );
        window[ANALYTICS_WINDOWS_KEY] = null;
        return;
    }

    // Initialize analytics
    try {
        const analytics = initializeContentAnalytics(config);

        if (analytics) {
            // Expose globally
            window[ANALYTICS_WINDOWS_KEY] = analytics;

            // Auto pageview if enabled
            if (config.autoPageView) {
                // Wait for DOM to be ready
                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', () => {
                        analytics.pageView();
                    });
                } else {
                    // DOM is already ready
                    analytics.pageView();
                }
            } else {
                console.warn('DotAnalytics: Auto page view is disabled');
            }
        } else {
            window[ANALYTICS_WINDOWS_KEY] = null;
        }
    } catch (error) {
        // eslint-disable-next-line no-console
        console.error('DotAnalytics: Failed to initialize:', error);
        window[ANALYTICS_WINDOWS_KEY] = null;
    }
})();
