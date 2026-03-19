import { useCallback, useMemo } from 'react';

import { getUVEState } from '@dotcms/uve';

import { DotCMSAnalytics, DotCMSAnalyticsConfig, JsonObject } from '../../core/shared/models';
import { initializeAnalytics } from '../internal';

/** No-op analytics instance returned when analytics cannot be initialized (e.g., inside UVE) */
const NOOP_ANALYTICS: DotCMSAnalytics = {
    track: () => {
        //
    },
    pageView: () => {
        //
    },
    conversion: () => {
        //
    }
};

/**
 * React hook for tracking user interactions and page views in your DotCMS application.
 *
 * Use this hook to add analytics tracking to your React components. It automatically
 * handles user sessions, device information, and UTM campaign parameters.
 *
 * **Important:** Tracking is automatically disabled when editing content in DotCMS to avoid
 * polluting your analytics data with editor activity.
 *
 * @example
 * Basic usage - Track custom events
 * ```tsx
 * function ProductCard({ title, price }) {
 *   const { track } = useContentAnalytics({
 *     server: 'https://demo.dotcms.com',
 *     siteAuth: 'my-site-auth',
 *     debug: false
 *   });
 *
 *   const handleAddToCart = () => {
 *     track('add-to-cart', {
 *       product: title,
 *       price: price
 *     });
 *   };
 *
 *   return <button onClick={handleAddToCart}>Add to Cart</button>;
 * }
 * ```
 *
 * @example
 * Track page views manually
 * ```tsx
 * function ArticlePage({ article }) {
 *   const { pageView } = useContentAnalytics({
 *     server: 'https://demo.dotcms.com',
 *     siteKey: 'your-site-key'
 *   });
 *
 *   useEffect(() => {
 *     pageView({
 *       category: article.category,
 *       author: article.author
 *     });
 *   }, [article.id]);
 * }
 * ```
 *
 * @param config - Configuration object with server URL and site key
 * @param config.server - The URL of your DotCMS Analytics server
 * @param config.siteKey - Your unique site key for authentication
 * @param config.debug - Optional. Set to true to see analytics events in the console
 * @returns Object with `track()`, `pageView()`, and `conversion()` methods for analytics tracking
 */
export const useContentAnalytics = (config: DotCMSAnalyticsConfig): DotCMSAnalytics => {
    // Memoize instance based on server and siteAuth (the critical config values)
    // Only re-initialize if these change. Log once when initialization fails.
    const instance = useMemo(() => {
        const result = initializeAnalytics(config);

        if (!result) {
            if (getUVEState()) {
                console.warn(
                    'DotCMS Analytics [React]: Analytics is not initialized because the site is inside the UVE editor. All tracking calls will be ignored.'
                );
            } else {
                console.error(
                    'DotCMS Analytics [React]: Failed to initialize. Please verify the required configuration (server and siteAuth).'
                );
            }
        }

        return result;
    }, [config.server, config.siteAuth]);

    // When inside UVE or config is invalid, return no-op functions
    // so consumers don't need to handle null checks.
    if (!instance) {
        return NOOP_ANALYTICS;
    }

    const track = useCallback(
        (eventName: string, payload: JsonObject = {}) => {
            instance.track(eventName, payload);
        },
        [instance]
    );

    const pageView = useCallback(
        (payload: JsonObject = {}) => {
            instance.pageView(payload);
        },
        [instance]
    );

    const conversion = useCallback(
        (name: string, options: JsonObject = {}) => {
            instance.conversion(name, options);
        },
        [instance]
    );

    return {
        track,
        pageView,
        conversion
    };
};
