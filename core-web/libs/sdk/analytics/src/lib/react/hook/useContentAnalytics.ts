import { useCallback, useMemo } from 'react';

import { getUVEState } from '@dotcms/uve';

import { DotCMSAnalytics, DotCMSAnalyticsConfig } from '../../core/shared/models';
import { initializeAnalytics } from '../internal';

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
 * @returns Object with `track()` and `pageView()` methods for analytics tracking
 * @throws {Error} If the configuration is invalid (missing server or siteKey)
 */
export const useContentAnalytics = (config: DotCMSAnalyticsConfig): DotCMSAnalytics => {
    // Memoize instance based on server and siteAuth (the critical config values)
    // Only re-initialize if these change
    const instance = useMemo(() => initializeAnalytics(config), [config.server, config.siteAuth]);

    // Memoize UVE state check to avoid repeated calls
    // UVE state is determined by URL params and window context, so it's stable during component lifecycle
    const isInUVE = useMemo(() => {
        return Boolean(getUVEState());
    }, []);

    if (!instance) {
        throw new Error(
            'DotCMS Analytics: Failed to initialize. Please verify the required configuration (server and siteAuth).'
        );
    }

    const track = useCallback(
        (eventName: string, payload: Record<string, unknown> = {}) => {
            // Skip analytics tracking when inside UVE editor to avoid polluting analytics data
            // with editor interactions and preview activities
            if (isInUVE) {
                return;
            }

            instance.track(eventName, payload);
        },
        [instance, isInUVE]
    );

    const pageView = useCallback(
        (payload: Record<string, unknown> = {}) => {
            // Skip analytics tracking when inside UVE editor to avoid polluting analytics data
            // with editor interactions and preview activities
            if (isInUVE) {
                return;
            }

            instance.pageView(payload);
        },
        [instance, isInUVE]
    );

    return {
        track,
        pageView
    };
};
